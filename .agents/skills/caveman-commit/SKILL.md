---
name: caveman-commit
description: >
  Write a Conventional Commits message compressed to intent only. Use for
  "write a commit", "commit message", /commit or /caveman-commit. Add the
  parameter "perform git commit" when the user wants the commits executed
  after review.
---

Write commit messages terse and exact. Conventional Commits format. No fluff. Why over what. Use minimal/low thinking effort (budget minimal reasoning tokens since generating commit messages is concise and direct).

## Modes

- **Message mode** (default): generate the message, then stop. Nothing runs.
- **Commit mode**: the request carries the parameter `perform git commit`. Generate the messages, audit the affected files, ask for confirmation, then run `git commit`.

## Rules

**Subject line:**
- `<type>(<scope>): <imperative summary>` — `<scope>` optional
- Types: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `chore`, `build`, `ci`, `style`, `revert`
- Imperative mood: "add", "fix", "remove" — not "added", "adds", "adding"
- ≤50 chars when possible, hard cap 72
- No trailing period
- Match project convention for capitalization after the colon

**Body (only if needed):**
- Skip entirely when subject is self-explanatory
- Add body only for: non-obvious *why*, breaking changes, migration notes, linked issues
- Wrap at 72 chars
- Bullets `-` not `*`
- Reference issues/PRs at end: `Closes #42`, `Refs #17`

**What NEVER goes in:**
- "This commit does X", "I", "we", "now", "currently" — the diff says what
- "As requested by..." — use Co-authored-by trailer
- "Generated with Claude Code" or any AI attribution — unless the user's own rule requires an `Assisted-by`/AI-attribution trailer, then add it as a trailer
- Emoji (unless project convention requires)
- Restating the file name when scope already says it

## Examples

Diff: new endpoint for user profile with body explaining the why
- ❌ "feat: add a new endpoint to get user profile information from the database"
- ✅
  ```
  feat(api): add GET /users/:id/profile

  Mobile client needs profile data without the full user payload
  to reduce LTE bandwidth on cold-launch screens.

  Closes #128
  ```

Diff: breaking API change
- ✅
  ```
  feat(api)!: rename /v1/orders to /v1/checkout

  BREAKING CHANGE: clients on /v1/orders must migrate to /v1/checkout
  before 2026-06-01. Old route returns 410 after that date.
  ```

## Commit Mode (`perform git commit`)

Enter commit mode when the request carries `perform git commit`, with or
without a count: `/caveman-commit perform git commit 3`. The number is `N`,
default `1`. Audit first with `git status --porcelain` and `git diff --stat`,
plus `git diff --cached --stat` when work is staged. Make no mutation yet.

Place every changed path in exactly one group, by logical change. Fewer than
`N` logical groups: split on file boundaries and state the limit. Do not pad
or invent changes.

Print each group in commit order, exactly this shape:

````
## Commit <i>/<N>

<type>(<scope>): <summary>

<body paragraph, when required>

Files (<count>):
- path/to/file.java (M)
````

Copy the status code verbatim: `M`, `A`, `D`, `??`, `R`. Close with
`Unassigned paths: none`. Then stop and ask one question: commit all `N`
groups, or stop.

The parameter selects the mode. It is not the confirmation. Run no `git add`,
`git commit`, `git amend`, or stash until the user answers yes.

On yes, per group in order: `git add -- <paths of that group>`, then `git
commit -m "<subject>" -m "<body>"` (drop the second `-m` for subject-only),
then report the short hash. After the last group, show `git status --short`.
On stop, leave the tree untouched.

Never use `git add -A`, `git add .`, or `git commit -a`. Stage explicit paths.
Never amend, push, tag, or open a pull request.

## Auto-Clarity

Always include body for: breaking changes, security fixes, data migrations, anything reverting a prior commit. Never compress these into subject-only — future debuggers need the context.

## Boundaries

Message mode only generates the commit message. It does not run `git commit`,
does not stage files, does not amend. Output the message as a code block ready
to paste.

Commit mode runs the commits, after the confirmation in step 4. It never runs
`git commit` on an unconfirmed plan, and it never runs on a plan the user did
not see.

"stop caveman-commit" or "normal mode": revert to verbose commit style.
