# Dungeon Generator — Hytale Asset & API Report

> Generated: 2026-02-19  
> Source: Live Hytale Dedicated Server (3964 BlockTypes, 16 Fluids, 91 asset types)

---

## 1. Block Palettes by Theme

> **Note:** Keys prefixed with `*` are internal state definitions (e.g., stair corners, door states). Only **non-prefixed keys** are the primary blocks you'd use in `BlockType.fromString()`.
> Each rock type follows a consistent pattern: `Rock_{Material}`, `_Brick`, `_Cobble` → with variants `_Beam`, `_Decorative`, `_Half`, `_Ornate`, `_Pillar_Base`, `_Pillar_Middle`, `_Roof`, `_Roof_Flat`, `_Roof_Hollow`, `_Roof_Shallow`, `_Roof_Steep`, `_Smooth`, `_Smooth_Half`, `_Stairs`, `_Wall`.

### 1.1 Dark Crypt / Undead Dungeon

**Primary walls:**
| Block ID | Description |
|---|---|
| `Rock_Stone` | Basic grey stone |
| `Rock_Stone_Brick` | Stone brick (classic dungeon wall) |
| `Rock_Stone_Brick_Mossy` | Mossy stone brick |
| `Rock_Stone_Brick_Smooth` | Polished stone |
| `Rock_Stone_Cobble` | Cobblestone |
| `Rock_Stone_Cobble_Mossy` | Mossy cobblestone |
| `Rock_Stone_Mossy` | Mossy stone |
| `Rock_Basalt` | Dark basalt stone |
| `Rock_Basalt_Brick` | Dark basalt brick |
| `Rock_Basalt_Cobble` | Basalt cobblestone |
| `Rock_Shale` | Deep dark shale |
| `Rock_Shale_Brick` | Shale brick |
| `Rock_Shale_Cobble` | Shale cobblestone |

**Structural:**
| Block ID | Use |
|---|---|
| `Rock_Stone_Brick_Pillar_Base` | Column base |
| `Rock_Stone_Brick_Pillar_Middle` | Column shaft |
| `Rock_Basalt_Brick_Pillar_Base` | Dark pillar base |
| `Rock_Basalt_Brick_Pillar_Middle` | Dark pillar shaft |
| `Rock_Stone_Brick_Stairs` | Stone stairs |
| `Rock_Stone_Brick_Half` | Half-slab |
| `Rock_Stone_Brick_Wall` | Low wall |
| `Rock_Stone_Brick_Beam` | Beam/lintel |

**Decay/Ruin:**
| Block ID | Use |
|---|---|
| `Rubble_Stone` | Small stone rubble |
| `Rubble_Stone_Medium` | Medium stone rubble |
| `Rubble_Stone_Mossy` | Mossy rubble |
| `Rubble_Stone_Mossy_Medium` | Mossy rubble (larger) |
| `Rubble_Basalt` | Basalt rubble |
| `Rubble_Basalt_Medium` | Basalt rubble (larger) |

---

### 1.2 Volcanic / Fire Dungeon

| Block ID | Description |
|---|---|
| `Rock_Volcanic` | Volcanic rock |
| `Rock_Volcanic_Brick` | Volcanic brick |
| `Rock_Volcanic_Cobble` | Volcanic cobble |
| `Rock_Volcanic_Cracked_Incandescent` | Cracked with glowing lava veins |
| `Rock_Volcanic_Cracked_Lava` | Cracked with lava |
| `Rock_Volcanic_Cracked_Poisoned` | Cracked with poison |
| `Rock_Volcanic_Brick_Pillar_Base` | Volcanic pillar base |
| `Rock_Volcanic_Brick_Pillar_Middle` | Volcanic pillar middle |
| `Rock_Volcanic_Stalactite_Large` | Stalactite decoration |
| `Rock_Volcanic_Stalactite_Small` | Small stalactite |
| `Rubble_Volcanic` | Volcanic rubble |
| `Rubble_Volcanic_Medium` | Volcanic rubble (larger) |

**Ores (treasure accents):**
| Block ID | Description |
|---|---|
| `Ore_Gold_Volcanic` | Gold embedded in volcanic |
| `Ore_Iron_Volcanic` | Iron in volcanic |
| `Ore_Silver_Volcanic` | Silver in volcanic |

---

### 1.3 Arcane / Runic Dungeon

| Block ID | Description |
|---|---|
| `Rock_Runic_Brick` | Runic brick (base) |
| `Rock_Runic_Brick_Ornate` | Ornate runic brick |
| `Rock_Runic_Cobble` | Runic cobblestone |
| `Rock_Runic_Blue_Brick` | Blue runic brick |
| `Rock_Runic_Dark_Brick` | Dark runic brick |
| `Rock_Runic_Teal_Brick` | Teal runic brick |
| `Rock_Runic_Cobble_Pillar_Base` | Runic pillar base |
| `Rock_Runic_Cobble_Pillar_Middle` | Runic pillar shaft |
| `Rock_Runic_Blue_Brick_Pillar_Base` | Blue runic pillar |
| `Rock_Runic_Dark_Brick_Pillar_Base` | Dark runic pillar |
| `Rock_Runic_Teal_Brick_Pillar_Base` | Teal runic pillar |
| `Rock_Runic_Blue_Brick_Pipe_Corner` | Pipe corner (arcane conduit) |
| `Rock_Runic_Blue_Brick_Pipe_Short` | Pipe straight |
| `Rock_Runic_Teal_Brick_Pipe_Corner` | Teal pipe corner |
| `Rock_Runic_Teal_Brick_Pipe_Short` | Teal pipe straight |

---

### 1.4 Desert / Sandstone Dungeon

