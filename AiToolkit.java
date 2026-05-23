///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS info.picocli:picocli:4.7.6

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(
    name = "ai-toolkit",
    mixinStandardHelpOptions = true,
    subcommands = {AiToolkit.InstallCommand.class, AiToolkit.ListCommand.class},
    description = "Manage Copilot customization bundles from the teggr/ai-toolkit repository.")
class AiToolkit implements Runnable {

    static final String OWNER = "teggr";
    static final String REPO = "ai-toolkit";
    static final int MAX_RETRIES = 3;

    public static void main(String[] args) {
        System.exit(new CommandLine(new AiToolkit()).execute(args));
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    // ─── shared helpers ───────────────────────────────────────────────────────

    static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    static BranchTree fetchTree(HttpClient client, boolean recursive) throws IOException, InterruptedException {
        for (String branch : List.of("main", "master")) {
            BranchTree tree = fetchTree(client, branch, recursive);
            if (tree != null) return tree;
        }
        throw new IOException("Unable to resolve repository tree for branches main/master.");
    }

    static BranchTree fetchTree(HttpClient client, String branch, boolean recursive)
            throws IOException, InterruptedException {
        String url = String.format(Locale.ROOT,
            "https://api.github.com/repos/%s/%s/git/trees/%s%s",
            OWNER, REPO, branch, recursive ? "?recursive=1" : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "AiToolkit-installer")
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return null;
        if (response.statusCode() != 200)
            throw new IOException("Failed to fetch tree: HTTP " + response.statusCode());
        return new BranchTree(branch, response.body());
    }

    /**
     * Extract paths from a GitHub tree JSON response.
     *
     * @param json       raw JSON from the trees API
     * @param typeFilter "blob" for files, "tree" for directories
     * @param prefix     only include paths starting with this prefix, or null for all
     */
    static List<String> extractPaths(String json, String typeFilter, String prefix) {
        Pattern p1 = Pattern.compile(
            "\\{[^{}]*\\\"path\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"[^{}]*\\\"type\\\"\\s*:\\s*\\\"(blob|tree)\\\"[^{}]*}",
            Pattern.DOTALL);
        Pattern p2 = Pattern.compile(
            "\\{[^{}]*\\\"type\\\"\\s*:\\s*\\\"(blob|tree)\\\"[^{}]*\\\"path\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"[^{}]*}",
            Pattern.DOTALL);

        List<String> paths = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            boolean pathFirst = i == 0;
            Matcher m = (pathFirst ? p1 : p2).matcher(json);
            while (m.find()) {
                String path = unescapeJsonString(pathFirst ? m.group(1) : m.group(2));
                String type = pathFirst ? m.group(2) : m.group(1);
                if (type.equals(typeFilter)
                        && (prefix == null || path.startsWith(prefix))
                        && !paths.contains(path)) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    static String unescapeJsonString(String value) {
        String result = value
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t");
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(result);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                String.valueOf((char) Integer.parseInt(m.group(1), 16))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String rawUrl(String branch, String path) {
        return String.format(Locale.ROOT,
            "https://raw.githubusercontent.com/%s/%s/%s/%s", OWNER, REPO, branch, path);
    }

    enum FileAction { OVERWRITE, SKIP }
    enum Decision { PROMPT, ALL_OVERWRITE, ALL_SKIP }
    record BranchTree(String branch, String json) {}

    // ─── install ──────────────────────────────────────────────────────────────

    @Command(
        name = "install",
        mixinStandardHelpOptions = true,
        description = "Install a bundle from teggr/ai-toolkit into .github (or a custom target).")
    static class InstallCommand implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "<bundle>",
            description = "Bundle to install (e.g. discovery).")
        String bundle;

        @Option(names = "--target", paramLabel = "<dir>",
            description = "Target install directory (default: ./.github).")
        Path targetDir;

        @Option(names = "--global",
            description = "Install into ~/.copilot (overrides --target).")
        boolean global;

        @Option(names = "--force",
            description = "Overwrite existing files without prompting.")
        boolean force;

        private final HttpClient client = newHttpClient();
        private Decision applyToAllDecision = Decision.PROMPT;

        @Override
        public Integer call() throws Exception {
            String bundlePrefix = bundle + "/";
            Path installRoot = resolveInstallRoot();
            Files.createDirectories(installRoot);

            BranchTree tree = fetchTree(client, true);
            List<String> files = extractPaths(tree.json(), "blob", bundlePrefix);

            if (files.isEmpty()) {
                System.err.printf("No files found under %s in %s/%s.%n", bundlePrefix, OWNER, REPO);
                return 1;
            }

            files.sort(Comparator.naturalOrder());
            System.out.printf("Installing bundle '%s' from %s/%s (%s) into %s%n",
                bundle, OWNER, REPO, tree.branch(), installRoot.toAbsolutePath());
            System.out.printf("Found %d files.%n", files.size());

            int installed = 0, skipped = 0, failed = 0;

            for (int i = 0; i < files.size(); i++) {
                String remotePath = files.get(i);
                String relativePath = remotePath.substring(bundlePrefix.length());
                Path destination = installRoot.resolve(relativePath).normalize();

                System.out.printf("[%d/%d] %s -> %s%n", i + 1, files.size(), remotePath, destination);

                FileAction action = decideAction(destination);
                if (action == FileAction.SKIP) {
                    skipped++;
                    System.out.println("  skipped");
                    continue;
                }

                try {
                    downloadWithRetry(rawUrl(tree.branch(), remotePath), destination);
                    installed++;
                    System.out.println("  installed");
                } catch (Exception ex) {
                    failed++;
                    System.err.printf("  failed: %s%n", ex.getMessage());
                }
            }

            System.out.printf("%nSummary: installed=%d skipped=%d failed=%d%n", installed, skipped, failed);
            return failed == 0 ? 0 : 1;
        }

        private Path resolveInstallRoot() {
            if (global) return Paths.get(System.getProperty("user.home"), ".copilot");
            if (targetDir != null) return targetDir.toAbsolutePath().normalize();
            return Paths.get(System.getProperty("user.dir"), ".github");
        }

        private FileAction decideAction(Path destination) {
            if (!Files.exists(destination)) return FileAction.OVERWRITE;
            if (force || applyToAllDecision == Decision.ALL_OVERWRITE) return FileAction.OVERWRITE;
            if (applyToAllDecision == Decision.ALL_SKIP) return FileAction.SKIP;

            Console console = System.console();
            if (console == null) {
                System.out.println("  exists. no interactive console available; skipping (use --force to overwrite).");
                return FileAction.SKIP;
            }

            while (true) {
                String answer = console.readLine("  exists. [o]verwrite, [s]kip, overwrite [a]ll, skip a[l]l: ");
                if (answer == null) return FileAction.SKIP;
                switch (answer.trim().toLowerCase(Locale.ROOT)) {
                    case "o", "overwrite"     -> { return FileAction.OVERWRITE; }
                    case "s", "skip"          -> { return FileAction.SKIP; }
                    case "a", "all-overwrite" -> { applyToAllDecision = Decision.ALL_OVERWRITE; return FileAction.OVERWRITE; }
                    case "l", "all-skip"      -> { applyToAllDecision = Decision.ALL_SKIP;      return FileAction.SKIP; }
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
                        long wait = 500L * attempt;
                        System.err.printf("  retry %d/%d in %dms (%s)%n",
                            attempt, MAX_RETRIES - 1, wait, ex.getMessage());
                        Thread.sleep(wait);
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
                if (response.statusCode() != 200)
                    throw new IOException("HTTP " + response.statusCode() + " for " + url);
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
    }

    // ─── list ─────────────────────────────────────────────────────────────────

    @Command(
        name = "list",
        mixinStandardHelpOptions = true,
        description = "List available bundles in the teggr/ai-toolkit repository.")
    static class ListCommand implements Callable<Integer> {

        private static final List<String> IGNORED = List.of(".github", "docs");

        private final HttpClient client = newHttpClient();

        @Override
        public Integer call() throws Exception {
            BranchTree tree = fetchTree(client, false);
            List<String> bundles = extractPaths(tree.json(), "tree", null)
                .stream()
                .filter(p -> !p.contains("/"))
                .filter(p -> !IGNORED.contains(p))
                .sorted()
                .toList();

            if (bundles.isEmpty()) {
                System.out.println("No bundles found.");
                return 1;
            }

            System.out.printf("Available bundles in %s/%s (%s):%n", OWNER, REPO, tree.branch());
            for (String bundle : bundles) {
                System.out.printf("  %s%n", bundle);
            }
            return 0;
        }
    }
}
