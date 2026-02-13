package org.mattseq.gitgud.trackers;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mattseq.gitgud.BlockChange;
import org.mattseq.gitgud.GitGudPlugin;
import org.mattseq.gitgud.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockBreakTrackerSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public BlockBreakTrackerSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent breakBlockEvent) {
        if (breakBlockEvent.getBlockType().getId().equals("Empty")) {
            return;
        }
        Repository.addBlockChange(new BlockChange(breakBlockEvent.getTargetBlock(), breakBlockEvent.getBlockType().getId(), "Empty"), true);
        GitGudPlugin.LOGGER.atInfo().log("Block broken at " + breakBlockEvent.getTargetBlock() + " with ID " + breakBlockEvent.getBlockType().getId());
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
