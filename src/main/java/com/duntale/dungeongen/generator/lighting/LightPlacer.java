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
 * @since 1.0.0
 */
public class LightPlacer {

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
     * Every room gets at least one light. Corridors get lights at intervals.
     *
     * @param grid        the voxel grid to modify
     * @param graph       the dungeon layout graph
     * @param paletteName the theme palette name
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

    private void placeLightsInRoom(BlockGrid grid, Room room, LightSet lights, boolean removeCeiling) {
        int interiorMinX = room.getX() + 1;
        int interiorMaxX = room.getX() + room.getWidth() - 2;
        int interiorMinZ = room.getZ() + 1;
        int interiorMaxZ = room.getZ() + room.getDepth() - 2;
        int floorY = room.getY() + 1;
        int lightY = floorY + 1; // torches at eye level

        int spacing = 5;

        // North and South walls
        for (int x = interiorMinX; x <= interiorMaxX; x += spacing) {
            tryPlaceWallLight(grid, x, lightY, interiorMinZ, 0, 0, -1, lights.wallLight());
            tryPlaceWallLight(grid, x, lightY, interiorMaxZ, 0, 0, 1, lights.wallLight());
        }
        // East and West walls
        for (int z = interiorMinZ; z <= interiorMaxZ; z += spacing) {
            tryPlaceWallLight(grid, interiorMinX, lightY, z, -1, 0, 0, lights.wallLight());
            tryPlaceWallLight(grid, interiorMaxX, lightY, z, 1, 0, 0, lights.wallLight());
        }

        // Boss rooms and large rooms get a center ceiling light (only if ceiling exists)
        if (!removeCeiling && (room.getType() == RoomType.BOSS || room.getWidth() >= 8) && lights.ceilingLight() != null) {
            int cx = room.centerX();
            int cz = room.centerZ();
            int ceilingY = room.getY() + room.getHeight() - 2;
            if (grid.isAir(cx, ceilingY, cz)) {
                grid.set(cx, ceilingY, cz, lights.ceilingLight());
            }
        }

        // SAFE rooms get a floor standing light (brazier/lantern)
        if (room.getType() == RoomType.SAFE && lights.floorLight() != null) {
            int cx = room.centerX();
            int cz = room.centerZ();
            if (grid.isAir(cx, floorY, cz)) {
                grid.set(cx, floorY, cz, lights.floorLight());
            }
        }
    }

    private void placeLightsInCorridor(BlockGrid grid, Corridor corridor, LightSet lights) {
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
                if (totalLength % 8 != 0) continue;

                int x = from.x() + dx * s;
                int y = from.y() + 2; // torch height
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

    private void tryPlaceWallLight(BlockGrid grid, int x, int y, int z,
                                   int wallDx, int wallDy, int wallDz,
                                   String lightBlock) {
        if (x < 0 || y < 0 || z < 0
                || x >= grid.getWidth() || y >= grid.getHeight() || z >= grid.getDepth()) return;
        if (!grid.isAir(x, y, z)) return;
        if (!grid.isSolid(x + wallDx, y + wallDy, z + wallDz)) return;
        grid.set(x, y, z, lightBlock);
    }

    /**
     * Resolve the light set for the given palette theme.
     *
     * @param paletteName the palette name
     * @return the themed light set
     */
    @Nonnull
    private LightSet getLightsForTheme(@Nonnull String paletteName) {
        return switch (paletteName) {
            case "crypt" -> new LightSet("Furniture_Crude_Torch", "Deco_Lantern_Ceiling", "Furniture_Ancient_Candle");
            case "volcanic" -> new LightSet("Furniture_Crude_Torch", null, "Furniture_Dungeon_Earth_Brazier");
            case "arcane" -> new LightSet("Furniture_Crude_Torch", "Furniture_Human_Ruins_Lantern_Ceiling", "Furniture_Royal_Magic_Potion_Glow");
            case "mine" -> new LightSet("Furniture_Crude_Torch", null, null);
            case "mushroom" -> new LightSet("Furniture_Crude_Torch", null, "Plant_Crop_Mushroom_Glowing_Purple");
            case "hive" -> new LightSet("Furniture_Crude_Torch", "Furniture_Scarak_Hive_Lamp", null);
            case "temple_dark" -> new LightSet("Furniture_Crude_Torch", null, "Furniture_Temple_Dark_Brazier");
            default -> new LightSet("Furniture_Crude_Torch", "Deco_Lantern_Ceiling", null);
        };
    }

    /**
     * A set of themed light blocks.
     *
     * @param wallLight    wall-mounted torch/sconce
     * @param ceilingLight ceiling-mounted lantern/chandelier (nullable)
     * @param floorLight   floor-standing brazier/lantern (nullable)
     */
    private record LightSet(@Nonnull String wallLight,
                            @Nullable String ceilingLight,
                            @Nullable String floorLight) {}
}
