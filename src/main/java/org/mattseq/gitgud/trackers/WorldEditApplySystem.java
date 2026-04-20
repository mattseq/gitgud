package org.mattseq.gitgud.trackers;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WorldEditApplySystem extends EntityTickingSystem<EntityStore> {
    private static final Queue<WorldEdit> pendingEdits = new ConcurrentLinkedQueue<>();

    public static void enqueue(int x, int y, int z, String blockId) {
        pendingEdits.add(new WorldEdit(x, y, z, blockId));
    }

    @Override
    public void tick(float v, int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        WorldEdit edit;
        while ((edit = pendingEdits.poll()) != null) {
            Objects.requireNonNull(Universe.get().getDefaultWorld()).setBlock(edit.x, edit.y, edit.z, edit.blockId);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private record WorldEdit(int x, int y, int z, String blockId) {
    }
}
