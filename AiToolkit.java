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

@Command(
    name = "ai-toolkit",
    mixinStandardHelpOptions = true,
    subcommands = {AiToolkit.InstallCommand.class, AiToolkit.ListCommand.class, AiToolkit.UninstallCommand.class},
    description = "Manage Copilot customization agent plugins from the teggr/ai-toolkit repository.")
class AiToolkit implements Runnable {

    static final String OWNER = "teggr";
    static final String REPO = "ai-toolkit";
    static final int MAX_RETRIES = 3;
    static final String ROOT_INSTRUCTIONS_FILE = "instructions.md";
    static final String ROOT_COPILOT_INSTRUCTIONS_FILE = "copilot-instructions.md";
    static final String SPECIFIC_INSTRUCTIONS_SUFFIX = ".instructions.md";
    static final String MCP_FILE = ".mcp.json";

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

    static boolean isRootInstructionsFile(String relativePath) {
        return ROOT_INSTRUCTIONS_FILE.equals(relativePath);
    }

    static boolean isMarkdownFile(String relativePath) {
        Path fileNamePath = Paths.get(relativePath).getFileName();
        if (fileNamePath == null) return false;
        return fileNamePath.toString().toLowerCase(Locale.ROOT).endsWith(".md");
    }

    static boolean isSpecificInstructionsFile(String relativePath) {
        Path fileNamePath = Paths.get(relativePath).getFileName();
        if (fileNamePath == null) return false;
        return fileNamePath.toString().endsWith(SPECIFIC_INSTRUCTIONS_SUFFIX);
    }

    static boolean isMergeInstructionsFile(String relativePath) {
        if (isSpecificInstructionsFile(relativePath)) return false;
        if (isRootInstructionsFile(relativePath)) return true;
        if (!relativePath.startsWith("instructions/")) return false;
        return isMarkdownFile(relativePath);
    }

    static Path mapDestination(Path installRoot, String relativePath) {
        if (!isSpecificInstructionsFile(relativePath)) {
            return installRoot.resolve(relativePath).normalize();
        }

        Path fileNamePath = Paths.get(relativePath).getFileName();
        if (fileNamePath == null) {
            return installRoot.resolve(relativePath).normalize();
        }

        return installRoot.resolve("instructions").resolve(fileNamePath.toString()).normalize();
    }

    static Path rootInstructionsTarget(Path installRoot) {
        Path leaf = installRoot.getFileName();
        String leafName = leaf == null ? "" : leaf.toString().toLowerCase(Locale.ROOT);
        if (".ai".equals(leafName)) {
            return installRoot.resolve(ROOT_INSTRUCTIONS_FILE).normalize();
        }
        return installRoot.resolve(ROOT_COPILOT_INSTRUCTIONS_FILE).normalize();
    }

    static String pluginInstructionSectionTag(String plugin) {
        return plugin + "_instructions";
    }

    static String pluginInstructionSection(String plugin, String instructionContent) {
        String normalized = instructionContent == null ? "" : instructionContent.strip();
        String sectionTag = pluginInstructionSectionTag(plugin);
        return "<" + sectionTag + ">" + System.lineSeparator()
            + normalized + System.lineSeparator()
            + "</" + sectionTag + ">";
    }

