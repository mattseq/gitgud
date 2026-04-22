package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;

public class LogCommand extends CommandBase {
    public LogCommand() {
        super("log", "Shows the commit history");
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("Commit History:"));
        long currentTimestamp = Repository.getCurrentTimestamp();
        Repository.getCommitHistory().forEach(commit -> {
            if (commit.timestamp == currentTimestamp) {
                commandContext.sendMessage(Message.raw("- " + commit.timestamp + ": " + commit.message + " <--"));
            } else {
                commandContext.sendMessage(Message.raw("- " + commit.timestamp + ": " + commit.message));
            }
        });
    }
}
