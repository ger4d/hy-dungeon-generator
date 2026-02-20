package com.duntale.dungeongen.generator.lighting;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.layout.Corridor;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Places themed light sources in rooms and corridors of a dungeon.
 *
 * <p>Light types supported:</p>
 * <ul>
 *   <li><b>Wall torches</b> — attached to walls at height floorY+3,
 *       minimum 3 blocks apart, with rotation matching the wall face.</li>
 *   <li><b>Ceiling lights</b> — centered in large/boss rooms
 *       (skipped when ceiling is removed).</li>
 *   <li><b>Floor lights</b> — standing braziers/torches placed in rooms,
 *       some occupy 2 vertical blocks.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class LightPlacer {

    /** Minimum blocks between wall torches (horizontal XZ distance). */
    private static final int MIN_TORCH_SPACING = 3;

    private final Random random;

    /**
     * Create a new light placer.
     *
     * @param seed RNG seed for deterministic placement
     */
    public LightPlacer(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Place light sources throughout the dungeon.
     * Every room gets wall torches, optional floor and ceiling lights.
     * Corridors get wall torches at regular intervals.
     *
     * @param grid          the voxel grid to modify
     * @param graph         the dungeon layout graph
     * @param paletteName   the theme palette name
     * @param removeCeiling whether ceilings have been stripped
     */
    public void placeLights(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                             @Nonnull String paletteName, boolean removeCeiling) {
        LightSet lights = getLightsForTheme(paletteName);

        for (Room room : graph.getRooms()) {
            placeLightsInRoom(grid, room, lights, removeCeiling);
        }

        for (Corridor corridor : graph.getCorridors()) {
            placeLightsInCorridor(grid, corridor, lights);
        }
    }

    // ================================================================
    // Room lighting
    // ================================================================

    private void placeLightsInRoom(@Nonnull BlockGrid grid, @Nonnull Room room,
                                   @Nonnull LightSet lights, boolean removeCeiling) {
        int floorY = room.getY() + 1;
        int torchY = floorY + 2; // torches at height +3 above room base

        // --- Wall torches along all four walls ---
        placeWallTorchesForRoom(grid, room, torchY, lights.wallLight());

        // --- Ceiling light — boss rooms and large rooms ---
        if (!removeCeiling && (room.getType() == RoomType.BOSS || room.getWidth() >= 8)
                && lights.ceilingLight() != null) {
            int cx = room.centerX();
            int cz = room.centerZ();
            int ceilingY = room.getY() + room.getHeight() - 2;
            if (grid.isAir(cx, ceilingY, cz)) {
                grid.set(cx, ceilingY, cz, lights.ceilingLight());
            }
        }

        // --- Floor lights — selected rooms get themed floor lighting ---
        placeFloorLights(grid, room, floorY, lights);
    }

    /**
     * Place wall torches along all four interior walls of a room with
     * a minimum spacing of {@link #MIN_TORCH_SPACING} blocks.
     */
    private void placeWallTorchesForRoom(@Nonnull BlockGrid grid, @Nonnull Room room,
                                         int torchY, @Nonnull String wallLight) {
        int minX = room.getX() + 1;
        int maxX = room.getX() + room.getWidth() - 2;
        int minZ = room.getZ() + 1;
        int maxZ = room.getZ() + room.getDepth() - 2;

        int spacing = Math.max(MIN_TORCH_SPACING + 1, 4);

        // North wall (z = minZ, wall at z - 1)
        for (int x = minX + 1; x <= maxX - 1; x += spacing) {
            tryPlaceWallLight(grid, x, torchY, minZ, 0, 0, -1, wallLight);
        }
        // South wall (z = maxZ, wall at z + 1)
        for (int x = minX + 1; x <= maxX - 1; x += spacing) {
            tryPlaceWallLight(grid, x, torchY, maxZ, 0, 0, 1, wallLight);
        }
        // West wall (x = minX, wall at x - 1)
        for (int z = minZ + 1; z <= maxZ - 1; z += spacing) {
            tryPlaceWallLight(grid, minX, torchY, z, -1, 0, 0, wallLight);
        }
        // East wall (x = maxX, wall at x + 1)
        for (int z = minZ + 1; z <= maxZ - 1; z += spacing) {
            tryPlaceWallLight(grid, maxX, torchY, z, 1, 0, 0, wallLight);
        }
    }

    /**
     * Place floor-standing lights in qualifying rooms.
     * Boss rooms and large rooms get corner floor lights; safe rooms get a
     * centered floor light.
     */
    private void placeFloorLights(@Nonnull BlockGrid grid, @Nonnull Room room,
                                  int floorY, @Nonnull LightSet lights) {
        FloorLight fl = lights.floorLight();
        if (fl == null) return;

        int cx = room.centerX();
        int cz = room.centerZ();

        // Safe rooms: single centered floor light
        if (room.getType() == RoomType.SAFE) {
            tryPlaceFloorLight(grid, cx, floorY, cz, fl);
            return;
        }

        // Boss rooms: four corner floor lights
        if (room.getType() == RoomType.BOSS && room.getWidth() >= 6 && room.getDepth() >= 6) {
            int inset = 2;
            int x0 = room.getX() + inset, x1 = room.getX() + room.getWidth() - 1 - inset;
            int z0 = room.getZ() + inset, z1 = room.getZ() + room.getDepth() - 1 - inset;
            tryPlaceFloorLight(grid, x0, floorY, z0, fl);
            tryPlaceFloorLight(grid, x1, floorY, z0, fl);
            tryPlaceFloorLight(grid, x0, floorY, z1, fl);
            tryPlaceFloorLight(grid, x1, floorY, z1, fl);
            return;
        }

        // Large rooms (>= 8 wide): center floor light
        if (room.getWidth() >= 8 || room.getDepth() >= 8) {
            tryPlaceFloorLight(grid, cx, floorY, cz, fl);
        }
    }

    /**
     * Attempt to place a floor light at the given position.
     * If the light is two blocks tall, verifies the block above is also air.
     */
    private void tryPlaceFloorLight(@Nonnull BlockGrid grid, int x, int y, int z,
                                    @Nonnull FloorLight fl) {
        if (x < 0 || y < 0 || z < 0
                || x >= grid.getWidth() || y >= grid.getHeight() || z >= grid.getDepth()) return;
        if (!grid.isAir(x, y, z)) return;
        if (!grid.isSolid(x, y - 1, z)) return; // must be on solid floor

        if (fl.tall()) {
            if (y + 1 >= grid.getHeight() || !grid.isAir(x, y + 1, z)) return;
            grid.set(x, y + 1, z, fl.blockId()); // top piece
        }
        grid.set(x, y, z, fl.blockId());
    }

    // ================================================================
    // Corridor lighting
    // ================================================================

    private void placeLightsInCorridor(@Nonnull BlockGrid grid, @Nonnull Corridor corridor,
                                       @Nonnull LightSet lights) {
        List<Vec3i> path = corridor.getPath();
        int totalLength = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3i from = path.get(i);
            Vec3i to = path.get(i + 1);
            int dx = Integer.signum(to.x() - from.x());
            int dz = Integer.signum(to.z() - from.z());
            int steps = Math.max(Math.abs(to.x() - from.x()), Math.abs(to.z() - from.z()));

            for (int s = 0; s <= steps; s++) {
                totalLength++;
                if (totalLength % 6 != 0) continue;

                int x = from.x() + dx * s;
                int y = from.y() + 3; // torch at height +3
                int z = from.z() + dz * s;

                // Try to place on a wall adjacent to the corridor
                int halfW = corridor.getWidth() / 2;
                if (dx != 0) {
                    tryPlaceWallLight(grid, x, y, z - halfW, 0, 0, -1, lights.wallLight());
                } else {
                    tryPlaceWallLight(grid, x - halfW, y, z, -1, 0, 0, lights.wallLight());
                }
            }
        }
    }

    // ================================================================
    // Wall light placement
    // ================================================================

    /**
     * Try to place a wall-mounted light at the given position with
     * rotation matching the wall face.
     */
    private void tryPlaceWallLight(@Nonnull BlockGrid grid, int x, int y, int z,
                                   int wallDx, int wallDy, int wallDz,
                                   @Nonnull String lightBlock) {
        if (x < 0 || y < 0 || z < 0
                || x >= grid.getWidth() || y >= grid.getHeight() || z >= grid.getDepth()) return;
        if (!grid.isAir(x, y, z)) return;
        if (!grid.isSolid(x + wallDx, y + wallDy, z + wallDz)) return;

        // Compute yaw rotation from wall direction:
        // 0=north(-Z), 1=west(-X), 2=south(+Z), 3=east(+X)
        int rotation = 0;
        if (wallDz == -1)      rotation = 0; // attached to north wall
        else if (wallDx == -1) rotation = 1; // attached to west wall
        else if (wallDz == 1)  rotation = 2; // attached to south wall
        else if (wallDx == 1)  rotation = 3; // attached to east wall

        grid.set(x, y, z, lightBlock, rotation);
    }

    // ================================================================
    // Theme resolution
    // ================================================================

    /**
     * Resolve the light set for the given palette theme.
     *
     * @param paletteName the palette name
     * @return the themed light set
     */
    @Nonnull
    private LightSet getLightsForTheme(@Nonnull String paletteName) {
        return switch (paletteName) {
            case "crypt" -> new LightSet(
                "Wood_Torch_Wall",
                "Deco_Lantern_Ceiling",
                new FloorLight("Furniture_Crude_Brazier", false)
            );
            case "volcanic" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Forniture_Jungle_Brazier", false)
            );
            case "arcane" -> new LightSet(
                "Wood_Torch_Wall",
                "Furniture_Human_Ruins_Lantern_Ceiling",
                new FloorLight("Forniture_Human_Ruins_Brazier", false)
            );
            case "mine" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Furniture_Feran_Torch", false)
            );
            case "mushroom" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Plant_Crop_Mushroom_Glowing_Purple", false)
            );
            case "hive" -> new LightSet(
                "Wood_Torch_Wall",
                "Furniture_Scarak_Hive_Lamp",
                new FloorLight("Furniture_Desert_Torch", true)
            );
            case "temple_dark" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Forniture_Temple_Dark_Brazier", false)
            );
            default -> new LightSet(
                "Wood_Torch_Wall",
                "Deco_Lantern_Ceiling",
                new FloorLight("Furniture_Feran_Torch_Tall", true)
            );
        };
    }

    // ================================================================
    // Data records
    // ================================================================

    /**
     * A floor-standing light block.
     *
     * @param blockId the block type key
     * @param tall    whether the light occupies 2 vertical blocks
     */
    private record FloorLight(@Nonnull String blockId, boolean tall) {}

    /**
     * A set of themed light blocks for a palette.
     *
     * @param wallLight    wall-mounted torch block (with rotation)
     * @param ceilingLight ceiling-mounted light — nullable
     * @param floorLight   floor-standing light — nullable
     */
    private record LightSet(
        @Nonnull String wallLight,
        @Nullable String ceilingLight,
        @Nullable FloorLight floorLight
    ) {}
}
