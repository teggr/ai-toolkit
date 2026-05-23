///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+

import java.io.Console;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AiToolkit {

    private static final String OWNER = "teggr";
    private static final String DISCOVERY_PREFIX = "discovery/";
    private static final int MAX_RETRIES = 3;

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private boolean global;
    private boolean force;
    private Path targetDir;
    private String repoRootName;
    private boolean helpRequested;
    private Decision applyToAllDecision = Decision.PROMPT;

    public static void main(String[] args) {
        AiToolkit app = new AiToolkit();
        int code = app.run(args);
        System.exit(code);
    }

    private int run(String[] args) {
        try {
            if (!parseArgs(args)) {
                return 1;
            }
            if (helpRequested) {
                return 0;
            }

            Path installRoot = resolveInstallRoot();
            Files.createDirectories(installRoot);

            BranchTree tree = fetchDiscoveryFiles(repoRootName);
            List<String> discoveryFiles = tree.files();
            if (discoveryFiles.isEmpty()) {
                System.err.println("No files found under discovery/ in remote repository.");
                return 1;
            }

            discoveryFiles.sort(Comparator.naturalOrder());
            System.out.printf("Installing discovery bundle from %s/%s (%s) into %s%n",
                OWNER, repoRootName, tree.branch(), installRoot.toAbsolutePath());
            System.out.printf("Found %d files.%n", discoveryFiles.size());

            int installed = 0;
            int skipped = 0;
            int failed = 0;

            for (int i = 0; i < discoveryFiles.size(); i++) {
                String remotePath = discoveryFiles.get(i);
                String relativePath = remotePath.substring(DISCOVERY_PREFIX.length());
                Path destination = installRoot.resolve(relativePath).normalize();

                System.out.printf("[%d/%d] %s -> %s%n", i + 1, discoveryFiles.size(), remotePath, destination);

                FileAction action = decideAction(destination);
                if (action == FileAction.SKIP) {
                    skipped++;
                    System.out.println("  skipped");
                    continue;
                }

                try {
                    downloadWithRetry(rawUrl(repoRootName, tree.branch(), remotePath), destination);
                    installed++;
                    System.out.println("  installed");
                } catch (Exception ex) {
                    failed++;
                    System.err.printf("  failed: %s%n", ex.getMessage());
                }
            }

            System.out.printf("%nSummary: installed=%d skipped=%d failed=%d%n", installed, skipped, failed);
            return failed == 0 ? 0 : 1;
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            return 1;
        }
    }

    private boolean parseArgs(String[] args) {
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> {
                    printUsage();
                    helpRequested = true;
                    return true;
                }
                case "--global" -> global = true;
                case "--force" -> force = true;
                case "--target" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Missing value for --target");
                        printUsage();
                        return false;
                    }
                    i++;
                    targetDir = Paths.get(args[i]);
                }
                default -> {
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                        printUsage();
                        return false;
                    }
                    positional.add(arg);
                }
            }
        }

        if (positional.size() != 1) {
            System.err.println("Expected a single <repo-root-name> argument.");
            printUsage();
            return false;
        }

        repoRootName = positional.get(0);
        return true;
    }

    private static void printUsage() {
        System.out.println("jbang AiToolkit.java [--target <dir> | --global] [--force] <repo-root-name>");
        System.out.println();
        System.out.println("Install discovery bundle resources from https://github.com/teggr/<repo-root-name>.");
        System.out.println("  --target <dir>  Target install directory (default: ./.github)");
        System.out.println("  --global        Install into ~/.copilot (overrides --target)");
        System.out.println("  --force         Overwrite existing files without prompting");
        System.out.println("  -h, --help      Show this help");
    }

    private Path resolveInstallRoot() {
        if (global) {
            return Paths.get(System.getProperty("user.home"), ".copilot");
        }
        if (targetDir != null) {
            return targetDir.toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), ".github");
    }

    private BranchTree fetchDiscoveryFiles(String repo) throws IOException, InterruptedException {
        for (String branch : List.of("main", "master")) {
            BranchTree branchTree = fetchDiscoveryFiles(repo, branch);
            if (branchTree != null) {
                return branchTree;
            }
        }
        throw new IOException("Unable to resolve repository tree for branches main/master.");
    }

    private BranchTree fetchDiscoveryFiles(String repo, String branch) throws IOException, InterruptedException {
        String treeUrl = String.format(Locale.ROOT,
            "https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", OWNER, repo, branch);

        HttpResponse<String> response = sendStringRequest(treeUrl, Duration.ofSeconds(30));
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch tree index: HTTP " + response.statusCode());
        }

        return new BranchTree(branch, extractDiscoveryFiles(response.body()));
    }

    private List<String> extractDiscoveryFiles(String json) {
        Pattern objectPattern = Pattern.compile("\\{[^{}]*\\\"path\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"[^{}]*\\\"type\\\"\\s*:\\s*\\\"(blob|tree)\\\"[^{}]*}", Pattern.DOTALL);
        Pattern reverseObjectPattern = Pattern.compile("\\{[^{}]*\\\"type\\\"\\s*:\\s*\\\"(blob|tree)\\\"[^{}]*\\\"path\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"[^{}]*}", Pattern.DOTALL);

        List<String> paths = new ArrayList<>();

        Matcher matcher = objectPattern.matcher(json);
        while (matcher.find()) {
            String path = unescapeJsonString(matcher.group(1));
            String type = matcher.group(2);
            if ("blob".equals(type) && path.startsWith(DISCOVERY_PREFIX)) {
                paths.add(path);
            }
        }

        Matcher reverseMatcher = reverseObjectPattern.matcher(json);
        while (reverseMatcher.find()) {
            String type = reverseMatcher.group(1);
            String path = unescapeJsonString(reverseMatcher.group(2));
            if ("blob".equals(type) && path.startsWith(DISCOVERY_PREFIX) && !paths.contains(path)) {
                paths.add(path);
            }
        }

        return paths;
    }

    private static String unescapeJsonString(String value) {
        String result = value
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t");

        Matcher unicodeMatcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(result);
        StringBuffer sb = new StringBuffer();
        while (unicodeMatcher.find()) {
            char ch = (char) Integer.parseInt(unicodeMatcher.group(1), 16);
            unicodeMatcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        unicodeMatcher.appendTail(sb);
        return sb.toString();
    }

    private HttpResponse<String> sendStringRequest(String url, Duration timeout) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "AiToolkit-installer")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String rawUrl(String repo, String branch, String path) {
        return String.format(Locale.ROOT,
            "https://raw.githubusercontent.com/%s/%s/%s/%s", OWNER, repo, branch, path);
    }

    private FileAction decideAction(Path destination) {
        if (!Files.exists(destination)) {
            return FileAction.OVERWRITE;
        }
        if (force || applyToAllDecision == Decision.ALL_OVERWRITE) {
            return FileAction.OVERWRITE;
        }
        if (applyToAllDecision == Decision.ALL_SKIP) {
            return FileAction.SKIP;
        }

        Console console = System.console();
        if (console == null) {
            System.out.println("  exists. no interactive console available; skipping (use --force to overwrite).");
            return FileAction.SKIP;
        }

        while (true) {
            String answer = console.readLine("  exists. [o]verwrite, [s]kip, overwrite [a]ll, skip a[l]l: ");
            if (answer == null) {
                return FileAction.SKIP;
            }
            String normalized = answer.trim().toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "o", "overwrite" -> {
                    return FileAction.OVERWRITE;
                }
                case "s", "skip" -> {
                    return FileAction.SKIP;
                }
                case "a", "all-overwrite" -> {
                    applyToAllDecision = Decision.ALL_OVERWRITE;
                    return FileAction.OVERWRITE;
                }
                case "l", "all-skip" -> {
                    applyToAllDecision = Decision.ALL_SKIP;
                    return FileAction.SKIP;
                }
                default -> System.out.println("  Please enter o, s, a, or l.");
            }
        }
    }

    private void downloadWithRetry(String url, Path destination) throws Exception {
        Files.createDirectories(destination.getParent());

        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                downloadAtomically(url, destination);
                return;
            } catch (Exception ex) {
                lastError = ex;
                if (attempt < MAX_RETRIES) {
                    long waitMillis = 500L * attempt;
                    System.err.printf("  retry %d/%d in %dms (%s)%n",
                        attempt, MAX_RETRIES - 1, waitMillis, ex.getMessage());
                    Thread.sleep(waitMillis);
                }
            }
        }

        throw lastError == null ? new IOException("Unknown download failure") : lastError;
    }

    private void downloadAtomically(String url, Path destination) throws IOException, InterruptedException {
        Path temp = Files.createTempFile(destination.getParent(), ".ai-toolkit-", ".tmp");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "AiToolkit-installer")
                .GET()
                .build();

            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }

            try {
                Files.move(temp, destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException ex) {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private enum FileAction { OVERWRITE, SKIP }

    private enum Decision { PROMPT, ALL_OVERWRITE, ALL_SKIP }

    private record BranchTree(String branch, List<String> files) {}
}
