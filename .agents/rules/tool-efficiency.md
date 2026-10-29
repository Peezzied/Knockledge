---
trigger: always_on
description: Prioritize native agent tools, read-only Git inspection, bounded line ranges, and exclude build artifacts to maximize token efficiency.
---

# Tool Efficiency and Context Management

Apply these rules to minimize token usage and avoid context flooding.

## Tool Precedence

1. Use native agent tools first.
   - Use `view_file` to inspect files.
   - Use `replace_file_content` to edit existing files.
   - Use `write_to_file` to create new files.
2. Use read-only Git commands to survey the project.
   - Use `git ls-files` to list tracked project files. This automatically ignores untracked build artifacts.
   - Use `git status`, `git diff`, and `git grep` for fast repository state and symbol discovery.
   - Read-only Git commands do not violate Git Safety rules.
3. Use shell commands (`run_command`) only for execution tasks.
   - Use shell commands for test execution, compilation, and git operations.
   - Do not use shell cmdlets (such as `Get-Content`, `cat`, `dir`, or `ls`) to read or search files when native file tools or Git commands exist.
4. Use MCP tools and external graph queries as secondary fallbacks.
   - Query MCP or external tools only when native tools cannot retrieve the required data.

## Targeted File Reading

- Do not read whole large files at once.
- Always specify `StartLine` and `EndLine` to inspect targeted line ranges.
- Read only the sections necessary to complete the task.

## Exclude Build Artifacts

Always exclude build artifacts and generated files from searches and directory listings:
- Exclude `node_modules/`
- Exclude `target/`
- Exclude `dist/` and `build/`
- Exclude `.git/`
- Exclude `.gradle/`
- Exclude `*.db` and binary database files
