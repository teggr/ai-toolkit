---
name: AI Toolkit
description: Optional post-install Agent plugin manager for teggr/ai-toolkit, focused on inventory, updates, and reconciliation.
---

This Agent plugin is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package.

It is **not** required to bootstrap installs from this repository. First-time Agent plugin installs should use `AiToolkit.java` directly from GitHub or from a local clone.

Install this Agent plugin when you want an in-editor manager for already-available toolkit workflows such as inventory, update, and drift reconciliation.

This Agent plugin includes:

- `skills/ai-toolkit/SKILL.md` — an invocable skill for inspecting installed toolkit-managed resources and managing updates or drift after bootstrap.
- `skills/ai-toolkit/docs/docs.md` — detailed workflows for bootstrap commands, install roots, installed-Agent plugin detection, and safe update paths.
