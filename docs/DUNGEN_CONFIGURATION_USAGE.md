# Dungen Configuration Usage Guide

This is a quick reference for the `/dungen` UI. It describes what each field currently does in the generation pipeline, based on the code path from `DungeonGeneratePage` through `DungeonGenerateConfigMapper`, `GenerationOrchestrator`, `LayoutGenerator`, `VoxelCarver`, `FeaturePlacer`, `ThemeDecorator`, `PropPlacer`, `LightPlacer`, and `SpawnerPlacer`.

Values are saved to `generate-config.json` in the plugin data directory after a successful Generate click. The live preview uses the same generation config, but never assembles blocks into the world.

## Generation Flow

1. The UI fields are mapped into `DungeonConfig`.
2. `complexity` is applied before layout generation. It scales `maxRooms`, `branchChance`, `loopChance`, `trapDensity`, `secretWallChance`, and `enemyDensity` with `0.3 + complexity * 1.4`.
3. Rooms are randomly scattered with overlap rejection, then connected with a greedy minimum spanning tree.
4. Optional branches and loops are added, connectivity is repaired, then entrance, exit, boss, and secondary room types are assigned.
5. The graph is carved into voxels, features are placed, theme decoration is applied, props and lights are placed, spawner and merchant definitions are produced, and the exit area is repaired so it stays reachable.
6. Generate clears the target volume in the current world, assembles blocks, closes the UI, and teleports the player to the generated entrance.

## General

| Field | Current effect |
| --- | --- |
| Seed | Any non-blank string is accepted. Blank values become `0`. The generator uses the Java `String.hashCode()` of the seed as the random seed, so the same seed string and config produce the same layout and decoration. |
| Roll | Replaces Seed with a random integer string from `0` to `999999999` and refreshes the preview. |
| Center X | World-space center X for assembly. Generate converts it to a corner with `centerX - width / 2`, clears the full volume, then places the dungeon there. Preview ignores world position. |
| Y | World-space center Y for assembly. Generate converts it to `centerY - height / 2`. |
| Z | World-space center Z for assembly. Generate converts it to `centerZ - depth / 2`. |
| Floor Lv | Used for spawn pool filtering, elite ratio calculation, spawner definitions, and merchant definitions. Higher floors can unlock different NPC roles and increase the elite budget according to the global sigmoid settings. |

## Size

| Field | Current effect |
| --- | --- |
| Width | X size of the generated voxel grid and cleared assembly volume. It also bounds random room placement and entrance/exit distance calculations. |
| Depth | Z size of the generated voxel grid and cleared assembly volume. It also bounds random room placement. |
| Height | Y size of every generated room and of the cleared assembly volume. Rooms and corridors are carved inside this height, with floor at local `Y=0` and ceiling at `height - 1`. |

## Rooms

| Field | Current effect |
| --- | --- |
| Max Rooms | Upper target before density and placement rejection. It is first scaled by complexity, then multiplied by Room Density. Actual room count can be lower if rooms cannot fit without overlap. |
| Density | Multiplies the complexity-scaled Max Rooms value to compute the effective room target: `max(1, int(maxRooms * roomDensity))`. This is not a physical fill percentage. |
| Complexity | Global scaler. Current formula is `0.3 + complexity * 1.4`, clamped from the UI value range. It scales room count, branch chance, loop chance, trap density, secret wall chance, and enemy density before generation. |
| Min Size | Minimum random room width and depth. Room sizes are chosen inclusively between Min Size and Max Size. |
| Max Size | Maximum random room width and depth. Larger values create larger rooms but can reduce the number that fit. |
| Shape | Selects the cell template used for every room: rectangular, circular, L-shaped, cross, or irregular. Unknown values fall back to rectangular. |
| Irregularity | For rectangular, circular, L-shaped, and cross rooms, randomly removes edge and corner cells. For irregular rooms, controls how many cells are kept; high values can make rooms sparse. Rooms with fewer than 4 cells are rejected. |

## Corridors

| Field | Current effect |
| --- | --- |
| Width | Stored as corridor width, then carved with `halfWidth = width / 2`. The actual carved footprint is odd-width because the carver uses `-halfWidth..+halfWidth`; for example, UI width `1` carves 1 block, `2` or `3` carves 3 blocks, and `4` or `5` carves 5 blocks. |
| Branch % | Scaled by Complexity. Branches are attempted only from corridor paths with at least 10 waypoints, so this mainly affects winding corridors in the current implementation. Branches start near the source corridor midpoint and extend 4-12 cells in a random cardinal direction. |
| Loop % | Scaled by Complexity, then internally multiplied by `0.3`. It can add extra corridors between non-adjacent room pairs that are close enough and not already connected. |
| Winding Corridors | When enabled and Factor is greater than `0.01`, room-to-room corridors use a random walk toward the target instead of a three-point L-shaped path. |
| Factor | Controls winding deviation. Each winding step has a `0.3 * factor` chance to move perpendicular to the target direction. |

## Features

