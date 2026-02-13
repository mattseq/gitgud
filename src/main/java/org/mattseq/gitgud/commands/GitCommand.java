package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.mattseq.gitgud.GitGudPlugin;

import javax.annotation.Nonnull;

public class GitCommand extends CommandBase {
    public GitCommand() {
        super("gitgud", "Displays information for git commands");
        this.addSubCommand(new CommitCommand());
        this.addSubCommand(new RollbackCommand());
        this.addSubCommand(new RevertCommand());
        this.addSubCommand(new StatusCommand());
        this.addSubCommand(new HelpCommand());
        this.addSubCommand(new StashCommand());
        GitGudPlugin.LOGGER.atInfo().log("Registered gitgud command with subcommands: commit, rollback, revert, status, help, stash");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {

    }
}
