---
name: PR Visualization
description: agent plugin for visualizing pull request age distribution using the Copilot Java SDK and JBang.
---

This agent plugin is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. You can install it directly in VS Code by pointing the plugin installer at this directory.

This agent plugin includes:

- `skills/pr-visualization/SKILL.md` — an invocable VS Code skill that helps you run the PR age chart generator, interpret its output, and ask follow-up questions about PR health.
- `skills/pr-visualization/docs/docs.md` — full reference for installation prerequisites, usage patterns, and how the script works.
- `skills/pr-visualization/scripts/PRVisualization.java` — the JBang script that uses the Copilot Java SDK and generates an interactive PR age distribution chart for any GitHub repository.
