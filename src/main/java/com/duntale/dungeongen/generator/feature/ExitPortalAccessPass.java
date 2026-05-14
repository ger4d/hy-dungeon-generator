package com.duntale.dungeongen.generator.feature;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.layout.Corridor;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Repairs the generated exit portal anchor and shortest dry access path.
 */
public final class ExitPortalAccessPass {

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    /**
     * Resolves the intended exit portal anchor without requiring the floor to already be dry.
     *
     * @param grid the final generated block grid to inspect
     * @param room the exit room containing the portal anchor
     * @return the preferred standing position, or {@code null} if no room cell can be patched
     */
    @Nullable
    public Vec3i resolveAnchor(@Nonnull BlockGrid grid, @Nonnull Room room) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(room, "room");

        return findPatchableStandingPosition(grid, room, getRoomStandingPosition(room));
    }

    /**
     * Resolves a reference position indicating where players enter the exit room from.
     *
     * @param graph the generated dungeon graph
     * @param exitRoomId the room containing the exit portal
     * @return a corridor or room position facing the incoming side, or {@code null} if unavailable
     */
    @Nullable
    public Vec3i resolveIncomingAccessPosition(@Nonnull DungeonGraph graph, int exitRoomId) {
        Objects.requireNonNull(graph, "graph");

        List<Integer> path = findRoomPath(graph, graph.getEntranceRoomId(), exitRoomId);
        if (path.size() < 2) {
            return null;
        }

        int previousRoomId = path.get(path.size() - 2);
        return findCorridorReferencePosition(graph, previousRoomId, exitRoomId);
    }

    /**
     * Protects the exit portal's anchor cell and shortest dry access path.
     *
     * @param grid the final generated block grid to patch
     * @param room the exit room containing the portal anchor
     * @param exitPosition the standing position where the portal will spawn
     * @param fallbackFloorBlock structural floor block used when no dry floor exists nearby
     * @return details of the protected exit position and cleared standing cells
     */
    @Nonnull
    public ExitPortalAccess protect(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Vec3i exitPosition,
            @Nonnull String fallbackFloorBlock
    ) {
        return protect(grid, room, exitPosition, fallbackFloorBlock, null);
    }

    /**
     * Protects the exit portal anchor and favors an access path facing a preferred position.
     *
     * @param grid the final generated block grid to patch
     * @param room the exit room containing the portal anchor
     * @param exitPosition the standing position where the portal will spawn
     * @param fallbackFloorBlock structural floor block used when no dry floor exists nearby
     * @param preferredAccessPosition optional room/corridor position the bridge should face
     * @return details of the protected exit position and cleared standing cells
     */
    @Nonnull
    public ExitPortalAccess protect(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Vec3i exitPosition,
            @Nonnull String fallbackFloorBlock,
            @Nullable Vec3i preferredAccessPosition
    ) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(exitPosition, "exitPosition");
        Objects.requireNonNull(fallbackFloorBlock, "fallbackFloorBlock");

        int floorY = exitPosition.y() - 1;
        int standingY = exitPosition.y();
        int headY = exitPosition.y() + 1;
        Cell anchor = new Cell(exitPosition.x(), exitPosition.z());
        Set<Vec3i> protectedStandingCells = new HashSet<>();

        if (!canPatchCell(grid, room, anchor, floorY, standingY, headY)) {
            return new ExitPortalAccess(exitPosition, Set.of());
        }

        String floorBlock = selectFloorBlock(grid, room, anchor, floorY, standingY, headY, fallbackFloorBlock);
        List<Cell> path = findPathToDryFloor(grid, room, anchor, floorY, standingY, headY, preferredAccessPosition);
        if (path == null) {
            patchCell(grid, anchor, floorY, standingY, headY, floorBlock, protectedStandingCells);
        } else {
            for (Cell cell : path) {
                patchCell(grid, cell, floorY, standingY, headY, floorBlock, protectedStandingCells);
            }
        }

        return new ExitPortalAccess(exitPosition, Set.copyOf(protectedStandingCells));
    }

    @Nonnull
    private static List<Integer> findRoomPath(@Nonnull DungeonGraph graph, int startRoomId, int targetRoomId) {
        if (startRoomId < 0 || targetRoomId < 0) {
            return List.of();
        }
        if (startRoomId == targetRoomId) {
            return List.of(startRoomId);
        }

        Queue<Integer> queue = new ArrayDeque<>();
        Map<Integer, Integer> parents = new HashMap<>();
        queue.add(startRoomId);
        parents.put(startRoomId, -1);

        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (current == targetRoomId) {
                break;
            }

            Room room = graph.getRoom(current);
            if (room == null) {
                continue;
            }

            for (int neighbour : room.getConnections()) {
                if (!parents.containsKey(neighbour)) {
                    parents.put(neighbour, current);
                    queue.add(neighbour);
                }
            }
        }

        if (!parents.containsKey(targetRoomId)) {
            return List.of();
        }

        List<Integer> path = new ArrayList<>();
        int current = targetRoomId;
        while (current != -1) {
            path.add(current);
            current = parents.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    @Nullable
    private static Vec3i findCorridorReferencePosition(
            @Nonnull DungeonGraph graph,
            int previousRoomId,
            int exitRoomId
    ) {
        for (Corridor corridor : graph.getCorridors()) {
            if (corridor.getFromRoomId() == previousRoomId && corridor.getToRoomId() == exitRoomId) {
                List<Vec3i> path = corridor.getPath();
                if (path.size() >= 2) {
                    return path.get(path.size() - 2);
                }
            }
            if (corridor.getFromRoomId() == exitRoomId && corridor.getToRoomId() == previousRoomId) {
                List<Vec3i> path = corridor.getPath();
                if (path.size() >= 2) {
                    return path.get(1);
                }
            }
        }

        Room previousRoom = graph.getRoom(previousRoomId);
        return previousRoom != null ? getRoomStandingPosition(previousRoom) : null;
    }

    @Nonnull
    private static String selectFloorBlock(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell anchor,
            int floorY,
            int standingY,
            int headY,
            @Nonnull String fallbackFloorBlock
    ) {
        String anchorBlock = grid.get(anchor.x(), floorY, anchor.z());
        if (anchorBlock != null && grid.isBlock(anchor.x(), floorY, anchor.z())) {
            return anchorBlock;
        }

        String nearestBlock = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
            for (int z = room.getZ(); z < room.getZ() + room.getDepth(); z++) {
                Cell cell = new Cell(x, z);
                if (!canPatchCell(grid, room, cell, floorY, standingY, headY)) {
                    continue;
                }
                if (!grid.isBlock(x, floorY, z)) {
                    continue;
                }

                int distance = Math.abs(anchor.x() - x) + Math.abs(anchor.z() - z);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestBlock = grid.get(x, floorY, z);
                }
            }
        }

        return nearestBlock != null ? nearestBlock : fallbackFloorBlock;
    }

    @Nullable
    private static List<Cell> findPathToDryFloor(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell anchor,
            int floorY,
            int standingY,
            int headY,
            @Nullable Vec3i preferredAccessPosition
    ) {
        Cell preferredDirection = preferredDirection(anchor, preferredAccessPosition);
        if (preferredDirection != null) {
            Cell target = findDryTargetAlongRay(grid, room, anchor, preferredDirection, floorY, standingY, headY);
            if (target != null) {
                List<Cell> path = findPathToTarget(grid, room, anchor, target, floorY, standingY, headY, preferredDirection);
                if (path != null) {
                    return path;
                }
            }

            target = findEntranceFacingDryTarget(grid, room, anchor, preferredDirection, floorY, standingY, headY);
            if (target != null) {
                List<Cell> path = findPathToTarget(grid, room, anchor, target, floorY, standingY, headY, preferredDirection);
                if (path != null) {
                    return path;
                }
            }
        }

        return findShortestPathToDryFloor(grid, room, anchor, floorY, standingY, headY);
    }

    @Nullable
    private static List<Cell> findShortestPathToDryFloor(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell anchor,
            int floorY,
            int standingY,
            int headY
    ) {
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        Set<Cell> visited = new HashSet<>();
        Map<Cell, Cell> parents = new HashMap<>();

        queue.add(anchor);
        visited.add(anchor);

        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            if (isDryWalkable(grid, current, floorY, standingY, headY)) {
                return reconstructPath(current, parents);
            }

            for (int[] direction : DIRECTIONS) {
                Cell next = new Cell(current.x() + direction[0], current.z() + direction[1]);
                if (visited.contains(next)) {
                    continue;
                }
                if (!canPatchCell(grid, room, next, floorY, standingY, headY)) {
                    continue;
                }

                visited.add(next);
                parents.put(next, current);
                queue.addLast(next);
            }
        }

        return null;
    }

    @Nullable
    private static Cell findDryTargetAlongRay(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell anchor,
            @Nonnull Cell direction,
            int floorY,
            int standingY,
            int headY
    ) {
        Cell current = new Cell(anchor.x() + direction.x(), anchor.z() + direction.z());
        while (canPatchCell(grid, room, current, floorY, standingY, headY)) {
            if (isDryWalkable(grid, current, floorY, standingY, headY)) {
                return current;
            }
            current = new Cell(current.x() + direction.x(), current.z() + direction.z());
        }

        return null;
    }

    @Nullable
    private static Cell findEntranceFacingDryTarget(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell anchor,
            @Nonnull Cell direction,
            int floorY,
            int standingY,
            int headY
    ) {
        Cell best = null;
        int bestLateral = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;

        for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
            for (int z = room.getZ(); z < room.getZ() + room.getDepth(); z++) {
                Cell cell = new Cell(x, z);
                if (!canPatchCell(grid, room, cell, floorY, standingY, headY)) {
                    continue;
                }
                if (!isDryWalkable(grid, cell, floorY, standingY, headY)) {
                    continue;
                }

                int forward = (cell.x() - anchor.x()) * direction.x() + (cell.z() - anchor.z()) * direction.z();
                if (forward <= 0) {
                    continue;
                }

                int lateral = direction.x() != 0 ? Math.abs(cell.z() - anchor.z()) : Math.abs(cell.x() - anchor.x());
                int distance = Math.abs(cell.x() - anchor.x()) + Math.abs(cell.z() - anchor.z());
                if (lateral < bestLateral || (lateral == bestLateral && distance < bestDistance)) {
                    best = cell;
                    bestLateral = lateral;
                    bestDistance = distance;
                }
            }
        }

        return best;
    }

    @Nullable
    private static List<Cell> findPathToTarget(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell anchor,
            @Nonnull Cell target,
            int floorY,
            int standingY,
            int headY,
            @Nonnull Cell preferredDirection
    ) {
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        Set<Cell> visited = new HashSet<>();
        Map<Cell, Cell> parents = new HashMap<>();

        queue.add(anchor);
        visited.add(anchor);
        List<Cell> directions = orderedDirections(preferredDirection);

        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            if (current.equals(target)) {
                return reconstructPath(current, parents);
            }

            for (Cell direction : directions) {
                Cell next = new Cell(current.x() + direction.x(), current.z() + direction.z());
                if (visited.contains(next)) {
                    continue;
                }
                if (!canPatchCell(grid, room, next, floorY, standingY, headY)) {
                    continue;
                }

                visited.add(next);
                parents.put(next, current);
                queue.addLast(next);
            }
        }

        return null;
    }

    @Nullable
    private static Cell preferredDirection(@Nonnull Cell anchor, @Nullable Vec3i preferredAccessPosition) {
        if (preferredAccessPosition == null) {
            return null;
        }

        int dx = preferredAccessPosition.x() - anchor.x();
        int dz = preferredAccessPosition.z() - anchor.z();
        if (dx == 0 && dz == 0) {
            return null;
        }
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
            return new Cell(Integer.signum(dx), 0);
        }
        if (dz != 0) {
            return new Cell(0, Integer.signum(dz));
        }
        return new Cell(Integer.signum(dx), 0);
    }

    @Nonnull
    private static List<Cell> orderedDirections(@Nonnull Cell preferredDirection) {
        List<Cell> directions = new ArrayList<>(4);
        directions.add(preferredDirection);
        if (preferredDirection.x() != 0) {
            directions.add(new Cell(0, 1));
            directions.add(new Cell(0, -1));
        } else {
            directions.add(new Cell(1, 0));
            directions.add(new Cell(-1, 0));
        }
        directions.add(new Cell(-preferredDirection.x(), -preferredDirection.z()));
        return directions;
    }

    @Nonnull
    private static List<Cell> reconstructPath(@Nonnull Cell target, @Nonnull Map<Cell, Cell> parents) {
        List<Cell> path = new ArrayList<>();
        Cell current = target;
        while (current != null) {
            path.add(current);
            current = parents.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean isDryWalkable(
            @Nonnull BlockGrid grid,
            @Nonnull Cell cell,
            int floorY,
            int standingY,
            int headY
    ) {
        return grid.isBlock(cell.x(), floorY, cell.z())
                && grid.isAir(cell.x(), standingY, cell.z())
                && grid.isAir(cell.x(), headY, cell.z());
    }

    private static void patchCell(
            @Nonnull BlockGrid grid,
            @Nonnull Cell cell,
            int floorY,
            int standingY,
            int headY,
            @Nonnull String floorBlock,
            @Nonnull Set<Vec3i> protectedStandingCells
    ) {
        if (!grid.isBlock(cell.x(), floorY, cell.z())) {
            grid.set(cell.x(), floorY, cell.z(), floorBlock);
        }
        grid.set(cell.x(), standingY, cell.z(), null);
        grid.set(cell.x(), headY, cell.z(), null);
        protectedStandingCells.add(new Vec3i(cell.x(), standingY, cell.z()));
    }

    private static boolean canPatchCell(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Cell cell,
            int floorY,
            int standingY,
            int headY
    ) {
        return isRoomInteriorPosition(room, cell.x(), cell.z())
                && grid.canPlace(cell.x(), floorY, cell.z())
                && grid.canPlace(cell.x(), standingY, cell.z())
                && grid.canPlace(cell.x(), headY, cell.z());
    }

    @Nullable
    private static Vec3i findPatchableStandingPosition(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            @Nonnull Vec3i candidate
    ) {
        int y = candidate.y();
        if (isPatchableStandingPosition(grid, room, candidate.x(), y, candidate.z())) {
            return candidate;
        }

        int maxRadius = Math.max(1, Math.max(room.getWidth(), room.getDepth()) / 2);
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = candidate.x() + dx;
                    int z = candidate.z() + dz;
                    if (isPatchableStandingPosition(grid, room, x, y, z)) {
                        return new Vec3i(x, y, z);
                    }
                }
            }
        }

        return null;
    }

    private static boolean isPatchableStandingPosition(
            @Nonnull BlockGrid grid,
            @Nonnull Room room,
            int x,
            int y,
            int z
    ) {
        return isRoomInteriorPosition(room, x, z)
                && grid.canPlace(x, y - 1, z)
                && grid.canPlace(x, y, z)
                && grid.canPlace(x, y + 1, z);
    }

    @Nonnull
    private static Vec3i getRoomStandingPosition(@Nonnull Room room) {
        return new Vec3i(room.centerX(), room.getY() + 1, room.centerZ());
    }

    private static boolean isRoomInteriorPosition(@Nonnull Room room, int x, int z) {
        if (room.hasCells()) {
            for (int[] cell : room.getCells()) {
                if (cell[0] == x && cell[1] == z) {
                    return true;
                }
            }
            return false;
        }

        return x >= room.getX() + 1
                && x < room.getX() + room.getWidth() - 1
                && z >= room.getZ() + 1
                && z < room.getZ() + room.getDepth() - 1;
    }

    /**
     * Result of protecting an exit portal anchor and optional access path.
     *
     * @param exitPosition the final exit standing position
     * @param protectedStandingCells the standing-height cells cleared for access
     */
    public record ExitPortalAccess(@Nonnull Vec3i exitPosition, @Nonnull Set<Vec3i> protectedStandingCells) {

        public ExitPortalAccess {
            Objects.requireNonNull(exitPosition, "exitPosition");
            protectedStandingCells = Set.copyOf(Objects.requireNonNull(protectedStandingCells, "protectedStandingCells"));
        }
    }

    private record Cell(int x, int z) {}
}