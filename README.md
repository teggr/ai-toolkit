# ai-toolkit

**🔗 https://teggr.github.io/ai-toolkit/**

A collection of Copilot customization resources focused on skills, agents, and custom instructions.

> Prompt files (`*.prompt.md`) are considered legacy in this repository. For cross-harness compatibility, create and maintain **skills** instead of prompts.

## Install

### Option A — jbang from GitHub (no clone required)

Run the installer directly from the GitHub-hosted `AiToolkit.java` URL with JBang. No cloning needed.

```bash
jbang https://github.com/teggr/ai-toolkit/AiToolkit.java list
jbang https://github.com/teggr/ai-toolkit/AiToolkit.java install discovery
```

### Option B — clone with git

Clone the repository, then use the embedded install guidance to pick and install bundles into your own environment.

```bash
git clone https://github.com/teggr/ai-toolkit
cd ai-toolkit
jbang AiToolkit.java list
jbang AiToolkit.java install discovery
```

### Option C — raw files via curl

Download the raw files only if you specifically want the optional `ai-toolkit` manager skill without cloning the repository. This is not the normal bootstrap path for installing bundles.

```bash
curl -L -O https://raw.githubusercontent.com/teggr/ai-toolkit/main/ai-toolkit/skills/ai-toolkit/SKILL.md
curl -L -O https://raw.githubusercontent.com/teggr/ai-toolkit/main/ai-toolkit/skills/ai-toolkit/docs/docs.md
```

---

### `list` — Show available bundles

```bash
jbang AiToolkit.java list
```

### `install <bundle>` — Install a bundle

```bash
jbang AiToolkit.java install [--target <dir> | --global] [--force] <bundle>
```

Options:

- `--target <dir>` — Target install directory (default: `./.github`)
- `--global` — Install into `~/.copilot` (overrides `--target`)
- `--force` — Overwrite existing files without prompting

Examples:

```bash
jbang AiToolkit.java install discovery
jbang AiToolkit.java install --target /tmp/copilot-resources discovery
jbang AiToolkit.java install --global --force discovery
```

### `uninstall <bundle>` — Remove a bundle


```bash
jbang AiToolkit.java uninstall [--target <dir> | --global] [--force] <bundle>
```

Options:

- `--target <dir>` — Install directory to remove from (default: `./.github`)
- `--global` — Remove from `~/.copilot` (overrides `--target`)
- `--force` — Delete files without prompting

Examples:

```bash
jbang AiToolkit.java uninstall discovery
jbang AiToolkit.java uninstall --global --force discovery
```

### `plugin package <bundle>` — Package a bundle as an Agent Plugin

Downloads all files for a bundle into a local directory and creates a `.zip` archive suitable for manual VS Code installation or marketplace submission.

```bash
jbang AiToolkit.java plugin package [--output <dir>] <bundle>
```

Options:

- `--output <dir>` — Output directory (default: `./ai-toolkit-plugins`)

Examples:

```bash
jbang AiToolkit.java plugin package discovery
jbang AiToolkit.java plugin package --output /tmp/plugins review
```

### `plugin validate <path>` — Validate a local plugin directory

Checks that a local directory is a valid Agent Plugin 1.0 package: verifies `plugin.json` is present, the `$schema` is correct, and the `skills/` layout is well-formed.

```bash
jbang AiToolkit.java plugin validate <path>
```

Example:

```bash
jbang AiToolkit.java plugin validate ./discovery
```

### Run directly from GitHub

You can run the script without cloning the repository by passing the GitHub-hosted `AiToolkit.java` URL to JBang:

```bash
jbang https://github.com/teggr/ai-toolkit/AiToolkit.java list
jbang https://github.com/teggr/ai-toolkit/AiToolkit.java install discovery
```

For help:

```bash
jbang https://github.com/teggr/ai-toolkit/AiToolkit.java --help
```

## Plugin format

Every bundle in this repository is also a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. Each bundle directory contains a `plugin.json` manifest that declares the canonical schema, so plugins work across GitHub Copilot in VS Code, GitHub Copilot CLI, and the GitHub Copilot app.

```json
{
  "$schema": "https://agent-plugins.org/schemas/1.0.0/plugin.schema.json",
  "name": "review",
  "description": "...",
  "version": "1.0.0"
}
```

Skills are auto-discovered from the `skills/` folder — no explicit listing in the manifest is needed. The `skills/<skill-name>/SKILL.md` layout already matches the Agent Plugin spec.

To install a bundle as a VS Code plugin, use `plugin package` to download and zip it, then install the zip via VS Code's plugin marketplace UI, or point VS Code at the unpacked directory.

