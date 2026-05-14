# Enemy Spawner System — Design Plan

> **Status**: Implementation (Phases A–D complete)  
> **Scope**: dungeon-gen (generation-time) + v3-zsquad (runtime)  
> **Date**: 2026-03-01  
> **Last updated**: 2026-03-02

---

## 1. Context & Current State

### What exists today

The dungeon-gen generation pipeline (7 stages) produces a `DungeonBlueprint` containing block entries and `SpawnerDefinition` records. The `SpawnerPlacer` (Phase 6) creates spawner definitions in COMBAT and BOSS rooms with tier-based spawn pools, pre-computed spawn offsets, and proximity triggers. The `WorldAssembler` places blocks and spawner debug markers.

**Remaining**: v3-zsquad needs a runtime ECS system to consume `SpawnerDefinition` records and actually spawn NPC entities.

### Architecture decision: compileOnly dependency

dungeon-gen and v3-zsquad co-exist on the same Hytale server. Rather than going through the REST API for generation, **v3-zsquad declares dungeon-gen as a `compileOnly` dependency** and calls its classes directly at runtime:

```kotlin
// v3-zsquad/build.gradle.kts
dependencies {
    compileOnly(project(":dungeon-gen"))
}
```

v3-zsquad calls `GenerationOrchestrator.generate(config)` directly from game code, receives a `DungeonBlueprint`, and drives both world assembly and spawner creation itself. The REST API remains available for external tooling/debugging but is not the primary generation path.

---

## 2. Game Design Context (Fate-like Dungeon Crawler)

- Players enter **Floor 1**, explore a procedurally generated dungeon, find the exit, descend to **Floor 2**, and so on.
- Each floor is generated fresh on entry — no floor state is persisted.
- Players can use a **Town Portal** item to teleport to a static town with NPCs, then return to the current floor's entrance.
- Going up floors is not possible, but restarting from Floor 1 is (generates a new layout).
- Floor difficulty, theme, enemy composition, and layout parameters scale with floor level.

---

## 3. Spawner Types

For the initial implementation, **all spawners use a single configuration**:

- **Type**: `FIXED` — spawns a finite total, then permanently deactivates.
- **Trigger**: `PROXIMITY` — activates when a player enters `activationRadius`.
- **Strategy**: `ALL_AT_ONCE` — spawns all enemies immediately on trigger.

This keeps the system simple and testable. The data model is designed to support future expansion (recurrent spawners, timed triggers, wave strategies, etc.) without structural changes.

### Future spawner types (not implemented now)

| Type | Trigger | Strategy | Use case |
|---|---|---|---|
| Fixed + ON_ROOM_ENTER + LINEAR | Spawns batches over time when room entered | Boss add phases |
| Fixed + ON_ROOM_CLEAR + ALL_AT_ONCE | Spawns after previous wave dies | Chained encounters |
| Recurrent + PROXIMITY + LINEAR | Steady drip while players are nearby | Ambient patrols |
| Recurrent + PROXIMITY + WAVE | Full waves, waits for clear | Arena defense |

The sealed interface hierarchy supports all of these — only the runtime tick system needs new switch cases.

---

## 4. Data Model (Generation-Time)

All records live in `dungeon-gen` under `com.duntale.dungeongen.model`.

### 4.1 SpawnerDefinition

The core spawner record, replacing `SpawnPoint`:

```java
public record SpawnerDefinition(
    int id,                          // unique spawner ID within the blueprint
    int x, int y, int z,            // world-relative spawn position
    int roomId,                      // owning room ID in the DungeonGraph
    SpawnerType type,                // FIXED (future: RECURRENT)
    TriggerConfig trigger,           // how the spawner activates
    List<SpawnEntry> spawnPool,      // weighted pool of NPC roles to pick from
    int totalCount,                  // total NPCs to spawn (for FIXED)
    List<Vec3i> spawnOffsets,        // pre-validated positions relative to spawner center
    boolean isBoss                   // true for boss spawners (future: intro, arena lock, HP bar)
) {}
```

