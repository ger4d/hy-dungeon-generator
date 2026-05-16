package com.duntale.dungeongen;

import com.duntale.dungeongen.assembly.DungeonAssemblyService;
import com.duntale.dungeongen.command.DungenCommand;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.generator.GenerationOrchestrator;
import com.duntale.dungeongen.util.BlockResolver;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * DungeonGen registers hot-reloadable asset stores and exposes the standalone
 * dungeon generation flow (orchestrator, assembly service, and {@code /dungen}
 * command).
 *
 * @since 1.0.0
 */
public class DungeonGenPlugin extends JavaPlugin {

    private static DungeonGenPlugin instance;

    private GenerationOrchestrator orchestrator;
    private DungeonAssemblyService assemblyService;

    // ============================================
    // Constructor
    // ============================================

    public DungeonGenPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        getLogger().at(Level.INFO).log("[DungeonGen] Plugin JAR loaded!");
    }

    /**
     * Returns the active plugin instance.
     *
     * @return the active plugin instance, or {@code null} before construction
     */
    @Nullable
    public static DungeonGenPlugin get() {
        return instance;
    }

    // ============================================
    // Plugin Lifecycle
    // ============================================

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[DungeonGen] Setup phase...");

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

        this.getCommandRegistry().registerCommand(new DungenCommand());

        getLogger().at(Level.INFO).log("[DungeonGen] Setup complete.");
    }

    @Override
    protected void start() {
        this.orchestrator = new GenerationOrchestrator(new BlockResolver());
        this.assemblyService = new DungeonAssemblyService(orchestrator);
        getLogger().at(Level.INFO).log("[DungeonGen] Start phase complete.");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[DungeonGen] Shutting down...");
        if (orchestrator != null) {
            orchestrator.shutdown();
            orchestrator = null;
        }
        assemblyService = null;
        getLogger().at(Level.INFO).log("[DungeonGen] Shutdown complete.");
    }

    // ============================================
    // Accessors
    // ============================================

    /**
     * Returns the shared dungeon generation orchestrator.
     *
     * @return the orchestrator, or {@code null} before {@link #start()} runs
     */
    @Nullable
    public GenerationOrchestrator getOrchestrator() {
        return orchestrator;
    }

    /**
     * Returns the shared assembly service facade.
     *
     * @return the assembly service, or {@code null} before {@link #start()} runs
     */
    @Nullable
    public DungeonAssemblyService getAssemblyService() {
        return assemblyService;
    }
}
