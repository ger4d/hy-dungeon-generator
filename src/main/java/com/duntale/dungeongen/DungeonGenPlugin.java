package com.duntale.dungeongen;

import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * DungeonGen registers hot-reloadable asset stores used by dungeon generation.
 *
 * @since 1.0.0
 */
public class DungeonGenPlugin extends JavaPlugin {

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

        getLogger().at(Level.INFO).log("[DungeonGen] Setup complete.");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("[DungeonGen] Start phase complete.");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[DungeonGen] Shutting down...");

        getLogger().at(Level.INFO).log("[DungeonGen] Shutdown complete.");
    }
}
