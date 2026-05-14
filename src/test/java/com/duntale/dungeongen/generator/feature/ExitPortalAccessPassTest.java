package com.duntale.dungeongen.generator.feature;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.layout.Corridor;
import com.duntale.dungeongen.generator.layout.CorridorType;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

@DisplayName("ExitPortalAccessPass")
class ExitPortalAccessPassTest {

    private static final String FLOOR = "Rock_Stone_Brick";
    private static final String FALLBACK_FLOOR = "Rock_Basalt_Brick";
    private static final String WATER = "Fluid_Water";
    private static final String LAVA = "Fluid_Lava";

    private final ExitPortalAccessPass pass = new ExitPortalAccessPass();

    @Test
    @DisplayName("Should resolve incoming access position from critical room path")
    void shouldResolveIncomingAccessPositionFromCriticalRoomPath() {
        DungeonGraph graph = new DungeonGraph();
        Room entrance = new Room(1, RoomType.ENTRANCE, 1, 0, 4, 5, 4, 5);
        Room middle = new Room(2, RoomType.COMBAT, 10, 0, 4, 5, 4, 5);
        Room exit = new Room(3, RoomType.BOSS, 19, 0, 4, 5, 4, 5);
        graph.addRoom(entrance);
        graph.addRoom(middle);
        graph.addRoom(exit);
        graph.setEntranceRoomId(entrance.getId());
        graph.setBossRoomId(exit.getId());
        graph.addCorridor(new Corridor(
            entrance.getId(),
            middle.getId(),
            List.of(new Vec3i(3, 0, 6), new Vec3i(12, 0, 6)),
            3,
            CorridorType.L_SHAPED
        ));
        graph.addCorridor(new Corridor(
            middle.getId(),
            exit.getId(),
            List.of(new Vec3i(12, 0, 6), new Vec3i(21, 0, 6), new Vec3i(21, 0, 6)),
            3,
            CorridorType.L_SHAPED
        ));

        Vec3i position = pass.resolveIncomingAccessPosition(graph, exit.getId());

        assertEquals(new Vec3i(21, 0, 6), position);
    }

    @Test
    @DisplayName("Should keep exit anchor at room center when center floor is fluid")
    void shouldKeepExitAnchorAtRoomCenterWhenCenterFloorIsFluid() {
        BlockGrid grid = new BlockGrid(10, 5, 10);
        Room room = rectangularRoom();
        fillRoomFloor(grid, room, FLOOR);
        grid.set(4, 0, 4, WATER);

        Vec3i position = pass.resolveAnchor(grid, room);

        assertEquals(new Vec3i(4, 1, 4), position);
    }

    @Test
    @DisplayName("Should use nearest patchable irregular room cell when center is outside cells")
    void shouldUseNearestPatchableIrregularRoomCellWhenCenterIsOutsideCells() {
        BlockGrid grid = new BlockGrid(10, 5, 10);
        Room room = rectangularRoom();
        room.addCell(3, 4);
        grid.set(3, 0, 4, WATER);

        Vec3i position = pass.resolveAnchor(grid, room);

        assertEquals(new Vec3i(3, 1, 4), position);
    }

    @Test
    @DisplayName("Should prefer entrance-facing dry path over shorter dry path")
    void shouldPreferEntranceFacingDryPathOverShorterDryPath() {
        BlockGrid grid = new BlockGrid(12, 5, 12);
        Room room = new Room(1, RoomType.BOSS, 1, 0, 1, 9, 4, 9);
        fillRoomFloor(grid, room, WATER);
        grid.set(5, 0, 4, FLOOR);
        grid.set(2, 0, 4, FLOOR);

        ExitPortalAccessPass.ExitPortalAccess access = pass.protect(
            grid,
            room,
            new Vec3i(4, 1, 4),
            FALLBACK_FLOOR,
            new Vec3i(0, 1, 4)
        );

        assertEquals(FLOOR, grid.get(4, 0, 4));
        assertEquals(FLOOR, grid.get(3, 0, 4));
        assertEquals(FLOOR, grid.get(2, 0, 4));
        assertTrue(access.protectedStandingCells().contains(new Vec3i(3, 1, 4)));
        assertTrue(access.protectedStandingCells().contains(new Vec3i(2, 1, 4)));
        assertFalse(access.protectedStandingCells().contains(new Vec3i(5, 1, 4)));
    }

