package org.mattseq.plugin;

import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;

import java.util.List;

public class BlockTracker {
    public static List<BlockChange> blockChanges;

    public static void onBlockPlaced(BreakBlockEvent event) {
        BlockChange change = new BlockChange(
                event.getTargetBlock().toVector3d(),
                event.getBlockType().getId()
        );
        blockChanges.add(change);
    }

    public static void onBlockBroken(BreakBlockEvent event) {
        BlockChange change = new BlockChange(
                event.getTargetBlock().toVector3d(),
                ""
        );
        blockChanges.add(change);
    }
}
