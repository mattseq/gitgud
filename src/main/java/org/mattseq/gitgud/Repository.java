package org.mattseq.gitgud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mattseq.gitgud.dto.BlockChange;
import org.mattseq.gitgud.dto.Commit;
import org.mattseq.gitgud.dto.Stash;
import org.mattseq.gitgud.dto.Tag;
import org.mattseq.gitgud.trackers.WorldEditApplySystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Repository {
    private static final Path REPO_PATH = Path.of(".gitgud");
    private static final Path COMMITS_PATH = REPO_PATH.resolve("commits");
    private static final Path STASH_PATH = REPO_PATH.resolve("stash");
    private static final Path HEAD_PATH = REPO_PATH.resolve("HEAD");
    private static final Path CURRENT_PATH = REPO_PATH.resolve("CURRENT");
    private static final Path TAGS_PATH = REPO_PATH.resolve("tags");

    // TODO: consider using a more efficient data structure like a TreeSet
    private static final ArrayList<BlockChange> blockChanges = new ArrayList<>();

    public static final class ActionResult {
        public final boolean success;
        public final String message;

        private ActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ActionResult success(String message) {
            return new ActionResult(true, message);
        }

        public static ActionResult failure(String message) {
            return new ActionResult(false, message);
        }
    }

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

        if (!Files.exists(CURRENT_PATH)) {
            try {
                Files.createFile(CURRENT_PATH);
                Files.writeString(CURRENT_PATH, String.valueOf(getHeadTimestamp()));
                GitGudPlugin.LOGGER.atInfo().log("Created CURRENT file at " + CURRENT_PATH.toAbsolutePath());
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create CURRENT file: " + e.getMessage());
            }
        }

        if (!Files.exists(TAGS_PATH)) {
            try {
                Files.createDirectory(TAGS_PATH);
                GitGudPlugin.LOGGER.atInfo().log("Created tags directory at " + TAGS_PATH.toAbsolutePath());
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to create tags directory: " + e.getMessage());
            }
        }
    }

    public static ActionResult saveCommit(String message) {
        if (isDetached()) {
            String failureMessage = "Cannot commit while detached. Checkout HEAD first so HEAD == CURRENT.";
            GitGudPlugin.LOGGER.atWarning().log(failureMessage);
            return ActionResult.failure(failureMessage);
        }

        long timestamp = System.currentTimeMillis();

        Path commitFile = COMMITS_PATH.resolve(timestamp + ".json.gz");

        unstashBlockChanges();

        ArrayList<BlockChange> changesToSave = getBlockChanges();
        if (changesToSave.isEmpty()) {
            String noChangesMessage = "No changes to commit.";
            GitGudPlugin.LOGGER.atInfo().log(noChangesMessage);
            return ActionResult.failure(noChangesMessage);
        }

        try {
            // sort block changes by timestamp
            changesToSave.sort(Comparator.comparingLong(a -> a.timestamp));

            // save commit
            String commitJson = serializeJson(new Commit(message, changesToSave, timestamp, getLastCommitTimestamp()));
            byte[] compressedData = gzipCompress(commitJson.getBytes());
            Files.write(commitFile, compressedData);
            setHead(timestamp);
            setCurrent(timestamp);
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to save commit: " + e.getMessage());
            return ActionResult.failure("Failed to save commit. Check logs for details.");
        }

        blockChanges.removeAll(changesToSave);
        String successMessage = "Commit saved with message: " + message;
        GitGudPlugin.LOGGER.atInfo().log(successMessage);
        return ActionResult.success(successMessage);
    }

    public static void revertCommit(Commit commit) {
        if (commit == null) {
            GitGudPlugin.LOGGER.atInfo().log("No commit found to revert.");
            return;
        }

        applyCommitBackward(commit);

        setCurrent(commit.parentCommit);
        GitGudPlugin.LOGGER.atInfo().log("Reverted commit " + commit.timestamp + " without deleting commit file.");
    }

    public static void applyCommit(Commit commit) {
        if (commit == null) {
            GitGudPlugin.LOGGER.atInfo().log("No commit found to apply.");
            return;
        }

        applyCommitForward(commit);

        setCurrent(commit.timestamp);
        GitGudPlugin.LOGGER.atInfo().log("Applied commit " + commit.timestamp + ".");
    }

    public static ActionResult revertLatestCommit() {
        if (isDetached()) {
            String failureMessage = "Cannot revert while detached. Checkout HEAD first so HEAD == CURRENT.";
            GitGudPlugin.LOGGER.atWarning().log(failureMessage);
            return ActionResult.failure(failureMessage);
        }

        // rollback any uncommitted changes first
        rollback();

        long lastCommitTimestamp = getCurrentTimestamp();

        Commit lastCommit = getCommitByTimestamp(lastCommitTimestamp);

        if (lastCommit != null) {
            revertCommit(lastCommit);
            // delete commit file after reverting
            Path commitFile = COMMITS_PATH.resolve(lastCommit.timestamp + ".json.gz");
            setHead(lastCommit.parentCommit);
            try {
                Files.deleteIfExists(commitFile);
                GitGudPlugin.LOGGER.atInfo().log("Deleted commit file for timestamp: " + lastCommit.timestamp);
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to delete commit file: " + e.getMessage());
            }
            return ActionResult.success("Reverted commit " + lastCommit.timestamp + ".");
        } else {
            String noCommitMessage = "No commits found to revert.";
            GitGudPlugin.LOGGER.atInfo().log(noCommitMessage);
            return ActionResult.failure(noCommitMessage);
        }
    }

    // TODO: support checking out by tag name and HEAD or TAIL
    public static ActionResult checkoutCommit(int targetIndex) {
        Map<Long, Commit> commitHistory = getCommitHistoryMap();
        if (targetIndex < 0 || targetIndex >= commitHistory.size()) {
            GitGudPlugin.LOGGER.atInfo().log("Invalid commit index: " + targetIndex);
            return ActionResult.failure("Invalid commit index: " + targetIndex);
        }
        long targetTimestamp = new ArrayList<>(commitHistory.keySet()).get(targetIndex);
        return checkoutCommit(getCommitByTimestamp(targetTimestamp));
    }

    public static ActionResult checkoutCommit(Commit targetCommit) {
        if (targetCommit == null) {
            GitGudPlugin.LOGGER.atInfo().log("Target commit not found.");
            return ActionResult.failure("Target commit not found.");
        }

        rollback();

        Map<Long, Commit> commitHistory = getCommitHistoryMap();
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp == targetCommit.timestamp) {
            GitGudPlugin.LOGGER.atInfo().log("Already on commit " + targetCommit.timestamp + ".");
            return ActionResult.failure("Already on target commit.");
        }

        int currentIndex = getCommitIndex(commitHistory, currentTimestamp);
        int targetIndex = getCommitIndex(commitHistory, targetCommit.timestamp);

        if (currentIndex == -1 || targetIndex == -1) {
            GitGudPlugin.LOGGER.atWarning().log("Cannot checkout. CURRENT or target is not in HEAD commit chain.");
            return ActionResult.failure("Cannot checkout. CURRENT or target is not in commit chain.");
        }

        List<Commit> chain = new ArrayList<>(commitHistory.values());

        if (currentIndex < targetIndex) {
            for (int i = currentIndex; i < targetIndex; i++) {
                revertCommit(chain.get(i));
            }
        } else {
            for (int i = currentIndex -1; i >= targetIndex; i--) {
                applyCommit(chain.get(i));
            }
        }

        return ActionResult.success("Checked out commit " + targetCommit.timestamp + ".");
    }

    public static ActionResult rollback() {
        // first, unstash any uncommitted changes to ensure it is included in the rollback
        unstashBlockChanges();

        List<BlockChange> changesToRollback = getBlockChanges().reversed();
        for (BlockChange change : changesToRollback) {
            WorldEditApplySystem.enqueue(change.position.x, change.position.y, change.position.z, change.oldBlockId);
            GitGudPlugin.LOGGER.atInfo().log("Reverting block at " + change.position + " to " + change.oldBlockId);
        }
        blockChanges.removeAll(changesToRollback);
        return ActionResult.success("Rolled back " + changesToRollback.size() + " uncommitted block changes.");
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

    public static ActionResult addTagToLatestCommit(String tagName, String description) {
        long lastCommitTimestamp = getLastCommitTimestamp();
        if (lastCommitTimestamp == 0) {
            GitGudPlugin.LOGGER.atInfo().log("No commits found to tag.");
            return ActionResult.failure("No commits found to tag.");
        }
        return addTag(tagName, description, lastCommitTimestamp);
    }

    public static ActionResult addTag(String tagName, String description, long commitTimestamp) {
        Path tagFile = TAGS_PATH.resolve(tagName);
        try {
            Tag tag = new Tag(tagName, description, commitTimestamp);
            String tagJson = serializeJson(tag);
            Files.writeString(tagFile, tagJson);
            return ActionResult.success("Tag '" + tagName + "' added to commit " + commitTimestamp + ".");
        } catch (IOException e) {
            return ActionResult.failure("Failed to add tag: " + e.getMessage());
        }
    }

    public static ActionResult deleteTag(String tagName) {
        Path tagFile = TAGS_PATH.resolve(tagName);
        if (!Files.exists(tagFile)) {
            return ActionResult.failure("No tag found with name: " + tagName);
        }
        try {
            Files.delete(tagFile);
            return ActionResult.success("Tag '" + tagName + "' deleted successfully.");
        } catch (IOException e) {
            return ActionResult.failure("Failed to delete tag: " + e.getMessage());
        }
    }

    public static Tag getTag(String tagName) {
        Path tagFile = TAGS_PATH.resolve(tagName);
        if (Files.exists(tagFile)) {
            try {
                String tagJson = Files.readString(tagFile);
                return deserializeJson(tagJson, Tag.class);
            } catch (IOException e) {
                GitGudPlugin.LOGGER.atWarning().log("Failed to read tag file: " + e.getMessage());
                return null;
            }
        } else {
            GitGudPlugin.LOGGER.atInfo().log("No tag found with name: " + tagName);
            return null;
        }
    }

    public static List<Tag> listTags() {
        List<Tag> tags = new ArrayList<>();
        try {
            List<Path> tagFiles = Files.list(TAGS_PATH).toList();
            for (Path tagFile : tagFiles) {
                String tagJson = Files.readString(tagFile);
                Tag tag = deserializeJson(tagJson, Tag.class);
                tags.add(tag);
            }
            GitGudPlugin.LOGGER.atInfo().log("Listed " + tags.size() + " tags.");
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to list tags: " + e.getMessage());
        }
        return tags;
    }

    public static long getLastCommitTimestamp() {
        return getHeadTimestamp();
    }

    public static long getHeadTimestamp() {
        try {
            String headContent = Files.readString(HEAD_PATH).trim();
            return headContent.isEmpty() ? 0 : Long.parseLong(headContent);
        } catch (IOException | NumberFormatException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to read HEAD: " + e.getMessage());
            return 0;
        }
    }

    public static long getCurrentTimestamp() {
        try {
            String currentContent = Files.readString(CURRENT_PATH).trim();
            return currentContent.isEmpty() ? 0 : Long.parseLong(currentContent);
        } catch (IOException | NumberFormatException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to read CURRENT: " + e.getMessage());
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

    public static List<Commit> getCommitHistory(int limit) {
        ArrayList<Commit> history = new ArrayList<>();
        if (limit <= 0) {
            GitGudPlugin.LOGGER.atInfo().log("Invalid commit history limit: " + limit);
            return history;
        }

        long lastCommitTimestamp = getLastCommitTimestamp();
        while (history.size() < limit && lastCommitTimestamp > 0) {
            Commit commit = getCommitByTimestamp(lastCommitTimestamp);
            if (commit != null) {
                history.add(commit);
                lastCommitTimestamp = commit.parentCommit;
            } else {
                break;
            }
        }
        GitGudPlugin.LOGGER.atInfo().log("Retrieved commit history with " + history.size() + " commits");
        return history;
    }

    public static Map<Long, Commit> getCommitHistoryMap() {
        LinkedHashMap<Long, Commit> history = new LinkedHashMap<>();
        for (Commit commit : getCommitHistory()) {
            history.put(commit.timestamp, commit);
        }
        return history;
    }

    public static List<Commit> getCommitHistory() {
        return getCommitHistory(Integer.MAX_VALUE);
    }

    public static void setHead(long timestamp) {
        try {
            Files.writeString(HEAD_PATH, String.valueOf(timestamp));
            GitGudPlugin.LOGGER.atInfo().log("HEAD updated to timestamp: " + timestamp);
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to update HEAD: " + e.getMessage());
        }
    }

    public static void setCurrent(long timestamp) {
        try {
            Files.writeString(CURRENT_PATH, String.valueOf(timestamp));
            GitGudPlugin.LOGGER.atInfo().log("CURRENT updated to timestamp: " + timestamp);
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to update CURRENT: " + e.getMessage());
        }
    }

    private static boolean isDetached() {
        return getHeadTimestamp() != getCurrentTimestamp();
    }

    private static int getCommitIndex(Map<Long, Commit> chain, long timestamp) {
        int index = 0;
        for (Long commitTimestamp : chain.keySet()) {
            if (commitTimestamp == timestamp) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static void applyCommitBackward(Commit commit) {
        List<BlockChange> changes = commit.blockChanges.reversed();
        for (BlockChange change : changes) {
            WorldEditApplySystem.enqueue(change.position.x, change.position.y, change.position.z, change.oldBlockId);
            GitGudPlugin.LOGGER.atInfo().log("Reverting block at " + change.position + " to " + change.oldBlockId);
        }
    }

    private static void applyCommitForward(Commit commit) {
        for (BlockChange change : commit.blockChanges) {
            WorldEditApplySystem.enqueue(change.position.x, change.position.y, change.position.z, change.newBlockId);
            GitGudPlugin.LOGGER.atInfo().log("Applying block at " + change.position + " to " + change.newBlockId);
        }
    }

    public static long getStashCount() {
        try {
            return Files.list(STASH_PATH).count();
        } catch (IOException e) {
            GitGudPlugin.LOGGER.atWarning().log("Failed to count stash files: " + e.getMessage());
            return 0;
        }
    }

    public static long getCommitCount() {
        return getCommitHistory().size();
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
}
