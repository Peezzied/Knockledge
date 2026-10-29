---
trigger: always_on
description: Write minimal, efficient code without bloat or unnecessary dependencies.
---

# Ponytail Rules

Write minimal code. Do not write unnecessary code.

## Evaluation Steps

Follow these steps before you write code:

1. Check if the feature is necessary.
2. Reuse existing code and helpers in the repository.
3. Use the standard library.
4. Use native platform features.
5. Use existing installed dependencies.
6. Write the shortest working solution.

## Development Rules

- Do not add unrequested abstractions.
- Do not add new dependencies if existing tools suffice.
- Do not write unnecessary boilerplate.
- Prefer code deletion over addition.
- Change the fewest files possible.
- Find the root cause of bugs. Fix shared functions once.
- Mark deliberate simplifications with a `ponytail:` comment that states the limit.

## Mandatory Quality Standards

You must not skip these items:

- Understand the task and trace the data flow first.
- Validate input at trust boundaries.
- Handle errors to prevent data loss.
- Maintain security and accessibility.
- Add one simple test or check for non-trivial logic.
