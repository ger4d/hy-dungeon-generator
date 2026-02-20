package com.duntale.dungeongen;

import com.duntale.dungeongen.generator.GenerationOrchestrator;
import com.duntale.dungeongen.rest.HttpRestServer;
import com.duntale.dungeongen.util.BlockResolver;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * DungeonGen — a procedural dungeon generator plugin for the Hytale
 * Dedicated Server, exposing a REST API for on-demand dungeon creation.
 *
 * <h2>REST Endpoints (port 3590):</h2>
 * <pre>{@code
 *   GET  /health   → server health check
 *   POST /generate → generate a dungeon from configuration JSON
 * }</pre>
 *
 * @since 1.0.0
 */
public class DungeonGenPlugin extends JavaPlugin {

    private static final int REST_PORT = 3590;

    // ============================================
    // Fields
    // ============================================

    private BlockResolver blockResolver;
    private GenerationOrchestrator orchestrator;
    private HttpRestServer restServer;

    // ============================================
    // Constructor
    // ============================================

    public DungeonGenPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        getLogger().at(Level.INFO).log("[DungeonGen] Plugin JAR loaded!");
    }

    // ============================================
    // Plugin Lifecycle
    // ============================================

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[DungeonGen] Setup phase...");

        // Initialize block resolver for string→int block ID conversion
        this.blockResolver = new BlockResolver();

        // Initialize generation orchestrator with block resolver for assembly
        this.orchestrator = new GenerationOrchestrator(blockResolver);

        // Initialize REST server wired to the orchestrator
        this.restServer = new HttpRestServer(orchestrator, REST_PORT);

        getLogger().at(Level.INFO).log("[DungeonGen] Setup complete.");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("╔══════════════════════════════════════════╗");
        getLogger().at(Level.INFO).log("║     DungeonGen REST Server               ║");
        getLogger().at(Level.INFO).log("║       Version: 1.0.0                     ║");
        getLogger().at(Level.INFO).log("║       Port: " + REST_PORT + "                          ║");
        getLogger().at(Level.INFO).log("╚══════════════════════════════════════════╝");

        // Start the REST server
        restServer.start();

        getLogger().at(Level.INFO).log("[DungeonGen] REST server started on port %d", REST_PORT);
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[DungeonGen] Shutting down...");

        if (restServer != null) {
            restServer.stop();
        }

        if (orchestrator != null) {
            orchestrator.shutdown();
        }

        getLogger().at(Level.INFO).log("[DungeonGen] Shutdown complete.");
    }
}