| Field | Current effect |
| --- | --- |
| Pillars | Not implemented yet, left as placeholder for future releases. Current pillar placement ignores this UI value and is controlled by global settings: large rooms can receive four corner pillars with a fixed skip chance. |
| Water | Per non-entrance-room chance to replace a centered floor patch with the theme fluid. Pool size is roughly one third of room width and depth, minimum 2x2. Values `<= 0.01` disable water pools. |
| Lava | Per non-entrance-room chance to replace a centered floor patch with the theme secondary fluid. Pool size is roughly one quarter of room width and depth, minimum 2x2. Values `<= 0.01` disable lava pools. |
| Traps | Scaled by Complexity and capped by global settings. Eligible walkable floor cells roll `trapDensity * TrapDensityMultiplier` to place a trap. Current global `TrapDensityMultiplier` is `0.05`, so this is intentionally sparse. |
| Secrets | Not implemented yet, left as placeholder for future releases. The value is accepted and scaled, but secret wall placement is currently disabled. |
| Floor Traps | Enables breakable floor trap blocks as one possible trap outcome. It only matters when Traps is high enough to place traps. Current global `FloorTrapChance` is `0.35`. |
| Merchant % | Per eligible room chance to add one merchant definition. A room is eligible only if it is not the entrance and contains fluid; the merchant position must be solid ground adjacent to that fluid. Current generation stops after the first merchant definition. The standalone `/dungen` assembly does not create merchant NPC entities. |

## Navigation

| Field | Current effect |
| --- | --- |
| Entrance | Selects the entrance room: `edge` chooses the room closest to any grid edge, `corner` chooses the room closest to any corner, `center` chooses the room closest to grid center, and `random` picks any room. |
| Exit Dist | Not implemented yet, left as placeholder for future releases. The exit currently ignores this value and is simply the room farthest from the entrance. |

## Enemies

| Field | Current effect |
| --- | --- |
| Density | Not implemented yet, left as placeholder for future releases. The value is accepted and scaled by Complexity, but current spawner counts are based on room area and global settings instead. |
| Max/Room | Not implemented yet, left as placeholder for future releases. Current spawner counts are not capped by this UI value. |
| Ambush % | Not implemented yet, left as placeholder for future releases. The value is saved and mapped but no ambush event generation currently reads it. |
| Boss Room | When enabled, the largest non-entrance, non-exit room becomes a BOSS room and gets boss spawner definitions. When disabled, no room is marked BOSS and the exit is used only for critical-path metadata. |

Spawner definitions are generated for COMBAT and BOSS rooms. In the standalone `/dungen` world assembly path, these remain definitions unless the global `SpawnerBlock` setting is configured, in which case debug marker blocks are placed at spawner positions. It does not create live ECS spawner entities.

## Architecture

| Field | Current effect |
| --- | --- |
| Erosion | Not implemented yet, left as placeholder for future releases. `VoxelCarver.applyErosion(...)` exists, but the orchestrator currently has that call disabled. |
| Remove Ceiling | Removes the topmost local Y layer after carving. Decoration also skips ceiling props, ceiling lights, and hanging overgrowth/rubble near the stripped top when this is enabled. |
| Flat Floor | Not implemented yet, left as placeholder for future releases. All generated rooms currently use local `Y=0` regardless of this value. |
| Solid Fill | When enabled, uncarved exterior space remains solid. When disabled, fully buried solid blocks are hollowed out, leaving mostly the visible shell around carved rooms and corridors. |

## Theme

| Field | Current effect |
| --- | --- |
| Palette | Selects the theme asset: `crypt`, `hive`, `mine`, `arcane`, `temple_dark`, `volcanic`, or `mushroom`. The palette controls fill block, walls, floors, ceilings, fluids, traps, props, lights, and spawn pools. Unknown palette values fall back to `crypt`. |
| Decay | Replaces exposed structural blocks with palette decay variants at this probability. Also drives rubble placement at `decay * RubbleMultiplier` on the lowest floor surface per column. |
| Overgrowth | Places palette overgrowth blocks in air cells adjacent to floor, wall, or ceiling supports. Actual roll rate is `overgrowth * OvergrowthMultiplier`; current global multiplier is `0.3`. |
| Flooding | Places palette fluid in eligible low air cells above solid blocks. Only the bottom fraction of the grid is scanned; current global `FloodScanFraction` is `0.333`. |

## Pacing

| Field | Current effect |
| --- | --- |
| Breathe Rooms | Not implemented yet, left as placeholder for future releases. Safe rooms are currently assigned by a fixed layout rule on the critical path, independent of this UI value. |
| Diff Ramp | Not implemented yet, left as placeholder for future releases. Difficulty scaling currently comes from Floor Lv and theme spawn-pool eligibility, not this UI value. |

## Generate And Preview Controls

| Control | Current effect |
| --- | --- |
| Preview panel | Regenerates preview artifacts after field changes with a short debounce. It uses origin `(0,0,0)` and does not assemble into the world. |
| Generate Dungeon | Saves the UI config, clears the target `width x height x depth` volume centered on Center X/Y/Z in the current world, generates the dungeon, places the block blueprint, closes the page, and teleports the player to the entrance if available. |