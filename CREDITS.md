# Credits

Maple Flavored is inspired by these projects. Neither author is involved in or endorses this mod.

- [Matcha Flavoured](https://modrinth.com/datapack/matcha-flavoured) by Klei ([kleiwright](https://github.com/kleiwright/matcha-flavoured)), licensed under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).
- [Raspberry Flavoured](https://www.curseforge.com/minecraft/modpacks/raspberry-flavoured) by Raspmary.

## Reused designs

- The idea of a Crystal Heart recipe from diamonds around 1 Pith, which Maple makes 5 diamonds in a heart shape where Matcha Flavoured uses 8, and the Nether Star recipe (1 Nether Star into 9 Pith) come from Matcha Flavoured by Klei (`dp/data/matcha/recipe/crafting/crystal_heart.json`, where Pith is a renamed turtle scute).
- Zephyr, a boots enchantment that gives a double jump, comes from Matcha Flavoured by Klei (`data/matcha/enchantment/zephyr.json`). Maple keeps the name and the double jump, and its own code adds the per-level heights and the Clutch. Matcha's sneak jump and slow falling are left out.
- Village spacing 80 and separation 50, which makes villages about 5.5 times rarer, comes from Matcha Flavoured by Klei (`dp/data/minecraft/worldgen/structure_set/villages.json`), at commit `71c6e65`.
- Dungeon frequency, 15 placement attempts per chunk above Y 0 and 8 below, comes from Matcha Flavoured by Klei (`dp/data/minecraft/worldgen/placed_feature/monster_room.json` and `monster_room_deep.json`).
- The common mob numbers come from Matcha Flavoured by Klei (`dp/data/matcha/function/mechanics/spawn_mechanic/easy_modifications/`, `normal_modifications/`, `normal_and_easy_modifications/` and `hard_modifications/`), at commit `71c6e65`. Maple's husks keep vanilla step height and movement efficiency, its Hard husk deals 8 damage instead of 10, and its spiders get no Weaving. Keeping creepers from spawning at Y 63 or above in the Overworld comes from the same mod's `dp/data/matcha/predicate/surface_spawn.json`, without its sky check.
- Using a Crystal Heart plays the Totem of Undying's activation and gives Regeneration XI for 3 seconds and Resistance V for 1 second, as Matcha Flavoured's `mechanics/heart_container/use_crystal_heart.mcfunction` does.

## Reused assets

Each reused asset is listed with its original path, its author, and whether it was changed, as CC BY-NC-SA 4.0 requires.

- `assets/maple_flavored/textures/font/broken_heart.png`: the broken heart glyph (`\uE010`) cut from Matcha Flavoured's `MF_resourcepack/assets/minecraft/textures/font/custom_emojis.png` by Klei, at commit `0f196bd`. Changed only by cropping it to its own 8 × 8 image.
- `assets/maple_flavored/textures/item/crystal_heart.png`: Matcha Flavoured's `assets/matcha/textures/item/heart_container.png` by Klei, from the 1.12.8-beta mod jar. Unchanged.
- `assets/maple_flavored/textures/item/pith.png`: Matcha Flavoured's Pith, `assets/minecraft/textures/item/turtle_scute.png` by Klei, from the 1.12.8-beta mod jar. Unchanged.
- `assets/maple_flavored/textures/item/diamond_shard.png`: Minecraft's `assets/minecraft/textures/item/amethyst_shard.png` by Mojang Studios, recoloured with the palette of Minecraft's `assets/minecraft/textures/item/diamond.png`, matched shade for shade by brightness.

- `assets/maple_flavored/textures/environment/subspace_shell.png`: Minecraft's `assets/minecraft/textures/environment/end_sky.png` by Mojang Studios, recoloured sculk teal by Jackson Yeager.
- The Alloy textures are all Matcha Flavoured's Netherite art by Klei, at commit `71c6e65`, which Maple uses as Electrum. For Steel, Shakudo, Adamant and Orichalcum, Jackson Yeager recoloured only the gold trim: silver for Steel, rose copper for Shakudo, diamond blue for Adamant (from Minecraft's `assets/minecraft/textures/item/diamond.png` by Mojang Studios) and emerald green for Orichalcum (from Minecraft's `assets/minecraft/textures/item/emerald.png`). The sources, under `assets/maple_flavored/` and with `{alloy}` standing for each of `steel`, `electrum`, `shakudo`, `adamant` and `orichalcum`, are:
  - `textures/item/{alloy}_ingot.png`: `MF_resourcepack/assets/minecraft/textures/item/netherite_ingot.png`.
  - `textures/block/{alloy}_block.png`: `MF_resourcepack/assets/minecraft/textures/block/netherite_block.png`.
  - `textures/item/{alloy}_{sword,shovel,pickaxe,axe,hoe,spear,spear_in_hand,helmet,chestplate,leggings,boots}.png`: `MF_resourcepack/assets/minecraft/textures/item/netherite_*.png` of the same names.
  - `textures/entity/equipment/{humanoid,humanoid_baby}/{alloy}.png`: `MF_resourcepack/assets/minecraft/textures/entity/equipment/{humanoid,humanoid_baby}/netherite.png`; `textures/entity/equipment/humanoid_leggings/{alloy}.png`: `MF_resourcepack/assets/matcha/textures/entity/equipment/humanoid_leggings/netherite.png`.
- `assets/maple_flavored/textures/item/{alloy}_horse_armor.png` and `textures/entity/equipment/horse_body/{alloy}.png`: Matcha Flavoured's Electrum horse armor by Klei, at commit `71c6e65` (`MF_resourcepack/assets/matcha/textures/item/electrum_horse_armor.png` and `MF_resourcepack/assets/matcha/textures/entity/equipment/horse_body/electrum.png`), recoloured to Matcha's Netherite greys and gold by Jackson Yeager, with the trim then recoloured for each Alloy as above.
- `assets/maple_flavored/textures/item/{alloy}_nautilus_armor.png` and `textures/entity/equipment/nautilus_body/{alloy}.png`: Minecraft's `assets/minecraft/textures/item/netherite_nautilus_armor.png` and `textures/entity/equipment/nautilus_body/netherite.png` by Mojang Studios, recoloured to Matcha's Netherite greys and gold by Jackson Yeager, with the trim then recoloured for each Alloy as above.
- `assets/maple_flavored/textures/item/{alloy}_armored_elytra.png`, `{alloy}_armored_elytra_broken.png` and `textures/entity/equipment/wings/{alloy}.png`: Minecraft's `assets/minecraft/textures/item/elytra.png`, `elytra_broken.png` and `textures/entity/equipment/wings/elytra.png` by Mojang Studios, recoloured by Jackson Yeager: the greys to the Alloy chestplates' Netherite greys above and the lavender to each Alloy's trim colours.
- `assets/maple_flavored/textures/entity/equipment/*/alloy_trim_mask.png`: the trim of each Electrum layer above, cut out as a plain white mask by Jackson Yeager.
- `assets/maple_flavored/textures/item/reinforcement_smithing_template.png`: Minecraft's `assets/minecraft/textures/item/netherite_upgrade_smithing_template.png` by Mojang Studios, recoloured to chainmail greys.
- `assets/maple_flavored/textures/entity/equipment/{humanoid,humanoid_leggings}/reinforced.png`: Minecraft's `assets/minecraft/textures/entity/equipment/{humanoid,humanoid_leggings}/chainmail.png` by Mojang Studios. Unchanged.
- `data/minecraft/structure/igloo/bottom.nbt`: Minecraft's igloo basement template by Mojang Studios, with its villager made a zombie villager by Jackson Yeager. Nothing else is changed.
- `assets/maple_flavored/textures/item/ancient_ember.png`: Minecraft's `assets/minecraft/textures/item/fire_charge.png` by Mojang Studios, recoloured by Jackson Yeager.
- `assets/maple_flavored/textures/block/alloy_furnace_{front,front_on,side,top}.png` and `alloy_furnace_front_on.png.mcmeta`: Minecraft's `assets/minecraft/textures/block/blast_furnace_{front,front_on,side,top}.png` and its animation file by Mojang Studios, recoloured to blackstone and gold by Jackson Yeager.
- `assets/maple_flavored/textures/gui/container/alloy_furnace.png`: Minecraft's `assets/minecraft/textures/gui/container/furnace.png` by Mojang Studios, with its input slot doubled and its flame and fuel slot moved by Jackson Yeager.
- `assets/maple_flavored/textures/entity/deep_zombie.png` and `deep_zombie_baby.png`: Minecraft's `assets/minecraft/textures/entity/zombie/zombie.png` and `zombie_baby.png` by Mojang Studios, with the head and arms reworked as deepslate by Jackson Yeager.
- `assets/maple_flavored/textures/item/deep_zombie_spawn_egg.png`: Minecraft's `assets/minecraft/textures/item/zombie_spawn_egg.png` by Mojang Studios, with its greens recoloured to the greys of Minecraft's `assets/minecraft/textures/block/deepslate.png`.
- `assets/maple_flavored/textures/entity/deep_skeleton.png`, `deep_creeper.png` and `deep_spider.png`: Minecraft's `assets/minecraft/textures/entity/skeleton/skeleton.png`, `creeper/creeper.png` and `spider/spider.png` by Mojang Studios, reworked as deepslate by Jackson Yeager.
- `assets/maple_flavored/textures/entity/deep_spider_eyes.png`: Minecraft's `assets/minecraft/textures/entity/spider/spider_eyes.png` by Mojang Studios, with its red recoloured to the teal of Jackson Yeager's `deep_spider.png`.
- `assets/maple_flavored/textures/item/deep_skeleton_spawn_egg.png`, `deep_creeper_spawn_egg.png` and `deep_spider_spawn_egg.png`: Minecraft's `assets/minecraft/textures/item/skeleton_spawn_egg.png`, `creeper_spawn_egg.png` and `spider_spawn_egg.png` by Mojang Studios, recoloured to the greys of Minecraft's `assets/minecraft/textures/block/deepslate.png`, with the spider's eyes made teal.

Matcha's own credits name other authors for some assets (for example the 3D food models, fish models and music). Credit those authors rather than Klei.
