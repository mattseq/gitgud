package org.mattseq.plugin;

import com.hypixel.hytale.math.vector.Vector3i;

public class BlockChange {
    public Vector3i position;
    public String newBlockId;
    public final long timestamp;

    public BlockChange(Vector3i position, String newBlockId) {
        this.position = position;
        this.newBlockId = newBlockId;
        this.timestamp = System.currentTimeMillis();
    }
}
