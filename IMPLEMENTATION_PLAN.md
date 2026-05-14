# Dungeon Generator Plugin — Implementation Plan

> **Plugin Name**: DungeonGen  
> **Package**: `com.duntale.dungeongen`  
> **Output JAR**: `DungeonGen.jar`  
> **Reference**: mcp-player plugin architecture  

> Historical note: older drafts of this plan described an embedded HTTP layer. That surface has been removed from the current project.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│              GenerationOrchestrator                      │
│  (runs on dedicated thread pool)                        │
│                                                         │
│  Pipeline:                                              │
│  1. LayoutGenerator   → room/corridor graph             │
│  2. VoxelCarver       → convert graph to 3D block data  │
│  3. ThemeDecorator    → apply material palette + decay  │
│  4. PropPlacer        → place furniture/decorations     │
│  5. LightPlacer       → place light sources             │
│  6. EntityPlacer      → define spawn points             │
│                                                         │
│  Output: DungeonBlueprint (block array + metadata)      │
└────────────────────┬────────────────────────────────────┘
                     │ CompletableFuture<DungeonBlueprint>
                     ▼
┌─────────────────────────────────────────────────────────┐
│              WorldAssembler                              │
│  (executes on World thread via World.execute())         │
│                                                         │
│  Reads DungeonBlueprint → calls setBlock() in batches   │
│  Respects tick budget (max N blocks per tick)            │
└─────────────────────────────────────────────────────────┘
```

---

## File Structure

```
dungeon-gen/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
│   ├── gradle-wrapper.jar          (copy from mcp-player)
│   └── gradle-wrapper.properties   (copy from mcp-player)
├── gradlew
├── gradlew.bat
└── src/main/
    ├── java/com/duntale/dungeongen/
    │   ├── DungeonGenPlugin.java           # Entry point (JavaPlugin)
    │   ├── config/
    │   │   ├── DungeonConfig.java          # Full generation configuration
    │   │   ├── LayoutConfig.java           # Layout parameters
    │   │   ├── ThemeConfig.java            # Theme / material palette
    │   │   ├── PacingConfig.java           # Tension curve / pacing
    │   │   └── Preset.java                 # Built-in preset profiles
    │   ├── generator/
    │   │   ├── GenerationOrchestrator.java # Pipeline coordinator
    │   │   ├── layout/
    │   │   │   ├── LayoutGenerator.java    # BSP/graph room layout
    │   │   │   ├── Room.java               # Room data (bounds, type, connections)
    │   │   │   ├── RoomType.java           # Enum: COMBAT, SAFE, LOOT, BOSS, HUB, etc.
    │   │   │   ├── Corridor.java           # Corridor connecting rooms
    │   │   │   └── DungeonGraph.java       # Graph of rooms + corridors
    │   │   ├── voxel/
    │   │   │   ├── VoxelCarver.java        # Carves rooms/corridors into block grid
    │   │   │   └── BlockGrid.java          # 3D array of block IDs
    │   │   ├── theme/
    │   │   │   ├── ThemeDecorator.java     # Applies theme palette + decay
    │   │   │   ├── BlockPalette.java       # Wall/floor/ceiling/accent block sets
    │   │   │   └── DecayPass.java          # Ruin/moss/overgrowth modifier
    │   │   ├── props/
    │   │   │   ├── PropPlacer.java         # Constraint-based prop placement
    │   │   │   └── PropRule.java           # Placement rule (wall-aligned, center, corner)
    │   │   ├── lighting/
    │   │   │   └── LightPlacer.java        # Light source placement
    │   │   └── entity/
    │   │       └── SpawnPointPlacer.java   # Define spawn point locations + tiers
    │   ├── model/
    │   │   ├── DungeonBlueprint.java       # Final output: blocks + metadata
    │   │   ├── BlockEntry.java             # (x, y, z, blockId, rotation, filler)
    │   │   └── SpawnPoint.java             # (x, y, z, spawnerTable, tier)
    │   ├── assembly/
    │   │   └── WorldAssembler.java         # Places blueprint into world on WorldThread
    │   └── util/
    │       ├── BlockResolver.java          # String ID → int block ID resolution
    │       ├── JsonParser.java             # Minimal JSON parser (from mcp-player)
    │       └── MathUtil.java               # RNG, noise, geometry helpers
    └── resources/
        └── manifest.json
