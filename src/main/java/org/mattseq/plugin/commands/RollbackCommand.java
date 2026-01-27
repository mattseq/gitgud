package org.mattseq.plugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.mattseq.plugin.Repository;

import javax.annotation.Nonnull;

public class RollbackCommand extends CommandBase {

    public RollbackCommand() {
        super("rollback", "Rolls back the world to the last commit");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Repository.rollback();
        commandContext.sendMessage(Message.raw("Rolled back to the last commit."));
    }
}
