---
name: show-me
description: A visual explanation skill that replaces walls of prose with diagrams, sketches, and focused HTML artifacts.
resource_type: skill
recommended: true
canonical_url: https://github.com/humanlayer/skills/blob/main/plugins/show-me/skills/show-me/SKILL.md
repository_url: https://github.com/humanlayer/skills
source_owner: humanlayer
source_repo: skills
source_branch: main
source_path: plugins/show-me/skills/show-me
install_path: skills/show-me
keywords: recommended, external, visualization
---

# show-me

A recommended external skill from [humanlayer/skills](https://github.com/humanlayer/skills). It asks the agent to explain the current topic visually with compact call trees, diagrams, file trees, pseudocode, diffs, or focused HTML artifacts.

## Canonical reference

- Skill definition: <https://github.com/humanlayer/skills/blob/main/plugins/show-me/skills/show-me/SKILL.md>
- Source repository: <https://github.com/humanlayer/skills>
- Background article: <https://www.humanlayer.com/blog/show-me-skill>

## Install from the source project

HumanLayer publishes `show-me` through its own installer:

```bash
npx skills add humanlayer/skills --skill show-me
```

## Install with ai-toolkit

Use ai-toolkit when you want to install the upstream files into the same local or global roots used for the toolkit's own resources.

```bash
jbang https://github.com/teggr/ai-toolkit/blob/main/AiToolkit.java install show-me
```

That installs the upstream files from `plugins/show-me/skills/show-me` into your target skill directory.

## Invoke

```text
/show-me
```
