package com.duntale.dungeongen.generator.props;

import com.duntale.dungeongen.generator.layout.RoomType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines where and how a decorative prop should be placed in a room.
 *
 * @since 1.0.0
 */
public class PropRule {

    /** Where relative to the room the prop should be placed. */
    public enum Placement {
        /** Adjacent to a wall — torches, banners, sconces. */
        WALL_ALIGNED,
        /** In room corners — cobwebs, small crates. */
        CORNER,
        /** Center area of room — altars, fountains, chests. */
        CENTER,
        /** On the floor away from walls — barrels, pots, tables. */
        FLOOR,
        /** On the ceiling — lanterns, chains. */
        CEILING
    }

    private final String blockId;
    private final Placement placement;
    private final double spawnChance;
    private final int maxPerRoom;
    private final RoomType[] allowedRooms;

    /**
     * Create a new prop rule.
     *
     * @param blockId      the block type ID for this prop
     * @param placement    where in the room the prop may appear
     * @param spawnChance  probability (0–1) of placement per valid position
     * @param maxPerRoom   maximum instances in a single room
     * @param allowedRooms room types that allow this prop ({@code null} = all)
     */
    public PropRule(@Nonnull String blockId,
                    @Nonnull Placement placement,
                    double spawnChance,
                    int maxPerRoom,
                    @Nullable RoomType[] allowedRooms) {
        this.blockId = blockId;
        this.placement = placement;
        this.spawnChance = spawnChance;
        this.maxPerRoom = maxPerRoom;
        this.allowedRooms = allowedRooms;
    }

    /** @return the block type ID for this prop. */
    @Nonnull
    public String getBlockId() { return blockId; }

    /** @return the placement constraint. */
    @Nonnull
    public Placement getPlacement() { return placement; }

    /** @return the spawn probability per valid position (0–1). */
    public double getSpawnChance() { return spawnChance; }

    /** @return the maximum number of this prop per room. */
    public int getMaxPerRoom() { return maxPerRoom; }

    /**
     * Check whether this prop may appear in the given room type.
     *
     * @param type the room type to check
     * @return {@code true} if the prop is allowed in that room type
     */
    public boolean isAllowedIn(@Nonnull RoomType type) {
        if (allowedRooms == null || allowedRooms.length == 0) return true;
        for (RoomType rt : allowedRooms) {
            if (rt == type) return true;
        }
        return false;
    }
}
