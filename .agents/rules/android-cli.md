---
trigger: always_on
description: Directives for using the Android CLI and Android Studio commands during development.
---

# Android CLI and Studio Tooling Rules

Use official Android CLI commands for development and device operations.

## Tooling Directives

1. **General Android CLI**: Use the `android` CLI tool for SDK management, project building, emulators, and UI inspection.
2. **Android Studio Integration**: When developing with a running Android Studio instance, use `android studio` commands:
   - Check connection status: `android studio check`
   - Open source files: `android studio open-file <file>`
   - Analyze files for issues: `android studio analyze-file <file>`
   - Find declarations and usages: `android studio find-declaration` and `android studio find-usages`
   - Render UI previews: `android studio render-compose-preview`
   - Check dependency versions: `android studio version-lookup <artifact>`
3. **PowerShell Compatibility**: Run all CLI commands with PowerShell syntax.