See the [VS Code agent plugins documentation](https://code.visualstudio.com/docs/agent-customization/agent-plugins) and the [Agent Plugins open standard](https://agent-plugins.org/) for details.

## Packaging

Each bundle is dual-purpose:

| Usage | How |
|---|---|
| JBang toolkit install | `jbang AiToolkit.java install <bundle>` copies files into `.github` or `~/.copilot` or `.ai` |
| Agent Plugin 1.0 package | `jbang AiToolkit.java plugin package <bundle>` downloads and zips the bundle for VS Code plugin install |

## Starter structure

```bash
jbang AiToolkit.java --help
```

## Bundles

Resources are organized by **purpose** and then by **type** (`agents/`, `skills/`, `instructions/`).

| Bundle | Type | Resource | Description |
|---|---|---|---|
| `discovery` | agent | `agents/discovery.md` | Discovery agent — clarifies goals, scope, dependencies, risks, and unknowns before planning |
| `review` | skill | `skills/agent-pr-review/SKILL.md` | Invocable skill — six-step structured review for agent-generated PRs (CI, reuse, security, evidence) |
| `review` | instructions | `instructions/agent-pr-review.md` | Copilot code review instructions — drop-in mechanical checks for every PR |
| `deploy4j` | skill | `skills/deploy4j/SKILL.md` | Reference skill — deploy4j CLI install, init, config/secrets, setup, and deploy workflows |
| `ai-toolkit` | skill | `skills/ai-toolkit/SKILL.md` | Optional manager skill — inspect, update, and reconcile toolkit-managed resources after bootstrap |
| `screaming-architecture` | skill | `skills/screaming-architecture/SKILL.md` | Invocable skill — design and review package-by-feature architecture (Screaming Architecture) |
| `screaming-architecture` | instructions | `instructions/screaming-architecture.md` | Copilot agent instructions — enforce feature-first architecture review with weighted scorecard |
| `spring-htmx` | skill | `skills/spring-htmx-skill.md` | Reference skill — htmx-spring-boot library (Maven, headers, fragments, Security, Thymeleaf) |
| `spring-j2html` | skill | `skills/spring-j2html-skill.md` | Reference skill — j2html type-safe HTML builder with Spring Boot |

### Detail

#### spring-htmx

**Path:** `spring-htmx/skills/spring-htmx-skill.md`

Reference skill for using the [htmx-spring-boot](https://github.com/wimdeblauwe/htmx-spring-boot) library. Covers Maven configuration, request/response headers, HTML fragments, Spring Security integration, and the Thymeleaf dialect. Not invocable — use as context when working with Spring Boot + htmx projects.

## Review

#### agent-pr-review

**Path:** `review/skills/agent-pr-review/SKILL.md`

An invocable VS Code skill for reviewing agent-generated pull requests. Guides through the six ordered steps from [GitHub's agent PR review guide](https://github.blog/ai-and-ml/generative-ai/agent-pull-requests-are-everywhere-heres-how-to-review-them/): CI integrity, code reuse, critical-path tracing, security boundaries, and evidence. Produces a structured review report with BLOCKER / WARN / PASS findings.

**Path:** `review/instructions/agent-pr-review.md`

Copilot code review custom instructions that automate the mechanical checks on every pull request. Drop into `.github/copilot-instructions.md` or add via GitHub Repository Settings → Copilot → Code review → Custom instructions.


## Deploy4j

#### deploy4j

**Path:** `deploy4j/skills/deploy4j/SKILL.md`

Reference skill for [deploy4j](https://deploy4j.dev/) installation and operations. Covers CLI-first discovery of command usage, project initialization, config/secrets setup, first-time `setup`, and repeatable `deploy` workflows, with fallback to website docs when needed.

## AI Toolkit

#### ai-toolkit

**Path:** `ai-toolkit/skills/ai-toolkit/SKILL.md`

An invocable skill for managing toolkit-installed resources after bootstrap: inspect what bundles are present in local (`.github`) or global (`~/.copilot`) roots, update installed bundles, and reconcile local files with remote bundle content.

It is optional and not part of the bootstrap path. First-time installs should go through `AiToolkit.java` directly.

## Architecture

#### screaming-architecture

**Path:** `screaming-architecture/skills/screaming-architecture/SKILL.md`

An invocable skill for designing, creating, and reviewing architecture with package-by-feature principles inspired by Screaming Architecture. It helps ensure package/module names communicate business capabilities first, with technical details nested beneath feature slices.

**Path:** `screaming-architecture/instructions/screaming-architecture.md`

Copilot agent instructions that reference the `screaming-architecture` skill and enforce feature-first architecture review outputs, including the weighted scorecard.

## Spring j2html

#### spring-j2html

**Path:** `spring-j2html/skills/j2html-extensions/SKILL.md`

Reference skill for working with the [j2html-extensions](https://github.com/teggr/j2html-extensions) library in Spring Boot applications. Covers Maven setup, the Spring Boot starter, core extensions, Bootstrap classes, HTMX attributes, and the j2html template engine. Not invocable — use as context when building type-safe HTML views with Spring Boot.

## References

- https://docs.github.com/en/copilot
- https://awesome-copilot.github.com/skills/
