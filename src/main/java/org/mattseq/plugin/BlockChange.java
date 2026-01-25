package org.mattseq.plugin;

import com.hypixel.hytale.math.vector.Vector3i;

public class BlockChange {
    private Vector3i position;
    private String newBlockId;
    private final long timestamp;

    public BlockChange(Vector3i position, String newBlockId) {
        this.position = position;
        this.newBlockId = newBlockId;
        this.timestamp = System.currentTimeMillis();
    }
}
