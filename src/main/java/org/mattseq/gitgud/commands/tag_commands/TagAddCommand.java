package org.mattseq.gitgud.commands.tag_commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;

public class TagAddCommand extends CommandBase {
    private final RequiredArg<String> nameArg;
    private final DefaultArg<String> descriptionArg;

    public TagAddCommand() {
        super("add", "Adds tag to the current commit with a name and description");

        this.nameArg = this.withRequiredArg("name", "The tag name", ArgTypes.STRING);
        this.descriptionArg = this.withDefaultArg("desc", "The tag description", ArgTypes.STRING, "", "No description provided");
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Repository.ActionResult result = Repository.addTagToLatestCommit(this.nameArg.get(commandContext), this.descriptionArg.get(commandContext));
        commandContext.sendMessage(Message.raw(result.message));
    }
}
