package org.mattseq.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Repository {
    private static final Path REPO_PATH = Path.of(".gitgud");

    private static final ArrayList<BlockChange> blockChanges = new ArrayList<>();

    public static ArrayList<BlockChange> getBlockChanges() {
        synchronized (blockChanges) {
            return new ArrayList<>(blockChanges);
        }
    }

    public static void addBlockChange(BlockChange change) {
        blockChanges.add(change);
        GitGudPlugin.LOGGER.atInfo().log("Block change added at " + change.position + " with new ID " + change.newBlockId);
        GitGudPlugin.LOGGER.atInfo().log("Total unsaved changes: " + getBlockChanges().size());
    }

    public static void initialize() {
        if (!Files.exists(REPO_PATH)) {
            try {
                Files.createDirectory(REPO_PATH);
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create repository directory: " + e.getMessage());
            }
        }
    }

    public static void saveCommit(String message) {
        long timestamp = System.currentTimeMillis();

        Path commitFile = REPO_PATH.resolve("commit_" + timestamp + ".json");

        ArrayList<BlockChange> changesToSave = getBlockChanges();
        if (changesToSave.isEmpty()) {
            GitGudPlugin.LOGGER.atInfo().log("No changes to commit.");
            return;
        }

        try {
            String commitJson = serializeCommitJson(message, changesToSave, timestamp);
            Files.write(commitFile, commitJson.getBytes());
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to save commit: " + e.getMessage());
        }

        blockChanges.removeAll(changesToSave);
        GitGudPlugin.LOGGER.atInfo().log("Commit saved with message: " + message);
    }

    public static String serializeCommitJson(String message, ArrayList<BlockChange> blockChanges, long timestamp) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(new Commit(message, blockChanges, timestamp));
    }
}
