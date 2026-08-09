---
name: PR Visualization
description: Bundle for visualizing pull request age distribution using the Copilot Java SDK and JBang.
---

This bundle is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. You can install it directly in VS Code by pointing the plugin installer at this directory, or use `jbang AiToolkit.java plugin package pr-visualization` to download and zip it.

This bundle includes:

- `skills/pr-visualization/SKILL.md` — an invocable VS Code skill that helps you run the PR age chart generator, interpret its output, and ask follow-up questions about PR health.
- `skills/pr-visualization/docs/docs.md` — full reference for installation prerequisites, usage patterns, and how the script works.
- `skills/pr-visualization/scripts/PRVisualization.java` — the JBang script that bundles the Copilot Java SDK and generates an interactive PR age distribution chart for any GitHub repository.