| Block ID | Description |
|---|---|
| `Rock_Sandstone` | Base sandstone |
| `Rock_Sandstone_Brick` | Sandstone brick |
| `Rock_Sandstone_Cobble` | Sandstone cobble |
| `Rock_Sandstone_Red` | Red sandstone |
| `Rock_Sandstone_Red_Brick` | Red sandstone brick |
| `Rock_Sandstone_Red_Cobble` | Red sandstone cobble |
| `Rock_Sandstone_White` | White sandstone |
| `Rock_Sandstone_White_Brick` | White sandstone brick |
| `Rock_Sandstone_White_Cobble` | White sandstone cobble |
| `Rock_Sandstone_Brick_Ornate` | Ornate sandstone |
| `Rock_Sandstone_Brick_Decorative` | Decorative sandstone |
| `Rock_Sandstone_Brick_Pillar_Base` | Sandstone pillar base |
| `Rock_Sandstone_Brick_Pillar_Middle` | Sandstone pillar shaft |
| `Rock_Sandstone_Stalactite_Large` | Stalactite |
| `Rubble_Sandstone` | Rubble |

---

### 1.5 Temple of Light / Holy Dungeon

| Block ID | Description |
|---|---|
| `Rock_Marble` | White marble |
| `Rock_Marble_Brick` | Marble brick |
| `Rock_Marble_Cobble` | Marble cobble |
| `Rock_Marble_Brick_Ornate` | Ornate marble |
| `Rock_Marble_Brick_Decorative` | Decorative marble |
| `Rock_Marble_Brick_Pillar_Base` | Marble pillar base |
| `Rock_Marble_Brick_Pillar_Middle` | Marble pillar shaft |
| `Rock_Gold_Brick` | Gold brick (accents) |
| `Rock_Gold_Brick_Ornate` | Ornate gold brick |
| `Rock_Gold_Brick_Decorative` | Decorative gold brick |
| `Rock_Gold_Brick_Pillar_Base` | Gold pillar base |
| `Rock_Gold_Brick_Pillar_Middle` | Gold pillar shaft |
| `Rock_Calcite_Brick` | Calcite (white-ish) |
| `Rock_Quartzite_Brick` | Quartzite |
| `Rubble_Marble` | Marble rubble |

---

### 1.6 Mine / Cave Dungeon

| Block ID | Description |
|---|---|
| `Rock_Stone` | Raw stone |
| `Rock_Stone_Cobble` | Cobble |
| `Rock_Slate` | Slate |
| `Rock_Slate_Cobble` | Slate cobble |
| `Soil_Gravel` | Gravel floor |
| `Soil_Gravel_Mossy` | Mossy gravel |
| `Rock_Stone_Stalactite_Large` | Stalactites |
| `Rock_Stone_Stalactite_Small` | Small stalactites |
| `Rock_Basalt_Stalactite_Large` | Basalt stalactites |

**Ore veins (mineable/decorative):**
| Block ID | Host Rock |
|---|---|
| `Ore_Iron_Stone` | Stone |
| `Ore_Gold_Stone` | Stone |
| `Ore_Silver_Stone` | Stone |
| `Ore_Copper_Stone` | Stone |
| `Ore_Mithril_Stone` | Stone |
| `Ore_Iron_Basalt` | Basalt |
| `Ore_Iron_Basalt_Cracked` | Cracked basalt |
| `Ore_Gold_Basalt` | Basalt |
| `Ore_Silver_Basalt` | Basalt |
| `Ore_Cobalt_Slate` | Slate |
| `Ore_Cobalt_Slate_Cracked` | Cracked slate |
| `Ore_Iron_Slate` | Slate |
| `Ore_Silver_Slate` | Slate |
| `Ore_Cobalt_Shale` | Shale |
| `Ore_Copper_Shale` | Shale |
| `Ore_Gold_Shale` | Shale |
| `Ore_Iron_Shale` | Shale |
| `Ore_Silver_Shale` | Shale |
| `Ore_Adamantite_Magma` | Magma (end-game) |
| `Ore_Thorium_Sandstone` | Sandstone |
| `Ore_Thorium_Mud` | Mud |

---

### 1.7 Organic / Mushroom Cavern

| Block ID | Description |
|---|---|
| `Plant_Moss_Block_Green` | Green moss block |
| `Plant_Moss_Block_Blue` | Blue moss block |
| `Plant_Moss_Block_Red` | Red moss block |
| `Plant_Moss_Block_Green_Dark` | Dark green moss |
| `Plant_Moss_Block_Yellow` | Yellow moss |
| `Plant_Moss_Cave_Green` | Cave wall moss |
| `Plant_Moss_Cave_Blue` | Blue cave moss |
| `Plant_Moss_Cave_Red` | Red cave moss |
| `Plant_Crop_Mushroom_Block_Blue` | Blue mushroom block |
| `Plant_Crop_Mushroom_Block_Brown` | Brown mushroom block |
| `Plant_Crop_Mushroom_Block_Green` | Green mushroom block |
| `Plant_Crop_Mushroom_Block_Purple` | Purple mushroom block |
| `Plant_Crop_Mushroom_Block_Red` | Red mushroom block |
| `Plant_Crop_Mushroom_Block_White` | White mushroom block |
| `Plant_Crop_Mushroom_Block_Yellow` | Yellow mushroom block |
| `Plant_Crop_Mushroom_Glowing_Blue` | Glowing blue (light source) |
| `Plant_Crop_Mushroom_Glowing_Green` | Glowing green |
| `Plant_Crop_Mushroom_Glowing_Orange` | Glowing orange |
| `Plant_Crop_Mushroom_Glowing_Purple` | Glowing purple |
| `Plant_Crop_Mushroom_Glowing_Red` | Glowing red |
| `Plant_Crop_Mushroom_Glowing_Violet` | Glowing violet |
| `Plant_Crop_Mushroom_Block_Blue_Trunk` | Blue mushroom trunk |
| `Plant_Crop_Mushroom_Block_Blue_Branch` | Blue mushroom branch |
| `Plant_Crop_Mushroom_Block_Blue_Mycelium` | Blue mycelium |

