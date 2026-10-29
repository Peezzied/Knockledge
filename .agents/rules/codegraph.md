---
trigger: always_on
description: Guidance for using CodeGraph exploration tools and CLI when indexed.
---

# CodeGraph Usage Rules

In repositories indexed by CodeGraph (a `.codegraph/` directory exists at the repo root), reach for it before grep, find, or manual file reads to understand or locate code:

- **MCP Tool**: `codegraph_explore` answers most code questions in one call. It shows relevant symbol source code and call paths.
- **Shell Alternative**: `codegraph explore "<symbol names or question>"` outputs the same information.

If no `.codegraph/` directory exists in the repo root, skip CodeGraph entirely.
