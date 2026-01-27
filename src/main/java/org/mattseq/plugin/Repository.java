package org.mattseq.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.universe.Universe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Repository {
    private static final Path REPO_PATH = Path.of(".gitgud");
    private static final Path COMMITS_PATH = REPO_PATH.resolve("commits");
    // TODO: dont use a separate stash folder, only one stash file
    private static final Path STASH_PATH = REPO_PATH.resolve("stash");

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
                GitGudPlugin.LOGGER.atInfo().log("Initialized new GitGud repository at " + REPO_PATH.toAbsolutePath());
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create repository directory: " + e.getMessage());
            }
        }

        if (!Files.exists(COMMITS_PATH)) {
            try {
                Files.createDirectory(COMMITS_PATH);
                GitGudPlugin.LOGGER.atInfo().log("Created commits directory at " + COMMITS_PATH.toAbsolutePath());
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create commits directory: " + e.getMessage());
            }
        }

        if (!Files.exists(STASH_PATH)) {
            try {
                Files.createDirectory(STASH_PATH);
                GitGudPlugin.LOGGER.atInfo().log("Created stash directory at " + STASH_PATH.toAbsolutePath());
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create stash directory: " + e.getMessage());
            }
        }
    }

    public static void saveCommit(String message) {
        long timestamp = System.currentTimeMillis();

        Path commitFile = COMMITS_PATH.resolve("commit_" + timestamp + ".json");

        ArrayList<BlockChange> changesToSave = getBlockChanges();
        if (changesToSave.isEmpty()) {
            GitGudPlugin.LOGGER.atInfo().log("No changes to commit.");
            return;
        }

        try {
            changesToSave.sort(Comparator.comparingLong(a -> a.timestamp));

            String commitJson = serializeCommitJson(new Commit(message, changesToSave, timestamp));
            // TODO: compress commit files
            Files.write(commitFile, commitJson.getBytes());
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to save commit: " + e.getMessage());
        }

        blockChanges.removeAll(changesToSave);
        GitGudPlugin.LOGGER.atInfo().log("Commit saved with message: " + message);
    }

    public static void revertCommit(Commit commit) {
        // reorder block changes to revert in reverse order of timestamp
        commit.blockChanges.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

        for (BlockChange change : commit.blockChanges) {
            Universe.get().getDefaultWorld().setBlock(change.position.x, change.position.y, change.position.z, change.oldBlockId);
            GitGudPlugin.LOGGER.atInfo().log("Reverting block at " + change.position + " to " + change.oldBlockId);
        }
        GitGudPlugin.LOGGER.atInfo().log("Reverted commit with message: " + commit.message);
    }

    public static void revertLatestCommit() {
        try {
            Path latestCommitFile = Files.list(COMMITS_PATH)
                    .filter(path -> path.getFileName().toString().startsWith("commit_"))
                    .max((p1, p2) -> {
                        long t1 = Long.parseLong(p1.getFileName().toString().split("_")[1].replace(".json", ""));
                        long t2 = Long.parseLong(p2.getFileName().toString().split("_")[1].replace(".json", ""));
                        return Long.compare(t1, t2);
                    })
                    .orElse(null);

            if (latestCommitFile != null) {
                String commitJson = Files.readString(latestCommitFile);
                Commit commit = deserializeCommitJson(commitJson);
                revertCommit(commit);
                Files.delete(latestCommitFile);
                GitGudPlugin.LOGGER.atInfo().log("Reverted and deleted latest commit file: " + latestCommitFile.getFileName());
            } else {
                GitGudPlugin.LOGGER.atInfo().log("No commits found to revert.");
            }
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to revert recent commit: " + e.getMessage());
        }
    }

    public static void rollback() {
        List<BlockChange> changesToRollback = getBlockChanges().reversed();
        for (BlockChange change : changesToRollback) {
            Universe.get().getDefaultWorld().setBlock(change.position.x, change.position.y, change.position.z, change.oldBlockId);
            GitGudPlugin.LOGGER.atInfo().log("Reverting block at " + change.position + " to " + change.oldBlockId);
        }
        blockChanges.removeAll(changesToRollback);
        GitGudPlugin.LOGGER.atInfo().log("Rollback completed for " + changesToRollback.size() + " changes.");
    }

    public static void stashBlockChanges() {

        if (getBlockChanges().isEmpty()) {
            GitGudPlugin.LOGGER.atInfo().log("No changes to stash.");
            return;
        }

        Path stashFile = STASH_PATH.resolve("stash_" + System.currentTimeMillis() + ".json");

        try {
            String stashJson = serializeCommitJson(new Commit("Stash", getBlockChanges(), System.currentTimeMillis()));
            Files.write(stashFile, stashJson.getBytes());
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to stash block changes: " + e.getMessage());
        }

        GitGudPlugin.LOGGER.atInfo().log("All uncommitted block changes have been stashed.");
    }

    public static void unstashBlockChanges() {

        try {
            Path latestStashFile = Files.list(STASH_PATH)
                    .filter(path -> path.getFileName().toString().startsWith("stash_"))
                    .max((p1, p2) -> {
                        long t1 = Long.parseLong(p1.getFileName().toString().split("_")[1].replace(".json", ""));
                        long t2 = Long.parseLong(p2.getFileName().toString().split("_")[1].replace(".json", ""));
                        return Long.compare(t1, t2);
                    })
                    .orElse(null);

            if (latestStashFile != null) {
                String stashJson = Files.readString(latestStashFile);
                Commit stashCommit = deserializeCommitJson(stashJson);
                for (BlockChange change : stashCommit.blockChanges) {
                    addBlockChange(change);
                }
                Files.delete(latestStashFile);
                GitGudPlugin.LOGGER.atInfo().log("Unstashed block changes from file: " + latestStashFile.getFileName());
            } else {
                GitGudPlugin.LOGGER.atInfo().log("No stashed changes found to restore.");
            }
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to unstash block changes: " + e.getMessage());
        }

        GitGudPlugin.LOGGER.atInfo().log("Stashed block changes have been restored.");
    }

    public static long getCommitCount() {
        try {
            return Files.list(COMMITS_PATH)
                    .filter(path -> path.getFileName().toString().startsWith("commit_"))
                    .count();
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to count commits: " + e.getMessage());
            return 0;
        }
    }

    public static String serializeCommitJson(Commit commit) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(commit);
    }

    public static Commit deserializeCommitJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Commit.class);
    }

    // TODO: add methods to list commits, get commit by name, etc.
}
