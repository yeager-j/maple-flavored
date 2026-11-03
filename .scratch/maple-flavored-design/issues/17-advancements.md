# Advancements

Type: grilling
Status: resolved

## Question

Which advancements do we add or change so that they guide players through Maple Flavored's progression, as Matcha Flavoured's do? Cover new systems such as Alloys, Crystal Hearts, Cleared spawners, the Library, earning villagers and Blood Moons; vanilla advancements that no longer fit, such as ones needing stone tools or enchanted books from chests; whether the seven Milestones that lower the Heart Floor appear as advancements; and how hidden or challenge advancements reward players.

## Answer

Advancements guide players through Maple Flavored inside vanilla's own tabs. Each new advancement teaches the first step of a system or caps it, and the seven Milestones show the way to a lower Heart Floor. Terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change. Research: [Matcha Flavoured's advancements](https://github.com/kleiwright/matcha-flavoured/tree/26.3/MF_datapack/data/matcha/advancement) were read at commit `71c6e65` (1.12.9), and vanilla's 126 advancements were checked against misode/mcmeta's `26.3-data` tag.

### Rules

- Vanilla's five tabs stay. Each new advancement sits in the tab and branch where its step happens, rather than in a separate guide tab as Matcha does.
- Each system gets one advancement for the first step a player might otherwise miss, plus at most one goal or challenge for mastering it. There are no new collect-everything lists, and vanilla's stay as they are.
- Descriptions teach the mechanic in plain words, without lore. Exact titles and descriptions are written at build time. Titles may be vanilla-style puns, except **But why?**, which is fixed.
- No new advancement is hidden except **But why?**.
- Rewards:
  - Crystal Hearts come only from the four boss advancements.
  - New challenges give 100 XP, like vanilla's.
  - No advancement gives items or Pith.

### Milestones

- The seven Milestones are advancements:
  - four from vanilla: Acquire Hardware (iron ingot), Diamonds!, We Need to Go Deeper and The End?
  - three new ones: copper ingot, Netherite Ingot and Eye of Ender
- The Heart Floor is calculated as 10 minus the number of Milestones the player holds, with a minimum of 3, on Normal and Hard. Revoking a Milestone raises the floor again, and nothing counts twice.
- Each Milestone's description ends with "Milestone: lowers your Heart Floor by 1 on Normal and Hard", including the vanilla four.
- The plain chat message from [Hearts and death](03-hearts-and-death.md) still announces the new floor alongside the advancement message.

### New advancements

| Tab | Advancement | Parent | Frame | Reward |
| --- | --- | --- | --- | --- |
| Story | Get a copper ingot (Milestone). Getting an Upgrade moves under it. | Stone Age | task | |
| Story | Shelve an enchanted book in a Library. Enchanter moves under it. | Diamonds! | task | |
| Story | Full Library | Enchanter | goal | |
| Story | Make an enchanted golden apple. Zombie Doctor moves under it. | Enchanter | task | |
| Story | Reinforce an armor piece | Zombie Doctor | task | |
| Story | Get an Eye of Ender (Milestone). Eye Spy moves under it. | We Need to Go Deeper | task | |
| Nether | Get a Netherite Ingot (Milestone). Cover Me in Debris moves under it. | Hidden in the Depths | task | |
| Nether | Get an Ancient Ember | Those Were the Days | task | |
| Nether | Smelt an Alloy ingot | Get an Ancient Ember | task | |
| Nether | Wear a full set of each Alloy, at any time | Smelt an Alloy ingot | challenge | 100 XP |
| Nether | Kill the Wither | Withering Heights | goal | Crystal Heart |
| Nether | Get a Withered Heart back | A Terrible Fortress | task | |
| Nether | **But why?** (hidden) | Beaconator | challenge | 100 XP |
| End | Get an Armored Elytra | Sky's the Limit | goal | |
| Adventure | Break a Cleared spawner | Monster Hunter | task | |
| Adventure | Consume a Crystal Heart | Break a Cleared spawner | task | |
| Adventure | Be saved by a Crystal Heart | Consume a Crystal Heart | task | |
| Adventure | Reach 30 Max Hearts | Consume a Crystal Heart | challenge | 100 XP |
| Adventure | Kill an Elder Guardian | Monster Hunter | goal | Crystal Heart |
| Adventure | Survive the Warden | Sneak 100 | goal | Crystal Heart |
| Adventure | Collect your own Remnant | Adventure | task | |
| Adventure | Be in the Overworld when a Blood Moon rises | Adventure | task | |

Husbandry gains nothing. Zephyr's Clutch, finding an Abandoned Village, building an Alloy Furnace and Grounded have no advancement of their own; Zombie Doctor, the first Alloy and the Warden cover the last three.

Criteria details:

- **Break a Cleared spawner** goes to the player who breaks it, as they get its Pith.
- **But why?**: a level 4 beacon whose whole pyramid is Alloy blocks, in any mix. At 164 blocks, or 1,476 Ancient Embers, almost nobody will earn it, and that's the joke.
- **Survive the Warden**: every player inside the Warden's Subspace when it digs back down, or when it's killed. There's no other condition, so a player who stays quietly inside the Subspace until it leaves has survived it.
- **Boss credit**: Kill the Wither goes to every player inside its Subspace. Kill an Elder Guardian goes to every player within 64 blocks. Free the End uses vanilla's dragon-fight player range.

### Vanilla advancements

- **Getting an Upgrade** asks for a copper pickaxe, as [Early-game tiers](07-early-game-tiers.md) decided.
- **Stone Age** keeps its criterion, so the `#stone_tool_materials` tag must survive the removal of stone tools.
- **Free the End** also rewards a Crystal Heart.
- **Postmortal** still counts only a Totem of Undying. Crystal Heart saves have their own advancement.
- **Enchanter** still fires on vanilla's `enchanted_item` trigger, so the reworked table must keep firing it.
- Every other vanilla advancement keeps its criteria and position, including ones our systems now delay:
  - curing: What a Deal!, Star Trader, Surge Protector, Very Very Frightening, Hero of the Village and A Complete Catalogue
  - the Library: Arbalistic, Two Birds One Arrow, Total Beelocation and How Did We Get Here?
  - Pith: Cover Me in Debris and Serious Dedication
- **Subspace Bubble** and **Respecting the Remnants** keep their titles. The glossary notes that they're unrelated to Subspace and Remnant.

### Changes to other tickets

- [Hearts and death](03-hearts-and-death.md): the Warden's Crystal Heart is earned by surviving it, not by killing it.
- [Enchanting](10-enchanting.md): a golden apple saved at the table with effects becomes the vanilla `enchanted_golden_apple` item, and the grindstone turns it back into a `golden_apple`. A Balanced Diet checks that item id.
