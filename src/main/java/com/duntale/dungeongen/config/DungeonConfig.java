package com.duntale.dungeongen.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import com.duntale.dungeongen.util.JsonParser;

/**
 * Top-level configuration for a dungeon generation request. Combines seed,
 * preset, layout, theme, pacing, world placement, and assembly settings.
 *
 * @param seed     deterministic seed string (null → random)
 * @param preset   optional built-in preset name
 * @param worldName target world for assembly (default: "default")
 * @param origin   world-space origin for the dungeon
 * @param layout   layout generation parameters
 * @param theme    theme / visual palette parameters
 * @param pacing   pacing / tension curve parameters
 * @param assemble   whether to place the dungeon in-world after generation
 * @param clear      whether to run a /clear command before generation to wipe the area
 * @param floorLevel the dungeon floor level for spawn-pool filtering (default: 1)
 * @since 1.0.0
 */
public record DungeonConfig(
    @Nullable String seed,
    @Nullable String preset,
    @Nonnull String worldName,
    @Nonnull Vec3i origin,
    @Nonnull LayoutConfig layout,
    @Nonnull ThemeConfig theme,
    @Nonnull PacingConfig pacing,
    boolean assemble,
    boolean clear,
    int floorLevel
) {

    /**
     * Parse a {@code DungeonConfig} from a JSON map (typically the HTTP request body).
     *
     * @param json the parsed JSON map
     * @return the dungeon config, with defaults applied for missing fields
     */
    @Nonnull
    public static DungeonConfig fromJson(@Nonnull Map<String, Object> json) {
        DungeonConfig d = withDefaults();

        String seed = JsonParser.toStringOrNull(json.get("seed"));
        String preset = JsonParser.toStringOrNull(json.get("preset"));
        String worldName = json.containsKey("worldName")
            ? JsonParser.toStringOrNull(json.get("worldName"))
            : d.worldName();

        Vec3i origin;
        Map<String, Object> originObj = JsonParser.getObject(json, "origin");
        if (originObj != null) {
            origin = Vec3i.fromJson(originObj);
        } else {
            origin = d.origin();
        }

        LayoutConfig layout;
        Map<String, Object> layoutObj = JsonParser.getObject(json, "layout");
        if (layoutObj != null) {
            layout = LayoutConfig.fromJson(layoutObj);
        } else {
            layout = d.layout();
        }

        ThemeConfig theme;
        Map<String, Object> themeObj = JsonParser.getObject(json, "theme");
        if (themeObj != null) {
            theme = ThemeConfig.fromJson(themeObj);
        } else {
            theme = d.theme();
        }

        PacingConfig pacing;
        Map<String, Object> pacingObj = JsonParser.getObject(json, "pacing");
        if (pacingObj != null) {
            pacing = PacingConfig.fromJson(pacingObj);
        } else {
            pacing = d.pacing();
        }

        boolean assemble = json.containsKey("assemble")
            ? JsonParser.toBoolean(json.get("assemble"))
            : d.assemble();

        boolean clear = json.containsKey("clear")
            ? JsonParser.toBoolean(json.get("clear"))
            : d.clear();

        int floorLevel = json.containsKey("floorLevel")
            ? JsonParser.toInt(json.get("floorLevel"))
            : d.floorLevel();

        return new DungeonConfig(seed, preset, worldName, origin, layout, theme, pacing, assemble, clear, floorLevel);
    }

    /**
     * Return a fully-defaulted {@code DungeonConfig} suitable for quick generation.
     *
     * @return default dungeon config
     */
    @Nonnull
    public static DungeonConfig withDefaults() {
        return new DungeonConfig(
            null,
            null,
            "default",
            Vec3i.ZERO,
            LayoutConfig.defaults(),
            ThemeConfig.defaults(),
            PacingConfig.defaults(),
            false,
            false,
            1
        );
    }
}
