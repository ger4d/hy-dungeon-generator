package com.duntale.dungeongen.generator.layout;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A room in the dungeon graph, defined by its grid-space position,
 * dimensions, type, per-cell membership (for non-rectangular shapes),
 * connections to other rooms, and feature flags.
 *
 * @since 1.0.0
 */
public class Room {

    private final int id;
    private RoomType type;
    private final int x;
    private final int y;
    private final int z;
    private final int width;
    private final int height;
    private final int depth;
    private final List<Integer> connections;
    private final List<int[]> cells;

    // Feature flags
    private boolean isTreasureRoom = false;
    private boolean isEntrance = false;
    private boolean isExit = false;
    private int numEnemies = 0;

    /**
     * Create a new room.
     *
     * @param id     unique room identifier
     * @param type   room classification
     * @param x      min X in grid space
     * @param y      min Y in grid space
     * @param z      min Z in grid space
     * @param width  X size
     * @param height Y size
     * @param depth  Z size
     */
    public Room(int id, @Nonnull RoomType type, int x, int y, int z,
                int width, int height, int depth) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.connections = new ArrayList<>();
        this.cells = new ArrayList<>();
    }

    /**
     * Add a connection to another room by ID.
     *
     * @param roomId the connected room's ID
     */
    public void addConnection(int roomId) {
        if (!connections.contains(roomId)) {
            connections.add(roomId);
        }
    }

    // ============================================
    // Cell tracking (non-rectangular shapes)
    // ============================================

    /**
     * Register a carved cell belonging to this room.
     *
     * @param x cell X coordinate in grid space
     * @param z cell Z coordinate in grid space
     */
    public void addCell(int x, int z) {
        cells.add(new int[]{x, z});
    }

    /**
     * @return unmodifiable list of carved cells ({@code int[]{x, z}} pairs).
     */
    @Nonnull
    public List<int[]> getCells() {
        return Collections.unmodifiableList(cells);
    }

    /**
     * @return {@code true} if this room has individually tracked cells.
     */
    public boolean hasCells() {
        return !cells.isEmpty();
    }

    // ============================================
    // Centre coordinates
    // ============================================

    /** @return the center X coordinate. */
    public int centerX() { return x + width / 2; }

    /** @return the center Y coordinate. */
    public int centerY() { return y + height / 2; }

    /** @return the center Z coordinate. */
    public int centerZ() { return z + depth / 2; }

    // ============================================
    // Getters / Setters
    // ============================================

    /** @return unique room identifier. */
    public int getId() { return id; }

    /** @return the room type classification. */
    @Nonnull
    public RoomType getType() { return type; }

    /**
     * Change the room type (used during type assignment phase).
     *
     * @param type the new room type
     */
    public void setType(@Nonnull RoomType type) { this.type = type; }

    /** @return min X in grid space. */
    public int getX() { return x; }

    /** @return min Y in grid space. */
    public int getY() { return y; }

    /** @return min Z in grid space. */
    public int getZ() { return z; }

    /** @return X size. */
    public int getWidth() { return width; }

    /** @return Y size. */
    public int getHeight() { return height; }

    /** @return Z size (depth). */
    public int getDepth() { return depth; }

    /**
     * @return unmodifiable list of connected room IDs.
     */
    @Nonnull
    public List<Integer> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    // ============================================
    // Utility methods
    // ============================================

    /**
     * Check whether a grid-space coordinate is within this room's bounds.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return {@code true} if the position is inside the room
     */
    public boolean contains(int x, int z) {
        return x >= this.x && x < this.x + width && z >= this.z && z < this.z + depth;
    }

    // ============================================
    // Feature flags
    // ============================================

    /** @return {@code true} if this room is a treasure room. */
    public boolean isTreasureRoom() { return isTreasureRoom; }

    /**
     * Mark or unmark this room as a treasure room.
     *
     * @param treasureRoom the treasure flag
     */
    public void setTreasureRoom(boolean treasureRoom) { this.isTreasureRoom = treasureRoom; }

    /** @return {@code true} if this room is the dungeon entrance. */
    public boolean isEntrance() { return isEntrance; }

    /**
     * Mark or unmark this room as the entrance.
     *
     * @param entrance the entrance flag
     */
    public void setEntrance(boolean entrance) { this.isEntrance = entrance; }

    /** @return {@code true} if this room is the dungeon exit. */
    public boolean isExit() { return isExit; }

    /**
     * Mark or unmark this room as the exit.
     *
     * @param exit the exit flag
     */
    public void setExit(boolean exit) { this.isExit = exit; }

    /** @return number of enemies assigned to this room. */
    public int getNumEnemies() { return numEnemies; }

    /**
     * Set the number of enemies for this room.
     *
     * @param numEnemies enemy count
     */
    public void setNumEnemies(int numEnemies) { this.numEnemies = numEnemies; }

    @Override
    public String toString() {
        return "Room{id=" + id + ", type=" + type + ", pos=(" + x + "," + y + "," + z +
               "), size=(" + width + "x" + height + "x" + depth +
               "), cells=" + cells.size() + ", connections=" + connections + "}";
    }
}
