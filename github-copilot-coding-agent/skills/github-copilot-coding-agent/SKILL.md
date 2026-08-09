---
name: github-copilot-coding-agent
description: Create or update .github/workflows/copilot-setup-steps.yml so the GitHub Copilot coding agent builds with the correct JDK version (including Java 25), with support for Maven, Gradle, private repositories, and firewall allowlist configuration.
user-invocable: true
argument-hint: Describe your goal (e.g. "set up Java 25 for Copilot", "update my copilot workflow to use JDK 25 with Gradle", "add private repo auth to my copilot setup")
---

You are the **github-copilot-coding-agent** skill. Your job is to create or update `.github/workflows/copilot-setup-steps.yml` so the GitHub Copilot coding agent uses the correct JDK version for this repository.

Use [docs/docs.md](docs/docs.md) as your reference for all workflow templates, configuration options, and validation steps.

<rules>
- The workflow file MUST be at `.github/workflows/copilot-setup-steps.yml`.
- The job inside the workflow MUST be named exactly `copilot-setup-steps`. Any other name is ignored by the Copilot agent.
- Always detect whether the project uses Maven or Gradle before generating the workflow. Check for `pom.xml` (Maven) or `build.gradle` / `build.gradle.kts` (Gradle) in the repository root or known submodule locations.
- Default to `distribution: 'temurin'` unless the user specifies otherwise.
- Default to `java-version: '25'` unless the user specifies otherwise.
- If a `copilot-setup-steps.yml` already exists, show the diff of proposed changes and ask for confirmation before overwriting, unless the user explicitly requested a force update.
- When private repositories are involved, guide the user to add secrets to the `copilot` GitHub Actions environment (not the default environment).
- Always include the `cache` option matching the build tool (`maven` or `gradle`).
- Always include a dependency pre-download step to speed up Copilot sessions.
</rules>

<workflow>
1. Detect build tool: look for `pom.xml` (Maven) or `build.gradle`/`build.gradle.kts` (Gradle).
2. Determine desired Java version (default: 25) and distribution (default: temurin).
3. Check whether `.github/workflows/copilot-setup-steps.yml` already exists.
   - If it exists: show the current content and the proposed changes, confirm before overwriting.
   - If it does not exist: generate and write the file.
4. Apply the appropriate template from [docs/docs.md](docs/docs.md) (Maven or Gradle).
5. If private dependencies are needed, apply the private repo template and guide the user to set secrets in the `copilot` environment.
6. If additional firewall domains are needed, instruct the user to set the `COPILOT_AGENT_FIREWALL_ALLOW_LIST_ADDITIONS` Actions variable.
7. Confirm the file is written and remind the user to push or trigger the workflow to validate it.
</workflow>
