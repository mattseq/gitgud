package org.mattseq.plugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.mattseq.plugin.Repository;

import javax.annotation.Nonnull;

public class RevertCommand extends CommandBase {
    public RevertCommand() {
        super("revert", "Reverts the world to the previous commit");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Repository.revertLatestCommit();
        commandContext.sendMessage(Message.raw("Reverted to the previous commit."));
    }
}
