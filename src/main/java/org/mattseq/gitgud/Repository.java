package org.mattseq.gitgud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.universe.Universe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Repository {
    private static final Path REPO_PATH = Path.of(".gitgud");
    private static final Path COMMITS_PATH = REPO_PATH.resolve("commits");
    private static final Path STASH_PATH = REPO_PATH.resolve("stash");
    private static final Path HEAD_PATH = REPO_PATH.resolve("HEAD");

    // TODO: consider using a more efficient data structure like a TreeSet
    private static final ArrayList<BlockChange> blockChanges = new ArrayList<>();

    public static ArrayList<BlockChange> getBlockChanges() {
        synchronized (blockChanges) {
            return new ArrayList<>(blockChanges);
        }
    }

    public static void addBlockChange(BlockChange change, boolean stashIfNeeded) {
        blockChanges.add(change);
        if (stashIfNeeded && blockChanges.size() > 32) {
            GitGudPlugin.LOGGER.atWarning().log("Block change count exceeded 32, stashing changes.");
            stashBlockChanges();
        }
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

        if (!Files.exists(HEAD_PATH)) {
            try {
                Files.createFile(HEAD_PATH);
                Files.writeString(HEAD_PATH, "0");
                GitGudPlugin.LOGGER.atInfo().log("Created HEAD file at " + HEAD_PATH.toAbsolutePath());
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create HEAD file: " + e.getMessage());
            }
        }
    }

    public static void saveCommit(String message) {
        long timestamp = System.currentTimeMillis();

        Path commitFile = COMMITS_PATH.resolve(timestamp + ".json.gz");

        unstashBlockChanges();

        ArrayList<BlockChange> changesToSave = getBlockChanges();
        if (changesToSave.isEmpty()) {
            GitGudPlugin.LOGGER.atInfo().log("No changes to commit.");
            return;
        }

        try {
            // sort block changes by timestamp
            changesToSave.sort(Comparator.comparingLong(a -> a.timestamp));

            // save commit
            String commitJson = serializeJson(new Commit(message, changesToSave, timestamp, getLastCommitTimestamp()));
            byte[] compressedData = gzipCompress(commitJson.getBytes());
            Files.write(commitFile, compressedData);
            setHead(timestamp);
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to save commit: " + e.getMessage());
        }

        blockChanges.removeAll(changesToSave);
        GitGudPlugin.LOGGER.atInfo().log("Commit saved with message: " + message);
    }

    public static void revertCommit(Commit commit) {

        for (BlockChange change : commit.blockChanges) {
            Universe.get().getDefaultWorld().setBlock(change.position.x, change.position.y, change.position.z, change.oldBlockId);
            GitGudPlugin.LOGGER.atInfo().log("Reverting block at " + change.position + " to " + change.oldBlockId);
        }

        try {
            Files.delete(COMMITS_PATH.resolve(commit.timestamp + ".json.gz"));
            GitGudPlugin.LOGGER.atInfo().log("Reverted and deleted latest commit with timestamp: " + commit.timestamp);
            setHead(commit.parentCommit);
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to delete latest commit file: " + e.getMessage());
        }
    }

    public static void revertLatestCommit() {
        // rollback any uncommitted changes first
        rollback();

        long lastCommitTimestamp = getLastCommitTimestamp();

        Commit lastCommit = getCommitByTimestamp(lastCommitTimestamp);

        if (lastCommit != null) {
            revertCommit(lastCommit);
        } else {
            GitGudPlugin.LOGGER.atInfo().log("No commits found to revert.");
        }
    }

    public static void rollback() {
        // first, unstash any uncommitted changes to ensure it is included in the rollback
        unstashBlockChanges();

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

        long timestamp = System.currentTimeMillis();

        Path stashFile = STASH_PATH.resolve(timestamp + ".json.gz");

        try {
            String stashJson = serializeJson(new Stash(getBlockChanges(), timestamp));
            byte[] compressedData = gzipCompress(stashJson.getBytes());
            Files.write(stashFile, compressedData);
            blockChanges.clear();
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to stash block changes: " + e.getMessage());
        }

        GitGudPlugin.LOGGER.atInfo().log("All uncommitted block changes have been stashed.");
    }

    public static void unstashBlockChanges() {

        try {
            List<Path> stashFiles = Files.list(STASH_PATH).toList();
            for (Path file: stashFiles) {
                GitGudPlugin.LOGGER.atInfo().log("Found stash file: " + file.getFileName());

                byte[] stashJson = Files.readAllBytes(file);
                Stash stashCommit = deserializeJson(new String(gzipDecompress(stashJson)), Stash.class);
                for (BlockChange change : stashCommit.blockChanges) {
                    addBlockChange(change, false);
                }
                Files.delete(file);
                GitGudPlugin.LOGGER.atInfo().log("Unstashed block changes from file: " + file.getFileName());
            }

            // sort block changes by timestamp after unstashing
            blockChanges.sort(Comparator.comparingLong(a -> a.timestamp));

            GitGudPlugin.LOGGER.atInfo().log("Stashed block changes have been restored.");

        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to unstash block changes: " + e.getMessage());
        }
    }

    public static void setHead(long timestamp) {
        try {
            Files.writeString(HEAD_PATH, String.valueOf(timestamp));
            GitGudPlugin.LOGGER.atInfo().log("HEAD updated to timestamp: " + timestamp);
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to update HEAD: " + e.getMessage());
        }
    }

    public static long getLastCommitTimestamp() {
        try {
            String headContent = Files.readString(HEAD_PATH).trim();
            return headContent.isEmpty() ? 0 : Long.parseLong(headContent);
        } catch (IOException | NumberFormatException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to read HEAD: " + e.getMessage());
            return 0;
        }
    }

    public static Commit getCommitByTimestamp(long timestamp) {
        Path commitFile = COMMITS_PATH.resolve(timestamp + ".json.gz");
        if (Files.exists(commitFile)) {
            try {
                byte[] commitJson = Files.readAllBytes(commitFile);
                String decompressedJson = new String(gzipDecompress(commitJson));
                return deserializeJson(decompressedJson, Commit.class);
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to read commit file: " + e.getMessage());
                return null;
            }
        } else {
            GitGudPlugin.LOGGER.atInfo().log("No commit found with timestamp: " + timestamp);
            return null;
        }
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

    public static String serializeJson(Object obj) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }

    public static <T> T deserializeJson(String json, Class<T> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(json, clazz);
    }

    public static byte[] gzipCompress(byte[] data) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data);
            gzipOutputStream.finish();
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to compress data: " + e.getMessage());
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gzipDecompress(byte[] compressedData) {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            return gzipInputStream.readAllBytes();
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to decompress data: " + e.getMessage());
            return new byte[0];
        }
    }

    // TODO: add methods to list commits, get commit by name, etc.
}