---

### 1.8 Hive / Scarak Dungeon

| Block ID | Description |
|---|---|
| `Soil_Hive` | Base hive material |
| `Soil_Hive_Brick` | Hive brick |
| `Soil_Hive_Brick_Beam` | Hive beam |
| `Soil_Hive_Brick_Fence` | Hive fence |
| `Soil_Hive_Brick_Smooth` | Smooth hive brick |
| `Soil_Hive_Corrupted` | Corrupted hive |
| `Soil_Hive_Corrupted_Brick` | Corrupted hive brick |
| `Soil_Hive_Corrupted_Brick_Beam` | Corrupted beam |
| `Deco_Hive` | Decorative hive |
| `Deco_Scarak_Eggsacks` | Scarak egg sacs |

---

### 1.9 Wood Varieties (11 types)

Each wood type has: `_Planks`, `_Planks_Half`, `_Beam`, `_Decorative`, `_Ornate`, `_Fence`, `_Fence_Gate`, `_Roof`, `_Roof_Flat`, `_Roof_Hollow`, `_Roof_Shallow`, `_Roof_Steep`, `_Stairs`.

| Wood Type | Prefix | Mood |
|---|---|---|
| Blackwood | `Wood_Blackwood_` | Dark, ominous |
| Darkwood | `Wood_Darkwood_` | Dark forest |
| Deadwood | `Wood_Deadwood_` | Rotted, abandoned |
| Drywood | `Wood_Drywood_` | Desert, arid |
| Goldenwood | `Wood_Goldenwood_` | Rich, golden |
| Greenwood | `Wood_Greenwood_` | Forest, verdant |
| Hardwood | `Wood_Hardwood_` | Sturdy, generic |
| Lightwood | `Wood_Lightwood_` | Light-colored |
| Redwood | `Wood_Redwood_` | Warm red |
| Softwood | `Wood_Softwood_` | Common, cheap |
| Tropicalwood | `Wood_Tropicalwood_` | Jungle |

**Dungeon-relevant picks:** `Wood_Deadwood_Planks` (abandoned), `Wood_Blackwood_Planks` (dark), `Wood_Darkwood_Planks` (dungeon).

---

### 1.10 Crystal Formations

| Block ID | Description |
|---|---|
| `Rock_Crystal_Blue_Block` | Blue crystal (solid) |
| `Rock_Crystal_Blue_Large` | Large blue crystal |
| `Rock_Crystal_Blue_Medium` | Medium blue crystal |
| `Rock_Crystal_Blue_Small` | Small blue crystal |
| `Rock_Crystal_Cyan_Block` / `_Large` / `_Medium` / `_Small` | Cyan crystals |
| `Rock_Crystal_Green_Block` / `_Large` / `_Medium` / `_Small` | Green crystals |
| `Rock_Crystal_Pink_Block` / `_Large` / `_Medium` / `_Small` | Pink crystals |
| `Rock_Crystal_Purple_Block` / `_Large` / `_Medium` / `_Small` | Purple crystals |
| `Rock_Crystal_Red_Block` / `_Large` / `_Medium` / `_Small` | Red crystals |
| `Rock_Crystal_White_Block` / `_Large` / `_Medium` / `_Small` | White crystals |
| `Rock_Crystal_Yellow_Block` / `_Large` / `_Medium` / `_Small` | Yellow crystals |
| `Rock_Gem_Voidstone` | Voidstone gem |

---

## 2. Decorative Props

### 2.1 Furniture by Theme

The game uses themed furniture sets. Dungeon-relevant sets:

| Set Prefix | Theme |
|---|---|
| `Furniture_Ancient_` | Ancient ruins — ideal for crypts |
| `Furniture_Human_Ruins_` | Human ruins — abandoned dungeons |
| `Furniture_Temple_Dark_` | Dark temple |
| `Furniture_Temple_Emerald_` | Emerald temple |
| `Furniture_Temple_Light_` | Light temple |
| `Furniture_Temple_Wind_` | Wind temple |
| `Furniture_Temple_Scarak_` | Scarak/insectoid temple |
| `Furniture_Dungeon_` | Explicit dungeon items |
| `Furniture_Crude_` | Primitive/rough |
| `Furniture_Feran_` | Feran civilization |
| `Furniture_Frozen_Castle_` | Ice castle |
| `Furniture_Jungle_` | Jungle temple |
| `Furniture_Desert_` | Desert ruins |

### 2.2 Chests (Loot Containers)

| Block ID | Tier |
|---|---|
| `Furniture_Dungeon_Chest_Epic` | Epic (small) |
| `Furniture_Dungeon_Chest_Epic_Large` | Epic (large) |
| `Furniture_Dungeon_Chest_Legendary_Large` | Legendary (large, double-state) |
| `Furniture_Ancient_Chest_Small` | Ancient small |
| `Furniture_Ancient_Chest_Large` | Ancient large |
| `Furniture_Human_Ruins_Chest_Small` | Ruins small |
| `Furniture_Human_Ruins_Chest_Large` | Ruins large |
| `Furniture_Temple_Dark_Chest_Small` | Dark temple small |
| `Furniture_Temple_Dark_Chest_Large` | Dark temple large |
| `Furniture_Temple_Emerald_Chest_Small` | Emerald temple small |
| `Furniture_Temple_Light_Chest_Small` | Light temple small |
| `Furniture_Temple_Scarak_Chest_Small` | Scarak small |
| `Furniture_Crude_Chest_Small` | Crude small |
| `Furniture_Crude_Chest_Large` | Crude large |
| `Furniture_Desert_Chest_Small` | Desert small |
| `Furniture_Frozen_Castle_Chest_Small` | Ice castle small |
| `Furniture_Jungle_Chest_Small` | Jungle small |
| `Furniture_Feran_Chest_Small` | Feran small |

