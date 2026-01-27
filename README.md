# GitGud — Lightweight in-game world versioning for Hytale

A simple server plugin that tracks block placements and breaks, lets builders save changes as local commits, and easily revert or stash changes. Designed for small teams and creative servers to recover mistakes without external tools.

## Features

- Track block changes (place / break) and store them as commits in `./gitgud/commits`.
- Save a commit with a message, revert the latest commit to restore previous blocks.
- Automatically stash and unstash uncommitted changes to preserve changes after server restarts.
- Rollback unsaved changes.
- Lightweight JSON storage for commits

## Installation

1. Download the JAR and place it in the `Mods` directory of your Hytale server.
2. Start the server. The plugin will create a `./gitgud` folder in the server working directory with `commits` and `stash` subfolders.

## Quick usage

1. Make block changes in the world (place/break blocks).
2. Run the commit command with a message to save changes as a JSON commit.
3. Revert the latest commit to restore previous blocks.

## Commands

Below are example in-game commands and how to use them.

- `/git status`
    - Show whether there are uncommitted block changes, and whether a stash exists.

- `/git commit "<message>"`
  - Create a new commit from the current uncommitted block changes.
  - Example: `/git commit "cathedral roof"`

- `/git revert`
  - Revert the previous commit, restoring blocks to their previous state.
  - Recommended to run `/git rollback` first to clear uncommitted changes.

- `/git rollback`
  - Roll back the current uncommitted changes in memory (rollback to last committed state).

Notes:
- If a command is missing or named differently in your build, check `/git help` or the plugin documentation bundled with the JAR.
- Permissions: only server operators can use these commands by default.

## Compatibility and Improvements

- Commits are stored as readable JSON in `./gitgud`; compression and advanced history tools are TODO.
- Not all elements are tracked (e.g., fluids, entities); focus is on block placements and breaks for now.
- Future improvements may include better diffing, multi-world support, a GUI interface, and memory and storage optimizations.

## Troubleshooting

- If the plugin does not create the `./gitgud` folder, ensure the server process has write permission to its working directory.
- If block changes are not being recorded, check server logs for plugin initialization messages and ensure the plugin is loaded.

## Contributing

Contributions and bug reports are welcome. Please open issues and pull requests on the project repository.

---

Lightweight recovery for accidental edits and iterative building — easy to use, easy to restore.