    static String combineInstructionFragments(List<InstructionFragment> fragments) {
        if (fragments.isEmpty()) return "";

        if (fragments.size() == 1) {
            return fragments.getFirst().content().strip();
        }

        String lineSeparator = System.lineSeparator();
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < fragments.size(); i++) {
            InstructionFragment fragment = fragments.get(i);
            if (i > 0) {
                combined.append(lineSeparator).append(lineSeparator);
            }
            combined
                .append("<!-- source: ")
                .append(fragment.relativePath())
                .append(" -->")
                .append(lineSeparator)
                .append(fragment.content().strip());
        }
        return combined.toString();
    }

    static void upsertPluginInstructionSection(Path targetFile, String plugin, String instructionContent) throws IOException {
        Files.createDirectories(targetFile.getParent());

        String existing = Files.exists(targetFile)
            ? Files.readString(targetFile, StandardCharsets.UTF_8)
            : "";
        String section = pluginInstructionSection(plugin, instructionContent);
        String updated = replaceOrAppendPluginSection(existing, plugin, section);

        if (!existing.equals(updated)) {
            Files.writeString(targetFile, updated, StandardCharsets.UTF_8);
        }
    }

    static boolean removePluginInstructionSection(Path targetFile, String plugin) throws IOException {
        if (!Files.exists(targetFile)) return false;

        String existing = Files.readString(targetFile, StandardCharsets.UTF_8);
        String sectionTag = pluginInstructionSectionTag(plugin);
        Pattern sectionPattern = Pattern.compile("(?s)\\n?<" + Pattern.quote(sectionTag) + ">\\R?.*?\\R?</" + Pattern.quote(sectionTag) + ">\\n?");
        Matcher matcher = sectionPattern.matcher(existing);
        if (!matcher.find()) return false;

        String updated = matcher.replaceAll("");
        updated = trimExcessLeadingAndTrailingBlankLines(updated);
        Files.writeString(targetFile, updated, StandardCharsets.UTF_8);
        return true;
    }

    static String replaceOrAppendPluginSection(String existingContent, String plugin, String section) {
        String sectionTag = pluginInstructionSectionTag(plugin);
        Pattern sectionPattern = Pattern.compile("(?s)<" + Pattern.quote(sectionTag) + ">\\R?.*?\\R?</" + Pattern.quote(sectionTag) + ">");
        Matcher matcher = sectionPattern.matcher(existingContent);
        if (matcher.find()) {
            String replaced = matcher.replaceFirst(Matcher.quoteReplacement(section));
            return ensureSingleTrailingNewline(replaced);
        }

        StringBuilder out = new StringBuilder(existingContent == null ? "" : existingContent);
        if (out.length() > 0 && !out.toString().endsWith("\n")) {
            out.append(System.lineSeparator());
        }
        if (out.length() > 0 && !out.toString().endsWith(System.lineSeparator() + System.lineSeparator())) {
            out.append(System.lineSeparator());
        }
        out.append(section).append(System.lineSeparator());
        return out.toString();
    }

    static String trimExcessLeadingAndTrailingBlankLines(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceFirst("^\\n+", "");
        normalized = normalized.replaceFirst("\\n+$", "");
        if (normalized.isEmpty()) return "";
        return normalized + System.lineSeparator();
    }

    static String ensureSingleTrailingNewline(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceFirst("\\n+$", "");
        return normalized + System.lineSeparator();
    }

    static boolean isMcpFile(String relativePath) {
        return MCP_FILE.equals(relativePath);
    }

    /** Returns the path where MCP server entries are merged for a given install root.
     * For .github workspace installs the file lives at the workspace root, not inside .github. */
    static Path resolveMcpTarget(Path installRoot) {
        Path leaf = installRoot.getFileName();
        String leafName = leaf == null ? "" : leaf.toString();
        if (".github".equals(leafName)) {
            Path parent = installRoot.getParent();
            if (parent != null) return parent.resolve(MCP_FILE);
        }
        return installRoot.resolve(MCP_FILE);
    }

    /**
     * Finds the index of the matching closing brace or bracket for the opening character at openPos.
     * Returns -1 if not found.
     */
    static int findMatchingBrace(String json, int openPos) {
        int depth = 0;
        boolean inString = false;
        for (int i = openPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /**
     * Extracts named server entries from an mcp.json string.
     * Returns a list of two-element arrays: [serverName, rawJsonObjectValue].
     */
    static List<String[]> extractMcpServerEntries(String mcpJson) {
        List<String[]> entries = new ArrayList<>();
        Pattern serversPattern = Pattern.compile("\"mcpServers\"\\s*:\\s*\\{");
        Matcher sm = serversPattern.matcher(mcpJson);
        if (!sm.find()) return entries;

        int serversStart = sm.end() - 1;
        int serversEnd = findMatchingBrace(mcpJson, serversStart);
        if (serversEnd < 0) return entries;

        String content = mcpJson.substring(serversStart + 1, serversEnd);
        Pattern keyPattern = Pattern.compile("\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*:");
        int scanFrom = 0;
        while (scanFrom < content.length()) {
            Matcher km = keyPattern.matcher(content);
            if (!km.find(scanFrom)) break;
            String key = unescapeJsonString(km.group(1));
            int valueStart = km.end();
            while (valueStart < content.length() && Character.isWhitespace(content.charAt(valueStart))) valueStart++;
            if (valueStart >= content.length() || content.charAt(valueStart) != '{') {
                scanFrom = km.end();
                continue;
            }
            int valueEnd = findMatchingBrace(content, valueStart);
            if (valueEnd < 0) break;
            entries.add(new String[]{key, content.substring(valueStart, valueEnd + 1)});
            scanFrom = valueEnd + 1;
        }
        return entries;
    }

    /**
     * Returns the server names in the given mcp.json string.
     */
    static List<String> extractMcpServerNames(String mcpJson) {
        return extractMcpServerEntries(mcpJson).stream().map(e -> e[0]).toList();
    }

    /** Builds a formatted mcp.json string from a list of [name, rawJsonValue] entries. */
    static String buildMcpJson(List<String[]> entries) {
        String ls = System.lineSeparator();
        if (entries.isEmpty()) {
            return "{" + ls + "  \"mcpServers\": {}" + ls + "}" + ls;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(ls).append("  \"mcpServers\": {").append(ls);
        for (int i = 0; i < entries.size(); i++) {
            sb.append("    \"").append(entries.get(i)[0]).append("\": ").append(entries.get(i)[1]);
            if (i < entries.size() - 1) sb.append(",");
            sb.append(ls);
        }
        sb.append("  }").append(ls).append("}").append(ls);
        return sb.toString();
    }

    /**
     * Merges the given mcp server entries into the target mcp.json file,
     * creating the file if it does not exist. Existing entries with the same
     * name are replaced; new entries are appended.
     */
    static void upsertMcpServerEntries(Path targetFile, List<String[]> newEntries) throws IOException {
        if (newEntries.isEmpty()) return;
        List<String[]> existing = new ArrayList<>();
        if (Files.exists(targetFile)) {
            existing = new ArrayList<>(extractMcpServerEntries(
                Files.readString(targetFile, StandardCharsets.UTF_8)));
        }
        for (String[] newEntry : newEntries) {
            boolean replaced = false;
            for (int i = 0; i < existing.size(); i++) {
                if (existing.get(i)[0].equals(newEntry[0])) {
                    existing.set(i, newEntry);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) existing.add(newEntry);
        }
        Files.createDirectories(targetFile.getParent());
        Files.writeString(targetFile, buildMcpJson(existing), StandardCharsets.UTF_8);
    }

    /**
     * Removes the named server entries from the target mcp.json file.
     * Returns true if any entries were removed.
     */
    static boolean removeMcpServerEntries(Path targetFile, List<String> keysToRemove) throws IOException {
        if (!Files.exists(targetFile) || keysToRemove.isEmpty()) return false;
        List<String[]> existing = new ArrayList<>(extractMcpServerEntries(
            Files.readString(targetFile, StandardCharsets.UTF_8)));
        int before = existing.size();
        existing.removeIf(e -> keysToRemove.contains(e[0]));
        if (existing.size() == before) return false;
        Files.writeString(targetFile, buildMcpJson(existing), StandardCharsets.UTF_8);
        return true;
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
    record InstructionFragment(String relativePath, String content) {}

    // ─── install ──────────────────────────────────────────────────────────────

    @Command(
        name = "install",
        mixinStandardHelpOptions = true,
        description = "Install an agent plugin from teggr/ai-toolkit into .github, .ai, or a custom target.")
    static class InstallCommand implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "<plugin>",
            description = "Agent plugin to install (e.g. discovery).")
        String plugin;

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
            String bundlePrefix = plugin + "/";
            Path installRoot = resolveInstallRoot();
            Files.createDirectories(installRoot);

            BranchTree tree = fetchTree(client, true);
            List<String> files = extractPaths(tree.json(), "blob", bundlePrefix)
                .stream()
                .filter(path -> !path.equals(bundlePrefix + "plugin.json"))
                .filter(path -> !path.equals(bundlePrefix + "README.md"))
                .sorted()
                .toList();

            if (files.isEmpty()) {
                System.err.printf("No files found under %s in %s/%s.%n", bundlePrefix, OWNER, REPO);
                return 1;
            }

            System.out.printf("Installing agent plugin '%s' from %s/%s (%s) into %s%n",
                plugin, OWNER, REPO, tree.branch(), installRoot.toAbsolutePath());
            System.out.printf("Found %d files.%n", files.size());

            int installed = 0, skipped = 0, failed = 0, merged = 0;
            List<InstructionFragment> instructionFragments = new ArrayList<>();
            List<String[]> mcpServerEntries = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                String remotePath = files.get(i);
                String relativePath = remotePath.substring(bundlePrefix.length());

                if (isMergeInstructionsFile(relativePath)) {
                    System.out.printf("[%d/%d] %s -> queue for merged root instructions%n", i + 1, files.size(), remotePath);

                    try {
                        String instructionContent = fetchText(client, rawUrl(tree.branch(), remotePath));
                        instructionFragments.add(new InstructionFragment(relativePath, instructionContent));
                        merged++;
                        System.out.println("  queued for merge");
                    } catch (Exception ex) {
                        failed++;
                        System.err.printf("  failed: %s%n", ex.getMessage());
                    }
                    continue;
                }

                if (isMcpFile(relativePath)) {
                    System.out.printf("[%d/%d] %s -> queue for mcp.json merge%n", i + 1, files.size(), remotePath);
                    try {
                        String mcpContent = fetchText(client, rawUrl(tree.branch(), remotePath));
                        mcpServerEntries.addAll(extractMcpServerEntries(mcpContent));
                        merged++;
                        System.out.println("  queued for merge");
                    } catch (Exception ex) {
                        failed++;
                        System.err.printf("  failed: %s%n", ex.getMessage());
                    }
                    continue;
                }

                Path destination = mapDestination(installRoot, relativePath);

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

            if (!instructionFragments.isEmpty()) {
                Path rootInstructions = rootInstructionsTarget(installRoot);
                System.out.printf("[merge] %d file(s) -> %s%n", instructionFragments.size(), rootInstructions);
                try {
                    String combinedInstructions = combineInstructionFragments(instructionFragments);
                    upsertPluginInstructionSection(rootInstructions, plugin, combinedInstructions);
                    System.out.println("  merged");
                } catch (Exception ex) {
                    failed++;
                    System.err.printf("  failed: %s%n", ex.getMessage());
                }
            }

            if (!mcpServerEntries.isEmpty()) {
                Path targetMcp = resolveMcpTarget(installRoot);
                System.out.printf("[mcp] %d server(s) -> %s%n", mcpServerEntries.size(), targetMcp);
                try {
                    upsertMcpServerEntries(targetMcp, mcpServerEntries);
                    System.out.println("  merged");
                } catch (Exception ex) {
                    failed++;
                    System.err.printf("  failed: %s%n", ex.getMessage());
                }
            }

            System.out.printf("%nSummary: installed=%d merged=%d skipped=%d failed=%d%n", installed, merged, skipped, failed);
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
        description = "Remove a previously installed agent plugin from .github (or a custom target).")
    static class UninstallCommand implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "<plugin>",
            description = "Agent plugin to uninstall (e.g. discovery).")
        String plugin;

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
            String bundlePrefix = plugin + "/";
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
            System.out.printf("Uninstalling agent plugin '%s' from %s%n", plugin, installRoot.toAbsolutePath());
            System.out.printf("Found %d files.%n", files.size());

            int removed = 0, skipped = 0, missing = 0, directoriesRemoved = 0, sectionsRemoved = 0;
            boolean mergedSectionHandled = false;
            boolean mcpEntriesHandled = false;

            for (int i = 0; i < files.size(); i++) {
                String remotePath = files.get(i);
                String relativePath = remotePath.substring(bundlePrefix.length());

                if (isMergeInstructionsFile(relativePath)) {
                    if (mergedSectionHandled) {
                        System.out.printf("[%d/%d] %s%n", i + 1, files.size(), remotePath);
                        System.out.println("  merged instruction source; section removal already handled");
                        continue;
                    }

                    mergedSectionHandled = true;
                    Path rootInstructions = rootInstructionsTarget(installRoot);
                    System.out.printf("[%d/%d] remove merged section from %s%n", i + 1, files.size(), rootInstructions);
                    try {
                        if (removePluginInstructionSection(rootInstructions, plugin)) {
                            sectionsRemoved++;
                            System.out.println("  section removed");
                        } else {
                            missing++;
                            System.out.println("  section not found, skipping");
                        }
                    } catch (IOException ex) {
                        System.err.printf("  failed: %s%n", ex.getMessage());
                    }
                    continue;
                }

                if (isMcpFile(relativePath)) {
                    if (mcpEntriesHandled) {
                        System.out.printf("[%d/%d] %s%n", i + 1, files.size(), remotePath);
                        System.out.println("  mcp entries already removed");
                        continue;
                    }

                    mcpEntriesHandled = true;
                    Path targetMcp = resolveMcpTarget(installRoot);
                    System.out.printf("[%d/%d] remove mcp entries from %s%n", i + 1, files.size(), targetMcp);
                    try {
                        String pluginMcpContent = fetchText(client, rawUrl(tree.branch(), remotePath));
                        List<String> serverNames = extractMcpServerNames(pluginMcpContent);
                        if (removeMcpServerEntries(targetMcp, serverNames)) {
                            sectionsRemoved++;
                            System.out.println("  mcp entries removed");
                        } else {
                            missing++;
                            System.out.println("  mcp entries not found, skipping");
                        }
                    } catch (Exception ex) {
                        System.err.printf("  failed: %s%n", ex.getMessage());
                    }
                    continue;
                }

                Path target = mapDestination(installRoot, relativePath);

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

            System.out.printf("%nSummary: removed=%d sections-removed=%d skipped=%d not-found=%d directories-removed=%d%n",
                removed, sectionsRemoved, skipped, missing, directoriesRemoved);
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
        description = "List available agent plugins in the teggr/ai-toolkit repository.")
    static class ListCommand implements Callable<Integer> {

        private final HttpClient client = newHttpClient();

        @Override
        public Integer call() throws Exception {
            BranchTree tree = fetchTree(client, true);
            List<String> manifests = extractPaths(tree.json(), "blob", null)
                .stream()
                // A valid installable agent plugin must have a top-level plugin.json manifest.
                .filter(p -> p.endsWith("/plugin.json") && p.indexOf('/') == p.lastIndexOf('/'))
                .sorted()
                .toList();

            if (manifests.isEmpty()) {
                System.out.println("No agent plugins found.");
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

            System.out.printf("Available agent plugins in %s/%s (%s):%n", OWNER, REPO, tree.branch());
            for (String manifestPath : manifests) {
                String plugin = manifestPath.substring(0, manifestPath.indexOf('/'));
                String description = "(no description)";
                try {
                    String manifestJson = fetchText(client, rawUrl(tree.branch(), manifestPath));
                    String parsed = extractJsonStringField(manifestJson, "description");
                    if (parsed != null && !parsed.isBlank()) {
                        description = parsed.trim();
                    }
                } catch (Exception ignored) {
                    // Keep listing agent plugins even if one manifest cannot be read.
                }
                printListEntry(plugin, description, nameColumnWidth, descriptionWidth);
            }
            return 0;
        }
    }

}

