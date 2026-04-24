package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;

public class CheckoutCommand extends CommandBase {
    private final RequiredArg<Integer> commitArg;
    public CheckoutCommand() {
        super("checkout", "Checks out a specific commit or tag. Usage: /checkout <commit index>");
        this.commitArg = this.withRequiredArg("commit", "The commit index", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Repository.ActionResult result = Repository.checkoutCommit(this.commitArg.get(commandContext));
        commandContext.sendMessage(Message.raw(result.message));
    }
}