### 2.3 Tables

| Block ID |
|---|
| `Furniture_Ancient_Table` |
| `Furniture_Human_Ruins_Table` |
| `Furniture_Temple_Dark_Table` |
| `Furniture_Temple_Emerald_Table` |
| `Furniture_Temple_Light_Table` |
| `Furniture_Temple_Wind_Table` |
| `Furniture_Royal_Magic_Table` |
| `Furniture_Crude_Table` |
| `Furniture_Desert_Table` |
| `Furniture_Frozen_Castle_Table` |

### 2.4 Chairs & Benches

| Block ID |
|---|
| `Furniture_Ancient_Chair` |
| `Furniture_Ancient_Bench` |
| `Furniture_Human_Ruins_Bench` |
| `Furniture_Temple_Light_Bench` |
| `Furniture_Castle_Bench` |
| `Furniture_Frozen_Castle_Bench` |
| `Furniture_Jungle_Bench` |
| `Furniture_Tavern_Bench` |
| `Furniture_Village_Bench` |

### 2.5 Bookshelves & Paintings

| Block ID |
|---|
| `Furniture_Ancient_Bookshelf` |
| `Furniture_Human_Ruins_Bookshelf` |
| `Furniture_Feran_Bookshelf` |
| `Furniture_Jungle_Bookshelf` |
| `Furniture_Bookshelf_Single` |
| `Furniture_Bookshelf_Left` / `_Middle` / `_Right` / `_Corner` |
| `Furniture_Ancient_Painting` |
| `Furniture_Jungle_Painting` |
| `Furniture_Kweebec_Painting` |
| `Furniture_Lumberjack_Painting` |
| `Furniture_Tavern_Painting` |
| `Furniture_Temple_Light_Painting` |
| `Furniture_Village_Painting_1x1` / `_1x2` / `_2x1` / `_2x2` / `_3x2` |

### 2.6 Coffins (Crypt-specific)

| Block ID |
|---|
| `Furniture_Ancient_Coffin` |
| `Furniture_Human_Ruins_Coffin` |
| `Furniture_Temple_Dark_Coffin` |
| `Furniture_Village_Coffin` |

### 2.7 Barrels, Crates, Pots, Sacks

| Block ID | Type |
|---|---|
| `Furniture_Ancient_Barrel` | Barrel |
| `Furniture_Tavern_Barrel` | Barrel |
| `Furniture_Ancient_Crate` | Crate |
| `Furniture_Village_Crate` | Crate |
| `Furniture_Ancient_Pot` | Pot |
| `Furniture_Human_Ruins_Pot` | Pot |
| `Furniture_Human_Ruins_Pot_Small` | Small pot |
| `Furniture_Temple_Dark_Pot` | Pot |
| `Furniture_Temple_Light_Pot` | Pot |
| `Furniture_Temple_Scarak_Pot` | Pot |
| `Furniture_Temple_Wind_Pot` | Pot |
| `Furniture_Frozen_Castle_Pot` | Pot |
| `Furniture_Jungle_Pot` | Pot |
| `Furniture_Royal_Magic_Pot` | Pot (arcane) |
| `Deco_Pot_Clay_Broken` | Broken clay pot |
| `Furniture_Ancient_Sack` | Sack |

### 2.8 Bones & Skulls (Horror/Crypt)

| Block ID | Description |
|---|---|
| `Deco_Bone_Full` | Full bone pile |
| `Deco_Bone_Pile` | Scattered bone pile |
| `Deco_Bone_Ribs` | Rib cage |
| `Deco_Bone_Ribs_Feran` | Feran rib cage |
| `Deco_Bone_Ribs_Long` | Long rib cage |
| `Deco_Bone_Skulls` | Skull pile |
| `Deco_Bone_Skulls_Feran` | Feran skull pile |
| `Deco_Bone_Skulls_Feran_Large` | Large Feran skulls |
| `Deco_Bone_Skulls_Feran_Wall` | Feran wall skulls |
| `Deco_Bone_Skulls_Wall` | Wall-mounted skulls |
| `Deco_Bone_Spike` | Bone spike |
| `Deco_Bone_Spike_Large` | Large bone spike |
| `Deco_Bone_Spine` | Spine |

### 2.9 Spider Webs, Vines, Banners

| Block ID | Description |
|---|---|
| `Deco_SpiderWeb` | Spider web (corner) |
| `Deco_SpiderWeb_Flat` | Flat web |
| `Deco_SpiderWeb_Full` | Full web |
| `Plant_Vine` | Vine |
| `Plant_Vine_Hanging` | Hanging vine |
| `Plant_Vine_Green_Hanging` | Green hanging vine |
| `Plant_Vine_Red_Hanging` | Red hanging vine |
| `Plant_Vine_Wall` | Wall vine |
| `Plant_Vine_Wall_Dead` | Dead wall vine |
| `Plant_Vine_Wall_Dry` | Dry wall vine |
| `Plant_Vine_Wall_Poisoned` | Poisoned wall vine |
| `Plant_Vine_Wall_Winter` | Winter wall vine |
| `Plant_Vine_Thick_Roots` | Thick root vine |
| `Plant_Vine_Thick_Vertical` | Vertical thick vine |
| `Furniture_Human_Ruins_Banner` | Ruins banner |
| `Furniture_Human_Ruins_Banner_Double` | Double banner |
| `Furniture_Human_Ruins_Banner_Triple` | Triple banner |
| `Furniture_Outlander_Banner` | Outlander banner |

### 2.10 Iron & Metal Decorations

