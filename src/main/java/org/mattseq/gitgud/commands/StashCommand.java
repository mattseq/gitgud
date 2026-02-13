package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;

public class StashCommand extends CommandBase {
    public StashCommand() {
        super("stash", "Manually stashes changes without committing to prevent memory issues. You shouldn't need to use this command, it's purely for testing.");

    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Repository.stashBlockChanges();
    }
}
