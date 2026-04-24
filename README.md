# GitGud

Lightweight in-game world versioning for Hytale.

GitGud tracks block place/break events, stores them as local commits, and lets you roll back, revert, and inspect history with in-game commands.

## Features

- Track block changes (place / break) and store them as commits in `./gitgud/commits`.
- Save a commit with a message, revert the latest commit to restore previous blocks.
- Automatically stash and unstash uncommitted changes to preserve changes after server restarts and save memory.
- Rollback unsaved changes.
- Lightweight, compressed JSON storage for commits

## Installation

1. Build the mod JAR.
2. Place the JAR in your server's mod path.
3. Start the server.

On first startup, GitGud creates a repository folder in the server working directory:

## Quick start

1. Make block changes in-game.
2. Run `/gitgud commit "your message"`.
3. Use `/gitgud log` or `/gitgud status` to inspect state.
4. Use `/gitgud rollback` for uncommitted changes, or `/gitgud revert` for the latest committed change.

## Commands

All commands are subcommands of `/gitgud`.

```text
/gitgud help
  Show command help text.

/gitgud status
  Shows:
  - commit count
  - in-memory unstashed change count ("Changes since last stash")
  - stash file count

/gitgud log
  Prints commit history from HEAD backward.
  The CURRENT commit is marked with "<--".

/gitgud commit "<message>"
  Creates a commit from recent block changes with the given message.
  Fails if there are no changes.
  Fails in detached mode (when HEAD != CURRENT).

/gitgud rollback
  Reverts uncommitted block changes only.

/gitgud revert
  Reverts the latest commit (from CURRENT), then deletes that commit file.
  Also rolls back uncommitted changes first.
  Fails in detached mode (when HEAD != CURRENT).

/gitgud stash
  Manually stash in-memory block changes to `.gitgud/stash`.
  Mostly useful for testing (auto-stash already exists).

/gitgud tag add <name> [--desc "<description>"]
  Adds a tag to the latest commit (HEAD).
  Description is optional and defaults to "No description provided".

/gitgud tag list
  Lists all tags in the repository.

/gitgud tag del <name>
  Deletes a tag from the repository.

/gitgud checkout <index|HEAD|TAIL> [--tag]
  Checks out by commit index, HEAD, TAIL, or a tag with --tag.
  Index is zero-based in HEAD-chain order:
  - 0 = HEAD (newest)
  - 1 = parent of HEAD
  - etc.
  Checkout moves CURRENT and applies/reverts commits as needed.
  Use `--tag` when the argument is a tag name instead of an index.
```

## Repository model (HEAD vs CURRENT)

- `HEAD`: tip of the main commit chain
- `CURRENT`: commit currently applied in world state

When `HEAD != CURRENT`, the repository is detached.

Detached mode rules in current implementation:

- `commit` is blocked
- `revert` is blocked
- `checkout` is allowed
- `rollback` is allowed (works on uncommitted changes)

## Stash behavior

- In-memory block changes auto-stash when count exceeds 32
- Stash data is stored as gzip-compressed JSON files in `.gitgud/stash`
- Commit and rollback operations unstash first to ensure a complete operation
- Plugin shutdown stashes remaining in-memory changes

## Troubleshooting

- Ensure that the user has OP permissions to run GitGud commands.
- If the plugin does not create the `./gitgud` folder, ensure the server process has write permission to its working directory.
- If block changes are not being recorded, check server logs for plugin initialization messages and ensure the plugin is loaded.

## Contributing

Issues and PRs are welcome.
