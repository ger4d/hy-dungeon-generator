package com.duntale.dungeongen.generator.layout;

/**
 * Classification of corridors by their path shape.
 *
 * <ul>
 *   <li>{@link #L_SHAPED} — right-angle turn corridor (3 waypoints).</li>
 *   <li>{@link #WINDING} — meandering path with perpendicular drift.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum CorridorType {
    /** Right-angle corridor with a single turn point. */
    L_SHAPED,
    /** Meandering corridor with random perpendicular drift. */
    WINDING
}
