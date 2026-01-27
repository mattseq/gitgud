package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class HelpCommand extends CommandBase {
    public HelpCommand() {
        super("help", "Displays help information for Git commands");
    }

    @Override
    protected void executeSync(@javax.annotation.Nonnull com.hypixel.hytale.server.core.command.system.CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("Git Command Help:"));
        commandContext.sendMessage(Message.raw("/git-status - Shows the current status of the repository"));
        commandContext.sendMessage(Message.raw("/git-rollback - Rolls back the world to the last commit"));
        commandContext.sendMessage(Message.raw("/git-commit - Commits the current changes to the repository"));
        commandContext.sendMessage(Message.raw("/git-revert - Reverts the world to the previous commit"));
        commandContext.sendMessage(Message.raw("/git-help - Displays this help information"));
    }
}