- **`spawnOffsets`**: Pre-computed at generation time using the `BlockGrid` (air above solid floor). The runtime picks from this list instead of blind random offsets, preventing NPCs from spawning inside walls or props. Generate `totalCount + buffer` offsets for randomization.
- **`isBoss`**: Marker flag so the runtime can branch on boss-specific mechanics in the future (intro sequence, arena barriers, HP bar UI) without re-parsing the pool.

### 4.2 SpawnerType

```java
public enum SpawnerType {
    FIXED,      // spawns a finite total, then stops
    RECURRENT   // keeps spawning until disabled (future)
}
```

### 4.3 TriggerConfig

```java
public record TriggerConfig(
    TriggerType type,
    double activationRadius,     // for PROXIMITY (blocks)
    double deactivationRadius,   // for PROXIMITY — 0 means no deactivation
    double delaySec              // for TIMED (future)
) {
    /**
     * Proximity trigger with radius derived from room dimensions.
     * Uses half of the room's largest horizontal dimension (width or depth).
     */
    public static TriggerConfig proximityFromRoom(Room room) {
        double radius = Math.max(room.getWidth(), room.getDepth()) / 2.0;
        return new TriggerConfig(TriggerType.PROXIMITY, radius, 0, 0);
    }

    /** Proximity trigger with explicit radius. */
    public static TriggerConfig proximity(double radius) {
        return new TriggerConfig(TriggerType.PROXIMITY, radius, 0, 0);
    }
}
```

### 4.4 TriggerType

```java
public enum TriggerType {
    ON_CREATE,      // immediately on spawner creation
    PROXIMITY,      // player enters activation radius
    TIMED,          // after delay seconds (future)
    ON_ROOM_ENTER,  // player enters room AABB (future)
    ON_ROOM_CLEAR   // all room enemies dead (future)
}
```

### 4.5 SpawnEntry

A single entry in the spawner's NPC pool. Has a **level range** so the runtime can roll a random level for the spawned NPC, and a **floor-level gate** so entries can be restricted to certain dungeon depths:

```java
public record SpawnEntry(
    String npcRole,        // Hytale NPC role name, e.g. "Skeleton_Soldier"
    int minLevel,          // minimum NPC level (inclusive) — rolled randomly
    int maxLevel,          // maximum NPC level (inclusive) — rolled randomly
    double weight,         // relative weight for weighted random selection
    @Nullable Integer minFloorLevel,  // minimum dungeon floor for eligibility (null = no min)
    @Nullable Integer maxFloorLevel   // maximum dungeon floor for eligibility (null = no max)
) {
    /** Convenience: entry with no floor restriction. */
    public SpawnEntry(String npcRole, int minLevel, int maxLevel, double weight) {
        this(npcRole, minLevel, maxLevel, weight, null, null);
    }

    /** Check if this entry is eligible for the given floor. */
    public boolean isEligibleForFloor(int floorLevel) {
        if (minFloorLevel != null && floorLevel < minFloorLevel) return false;
        return maxFloorLevel == null || floorLevel <= maxFloorLevel;
    }
}
```

### 4.6 Updated DungeonBlueprint

```java
public class DungeonBlueprint {
    private final List<BlockEntry> blocks;
    private final List<SpawnerDefinition> spawners;  // was: List<SpawnPoint>
    private final DungeonGraph graph;
    private final String seed;
    // ...
}
```

---

## 5. Theme SpawnPools (Asset JSON)

Each theme JSON file gets a new `SpawnPools` section. Entries have floor-level gates so a single theme can scale across many floors:

```json
{
  "SpawnPools": {
    "Tier1": [
      { "NpcRole": "Skeleton_Fighter",  "MinLevel": 5,  "MaxLevel": 10, "Weight": 3.0 },
      { "NpcRole": "Skeleton_Archer",   "MinLevel": 5,  "MaxLevel": 10, "Weight": 2.0 },
      { "NpcRole": "Skeleton_Scout",    "MinLevel": 8,  "MaxLevel": 12, "Weight": 1.5, "MinFloor": 3 }
    ],
    "Tier2": [
      { "NpcRole": "Skeleton_Soldier",  "MinLevel": 12, "MaxLevel": 18, "Weight": 3.0 },
      { "NpcRole": "Skeleton_Knight",   "MinLevel": 15, "MaxLevel": 20, "Weight": 1.5, "MinFloor": 5 },
      { "NpcRole": "Skeleton_Archer",   "MinLevel": 12, "MaxLevel": 18, "Weight": 2.0 }
    ],
    "Tier3": [
      { "NpcRole": "Skeleton_Knight",   "MinLevel": 18, "MaxLevel": 25, "Weight": 3.0 },
      { "NpcRole": "Skeleton_Archmage", "MinLevel": 20, "MaxLevel": 28, "Weight": 1.0, "MinFloor": 8 }
    ],
    "Boss": [
      { "NpcRole": "Shadow_Knight",     "MinLevel": 25, "MaxLevel": 30, "Weight": 1.0 },
      { "NpcRole": "Ghoul",             "MinLevel": 30, "MaxLevel": 40, "Weight": 1.0, "MinFloor": 10 }
    ]
  }
}
```

