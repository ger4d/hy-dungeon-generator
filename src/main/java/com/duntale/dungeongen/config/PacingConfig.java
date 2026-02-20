package com.duntale.dungeongen.config;

import javax.annotation.Nonnull;
import java.util.Map;
import com.duntale.dungeongen.util.JsonParser;

/**
 * Pacing configuration controlling the tension curve and difficulty
 * scaling through the dungeon.
 *
 * @param breatheRoomFrequency how often safe/breathe rooms appear, 0–1 (default: 0.3)
 * @param difficultyRamp       rate at which difficulty increases along the critical path, 0–1 (default: 0.5)
 * @since 1.0.0
 */
public record PacingConfig(
    double breatheRoomFrequency,
    double difficultyRamp
) {

    /**
     * Parse a {@code PacingConfig} from a JSON map.
     *
     * @param json the JSON map with pacing keys
     * @return the parsed config, falling back to defaults for missing keys
     */
    @Nonnull
    public static PacingConfig fromJson(@Nonnull Map<String, Object> json) {
        PacingConfig d = defaults();
        return new PacingConfig(
            json.containsKey("breatheRoomFrequency") ? JsonParser.toDouble(json.get("breatheRoomFrequency")) : d.breatheRoomFrequency(),
            json.containsKey("difficultyRamp") ? JsonParser.toDouble(json.get("difficultyRamp")) : d.difficultyRamp()
        );
    }

    /**
     * Return a {@code PacingConfig} with sensible defaults.
     *
     * @return default pacing config
     */
    @Nonnull
    public static PacingConfig defaults() {
        return new PacingConfig(0.3, 0.5);
    }
}
