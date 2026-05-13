package com.duntale.dungeongen;

import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.rest.BalanceAssetExportService;
import com.duntale.dungeongen.rest.HttpRestServer;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * DungeonGen exposes a lightweight asset inspection API for balancing work.
 *
 * <h2>REST Endpoints (port 3590):</h2>
 * <pre>{@code
 *   GET /health
 *   GET /assets/summary
 *   GET /assets/weapons
 *   GET /assets/armor
 *   GET /assets/npcs
 *   GET /assets/balance-dataset
 * }</pre>
 *
 * @since 1.0.0
 */
public class DungeonGenPlugin extends JavaPlugin {

    private static final int REST_PORT = 3590;

    // ============================================
    // Fields
    // ============================================

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

        // Register asset stores for hot-reloadable JSON configs
        AssetRegistry.register(
            HytaleAssetStore.builder(
                    DungeonThemeConfig.class,
                    new IndexedLookupTableAssetMap<>(DungeonThemeConfig[]::new))
                .setPath("Configs/DungeonGen/Themes")
                .setCodec(DungeonThemeConfig.CODEC)
                .setKeyFunction(DungeonThemeConfig::getId)
                .setReplaceOnRemove(id -> null)
                .build()
        );

        AssetRegistry.register(
            HytaleAssetStore.builder(
                    DungeonSettingsConfig.class,
                    new IndexedLookupTableAssetMap<>(DungeonSettingsConfig[]::new))
                .setPath("Configs/DungeonGen/Settings")
                .setCodec(DungeonSettingsConfig.CODEC)
                .setKeyFunction(DungeonSettingsConfig::getId)
                .setReplaceOnRemove(id -> null)
                .build()
        );

        // Initialize read-only asset export API
        this.restServer = new HttpRestServer(new BalanceAssetExportService(), REST_PORT);

        getLogger().at(Level.INFO).log("[DungeonGen] Setup complete.");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("╔══════════════════════════════════════════╗");
        getLogger().at(Level.INFO).log("║     DungeonGen Asset API                 ║");
        getLogger().at(Level.INFO).log("║       Version: 1.0.0                     ║");
        getLogger().at(Level.INFO).log("║       Port: " + REST_PORT + "                          ║");
        getLogger().at(Level.INFO).log("╚══════════════════════════════════════════╝");

        // Start the REST server
        restServer.start();

        getLogger().at(Level.INFO).log("[DungeonGen] Asset API started on port %d", REST_PORT);
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[DungeonGen] Shutting down...");

        if (restServer != null) {
            restServer.stop();
        }

        getLogger().at(Level.INFO).log("[DungeonGen] Shutdown complete.");
    }
}
