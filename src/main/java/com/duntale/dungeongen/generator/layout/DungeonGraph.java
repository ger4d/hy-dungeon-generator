package com.duntale.dungeongen.generator.layout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Graph data structure representing a generated dungeon layout. Contains
 * all rooms and corridors plus metadata about the entrance and boss rooms.
 *
 * @since 1.0.0
 */
public class DungeonGraph {

    private final List<Room> rooms;
    private final List<Corridor> corridors;
    private int entranceRoomId = -1;
    private int bossRoomId = -1;

    /** Create an empty dungeon graph. */
    public DungeonGraph() {
        this.rooms = new ArrayList<>();
        this.corridors = new ArrayList<>();
    }

    // ============================================
    // Mutation
    // ============================================

    /**
     * Add a room to the graph.
     *
     * @param room the room to add
     */
    public void addRoom(@Nonnull Room room) {
        rooms.add(room);
    }

    /**
     * Add a corridor to the graph and register the connection
     * on both endpoint rooms.
     *
     * @param corridor the corridor to add
     */
    public void addCorridor(@Nonnull Corridor corridor) {
        corridors.add(corridor);
        Room from = getRoom(corridor.getFromRoomId());
        Room to = getRoom(corridor.getToRoomId());
        if (from != null) from.addConnection(corridor.getToRoomId());
        if (to != null) to.addConnection(corridor.getFromRoomId());
    }

    // ============================================
    // Queries
    // ============================================

    /**
     * Look up a room by ID.
     *
     * @param id the room ID
     * @return the room, or {@code null} if not found
     */
    @Nullable
    public Room getRoom(int id) {
        for (Room room : rooms) {
            if (room.getId() == id) return room;
        }
        return null;
    }

    /**
     * @return unmodifiable list of all rooms.
     */
    @Nonnull
    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    /**
     * @return unmodifiable list of all corridors.
     */
    @Nonnull
    public List<Corridor> getCorridors() {
        return Collections.unmodifiableList(corridors);
    }

    /** @return the entrance room ID, or -1 if not set. */
    public int getEntranceRoomId() { return entranceRoomId; }

    /** @param id the entrance room ID. */
    public void setEntranceRoomId(int id) { this.entranceRoomId = id; }

    /** @return the boss room ID, or -1 if not set. */
    public int getBossRoomId() { return bossRoomId; }

    /** @param id the boss room ID. */
    public void setBossRoomId(int id) { this.bossRoomId = id; }

    // ============================================
    // Graph Analysis
    // ============================================

    /**
     * Check whether every room in the graph is reachable from every
     * other room via BFS.
     *
     * @return {@code true} if the graph is fully connected
     */
    public boolean isFullyConnected() {
        if (rooms.isEmpty()) return true;

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(rooms.getFirst().getId());
        visited.add(rooms.getFirst().getId());

        while (!queue.isEmpty()) {
            int current = queue.poll();
            Room room = getRoom(current);
            if (room == null) continue;

            for (int neighbour : room.getConnections()) {
                if (visited.add(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }

        return visited.size() == rooms.size();
    }

    /**
     * Compute the critical path from the entrance room to the boss room
     * using BFS (shortest path in an unweighted graph).
     *
     * @return ordered list of room IDs on the critical path, or empty if
     *         no path exists or entrance/boss not set
     */
    @Nonnull
    public List<Integer> getCriticalPath() {
        if (entranceRoomId < 0 || bossRoomId < 0) return List.of();
        if (entranceRoomId == bossRoomId) return List.of(entranceRoomId);

        // BFS from entrance to boss
        Map<Integer, Integer> parent = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(entranceRoomId);
        parent.put(entranceRoomId, -1);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == bossRoomId) break;

            Room room = getRoom(current);
            if (room == null) continue;

            for (int neighbour : room.getConnections()) {
                if (!parent.containsKey(neighbour)) {
                    parent.put(neighbour, current);
                    queue.add(neighbour);
                }
            }
        }

        if (!parent.containsKey(bossRoomId)) return List.of();

        // Reconstruct path
        List<Integer> path = new ArrayList<>();
        int current = bossRoomId;
        while (current != -1) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}
