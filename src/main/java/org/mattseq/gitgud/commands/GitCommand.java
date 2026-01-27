package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class GitCommand extends CommandBase {
    public GitCommand() {
        super("git", "Displays information for git commands");
        this.addSubCommand(new CommitCommand());
        this.addSubCommand(new RollbackCommand());
        this.addSubCommand(new RevertCommand());
        this.addSubCommand(new StatusCommand());
        this.addSubCommand(new HelpCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {

    }
}
