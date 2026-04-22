package org.mattseq.gitgud.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class HelpCommand extends CommandBase {
    public HelpCommand() {
        super("help", "Displays help information for Git commands");
    }

    @Override
    protected void executeSync(@javax.annotation.Nonnull com.hypixel.hytale.server.core.command.system.CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("GitGud Command Help:"));
        commandContext.sendMessage(Message.raw("/gitgud status - Show commits, unstashed changes, and stash count"));
        commandContext.sendMessage(Message.raw("/gitgud log - Show commit history (CURRENT is marked with <-- )"));
        commandContext.sendMessage(Message.raw("/gitgud commit <message> - Save new changes as a commit"));
        commandContext.sendMessage(Message.raw("/gitgud rollback - Revert uncommitted changes only"));
        commandContext.sendMessage(Message.raw("/gitgud revert - Revert and delete the latest commit"));
        commandContext.sendMessage(Message.raw("/gitgud stash - Manually stash in-memory block changes"));
        commandContext.sendMessage(Message.raw("/gitgud tag <name> [--desc <description>] - Tag the latest commit"));
        commandContext.sendMessage(Message.raw("/gitgud checkout <index> - Checkout commit by index (0 = HEAD)"));
        commandContext.sendMessage(Message.raw("/gitgud help - Show this help information"));
    }
}
