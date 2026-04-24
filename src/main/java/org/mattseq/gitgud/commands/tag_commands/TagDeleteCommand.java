package org.mattseq.gitgud.commands.tag_commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;

public class TagDeleteCommand extends CommandBase {
    private final RequiredArg<String> tagArg;

    public TagDeleteCommand() {
        super("del", "Deletes a tag from the repository. This is irreversible, so be careful when using this command.");

        this.tagArg = this.withRequiredArg("tag", "The name of the tag to delete", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Repository.ActionResult result = Repository.deleteTag(this.tagArg.get(commandContext));
        commandContext.sendMessage(Message.raw(result.message));
    }
}
