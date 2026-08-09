# ai-toolkit bundle management guide

Use this guide to manage Copilot customization bundles from `teggr/ai-toolkit` after they have been installed into a local repository or global Copilot configuration.

This skill is optional. You do not need it to bootstrap your first bundle because `AiToolkit.java` already provides the install surface.

## Core command surface

Run from a clone of this repository:

```bash
jbang AiToolkit.java list
jbang AiToolkit.java install [--target <dir> | --global] [--force] <bundle>
```

Run directly from GitHub (without cloning):

```bash
jbang https://raw.githubusercontent.com/teggr/ai-toolkit/main/AiToolkit.java list
jbang https://raw.githubusercontent.com/teggr/ai-toolkit/main/AiToolkit.java install <bundle>
```

## Install roots

- **Repo-local install root**: `<repo>/.github`
- **Global install root**: `~/.copilot`
- **Custom install root**: `--target <dir>`

`--global` overrides `--target`.

## 1) Inspect toolkit-managed bundles

Use this flow when you want to understand what `ai-toolkit`-managed skills, agents, or instructions are already present.

Start by discovering the available upstream bundles:

```bash
jbang AiToolkit.java list
```

This lists top-level bundle folders in `teggr/ai-toolkit`.

Then compare those bundles to the files present in your chosen install root.

## 2) Bootstrap or add a bundle

For first-time installs, use these commands directly instead of trying to install this skill first.

Local repository install:

```bash
jbang AiToolkit.java install discovery
```

Custom target install:

```bash
jbang AiToolkit.java install --target /path/to/resources review
```

Global install:

```bash
jbang AiToolkit.java install --global spring-htmx
```

Use `--force` to overwrite existing files without prompts.

## 3) Determine what bundles are installed

There is no dedicated `installed` command yet, so detect by file matching:

1. Get remote bundle names: `jbang AiToolkit.java list`
2. For each bundle, get its remote file set from the repository tree (`<bundle>/...`)
3. For the chosen install root (`.github`, `~/.copilot`, or custom):
   - Remove the `<bundle>/` prefix from remote file paths
   - Check whether those relative files exist under the install root
4. Mark bundle state:
   - **installed**: all expected files present
   - **partial**: some files present
   - **not installed**: no files present

Prefer reporting partial bundles explicitly so users can repair drift.

## 4) Update an installed bundle

Updates are done by reinstalling into the same root.

Safe update flow:

1. Inventory installed bundles and select target bundle(s)
2. Preview local modifications where possible (e.g., compare files before overwrite)
3. Reinstall bundle into same root:

```bash
jbang AiToolkit.java install [--target <same-root> | --global] <bundle>
```

4. Use `--force` only when user approves overwriting local edits
5. Report installed/skipped/failed counts

This is the main ongoing management task for this skill.

## 5) Reconcile local and remote versions

When local files diverge from remote bundle content:

- **Keep local customizations**: skip overwrite and note drift
- **Adopt remote latest**: reinstall with overwrite (`--force` or explicit overwrite confirmation)
- **Mixed strategy**: overwrite selected files only via prompt-driven install (without `--force`)

Always report:

- Target root used
- Bundle(s) processed
- File outcomes (installed, skipped, failed)
- Any unresolved drift or manual follow-up needed
