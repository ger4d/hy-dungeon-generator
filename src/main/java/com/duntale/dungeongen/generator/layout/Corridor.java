package com.duntale.dungeongen.generator.layout;

import com.duntale.dungeongen.config.Vec3i;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * A corridor connecting two rooms in the dungeon graph, defined by
 * a sequence of grid-space waypoints and a width.
 *
 * @since 1.0.0
 */
public class Corridor {

    private final int fromRoomId;
    private final int toRoomId;
    private final List<Vec3i> path;
    private final int width;

    /**
     * Create a new corridor.
     *
     * @param fromRoomId source room ID
     * @param toRoomId   destination room ID
     * @param path       ordered waypoints from source centre to destination centre
     * @param width      corridor width in blocks (default: 3)
     */
    public Corridor(int fromRoomId, int toRoomId, @Nonnull List<Vec3i> path, int width) {
        this.fromRoomId = fromRoomId;
        this.toRoomId = toRoomId;
        this.path = List.copyOf(path);
        this.width = width;
    }

    /** @return the source room ID. */
    public int getFromRoomId() { return fromRoomId; }

    /** @return the destination room ID. */
    public int getToRoomId() { return toRoomId; }

    /**
     * @return the corridor waypoints (unmodifiable).
     */
    @Nonnull
    public List<Vec3i> getPath() { return path; }

    /** @return the corridor width in blocks. */
    public int getWidth() { return width; }

    @Override
    public String toString() {
        return "Corridor{from=" + fromRoomId + ", to=" + toRoomId +
               ", waypoints=" + path.size() + ", width=" + width + "}";
    }
}
