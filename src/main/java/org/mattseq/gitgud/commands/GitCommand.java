package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.mattseq.gitgud.GitGudPlugin;

import javax.annotation.Nonnull;

public class GitCommand extends CommandBase {
    public GitCommand() {
        super("gitgud", "Main command for gitgud plugin");
        this.addSubCommand(new CommitCommand());
        this.addSubCommand(new RollbackCommand());
        this.addSubCommand(new RevertCommand());
        this.addSubCommand(new StatusCommand());
        this.addSubCommand(new HelpCommand());
        this.addSubCommand(new StashCommand());
        this.addSubCommand(new LogCommand());
        this.addSubCommand(new TagCommand());
        this.addSubCommand(new CheckoutCommand());
        GitGudPlugin.LOGGER.atInfo().log("Registered gitgud command");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {

    }
}
