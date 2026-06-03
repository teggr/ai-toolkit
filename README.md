# ai-toolkit

**🔗 https://teggr.github.io/ai-toolkit/**

A collection of Copilot customization resources including prompts, skills, agents, and custom instructions.

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
- `discovery/prompts/`
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

## References

- https://docs.github.com/en/copilot
- https://awesome-copilot.github.com/skills/
