package org.mattseq.gitgud.dto;

public class Tag {
    public String name;
    public String description;
    public long commitId;

    public Tag(String name, String description, long commitId) {
        this.name = name;
        this.description = description;
        this.commitId = commitId;
    }
}
