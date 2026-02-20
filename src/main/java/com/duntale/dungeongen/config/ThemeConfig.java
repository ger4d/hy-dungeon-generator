package com.duntale.dungeongen.config;

import javax.annotation.Nonnull;
import java.util.Map;
import com.duntale.dungeongen.util.JsonParser;

/**
 * Theme configuration controlling the visual palette and environmental
 * decay applied to a generated dungeon.
 *
 * @param palette          the block palette name: "crypt", "volcanic", "arcane", "mine",
 *                         "mushroom", "hive", "temple_dark"
 * @param decayFactor      how much structural decay to apply, 0–1 (default: 0.3)
 * @param overgrowthFactor density of vines / moss / roots, 0–1 (default: 0.1)
 * @param floodingFactor   proportion of floors flooded with liquid, 0–1 (default: 0.0)
 * @since 1.0.0
 */
public record ThemeConfig(
    String palette,
    double decayFactor,
    double overgrowthFactor,
    double floodingFactor
) {

    /**
     * Parse a {@code ThemeConfig} from a JSON map.
     *
     * @param json the JSON map with theme keys
     * @return the parsed config, falling back to defaults for missing keys
     */
    @Nonnull
    public static ThemeConfig fromJson(@Nonnull Map<String, Object> json) {
        ThemeConfig d = defaults();
        return new ThemeConfig(
            json.containsKey("palette") ? JsonParser.toStringOrNull(json.get("palette")) : d.palette(),
            json.containsKey("decayFactor") ? JsonParser.toDouble(json.get("decayFactor")) : d.decayFactor(),
            json.containsKey("overgrowthFactor") ? JsonParser.toDouble(json.get("overgrowthFactor")) : d.overgrowthFactor(),
            json.containsKey("floodingFactor") ? JsonParser.toDouble(json.get("floodingFactor")) : d.floodingFactor()
        );
    }

    /**
     * Return a {@code ThemeConfig} with sensible defaults (crypt palette, mild decay).
     *
     * @return default theme config
     */
    @Nonnull
    public static ThemeConfig defaults() {
        return new ThemeConfig("crypt", 0.3, 0.1, 0.0);
    }
}
