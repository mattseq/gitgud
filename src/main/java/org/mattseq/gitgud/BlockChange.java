package org.mattseq.gitgud;

import com.hypixel.hytale.math.vector.Vector3i;

public class BlockChange {
    public Vector3i position;
    public String oldBlockId;
    public String newBlockId;
    public final long timestamp;
    // TODO: consider adding author info and block orientation (for relevant blocks, e.g. stairs)

    public BlockChange(Vector3i position, String oldBlockId, String newBlockId) {
        this.position = position;
        this.oldBlockId = oldBlockId;
        this.newBlockId = newBlockId;
        this.timestamp = System.currentTimeMillis();
    }
}
