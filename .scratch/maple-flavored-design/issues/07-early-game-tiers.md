# Early-game tiers

Type: grilling
Blocked by: 06
Status: resolved

## Question

What does the early game add to vanilla's progression, if anything, that still matters past the first hour?

## Answer

The early game gets no new steps of its own. It loses stone tools, gives gold gear a lasting niche, and adds Reinforcement to carry armor through the long stretch before Protection. Terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change. The rules are fixed.

### No early-only steps

- No new workstations, heat sources or smelting gates. The furnace, smoker, blast furnace and campfire stay vanilla.
- Rule 4 judges what we add. Vanilla gear can be outgrown.
- Below Netherite, the gear ladder can be strict, so diamond simply beats iron. "No gear strictly best" applies between Netherite and the Alloys. Every material still keeps a use.
- There's no compensation for the stretch before enchanting. The early game is meant to be unenchanted, dying costs hearts rather than gear, and Reinforcement fills the gap later.

### Stone tools are removed

- The ladder is wood, copper, iron, diamond, Netherite, then Alloys.
- A wooden pickaxe mines copper ore, deepslate copper ore and raw copper blocks. Everything else vanilla gates at stone tier needs copper or better, as copper tools already count as stone tier.
- Stone tool recipes are removed, including the stone spear. Wherever vanilla hands out a stone tool (trades, chest loot, the Bonus Chest), it gives the copper version instead. The "Getting an Upgrade" advancement asks for a copper pickaxe.
- Wooden and copper tool stats stay vanilla. Copper already beats stone slightly on durability and speed.
- A vanilla tier is cut only when another tier fills its exact slot and it has no niche of its own. Stone tools are the only cut: leather armor, chainmail and wooden tools stay.

### Gold

- Every enchantable gold item, meaning all gold tools, weapons and armor, gets 1 extra Enchantment Slot. This carries vanilla's high enchantability into the new enchanting system. Golden horse armor can't be enchanted, so it's unaffected.
- Following from this, Electrum and Orichalcum swap two perks, so each Alloy takes on its storage block's vanilla identity. Electrum tools and armor get the extra Enchantment Slot, and Orichalcum tools get the +50% drop bonus. [Material roles and alloys](06-material-roles-and-alloys.md) and [Enchanting](10-enchanting.md) are updated to match.

### Reinforcement

Reinforced armor holds you over until you find Protection, which takes a long time.

- At a Smithing Table, 1 Reinforcement template + 1 armor piece + 1 chainmail piece of any slot gives the Reinforced piece, with +1 armor toughness. The template and chainmail are used up. A piece can be Reinforced once.
- It works on every armor piece, including the turtle helmet and the Armored Elytra. Things that are only worn don't qualify: plain Elytra, carved pumpkins, heads and mob armor.
- It stacks with Adamant, so Reinforced Adamant has 5 toughness per piece against Reinforced Netherite's 4.
- It carries through smithing upgrades the way trims do, from diamond to Netherite to an Alloy and between Alloys. It can be applied to enchanted armor, and Reinforced armor can be enchanted. This is an exception to "enchanted gear can't be smithed", which is about changing material.
- It shows as a "Reinforced" tooltip line and a chainmail overlay drawn like a trim layer, so it combines with real trims.
- The template is a Master Armorer trade for 32 emeralds, and appears in no chest loot. Like vanilla templates, it duplicates: 7 diamonds + 1 template + 1 block of iron makes 2.
- Chainmail stays uncraftable. It comes from mob drops, from chests, and from vanilla's Armorer trades.

### Effects on other tickets

- [Getting villagers and removing villages](08-villagers-and-villages.md): Armorers must stay reachable, as they gate Reinforcement.
