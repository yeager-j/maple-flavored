# Plan: ticket 32, Alloy Furnace and Alloy materials

Repo: `C:\Users\jackson\Developer\maple-flavored` (Fabric, MC 26.3). Ticket `.scratch/maple-flavored/issues/32-alloy-furnace-and-materials.md`. Spec: `.scratch/maple-flavored/spec.md` ("Materials and Alloys", "Milestones and advancements", Drops). Design: `.scratch/maple-flavored-design/issues/06-material-roles-and-alloys.md`, `17-advancements.md`. Branch: `32-alloy-furnace-and-materials`.

## Context / end goal

Alloys are Maple's endgame: every storage block gets a use, and Bastions stay worth raiding. This ticket lays the materials layer that tickets 33–36 (Alloy gear, traits, set bonuses, Armored Elytra) build on: a player kills Piglin Brutes for Ancient Embers, crafts an Alloy Furnace, and turns 1 Netherite Ingot + 1 storage block + 1 Ember into one of five Alloy ingots, which store as beacon-base blocks or smelt back to Netherite. Closing it means the whole loop is playable in survival, hopper-automatable apart from the player-kill Ember, and the `Alloy` type exists for ticket 33 to hang traits on.

Decisions from Jackson (this session): the Ember is used up **at completion** (an interrupted smelt loses nothing); textures are **recoloured vanilla**, credited; Shakudo takes **plain `copper_block` only**.

## User-visible outcomes

