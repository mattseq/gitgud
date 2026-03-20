package org.mattseq.gitgud;

public class Tag {
    String name;
    String description;
    long commitId;

    public Tag(String name, String description, long commitId) {
        this.name = name;
        this.description = description;
        this.commitId = commitId;
    }
}
