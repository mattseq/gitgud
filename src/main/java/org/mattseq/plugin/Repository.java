package org.mattseq.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Repository {
    public static ArrayList<BlockChange> blockChanges = new ArrayList<>();

    public static ArrayList<BlockChange> getBlockChanges() {
        return blockChanges;
    }

    public static void addBlockChange(BlockChange change) {
        blockChanges.add(change);
    }

    public static void initialize() {
        Path repoPath = Path.of(".gitgud");
        if (!Files.exists(repoPath)) {
            try {
                Files.createDirectory(repoPath);
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create repository directory: " + e.getMessage());
            }
        }
    }

    public static void saveCommit(String message) {
        // Placeholder for commit saving logic
        GitGudPlugin.LOGGER.atInfo().log("Commit saved with message: " + message);
    }
}
