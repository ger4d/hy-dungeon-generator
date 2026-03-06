package com.duntale.dungeongen.config.asset;

import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.props.PropRule;
import com.duntale.dungeongen.generator.theme.BlockPalette;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Hytale asset-backed dungeon theme configuration.
 *
 * <p>Each JSON file under {@code Configs/DungeonGen/Themes/} defines a
 * complete theme (palette, lights, props, spawner prefix). Files are
 * auto-discovered and hot-reloaded by the engine's asset monitor.</p>
 *
 * <p>The filename (without extension) becomes the theme ID, e.g.
 * {@code crypt.json → "crypt"}.</p>
 *
 * @since 1.3.0
 */
public class DungeonThemeConfig
        implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, DungeonThemeConfig>> {

    // ============================================
    // Static asset machinery
    // ============================================

    public static AssetBuilderCodec<String, DungeonThemeConfig> CODEC;
    private static AssetStore<String, DungeonThemeConfig,
            IndexedLookupTableAssetMap<String, DungeonThemeConfig>> ASSET_STORE;

    // ============================================
    // Fields — populated by CODEC from JSON
    // ============================================

    protected String id;
    protected AssetExtraInfo.Data data;

    // Palette
    protected PaletteEntry palette = new PaletteEntry();

    // Lights
    protected LightEntry lights = new LightEntry();

    // Props
    protected PropRuleEntry[] props = new PropRuleEntry[0];

    // Spawner
    protected String spawnerPrefix = "Zone1_Undead";

    // Traps — per-theme trap block sets
    protected TrapEntry traps = new TrapEntry();

    // Fill block used by VoxelCarver and WorldAssembler for this theme
    protected String fillBlock = "Rock_Stone_Brick";

    // Secondary wall replacement chance (0.0 to 1.0)
    protected double secondaryWallChance = 0.2;

    // Spawn Pools — NPC spawner entries per tier
    protected SpawnPoolsEntry spawnPools = new SpawnPoolsEntry();

    // Level variance applied to spawned NPCs: actual level = floorLevel ± variance
    protected int levelVariance = 5;

    /** No-arg constructor required by {@link com.hypixel.hytale.codec.builder.BuilderCodec}. */
    public DungeonThemeConfig() {}

    // ============================================
    // CODEC definition
    // ============================================

    static {
        CODEC = AssetBuilderCodec.builder(
                DungeonThemeConfig.class,
                DungeonThemeConfig::new,
                Codec.STRING,
                (t, k) -> t.id = k,
                t -> t.id,
                (asset, d) -> asset.data = d,
                asset -> asset.data
            )
            .append(new KeyedCodec<>("Palette", PaletteEntry.CODEC),
                (c, v) -> c.palette = v, c -> c.palette).add()
            .append(new KeyedCodec<>("Lights", LightEntry.CODEC),
                (c, v) -> c.lights = v, c -> c.lights).add()
            .append(new KeyedCodec<>("Props", PropRuleEntry.ARRAY_CODEC),
                (c, v) -> c.props = v, c -> c.props).add()
            .append(new KeyedCodec<>("SpawnerPrefix", Codec.STRING),
                (c, v) -> c.spawnerPrefix = v, c -> c.spawnerPrefix).add()
            .append(new KeyedCodec<>("Traps", TrapEntry.CODEC),
                (c, v) -> c.traps = v, c -> c.traps).add()
            .append(new KeyedCodec<>("FillBlock", Codec.STRING),
                (c, v) -> c.fillBlock = v, c -> c.fillBlock).add()
            .append(new KeyedCodec<>("SecondaryWallChance", Codec.DOUBLE),
                (c, v) -> c.secondaryWallChance = v, c -> c.secondaryWallChance).add()
            .append(new KeyedCodec<>("SpawnPools", SpawnPoolsEntry.CODEC),
                (c, v) -> c.spawnPools = v, c -> c.spawnPools).add()
            .append(new KeyedCodec<>("LevelVariance", Codec.INTEGER),
                (c, v) -> c.levelVariance = v, c -> c.levelVariance).add()
            .build();
    }

    // ============================================
    // Static accessors
    // ============================================

    /**
     * Look up a theme config by ID.
     *
     * @param id the theme ID (filename without extension, e.g. "crypt")
     * @return the theme config, or {@code null} if not loaded
     */
    @Nullable
    public static DungeonThemeConfig get(@Nonnull String id) {
        return getAssetMap().getAsset(id);
    }

    /**
     * @return the underlying asset store (lazily resolved from the registry).
     */
    @Nonnull
    public static AssetStore<String, DungeonThemeConfig,
            IndexedLookupTableAssetMap<String, DungeonThemeConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(DungeonThemeConfig.class);
        }
        return ASSET_STORE;
    }

    /**
     * @return the asset map containing all loaded theme configs.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static IndexedLookupTableAssetMap<String, DungeonThemeConfig> getAssetMap() {
        return (IndexedLookupTableAssetMap<String, DungeonThemeConfig>) getAssetStore().getAssetMap();
    }

    // ============================================
    // JsonAssetWithMap implementation
    // ============================================

    @Override
    public String getId() {
        return id;
    }

    // ============================================
    // Getters
    // ============================================

    @Nonnull public PaletteEntry getPalette() { return palette; }
    @Nonnull public LightEntry getLights() { return lights; }
    @Nonnull public PropRuleEntry[] getProps() { return props; }
    @Nonnull public String getSpawnerPrefix() { return spawnerPrefix; }
    @Nonnull public TrapEntry getTraps() { return traps; }
    @Nonnull public String getFillBlock() { return fillBlock; }
    public double getSecondaryWallChance() { return secondaryWallChance; }

    /**
     * @return the spawn pools configuration for this theme.
     * @since 1.1.0
     */
    @Nonnull
    public SpawnPoolsEntry getSpawnPools() { return spawnPools; }

    /**
     * @return the level variance for spawned NPCs (actual level = floorLevel ± variance).
     * @since 1.1.0
     */
    public int getLevelVariance() { return levelVariance; }

    // ============================================
    // Domain conversions
    // ============================================

    /**
     * Convert the palette entry to a {@link BlockPalette} domain object.
     *
     * @return the block palette for this theme
     */
    @Nonnull
    public BlockPalette toPalette() {
        OvergrowthEntry og = palette.getOvergrowthBlocks();
        return new BlockPalette(
            id,
            palette.getPrimaryWall(),
            palette.getSecondaryWall(),
            palette.getFloor(),
            palette.getCeiling(),
            palette.getPillarBase(),
            palette.getPillarMiddle(),
            palette.getStairs(),
            palette.getSlab(),
            palette.getDecayVariants(),
            og.getFloor(),
            og.getWall(),
            og.getCeiling(),
            palette.getRubbleBlocks(),
            palette.getFluidBlock(),
            palette.getAccentBlock() != null ? palette.getAccentBlock() : palette.getPrimaryWall()
        );
    }

    /**
     * Convert the prop entries to a list of {@link PropRule} domain objects.
     *
     * @return the prop rules for this theme
     */
    @Nonnull
    public List<PropRule> toProps() {
        List<PropRule> rules = new ArrayList<>();
        for (PropRuleEntry entry : props) {
            PropRule.Placement placement;
            try {
                placement = PropRule.Placement.valueOf(entry.getPlacement());
            } catch (IllegalArgumentException e) {
                placement = PropRule.Placement.FLOOR;
            }

            RoomType[] allowed = null;
            if (entry.getAllowedRoomTypes() != null && entry.getAllowedRoomTypes().length > 0) {
                List<RoomType> types = new ArrayList<>();
                for (String typeName : entry.getAllowedRoomTypes()) {
                    try {
                        types.add(RoomType.valueOf(typeName));
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!types.isEmpty()) {
                    allowed = types.toArray(new RoomType[0]);
                }
            }

            rules.add(new PropRule(
                entry.getBlockId(),
                placement,
                entry.getSpawnChance(),
                entry.getMaxPerRoom(),
                allowed,
                entry.getYOffset()
            ));
        }
        return rules;
    }
}
