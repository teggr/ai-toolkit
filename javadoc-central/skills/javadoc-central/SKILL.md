---
name: javadoc-central
description: Skill for browsing JavaDoc for any Maven Central artifact using the javadocs.dev MCP server (preferred) or HTTP endpoints (fallback).
user-invocable: true
---

<!-- Source: https://github.com/jamesward/javadoccentral -->

This skill helps you find and link to JavaDoc for any artifact on Maven Central via [javadocs.dev](https://javadocs.dev/).

## Preferred: MCP tool

Use the `javadoc-central` MCP server tools when available. They provide structured, up-to-date lookups without constructing URLs manually.

## Fallback: HTTP endpoints

If the MCP server is unavailable, use the following URL patterns directly.

### URL format

```
https://javadocs.dev/GROUP_ID/ARTIFACT_ID/VERSION
```

| What you have | Example URL |
|---|---|
| Group, artifact, version | `https://javadocs.dev/org.webjars/webjars-locator/0.32` |
| Group and artifact (latest) | `https://javadocs.dev/org.webjars/webjars-locator` |
| Group only | `https://javadocs.dev/org.webjars` |
| Explicit latest version | `https://javadocs.dev/org.webjars/webjars-locator/latest` |

### Symbol search

Search for a class or symbol across all indexed artifacts:

```
https://www.javadocs.dev/?WebJarAssetLocator
```

### Version badge

Display a `javadocs.dev | <latest version>` badge that resolves the latest Maven Central version (cached for one hour):

```
https://www.javadocs.dev/GROUP_ID/ARTIFACT_ID/badge.svg
```

Markdown example:

```markdown
[![javadocs.dev](https://www.javadocs.dev/com.example/my-lib/badge.svg)](https://www.javadocs.dev/com.example/my-lib/latest)
```
