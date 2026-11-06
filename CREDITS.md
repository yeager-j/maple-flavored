# Credits

Maple Flavored is inspired by these projects. Neither author is involved in or endorses this mod.

- [Matcha Flavoured](https://modrinth.com/datapack/matcha-flavoured) by Klei ([kleiwright](https://github.com/kleiwright/matcha-flavoured)), licensed under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).
- [Raspberry Flavoured](https://www.curseforge.com/minecraft/modpacks/raspberry-flavoured) by Raspmary.

## Reused designs

- The Crystal Heart recipe (8 diamonds around 1 Pith) and the Nether Star recipe (1 Nether Star into 9 Pith) come from Matcha Flavoured by Klei (`dp/data/matcha/recipe/crafting/crystal_heart.json`, where Pith is a renamed turtle scute).
- Zephyr, a boots enchantment that gives a double jump, comes from Matcha Flavoured by Klei (`data/matcha/enchantment/zephyr.json`). Maple keeps the name and the double jump, and its own code adds the per-level heights and the Clutch. Matcha's sneak jump and slow falling are left out.
- Dungeon frequency, 15 placement attempts per chunk above Y 0 and 8 below, comes from Matcha Flavoured by Klei (`dp/data/minecraft/worldgen/placed_feature/monster_room.json` and `monster_room_deep.json`).
- The common mob numbers come from Matcha Flavoured by Klei (`dp/data/matcha/function/mechanics/spawn_mechanic/easy_modifications/`, `normal_modifications/`, `normal_and_easy_modifications/` and `hard_modifications/`), at commit `71c6e65`. Maple's husks keep vanilla step height and movement efficiency, its Hard husk deals 8 damage instead of 10, and its spiders get no Weaving. Keeping creepers from spawning at Y 63 or above in the Overworld comes from the same mod's `dp/data/matcha/predicate/surface_spawn.json`, without its sky check.
- Using a Crystal Heart plays the Totem of Undying's activation and gives Regeneration XI for 3 seconds and Resistance V for 1 second, as Matcha Flavoured's `mechanics/heart_container/use_crystal_heart.mcfunction` does.

## Reused assets

Each reused asset is listed with its original path, its author, and whether it was changed, as CC BY-NC-SA 4.0 requires.

- `assets/maple_flavored/textures/font/broken_heart.png`: the broken heart glyph (`\uE010`) cut from Matcha Flavoured's `MF_resourcepack/assets/minecraft/textures/font/custom_emojis.png` by Klei, at commit `0f196bd`. Changed only by cropping it to its own 8 × 8 image.
- `assets/maple_flavored/textures/item/crystal_heart.png`: Matcha Flavoured's `assets/matcha/textures/item/heart_container.png` by Klei, from the 1.12.8-beta mod jar. Unchanged.
- `assets/maple_flavored/textures/item/pith.png`: Matcha Flavoured's Pith, `assets/minecraft/textures/item/turtle_scute.png` by Klei, from the 1.12.8-beta mod jar. Unchanged.

- `assets/maple_flavored/textures/environment/subspace_shell.png`: Minecraft's `assets/minecraft/textures/environment/end_sky.png` by Mojang Studios, recoloured sculk teal by Jackson Yeager.
- `assets/maple_flavored/textures/item/{steel,electrum,shakudo,adamant,orichalcum}_ingot.png`: Minecraft's `assets/minecraft/textures/item/netherite_ingot.png` by Mojang Studios, recoloured for each Alloy by Jackson Yeager.
- `assets/maple_flavored/textures/block/{steel,electrum,shakudo,adamant,orichalcum}_block.png`: Minecraft's `assets/minecraft/textures/block/netherite_block.png` by Mojang Studios, recoloured for each Alloy by Jackson Yeager.
- `assets/maple_flavored/textures/item/ancient_ember.png`: Minecraft's `assets/minecraft/textures/item/fire_charge.png` by Mojang Studios, recoloured by Jackson Yeager.
- `assets/maple_flavored/textures/block/alloy_furnace_{front,front_on,side,top}.png` and `alloy_furnace_front_on.png.mcmeta`: Minecraft's `assets/minecraft/textures/block/blast_furnace_{front,front_on,side,top}.png` and its animation file by Mojang Studios, recoloured to blackstone and gold by Jackson Yeager.
- `assets/maple_flavored/textures/gui/container/alloy_furnace.png`: Minecraft's `assets/minecraft/textures/gui/container/furnace.png` by Mojang Studios, with its input slot doubled and its flame and fuel slot moved by Jackson Yeager.

Matcha's own credits name other authors for some assets (for example the 3D food models, fish models and music). Credit those authors rather than Klei.
