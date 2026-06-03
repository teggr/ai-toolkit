# Agent PR Review — Reference Docs

> Source: https://github.blog/ai-and-ml/generative-ai/agent-pull-requests-are-everywhere-heres-how-to-review-them/

This document is the full reference for the `agent-pr-review` skill. It covers the ordered six-step review process, the five red-flag categories, and the escalation criteria for requesting a smaller pull request.

---

## Why agent PRs need a different review mindset

A coding agent is a productive, literal, pattern-following contributor with zero context about your incident history, your team's edge case lore, or the operational constraints that don't live in the repository. It produces code that **looks** complete. That "looks complete" failure mode is dangerous.

A January 2026 study, ["More Code, Less Reuse"](https://arxiv.org/abs/2601.21276), found that agent-generated code introduces more redundancy and more technical debt per change than human-written code — and that reviewers actually feel *better* about approving it.

---

## The six-step ordered review process

Work through these steps **in order**. Each step is a gate: a ❌ BLOCKER in any step must be resolved before you invest time in later steps.

| Time     | Step                      | What to do |
|----------|---------------------------|------------|
| 1–2 min  | **1. Scan and classify**  | File list and diff size. Is this narrow (docs, CI, small change) or complex (multi-file, logic, performance, tests)? Can you describe the purpose in one sentence? |
| 2–3 min  | **2. CI integrity check** | Inspect anything touching `.github/workflows/`, test configs, coverage settings, or build scripts. Flag any CI weakening. This is a stop sign. |
| 3–5 min  | **3. Code reuse scan**    | For every new helper, utility, or module: search for an existing equivalent. Flag duplicates and require consolidation before merge. |
| 5–8 min  | **4. Critical path trace**| Pick the most important logic change. Trace input → transforms → output. Check boundary conditions, permissions, and surprising branching. |
| 8–9 min  | **5. Security boundaries**| Only if the PR touches LLM workflows or untrusted input handling. Run through the security checklist. |
| 9–10 min | **6. Require evidence**   | For every non-trivial logic change: is there a test that fails on pre-change behaviour? Is there a rollback plan for risky changes? |

---

## Red flag reference

### 1. CI gaming

Agents fail CI. When they do, they have an obvious path to get tests passing: remove tests, skip lint steps, add `|| true` to test commands. Some agents take it.

**Any CI weakening is a blocker. Full stop.**

Check:
- Did coverage thresholds change?
- Were any tests removed, renamed, or marked as skipped?
- Did the workflow stop running on forks or pull requests?
- Are any CI steps now gated behind conditions they weren't before?
- Are test commands weakened (`|| true`, `--passWithNoTests`, `continue-on-error`)?

---

### 2. Code reuse blindness

Agents look for prior art. They'll find a pattern in the codebase and replicate it — often without checking whether a utility that already does the same thing exists elsewhere.

Symptoms:
- New utility functions that duplicate existing ones with slightly different names
- Validation logic reimplemented in multiple places
- Middleware written from scratch that already lives in a shared module
- Helpers that are "almost the same" but with different names

**This is the highest-ROI check you can do.** Leaving duplicated logic means agents will find it as prior art and replicate it further.

For every new helper or utility: do a quick search. If you find an equivalent, require consolidation before merge.

> 💡 Pro tip: Require justification for adding new utilities in agent PRs above a size threshold.

---

### 3. Hallucinated correctness

The obvious hallucination (calling an API that doesn't exist) gets caught in CI. The dangerous kind compiles, passes every test, and is *wrong*.

Examples:
- Off-by-one errors in pagination
- Missing permission checks on a branch never hit in tests
- Validation that short-circuits under an edge case the agent never considered
- Wrong behaviour under a race condition that only surfaces at scale

**Trace it, don't just scan it.** Pick the most critical path in the diff. Follow it from input through every transform to output. Check boundary conditions (zero, max, empty), missing validation on external values, permission checks on every branch, and surprising conditional logic.

Require a new test that fails on the pre-change behaviour. If the agent can't write a test that would have caught the bug it claims to fix, the fix is incomplete or the understanding is wrong.

---

### 4. Agentic ghosting

You leave a thorough review. The PR goes quiet. Or the agent responds and misses the point entirely, running in circles.

**Before investing deep review on a large agent PR, check the PR history.** Has it been responsive in previous rounds? Does it have a clear implementation plan?

If there's no plan, request a breakdown first:

> *"This PR is too large for me to review without a clearer implementation plan. Can you break it into smaller scoped units, or add a summary of what each part does and why it's structured this way? Happy to review after that."*

---

### 5. Untrusted input in workflows

Prompt injection in CI agents is underappreciated. The pattern: an agent workflow reads content from a PR body, an issue, or a commit message. That content gets interpolated into a prompt. The prompt goes to a model. The model output gets piped to a shell command. The whole thing runs with `GITHUB_TOKEN` permissions.

**Blockers when reviewing any workflow that calls an LLM:**
- Is untrusted user input (PR bodies, issue bodies, commit messages) interpolated into prompts without sanitization?
- Is `GITHUB_TOKEN` write-scoped when it only needs read access?
- Is model output being executed as shell commands without validation?
- Are secrets accessible to the agent step or being printed to logs?

What to require before merge:
- Least-privilege permissions in the workflow YAML (`permissions: read-all` is a reasonable default)
- Sanitize and quote untrusted content before it touches a prompt
- Separate the "analysis" step from the "execution" step with a human approval gate for anything touching production
- Never eval model output

---

## When to request a smaller PR

Request a breakdown before writing a single review comment if:

1. The diff touches more than five unrelated files
2. You cannot describe the purpose of the PR in one sentence
3. The agent has no implementation plan or the PR body is empty
4. CI is failing and the only changes in the diff are to test files

---

## Using with GitHub Copilot code review

See `../../instructions/agent-pr-review.md` for ready-to-use custom instructions you can configure for GitHub Copilot code review. These automate the mechanical checks (CI weakening, duplicate utilities, missing tests) on every PR before a human reviewer sees it.

Add to your repository's Copilot code review custom instructions via:
- **GitHub:** Repository Settings → Copilot → Code review → Custom instructions
- **VS Code:** Add the file contents to `.github/copilot-instructions.md` or reference this skill directly in agent mode