    @Test
    @DisplayName("Should replace fluid below exit anchor with structural floor")
    void shouldReplaceFluidBelowExitAnchorWithStructuralFloor() {
        BlockGrid grid = new BlockGrid(10, 5, 10);
        Room room = rectangularRoom();
        fillRoomFloor(grid, room, FLOOR);
        grid.set(4, 0, 4, WATER);

        ExitPortalAccessPass.ExitPortalAccess access = pass.protect(
            grid,
            room,
            new Vec3i(4, 1, 4),
            FALLBACK_FLOOR
        );

        assertEquals(FLOOR, grid.get(4, 0, 4));
        assertEquals(2, access.protectedStandingCells().size());
        assertTrue(access.protectedStandingCells().contains(new Vec3i(4, 1, 4)));
    }

    @Test
    @DisplayName("Should clear fluid from standing and head cells")
    void shouldClearFluidFromStandingAndHeadCells() {
        BlockGrid grid = new BlockGrid(10, 5, 10);
        Room room = rectangularRoom();
        fillRoomFloor(grid, room, FLOOR);
        grid.set(4, 1, 4, WATER);
        grid.set(4, 2, 4, LAVA);

        pass.protect(grid, room, new Vec3i(4, 1, 4), FALLBACK_FLOOR);

        assertNull(grid.get(4, 1, 4));
        assertNull(grid.get(4, 2, 4));
    }

    @Test
    @DisplayName("Should patch only shortest path through large pool")
    void shouldPatchOnlyShortestPathThroughLargePool() {
        BlockGrid grid = new BlockGrid(12, 5, 12);
        Room room = new Room(1, RoomType.LOOT, 1, 0, 1, 9, 4, 9);
        fillRoomFloor(grid, room, WATER);
        grid.set(7, 0, 4, FLOOR);

        ExitPortalAccessPass.ExitPortalAccess access = pass.protect(
            grid,
            room,
            new Vec3i(4, 1, 4),
            FALLBACK_FLOOR
        );

        assertEquals(FLOOR, grid.get(4, 0, 4));
        assertEquals(FLOOR, grid.get(5, 0, 4));
        assertEquals(FLOOR, grid.get(6, 0, 4));
        assertEquals(FLOOR, grid.get(7, 0, 4));
        assertEquals(WATER, grid.get(4, 0, 5));
        assertEquals(4, access.protectedStandingCells().size());
    }

    @Test
    @DisplayName("Should not extend path when room has no dry floor target")
    void shouldNotExtendPathWhenNoDryFloorTargetExists() {
        BlockGrid grid = new BlockGrid(10, 5, 10);
        Room room = rectangularRoom();
        fillRoomFloor(grid, room, WATER);

        ExitPortalAccessPass.ExitPortalAccess access = pass.protect(
            grid,
            room,
            new Vec3i(4, 1, 4),
            FALLBACK_FLOOR
        );

        assertEquals(FALLBACK_FLOOR, grid.get(4, 0, 4));
        assertEquals(WATER, grid.get(5, 0, 4));
        assertEquals(WATER, grid.get(4, 0, 5));
        assertEquals(1, access.protectedStandingCells().size());
    }

    @Test
    @DisplayName("Should not patch outside irregular room cells")
    void shouldNotPatchOutsideIrregularRoomCells() {
        BlockGrid grid = new BlockGrid(10, 5, 10);
        Room room = rectangularRoom();
        room.addCell(4, 4);
        grid.set(4, 0, 4, WATER);
        grid.set(5, 0, 4, FLOOR);

        ExitPortalAccessPass.ExitPortalAccess access = pass.protect(
            grid,
            room,
            new Vec3i(4, 1, 4),
            FALLBACK_FLOOR
        );

        assertEquals(FALLBACK_FLOOR, grid.get(4, 0, 4));
        assertEquals(FLOOR, grid.get(5, 0, 4));
        assertEquals(1, access.protectedStandingCells().size());
    }

    private static Room rectangularRoom() {
        return new Room(1, RoomType.LOOT, 1, 0, 1, 7, 4, 7);
    }

    private static void fillRoomFloor(BlockGrid grid, Room room, String blockId) {
        for (int x = room.getX() + 1; x < room.getX() + room.getWidth() - 1; x++) {
            for (int z = room.getZ() + 1; z < room.getZ() + room.getDepth() - 1; z++) {
                grid.set(x, room.getY(), z, blockId);
            }
        }
    }
}