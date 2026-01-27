package org.mattseq.plugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.mattseq.plugin.Repository;

import javax.annotation.Nonnull;

public class StatusCommand extends CommandBase {
    public StatusCommand() {
        super("status", "Shows the current status of the repository");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("Current Repository Status:"));
        commandContext.sendMessage(Message.raw(Repository.getCommitCount() + " commits in repository."));
        commandContext.sendMessage(Message.raw("Changes since last commit: " + Repository.getBlockChanges().size()));
    }
}