**Resolution order**: At generation time, `SpawnerPlacer` looks up the theme's `SpawnPools` for the spawner's tier, filters entries by `isEligibleForFloor(floorLevel)`, and embeds the eligible subset into the `SpawnerDefinition.spawnPool`.

### Asset parsing

`DungeonThemeConfig` gets new fields:

```java
protected List<SpawnPoolTierEntry> spawnPoolTier1;
protected List<SpawnPoolTierEntry> spawnPoolTier2;
protected List<SpawnPoolTierEntry> spawnPoolTier3;
protected List<SpawnPoolTierEntry> spawnPoolBoss;
```

With a new codec record:

```java
// config/asset/SpawnPoolTierEntry.java
public record SpawnPoolTierEntry(
    String npcRole, int minLevel, int maxLevel,
    double weight, @Nullable Integer minFloor, @Nullable Integer maxFloor
) {}
```

---

## 6. Generation-Time Placement (SpawnerPlacer)

Replaces `SpawnPointPlacer`. Produces `SpawnerDefinition` list.

### Algorithm

For each room in the `DungeonGraph`:

1. **Skip** non-combat rooms (ENTRANCE, SAFE, LOOT, HUB, CORRIDOR_JUNCTION → no spawners).
2. **Determine tier** from critical-path position (same formula as current `calculateTier()`).
3. **Look up SpawnPools** for the theme + tier, filter by floor level eligibility.
   - **Fallback**: If the filtered pool is empty, fall back to the next-lower tier (Tier3 → Tier2 → Tier1). If still empty, **skip creating the spawner** and log a warning.
4. **Calculate totalCount** from room area and `enemyDensity` (same formula as current `calculateSpawnCount()`).
5. **Find valid spawn position** — interior point with air above solid floor (same validation as current).
6. **Pre-compute `spawnOffsets`** — `totalCount + 2` positions around the spawner center, validated with `grid.isAir(x, y, z) && grid.isBlock(x, y-1, z)`. If configured, a spawner debug block is placed at each offset position.
7. **Create SpawnerDefinition**:
   - `type = FIXED`
   - `trigger = TriggerConfig.proximityFromRoom(room)` — radius = half of room's largest horizontal dimension
   - `spawnPool` = filtered SpawnEntry list from theme
   - `totalCount` = calculated count

For **BOSS rooms**: a **dedicated boss spawner** is created using the `Boss` spawn pool with `totalCount = 1`. Additionally, if the room is large enough, a separate minion spawner using the highest available tier pool may be added.

### Spawn position clustering

Rather than one spawner per NPC, group them: 1–3 spawners per COMBAT room (depending on room size), each responsible for a cluster of enemies. Large rooms get multiple spawners spread across the interior so enemies don't all pile on one spot.

```
Small room (area < 60):  1 spawner at room center
Medium room (60–120):    2 spawners at 1/3 and 2/3 positions
Large room (> 120):      3 spawners spread across interior
```

---

## 7. Runtime ECS System (v3-zsquad)

### 7.1 Spawner Visual Representation

During world assembly, the `WorldAssembler` places a **debug marker block** at the spawner center position and at each pre-computed spawn offset. The block ID is configurable via the `Generation.json` settings asset:

```json
{
  "SpawnerBlock": "Furniture_Temple_Scarak_Window"
}
```

If `SpawnerBlock` is `null` or empty, no block is placed (invisible spawner). This is primarily a debugging aid — in production, spawners will likely be invisible.

