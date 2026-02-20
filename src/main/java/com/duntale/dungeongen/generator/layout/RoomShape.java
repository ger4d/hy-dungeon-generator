package com.duntale.dungeongen.generator.layout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shape template for generated rooms.
 *
 * @since 1.1.0
 */
public enum RoomShape {
    RECTANGULAR,
    CIRCULAR,
    L_SHAPED,
    CROSS_SHAPED,
    IRREGULAR;

    /**
     * Parse a shape from its JSON string representation.
     *
     * @param name the shape name (case-insensitive), or {@code null}
     * @return the matching shape, or {@link #RECTANGULAR} if unrecognised
     */
    @Nonnull
    public static RoomShape fromString(@Nullable String name) {
        if (name == null) return RECTANGULAR;
        return switch (name.toLowerCase()) {
            case "circular" -> CIRCULAR;
            case "lshaped", "l_shaped", "l-shaped" -> L_SHAPED;
            case "cross", "cross_shaped", "crossshaped" -> CROSS_SHAPED;
            case "irregular" -> IRREGULAR;
            default -> RECTANGULAR;
        };
    }
}
