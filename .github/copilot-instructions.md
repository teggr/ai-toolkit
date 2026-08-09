# Copilot instructions for this repository

This repository is a curated toolkit of Copilot customization resources (skills, agents, and custom instructions).

Prompt files (`*.prompt.md`) are legacy and should not be added for new work. Use skills for slash-command-style behavior and keep resources compatible across harnesses.

## Repository structure rules
- Organize content by **purpose first**, then by **resource type**.
- Use this path pattern: `/<purpose>/<type>/...`
- Resource types should generally be one of: `agents`, `skills`, `instructions`.
- Do not introduce new `prompts/` directories or `*.prompt.md` files. If prompt content exists, migrate it into `skills/`.

## Content rules
- Keep files practical and reusable.
- Prefer linking to external maintained sources when appropriate.
- If content is copied from an external source, include the source URL at the top of the file.
- Keep names descriptive and consistent.
- When changing install or setup instructions, keep the guidance synchronized in both the threadme install documentation and the website install docs in [docs/install.html](../docs/install.html), so users see the same steps everywhere.
- New or updated Agent Plugins must follow the canonical Agent Plugins layout from the VS Code documentation: https://code.visualstudio.com/docs/agent-customization/agent-plugins
- Required layout baseline: a root `plugin.json` manifest with the Agent Plugins schema, skills under `skills/`, and any additional portable components in the locations the canonical spec expects. Do not introduce non-standard plugin layouts for new Agent Plugins.
- Agent plugin instruction files are split into two types:
	- General instruction files are merged into the install root instruction file:
		- `instructions.md` at plugin root
		- any Markdown file under plugin `instructions/` that does **not** end with `*.instructions.md`
	- Any `*.instructions.md` file contains specific rules and should be installed as a standalone file under the target `instructions/` folder.
- During install, merge all general instruction files into the root instruction file using a plugin-tagged section format:
	- `<plugin_name_instructions>...content...</plugin_name_instructions>` where `plugin_name` is the actual plugin id (for example, `discovery_instructions`).
	- This section is replaceable on re-install and removable on uninstall.
- Merge target selection rule:
	- Install root `.ai` merges into `instructions.md`.
	- All other install roots (including `.github`, `~/.copilot`, and custom targets) merge into `copilot-instructions.md`.

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
