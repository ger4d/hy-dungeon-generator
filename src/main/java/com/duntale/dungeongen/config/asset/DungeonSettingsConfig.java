package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Global dungeon generation settings, loaded from a single JSON asset file.
 *
 * <p>Asset path: {@code Configs/DungeonGen/Settings/}</p>
 * <p>Expected file: {@code generation.json}</p>
 *
 * <p>Contains all global tuning knobs for the generation pipeline:
 * complexity scaling, feature placement thresholds, lighting parameters,
 * spawn formulas, and decay multipliers.</p>
 *
 * @since 1.3.0
 */
public class DungeonSettingsConfig
        implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, DungeonSettingsConfig>> {

    // ============================================
    // Static asset machinery
    // ============================================

    public static AssetBuilderCodec<String, DungeonSettingsConfig> CODEC;
    private static AssetStore<String, DungeonSettingsConfig,
            IndexedLookupTableAssetMap<String, DungeonSettingsConfig>> ASSET_STORE;

    private static final String DEFAULT_ID = "generation";

    // ============================================
    // Fields — populated by CODEC from JSON
    // ============================================

    protected String id;
    protected AssetExtraInfo.Data data;

    // --- Complexity ---
    protected double complexityBase = 0.3;
    protected double complexityMultiplier = 1.4;
    protected double trapDensityCap = 0.5;
    protected double secretWallCap = 0.3;

    // --- Traps ---
    protected double trapDensityMultiplier = 0.05;
    protected double floorTrapChance = 0.35;
    protected double wallSpikeChance = 0.4;

    // --- Pillars ---
    protected int pillarMinRoomSize = 8;
    protected double pillarSkipChance = 0.15;

    // --- Decay ---
    protected double overgrowthMultiplier = 0.3;
    protected double rubbleMultiplier = 0.1;
    protected double floodScanFraction = 0.333;
    protected double erosionMultiplier = 0.3;
    protected int erosionMinExposedFaces = 2;

    // --- Lighting ---
    protected int minWallTorchSpacing = 3;
    protected int minFloorTorchSpacing = 4;
    protected int roomTorchYOffset = 2;
    protected int corridorTorchYOffset = 3;
    protected int ceilingLightMinRoomWidth = 8;
    protected int floorLightMinRoomSize = 5;
    protected int bossAccentMinRoomSize = 8;
    protected int corridorLightDensity = 8;

    // --- Spawns — elite budget (sigmoid model) ---
    protected double eliteRatioMin = 0.05;
    protected double eliteRatioMax = 0.30;
    protected double eliteRatioMidpoint = 30.0;
    protected double eliteRatioSteepness = 0.12;
    protected double eliteRoomWeightExponent = 1.0;
    protected int maxElitesPerCombatRoom = 4;
    protected int bossRoomEliteMax = 2;
    protected int bossSpawnBase = 3;
    protected int bossSpawnAreaDivisor = 30;
    protected int combatSpawnBase = 1;
    protected int combatSpawnAreaDivisor = 40;
    protected int spawnAttemptMultiplier = 5;
    protected int spawnInteriorInset = 2;

    // --- Assembly ---
    protected int blocksPerBatch = 1000;
    protected String fluidFallbackBlock = "Rock_Stone_Brick";

    // Spawner visual marker block (null or empty = invisible)
    protected String spawnerBlock = "Furniture_Temple_Scarak_Window";

    /** No-arg constructor required by codec. */
    public DungeonSettingsConfig() {}

    // ============================================
    // CODEC definition
    // ============================================

    static {
        CODEC = AssetBuilderCodec.builder(
                DungeonSettingsConfig.class,
                DungeonSettingsConfig::new,
                Codec.STRING,
                (t, k) -> t.id = k,
                t -> t.id,
                (asset, d) -> asset.data = d,
                asset -> asset.data
            )
            // Complexity
            .append(new KeyedCodec<>("ComplexityBase", Codec.DOUBLE),
                (c, v) -> c.complexityBase = v, c -> c.complexityBase).add()
            .append(new KeyedCodec<>("ComplexityMultiplier", Codec.DOUBLE),
                (c, v) -> c.complexityMultiplier = v, c -> c.complexityMultiplier).add()
            .append(new KeyedCodec<>("TrapDensityCap", Codec.DOUBLE),
                (c, v) -> c.trapDensityCap = v, c -> c.trapDensityCap).add()
            .append(new KeyedCodec<>("SecretWallCap", Codec.DOUBLE),
                (c, v) -> c.secretWallCap = v, c -> c.secretWallCap).add()
            // Traps
            .append(new KeyedCodec<>("TrapDensityMultiplier", Codec.DOUBLE),
                (c, v) -> c.trapDensityMultiplier = v, c -> c.trapDensityMultiplier).add()
            .append(new KeyedCodec<>("FloorTrapChance", Codec.DOUBLE),
                (c, v) -> c.floorTrapChance = v, c -> c.floorTrapChance).add()
            .append(new KeyedCodec<>("WallSpikeChance", Codec.DOUBLE),
                (c, v) -> c.wallSpikeChance = v, c -> c.wallSpikeChance).add()
            // Pillars
            .append(new KeyedCodec<>("PillarMinRoomSize", Codec.INTEGER),
                (c, v) -> c.pillarMinRoomSize = v, c -> c.pillarMinRoomSize).add()
            .append(new KeyedCodec<>("PillarSkipChance", Codec.DOUBLE),
                (c, v) -> c.pillarSkipChance = v, c -> c.pillarSkipChance).add()
            // Decay
            .append(new KeyedCodec<>("OvergrowthMultiplier", Codec.DOUBLE),
                (c, v) -> c.overgrowthMultiplier = v, c -> c.overgrowthMultiplier).add()
            .append(new KeyedCodec<>("RubbleMultiplier", Codec.DOUBLE),
                (c, v) -> c.rubbleMultiplier = v, c -> c.rubbleMultiplier).add()
            .append(new KeyedCodec<>("FloodScanFraction", Codec.DOUBLE),
                (c, v) -> c.floodScanFraction = v, c -> c.floodScanFraction).add()
            .append(new KeyedCodec<>("ErosionMultiplier", Codec.DOUBLE),
                (c, v) -> c.erosionMultiplier = v, c -> c.erosionMultiplier).add()
            .append(new KeyedCodec<>("ErosionMinExposedFaces", Codec.INTEGER),
                (c, v) -> c.erosionMinExposedFaces = v, c -> c.erosionMinExposedFaces).add()
            // Lighting
            .append(new KeyedCodec<>("MinWallTorchSpacing", Codec.INTEGER),
                (c, v) -> c.minWallTorchSpacing = v, c -> c.minWallTorchSpacing).add()
            .append(new KeyedCodec<>("MinFloorTorchSpacing", Codec.INTEGER),
                (c, v) -> c.minFloorTorchSpacing = v, c -> c.minFloorTorchSpacing).add()
            .append(new KeyedCodec<>("RoomTorchYOffset", Codec.INTEGER),
                (c, v) -> c.roomTorchYOffset = v, c -> c.roomTorchYOffset).add()
            .append(new KeyedCodec<>("CorridorTorchYOffset", Codec.INTEGER),
                (c, v) -> c.corridorTorchYOffset = v, c -> c.corridorTorchYOffset).add()
            .append(new KeyedCodec<>("CeilingLightMinRoomWidth", Codec.INTEGER),
                (c, v) -> c.ceilingLightMinRoomWidth = v, c -> c.ceilingLightMinRoomWidth).add()
            .append(new KeyedCodec<>("FloorLightMinRoomSize", Codec.INTEGER),
                (c, v) -> c.floorLightMinRoomSize = v, c -> c.floorLightMinRoomSize).add()
            .append(new KeyedCodec<>("BossAccentMinRoomSize", Codec.INTEGER),
                (c, v) -> c.bossAccentMinRoomSize = v, c -> c.bossAccentMinRoomSize).add()
            .append(new KeyedCodec<>("CorridorLightDensity", Codec.INTEGER),
                (c, v) -> c.corridorLightDensity = v, c -> c.corridorLightDensity).add()
            // Spawns — elite budget (sigmoid model)
            .append(new KeyedCodec<>("EliteRatioMin", Codec.DOUBLE),
                (c, v) -> c.eliteRatioMin = v, c -> c.eliteRatioMin).add()
            .append(new KeyedCodec<>("EliteRatioMax", Codec.DOUBLE),
                (c, v) -> c.eliteRatioMax = v, c -> c.eliteRatioMax).add()
            .append(new KeyedCodec<>("EliteRatioMidpoint", Codec.DOUBLE),
                (c, v) -> c.eliteRatioMidpoint = v, c -> c.eliteRatioMidpoint).add()
            .append(new KeyedCodec<>("EliteRatioSteepness", Codec.DOUBLE),
                (c, v) -> c.eliteRatioSteepness = v, c -> c.eliteRatioSteepness).add()
            .append(new KeyedCodec<>("EliteRoomWeightExponent", Codec.DOUBLE),
                (c, v) -> c.eliteRoomWeightExponent = v, c -> c.eliteRoomWeightExponent).add()
            .append(new KeyedCodec<>("MaxElitesPerCombatRoom", Codec.INTEGER),
                (c, v) -> c.maxElitesPerCombatRoom = v, c -> c.maxElitesPerCombatRoom).add()
            .append(new KeyedCodec<>("BossRoomEliteMax", Codec.INTEGER),
                (c, v) -> c.bossRoomEliteMax = v, c -> c.bossRoomEliteMax).add()
            .append(new KeyedCodec<>("BossSpawnBase", Codec.INTEGER),
                (c, v) -> c.bossSpawnBase = v, c -> c.bossSpawnBase).add()
            .append(new KeyedCodec<>("BossSpawnAreaDivisor", Codec.INTEGER),
                (c, v) -> c.bossSpawnAreaDivisor = v, c -> c.bossSpawnAreaDivisor).add()
            .append(new KeyedCodec<>("CombatSpawnBase", Codec.INTEGER),
                (c, v) -> c.combatSpawnBase = v, c -> c.combatSpawnBase).add()
            .append(new KeyedCodec<>("CombatSpawnAreaDivisor", Codec.INTEGER),
                (c, v) -> c.combatSpawnAreaDivisor = v, c -> c.combatSpawnAreaDivisor).add()
            .append(new KeyedCodec<>("SpawnAttemptMultiplier", Codec.INTEGER),
                (c, v) -> c.spawnAttemptMultiplier = v, c -> c.spawnAttemptMultiplier).add()
            .append(new KeyedCodec<>("SpawnInteriorInset", Codec.INTEGER),
                (c, v) -> c.spawnInteriorInset = v, c -> c.spawnInteriorInset).add()
            // Assembly
            .append(new KeyedCodec<>("BlocksPerBatch", Codec.INTEGER),
                (c, v) -> c.blocksPerBatch = v, c -> c.blocksPerBatch).add()
            .append(new KeyedCodec<>("FluidFallbackBlock", Codec.STRING),
                (c, v) -> c.fluidFallbackBlock = v, c -> c.fluidFallbackBlock).add()
            .append(new KeyedCodec<>("SpawnerBlock", Codec.STRING),
                (c, v) -> c.spawnerBlock = v, c -> c.spawnerBlock).add()
            .build();
    }

    // ============================================
    // Static accessors
    // ============================================

    /**
     * Get the default generation settings.
     *
     * @return the settings config, or a default instance if not loaded
     */
    @Nonnull
    public static DungeonSettingsConfig getDefault() {
        DungeonSettingsConfig config = get(DEFAULT_ID);
        return config != null ? config : new DungeonSettingsConfig();
    }

    /**
     * @param id the settings ID
     * @return the settings config, or {@code null} if not loaded
     */
    @Nullable
    public static DungeonSettingsConfig get(@Nonnull String id) {
        return getAssetMap().getAsset(id);
    }

    /**
     * @return the underlying asset store (lazily resolved from the registry).
     */
    @Nonnull
    public static AssetStore<String, DungeonSettingsConfig,
            IndexedLookupTableAssetMap<String, DungeonSettingsConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(DungeonSettingsConfig.class);
        }
        return ASSET_STORE;
    }

    /**
     * @return the asset map containing all loaded settings configs.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static IndexedLookupTableAssetMap<String, DungeonSettingsConfig> getAssetMap() {
        return (IndexedLookupTableAssetMap<String, DungeonSettingsConfig>) getAssetStore().getAssetMap();
    }

    // ============================================
    // JsonAssetWithMap implementation
    // ============================================

    @Override
    public String getId() {
        return id;
    }

    // ============================================
    // Getters — Complexity
    // ============================================

    public double getComplexityBase() { return complexityBase; }
    public double getComplexityMultiplier() { return complexityMultiplier; }
    public double getTrapDensityCap() { return trapDensityCap; }
    public double getSecretWallCap() { return secretWallCap; }

    // ============================================
    // Getters — Traps
    // ============================================

    public double getTrapDensityMultiplier() { return trapDensityMultiplier; }
    public double getFloorTrapChance() { return floorTrapChance; }
    public double getWallSpikeChance() { return wallSpikeChance; }

    // ============================================
    // Getters — Pillars
    // ============================================

    public int getPillarMinRoomSize() { return pillarMinRoomSize; }
    public double getPillarSkipChance() { return pillarSkipChance; }

    // ============================================
    // Getters — Decay
    // ============================================

    public double getOvergrowthMultiplier() { return overgrowthMultiplier; }
    public double getRubbleMultiplier() { return rubbleMultiplier; }
    public double getFloodScanFraction() { return floodScanFraction; }
    public double getErosionMultiplier() { return erosionMultiplier; }
    public int getErosionMinExposedFaces() { return erosionMinExposedFaces; }

    // ============================================
    // Getters — Lighting
    // ============================================

    public int getMinWallTorchSpacing() { return minWallTorchSpacing; }
    public int getMinFloorTorchSpacing() { return minFloorTorchSpacing; }
    public int getRoomTorchYOffset() { return roomTorchYOffset; }
    public int getCorridorTorchYOffset() { return corridorTorchYOffset; }
    public int getCeilingLightMinRoomWidth() { return ceilingLightMinRoomWidth; }
    public int getFloorLightMinRoomSize() { return floorLightMinRoomSize; }
    public int getBossAccentMinRoomSize() { return bossAccentMinRoomSize; }
    public int getCorridorLightDensity() { return corridorLightDensity; }

    // ============================================
    // Getters — Spawns
    // ============================================

    public double getEliteRatioMin() { return eliteRatioMin; }
    public double getEliteRatioMax() { return eliteRatioMax; }
    public double getEliteRatioMidpoint() { return eliteRatioMidpoint; }
    public double getEliteRatioSteepness() { return eliteRatioSteepness; }
    public double getEliteRoomWeightExponent() { return eliteRoomWeightExponent; }
    public int getMaxElitesPerCombatRoom() { return maxElitesPerCombatRoom; }
    public int getBossRoomEliteMax() { return bossRoomEliteMax; }
    public int getBossSpawnBase() { return bossSpawnBase; }
    public int getBossSpawnAreaDivisor() { return bossSpawnAreaDivisor; }
    public int getCombatSpawnBase() { return combatSpawnBase; }
    public int getCombatSpawnAreaDivisor() { return combatSpawnAreaDivisor; }
    public int getSpawnAttemptMultiplier() { return spawnAttemptMultiplier; }
    public int getSpawnInteriorInset() { return spawnInteriorInset; }

    // ============================================
    // Getters — Assembly
    // ============================================

    public int getBlocksPerBatch() { return blocksPerBatch; }
    @Nonnull
    public String getFluidFallbackBlock() { return fluidFallbackBlock; }

    /**
     * @return the block ID to place as a visual marker at spawner positions,
     *         or {@code null}/empty for invisible spawners.
     * @since 1.1.0
     */
    @Nullable
    public String getSpawnerBlock() { return spawnerBlock; }
}
