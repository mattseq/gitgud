package org.mattseq.gitgud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.mattseq.gitgud.commands.*;
import org.mattseq.gitgud.trackers.BlockBreakTrackerSystem;
import org.mattseq.gitgud.trackers.BlockPlaceTrackerSystem;

import javax.annotation.Nonnull;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class GitGudPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public GitGudPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        Repository.initialize();
        Repository.unstashBlockChanges();
        this.getEntityStoreRegistry().registerSystem(new BlockBreakTrackerSystem());
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceTrackerSystem());
        this.getCommandRegistry().registerCommand(new GitCommand());
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down plugin " + this.getName());
        Repository.stashBlockChanges();
    }
}