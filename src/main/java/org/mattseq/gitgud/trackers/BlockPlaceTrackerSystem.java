package org.mattseq.gitgud.trackers;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mattseq.gitgud.BlockChange;
import org.mattseq.gitgud.GitGudPlugin;
import org.mattseq.gitgud.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockPlaceTrackerSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    public BlockPlaceTrackerSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent placeBlockEvent) {
        assert placeBlockEvent.getItemInHand() != null;
        Repository.addBlockChange(new BlockChange(placeBlockEvent.getTargetBlock(), "Empty", placeBlockEvent.getItemInHand().getItem().getBlockId()), true);
        GitGudPlugin.LOGGER.atInfo().log("Block placed at " + placeBlockEvent.getTargetBlock() + " with ID " + placeBlockEvent.getItemInHand().getItemId());
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
