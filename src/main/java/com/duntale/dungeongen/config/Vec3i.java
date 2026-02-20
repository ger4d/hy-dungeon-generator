package com.duntale.dungeongen.config;

import javax.annotation.Nonnull;
import java.util.Map;
import com.duntale.dungeongen.util.JsonParser;

/**
 * Immutable 3D integer vector used for dungeon origin coordinates and
 * corridor waypoints.
 *
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @since 1.0.0
 */
public record Vec3i(int x, int y, int z) {

    /** The zero vector (0, 0, 0). */
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);

    /**
     * Parse a {@code Vec3i} from a JSON map with keys {@code "x"}, {@code "y"}, {@code "z"}.
     *
     * @param json the JSON map
     * @return the parsed vector
     */
    @Nonnull
    public static Vec3i fromJson(@Nonnull Map<String, Object> json) {
        return new Vec3i(
            JsonParser.toInt(json.get("x")),
            JsonParser.toInt(json.get("y")),
            JsonParser.toInt(json.get("z"))
        );
    }
}
