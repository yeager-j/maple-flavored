# Material roles and alloys

Type: grilling
Status: resolved
Blocked by: 02

## Question

What role does each material play, and what alloy system makes those roles distinct from Matcha's, so that netherite isn't strictly best and no material ends up as dead weight?

## Answer

Numbers are starting values that balance work can change. The rules are fixed. Terms are in `GLOSSARY.md`. Research: [Raspberry Flavoured](../../../docs/research/raspberry-alloys.md) and [BetterEnd's Armored Elytra](../../../docs/research/betterend-armored-elytra.md).

### Netherite and Pith

- A Netherite Ingot is 4 Netherite Scrap + 4 Pith. Base Netherite gear is otherwise unchanged.
- Iron, gold, copper and diamond gear stay vanilla. Their new role is as Alloy ingredients. Early-game changes belong to Early-game tiers.
- Pith drops: a broken spawner always gives 1, an Elder Guardian always gives 2, an Evoker gives 1 at 25%, and a Piglin Brute gives 1 at 10%.

### Making Alloys

- The Alloy Furnace smelts 1 Netherite Ingot + 1 storage block + 1 Ancient Ember into 1 Alloy ingot in 10 seconds. It has a normal crafting recipe that needs no Ember (set in [Trading, combat and recipes](16-trading-combat-and-recipes.md)), works with hoppers, and has no other use.
- Steel uses a block of iron, Electrum gold, Shakudo copper, Adamant diamond and Orichalcum emerald.
- Piglin Brutes drop an Ancient Ember at 25%, plus 5% per Looting level, using vanilla's killed-by-player condition. It rolls separately from Pith.
- Brutes only return with a [Blood Moon](15-blood-moons.md) and Embers need a player kill, so Alloy ingots can't be automated, though their inputs can.
- An Alloy ingot smelts back to a Netherite Ingot in any furnace, losing the block and Ember.

### Alloy gear

- Netherite gear + 1 Alloy ingot at a Smithing Table gives the Alloy version, with no template. This covers exactly the gear that has a Netherite version.
- Alloy gear can be re-smithed with a different ingot, losing the old Alloy. It can't be salvaged.
- Each Alloy beats Netherite in its own way and trades off against the others.

| Alloy | Tools and weapons | Armor, per piece | Full set bonus |
|---|---|---|---|
| **Steel** | +50% durability, more weapon knockback | +50% durability, more knockback resistance, 10% less sonic boom and explosion damage | Complete knockback and Levitation immunity |
| **Electrum** | +1 Enchantment Slot | +1 Enchantment Slot, 15% less fire damage; Piglins treat it as gold | Complete fire and lava immunity; a Withered Heart takes 10 seconds instead of 5 |
| **Shakudo** | +30% mining speed, +10% attack speed | +2.5% movement speed | No slowdown from soul sand, honey, cobwebs or powder snow |
| **Adamant** | +1 damage; mines hard blocks as if they were deepslate; the pickaxe can break bedrock | +1 armor toughness | 25% of melee damage ignores armor points, but not Protection |
| **Orichalcum** | +50% to drops that Fortune or Looting affect | 10% less magic and indirect magic damage; Illagers are neutral | A lethal hit is survived with Totem effects, then this recharges over 30 minutes of play |

The drop bonus is added to Fortune and Looting rather than multiplied, on only the drops they affect, skipping Ancient Embers, Pith and enchanted books.

Shakudo's mining bonus multiplies the final break speed, after Efficiency, not the tool's base speed. At +30%, a Shakudo pickaxe with Efficiency V reaches a speed of 45.5, enough to insta-mine stone (hardness 1.5) with no Haste. A Netherite one reaches 35. At +25%, Shakudo would reach 43.75, just short.

Adamant's hardness cap: an Adamant tool mines any block in its own mineable tag as if the block's hardness were at most 3, deepslate's. Blocks outside the tool's tag, such as trial spawners, vaults and reinforced deepslate, keep their normal times, as do unbreakable blocks. Stone and deepslate mine at Netherite speed, so Shakudo stays the everyday miner and Adamant breaks through obsidian, crying obsidian, ancient debris, ender chests and Alloy blocks.

Breaking bedrock: an Adamant pickaxe with Efficiency V or higher, used by a player with Haste II or higher, breaks bedrock in a flat 15 seconds. Higher levels don't make it faster. Bedrock drops nothing, as in vanilla. Any bedrock counts, including the Overworld's bottom layer, the Nether roof and the End's. Breaking the bottom layer opens the void, and that risk is the player's. With no Haste potion in vanilla, Haste II in practice means a full beacon within range.

The Shakudo mining bonus, the hardness cap and the bedrock time are playtest game rules in `MapleGameRules`.

Illager neutrality needs only 1 piece, like gold with Piglins, and ends during Raids or once you attack one. The set bonus timer is stored on the player, fires before a held Totem or Crystal Heart, and is disabled in Hardcore.

Vanilla facts behind these: Evoker Fangs deal 6 indirect magic damage, which ignores armor but not Protection, so Orichalcum reduces it. A Warden's sonic boom deals 10 damage and ignores both, so Steel is the only gear that reduces it apart from Resistance.

### Armored Elytra

- An Elytra + an Alloy chestplate at a Smithing Table gives an Armored Elytra, permanently, with that chestplate's armor and effects. Plain Netherite chestplates don't qualify. It counts as the chest piece for set bonuses.
- It flies worse than a plain Elytra: horizontal glide drag is multiplied by 0.995 per tick, and Firework Rocket boost is reduced by 25%.

## Comments

From [the enchanting manifesto](../../../docs/manifestos/enchanting.md): Orichalcum's Better enchanting becomes 1 extra enchantment slot on tools and armor. Electrum's Fortune or Looting one level above the cap becomes a flat +50% drop bonus, added to Fortune and Looting, on only the drops they affect, skipping Ancient Embers, Pith and enchanted books. Alloy gear is repaired with 1 ingot of its own Alloy.

From [Early-game tiers](07-early-game-tiers.md): Electrum and Orichalcum swap two perks, so each Alloy takes on its storage block's vanilla identity. Gold is vanilla's enchantable metal, and gold gear now has 1 extra Enchantment Slot, so Electrum tools and armor carry the extra slot. Emerald is the wealth metal, so Orichalcum tools carry the +50% drop bonus. The table above shows the swap; set bonuses are unchanged.

Jackson's change on 2026-11-06: Adamant tools had no purpose beyond +1 damage, which only weapons use. They gained the hardness cap and bedrock breaking. Shakudo's mining bonus rose from +25% to +30%: at +25% it was rounded away on stone and deepslate, since an Efficiency V pick broke both in the same whole number of ticks as Netherite. Bedrock breaking gets the [advancement](17-advancements.md) Breaking the Rules.
