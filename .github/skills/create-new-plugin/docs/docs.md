# create-new-plugin docs

This skill is for repository-internal scaffolding only. It creates and validates source structure for this repo and does not produce publishable artifacts.

Canonical source for Agent plugins:

- https://code.visualstudio.com/docs/agent-customization/agent-plugins
- https://agent-plugins.org/

## Standard Scaffold

Use this tree for new agent plugins:

<plugin-name>/
- plugin.json
- README.md
- skills/
  - <skill-name>/
    - SKILL.md
    - docs/
      - docs.md

Optional as needed:

- agents/
- instructions/

Do not add prompts/ for new agent plugins.

Do not add packaging/release instructions (zip creation, marketplace submission, or artifact publishing).

## Template: plugin.json

{
  "$schema": "https://agent-plugins.org/schemas/1.0.0/plugin.schema.json",
  "name": "<plugin-name>",
  "description": "<one sentence purpose>",
  "version": "1.0.0",
  "homepage": "https://teggr.github.io/ai-toolkit/",
  "repository": "https://github.com/teggr/ai-toolkit"
}

## Template: README.md Frontmatter

---
name: <Display Name>
description: <one sentence purpose>
---

## Template: SKILL.md Frontmatter

---
name: <skill-name>
description: '<what and when to use with trigger keywords>'
argument-hint: '<optional invocation hint>'
user-invocable: true
---

## Validation Checklist

1. Folder names and frontmatter name fields are consistent.
2. plugin.json uses the canonical schema URL.
3. README.md has name and description frontmatter.
4. SKILL.md includes step-by-step procedure and trigger-rich description.
5. Relative path references are valid.
6. Workflow output remains source-only (no publishable artifact steps).
