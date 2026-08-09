---
name: AI Toolkit
description: Bundle for managing Copilot customization bundles from the teggr/ai-toolkit repository, including install, inventory, and update workflows.
---

This bundle is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. You can install it directly in VS Code by pointing the plugin installer at this directory, or use `jbang AiToolkit.java plugin package ai-toolkit` to download and zip it.

This bundle includes:

- `skills/ai-toolkit/SKILL.md` — an invocable skill for discovering available bundles, identifying installed bundles, installing into local/global targets, and updating bundle files from remote.
- `skills/ai-toolkit/docs/docs.md` — detailed workflows for local and remote bundle management, including install roots, installed-bundle detection, and safe update paths.
