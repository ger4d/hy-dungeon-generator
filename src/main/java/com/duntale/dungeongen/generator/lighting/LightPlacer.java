package com.duntale.dungeongen.generator.lighting;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.layout.Corridor;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
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

    /** All placed wall-torch positions; checked for min-distance enforcement. */
    private final List<int[]> placedTorches = new ArrayList<>();

    /** Next side to place a torch on -1 left, 1 right */
    private int wallSide = -1;

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
            placeLightsInCorridor(
                grid, corridor,
                graph.getRoom(corridor.getFromRoomId()), graph.getRoom(corridor.getToRoomId()),
                lights
            );
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
     * Place floor-standing lights in rooms based on size and type.
     * <p>
     * Each room gets at most <b>one</b> central brazier/campfire ({@code mainFloorLight}).
     * Additional standalone floor torches ({@code accentFloorLight}) may be placed
     * near walls, at least {@link #MIN_FLOOR_TORCH_SPACING} blocks from any other
     * floor torch or wall torch.
     * </p>
     */
    private void placeFloorLights(@Nonnull BlockGrid grid, @Nonnull Room room,
                                  int floorY, @Nonnull LightSet lights) {
        int cx = room.centerX();
        int cz = room.centerZ();
        int w = room.getWidth();
        int d = room.getDepth();

        // --- Central brazier (at most 1 per room) ---
        if (lights.floorLight() != null && w >= 5 && d >= 5) {
            tryPlaceFloorLight(grid, cx, floorY, cz, lights.floorLight());
        }

        // --- Accent floor torches along walls ---
        if (lights.accentFloorLight() != null && w >= 5 && d >= 5) {
            String accent = lights.accentFloorLight();
            int inset = 2;
            int x0 = room.getX() + inset;
            int x1 = room.getX() + w - 1 - inset;
            int z0 = room.getZ() + inset;
            int z1 = room.getZ() + d - 1 - inset;

            // Boss rooms: four corner accent torches
            if (room.getType() == RoomType.BOSS && w >= 8 && d >= 8) {
                tryPlaceAccentTorch(grid, x0, floorY, z0, accent);
                tryPlaceAccentTorch(grid, x1, floorY, z0, accent);
                tryPlaceAccentTorch(grid, x0, floorY, z1, accent);
                tryPlaceAccentTorch(grid, x1, floorY, z1, accent);
            }
            // Large rooms: two flanking torches on the long axis
            else if (w >= 8 || d >= 8) {
                if (w >= d) {
                    tryPlaceAccentTorch(grid, room.getX() + w / 3, floorY, cz, accent);
                    tryPlaceAccentTorch(grid, room.getX() + 2 * w / 3, floorY, cz, accent);
                } else {
                    tryPlaceAccentTorch(grid, cx, floorY, room.getZ() + d / 3, accent);
                    tryPlaceAccentTorch(grid, cx, floorY, room.getZ() + 2 * d / 3, accent);
                }
            }
        }
    }

    /** Minimum blocks between accent floor torches (Manhattan distance). */
    private static final int MIN_FLOOR_TORCH_SPACING = 4;

    /**
     * Attempt to place a floor light at the given position.
     * If the light is two blocks tall, verifies that the block above is
     * air for clearance but only places the base block.
     * Skips positions above fluid blocks.
     */
    private void tryPlaceFloorLight(@Nonnull BlockGrid grid, int x, int y, int z,
                                    @Nonnull FloorLight fl) {
        if (x < 0 || y < 0 || z < 0
                || x >= grid.getWidth() || y >= grid.getHeight() || z >= grid.getDepth()) return;
        if (!grid.isAir(x, y, z)) return;
        if (!grid.isBlock(x, y - 1, z)) return; // must be on structural floor

        if (fl.tall()) {
            if (y + 1 >= grid.getHeight() || !grid.isAir(x, y + 1, z)) return;
        }
        grid.set(x, y, z, fl.blockId());
    }

    /**
     * Place an accent floor torch if the position is valid and far enough
     * from all previously placed torches (wall + floor).
     */
    private void tryPlaceAccentTorch(@Nonnull BlockGrid grid, int x, int y, int z,
                                     @Nonnull String blockId) {
        if (x < 0 || y < 0 || z < 0
                || x >= grid.getWidth() || y >= grid.getHeight() || z >= grid.getDepth()) return;
        if (!grid.isAir(x, y, z)) return;
        if (!grid.isBlock(x, y - 1, z)) return;

        // Enforce minimum distance from every previously placed torch
        for (int[] pos : placedTorches) {
            int dx = Math.abs(x - pos[0]);
            int dz = Math.abs(z - pos[1]);
            if (dx + dz < MIN_FLOOR_TORCH_SPACING) return;
        }

        grid.set(x, y, z, blockId);
        placedTorches.add(new int[]{x, z});
    }

    // ================================================================
    // Corridor lighting
    // ================================================================

    private void placeLightsInCorridor(@Nonnull BlockGrid grid, @Nonnull Corridor corridor,
                                       @Nonnull Room fromRoom, @Nonnull Room toRoom,
                                       @Nonnull LightSet lights) {
        List<Vec3i> path = corridor.getPath();

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3i from = path.get(i);
            Vec3i to = path.get(i + 1);

            // Skip if both endpoints are inside the same room
            if (fromRoom.contains(to.x(), to.z()) ||
                toRoom.contains(from.x(), from.z())) {
                continue;
            }
                       
            int dx = Integer.signum(to.x() - from.x());
            int dz = Integer.signum(to.z() - from.z());
            int steps = Math.max(Math.abs(to.x() - from.x()), Math.abs(to.z() - from.z()));

            int localLeght = 0;

            for (int s = 0; s <= steps; s++) {

                int x = from.x() + dx * s;
                int y = from.y() + 3; // torch at height +3
                int z = from.z() + dz * s;
                
                // Try to place on a wall adjacent to the corridor
                // TODO: This doesn't work well with whinding corridors
                int halfW = corridor.getWidth() / 2 * wallSide;
                int torchX = x - (dx == 0 ? halfW : 0);
                int torchZ = z - (dz == 0 ? halfW : 0);
                int wallDx = dx == 0 ? - 1 * wallSide : 0;
                int wallDz = dz == 0 ? -1 * wallSide : 0;

                if (localLeght > 0 && localLeght++ % 6 != 0) continue; // Space torches every 6 blocks along the corridor) {

                if (fromRoom.contains(torchX, torchZ) || toRoom.contains(torchX, torchZ)) {
                    // Don't place torches on walls inside rooms
                    continue;
                }

                ++localLeght;

                tryPlaceWallLight(grid, torchX, y, torchZ, wallDx, 0, wallDz, lights.wallLight());

                wallSide *= -1;
            }
        }
    }

    // ================================================================
    // Wall light placement
    // ================================================================

    /**
     * Try to place a wall-mounted light at the given position with
     * rotation matching the wall face. Enforces minimum distance from
     * every previously placed torch.
     */
    private void tryPlaceWallLight(@Nonnull BlockGrid grid, int x, int y, int z,
                                   int wallDx, int wallDy, int wallDz,
                                   @Nonnull String lightBlock) {
        if (!grid.isAir(x, y, z) || !grid.canPlace(x, y, z)) return;
        if (!grid.isBlock(x + wallDx, y + wallDy, z + wallDz)) return;

        // Enforce minimum distance from all previously placed torches
        // for (int[] pos : placedTorches) {
        //     int dx = Math.abs(x - pos[0]);
        //     int dz = Math.abs(z - pos[1]);
        //     if (dx + dz < MIN_TORCH_SPACING) return; // Too close — skip
        // }

        // Compute yaw rotation from wall direction:
        // 0=north(-Z), 1=west(-X), 2=south(+Z), 3=east(+X)
        int rotation = 0;
        if (wallDz == -1)      rotation = 0; // attached to north wall
        else if (wallDx == -1) rotation = 1; // attached to west wall
        else if (wallDz == 1)  rotation = 2; // attached to south wall
        else if (wallDx == 1)  rotation = 3; // attached to east wall

        grid.set(x, y, z, lightBlock, rotation);
        placedTorches.add(new int[]{x, z});
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
                new FloorLight("Furniture_Crude_Brazier", false),
                "Furniture_Human_Ruins_Torch"
            );
            case "volcanic" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Forniture_Jungle_Brazier", false),
                "Furniture_Human_Ruins_Torch"
            );
            case "arcane" -> new LightSet(
                "Wood_Torch_Wall",
                "Furniture_Human_Ruins_Lantern_Ceiling",
                new FloorLight("Forniture_Human_Ruins_Brazier", false),
                "Furniture_Human_Ruins_Torch"
            );
            case "mine" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Furniture_Feran_Torch", false),
                "Furniture_Human_Ruins_Torch"
            );
            case "mushroom" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Plant_Crop_Mushroom_Glowing_Purple", false),
                "Furniture_Human_Ruins_Torch"
            );
            case "hive" -> new LightSet(
                "Wood_Torch_Wall",
                "Furniture_Scarak_Hive_Lamp",
                new FloorLight("Furniture_Desert_Torch", true),
                "Furniture_Human_Ruins_Torch"
            );
            case "temple_dark" -> new LightSet(
                "Wood_Torch_Wall",
                null,
                new FloorLight("Forniture_Temple_Dark_Brazier", false),
                "Furniture_Human_Ruins_Torch"
            );
            default -> new LightSet(
                "Wood_Torch_Wall",
                "Deco_Lantern_Ceiling",
                new FloorLight("Furniture_Feran_Torch_Tall", true),
                "Furniture_Human_Ruins_Torch"
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
     * @param wallLight        wall-mounted torch block (with rotation)
     * @param ceilingLight     ceiling-mounted light — nullable
     * @param floorLight       central brazier/campfire (max 1 per room) — nullable
     * @param accentFloorLight standalone floor torch placed near walls — nullable
     */
    private record LightSet(
        @Nonnull String wallLight,
        @Nullable String ceilingLight,
        @Nullable FloorLight floorLight,
        @Nullable String accentFloorLight
    ) {}
}