```

---

## Phase 4 Implementation Steps

### Step 1: Project Scaffolding
- Create `dungeon-gen/` directory structure
- Copy gradle wrapper from mcp-player
- Create `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Create `manifest.json`
- Verify `./gradlew build` compiles

### Step 2: Plugin Entry Point
- `DungeonGenPlugin.java` - extends `JavaPlugin`, lifecycle methods
- Wire up lifecycle for asset registration and generation services

### Step 3: Configuration Model
- `DungeonConfig.java` - top-level config record
- `LayoutConfig.java` - grid size, room counts, branching, loops, dead-ends
- `ThemeConfig.java` - palette name, decay%, overgrowth%, flooding%
- `PacingConfig.java` - tension curve parameters
- `Preset.java` - built-in presets (ForgottenCrypt, FloodedMine, ArcaneVault)
- JSON parsing for config payloads

### Step 4: Block Resolution Utility
- `BlockResolver.java` - wraps `BlockType.fromString()` and `BlockTypeAssetMap.getIndex()`
- Cache resolved block IDs at startup for all palette blocks
- Validate blocks exist before using them

### Step 5: Layout Generator (Core Algorithm)
- BSP (Binary Space Partition) for initial room placement
- Room type assignment (combat, safe, loot, boss, hub)
- Corridor generation connecting rooms
- Graph validation (100% reachability)
- Critical path computation (entrance → boss → exit)

### Step 6: Voxel Carver
- `BlockGrid.java` - 3D array representation
- Carve rooms as hollow rectangles (walls, floor, ceiling)
- Carve corridors with appropriate width/height
- Handle elevation shifts (1-3 block steps)
- Door placement at room-corridor junctions

### Step 7: Theme Decorator
- `BlockPalette.java` - define palettes from asset report data
- Apply wall/floor/ceiling blocks from palette
- `DecayPass.java` - randomly replace blocks with mossy/cracked variants
- Overgrowth pass - add vines, moss, roots
- Flooding pass - replace floor with fluid at low elevations

### Step 8: Prop Placer
- Wall-aligned props (torches, banners, shelves)
- Corner props (cobwebs, small crates)
- Center props (altars, fountain, chests)
- Room-type aware (combat rooms get spawners, safe rooms get merchants)

### Step 9: Light Placer
- Place light sources based on theme
- Respect density settings
- Ensure no pitch-black corridors

### Step 10: Spawn Point Placer
- Define spawn point metadata (location, spawner table, tier)
- Scale with pacing/tension curve

### Step 11: World Assembler
- Read `DungeonBlueprint`, convert to `setBlock()` calls
- Execute on world thread via `World.execute()`
- Batch blocks per tick to avoid lag spikes
- Report progress

### Step 12: Generation Orchestrator
- Thread pool for generation work
- Pipeline: Layout → Carve → Theme → Props → Light → Spawns
- Seed determinism (all RNG seeded)
- Return `CompletableFuture<DungeonBlueprint>`

---

## Block Resolution Strategy

```java
// At plugin startup (in setup()):
int stoneWall = BlockResolver.resolve("Rock_Stone_Brick");     // → int ID
int mossyWall = BlockResolver.resolve("Rock_Stone_Brick_Mossy"); // → int ID
// ...

// During generation (off world thread):
blockGrid.set(x, y, z, stoneWall, 0, 0);

// During assembly (on world thread):
WorldChunk chunk = ...;
chunk.setBlock(localX, localY, localZ, blockId, rotation, filler);
```

---

## Key Technical Decisions

1. **Generation runs off-thread**: The entire pipeline (layout → voxel → decorate) runs on a dedicated `ExecutorService`. Only the final assembly step touches the World thread.

2. **BlockGrid as intermediate format**: We don't touch Hytale's chunk system during generation. We build a simple `int[][][]` grid, then translate to world coordinates during assembly.

3. **Seed determinism**: All `Random` instances are seeded from the dungeon seed. Same seed → identical dungeon.

4. **No external dependencies**: JSON parsing is hand-rolled. No Jackson/Gson.

5. **Block IDs resolved once at startup**: `BlockResolver` resolves all palette string IDs to integer IDs during `setup()`, avoiding repeated lookups during generation.

6. **Batched assembly**: WorldAssembler places blocks in configurable batches (e.g., 1000 blocks/tick) to avoid freezing the server.