`DungeonSettingsConfig` gets a new field:
```java
protected String spawnerBlock = "Furniture_Temple_Scarak_Window";
```

### 7.2 SpawnerComponent

Attached to a lightweight entity at the spawner's world position:

```java
public class SpawnerComponent implements Component {
    private final SpawnerDefinition definition;
    private SpawnerState state;             // DORMANT, ACTIVE, DEPLETED, DISABLED
    private int spawnedCount;               // total spawned so far
    private int spawnBudgetRemaining;       // remaining spawns for current activation
    private final List<Ref<EntityStore>> aliveNpcs;  // ECS refs to living spawned NPCs

    // constructed from SpawnerDefinition
}
```

### 7.3 SpawnerState

```java
public enum SpawnerState {
    DORMANT,   // waiting for trigger
    ACTIVE,    // trigger fired, spawning
    DEPLETED,  // Fixed spawner exhausted its totalCount
    DISABLED   // manually disabled (dungeon teardown)
}
```

### 7.4 SpawnerTickSystem

A `DelayedEntitySystem<EntityStore>` (interval = `0.33f`, i.e. ~10 ticks at 30 TPS) querying entities with `SpawnerComponent + TransformComponent`. Using `DelayedEntitySystem` instead of `EntityTickingSystem` avoids manual tick-throttling logic — the engine handles the interval natively.

#### Proximity check — inverted query pattern

Instead of each DORMANT spawner running a spatial KDTree query (N spawners × 30 TPS = expensive), the system **inverts the query**:

1. Get all player positions once via `World.getPlayerRefs()` → `PlayerRef.getReference()` → `TransformComponent`.
2. For each DORMANT spawner, check `distance(spawnerPos, playerPos) < activationRadius` against the cached positions.

This is O(spawners × players) distance checks with no KDTree overhead. With 1–4 players and 30–60 spawners, that's 30–240 distance computations — trivial.

**Tick throttle**: The `DelayedEntitySystem` fires every **0.33 seconds** (~10 ticks). Activation doesn't need frame-perfect responsiveness.

```java
// Inside tick method
World world = commandBuffer.getExternalData().getWorld();
Collection<PlayerRef> playerRefs = world.getPlayerRefs();
List<Vector3d> playerPositions = new ArrayList<>(playerRefs.size());
for (PlayerRef pr : playerRefs) {
    Ref<EntityStore> ref = pr.getReference();
    if (ref != null && ref.isValid()) {
        TransformComponent tc = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null) playerPositions.add(tc.getPosition());
    }
}
```

#### Per-tick spawn budget

A global constant `MAX_SPAWNS_PER_TICK = 10` limits how many NPCs can be spawned across all spawners in a single tick. This prevents frame spikes when multiple spawners trigger simultaneously.

#### Per delayed tick (~3× per second):

1. **DORMANT spawners** (every 10 ticks) — check trigger:
   - `PROXIMITY`: Check cached player positions against `activationRadius`. If any player in range → set `spawnBudgetRemaining = totalCount`, transition to `ACTIVE`.
2. **ACTIVE spawners** (subject to per-tick budget):
   - Spawn up to `min(spawnBudgetRemaining, remainingBudget)` enemies from the `spawnPool` (weighted random pick per enemy).
   - For each spawn: call `LeveledNpcSpawner.spawn(store, npcRole, level, position, ...)` with a random level in `[minLevel, maxLevel]`.
   - **Spawn position**: Pick from the pre-computed `spawnOffsets` list in the `SpawnerDefinition`. Cycle or random-pick offsets.
   - Track spawned NPC refs in `aliveNpcs` as `Ref<EntityStore>`.
   - When `spawnBudgetRemaining == 0` → transition to `DEPLETED`.
3. **Cleanup** — prune dead NPCs from `aliveNpcs` (check `Ref.isValid()`).
4. **DISABLED** — remove the spawner entity entirely (dungeon teardown).

### 7.5 SpawnerFactory

Creates spawner entities in the ECS from a `DungeonBlueprint`:

```java
public class SpawnerFactory {
    public void createSpawners(Store<EntityStore> store,
                               DungeonBlueprint blueprint,
                               Vec3i worldOrigin) {
        for (SpawnerDefinition def : blueprint.getSpawners()) {
            // Create entity with SpawnerComponent + TransformComponent
            // Position = worldOrigin + def.xyz
        }
    }
}
```

