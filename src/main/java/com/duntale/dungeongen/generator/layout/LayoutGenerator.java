package com.duntale.dungeongen.generator.layout;

import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.Vec3i;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Random-placement + MST corridor dungeon layout generator. Places rooms
 * via rejection sampling, connects them with a greedy minimum spanning
 * tree, then adds optional branches and loops. Room shapes are carved
 * per-cell for non-rectangular templates.
 *
 * <h2>Algorithm:</h2>
 * <ol>
 *   <li>Randomly scatter rooms with overlap rejection.</li>
 *   <li>Carve room cells according to the configured shape.</li>
 *   <li>Build a greedy MST to connect all rooms.</li>
 *   <li>Add branch corridors from corridor midpoints.</li>
 *   <li>Add loop corridors between distant rooms.</li>
 *   <li>Validate 100% connectivity.</li>
 *   <li>Assign entrance, exit, boss, and room types.</li>
 * </ol>
 *
 * @since 1.1.0
 */
public class LayoutGenerator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Random random;
    private final LayoutConfig config;
    private final RoomShape shape;
    private int nextRoomId;

    /**
     * Create a layout generator with the given seed and config.
     *
     * @param seed   deterministic RNG seed
     * @param config layout generation parameters
     */
    public LayoutGenerator(long seed, @Nonnull LayoutConfig config) {
        this.random = new Random(seed);
        this.config = config;
        this.shape = RoomShape.fromString(config.roomShape());
        this.nextRoomId = 0;
    }

    /**
     * Generate a complete dungeon layout graph.
     *
     * @return the dungeon graph with rooms, corridors, and assigned types
     */
    @Nonnull
    public DungeonGraph generate() {
        DungeonGraph graph = new DungeonGraph();

        // Phase 1: Random placement
        List<Room> rooms = placeRooms();
        for (Room room : rooms) {
            LOGGER.atFine().log("[DungeonGen] Placed room %d at (%d, %d) size (%d x %d) center (%d, %d)",
                room.getId(), room.getX(), room.getZ(), room.getWidth(), room.getDepth(), room.centerX(), room.centerZ());
            graph.addRoom(room);
        }

        // Phase 2: Greedy MST corridors
        List<Corridor> mstCorridors = connectRooms(rooms);
        for (Corridor c : mstCorridors) {
            LOGGER.atFine().log("[DungeonGen] Carved corridor from room %d to room %d with %d waypoints start (%d, %d) end (%d, %d)",
                c.getFromRoomId(), c.getToRoomId(), c.getPath().size(),
                c.getPath().get(0).x(), c.getPath().get(0).z(),
                c.getPath().get(c.getPath().size() - 1).x(), c.getPath().get(c.getPath().size() - 1).z());
            graph.addCorridor(c);
        }

        // Phase 3: Branch corridors
        addBranches(graph, mstCorridors);

        // Phase 4: Loop corridors
        addLoops(graph, rooms);

        // Phase 5: Ensure full connectivity
        ensureFullConnectivity(graph, rooms);

        // Phase 6: Assign room types (entrance, exit, boss, etc.)
        assignRoomTypes(graph, rooms);

        return graph;
    }

    // ============================================
    // Phase 1: Room Placement
    // ============================================

    @Nonnull
    private List<Room> placeRooms() {
        int effectiveMax = Math.max(1, (int) (config.maxRooms() * config.roomDensity()));
        int attempts = effectiveMax * 15;
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < attempts && rooms.size() < effectiveMax; i++) {
            int w = config.minRoomSize() + random.nextInt(
                Math.max(1, config.maxRoomSize() - config.minRoomSize() + 1));
            int d = config.minRoomSize() + random.nextInt(
                Math.max(1, config.maxRoomSize() - config.minRoomSize() + 1));
            int h = config.height();

            // Position with 1-cell border
            int maxX = config.width() - w - 1;
            int maxZ = config.depth() - d - 1;
            if (maxX < 1 || maxZ < 1) continue;

            int rx = 1 + random.nextInt(maxX);
            int rz = 1 + random.nextInt(maxZ);

            // Check w+2 x d+2 area is clear of existing rooms
            if (!isAreaClear(rx - 1, rz - 1, w + 2, d + 2, rooms)) continue;

            Room room = new Room(nextRoomId++, RoomType.COMBAT, rx, 0, rz, w, h, d);
            if (tryPlaceRoom(room)) {
                rooms.add(room);
            }
        }

        return rooms;
    }

    /**
     * Check that a rectangular area does not overlap any existing room
     * (including a 1-cell buffer around placed rooms).
     *
     * @param ax start X of the area
     * @param az start Z of the area
     * @param aw width of the area
     * @param ad depth of the area
     * @param rooms existing rooms to test against
     * @return {@code true} if the area is free of overlaps
     */
    private boolean isAreaClear(int ax, int az, int aw, int ad,
                                @Nonnull List<Room> rooms) {
        for (Room r : rooms) {
            if (ax < r.getX() + r.getWidth() && ax + aw > r.getX() &&
                az < r.getZ() + r.getDepth() && az + ad > r.getZ()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Carve cells for a room according to the configured shape. Returns
     * {@code true} if the room has enough cells to be valid (at least 4).
     *
     * @param room the room to carve
     * @return {@code true} if the room was successfully carved
     */
    private boolean tryPlaceRoom(@Nonnull Room room) {
        int w = room.getWidth();
        int d = room.getDepth();
        double irr = config.irregularity();

        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                boolean include = switch (shape) {
                    case RECTANGULAR -> true;
                    case CIRCULAR -> {
                        double fx = (w > 1) ? 2.0 * dx / (w - 1) - 1.0 : 0.0;
                        double fz = (d > 1) ? 2.0 * dz / (d - 1) - 1.0 : 0.0;
                        yield (fx * fx + fz * fz) <= 1.0;
                    }
                    case L_SHAPED -> dx < w / 2 || dz < d / 2;
                    case CROSS_SHAPED -> (dx >= w / 3 && dx < w - w / 3)
                                      || (dz >= d / 3 && dz < d - d / 3);
                    case IRREGULAR -> random.nextDouble() >= irr * 1.5;
                };

                if (!include) continue;

                // Apply irregularity nibbling for non-irregular shapes
                if (shape != RoomShape.IRREGULAR && irr > 0.0) {
                    boolean isCorner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1);
                    boolean isEdge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
                    if (isCorner && random.nextDouble() < irr * 0.8) continue;
                    if (isEdge && !isCorner && random.nextDouble() < irr * 0.3) continue;
                }

                room.addCell(room.getX() + dx, room.getZ() + dz);
            }
        }

        return room.getCells().size() >= 4;
    }

    // ============================================
    // Phase 2: Greedy MST Connection
    // ============================================

    /**
     * Connect all rooms using a greedy MST by Manhattan distance.
     *
     * @param rooms the rooms to connect
     * @return list of corridors forming the spanning tree
     */
    @Nonnull
    private List<Corridor> connectRooms(@Nonnull List<Room> rooms) {
        List<Corridor> corridors = new ArrayList<>();
        if (rooms.size() < 2) return corridors;

        boolean[] connected = new boolean[rooms.size()];
        connected[0] = true;
        int connectedCount = 1;

        while (connectedCount < rooms.size()) {
            int bestFrom = -1;
            int bestTo = -1;
            int bestDist = Integer.MAX_VALUE;

            for (int i = 0; i < rooms.size(); i++) {
                if (!connected[i]) continue;
                for (int j = 0; j < rooms.size(); j++) {
                    if (connected[j]) continue;
                    int dist = manhattanDistance(rooms.get(i), rooms.get(j));
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestFrom = i;
                        bestTo = j;
                    }
                }
            }

            if (bestFrom < 0 || bestTo < 0) break;

            Corridor corridor = carveCorridor(rooms.get(bestFrom), rooms.get(bestTo));
            corridors.add(corridor);
            connected[bestTo] = true;
            connectedCount++;
        }

        return corridors;
    }

    // ============================================
    // Corridor Carving
    // ============================================

    /**
     * Carve a corridor between two rooms. Uses winding or L-shaped path
     * depending on configuration.
     *
     * @param a source room
     * @param b destination room
     * @return the carved corridor (type determined by path algorithm)
     */
    @Nonnull
    private Corridor carveCorridor(@Nonnull Room a, @Nonnull Room b) {
        int ax = a.centerX();
        int ay = a.getY();
        int az = a.centerZ();
        int bx = b.centerX();
        int by = b.getY();
        int bz = b.centerZ();

        List<Vec3i> path;
        CorridorType type;

        if (config.windingCorridors() && config.windingFactor() > 0.01) {
            path = carveWindingPath(ax, ay, az, bx, by, bz);
            type = CorridorType.WINDING;
        } else {
            path = carveLShapedPath(ax, ay, az, bx, by, bz);
            type = CorridorType.L_SHAPED;
        }

        return new Corridor(a.getId(), b.getId(), path, config.corridorWidth(), type);
    }

    /**
     * L-shaped corridor: random coin flip for horizontal-first or vertical-first.
     * Produces 3 waypoints.
     */
    @Nonnull
    private List<Vec3i> carveLShapedPath(int ax, int ay, int az,
                                          int bx, int by, int bz) {
        List<Vec3i> path = new ArrayList<>(3);
        if (random.nextBoolean()) {
            // Horizontal (X) first, then vertical (Z)
            path.add(new Vec3i(ax, ay, az));
            path.add(new Vec3i(bx, ay, az));
            path.add(new Vec3i(bx, by, bz));
        } else {
            // Vertical (Z) first, then horizontal (X)
            path.add(new Vec3i(ax, ay, az));
            path.add(new Vec3i(ax, ay, bz));
            path.add(new Vec3i(bx, by, bz));
        }
        return path;
    }

    /**
     * Winding corridor: random walk toward target with perpendicular
     * deviation controlled by {@code windingFactor}. Each step moves one cell.
     */
    @Nonnull
    private List<Vec3i> carveWindingPath(int ax, int ay, int az,
                                          int bx, int by, int bz) {
        List<Vec3i> path = new ArrayList<>();
        int cx = ax;
        int cz = az;
        path.add(new Vec3i(cx, ay, cz));

        int maxSteps = (Math.abs(bx - ax) + Math.abs(bz - az)) * 3;
        int steps = 0;

        while ((cx != bx || cz != bz) && steps < maxSteps) {
            steps++;

            if (random.nextDouble() < 0.3 * config.windingFactor()) {
                // Random perpendicular deviation
                if (cx != bx && cz != bz) {
                    if (Math.abs(bx - cx) > Math.abs(bz - cz)) {
                        cz += random.nextBoolean() ? 1 : -1;
                    } else {
                        cx += random.nextBoolean() ? 1 : -1;
                    }
                } else if (cx == bx) {
                    cx += random.nextBoolean() ? 1 : -1;
                } else {
                    cz += random.nextBoolean() ? 1 : -1;
                }
            } else {
                // Move toward target
                if (Math.abs(bx - cx) >= Math.abs(bz - cz)) {
                    cx += (bx > cx) ? 1 : -1;
                } else {
                    cz += (bz > cz) ? 1 : -1;
                }
            }

            // Clamp to grid bounds
            cx = Math.max(0, Math.min(config.width() - 1, cx));
            cz = Math.max(0, Math.min(config.depth() - 1, cz));

            path.add(new Vec3i(cx, ay, cz));
        }

        // Ensure final point is the target
        if (cx != bx || cz != bz) {
            path.add(new Vec3i(bx, by, bz));
        }

        return path;
    }

    // ============================================
    // Phase 3: Branches
    // ============================================

    /**
     * Add branch corridors from midpoints of long MST corridors.
     *
     * @param graph        the dungeon graph to add branches to
     * @param mstCorridors the MST corridors to branch from
     */
    private void addBranches(@Nonnull DungeonGraph graph,
                              @Nonnull List<Corridor> mstCorridors) {
        for (Corridor corridor : mstCorridors) {
            List<Vec3i> path = corridor.getPath();
            if (path.size() < 10) continue;
            if (random.nextDouble() >= config.branchChance()) continue;

            // Pick midpoint
            Vec3i mid = path.get(path.size() / 2);
            int length = 4 + random.nextInt(9); // 4-12

            // Random cardinal direction
            int dir = random.nextInt(4);
            int endX = mid.x();
            int endZ = mid.z();
            switch (dir) {
                case 0 -> endX += length; // east
                case 1 -> endX -= length; // west
                case 2 -> endZ += length; // south
                case 3 -> endZ -= length; // north
            }

            // Clamp to grid
            endX = Math.max(1, Math.min(config.width() - 2, endX));
            endZ = Math.max(1, Math.min(config.depth() - 2, endZ));

            List<Vec3i> branchPath = carveLShapedPath(
                mid.x(), mid.y(), mid.z(),
                endX, mid.y(), endZ);

            Corridor branch = new Corridor(
                corridor.getFromRoomId(), corridor.getToRoomId(),
                branchPath, config.corridorWidth(), CorridorType.L_SHAPED);
            graph.addCorridor(branch);
        }
    }

    // ============================================
    // Phase 4: Loop Corridors
    // ============================================

    /**
     * Add loop corridors between close room pairs that skip at least one room
     * in the room list ordering (j >= i+2) to avoid redundant connections.
     *
     * @param graph the dungeon graph
     * @param rooms all placed rooms
     */
    private void addLoops(@Nonnull DungeonGraph graph, @Nonnull List<Room> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 2; j < rooms.size(); j++) {
                Room a = rooms.get(i);
                Room b = rooms.get(j);

                if (a.getConnections().contains(b.getId())) continue;

                double dist = euclideanDistance(a, b);
                if (dist < config.width() * 0.5
                    && random.nextDouble() < config.loopChance() * 0.3) {
                    Corridor corridor = carveCorridor(a, b);
                    graph.addCorridor(corridor);
                }
            }
        }
    }

    // ============================================
    // Phase 5: Connectivity Validation
    // ============================================

    /**
     * Ensure all rooms are reachable via BFS; connect disconnected
     * components with additional corridors.
     *
     * @param graph the dungeon graph
     * @param rooms all placed rooms
     */
    private void ensureFullConnectivity(@Nonnull DungeonGraph graph,
                                         @Nonnull List<Room> rooms) {
        int maxAttempts = rooms.size();
        int attempts = 0;

        while (!graph.isFullyConnected() && attempts < maxAttempts) {
            Set<Integer> visited = new HashSet<>();
            Queue<Integer> queue = new LinkedList<>();
            queue.add(rooms.getFirst().getId());
            visited.add(rooms.getFirst().getId());

            while (!queue.isEmpty()) {
                int current = queue.poll();
                Room room = graph.getRoom(current);
                if (room == null) continue;
                for (int nb : room.getConnections()) {
                    if (visited.add(nb)) {
                        queue.add(nb);
                    }
                }
            }

            Room unvisited = null;
            Room closest = null;
            double bestDist = Double.MAX_VALUE;

            for (Room room : rooms) {
                if (!visited.contains(room.getId())) {
                    unvisited = room;
                    for (Room v : rooms) {
                        if (visited.contains(v.getId())) {
                            double dist = euclideanDistance(room, v);
                            if (dist < bestDist) {
                                bestDist = dist;
                                closest = v;
                            }
                        }
                    }
                    break;
                }
            }

            if (unvisited != null && closest != null) {
                Corridor corridor = carveCorridor(closest, unvisited);
                graph.addCorridor(corridor);
            }
            attempts++;
        }
    }

    // ============================================
    // Phase 6: Room Type Assignment
    // ============================================

    /**
     * Assign entrance, exit, boss, and secondary room types to all rooms.
     *
     * @param graph the dungeon graph
     * @param rooms all placed rooms
     */
    private void assignRoomTypes(@Nonnull DungeonGraph graph,
                                  @Nonnull List<Room> rooms) {
        if (rooms.isEmpty()) return;

        // --- Entrance ---
        Room entrance = pickEntrance(rooms);
        entrance.setType(RoomType.ENTRANCE);
        entrance.setEntrance(true);
        graph.setEntranceRoomId(entrance.getId());

        // --- Exit ---
        Room exit = pickExit(rooms, entrance);
        if (exit != null && exit.getId() != entrance.getId()) {
            exit.setType(RoomType.LOOT);
            exit.setExit(true);
        }

        // --- Boss ---
        Room boss = null;
        if (config.bossRoom()) {
            boss = pickBossRoom(rooms, entrance, exit);
            if (boss != null) {
                boss.setType(RoomType.BOSS);
                graph.setBossRoomId(boss.getId());
            }
        }
        // If no boss was picked, set boss to the exit room for critical-path purposes
        if (boss == null && exit != null) {
            graph.setBossRoomId(exit.getId());
        }

        // --- Critical path ---
        List<Integer> criticalPath = graph.getCriticalPath();
        Set<Integer> criticalSet = new HashSet<>(criticalPath);

        // --- Dead-ends, hubs, treasure ---
        for (Room room : rooms) {
            if (room.getType() != RoomType.COMBAT) continue;

            int conns = room.getConnections().size();

            if (conns == 1 && !criticalSet.contains(room.getId())) {
                // Dead-end room
                if (random.nextDouble() < 0.5) {
                    room.setType(RoomType.DEAD_END);
                } else {
                    room.setType(RoomType.LOOT);
                    room.setTreasureRoom(true);
                }
            } else if (conns >= 3) {
                room.setType(RoomType.HUB);
            }
        }

        // --- Scatter safe rooms along the critical path ---
        for (int i = 1; i < criticalPath.size() - 1; i++) {
            Room room = graph.getRoom(criticalPath.get(i));
            if (room == null || room.getType() != RoomType.COMBAT) continue;

            // Place a safe room roughly every 5-6 rooms along the path
            if (i % 5 == 0 && random.nextDouble() < 0.4) {
                room.setType(RoomType.SAFE);
            }
        }
    }

    // ============================================
    // Entrance / Exit / Boss selection
    // ============================================

    /**
     * Pick the entrance room based on configured placement strategy.
     *
     * @param rooms all placed rooms
     * @return the selected entrance room
     */
    @Nonnull
    private Room pickEntrance(@Nonnull List<Room> rooms) {
        String placement = config.entrancePlacement() != null
            ? config.entrancePlacement().toLowerCase() : "edge";
        return switch (placement) {
            case "corner" -> closestToCorner(rooms);
            case "center" -> closestToCenter(rooms);
            case "random" -> rooms.get(random.nextInt(rooms.size()));
            default -> closestToEdge(rooms); // "edge"
        };
    }

    @Nonnull
    private Room closestToEdge(@Nonnull List<Room> rooms) {
        Room best = rooms.getFirst();
        int bestDist = Integer.MAX_VALUE;
        for (Room r : rooms) {
            int distToEdge = Math.min(
                Math.min(r.centerX(), config.width() - r.centerX()),
                Math.min(r.centerZ(), config.depth() - r.centerZ()));
            if (distToEdge < bestDist) {
                bestDist = distToEdge;
                best = r;
            }
        }
        return best;
    }

    @Nonnull
    private Room closestToCorner(@Nonnull List<Room> rooms) {
        int[][] corners = {
            {0, 0},
            {config.width(), 0},
            {0, config.depth()},
            {config.width(), config.depth()}
        };
        Room best = rooms.getFirst();
        int bestDist = Integer.MAX_VALUE;
        for (Room r : rooms) {
            for (int[] c : corners) {
                int dist = Math.abs(r.centerX() - c[0]) + Math.abs(r.centerZ() - c[1]);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = r;
                }
            }
        }
        return best;
    }

    @Nonnull
    private Room closestToCenter(@Nonnull List<Room> rooms) {
        int cx = config.width() / 2;
        int cz = config.depth() / 2;
        Room best = rooms.getFirst();
        int bestDist = Integer.MAX_VALUE;
        for (Room r : rooms) {
            int dist = Math.abs(r.centerX() - cx) + Math.abs(r.centerZ() - cz);
            if (dist < bestDist) {
                bestDist = dist;
                best = r;
            }
        }
        return best;
    }

    /**
     * Pick the exit room: farthest from entrance with distance constraint.
     *
     * @param rooms    all placed rooms
     * @param entrance the entrance room
     * @return the selected exit room, or {@code null} if no suitable room found
     */
    @Nullable
    private Room pickExit(@Nonnull List<Room> rooms, @Nonnull Room entrance) {
        // exitDistance is a normalised minimum; we pick farthest regardless
        Room farthest = null;
        double farthestDist = 0;

        for (Room r : rooms) {
            if (r.getId() == entrance.getId()) continue;
            double dist = euclideanDistance(r, entrance);
            if (dist > farthestDist) {
                farthestDist = dist;
                farthest = r;
            }
        }

        return farthest;
    }

    /**
     * Pick the boss room: largest non-entrance, non-exit room by area.
     *
     * @param rooms    all placed rooms
     * @param entrance the entrance room
     * @param exit     the exit room (may be {@code null})
     * @return the selected boss room, or {@code null} if none qualifies
     */
    @Nullable
    private Room pickBossRoom(@Nonnull List<Room> rooms,
                               @Nonnull Room entrance,
                               @Nullable Room exit) {
        Room best = null;
        int bestArea = 0;
        for (Room r : rooms) {
            if (r.getId() == entrance.getId()) continue;
            if (exit != null && r.getId() == exit.getId()) continue;
            int area = r.getWidth() * r.getDepth();
            if (area > bestArea) {
                bestArea = area;
                best = r;
            }
        }
        return best;
    }

    // ============================================
    // Helpers
    // ============================================

    private int manhattanDistance(@Nonnull Room a, @Nonnull Room b) {
        return Math.abs(a.centerX() - b.centerX()) + Math.abs(a.centerZ() - b.centerZ());
    }

    private double euclideanDistance(@Nonnull Room a, @Nonnull Room b) {
        int dx = a.centerX() - b.centerX();
        int dz = a.centerZ() - b.centerZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
