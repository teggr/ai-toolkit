---
name: Copilot Java
description: Bundle for configuring the GitHub Copilot coding agent to use Java 25 (or any JDK version) via a copilot-setup-steps.yml workflow, covering Maven, Gradle, private repositories, and firewall allowlist configuration.
---

This bundle is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. You can install it directly in VS Code by pointing the plugin installer at this directory, or use `jbang AiToolkit.java plugin package copilot-java` to download and zip it.

This bundle includes:

- `skills/copilot-java/SKILL.md` — an invocable skill for creating or updating `.github/workflows/copilot-setup-steps.yml` so the Copilot coding agent uses the correct JDK version.
- `skills/copilot-java/docs/docs.md` — detailed workflow guide covering Maven and Gradle configurations, private dependency authentication, firewall allowlist setup, and validation tips.
