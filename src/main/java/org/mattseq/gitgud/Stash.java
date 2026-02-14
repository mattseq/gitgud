package org.mattseq.gitgud;

import java.util.ArrayList;

public class Stash {
    public ArrayList<BlockChange> blockChanges;
    public long timestamp;

    public Stash(ArrayList<BlockChange> blockChanges, long timestamp) {
        this.blockChanges = blockChanges;
        this.timestamp = timestamp;
    }
}
