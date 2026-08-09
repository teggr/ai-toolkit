---
name: setup-java
description: Reference skill for installing and configuring Java in GitHub Actions workflows using actions/setup-java, including distribution selection, caching, Maven/Gradle publishing, and multi-JDK setups.
user-invocable: false
---

This skill provides guidance for using [actions/setup-java](https://github.com/actions/setup-java) in GitHub Actions workflows.

Use this skill to:
- Introduce a Java setup step into a project that does not yet have a CI pipeline.
- Update or audit an existing workflow to use the latest stable action version and recommended inputs.
- Configure dependency caching for Maven, Gradle, or sbt.
- Set up Maven publishing or GPG signing.
- Manage multiple JDKs or Maven toolchains in a single job.

Primary usage pattern:
1. Use [docs/docs.md](docs/docs.md) in this skill for inputs, supported distributions, caching rules, version syntax, and example workflows.
2. Always use the latest stable release (`v5`) unless the user explicitly targets a newer major version.
3. Prefer `distribution: temurin` and a pinned LTS version (`21` or `25`) unless the project already specifies a version file or a different distribution.
4. Use `cache: maven` or `cache: gradle` whenever the project uses those build tools to avoid redundant dependency downloads.
5. Use the official repository as a live fallback for edge cases: https://github.com/actions/setup-java

See [docs/docs.md](docs/docs.md) for the full reference.
