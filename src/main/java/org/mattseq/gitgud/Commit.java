package org.mattseq.gitgud;

import java.util.ArrayList;

public class Commit {
    public String message;
    public ArrayList<BlockChange> blockChanges;
    public long timestamp;
    public long parentCommit;

    public Commit(String message, ArrayList<BlockChange> blockChanges, long timestamp, long parentCommit) {
        this.message = message;
        this.blockChanges = blockChanges;
        this.timestamp = timestamp;
        this.parentCommit = parentCommit;
    }
}