| Block ID | Description |
|---|---|
| `Deco_Iron_Bars` | Iron bars (jail cell) |
| `Deco_Iron_Bars_Corner` | Iron bars corner |
| `Deco_Iron_Bars_Platforms` | Iron bar platform |
| `Deco_Iron_Chain_Small` | Small iron chain |
| `Deco_Iron_Chains` | Medium iron chains |
| `Deco_Iron_Chains_Vertical` | Vertical chains |
| `Deco_Iron_Stack` | Iron stack |
| `Deco_Wallchain` | Wall-mounted chain |

### 2.11 Statues

| Block ID | Description |
|---|---|
| `Furniture_Ancient_Statue` | Ancient statue |
| `Furniture_Human_Ruins_Statue_Broken` | Broken statue |
| `Furniture_Kweebec_Statue` | Kweebec statue |
| `Furniture_Temple_Dark_Statue` | Dark temple statue |
| `Furniture_Temple_Dark_Statue_Gaia` | Gaia dark statue |
| `Furniture_Temple_Emerald_Statue` | Emerald temple statue |
| `Furniture_Temple_Light_Statue` | Light temple statue |
| `Furniture_Temple_Scarak_Statue` | Scarak statue |
| `Furniture_Temple_Wind_Statue` | Wind temple statue |
| `Furniture_Temple_Wind_Statue_Gaia` | Gaia wind statue |
| `Furniture_Village_Statue` | Village statue |

### 2.12 Doors & Trapdoors

**Doors** (themed, all have Open/Close states):
`Furniture_Ancient_Door`, `Furniture_Human_Ruins_Door`, `Furniture_Temple_Dark_Door`, `Furniture_Temple_Emerald_Door`, `Furniture_Temple_Light_Door`, `Furniture_Temple_Wind_Door`, `Furniture_Crude_Door`, `Furniture_Desert_Door`, `Furniture_Feran_Door`, `Furniture_Frozen_Castle_Door`, `Furniture_Jungle_Door`, `Furniture_Kweebec_Door`, `Furniture_Lumberjack_Door`, `Furniture_Scarak_Hive_Door_Large`, `Furniture_Scarak_Hive_Door_Medium`, `Furniture_Tavern_Door`, `Furniture_Village_Door`

**Trapdoors:**
`Furniture_Ancient_Trapdoor`, `Furniture_Human_Ruins_Trapdoor`, `Furniture_Temple_Dark_Trapdoor`, `Furniture_Temple_Emerald_Trapdoor`, `Furniture_Temple_Light_Trapdoor`, `Furniture_Temple_Wind_Trapdoor`, `Furniture_Crude_Trapdoor`, `Furniture_Desert_Trapdoor`, `Furniture_Frozen_Castle_Trapdoor`, `Furniture_Jungle_Trapdoor`

### 2.13 Ladders

`Furniture_Ancient_Ladder`, `Furniture_Human_Ruins_Ladder`, `Furniture_Temple_Dark_Ladder`, `Furniture_Temple_Emerald_Ladder`, `Furniture_Temple_Light_Ladder`, `Furniture_Temple_Scarak_Ladder`, `Furniture_Temple_Wind_Ladder`, `Furniture_Crude_Ladder`, `Furniture_Desert_Ladder`, `Furniture_Frozen_Castle_Ladder`, `Furniture_Jungle_Ladder`, `Furniture_Kweebec_Ladder`, `Furniture_Scarak_Hive_Ladder`

### 2.14 Crafting Benches (room dressing)

`Bench_Alchemy`, `Bench_Arcane`, `Bench_Armory`, `Bench_Armour`, `Bench_Campfire`, `Bench_Cooking`, `Bench_Furnace`, `Bench_Weapon`, `Bench_WorkBench`, `Bench_Salvage`

---

## 3. Light Sources

### 3.1 Torches

| Block ID | Theme |
|---|---|
| `Furniture_Ancient_Torch` | Ancient (has On/Off state) |
| `Furniture_Crude_Torch` | Crude |
| `Furniture_Desert_Torch` | Desert |
| `Furniture_Feran_Torch` | Feran |
| `Furniture_Feran_Torch_Tall` | Feran (tall) |
| `Furniture_Human_Ruins_Torch` | Human ruins |
| `Furniture_Jungle_Torch` | Jungle |
| `Furniture_Temple_Emerald_Torch` | Emerald temple |
| `Wood_Torch_Wall` | Generic wall torch |

### 3.2 Lanterns

| Block ID | Theme |
|---|---|
| `Deco_Lantern` | Generic (floor) |
| `Deco_Lantern_Ceiling` | Generic (ceiling) |
| `Furniture_Desert_Lantern` | Desert (floor) |
| `Furniture_Desert_Lantern_Ceiling` | Desert (ceiling) |
| `Furniture_Desert_Lantern_Tall` | Desert (tall) |
| `Furniture_Human_Ruins_Lantern` | Ruins (floor, 10 color states) |
| `Furniture_Human_Ruins_Lantern_Ceiling` | Ruins (ceiling, 10 color states) |
| `Furniture_Kweebec_Lantern` | Kweebec (floor) |
| `Furniture_Kweebec_Lantern_Ceiling` | Kweebec (ceiling) |
| `Furniture_Lumberjack_Lantern` | Lumberjack |
| `Furniture_Scarak_Hive_Lantern` | Scarak (floor) |
| `Furniture_Temple_Light_Lantern` | Light temple (floor) |
| `Furniture_Temple_Light_Lantern_Ceiling` | Light temple (ceiling) |
| `Furniture_Temple_Scarak_Lantern` | Scarak temple |

### 3.3 Lamps