1. **Ancient Ember** item (uncommon, fire resistant). Piglin Brutes drop 1 on a player kill at 25%, +5% per Looting level, in its own pool (Pith's 10% pool unchanged).
2. **Five Alloy ingots** (Steel, Electrum, Shakudo, Adamant, Orichalcum; fire resistant like Netherite) and **five Alloy blocks** ("Block of Steel" …; Netherite-block strength, needs diamond pickaxe, fire-resistant item). 9 ingots ↔ 1 block. Blocks are beacon bases.
3. **Alloy Furnace** block (facing + lit, like a blast furnace): base slot (Netherite Ingot), addition slot (storage block), fuel slot (Ember), result slot. Recipe `GGG / GBG / PPP` (gold ingots, blast furnace, polished blackstone bricks). 200 ticks per ingot. Hoppers: top feeds base/addition, sides feed the Ember, bottom pulls the result. Comparator output, drops contents when broken.
   - Pairings: iron block → Steel, gold block → Electrum, copper block → Shakudo, diamond block → Adamant, emerald block → Orichalcum.
4. **Smelt back:** furnace (200 ticks) and blast furnace (100 ticks) turn any Alloy ingot into a Netherite Ingot, no XP. Smoker and Alloy Furnace don't.
5. **Advancements (Nether tab):** Get an Ancient Ember (task, under Those Were the Days `minecraft:nether/find_bastion`); Smelt an Alloy ingot (task, under it); **But why?** (hidden challenge, 100 XP, under Beaconator `minecraft:nether/create_full_beacon`).
6. Recipe-book unlocks for the furnace, blocks, ingots-from-blocks and smelt-back; creative tab placement; lang.

## Seams and API

New package `mapleflavored.alloys`, registered from `MapleFlavored.onInitialize` via `Alloys.register()` (after `MapleItems.register()`).

```java
/** The five Alloys. Each owns its ingot and block; ticket 33 adds traits here. */
public enum Alloy {
    STEEL("steel", MapColor...), ELECTRUM("electrum", ...), SHAKUDO("shakudo", ...),
    ADAMANT("adamant", ...), ORICHALCUM("orichalcum", ...);
    public Item ingot();
    public Block block();          // its BlockItem registered alongside
}

public final class Alloys {
    public static final Item ANCIENT_EMBER;
    public static final Block ALLOY_FURNACE;                                   // + BlockItem
    public static final BlockEntityType<AlloyFurnaceBlockEntity> ALLOY_FURNACE_BLOCK_ENTITY;
    public static final MenuType<AlloyFurnaceMenu> ALLOY_FURNACE_MENU;
    public static final RecipeType<AlloyingRecipe> ALLOYING;                  // "maple_flavored:alloying"
    public static final RecipeSerializer<AlloyingRecipe> ALLOYING_SERIALIZER;
    public static void register();   // forces class init, creative tabs, RecipeSynchronization
}

public record AlloyingInput(ItemStack base, ItemStack addition) implements RecipeInput {}

/** {"type":"maple_flavored:alloying","base":<ingredient>,"addition":<ingredient>,"result":<item stack template>} */
public final class AlloyingRecipe implements Recipe<AlloyingInput> {
    // modelled on vanilla BrewingRecipe: isSpecial() true, PlacementInfo.NOT_PLACEABLE,
    // showNotification false, recipeBookCategory CRAFTING_MISC
    public boolean acceptsBase(ItemStack), acceptsAddition(ItemStack);
}
```

- **Slot rules have one authority: the alloying recipes.** `RecipeSynchronization.synchronizeRecipeSerializer(ALLOYING_SERIALIZER)` (fabric-recipe-api-v1) sends them to clients, so `level.recipeAccess().getSynchronizedRecipes().getAllOfType(ALLOYING)` works on both sides. A small package-private `AlloyingSlots.isBase(Level, ItemStack)` / `isAddition(...)` reads it; the block entity's `canPlaceItem` (hoppers, server) and the menu's slots (client prediction + server) both use it. No duplicate item tags.
- **Fuel** is a fixed rule, not data: the fuel slot accepts only `Alloys.ANCIENT_EMBER`.
- `AlloyFurnaceBlock extends AbstractFurnaceBlock` (reuses FACING/LIT, rotation, comparator, neighbour updates); own `codec()`, `newBlockEntity`, `getTicker` (server only), `openContainer`, blast-furnace `animateTick`.
- `AlloyFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer` — 4 slots, `ContainerData` [progress, total=200]. Saved: items + `progress`.
- `AlloyFurnaceMenu extends AbstractContainerMenu` — client ctor `(id, inv)` with `SimpleContainer(4)`/`SimpleContainerData(2)`; result slot refuses placement; `quickMoveStack` routes player items to base/addition/fuel by the same rules; `progressScaled(int)` / `isWorking()` for the screen.
- Client: `AlloyFurnaceScreen extends AbstractContainerScreen<AlloyFurnaceMenu>` registered in `MapleFlavoredClient` with `MenuScreens.register`. Background `textures/gui/container/alloy_furnace.png`; flame/arrow drawn with vanilla's `container/furnace/lit_progress` and `burn_progress` sprites. Follow `EnchantingTableScreen` for 26.3's `extract*` render API.
- **But why?**: `AlloyBeacons.isAllAlloy(Level, BlockPos beacon)` checks the 4 layers (164 blocks) under the beacon against `#maple_flavored:alloy_blocks`. New `BeaconBlockEntityMixin`: `@WrapOperation` on the `updateBase(level, x, y, z)` call in `BeaconBlockEntity.tick` (it runs every 80 ticks while the beam is unobstructed). After the original returns, if it is 4 and `isAllAlloy`, fire `MapleTriggers.ALLOY_BEACON_CONSTRUCTED` for the `ServerPlayer`s in vanilla's Beaconator box (`new AABB(x, y, z, x, y - 4, z).inflate(10, 5, 10)`). So, unlike vanilla's activation-only Beaconator, upgrading an already-lit pyramid to all-Alloy also counts. The check only runs on beacons that are already level 4, every 4 seconds, and re-firing an advancement that's already granted does nothing.
- `MapleTriggers.ALLOY_BEACON_CONSTRUCTED` (`maple_flavored:alloy_beacon_constructed`, PlayerTrigger).

## Furnace rules (invariants)

Each server tick:
1. `recipe` = first `ALLOYING` match for (base, addition) via `RecipeManager.createCheck`.
2. `canSmelt` = recipe present ∧ fuel slot has an Ember ∧ result slot is empty or holds the same item with room.
3. If `canSmelt`: `progress++`; at 200: shrink base, addition and fuel by 1, add `recipe.assemble` to result, `progress = 0`. Else `progress = 0`.
4. `LIT` = `canSmelt` (set block state only on change).
5. `setItem` on base/addition with a different item resets progress (vanilla furnace rule), so swapping iron for gold mid-smelt restarts.

So: exactly one Ember per ingot, nothing is used until an ingot is made, no XP.

Hopper faces: UP → [base, addition]; sides → [fuel]; DOWN → [result]. `canPlaceItemThroughFace` = `canPlaceItem`; `canTakeItemThroughFace` only the result slot. Wrong items stay in the hopper.

## Data and assets

- **Tags:** `maple_flavored:alloy_ingots`, `maple_flavored:alloy_blocks` (5 each); `minecraft:beacon_base_blocks` += `#maple_flavored:alloy_blocks`; `minecraft:mineable/pickaxe` += furnace + blocks; `minecraft:needs_diamond_tool` += blocks.
- **Loot:** `data/minecraft/loot_table/entities/piglin_brute.json` gains a pool: `all_of[killed_by_player, random_chance_with_enchanted_bonus{unenchanted_chance 0.25, enchanted_chance linear base 0.30 per_level_above_first 0.05, enchantment looting}]` → `maple_flavored:ancient_ember` (shape from vanilla wither_skeleton skull). Block loot tables for the furnace (copy of vanilla `blast_furnace.json`) and each Alloy block.
- **Recipes:** `alloy_furnace` (shaped), `<alloy>_ingot` (alloying ×5), `<alloy>_block` and `<alloy>_ingot_from_<alloy>_block` (×5 each), `netherite_ingot_from_smelting_alloy_ingot` and `netherite_ingot_from_blasting_alloy_ingot` (ingredient `#maple_flavored:alloy_ingots`).
- **Recipe unlock advancements** under `advancement/recipes/…` following `recipes/misc/crystal_heart.json`: furnace (has Ember or Netherite Ingot), blocks (has ingot), ingots-from-block (has block), smelt-back (has any Alloy ingot).
- **Advancements:** `maple_flavored:nether/obtain_ancient_ember` ("Old Flame", inventory_changed Ember), `maple_flavored:nether/smelt_alloy_ingot` ("Mixed Metals", inventory_changed `#maple_flavored:alloy_ingots`, matching how vanilla's "Smelt an iron ingot" works), `maple_flavored:nether/but_why` ("But why?", hidden, challenge, `rewards.experience 100`, `announce_to_chat` default).
- **Assets:** blockstates (furnace copies vanilla blast_furnace facing×lit), block/item models, item model definitions under `assets/maple_flavored/items/`, lang (`item.`, `block.`, `container.maple_flavored.alloy_furnace`, advancement title/description).
- **Textures** (Pillow/ImageMagick script kept in the scratchpad, outputs committed): ingots recoloured from `netherite_ingot.png`, blocks from `netherite_block.png`, Ember from `fire_charge.png`, furnace front/front_on/side/top from blast furnace, GUI from `furnace.png` with a second input slot. Each listed in `CREDITS.md` as Mojang's, recoloured by Jackson Yeager (matching the subspace_shell entry). Send Jackson a contact sheet.
- **Creative tabs:** Ember after Pith, ingots after Netherite Ingot (Ingredients); blocks after Block of Netherite (Building Blocks); furnace after Blast Furnace (Functional).

## Steps

1. Branch. `Alloy`, `Alloys` (items, blocks, Ember), tags, block loot, lang, models, textures, creative tabs.
2. `AlloyingInput`, `AlloyingRecipe`, type/serializer registration + sync; alloying, block and smelt-back recipes.
3. `AlloyFurnaceBlock`, block entity, menu; client screen + GUI texture; furnace recipe.
4. Brute loot pool.
5. `MapleTriggers.ALLOY_BEACON_CONSTRUCTED`, `AlloyBeacons`, `BeaconBlockEntityMixin` (+ mixins json), three advancements, recipe-unlock advancements.
6. GameTests (below), registered by adding `mapleflavored.gametest.AlloyGameTests` to the `fabric-gametest` entrypoints in `src/gametest/resources/fabric.mod.json`, and checked in `build/gametest/report.xml`. Then `CREDITS.md`, and tick the ticket boxes (hand check left open).

## Tests (`AlloyGameTests`, new environments where players or a beacon need a batch to themselves)

- Ember drops: roll the Brute table with a seeded random (pattern of `CrystalHeartGameTests.assertDropRate`) killed by a player: no Looting ≈ 0.25, Looting III ≈ 0.40; with no player 0; Pith still ≈ 0.10 on player kills.
- Alloying recipes: each storage block + Netherite Ingot → its Alloy; netherite block, waxed copper block, lapis block, and iron block + diamond → nothing.
- Furnace timing: slots set directly; no result at 199 ticks, Steel at 200 (±1); base, block and Ember each down 1.
- One Ember per ingot: 3 ingots, 3 blocks, 1 Ember → after 450 ticks 1 Steel, 2+2 left, 0 Embers.
- Interrupted: remove the block at 100 ticks → Ember kept; put it back → result only after a further full 200.
- No Ember: nothing after 300 ticks.
- Hoppers: hopper above (Netherite Ingot, iron block, dirt), hopper on a side (Ember), hopper below into a chest → chest gets Steel; dirt stays in the top hopper.
- Menu: mock player opens the furnace; `quickMoveStack` puts ingot, block and Ember in their slots and refuses dirt.
- Smelt back: SMELTING and BLASTING turn each Alloy ingot into a Netherite Ingot; SMOKING doesn't.
- Crafting: furnace pattern; each block ↔ 9 ingots.
- Blocked output: result slot holds 64 Steel → no progress after 300 ticks, all three inputs kept; holding Electrum → same.
- Swap mid-smelt: iron block swapped for gold block at 100 ticks → Electrum only after a further full 200, Ember kept until then.
- Persistence: a furnace 100 ticks in, saved and loaded (block entity round trip through `saveWithFullMetadata` / `loadWithComponents`), keeps its items and progress, and finishes 100 ticks later.
- Breaking: a part-smelted furnace broken by a player drops itself plus its contents exactly once.
- Comparator: signal rises from 0 when inputs are added.
- Beacon (in a cleared space with sky above, like `SubspaceArena`): mixed-Alloy level 4 pyramid + nearby player → Beaconator and But why?. Second test: pyramid with one iron block → Beaconator only; then, with the beacon left lit, the iron block is replaced with an Alloy block → But why? within 200 ticks.
- Advancements: giving an Ember / an Alloy ingot grants the two tasks; parents, frames, hidden flag and 100 XP checked (pattern `CrystalHeartGameTests.assertAdvancement`).

## Checks

`gradlew build` and `gradlew runGameTest` (JAVA_HOME from `gradle.properties`), full suite green. LARP review of the diff. PR closes ticket 32.

## Hand check (left open in the ticket)

Furnace GUI layout, flame/arrow, lit texture and particles; textures in inventory and placed; hopper automation in a real world; beacon beam on Alloy blocks.

## Out of scope

Alloy gear, traits, repairs, set bonuses, Armored Elytra (33–36); Orichalcum's drop bonus excluding Embers (34); Electrum counting as gold for Piglins; Alloy ingots as beacon payment.

## Least confident

- The spec lists "getting an Ancient Ember" and "smelting an Alloy ingot" among its custom criteria triggers. The plan uses vanilla's `inventory_changed` for both instead, as vanilla's own "Smelt an iron ingot" does. Alloy ingots come only from the Alloy Furnace, so the observable result is the same, and there's no extra code.
- Getting a beacon beam inside the GameTest world (heightmap/sky above); fall back to building high above the test like `SubspaceArena`.
- Exact 26.3 names (`LootContextParams.LAST_DAMAGE_PLAYER`, screen `extract*` methods, `addStandardInventorySlots`) are checked against the extracted sources while implementing.
