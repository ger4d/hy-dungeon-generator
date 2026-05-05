package com.duntale.dungeongen.generator.props;

import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.ChestDefinition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Places decorative props into the {@link BlockGrid} based on
 * {@link PropRule} definitions and room context.
 *
 * @since 1.0.0
 */
public class PropPlacer {

    private final Random random;

    /**
     * Create a new prop placer.
     *
     * @param seed RNG seed for deterministic placement
     */
    public PropPlacer(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Place props into the block grid based on the palette theme.
     * Different themes get different prop sets.
     *
     * @param grid        the voxel grid to modify
     * @param graph       the dungeon layout graph
     * @param paletteName the theme palette name
     * @return placed loot container definitions that should be filled at runtime
     */
    @Nonnull
    public List<ChestDefinition> placeProps(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                            @Nonnull String paletteName, boolean removeCeiling) {
        List<PropRule> rules = getPropsForTheme(paletteName);
        List<ChestDefinition> chestDefinitions = new ArrayList<>();

        for (Room room : graph.getRooms()) {
            for (PropRule rule : rules) {
                if (!rule.isAllowedIn(room.getType())) continue;
                // Skip ceiling props when ceiling is removed
                if (removeCeiling && rule.getPlacement() == PropRule.Placement.CEILING) continue;
                placePropsInRoom(grid, room, rule, chestDefinitions);
            }
        }

        return List.copyOf(chestDefinitions);
    }

    private void placePropsInRoom(BlockGrid grid, Room room, PropRule rule, List<ChestDefinition> chestDefinitions) {
        int placed = 0;
        int interiorMinX = room.getX() + 1;
        int interiorMaxX = room.getX() + room.getWidth() - 2;
        int interiorMinZ = room.getZ() + 1;
        int interiorMaxZ = room.getZ() + room.getDepth() - 2;
        int floorY = room.getY() + 1;

        switch (rule.getPlacement()) {
            case WALL_ALIGNED -> {
                int wallY = floorY + rule.getYOffset();
                for (int x = interiorMinX; x <= interiorMaxX && placed < rule.getMaxPerRoom(); x++) {
                    if (tryPlaceWallProp(grid, x, wallY, interiorMinZ, 0, 0, -1, rule, chestDefinitions)) placed++;
                    if (placed < rule.getMaxPerRoom()
                            && tryPlaceWallProp(grid, x, wallY, interiorMaxZ, 0, 0, 1, rule, chestDefinitions)) placed++;
                }
                for (int z = interiorMinZ; z <= interiorMaxZ && placed < rule.getMaxPerRoom(); z++) {
                    if (tryPlaceWallProp(grid, interiorMinX, wallY, z, -1, 0, 0, rule, chestDefinitions)) placed++;
                    if (placed < rule.getMaxPerRoom()
                            && tryPlaceWallProp(grid, interiorMaxX, wallY, z, 1, 0, 0, rule, chestDefinitions)) placed++;
                }
            }
            case CORNER -> {
                int[][] corners = {
                    {interiorMinX, interiorMinZ}, {interiorMinX, interiorMaxZ},
                    {interiorMaxX, interiorMinZ}, {interiorMaxX, interiorMaxZ}
                };
                for (int[] c : corners) {
                    if (placed >= rule.getMaxPerRoom()) break;
                    if (!grid.isBlock(c[0], floorY - 1, c[1])) continue; // must have structural floor
                    if (grid.isAir(c[0], floorY, c[1]) && random.nextDouble() < rule.getSpawnChance()) {
                        grid.set(c[0], floorY, c[1], rule.getBlockId());
                        maybeRecordChest(c[0], floorY, c[1], rule, chestDefinitions);
                        placed++;
                    }
                }
            }
            case CENTER -> {
                int cx = room.centerX();
                int cz = room.centerZ();
                if (!grid.isBlock(cx, floorY - 1, cz)) break; // must have structural floor
                if (grid.isAir(cx, floorY, cz) && random.nextDouble() < rule.getSpawnChance()) {
                    grid.set(cx, floorY, cz, rule.getBlockId());
                    maybeRecordChest(cx, floorY, cz, rule, chestDefinitions);
                    placed++;
                }
            }
            case FLOOR -> {
                for (int attempt = 0; attempt < 20 && placed < rule.getMaxPerRoom(); attempt++) {
                    int rx = interiorMinX + 1 + random.nextInt(Math.max(1, interiorMaxX - interiorMinX - 1));
                    int rz = interiorMinZ + 1 + random.nextInt(Math.max(1, interiorMaxZ - interiorMinZ - 1));
                    if (grid.isAir(rx, floorY, rz) && grid.isBlock(rx, floorY - 1, rz)
                            && random.nextDouble() < rule.getSpawnChance()) {
                        grid.set(rx, floorY, rz, rule.getBlockId());
                        maybeRecordChest(rx, floorY, rz, rule, chestDefinitions);
                        placed++;
                    }
                }
            }
            case CEILING -> {
                int ceilingY = room.getY() + room.getHeight() - 2;
                for (int attempt = 0; attempt < 10 && placed < rule.getMaxPerRoom(); attempt++) {
                    int rx = interiorMinX + random.nextInt(Math.max(1, interiorMaxX - interiorMinX + 1));
                    int rz = interiorMinZ + random.nextInt(Math.max(1, interiorMaxZ - interiorMinZ + 1));
                    if (grid.isAir(rx, ceilingY, rz) && grid.isBlock(rx, ceilingY + 1, rz)
                            && random.nextDouble() < rule.getSpawnChance()) {
                        grid.set(rx, ceilingY, rz, rule.getBlockId());
                        maybeRecordChest(rx, ceilingY, rz, rule, chestDefinitions);
                        placed++;
                    }
                }
            }
        }
    }

    private boolean tryPlaceWallProp(BlockGrid grid, int x, int y, int z,
                                     int wallDx, int wallDy, int wallDz,
                                     PropRule rule,
                                     List<ChestDefinition> chestDefinitions) {
        if (!grid.isAir(x, y, z)) return false;
        if (!grid.isBlock(x + wallDx, y + wallDy, z + wallDz)) return false;
        if (random.nextDouble() >= rule.getSpawnChance()) return false;

        // Compute yaw rotation from wall direction:
        // 0=north(-Z), 1=west(-X), 2=south(+Z), 3=east(+X)
        int rotation = 0;
        if (wallDz == -1)      rotation = 0;
        else if (wallDx == -1) rotation = 1;
        else if (wallDz == 1)  rotation = 2;
        else if (wallDx == 1)  rotation = 3;

        grid.set(x, y, z, rule.getBlockId(), rotation);
        maybeRecordChest(x, y, z, rule, chestDefinitions);
        return true;
    }

    private static void maybeRecordChest(int x, int y, int z,
                                         @Nonnull PropRule rule,
                                         @Nonnull List<ChestDefinition> chestDefinitions) {
        if (rule.getChestTier() == null) {
            return;
        }
        chestDefinitions.add(new ChestDefinition(x, y, z, rule.getChestTier(), rule.getBlockId()));
    }

    // ============================================
    // Asset-backed theme resolution
    // ============================================

    /**
     * Build the prop rule list for a given theme from asset config.
     *
     * @param paletteName the palette name
     * @return list of prop rules for that theme
     */
    @Nonnull
    private List<PropRule> getPropsForTheme(@Nonnull String paletteName) {
        DungeonThemeConfig config = DungeonThemeConfig.get(paletteName);
        if (config != null) {
            return config.toProps();
        }
        // Fallback if assets not loaded
        return List.of();
    }
}
