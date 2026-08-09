# Agent PR Review — Copilot Code Review Custom Instructions

> Source: https://github.blog/ai-and-ml/generative-ai/agent-pull-requests-are-everywhere-heres-how-to-review-them/
>
> Add these instructions to your repository's Copilot code review settings:
> - **GitHub:** Repository Settings → Copilot → Code review → Custom instructions
> - **VS Code:** Include in `.github/copilot-instructions.md`

---

When reviewing any pull request, apply the following checks in order. Flag every issue with a severity level: **BLOCKER** (must fix before merge), **WARN** (should fix or justify), or **PASS** (no issue found).

## 1. CI integrity (check first, before reading application code)

Inspect any changes to `.github/workflows/`, test configuration files, coverage threshold settings, and build scripts.

Flag as **BLOCKER** if any of the following are true:
- Coverage thresholds were decreased or removed
- Tests were deleted, renamed, or marked as skipped without justification
- A workflow no longer runs on forks or pull requests
- CI steps are gated behind new conditions that weren't there before
- Test commands now include `|| true`, `--passWithNoTests`, or `continue-on-error`

## 2. Code reuse (scan for duplicates before reviewing new utilities)

For every new function, helper, utility, module, or middleware introduced in this pull request:
- Search the codebase for functionally equivalent code
- If a duplicate or near-duplicate exists, flag as **BLOCKER** and require consolidation before merge
- If a new utility is added that cannot be justified as unique, flag as **WARN**

## 3. Critical path logic

Identify the most significant logic change in the diff. Trace it end-to-end from input through every transform to output.

Flag as **BLOCKER** or **WARN** if any of the following are true:
- Missing validation on external or untrusted input values
- Permission checks are absent on any conditional branch
- Off-by-one errors in pagination, loops, or index-based access
- A test that would fail on the pre-change behaviour does not exist

## 4. Security (only if the PR touches LLM workflows or untrusted input)

Flag as **BLOCKER** if any of the following are true:
- Untrusted content (PR bodies, issue bodies, commit messages, user input) is interpolated into prompts without sanitization
- `GITHUB_TOKEN` has write permissions when read-only is sufficient
- Model output is executed as a shell command without validation
- Secrets are accessible to an agent step or printed to logs

## 5. Evidence

For every non-trivial logic change, flag as **BLOCKER** if:
- No test exists that would fail on the pre-change behaviour

Flag as **WARN** if:
- A risky change (data migration, deletion, infrastructure modification) has no rollback plan

---

## When to request a smaller PR

Comment requesting a breakdown (without writing inline review comments) if:
- The diff touches more than five unrelated files
- The purpose of the PR cannot be described in one sentence
- The PR body is empty and there is no implementation plan
- CI is failing and the only changes are to test files
