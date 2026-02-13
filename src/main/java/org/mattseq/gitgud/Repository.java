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
    }

    public static void saveCommit(String message) {
        long timestamp = System.currentTimeMillis();

        // TODO: consider using hashes and storing parent commits, also for stash files; would need HEAD pointers as well
        Path commitFile = COMMITS_PATH.resolve("commit_" + timestamp + ".json.gz");

        unstashBlockChanges();

        ArrayList<BlockChange> changesToSave = getBlockChanges();
        if (changesToSave.isEmpty()) {
            GitGudPlugin.LOGGER.atInfo().log("No changes to commit.");
            return;
        }

        try {
            changesToSave.sort(Comparator.comparingLong(a -> a.timestamp));

            String commitJson = serializeCommitJson(new Commit(message, changesToSave, timestamp));
            byte[] compressedData = gzipCompress(commitJson.getBytes());
            Files.write(commitFile, compressedData);
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
        // TODO: rollback current changes first
        try {
            Path latestCommitFile = Files.list(COMMITS_PATH)
                    .filter(path -> path.getFileName().toString().startsWith("commit_"))
                    .max((p1, p2) -> {
                        long t1 = Long.parseLong(p1.getFileName().toString().split("_")[1].replace(".json.gz", ""));
                        long t2 = Long.parseLong(p2.getFileName().toString().split("_")[1].replace(".json.gz", ""));
                        return Long.compare(t1, t2);
                    })
                    .orElse(null);

            if (latestCommitFile != null) {
                byte[] commitJson = Files.readAllBytes(latestCommitFile);
                String decompressedJson = new String(gzipDecompress(commitJson));
                Commit commit = deserializeCommitJson(decompressedJson);
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

        Path stashFile = STASH_PATH.resolve("stash_" + System.currentTimeMillis() + ".json.gz");

        try {
            String stashJson = serializeCommitJson(new Commit("Stash", getBlockChanges(), System.currentTimeMillis()));
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
                Commit stashCommit = deserializeCommitJson(new String(gzipDecompress(stashJson)));
                for (BlockChange change : stashCommit.blockChanges) {
                    addBlockChange(change, false);
                }
                Files.delete(file);
                GitGudPlugin.LOGGER.atInfo().log("Unstashed block changes from file: " + file.getFileName());
            }

            GitGudPlugin.LOGGER.atInfo().log("Stashed block changes have been restored.");

        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to unstash block changes: " + e.getMessage());
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

    public static String serializeCommitJson(Commit commit) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(commit);
    }

    public static Commit deserializeCommitJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Commit.class);
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