| Block ID | Theme |
|---|---|
| `Furniture_Frozen_Castle_Lamp` | Ice castle (8 color states) |
| `Furniture_Frozen_Castle_Secondary_Lamp` | Secondary ice (8 color states) |
| `Furniture_Lumberjack_Lamp` | Lumberjack |
| `Furniture_Scarak_Hive_Lamp` | Scarak hive (8 color states) |
| `Furniture_Temple_Scarak_Lamp` | Scarak temple (8 color states) |

### 3.4 Candles

| Block ID | Theme |
|---|---|
| `Furniture_Ancient_Candle` | Ancient |
| `Furniture_Crude_Candle` | Crude |
| `Furniture_Human_Ruins_Candle` | Ruins |
| `Furniture_Jungle_Candle` | Jungle |
| `Furniture_Kweebec_Candle` | Kweebec |
| `Furniture_Tavern_Candle` | Tavern |
| `Furniture_Temple_Dark_Candle` | Dark temple |
| `Furniture_Temple_Wind_Candle` | Wind temple |

### 3.5 Braziers

| Block ID | Theme |
|---|---|
| `Furniture_Crude_Brazier` | Crude |
| `Furniture_Dungeon_Earth_Brazier` | **Dungeon-specific!** |
| `Furniture_Human_Ruins_Brazier` | Human ruins |
| `Furniture_Jungle_Brazier` | Jungle |
| `Furniture_Temple_Dark_Brazier` | Dark temple |
| `Furniture_Temple_Light_Brazier` | Light temple |
| `Furniture_Temple_Scarak_Brazier` | Scarak temple |
| `Furniture_Village_Brazier` | Village |

### 3.6 Glowing / Emissive Blocks

| Block ID | Description |
|---|---|
| `Furniture_Royal_Magic_Potion_Glow` | Glowing magic potion (floor) |
| `Furniture_Royal_Magic_Potion_Glow_Ceiling` | Glowing potion (ceiling) |
| `Plant_Crop_Mushroom_Glowing_Blue` | Glowing blue mushroom |
| `Plant_Crop_Mushroom_Glowing_Green` | Glowing green mushroom |
| `Plant_Crop_Mushroom_Glowing_Orange` | Glowing orange mushroom |
| `Plant_Crop_Mushroom_Glowing_Purple` | Glowing purple mushroom |
| `Plant_Crop_Mushroom_Glowing_Red` | Glowing red mushroom |
| `Plant_Crop_Mushroom_Glowing_Violet` | Glowing violet mushroom |
| `Rock_Volcanic_Cracked_Incandescent` | Incandescent cracked volcanic |
| `Rock_Volcanic_Cracked_Lava` | Lava-cracked volcanic |
| `Rock_Crystal_*_Block` | Crystal blocks (may emit light) |

---

## 4. Fluids (16 total)

| Fluid ID | Description |
|---|---|
| `Water` | Flowing water |
| `Water_Source` | Water source block |
| `Water_Finite` | Finite water (drains) |
| `Lava` | Flowing lava |
| `Lava_Source` | Lava source |
| `Poison` | Flowing poison |
| `Poison_Source` | Poison source |
| `Slime` | Green slime |
| `Slime_Source` | Slime source |
| `Slime_Red` | Red slime |
| `Slime_Red_Source` | Red slime source |
| `Tar` | Flowing tar |
| `Tar_Source` | Tar source |
| `Fire` | Fire (fluid type) |
| `Empty` | No fluid |
| `Unknown` | Unknown |

---

## 5. Traps & Hazards

| Block ID | Description |
|---|---|
| `Survival_Trap_Spike_Iron` | Iron spike trap |
| `Survival_Trap_Spike_Wood` | Wood spike trap |
| `Survival_Trap_Spike_Wood_Large` | Large wood spike |
| `Survival_Trap_Snapjaw` | Snapjaw bear trap |
| `Survival_Trap_Grass` | Hidden grass trap |
| `Trap_Ancient_Platform` | Collapsing ancient platform |
| `Trap_Ice` | Ice trap |
| `Trap_Slate` | Slate trap |
| `Deco_Bone_Spike` | Bone spike (decorative hazard) |
| `Deco_Bone_Spike_Large` | Large bone spike |

---

## 6. Entity / Spawner Info

### 6.1 Block Spawner Tables (46 tables)

Spawner tables are configured per zone and encounter tier:

| Spawner Table | Description |
|---|---|
| `Zone1_Undead_Tier1` / `_Tier2` / `_Tier3` | Zone 1 undead spawns |
| `Zone2_Undead_Tier1` / `_Tier2` / `_Tier3` | Zone 2 undead |
| `Zone3_Undead_Tier1` / `_Tier2` / `_Tier3` | Zone 3 undead |
| `Zone4_Undead_Tier1` / `_Tier2` / `_Tier3` | Zone 4 undead |
| `Zone1_Trork_Tier1` / `_Tier2` / `_Tier3` | Trork spawns |
| `Zone1_Goblin_Tier1` / `_Tier2` / `_Tier3` | Goblin spawns |
| `Zone2_Feran_Tier1` / `_Tier2` | Feran spawns |
| `Zone3_Outlander_Tier1` / `_Tier2` / `_Tier3` | Outlander spawns |
| `Zone{1-4}_Encounters_Tier{1-4}` | General encounter spawns |

### 6.2 NPC Groups (dungeon-relevant)

| Group | Dungeon Use |
|---|---|
| `Undead` | Undead mobs (skeletons, zombies) |
| `Zombie` | Zombie-specific |
| `Skeleton` | Skeleton-specific |
| `Spiders` | Spider enemies |
| `Scarak` | Insectoid enemies (hive dungeon) |
| `Goblin` | Goblin enemies |
| `Goblin_Scrapper` | Goblin scrapper variant |
| `Trork` | Trork warriors |
| `Trork_Warrior` | Trork warrior variant |
| `Feran` | Feran faction |
| `Outlander` | Outlander enemies |
| `Void` | Void creatures |
| `Aggressive` | All aggressive mobs |
| `Scorpions` | Scorpion enemies |
| `Snakes` | Snake enemies |
| `Rat` | Rats (vermin) |
| `Fen_Stalker` | Fen stalker |

