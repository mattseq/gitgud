package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mattseq.gitgud.Repository;
import org.mattseq.gitgud.dto.Tag;

public class CheckoutCommand extends CommandBase {
    private final RequiredArg<String> commitArg;
    private final FlagArg tagFlag;

    public CheckoutCommand() {
        super("checkout", "Checks out a specific commit or tag. Usage: /checkout <commit index>");
        this.commitArg = this.withRequiredArg("commit", "index/pointer/tag", ArgTypes.STRING);
        this.tagFlag = this.withFlagArg("tag", "Indicates that the provided index is a tag name instead of a commit index");
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        // if tag flag is set, find tag by name and checkout commit it points to, otherwise checkout commit by index
        if (this.tagFlag.get(commandContext)) {
            try {
                Tag tag = Repository.getTag(this.commitArg.get(commandContext));
                assert tag != null;
                Repository.ActionResult result = Repository.checkoutCommit(Repository.getCommitByTimestamp(tag.commitId));
                commandContext.sendMessage(Message.raw(result.message));
            } catch (Exception e) {
                commandContext.sendMessage(Message.raw("Tag not found: " + this.commitArg.get(commandContext)));
            }
        } else {
            int commitIndex = -1;
            try {
                commitIndex = Integer.parseInt(this.commitArg.get(commandContext));
            } catch (NumberFormatException e) {
                // allow special pointers "HEAD" and "TAIL" to be used instead of commit index
                if (this.commitArg.get(commandContext).equals("HEAD")) {
                    commitIndex = 0;
                } else if (this.commitArg.get(commandContext).equals("TAIL")) {
                    commitIndex = Repository.getCommitHistory().size() - 1;
                } else {
                    commandContext.sendMessage(Message.raw("Invalid commit index: " + this.commitArg.get(commandContext)));
                }
            }
            Repository.ActionResult result = Repository.checkoutCommit(commitIndex);
            commandContext.sendMessage(Message.raw(result.message));
        }
    }
}
