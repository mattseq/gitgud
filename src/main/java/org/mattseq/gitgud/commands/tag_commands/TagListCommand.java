package org.mattseq.gitgud.commands.tag_commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;

public class TagListCommand extends CommandBase {
    public TagListCommand() {
        super("list", "Lists all tags in the repository");
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("Tags:"));
        Repository.listTags().forEach(tag -> {
            commandContext.sendMessage(Message.raw("- " + tag.name + " (" + tag.commitId + "): " + tag.description));
        });
    }
}
