package org.mattseq.plugin;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class CommitCommand extends CommandBase {
    private final RequiredArg<String> messageArg;
    public CommitCommand() {
        super("commit", "Commits changes to the repository");
        this.messageArg = this.withRequiredArg("message", "The commit message",ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Repository.saveCommit(this.messageArg.get(commandContext));
    }
}
