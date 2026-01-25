package org.mattseq.plugin;

import com.hypixel.hytale.math.vector.Vector3d;

public class BlockChange {
    private Vector3d position;
    private String newBlockId;
    private final long timestamp;

    public BlockChange(Vector3d position, String newBlockId) {
        this.position = position;
        this.newBlockId = newBlockId;
        this.timestamp = System.currentTimeMillis();
    }
}
