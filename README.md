# ai-toolkit

**🔗 https://teggr.github.io/ai-toolkit/**

A collection of Copilot customization resources focused on skills, agents, and custom instructions.

> Prompt files (`*.prompt.md`) are considered legacy in this repository. For cross-harness compatibility, create and maintain **skills** instead of prompts.

## CLI installer

Use the JBang script `AiToolkit.java` to manage Copilot customization bundles from this repository.

```bash
jbang AiToolkit.java <command> [options]
```

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

### Run directly from GitHub

You can run the script without cloning the repository by passing the raw GitHub URL to JBang:

```bash
jbang https://raw.githubusercontent.com/teggr/ai-toolkit/main/AiToolkit.java list
jbang https://raw.githubusercontent.com/teggr/ai-toolkit/main/AiToolkit.java install discovery
```

For help:

```bash
jbang https://raw.githubusercontent.com/teggr/ai-toolkit/main/AiToolkit.java --help
```

## Starter structure

Resources are organized by **purpose** and then by **type**.

Example:

- `discovery/agents/discovery.md`
- `discovery/skills/`
- `discovery/instructions/`

## Skills

### spring-htmx-skill

**Path:** `spring-htmx/skills/spring-htmx-skill.md`

Reference skill for using the [htmx-spring-boot](https://github.com/wimdeblauwe/htmx-spring-boot) library. Covers Maven configuration, request/response headers, HTML fragments, Spring Security integration, and the Thymeleaf dialect. Not invocable — use as context when working with Spring Boot + htmx projects.

## Review

### agent-pr-review skill

**Path:** `review/skills/agent-pr-review/SKILL.md`

An invocable VS Code skill for reviewing agent-generated pull requests. Guides through the six ordered steps from [GitHub's agent PR review guide](https://github.blog/ai-and-ml/generative-ai/agent-pull-requests-are-everywhere-heres-how-to-review-them/): CI integrity, code reuse, critical-path tracing, security boundaries, and evidence. Produces a structured review report with BLOCKER / WARN / PASS findings.

**Path:** `review/instructions/agent-pr-review.md`

Copilot code review custom instructions that automate the mechanical checks on every pull request. Drop into `.github/copilot-instructions.md` or add via GitHub Repository Settings → Copilot → Code review → Custom instructions.


## Deploy4j

### deploy4j skill

**Path:** `deploy4j/skills/deploy4j/SKILL.md`

Reference skill for [deploy4j](https://deploy4j.dev/) installation and operations. Covers CLI-first discovery of command usage, project initialization, config/secrets setup, first-time `setup`, and repeatable `deploy` workflows, with fallback to website docs when needed.

## AI Toolkit

### ai-toolkit skill

**Path:** `ai-toolkit/skills/ai-toolkit/SKILL.md`

An invocable skill for managing this toolkit's bundles end-to-end: discover available bundles, detect installed bundles in local (`.github`) or global (`~/.copilot`) roots, install/update bundles, and reconcile local files with remote bundle content.

## Architecture

### screaming-architecture skill

**Path:** `screaming-architecture/skills/screaming-architecture/SKILL.md`

An invocable skill for designing, creating, and reviewing architecture with package-by-feature principles inspired by Screaming Architecture. It helps ensure package/module names communicate business capabilities first, with technical details nested beneath feature slices.

**Path:** `screaming-architecture/instructions/screaming-architecture.md`

Copilot agent instructions that reference the `screaming-architecture` skill and enforce feature-first architecture review outputs, including the weighted scorecard.

## References

- https://docs.github.com/en/copilot
- https://awesome-copilot.github.com/skills/
