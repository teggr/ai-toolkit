---
name: Review
description: Agent plugin for reviewing agent-generated pull requests using a structured, ordered checklist based on GitHub's guide to agent PR review.
---

This Agent plugin is a valid [Agent Plugin 1.0](https://agent-plugins.org/) package. You can install it directly in VS Code by pointing the plugin installer at this directory.

This Agent plugin includes:

- `skills/agent-pr-review/SKILL.md` — an invocable VS Code skill that guides you through the six-step ordered review process for agent-generated pull requests, covering CI integrity, code reuse, critical-path tracing, security boundaries, and evidence requirements.
- `skills/agent-pr-review/docs/docs.md` — full reference for the review checklist, red flags, and when to request a smaller pull request.
- `instructions/agent-pr-review.md` — Copilot code review custom instructions you can drop into `.github/copilot-instructions.md` or configure in GitHub repository settings to automate the mechanical checks on every pull request.
