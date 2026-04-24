package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.commands.tag_commands.TagAddCommand;
import org.mattseq.gitgud.commands.tag_commands.TagDeleteCommand;
import org.mattseq.gitgud.commands.tag_commands.TagListCommand;

public class TagCommand extends CommandBase {
    public TagCommand() {
        super("tag", "Base command for managing tags.");

        this.addSubCommand(new TagAddCommand());
        this.addSubCommand(new TagListCommand());
        this.addSubCommand(new TagDeleteCommand());

    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
    }
}
