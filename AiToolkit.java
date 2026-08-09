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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Command(
    name = "ai-toolkit",
    mixinStandardHelpOptions = true,
    subcommands = {AiToolkit.InstallCommand.class, AiToolkit.ListCommand.class, AiToolkit.PluginCommand.class, AiToolkit.UninstallCommand.class},
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

    static String fetchText(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "AiToolkit-installer")
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    static String extractJsonStringField(String json, String fieldName) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(fieldName) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        return unescapeJsonString(m.group(1));
    }

    static List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = normalized.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }

            if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    static void printListEntry(String name, String description, int nameColumnWidth, int descriptionWidth) {
        List<String> wrapped = wrapText(description, descriptionWidth);
        String nameColumn = String.format("  %-" + nameColumnWidth + "s", name);
        System.out.printf("%s  %s%n", nameColumn, wrapped.getFirst());
        for (int i = 1; i < wrapped.size(); i++) {
            System.out.printf("  %" + nameColumnWidth + "s  %s%n", "", wrapped.get(i));
        }
    }

    enum FileAction { OVERWRITE, SKIP }
    enum Decision { PROMPT, ALL_OVERWRITE, ALL_SKIP }
    record BranchTree(String branch, String json) {}

    // ─── install ──────────────────────────────────────────────────────────────

    @Command(
        name = "install",
        mixinStandardHelpOptions = true,
        description = "Install a bundle from teggr/ai-toolkit into .github, .ai, or a custom target.")
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

        @Option(names = "--ai",
            description = "Install into ./.ai — the shared source-of-truth directory that tools like Cursor and Claude Code can symlink into (overrides --target).")
        boolean ai;

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
            List<String> files = extractPaths(tree.json(), "blob", bundlePrefix)
                .stream()
                .filter(path -> !path.equals(bundlePrefix + "plugin.json"))
                .filter(path -> !path.equals(bundlePrefix + "README.md"))
                .toList();

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
            if (ai) return Paths.get(System.getProperty("user.dir"), ".ai");
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

    // ─── uninstall ────────────────────────────────────────────────────────────

    @Command(
        name = "uninstall",
        mixinStandardHelpOptions = true,
        description = "Remove a previously installed bundle from .github (or a custom target).")
    static class UninstallCommand implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "<bundle>",
            description = "Bundle to uninstall (e.g. discovery).")
        String bundle;

        @Option(names = "--target", paramLabel = "<dir>",
            description = "Install directory to remove from (default: ./.github).")
        Path targetDir;

        @Option(names = "--global",
            description = "Remove from ~/.copilot (overrides --target).")
        boolean global;

        @Option(names = "--force",
            description = "Delete files without prompting.")
        boolean force;

        private final HttpClient client = newHttpClient();

        @Override
        public Integer call() throws Exception {
            String bundlePrefix = bundle + "/";
            Path installRoot = resolveInstallRoot();

            if (!Files.isDirectory(installRoot)) {
                System.err.printf("Install directory does not exist: %s%n", installRoot.toAbsolutePath());
                return 1;
            }

            BranchTree tree = fetchTree(client, true);
            List<String> files = extractPaths(tree.json(), "blob", bundlePrefix);

            if (files.isEmpty()) {
                System.err.printf("No files found under %s in %s/%s.%n", bundlePrefix, OWNER, REPO);
                return 1;
            }

            files.sort(Comparator.naturalOrder());
            System.out.printf("Uninstalling bundle '%s' from %s%n", bundle, installRoot.toAbsolutePath());
            System.out.printf("Found %d files.%n", files.size());

            int removed = 0, skipped = 0, missing = 0, directoriesRemoved = 0;

            for (int i = 0; i < files.size(); i++) {
                String remotePath = files.get(i);
                String relativePath = remotePath.substring(bundlePrefix.length());
                Path target = installRoot.resolve(relativePath).normalize();

                System.out.printf("[%d/%d] %s%n", i + 1, files.size(), target);

                if (!Files.exists(target)) {
                    missing++;
                    System.out.println("  not found, skipping");
                    continue;
                }

                boolean doDelete = force;
                if (!doDelete) {
                    Console console = System.console();
                    if (console == null) {
                        System.out.println("  exists. no interactive console; skipping (use --force to delete).");
                        skipped++;
                        continue;
                    }
                    String answer = console.readLine("  delete? [y]es / [n]o: ");
                    doDelete = answer != null && answer.trim().equalsIgnoreCase("y");
                }

                if (doDelete) {
                    try {
                        Files.delete(target);
                        removed++;
                        directoriesRemoved += deleteEmptyParents(target.getParent(), installRoot);
                        System.out.println("  removed");
                    } catch (IOException ex) {
                        System.err.printf("  failed: %s%n", ex.getMessage());
                    }
                } else {
                    skipped++;
                    System.out.println("  skipped");
                }
            }

            System.out.printf("%nSummary: removed=%d skipped=%d not-found=%d directories-removed=%d%n",
                removed, skipped, missing, directoriesRemoved);
            return 0;
        }

        private Path resolveInstallRoot() {
            if (global) return Paths.get(System.getProperty("user.home"), ".copilot");
            if (targetDir != null) return targetDir.toAbsolutePath().normalize();
            return Paths.get(System.getProperty("user.dir"), ".github");
        }

        private int deleteEmptyParents(Path startDir, Path stopDirExclusive) {
            if (startDir == null) return 0;

            int removed = 0;
            Path current = startDir.normalize();

            while (current != null && !current.equals(stopDirExclusive)) {
                if (!Files.isDirectory(current)) {
                    current = current.getParent();
                    continue;
                }

                try (var entries = Files.list(current)) {
                    if (entries.findAny().isPresent()) {
                        break;
                    }
                } catch (IOException ex) {
                    break;
                }

                try {
                    Files.delete(current);
                    removed++;
                    current = current.getParent();
                } catch (DirectoryNotEmptyException ex) {
                    break;
                } catch (IOException ex) {
                    break;
                }
            }

            return removed;
        }
    }

    // ─── list ─────────────────────────────────────────────────────────────────

    @Command(
        name = "list",
        mixinStandardHelpOptions = true,
        description = "List available bundles in the teggr/ai-toolkit repository.")
    static class ListCommand implements Callable<Integer> {

        private final HttpClient client = newHttpClient();

        @Override
        public Integer call() throws Exception {
            BranchTree tree = fetchTree(client, true);
            List<String> manifests = extractPaths(tree.json(), "blob", null)
                .stream()
                // A valid installable bundle must have a top-level plugin.json manifest.
                .filter(p -> p.endsWith("/plugin.json") && p.indexOf('/') == p.lastIndexOf('/'))
                .sorted()
                .toList();

            if (manifests.isEmpty()) {
                System.out.println("No bundles found.");
                return 1;
            }

            List<String> bundleNames = manifests.stream()
                .map(manifestPath -> manifestPath.substring(0, manifestPath.indexOf('/')))
                .toList();
            int longestName = bundleNames.stream().mapToInt(String::length).max().orElse(0);
            int nameColumnWidth = Math.max(12, longestName);
            int descriptionWidth = 96 - (2 + nameColumnWidth + 2);
            if (descriptionWidth < 40) {
                descriptionWidth = 40;
            }

            System.out.printf("Available bundles in %s/%s (%s):%n", OWNER, REPO, tree.branch());
            for (String manifestPath : manifests) {
                String bundle = manifestPath.substring(0, manifestPath.indexOf('/'));
                String description = "(no description)";
                try {
                    String manifestJson = fetchText(client, rawUrl(tree.branch(), manifestPath));
                    String parsed = extractJsonStringField(manifestJson, "description");
                    if (parsed != null && !parsed.isBlank()) {
                        description = parsed.trim();
                    }
                } catch (Exception ignored) {
                    // Keep listing bundles even if one manifest cannot be read.
                }
                printListEntry(bundle, description, nameColumnWidth, descriptionWidth);
            }
            return 0;
        }
    }

    // ─── plugin ───────────────────────────────────────────────────────────────

    @Command(
        name = "plugin",
        mixinStandardHelpOptions = true,
        subcommands = {AiToolkit.PluginCommand.PackageCommand.class, AiToolkit.PluginCommand.ValidateCommand.class},
        description = "Work with Agent Plugin 1.0 packages.")
    static class PluginCommand implements Runnable {

        @Override
        public void run() {
            CommandLine.usage(this, System.out);
        }

        static final String PLUGIN_SCHEMA = "https://agent-plugins.org/schemas/1.0.0/plugin.schema.json";

        @Command(
            name = "package",
            mixinStandardHelpOptions = true,
            description = "Download a bundle and package it as an Agent Plugin 1.0 zip archive.")
        static class PackageCommand implements Callable<Integer> {

            @Parameters(index = "0", paramLabel = "<bundle>",
                description = "Bundle to package (e.g. discovery).")
            String bundle;

            @Option(names = "--output", paramLabel = "<dir>",
                description = "Output directory for the zip (default: ./ai-toolkit-plugins).")
            Path outputDir;

            private final HttpClient client = newHttpClient();

            @Override
            public Integer call() throws Exception {
                Path outDir = outputDir != null
                    ? outputDir.toAbsolutePath().normalize()
                    : Paths.get(System.getProperty("user.dir"), "ai-toolkit-plugins");
                Files.createDirectories(outDir);

                BranchTree tree = fetchTree(client, true);
                String bundlePrefix = bundle + "/";
                List<String> files = extractPaths(tree.json(), "blob", bundlePrefix);

                if (files.isEmpty()) {
                    System.err.printf("No files found under %s in %s/%s.%n", bundlePrefix, OWNER, REPO);
                    return 1;
                }

                // Verify plugin.json is present in the bundle
                boolean hasManifest = files.stream()
                    .anyMatch(f -> f.equals(bundlePrefix + "plugin.json"));
                if (!hasManifest) {
                    System.err.printf(
                        "Warning: no plugin.json found in bundle '%s'. The package will not be a valid Agent Plugin 1.0 archive.%n",
                        bundle);
                }

                files.sort(Comparator.naturalOrder());
                Path zipPath = outDir.resolve(bundle + ".zip");
                System.out.printf("Packaging bundle '%s' from %s/%s (%s) into %s%n",
                    bundle, OWNER, REPO, tree.branch(), zipPath);
                System.out.printf("Found %d files.%n", files.size());

                // Download files into a temp directory, then zip
                Path tempDir = Files.createTempDirectory("ai-toolkit-plugin-");
                try {
                    int downloaded = 0, failed = 0;
                    for (int i = 0; i < files.size(); i++) {
                        String remotePath = files.get(i);
                        String relativePath = remotePath.substring(bundlePrefix.length());
                        Path dest = tempDir.resolve(relativePath).normalize();
                        System.out.printf("[%d/%d] %s%n", i + 1, files.size(), relativePath);
                        try {
                            Files.createDirectories(dest.getParent());
                            HttpRequest req = HttpRequest.newBuilder(URI.create(rawUrl(tree.branch(), remotePath)))
                                .timeout(Duration.ofSeconds(30))
                                .header("User-Agent", "AiToolkit-installer")
                                .GET()
                                .build();
                            HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(dest));
                            if (resp.statusCode() != 200)
                                throw new IOException("HTTP " + resp.statusCode());
                            downloaded++;
                        } catch (Exception ex) {
                            failed++;
                            System.err.printf("  failed: %s%n", ex.getMessage());
                        }
                    }

                    // Zip the temp directory
                    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                        Files.walk(tempDir)
                            .filter(p -> !Files.isDirectory(p))
                            .sorted()
                            .forEach(p -> {
                                String entryName = tempDir.relativize(p).toString().replace('\\', '/');
                                try {
                                    zos.putNextEntry(new ZipEntry(bundle + "/" + entryName));
                                    Files.copy(p, zos);
                                    zos.closeEntry();
                                } catch (IOException ex) {
                                    System.err.printf("  zip error for %s: %s%n", entryName, ex.getMessage());
                                }
                            });
                    }

                    System.out.printf("%nSummary: downloaded=%d failed=%d%n", downloaded, failed);
                    System.out.printf("Plugin archive: %s%n", zipPath.toAbsolutePath());
                    return failed == 0 ? 0 : 1;
                } finally {
                    // Clean up temp directory
                    Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
                }
            }
        }

        @Command(
            name = "validate",
            mixinStandardHelpOptions = true,
            description = "Validate a local directory as an Agent Plugin 1.0 package.")
        static class ValidateCommand implements Callable<Integer> {

            @Parameters(index = "0", paramLabel = "<path>",
                description = "Path to the local plugin directory to validate.")
            Path pluginPath;

            @Override
            public Integer call() throws Exception {
                Path dir = pluginPath.toAbsolutePath().normalize();
                System.out.printf("Validating plugin at %s%n", dir);

                boolean ok = true;

                // Check directory exists
                if (!Files.isDirectory(dir)) {
                    System.err.printf("  ERROR: %s is not a directory.%n", dir);
                    return 1;
                }

                // Check plugin.json
                Path manifest = dir.resolve("plugin.json");
                if (!Files.exists(manifest)) {
                    System.err.println("  ERROR: plugin.json not found.");
                    ok = false;
                } else {
                    String content = Files.readString(manifest, StandardCharsets.UTF_8);
                    if (!content.contains(PLUGIN_SCHEMA)) {
                        System.err.printf("  ERROR: plugin.json $schema must be \"%s\".%n", PLUGIN_SCHEMA);
                        ok = false;
                    } else {
                        System.out.println("  OK: plugin.json present with correct $schema.");
                    }
                    if (!content.contains("\"name\"")) {
                        System.err.println("  ERROR: plugin.json missing required field \"name\".");
                        ok = false;
                    }
                }

                // Check skills/ directory
                Path skillsDir = dir.resolve("skills");
                if (!Files.isDirectory(skillsDir)) {
                    System.out.println("  INFO: no skills/ directory found (plugin provides no skills).");
                } else {
                    long skillCount = Files.list(skillsDir)
                        .filter(Files::isDirectory)
                        .count();
                    System.out.printf("  OK: skills/ directory found with %d skill(s).%n", skillCount);

                    // Each skill should have a SKILL.md
                    Files.list(skillsDir)
                        .filter(Files::isDirectory)
                        .forEach(skillDir -> {
                            if (!Files.exists(skillDir.resolve("SKILL.md"))) {
                                System.err.printf("  WARN: skill directory '%s' has no SKILL.md.%n",
                                    skillDir.getFileName());
                            }
                        });
                }

                if (ok) {
                    System.out.println("Validation passed.");
                    return 0;
                } else {
                    System.err.println("Validation failed.");
                    return 1;
                }
            }
        }
    }
}