### 6.3 Damage Causes

`Bludgeoning`, `Slashing`, `Physical`, `Projectile`, `Fire`, `Ice`, `Poison`, `Elemental`, `Environmental`, `Environment`, `Fall`, `Drowning`, `Suffocation`, `OutOfWorld`, `Command`

### 6.4 Entity Effects (dungeon-relevant)

| Effect ID | Description |
|---|---|
| `Damage` | Base damage |
| `Damage_High` | High damage |
| `Burn` | Fire burn |
| `Lava_Burn` | Lava burn |
| `Poison` | Poison (base) |
| `Poison_T1` / `_T2` / `_T3` | Tiered poison |
| `Freeze` | Freeze |
| `Slow` | Slowness |
| `Stun` | Stun |
| `Bomb_Explode_Stun` | Explosion stun |
| `Root` | Root (immobilize) |
| `Stoneskin` | Defensive buff |
| `Immune` | Immunity |
| `Immunity_Fire` | Fire immunity |
| `Immunity_Environmental` | Environmental immunity |
| `Death` | Instant death |

---

## 7. Stalactite Collection (Cave Decorations)

| Block ID | Rock Type |
|---|---|
| `Rock_Stone_Stalactite_Large` / `_Small` | Stone |
| `Rock_Basalt_Stalactite_Large` / `_Small` | Basalt |
| `Rock_Volcanic_Stalactite_Large` / `_Small` | Volcanic |
| `Rock_Sandstone_Stalactite_Large` / `_Small` | Sandstone |
| `Rock_Sandstone_Red_Stalactite_Large` / `_Small` | Red sandstone |
| `Rock_Sandstone_White_Stalactite_Large` / `_Small` | White sandstone |
| `Rock_Marble_Stalactite_Large` / `_Small` | Marble |
| `Rock_Calcite_Stalactite_Large` / `_Small` | Calcite |
| `Rock_Quartzite_Stalactite_Large` / `_Small` | Quartzite |
| `Rock_Shale_Stalactites_Large` / `_Small` | Shale |
| `Rock_Slate_Stalactite_Large` / `_Small` | Slate |
| `Rock_Lime_Stalactite_Large` / `_Small` | Lime |
| `Rock_Aqua_Stalactite_Large` / `_Small` | Aqua |
| `Rock_Ice_Stalactite_Large` | Ice |
| `Soil_Clay_Ocean_Stalactite_Large` / `_Small` | Ocean clay |
| `Soil_Clay_Stalactite_Large` / `_Small` | Clay |
| `Stalactite_Smoke_Floor` | Smoke effect |

---

## 8. API Summary

### 8.1 Resolving a BlockType from String ID

```java
// Primary method — resolves "Rock_Stone_Brick" to a BlockType instance
BlockType blockType = BlockType.fromString("Rock_Stone_Brick");

// Get the string ID back
String id = blockType.getId();

// Check if valid
boolean valid = !blockType.isUnknown();
```

**Class:** `com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType`  
**Key methods:** `fromString(String)`, `getId()`, `isUnknown()`, `getAssetStore()`, `getAssetMap()`

### 8.2 Getting the Integer Block ID (for setBlock)

The `BlockTypeAssetMap` maps string keys to integer indexes:

```java
// Get the asset map (singleton)
BlockTypeAssetMap<String, BlockType> blockMap = BlockType.getAssetMap();

// Get integer ID from string key
int blockId = blockMap.getIndex("Rock_Stone_Brick");

// Reverse: get BlockType from integer ID
BlockType bt = blockMap.getAsset(blockId);

// Safe version with default
int blockIdSafe = blockMap.getIndexOrDefault("Rock_Stone_Brick", 0);
```

**Class:** `com.hypixel.hytale.assetstore.map.BlockTypeAssetMap`  
**Key methods:** `getIndex(K key)`, `getIndexOrDefault(K key, int def)`, `getAsset(int index)`, `getAssetOrDefault(int index, T def)`, `getNextIndex()`

### 8.3 Placing Blocks Programmatically

Block placement uses integer block IDs at the chunk/section level:

```java
// Via WorldChunk (implements BlockAccessor)
WorldChunk chunk = ...; // from world
chunk.setBlock(x, y, z, blockId, rotation, filler);
int existingBlock = chunk.getBlock(x, y, z);

// Via GeneratedBlockChunk (world gen context)
GeneratedBlockChunk genChunk = ...;
genChunk.setBlock(x, y, z, blockId, rotation, filler);

// Via GeneratedChunkSection
GeneratedChunkSection section = ...;
section.setBlock(x, y, z, blockId, rotation, filler);
```

**Parameters:**
- `x, y, z` — local coordinates within the chunk/section
- `blockId` — integer ID from `BlockTypeAssetMap.getIndex()`
- `rotation` — block rotation (0 for default)
- `filler` — filler block ID (0 for none)

### 8.4 The Material Helper

`Material` in `com.hypixel.hytale.builtin.buildertools.utils` provides a convenient abstraction:

```java
Material mat = Material.block(blockId);
Material mat2 = Material.block(blockId, rotation);
Material fluidMat = Material.fluid(fluidId, fluidLevel);
Material fromStr = Material.fromKey("Rock_Stone_Brick"); // string lookup

boolean isBlock = mat.isBlock();
int id = mat.getBlockId();
```

### 8.5 Spawning Entities

Entity spawning is done via the ECS `Store<EntityStore>` / `CommandBuffer<EntityStore>` pattern:

