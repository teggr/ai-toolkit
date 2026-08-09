# PR Visualization — Reference Docs

> Source: https://github.github.com/copilot-sdk-java/1.0.8/cookbook/pr-visualization.html

This document is the full reference for the `pr-visualization` skill. It covers prerequisites, usage patterns, and a walkthrough of how the JBang script works.

---

## Prerequisites

### JBang

[JBang](https://www.jbang.dev/) is required to run the script. It handles dependency resolution (including the Copilot Java SDK) without requiring a Maven or Gradle build.

```bash
# macOS (Homebrew)
brew install jbangdev/tap/jbang

# Linux / macOS (curl)
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Windows (Scoop)
scoop install jbang
```

### GitHub Copilot CLI

The script uses the Copilot Java SDK (`com.github:copilot-sdk-java:1.0.8`), which in turn connects to the locally installed Copilot CLI. Ensure you are authenticated:

```bash
gh copilot --version   # verify the Copilot CLI extension is installed
gh auth status         # verify GitHub authentication
```

---

## Usage

```bash
# Auto-detect from current git repo
jbang PRVisualization.java

# Specify a repo explicitly
jbang PRVisualization.java owner/repo
```

The script prints progress to stdout and saves the generated chart to `pr-age-chart.png` in the current working directory.

---

## How the script works

### 1. Repository detection

The script resolves the target repository in order:

1. **CLI argument** — use `owner/repo` if provided.
2. **Git remote auto-detection** — if the current directory is a git repository, parse `origin` for a GitHub SSH or HTTPS remote and extract `owner/repo`.
3. **Interactive prompt** — if neither is available, prompt the user.

Exits with a clear error if the resolved value is not in `owner/repo` format.

### 2. Copilot session setup

A `CopilotClient` session is started with:
- **Model:** `gpt-5`
- **Permission handler:** `APPROVE_ALL` (automatically approves tool use)
- **System message:** sets the repository context and instructs Copilot to use the GitHub MCP Server for data fetching and built-in tools for chart generation

### 3. Initial analysis prompt

The script sends a structured prompt asking Copilot to:
- Fetch open pull requests from the last week via the GitHub MCP Server
- Calculate age in days for each PR
- Generate a bar chart (`pr-age-chart.png`) bucketed into sensible age ranges
- Summarize PR health: average age, oldest PR, stale PR count

Copilot handles all the implementation details — data fetching, chart rendering, and bucketing — using its built-in tools.

### 4. Interactive session

After the initial analysis, the script enters an interactive loop. The user can type follow-up questions or refinements such as:

- `"Expand to the last month"`
- `"Show me the 5 oldest PRs"`
- `"Generate a pie chart instead"`
- `"Group by author instead of age"`

Type `exit` or `quit` to end the session.

---

## Why use built-in Copilot tools instead of custom tools?

The script intentionally avoids custom tool implementations and delegates entirely to Copilot's built-in capabilities:

| Aspect         | Custom tools          | Built-in Copilot                  |
|----------------|-----------------------|-----------------------------------|
| Code complexity | High                 | **Minimal**                       |
| Maintenance     | You maintain         | **Copilot maintains**             |
| Flexibility     | Fixed logic          | **AI decides best approach**      |
| Chart types     | What you coded       | **Any type Copilot can generate** |
| Data grouping   | Hardcoded buckets    | **Intelligent grouping**          |

This keeps the script short and future-proof: improvements to Copilot's GitHub MCP Server integration or code-execution tools automatically benefit the script without requiring changes.
