package com.duntale.dungeongen.generator.props;

import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

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
     */
    public void placeProps(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph, @Nonnull String paletteName) {
        List<PropRule> rules = getPropsForTheme(paletteName);

        for (Room room : graph.getRooms()) {
            for (PropRule rule : rules) {
                if (!rule.isAllowedIn(room.getType())) continue;
                placePropsInRoom(grid, room, rule);
            }
        }
    }

    private void placePropsInRoom(BlockGrid grid, Room room, PropRule rule) {
        int placed = 0;
        int interiorMinX = room.getX() + 1;
        int interiorMaxX = room.getX() + room.getWidth() - 2;
        int interiorMinZ = room.getZ() + 1;
        int interiorMaxZ = room.getZ() + room.getDepth() - 2;
        int floorY = room.getY() + 1;

        switch (rule.getPlacement()) {
            case WALL_ALIGNED -> {
                for (int x = interiorMinX; x <= interiorMaxX && placed < rule.getMaxPerRoom(); x++) {
                    if (tryPlaceWallProp(grid, x, floorY, interiorMinZ, 0, 0, -1, rule)) placed++;
                    if (placed < rule.getMaxPerRoom() && tryPlaceWallProp(grid, x, floorY, interiorMaxZ, 0, 0, 1, rule)) placed++;
                }
                for (int z = interiorMinZ; z <= interiorMaxZ && placed < rule.getMaxPerRoom(); z++) {
                    if (tryPlaceWallProp(grid, interiorMinX, floorY, z, -1, 0, 0, rule)) placed++;
                    if (placed < rule.getMaxPerRoom() && tryPlaceWallProp(grid, interiorMaxX, floorY, z, 1, 0, 0, rule)) placed++;
                }
            }
            case CORNER -> {
                int[][] corners = {
                    {interiorMinX, interiorMinZ}, {interiorMinX, interiorMaxZ},
                    {interiorMaxX, interiorMinZ}, {interiorMaxX, interiorMaxZ}
                };
                for (int[] c : corners) {
                    if (placed >= rule.getMaxPerRoom()) break;
                    if (grid.isAir(c[0], floorY, c[1]) && random.nextDouble() < rule.getSpawnChance()) {
                        grid.set(c[0], floorY, c[1], rule.getBlockId());
                        placed++;
                    }
                }
            }
            case CENTER -> {
                int cx = room.centerX();
                int cz = room.centerZ();
                if (grid.isAir(cx, floorY, cz) && random.nextDouble() < rule.getSpawnChance()) {
                    grid.set(cx, floorY, cz, rule.getBlockId());
                }
            }
            case FLOOR -> {
                for (int attempt = 0; attempt < 20 && placed < rule.getMaxPerRoom(); attempt++) {
                    int rx = interiorMinX + 1 + random.nextInt(Math.max(1, interiorMaxX - interiorMinX - 1));
                    int rz = interiorMinZ + 1 + random.nextInt(Math.max(1, interiorMaxZ - interiorMinZ - 1));
                    if (grid.isAir(rx, floorY, rz) && grid.isSolid(rx, floorY - 1, rz)
                            && random.nextDouble() < rule.getSpawnChance()) {
                        grid.set(rx, floorY, rz, rule.getBlockId());
                        placed++;
                    }
                }
            }
            case CEILING -> {
                int ceilingY = room.getY() + room.getHeight() - 2;
                for (int attempt = 0; attempt < 10 && placed < rule.getMaxPerRoom(); attempt++) {
                    int rx = interiorMinX + random.nextInt(Math.max(1, interiorMaxX - interiorMinX + 1));
                    int rz = interiorMinZ + random.nextInt(Math.max(1, interiorMaxZ - interiorMinZ + 1));
                    if (grid.isAir(rx, ceilingY, rz) && grid.isSolid(rx, ceilingY + 1, rz)
                            && random.nextDouble() < rule.getSpawnChance()) {
                        grid.set(rx, ceilingY, rz, rule.getBlockId());
                        placed++;
                    }
                }
            }
        }
    }

    private boolean tryPlaceWallProp(BlockGrid grid, int x, int y, int z,
                                     int wallDx, int wallDy, int wallDz,
                                     PropRule rule) {
        if (!grid.isAir(x, y, z)) return false;
        if (!grid.isSolid(x + wallDx, y + wallDy, z + wallDz)) return false;
        if (random.nextDouble() >= rule.getSpawnChance()) return false;
        grid.set(x, y, z, rule.getBlockId());
        return true;
    }

    // ============================================
    // Theme prop sets
    // ============================================

    /**
     * Build the prop rule list for a given theme.
     *
     * @param paletteName the palette name
     * @return list of prop rules for that theme
     */
    @Nonnull
    private List<PropRule> getPropsForTheme(@Nonnull String paletteName) {
        return switch (paletteName) {
            case "crypt" -> getCryptProps();
            case "volcanic" -> getVolcanicProps();
            case "arcane" -> getArcaneProps();
            case "mine" -> getMineProps();
            case "mushroom" -> getMushroomProps();
            case "hive" -> getHiveProps();
            case "temple_dark" -> getTempleDarkProps();
            default -> getCryptProps();
        };
    }

    private List<PropRule> getCryptProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Deco_SpiderWeb", PropRule.Placement.CORNER, 0.7, 4, null));
        props.add(new PropRule("Deco_Bone_Skulls", PropRule.Placement.CORNER, 0.3, 2,
                new RoomType[]{RoomType.COMBAT, RoomType.DEAD_END, RoomType.BOSS}));
        props.add(new PropRule("Deco_Bone_Pile", PropRule.Placement.FLOOR, 0.2, 3,
                new RoomType[]{RoomType.COMBAT, RoomType.DEAD_END}));
        props.add(new PropRule("Furniture_Ancient_Pot", PropRule.Placement.FLOOR, 0.15, 2, null));
        props.add(new PropRule("Furniture_Ancient_Barrel", PropRule.Placement.FLOOR, 0.1, 2,
                new RoomType[]{RoomType.SAFE, RoomType.LOOT}));
        props.add(new PropRule("Furniture_Ancient_Coffin", PropRule.Placement.CENTER, 0.4, 1,
                new RoomType[]{RoomType.COMBAT, RoomType.DEAD_END}));
        props.add(new PropRule("Furniture_Ancient_Chest_Small", PropRule.Placement.CENTER, 0.8, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic", PropRule.Placement.CENTER, 0.9, 1,
                new RoomType[]{RoomType.BOSS}));
        props.add(new PropRule("Furniture_Ancient_Statue", PropRule.Placement.CENTER, 0.3, 1,
                new RoomType[]{RoomType.HUB, RoomType.ENTRANCE}));
        props.add(new PropRule("Furniture_Human_Ruins_Banner", PropRule.Placement.WALL_ALIGNED, 0.15, 2, null));
        props.add(new PropRule("Furniture_Ancient_Table", PropRule.Placement.FLOOR, 0.2, 1,
                new RoomType[]{RoomType.SAFE}));
        props.add(new PropRule("Deco_Iron_Chains_Vertical", PropRule.Placement.CEILING, 0.1, 2,
                new RoomType[]{RoomType.COMBAT, RoomType.BOSS}));
        return props;
    }

    private List<PropRule> getVolcanicProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Bench_Furnace", PropRule.Placement.FLOOR, 0.3, 1,
                new RoomType[]{RoomType.SAFE}));
        props.add(new PropRule("Bench_Armory", PropRule.Placement.FLOOR, 0.2, 1,
                new RoomType[]{RoomType.SAFE}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic", PropRule.Placement.CENTER, 0.9, 1,
                new RoomType[]{RoomType.BOSS}));
        props.add(new PropRule("Furniture_Crude_Chest_Large", PropRule.Placement.CENTER, 0.7, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Ancient_Crate", PropRule.Placement.FLOOR, 0.15, 2, null));
        props.add(new PropRule("Deco_Iron_Stack", PropRule.Placement.CORNER, 0.2, 2, null));
        return props;
    }

    private List<PropRule> getArcaneProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Furniture_Ancient_Bookshelf", PropRule.Placement.WALL_ALIGNED, 0.2, 3,
                new RoomType[]{RoomType.SAFE, RoomType.LOOT, RoomType.HUB}));
        props.add(new PropRule("Furniture_Royal_Magic_Table", PropRule.Placement.CENTER, 0.4, 1,
                new RoomType[]{RoomType.SAFE}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic", PropRule.Placement.CENTER, 0.9, 1,
                new RoomType[]{RoomType.BOSS}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic", PropRule.Placement.CENTER, 0.7, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Royal_Magic_Pot", PropRule.Placement.FLOOR, 0.15, 2, null));
        props.add(new PropRule("Rock_Crystal_Blue_Large", PropRule.Placement.FLOOR, 0.1, 2,
                new RoomType[]{RoomType.COMBAT, RoomType.BOSS}));
        props.add(new PropRule("Rock_Crystal_Purple_Large", PropRule.Placement.FLOOR, 0.1, 2,
                new RoomType[]{RoomType.COMBAT, RoomType.BOSS}));
        return props;
    }

    private List<PropRule> getMineProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Furniture_Ancient_Crate", PropRule.Placement.FLOOR, 0.15, 3, null));
        props.add(new PropRule("Furniture_Crude_Chest_Small", PropRule.Placement.FLOOR, 0.6, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Crude_Chest_Large", PropRule.Placement.CENTER, 0.8, 1,
                new RoomType[]{RoomType.BOSS}));
        props.add(new PropRule("Wood_Darkwood_Beam", PropRule.Placement.WALL_ALIGNED, 0.2, 4, null));
        props.add(new PropRule("Deco_SpiderWeb", PropRule.Placement.CORNER, 0.3, 2, null));
        return props;
    }

    private List<PropRule> getMushroomProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Plant_Crop_Mushroom_Block_Blue", PropRule.Placement.FLOOR, 0.2, 3, null));
        props.add(new PropRule("Plant_Crop_Mushroom_Block_Purple", PropRule.Placement.FLOOR, 0.15, 2, null));
        props.add(new PropRule("Plant_Crop_Mushroom_Block_Red", PropRule.Placement.FLOOR, 0.1, 2,
                new RoomType[]{RoomType.COMBAT}));
        props.add(new PropRule("Furniture_Jungle_Chest_Small", PropRule.Placement.CENTER, 0.7, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic", PropRule.Placement.CENTER, 0.9, 1,
                new RoomType[]{RoomType.BOSS}));
        return props;
    }

    private List<PropRule> getHiveProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Deco_Scarak_Eggsacks", PropRule.Placement.FLOOR, 0.3, 3,
                new RoomType[]{RoomType.COMBAT, RoomType.DEAD_END}));
        props.add(new PropRule("Deco_Hive", PropRule.Placement.FLOOR, 0.15, 2, null));
        props.add(new PropRule("Furniture_Temple_Scarak_Chest_Small", PropRule.Placement.CENTER, 0.7, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic", PropRule.Placement.CENTER, 0.9, 1,
                new RoomType[]{RoomType.BOSS}));
        props.add(new PropRule("Furniture_Temple_Scarak_Pot", PropRule.Placement.WALL_ALIGNED, 0.1, 2, null));
        return props;
    }

    private List<PropRule> getTempleDarkProps() {
        List<PropRule> props = new ArrayList<>();
        props.add(new PropRule("Furniture_Temple_Dark_Statue", PropRule.Placement.CENTER, 0.4, 1,
                new RoomType[]{RoomType.HUB, RoomType.ENTRANCE, RoomType.BOSS}));
        props.add(new PropRule("Furniture_Temple_Dark_Coffin", PropRule.Placement.CENTER, 0.3, 1,
                new RoomType[]{RoomType.COMBAT, RoomType.DEAD_END}));
        props.add(new PropRule("Furniture_Temple_Dark_Chest_Large", PropRule.Placement.CENTER, 0.8, 1,
                new RoomType[]{RoomType.LOOT}));
        props.add(new PropRule("Furniture_Dungeon_Chest_Epic_Large", PropRule.Placement.CENTER, 0.9, 1,
                new RoomType[]{RoomType.BOSS}));
        props.add(new PropRule("Furniture_Temple_Dark_Pot", PropRule.Placement.FLOOR, 0.15, 2, null));
        props.add(new PropRule("Deco_SpiderWeb", PropRule.Placement.CORNER, 0.5, 4, null));
        props.add(new PropRule("Deco_Bone_Skulls", PropRule.Placement.CORNER, 0.2, 2,
                new RoomType[]{RoomType.COMBAT, RoomType.BOSS}));
        props.add(new PropRule("Furniture_Human_Ruins_Banner", PropRule.Placement.WALL_ALIGNED, 0.1, 2, null));
        return props;
    }
}
