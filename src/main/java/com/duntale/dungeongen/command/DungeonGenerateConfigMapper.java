package com.duntale.dungeongen.command;

import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.ThemeConfig;
import com.duntale.dungeongen.config.Vec3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Maps {@link DungeonGeneratePage.GenerateEventData} into a {@link DungeonConfig}
 * by applying defaults for missing or invalid fields.
 *
 * <p>Pure (no Hytale runtime dependencies) so it can be unit-tested directly.</p>
 *
 * @since 1.0.3
 */
public final class DungeonGenerateConfigMapper {

    /**
     * Known dungeon palette IDs (lowercase, matching {@code DungeonThemeConfig} asset IDs).
     */
    public static final Set<String> KNOWN_PALETTES = Set.of(
            "crypt", "hive", "mine", "arcane", "temple_dark", "volcanic", "mushroom"
    );

    private static final String DEFAULT_PALETTE = "crypt";
    private static final String DEFAULT_SEED = "0";

    private DungeonGenerateConfigMapper() {
    }

    /**
     * Build a {@link DungeonConfig} from the page's event data, applying defaults
     * for any missing or invalid fields.
     *
     * @param d the captured event data
     * @return a fully-resolved dungeon config
     */
    @Nonnull
    public static DungeonConfig toConfig(@Nonnull DungeonGeneratePage.GenerateEventData d) {
        LayoutConfig ld = LayoutConfig.defaults();
        ThemeConfig td = ThemeConfig.defaults();
        PacingConfig pd = PacingConfig.defaults();
        DungeonConfig dd = DungeonConfig.withDefaults();

        String seed = isBlank(d.seed) ? DEFAULT_SEED : d.seed.trim();
        String worldName = dd.worldName();

        Vec3i origin = new Vec3i(
                intOrDefault(d.originX, dd.origin().x()),
                intOrDefault(d.originY, dd.origin().y()),
                intOrDefault(d.originZ, dd.origin().z())
        );

        LayoutConfig layout = new LayoutConfig(
                intOrDefault(d.width, ld.width()),
                intOrDefault(d.depth, ld.depth()),
                intOrDefault(d.height, ld.height()),
                floatOrDefault(d.roomDensity, ld.roomDensity()),
                intOrDefault(d.minRoomSize, ld.minRoomSize()),
                intOrDefault(d.maxRoomSize, ld.maxRoomSize()),
                intOrDefault(d.maxRooms, ld.maxRooms()),
                isBlank(d.roomShape) ? ld.roomShape() : d.roomShape,
                floatOrDefault(d.irregularity, ld.irregularity()),
                intOrDefault(d.corridorWidth, ld.corridorWidth()),
                floatOrDefault(d.branchChance, ld.branchChance()),
                floatOrDefault(d.loopChance, ld.loopChance()),
                d.windingCorridors != null ? d.windingCorridors : ld.windingCorridors(),
                floatOrDefault(d.windingFactor, ld.windingFactor()),
                floatOrDefault(d.pillarFreq, ld.pillarFrequency()),
                floatOrDefault(d.waterFreq, ld.waterFrequency()),
                floatOrDefault(d.lavaFreq, ld.lavaFrequency()),
                floatOrDefault(d.trapDensity, ld.trapDensity()),
                d.floorTraps != null ? d.floorTraps : ld.floorTraps(),
                floatOrDefault(d.secretWallChance, ld.secretWallChance()),
                floatOrDefault(d.merchantSpawnChance, ld.merchantSpawnChance()),
                isBlank(d.entrancePlacement) ? ld.entrancePlacement() : d.entrancePlacement,
                floatOrDefault(d.exitDistance, ld.exitDistance()),
                floatOrDefault(d.enemyDensity, ld.enemyDensity()),
                intOrDefault(d.maxEnemiesPerRoom, ld.maxEnemiesPerRoom()),
                d.bossRoom != null ? d.bossRoom : ld.bossRoom(),
                floatOrDefault(d.ambushChance, ld.ambushChance()),
                floatOrDefault(d.erosion, ld.erosion()),
                d.removeCeiling != null ? d.removeCeiling : ld.removeCeiling(),
                d.flatFloor != null ? d.flatFloor : ld.flatFloor(),
                d.solidFill != null ? d.solidFill : ld.solidFill(),
                floatOrDefault(d.complexity, ld.complexity())
        );

        ThemeConfig theme = new ThemeConfig(
                resolvePalette(d.palette, td.palette()),
                floatOrDefault(d.decayFactor, td.decayFactor()),
                floatOrDefault(d.overgrowthFactor, td.overgrowthFactor()),
                floatOrDefault(d.floodingFactor, td.floodingFactor())
        );

        PacingConfig pacing = new PacingConfig(
                floatOrDefault(d.breatheRoomFreq, pd.breatheRoomFrequency()),
                floatOrDefault(d.difficultyRamp, pd.difficultyRamp())
        );

        int floorLevel = intOrDefault(d.floorLevel, dd.floorLevel());

        return new DungeonConfig(seed, null, worldName, origin, layout, theme, pacing, true, floorLevel);
    }

    /**
     * Normalize a palette ID to a known lowercase asset ID, or fall back to the
     * given default if the input is missing or unrecognized.
     *
     * @param raw      the raw palette value from the form
     * @param fallback the fallback palette ID
     * @return a known palette ID, lowercased
     */
    @Nonnull
    public static String resolvePalette(@Nullable String raw, @Nonnull String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase();
        if (KNOWN_PALETTES.contains(normalized)) {
            return normalized;
        }
        return fallback;
    }

    /**
     * @return the default palette ID used when none is specified.
     */
    @Nonnull
    public static String defaultPalette() {
        return DEFAULT_PALETTE;
    }

    private static int intOrDefault(@Nullable Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private static double floatOrDefault(@Nullable Float value, double fallback) {
        return value != null ? value.doubleValue() : fallback;
    }

    private static boolean isBlank(@Nullable String s) {
        return s == null || s.isBlank();
    }
}
