# Copilot instructions for this repository

This repository is a curated toolkit of Copilot customization resources (skills, agents, and custom instructions).

Prompt files (`*.prompt.md`) are legacy and should not be added for new work. Use skills for slash-command-style behavior and keep resources compatible across harnesses.

## Repository structure rules
- Organize content by **purpose first**, then by **resource type**.
- Use this path pattern: `/<purpose>/<type>/...`
- Resource types should generally be one of: `agents`, `skills`, `instructions`.
- Do not introduce new `prompts/` directories or `*.prompt.md` files. If prompt content exists, migrate it into `skills/`.
- Every top-level agent plugin folder must include a populated `README.md`.

## Content rules
- Keep files practical and reusable.
- Prefer linking to external maintained sources when appropriate.
- If content is copied from an external source, include the source URL at the top of the file.
- Keep names descriptive and consistent.
- Each agent plugin `README.md` must include YAML frontmatter with `name` and `description`.
- The body of each agent plugin `README.md` should describe what the agent plugin includes.
- When adding or updating an agent plugin, always add or update its `README.md` so the agent plugin metadata stays complete.
- When changing install or setup instructions, keep the guidance synchronized in both the threadme install documentation and the website install docs in [docs/install.html](../docs/install.html), so users see the same steps everywhere.
- New or updated Agent Plugins must follow the canonical Agent Plugins layout from the VS Code documentation: https://code.visualstudio.com/docs/agent-customization/agent-plugins
- Required layout baseline: a root `plugin.json` manifest with the Agent Plugins schema, skills under `skills/`, and any additional portable components in the locations the canonical spec expects. Do not introduce non-standard plugin layouts for new Agent Plugins.

## Installing Agent Plugins (for AI agents)

When a user asks you to install an agent plugin from this toolkit, use the JBang installer directly from the GitHub-hosted `AiToolkit.java` URL — no cloning required.

**List available Agent Plugins:**
```
jbang https://github.com/teggr/ai-toolkit/blob/main/AiToolkit.java list
```

**Install an agent plugin into the current repository (`.github/`):**
```
jbang https://github.com/teggr/ai-toolkit/blob/main/AiToolkit.java install <agent-plugin>
```

**Install globally into `~/.copilot`:**
```
jbang https://github.com/teggr/ai-toolkit/blob/main/AiToolkit.java install --global <agent-plugin>
```

**Uninstall an agent plugin:**
```
jbang https://github.com/teggr/ai-toolkit/blob/main/AiToolkit.java uninstall <agent-plugin>
```

Use `--force` to skip prompts. Use `--target <dir>` to specify a custom install directory. The installer requires Java 21+ and JBang. If neither is available, instruct the user to follow the [install guide](https://teggr.github.io/ai-toolkit/install.html) manually.

## Contribution expectations
- Make focused changes with minimal unrelated edits.
- Update `README.md` when adding new top-level purposes or key references.
