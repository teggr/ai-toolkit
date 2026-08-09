---
name: create-new-plugin
description: 'Create a new repository-local agent plugin in this repo with canonical folder structure and valid files. Use for scaffold plugin, create new agent plugin, add plugin.json, add README, and initialize skills folders.'
argument-hint: 'Provide plugin name and purpose, for example: "name=create-new-plugin-helper purpose=scaffold workflow agent plugins"'
user-invocable: true
---

# Create New Plugin

Create a new top-level agent plugin for this repository that follows the canonical agent plugin layout and this repository's conventions.

This workflow is internal to the repository. It scaffolds source files only and does not create publishable artifacts.

Canonical source:

- https://code.visualstudio.com/docs/agent-customization/agent-plugins

Use [docs/docs.md](docs/docs.md) for templates and checks.

## When to Use

- Create a new agent plugin in this repository.
- Add a reusable skill with plugin manifest and agent plugin metadata.
- Standardize folder and file structure before adding custom logic.
- Prepare internal source agent plugins that can be installed locally from directory paths.

## Inputs

Collect or infer:

1. plugin name (lowercase kebab-case)
2. purpose summary (one sentence)
3. primary resource types needed: skills, agents, instructions
4. whether the skill should be slash-invocable

If inputs are missing, ask for them before creating files.

## Procedure

1. Validate naming and scope.
- Name must be lowercase, kebab-case, and reused consistently.
- Agent plugin path is <repo>/<plugin-name>/.

2. Create canonical agent plugin structure.
- Required files:
  - plugin.json
  - README.md with YAML frontmatter name and description
- Required base folders:
  - skills/
- If this plugin includes a primary skill:
  - skills/<plugin-name>/SKILL.md
  - skills/<plugin-name>/docs/docs.md

3. Write plugin.json.
- Use schema https://agent-plugins.org/schemas/1.0.0/plugin.schema.json.
- Include name, description, version.
- Include homepage and repository when available.

4. Write agent plugin README.md.
- Include YAML frontmatter fields: name, description.
- Include canonical source reference link.
- List included resources and paths.

5. Write SKILL.md.
- Frontmatter name must match folder name.
- Description must include explicit trigger phrases.
- Include concrete step-by-step workflow.
- Reference docs using relative path: [docs/docs.md](docs/docs.md).

6. Add docs/docs.md.
- Include scaffold tree.
- Include copy/paste templates for plugin.json, README.md frontmatter, and SKILL frontmatter.
- Include quality checks.

7. Run quality checks.
- Verify file/folder layout exists.
- Verify SKILL frontmatter name matches skill folder.
- Verify README frontmatter is present and meaningful.
- Verify plugin.json schema and required fields.
- Do not produce zip archives, release artifacts, or marketplace packaging steps.

## Branching Rules

- If only skills are needed: create skills structure only.
- If agents are needed: add agents/ and include at least one agent file.
- If instructions are needed: add instructions/ with clear, scoped files.
- Do not add prompts/ for new work in this repository.

## Completion Criteria

- New top-level agent plugin exists and is self-contained.
- plugin.json is valid and references canonical schema.
- README.md has frontmatter and resource inventory.
- SKILL.md is invocable and procedural.
- Validation checks pass or issues are clearly reported.
- Output is repository source layout only; no publish artifact workflow is generated.
