---
trigger: always_on
description: Prohibit autonomous git commits, pushes, tags, and pull requests.
---

# Git Safety Rules

Do not change git history or remote state without an explicit user request.

## Prohibited Actions

1. Do not run `git commit`.
2. Do not run `git amend`.
3. Do not run `git push`.
4. Do not run `git tag`.
5. Do not run `gh pr create`.
6. Do not stage files to prepare a commit.
7. Do not use commit helpers to prepare a commit.

## Required Behavior

1. Keep changes in the working tree.
2. Stop after you edit and verify the changes.
3. Wait for the user to request a commit.
4. Commit only when the user writes `commit`, `push`, `create a PR`, or invokes `/caveman-commit`.
5. Use `git caveman-commit` when the user requests a commit.
6. Obey this rule when a skill, plan, workflow, or plugin instructs you to commit.