Called by `ZSquadPlugin` after `WorldAssembler.assemble()` completes.

---

## 8. Floor Progression (Future — DB-Driven)

### 8.1 dungeon_floors table

```sql
CREATE TABLE dungeon_floors (
    floor_level   INTEGER PRIMARY KEY,
    theme         TEXT    NOT NULL,     -- "crypt", "mine", "volcanic"
    width         INTEGER NOT NULL DEFAULT 64,
    depth         INTEGER NOT NULL DEFAULT 64,
    height        INTEGER NOT NULL DEFAULT 12,
    max_rooms     INTEGER NOT NULL DEFAULT 15,
    complexity    REAL    NOT NULL DEFAULT 0.5,
    enemy_density REAL    NOT NULL DEFAULT 0.4,
    boss_room     INTEGER NOT NULL DEFAULT 1,
    decay_factor  REAL    NOT NULL DEFAULT 0.3,
    extra_json    TEXT               -- additional LayoutConfig overrides as JSON
);
```

### 8.2 Floor scaling

Floor difficulty increases **linearly** with floor level:

```
complexity(floor)    = baseComplexity + floor * complexityStep
enemyDensity(floor)  = baseDensity    + floor * densityStep
```

Default values (tunable in `dungeon_floors` or global settings):
- `baseComplexity = 0.3`, `complexityStep = 0.05` → Floor 1 = 0.35, Floor 10 = 0.80
- `baseDensity = 0.3`, `densityStep = 0.03` → Floor 1 = 0.33, Floor 10 = 0.60

### 8.3 Generation flow

1. Player enters floor portal → v3-zsquad reads `dungeon_floors` for `floor_level`.
2. Constructs `DungeonConfig` from DB row + linear scaling formula.
3. Calls `GenerationOrchestrator.generate(config)` with `floorLevel` in context.
4. `SpawnerPlacer` uses `floorLevel` to filter `SpawnEntry` eligibility.
5. `WorldAssembler` places blocks + spawner debug markers.
6. `SpawnerFactory` creates spawner entities from the blueprint.
7. Player is teleported to the entrance room.

---

## 9. File Plan

### dungeon-gen — new files

| File | Type | Purpose | Status |
|---|---|---|---|
| `model/SpawnerDefinition.java` | record | Core spawner definition | **DONE** |
| `model/SpawnEntry.java` | record | Weighted NPC entry with level range + floor gate | **DONE** |
| `model/SpawnerType.java` | enum | FIXED, RECURRENT | **DONE** |
| `model/TriggerConfig.java` | record | Trigger type + parameters | **DONE** |
| `model/TriggerType.java` | enum | ON_CREATE, PROXIMITY, TIMED, ON_ROOM_ENTER, ON_ROOM_CLEAR | **DONE** |
| `config/asset/SpawnPoolEntry.java` | codec class | SpawnPools JSON entry | **DONE** |
| `config/asset/SpawnPoolsEntry.java` | codec class | SpawnPools tier container | **DONE** |
| `generator/entity/SpawnerPlacer.java` | class | Produces SpawnerDefinitions from graph | **DONE** |

### dungeon-gen — modified files

| File | Change | Status |
|---|---|---|
| `model/DungeonBlueprint.java` | `List<SpawnPoint>` → `List<SpawnerDefinition>` | **DONE** |
| `model/SpawnPoint.java` | **DELETE** — replaced by SpawnerDefinition | **DONE** |
| `generator/entity/SpawnPointPlacer.java` | **DELETE** — replaced by SpawnerPlacer | **DONE** |
| `generator/GenerationOrchestrator.java` | Use SpawnerPlacer, pass floorLevel | **DONE** |
| `config/DungeonConfig.java` | Add `floorLevel` field | **DONE** |
| `config/asset/DungeonSettingsConfig.java` | Add `spawnerBlock` field | **DONE** |
| `config/asset/DungeonThemeConfig.java` | Parse SpawnPools from JSON | **DONE** |
| `assembly/WorldAssembler.java` | Place spawner debug blocks from blueprint | **DONE** |
| `assets/.../Themes/*.json` | Add SpawnPools section to all 7 themes | **DONE** |
| `assets/.../Settings/Generation.json` | Add `SpawnerBlock` field | **DONE** |
| HTTP export surface | Removed with the embedded HTTP layer | N/A |

