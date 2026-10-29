---
trigger: always_on
description: Behavioral guidelines to reduce common LLM coding mistakes (think before coding, simplicity first, surgical changes, goal-driven execution).
---

# Karpathy Guidelines

Apply these behavioral guidelines to reduce common coding mistakes.

**Tradeoff:** These guidelines favor caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Do not assume. Do not conceal confusion. State trade-offs.**

Before implementation:
- State your assumptions explicitly. If uncertain, ask questions.
- If multiple interpretations exist, show each option. Do not select silently.
- If a simpler approach exists, suggest that approach.
- If requirements remain unclear, pause execution. State the ambiguity and ask.

## 2. Simplicity First

**Write the minimum code that solves the problem. Add nothing speculative.**

- Do not add features beyond the request.
- Do not create abstractions for single-use code.
- Do not introduce unrequested configuration or flexibility.
- Do not add error handling for impossible cases.
- If 50 lines can replace 200 lines, rewrite the solution.

## 3. Surgical Changes

**Touch only required code. Remove only orphaned artifacts from your edits.**

When modifying existing code:
- Do not reformat adjacent code, comments, or whitespace.
- Do not refactor functional code.
- Match the existing project style.
- If you find unrelated dead code, report it. Do not delete it.

When your edits produce unused items:
- Remove imports, variables, and functions that your edits made unused.
- Do not delete prior dead code unless the user requests it.

Verify every line: Every edited line must trace directly to the request.

## 4. Goal-Driven Execution

**Define success criteria. Verify until complete.**

Convert requests into testable criteria:
- "Add validation" → Write tests for invalid inputs, then make them pass.
- "Fix bug" → Write a reproducing test, then make it pass.
- "Refactor code" → Verify tests pass before and after the change.

For tasks with multiple steps, declare a clear sequence:
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
