---
name: Copilot Java
description: Agent plugin for configuring the GitHub Copilot coding agent to use Java 25 (or any JDK version) via a copilot-setup-steps.yml workflow, covering Maven, Gradle, private repositories, and firewall allowlist configuration.
---

This Agent plugin is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. You can install it directly in VS Code by pointing the plugin installer at this directory.

This Agent plugin includes:

- `skills/copilot-java/SKILL.md` — an invocable skill for creating or updating `.github/workflows/copilot-setup-steps.yml` so the Copilot coding agent uses the correct JDK version.
- `skills/copilot-java/docs/docs.md` — detailed workflow guide covering Maven and Gradle configurations, private dependency authentication, firewall allowlist setup, and validation tips.
