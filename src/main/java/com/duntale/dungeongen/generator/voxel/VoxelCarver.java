package com.duntale.dungeongen.generator.voxel;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.layout.Corridor;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

/**
 * Converts a {@link DungeonGraph} into carved voxel space via a {@link BlockGrid}.
 * <p>
 * Strategy:
 * <ol>
 *   <li>Fill the entire grid with a solid "fill" block (the primary wall).</li>
 *   <li>For each room: carve the interior to air, leaving 1-block thick
 *       walls, floor, and ceiling.</li>
 *   <li>For each corridor: carve a tube along its waypoints.</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class VoxelCarver {

    private static final int CORRIDOR_HEIGHT = 4;

    private final Random random;
    private final int gridWidth;
    private final int gridHeight;
    private final int gridDepth;

    /**
     * Create a new voxel carver.
     *
     * @param seed       random seed
     * @param gridWidth  X dimension of the output grid
     * @param gridHeight Y dimension of the output grid
     * @param gridDepth  Z dimension of the output grid
     */
    public VoxelCarver(long seed, int gridWidth, int gridHeight, int gridDepth) {
        this.random = new Random(seed);
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.gridDepth = gridDepth;
    }

    /**
     * Carve the dungeon layout into a {@link BlockGrid}.
     *
     * @param graph     the dungeon layout
     * @param fillBlock the default wall block ID (e.g. "Rock_Stone_Brick")
     * @return the carved block grid
     */
    @Nonnull
    public BlockGrid carve(@Nonnull DungeonGraph graph, @Nonnull String fillBlock,
                           boolean removeCeiling, boolean solidFill) {
        BlockGrid grid = new BlockGrid(gridWidth, gridHeight, gridDepth);

        // Step 1: Fill everything solid
        fillSolid(grid, fillBlock);

        // Step 2: Carve rooms
        for (Room room : graph.getRooms()) {
            carveRoom(grid, room);
        }

        // Step 3: Carve corridors
        for (Corridor corridor : graph.getCorridors()) {
            carveCorridor(grid, corridor);
        }

        // Step 4: Seal boundary air cells to prevent geometry leaks
        sealWalls(grid, fillBlock);

        // Step 5 (optional): Hollow out unexposed exterior blocks
        // Must run BEFORE ceiling strip so ceiling is still solid during check
        if (!solidFill) {
            hollowExterior(grid);
        }

        // Step 6 (optional): Remove ceiling blocks for top-down visibility
        if (removeCeiling) {
            stripCeilings(grid, graph);
        }

        if (!solidFill) {
            // Step 7: Fill any remaining holes in walls (1x1 air cells with 3+ solid neighbours)
            fillWalls(grid, fillBlock);
        }

        return grid;
    }

    /**
     * Carve the dungeon layout into a {@link BlockGrid} (ceiling retained).
     *
     * @param graph     the dungeon layout
     * @param fillBlock the default wall block ID
     * @return the carved block grid
     */
    @Nonnull
    public BlockGrid carve(@Nonnull DungeonGraph graph, @Nonnull String fillBlock) {
        return carve(graph, fillBlock, false, true);
    }

    private void fillSolid(@Nonnull BlockGrid grid, @Nonnull String block) {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int z = 0; z < gridDepth; z++) {
                    grid.set(x, y, z, block);
                }
            }
        }
    }

    private void carveRoom(@Nonnull BlockGrid grid, @Nonnull Room room) {
        if (room.hasCells()) {
            // Cell-based carving for non-rectangular shapes (circular, L-shaped, etc.)
            for (int[] cell : room.getCells()) {
                int cx = cell[0];
                int cz = cell[1];
                // Carve column from above floor to below ceiling
                for (int y = room.getY() + 1; y < room.getY() + room.getHeight() - 1; y++) {
                    grid.set(cx, y, cz, null);
                }
            }
        } else {
            // Rectangular fallback: carve interior leaving 1-block walls
            int minX = room.getX() + 1;
            int maxX = room.getX() + room.getWidth() - 1;
            int minZ = room.getZ() + 1;
            int maxZ = room.getZ() + room.getDepth() - 1;
            int minY = room.getY() + 1;  // above floor
            int maxY = room.getY() + room.getHeight() - 1; // below ceiling

            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        grid.set(x, y, z, null); // air
                    }
                }
            }
        }
    }

    private void carveCorridor(@Nonnull BlockGrid grid, @Nonnull Corridor corridor) {
        List<Vec3i> path = corridor.getPath();
        int halfWidth = corridor.getWidth() / 2;
        // Corridors match room height so they're consistent
        int height = gridHeight;

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3i from = path.get(i);
            Vec3i to = path.get(i + 1);
            carveSegment(grid, from, to, halfWidth, height);
        }
    }

    private void carveSegment(@Nonnull BlockGrid grid, @Nonnull Vec3i from,
                              @Nonnull Vec3i to, int halfWidth, int height) {
        int dx = Integer.signum(to.x() - from.x());
        int dy = Integer.signum(to.y() - from.y());
        int dz = Integer.signum(to.z() - from.z());

        int steps = Math.max(
            Math.abs(to.x() - from.x()),
            Math.max(Math.abs(to.y() - from.y()), Math.abs(to.z() - from.z()))
        );

        for (int s = 0; s <= steps; s++) {
            int cx = from.x() + dx * s;
            int cy = from.y();
            int cz = from.z() + dz * s;

            // Carve a cross-section at this point
            for (int ox = -halfWidth; ox <= halfWidth; ox++) {
                for (int oy = 1; oy < height; oy++) { // start at 1 to preserve floor
                    for (int oz = -halfWidth; oz <= halfWidth; oz++) {
                        grid.set(cx + ox, cy + oy, cz + oz, null);
                    }
                }
            }
        }
    }

    /**
     * Remove all blocks above a low wall height so the dungeon is fully
     * visible from above. This strips the entire grid above Y=2, leaving
     * the floor (Y=0) and a 2-block-high wall (Y=1..2) for room boundaries.
     */
    private void stripCeilings(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph) {
        // Remove only the ceiling layer (topmost Y). Rooms and corridors
        // are already carved to full height, so removing Y=gridHeight-1
        // opens them from above while preserving the wall height.
        int ceilingY = gridHeight - 1;

        for (int x = 0; x < gridWidth; x++) {
            for (int z = 0; z < gridDepth; z++) {
                grid.set(x, ceilingY, z, null);
            }
        }
    }

    /**
     * Seal any air cell that sits on the grid boundary. This prevents
     * geometry leaks where carving reaches the edge of the voxel grid.
     *
     * @param grid      the voxel grid
     * @param fillBlock the solid block to seal with
     */
    private void sealWalls(@Nonnull BlockGrid grid, @Nonnull String fillBlock) {
        for (int x = 0; x < gridWidth; x++) {
            for (int z = 0; z < gridDepth; z++) {
                for (int y = 0; y < gridHeight; y++) {
                    if (!grid.isAir(x, y, z)) continue;
                    if (x == 0 || x == gridWidth - 1
                        || z == 0 || z == gridDepth - 1
                        || y == 0 || y == gridHeight - 1) {
                        grid.set(x, y, z, fillBlock);
                    }
                }
            }
        }
    }

    private void fillWalls(@Nonnull BlockGrid grid, @Nonnull String fillBlock) {
        for (int x = 0; x < gridWidth; ++x) {
            for (int z = 0; z < gridDepth; ++z) {
                boolean isWall = false;
                for (int y = 1; y < gridHeight - 1; ++y) {
                    if (!grid.isAir(x, y, z)) {
                        isWall = true;
                        break;
                    }
                }

                if (isWall) {
                    for (int y = 0; y < gridHeight - 1; ++y) {
                        if (grid.isAir(x, y, z)) {
                            grid.set(x, y, z, fillBlock);
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove all solid blocks that have no adjacent air face (fully buried).
     * This leaves only the visible shell — walls, floors, and ceilings — and
     * replaces deep interior blocks with air so the exterior becomes void.
     *
     * @param grid the voxel grid
     */
    private void hollowExterior(@Nonnull BlockGrid grid) {
        // First pass: mark all solid blocks that have NO adjacent air cell.
        // Grid boundaries do NOT count as air — only actual carved air matters.
        boolean[][][] toRemove = new boolean[gridWidth][gridHeight][gridDepth];

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int z = 0; z < gridDepth; z++) {
                    if (grid.isAir(x, y, z)) continue;

                    boolean adjacentToAir = false;
                    if (x > 0 && grid.isAir(x - 1, y, z)) adjacentToAir = true;
                    if (!adjacentToAir && x < gridWidth - 1 && grid.isAir(x + 1, y, z)) adjacentToAir = true;
                    if (!adjacentToAir && y > 0 && grid.isAir(x, y - 1, z)) adjacentToAir = true;
                    if (!adjacentToAir && y < gridHeight - 1 && grid.isAir(x, y + 1, z)) adjacentToAir = true;
                    if (!adjacentToAir && z > 0 && grid.isAir(x, y, z - 1)) adjacentToAir = true;
                    if (!adjacentToAir && z < gridDepth - 1 && grid.isAir(x, y, z + 1)) adjacentToAir = true;

                    if (!adjacentToAir) {
                        toRemove[x][y][z] = true;
                    }
                }
            }
        }

        // Second pass: remove marked blocks
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int z = 0; z < gridDepth; z++) {
                    if (toRemove[x][y][z]) {
                        grid.set(x, y, z, null);
                    }
                }
            }
        }
    }

    /**
     * Apply erosion to exposed wall blocks, randomly converting blocks
     * with two or more adjacent air faces to air. This creates a
     * crumbling, weathered appearance.
     *
     * @param grid          the voxel grid
     * @param erosionFactor 0–1 intensity (values &le; 0.01 are no-ops)
     */
    public void applyErosion(@Nonnull BlockGrid grid, double erosionFactor) {
        if (erosionFactor <= 0.01) return;

        for (int x = 1; x < gridWidth - 1; x++) {
            for (int z = 1; z < gridDepth - 1; z++) {
                for (int y = 1; y < gridHeight - 1; y++) {
                    if (grid.isAir(x, y, z)) continue;

                    // Skip mid-column blocks (solid above AND below) to prevent
                    // 1x1 holes in walls — only erode top/bottom edges
                    if (y > 0 && y < gridHeight - 1
                        && !grid.isAir(x, y - 1, z) && !grid.isAir(x, y + 1, z)) {
                        continue;
                    }

                    int adjAir = 0;
                    if (grid.isAir(x - 1, y, z)) adjAir++;
                    if (grid.isAir(x + 1, y, z)) adjAir++;
                    if (grid.isAir(x, y - 1, z)) adjAir++;
                    if (grid.isAir(x, y + 1, z)) adjAir++;
                    if (grid.isAir(x, y, z - 1)) adjAir++;
                    if (grid.isAir(x, y, z + 1)) adjAir++;

                    if (adjAir >= 2 && random.nextDouble() < erosionFactor * 0.3) {
                        grid.set(x, y, z, null);
                    }
                }
            }
        }
    }
}