```java
// The EntityStore holds all entities for a World
EntityStore entityStore = world.getEntityStore(); // conceptual
Store<EntityStore> store = entityStore.getStore();

// Entities are added via CommandBuffer (deferred execution)
// Lookup by UUID or network ID
Ref<EntityStore> ref = entityStore.getRefFromUUID(uuid);
Ref<EntityStore> ref2 = entityStore.getRefFromNetworkId(networkId);

// For NPC spawning, see NPCSpawnCommand pattern:
// It extends AbstractPlayerCommand and uses
// (CommandContext, Store<EntityStore>, Ref<EntityStore>, PlayerRef, World)
```

The `SpawnNPCInteraction` class shows block-triggered NPC spawning. The `SpawnMarkerEntity` ECS component manages spawn markers with respawn timers, flock counts, and suppression.

### 8.6 The AssetRegistry Pattern

```java
// Global registry — maps asset class to its store
AssetStore<K, T, M> store = AssetRegistry.getAssetStore(BlockType.class);

// Each asset type has a static shortcut:
BlockType.getAssetStore();  // AssetStore<String, BlockType, BlockTypeAssetMap<...>>
BlockType.getAssetMap();    // BlockTypeAssetMap<String, BlockType>

// FluidType, EntityEffect, etc. follow the same pattern
```

**Two AssetRegistry classes exist:**
1. `com.hypixel.hytale.assetstore.AssetRegistry` — global static registry
2. `com.hypixel.hytale.server.core.plugin.registry.AssetRegistry` — plugin-scoped (auto-unregister on shutdown)

### 8.7 Fluid Resolution

Same pattern as blocks:

```java
// Fluid is the asset type (16 fluids total)
// Path: Item/Block/Fluids
// Access: Fluid.getAssetMap() / Fluid.getAssetStore()
// Material.fluid(fluidId, fluidLevel)
```

---

## 9. Quick Reference: Themed Room Palette Suggestions

### Crypt Room
- Walls: `Rock_Stone_Brick`, `Rock_Stone_Brick_Mossy`
- Floor: `Rock_Stone_Cobble`, `Rock_Stone_Cobble_Mossy`
- Pillars: `Rock_Stone_Brick_Pillar_Base` + `_Middle`
- Decay: `Rubble_Stone`, `Rubble_Stone_Mossy`
- Props: `Furniture_Ancient_Coffin`, `Deco_Bone_Skulls`, `Deco_SpiderWeb`
- Light: `Furniture_Ancient_Torch`, `Furniture_Ancient_Candle`
- Loot: `Furniture_Ancient_Chest_Small`

### Volcanic Forge
- Walls: `Rock_Volcanic_Brick`, `Rock_Volcanic_Cobble`
- Floor: `Rock_Volcanic`, `Rock_Volcanic_Cracked_Lava`
- Accent: `Rock_Volcanic_Cracked_Incandescent`
- Fluid: `Lava_Source`, `Lava`
- Props: `Bench_Furnace`, `Bench_Armory`
- Light: `Furniture_Dungeon_Earth_Brazier`

### Arcane Library
- Walls: `Rock_Runic_Blue_Brick`, `Rock_Runic_Teal_Brick`
- Floor: `Rock_Runic_Cobble`
- Pillars: `Rock_Runic_Blue_Brick_Pillar_Base` + `_Middle`
- Props: `Furniture_Ancient_Bookshelf`, `Furniture_Royal_Magic_Table`
- Light: `Furniture_Royal_Magic_Potion_Glow`, `Furniture_Human_Ruins_Lantern`
- Loot: `Furniture_Dungeon_Chest_Epic`
- Crystal: `Rock_Crystal_Blue_Large`, `Rock_Crystal_Purple_Large`

### Mine Shaft
- Walls: `Rock_Stone`, `Rock_Slate`
- Floor: `Soil_Gravel`, `Soil_Gravel_Mossy`
- Ceiling: `Rock_Stone_Stalactite_Large`, `Rock_Basalt_Stalactite_Small`
- Ore: `Ore_Iron_Stone`, `Ore_Gold_Stone`, `Ore_Mithril_Stone`
- Props: `Wood_Darkwood_Beam`, `Wood_Darkwood_Planks`
- Light: `Furniture_Crude_Torch`

### Mushroom Grotto
- Walls: `Plant_Moss_Block_Green`, `Plant_Moss_Block_Blue`
- Floor: `Plant_Moss_Cave_Green`, `Soil_Gravel_Mossy`
- Mushrooms: `Plant_Crop_Mushroom_Block_Blue`, `_Purple`, `_Red`
- Light: `Plant_Crop_Mushroom_Glowing_Blue`, `_Green`, `_Purple`
- Vines: `Plant_Vine_Hanging`, `Plant_Vine_Wall`

### Hive Nest
- Walls: `Soil_Hive_Brick`, `Soil_Hive_Corrupted_Brick`
- Floor: `Soil_Hive`, `Soil_Hive_Corrupted`
- Props: `Deco_Scarak_Eggsacks`, `Deco_Hive`
- Light: `Furniture_Scarak_Hive_Lamp`, `Furniture_Scarak_Hive_Lantern`
- Doors: `Furniture_Scarak_Hive_Door_Large`
- Loot: `Furniture_Scarak_Hive_Chest_Small`

### Temple of Darkness
- Walls: `Rock_Basalt_Brick`, `Rock_Shale_Brick`
- Floor: `Rock_Basalt_Cobble`, `Rock_Shale_Cobble`
- Pillars: `Rock_Basalt_Brick_Pillar_Base` + `_Middle`
- Props: `Furniture_Temple_Dark_Statue`, `Furniture_Temple_Dark_Coffin`
- Light: `Furniture_Temple_Dark_Brazier`, `Furniture_Temple_Dark_Candle`
- Loot: `Furniture_Temple_Dark_Chest_Large`