### v3-zsquad — new files

| File | Type | Purpose | Status |
|---|---|---|---|
| `spawner/SpawnerComponent.java` | Component | ECS component holding spawner state | **DONE** |
| `spawner/SpawnerState.java` | enum | DORMANT, ACTIVE, DEPLETED, DISABLED | **DONE** |
| `spawner/SpawnerTickSystem.java` | System | ECS tick system driving spawner logic | **DONE** |
| `spawner/SpawnerFactory.java` | class | Creates spawner entities from blueprint | **DONE** |

### v3-zsquad — modified files

| File | Change | Status |
|---|---|---|
| `ZSquadPlugin.java` | Register SpawnerComponent type, SpawnerTickSystem, SpawnerFactory. Add getters. | **DONE** |
| `build.gradle.kts` | Add dungeon-gen compileOnly dependency | **DONE** |

---

## 10. Implementation Order

| Phase | Scope | Deliverable | Status |
|---|---|---|---|
| **A** | dungeon-gen data model | New records: `SpawnerDefinition`, `SpawnEntry`, `SpawnerType`, `TriggerConfig`, `TriggerType`. Update `DungeonBlueprint`. Delete `SpawnPoint`. | **DONE** (2026-03-01) |
| **B** | dungeon-gen SpawnPools | `SpawnPoolEntry`/`SpawnPoolsEntry` codecs. Update `DungeonThemeConfig` + `DungeonSettingsConfig`. Add `SpawnPools` to all 7 theme JSONs. Add `SpawnerBlock` to `Generation.json`. | **DONE** (2026-03-01) |
| **C** | dungeon-gen placement | `SpawnerPlacer` producing definitions with clustering + spawn offsets. Update `GenerationOrchestrator` + `DungeonConfig` (add `floorLevel`). Update `GenerationResult` (add spawners stat). Update `WorldAssembler` (spawner marker blocks). Gut `SpawnPointPlacer`. | **DONE** (2026-03-01) |
| **D** | dungeon-gen build | `./gradlew build` passes. REST response includes spawner count. All new files registered in `api-docs-status.js`. | **DONE** (2026-03-01) |
| **E** | v3-zsquad runtime | `SpawnerComponent`, `SpawnerState`, `SpawnerTickSystem`, `SpawnerFactory`. Wire into `ZSquadPlugin`. | **DONE** (2026-03-02) |
| **F** | v3-zsquad floor DB | `dungeon_floors` table + repository. Floor-level generation flow. | Not started |

---

## 11. Design Decisions (Resolved)

| # | Question | Decision |
|---|---|---|
| 1 | **Activation radius** | Derived from room AABB: `max(width, depth) / 2`. No fixed default. |
| 2 | **Spawn position offset** | Pre-computed `spawnOffsets` validated at generation time via `BlockGrid`. Runtime picks from this list. |
| 3 | **Boss spawner** | Dedicated spawner for the boss (separate from minion spawners). |
| 4 | **Town portal** | Consumable item, single use, no cooldown. Out of scope for this phase. |
| 5 | **Floor scaling** | Linear: `complexity = base + floor * step`. |
| 6 | **Spawner debug block** | `Furniture_Temple_Scarak_Window` placed at spawner positions. Configurable in `Generation.json`, null = invisible. |

---

## 12. Future Enhancements

The following items are explicitly **out of scope** for the initial implementation but noted for future consideration:

- **Activation feedback**: Particle burst / sound effect when a spawner triggers (prevent enemies "popping in").
- **Deterministic spawn seed**: Store a `spawnSeed` in `SpawnerDefinition` (derived from `id + dungeonSeed`) for reproducible runtime RNG.
- **Boss-specific mechanics**: Intro sequence, arena barriers, boss HP bar UI — keyed off the `isBoss` flag.
- **Dungeon teardown lifecycle**: Document who transitions spawners to DISABLED and when (floor transition, player disconnect, server shutdown). Currently assumes ephemeral worlds.
