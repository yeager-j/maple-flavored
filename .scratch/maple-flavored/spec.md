# Maple Flavored

Status: ready-for-agent

## Problem Statement

Vanilla Minecraft gives a player strong gear fast, and then most of the world stops mattering. A village found on day 1 gives an iron farm, villager trades and enchanted books. Enchanting is a loot box: you spend levels on a random roll, and there is always one best choice. A player has no reason to visit most structures after their first diamond set. Netherite is strictly the best material, so iron, gold, copper, emerald and diamond end up as dead weight in storage. Death costs your whole inventory, which can make a player quit a save, or costs nothing at all with Keep Inventory on.

Matcha Flavoured fixes some of this, but it is built for players who don't build farms. It discourages farms, renames vanilla things for lore, gives early-game steps that are forgotten after an hour, changes the world permanently after a boss kill, and removes XP as a mechanic. A technical player who loves farms has no overhaul that slows progress down while keeping farms worthwhile and the sandbox intact.

## Solution

Maple Flavored is a Fabric mod for Minecraft 26.3 that starts from vanilla and adds each change on purpose. It is required on the client and server, works on new worlds, and stands alone with no required content mods.

- By default, death costs Max Hearts and XP, never items. Three game rules (Keep Hearts, Keep XP, Keep Inventory) let a world change each cost on its own. A Heart Floor that falls with Milestones protects early saves. Crystal Hearts restore and raise Max Hearts. XP drops as a Remnant that only its owner can collect.
- Enchanting becomes exploration. Each structure holds one guaranteed level I book. Books combine in the anvil and are shared through a Library around the enchanting table. The table is a predictable checklist. A player's level sets their Enchantment Slots.
- Villagers are earned by curing, which needs the Nether, a Library and level 30. Every village is an Abandoned Village. Farms stay possible, just not on day 1.
- Netherite needs Pith. Five Alloys improve on Netherite in different ways, so no gear is strictly best and every storage block has an endgame use.
- Spawners must be Cleared before they break. Blood Moons Restore structure mobs, so rare materials renew slowly without changing the world for good.
- Bosses open Subspaces that close their cheapest escape. Common mobs are faster and more fragile.
- New advancements in vanilla's tabs teach every system.

Every rule below is fixed. Every number is a starting value that playtesting can change.

## User Stories

### Hearts and death

1. As a player, I want death to cost Max Hearts instead of my items, so that dying hurts without making me want to quit the save.
2. As a world creator, I want three game rules, Keep Hearts, Keep XP and Keep Inventory, defaulting to off, off and on, so that each death cost can be switched on its own while new worlds play as designed.
3. As a player, I want to start with 10 Max Hearts, so that the first night feels like vanilla.
4. As a player, I want a Heart Floor that starts at 10, so that early deaths cannot ruin my save.
5. As a player on Normal or Hard, I want each Milestone I hold to lower my Heart Floor by 1, down to 3, so that the cost of death grows with my progress.
6. As a player, I want the item Milestones (copper ingot, iron ingot, diamond, Netherite Ingot, Eye of Ender) to count as soon as the item is in my inventory, however I got it, so that crafting, looting and trading all count the same.
7. As a player, I want entering the Nether and entering the End to be Milestones, so that dangerous trips count as progress.
8. As a player, I want Milestones to count in any order, so that I can progress my own way.
9. As a player, I want a lower Heart Floor to remove no hearts by itself, so that progress never punishes me directly.
10. As a player, I want a chat message when my Heart Floor drops, so that I know what my next death can cost.
11. As a player, I want the death screen to show the Max Hearts I lost, so that the cost of each death is clear.
12. As a player on Peaceful or Easy, I want to lose 1 heart per death with my Heart Floor fixed at 10, so that the gentle difficulties stay gentle.
13. As a player on Hard, I want to lose 2 hearts per death, so that Hard has higher stakes than Normal.
14. As a Hardcore player, I want vanilla's death rule, so that Hardcore keeps its meaning.
15. As a player, I want each Milestone advancement marked with a broken heart, so that I can see which steps raise the stakes.
16. As a player, I want a Crystal Heart to add 1 Max Heart when I consume it, up to 30, so that I can recover lost hearts and grow past 10.
17. As a player at 30 Max Hearts, I want a Crystal Heart to be refused without being used up, so that I never waste one.
18. As a player, I want a Crystal Heart in either hand to save me from a lethal hit like a Totem of Undying, so that I choose between more health and a safety net.
19. As a player, I want a Crystal Heart or Totem save not to count as a death, so that it costs me no hearts and no XP.
20. As a player, I want to craft a Crystal Heart from 8 diamonds around 1 Pith, so that mining and dangerous fights both feed my health.
21. As a player, I want a Crystal Heart for my first Elder Guardian kill, my first Wither kill and Free the End, so that each boss rewards my health once.
22. As a player, I want a Crystal Heart for surviving a Warden until it digs back down or dies, so that sneaking past it is a real win.
23. As a member of a group, I want every nearby player to get boss credit, so that we can fight bosses together.
24. As a player, I want Pith from broken spawners, Elder Guardians, Evokers and Piglin Brutes, so that dangerous fights feed both health and Netherite.
25. As a player, I want a Nether Star to craft into 9 Pith, so that a Wither kill feeds my progress.
26. As a farm builder, I want Evoker and Wither farms to give Pith, so that my farms are rewarded while diamonds still limit Crystal Hearts.
27. As a player under the Wither effect, I want one heart to become a Withered Heart every 5 seconds, down to 1 usable heart, so that the Wither effect is a real threat.
28. As a player, I want all Withered Hearts to return when I am outside the Nether without the Wither effect, so that I recover by leaving.
29. As a player in the Nether, I want Withered Hearts to stay until I leave, so that the Nether stays dangerous.
30. As a player inside a Wither's Subspace, I want Withered Hearts never to return, in any dimension, so that the Wither fight can't be cheesed by recovering on the spot.
31. As a player, I want Withered Hearts to show as darkened hearts and to survive logging out, so that I can't clear them by reconnecting.
32. As a player, I want death to clear Withered Hearts, so that I respawn with all my usable hearts.
33. As a player, I want Withered Hearts not to change my Max Hearts, so that death and Crystal Hearts work as normal.

### Remnants and XP

34. As a player, I want all my XP to drop as a Remnant when I die, so that a large XP reserve gives a dangerous trip real stakes.
35. As a player, I want only me to see and collect my Remnant, so that no one can steal it or use it to pass XP to a friend.
36. As a player, I want my Remnant never to despawn or be destroyed, even while I am offline or its chunk is unloaded, so that I can always go back for it.
37. As a player, I want a Remnant to return all its points at once when I walk over it, so that Mending can't eat it.
38. As a player who dies in the void, in lava or anywhere a Remnant can't be collected, I want it to appear at my last solid ground, even inside a Warden Subspace, so that I can still recover it.
39. As a player who dies inside a Warden's Subspace, I want my Remnant to stay where I died, so that I choose when to go back.
40. As a player, I want the Recovery Compass to point to my latest Remnant, even when it was moved to safe ground, so that I can always find it.
41. As a player, I want each further death to cut every uncollected Remnant by 20% of its current points, even when I die with no XP, so that going back soon matters.
42. As a player, I want to hold any number of Remnants, so that a second death doesn't erase the first.
43. As a player, I want my level to set my Enchantment Slots, 1 at level 30 and 1 more every 30 levels, with no cap, so that XP stays useful for the whole save.
44. As a player, I want repairs to cost no XP, so that keeping my gear doesn't drain my level.
45. As a player, I want combining books to cost 50 points × the resulting level, so that combining is a real cost.
46. As a player, I want renaming to cost a flat 10 points, so that naming things stays cheap.
47. As a player, I want anvil costs in points, not levels, so that the same job costs every player the same.
48. As a player, I want no prior-work penalty, so that old gear never becomes too expensive to work on.
49. As a farm builder, I want XP farms to stay worthwhile, so that my farm keeps a purpose all save.
50. As a player, I want the grindstone to give no XP, so that saving and grinding can't turn lapis into XP.

### Finding books

51. As an explorer, I want each structure to hold one guaranteed level I book in a fixed, visible spot, so that finding the structure is the reward, not a chest roll.
52. As an explorer, I want a structure with several possible enchantments to pick one when it generates, so that each structure has one known prize.
53. As a member of a group, I want one book per structure per world, so that a server doesn't multiply finds.
54. As an explorer, I want an Ancient City's book locked until three keeper shriekers in a ring near the city's edge are broken, and the city to keep me Grounded until the book is taken, so that the prize takes stealth rather than a flight, and a Warden comes only when I'm loud.
55. As an explorer, I want a Bastion's book locked until its keeper Brutes are dead, and taking it to set every Piglin in it hunting me, so that the prize is a fight.
56. As an explorer, I want taking a Stronghold's or Woodland Mansion's book to alert its Illagers and turn them hostile, even against Orichalcum, so that the book is never free.
57. As an explorer, I want a Nether Fortress's book to stay visibly locked until every Blaze spawner that generated there is Cleared, so that I earn it while the spawners can stay for a farm.
58. As an explorer, I want every Nether Fortress to generate with a Blaze spawner, and a fortress that somehow has none to have its book unlocked from the start, so that I am never blocked for good.
59. As an explorer, I want to clear a structure before taking its book, so that careful play is a valid approach.
60. As a player, I want to combine two matching books of the same level into the next level, so that many finds build up to strong enchantments.
61. As a player, I want specialized enchantments (Smite, Bane of Arthropods, Blast and Projectile Protection) to come from early structures, so that a narrow enchantment is the best I have for a while.
62. As a player, I want Strongholds full of Illagers, so that the path to the End Portal is a fight and Illager books have a home outside raids.
63. As a player, I want Vindicators, Pillager Captains, Evokers and Ravagers to drop some books by chance, so that Illager fights pay.
64. As a player, I want raid-spawned Illagers to drop no books except raid-only rewards like Ravager Knockback, so that raid farms don't flood the Library.
65. As a player, I want the Ender Dragon to drop a Zephyr book and the Wither a Blight book every kill, so that repeating those fights stays worthwhile.
66. As a player who fishes, I want fishing to give only Luck of the Sea and Lure books, so that fishing books come from fishing.
67. As a player, I want ominous vaults to give the Mace's books, so that Trial Chambers stay worth raiding.
68. As a player, I want every other enchanted-book chest entry replaced by 1 diamond, so that chests still feel worth opening.
69. As a player, I want gear from chests, fishing, trades and bartering to be plain, so that enchantments come only from the Library.
70. As a player, I want gear a mob spawned with to drop without enchantments, so that mobs are never an enchantment source.
71. As a player, I want gear a mob picked up from me to drop unchanged, so that I get my own gear back.
72. As a player, I want every Novice Librarian to buy any enchanted book for 4 emeralds, 10 villager XP and 50 XP points per find it holds, instead of selling books, so that my surplus finds have a use.
73. As a player, I want bartering to drop Soul Speed books and iron boots, so that bartering doesn't hand out books.
74. As an explorer, I want Unbreaking III to need four Stronghold books and Fortune III four Mansions, so that I explore past the first ring.

### The Library and the table

75. As a member of a group, I want books shelved in Chiseled Bookshelves around a table to form one shared Library, so that we pool our finds.
76. As a player, I want the table to offer each shelved enchantment at its highest shelved level without using up the book, so that books are reusable.
77. As a player, I want the three random offers gone, so that I never gamble at the table.
78. As a player, I want the table to still need 15 bookshelves, any of which can be Chiseled, so that building a table stays familiar.
79. As a player, I want the table to work as a checklist with my item's current enchantments pre-selected, so that I can change one thing without redoing everything.
80. As a player, I want saving to cost 1 lapis per selected enchantment, so that every save has a small cost.
81. As a player, I want the first save on an unenchanted item to take one unit of its material, so that the item holds a deposit.
82. As a player, I want later edits to cost only lapis, so that swapping enchantments stays cheap.
83. As a player, I want the grindstone to remove all enchantments and return the material unit but not the lapis, so that the deposit is only lost if the item breaks.
84. As a player, I want cheap swaps, so that situational enchantments like Smite or Fire Protection are worth preparing for a trip.
85. As a group enchanter, I want the slot check to happen only when I save, so that I can prepare gear for friends with fewer levels.
86. As a player, I want gear above my level to keep working, so that losing levels never disables my equipment.
87. As a player editing gear above my level, I want the table to refuse the save until it fits my slots, so that I must reduce it or ask the group's enchanter.
88. As a player, I want gold tools, weapons and armor to get 1 extra Enchantment Slot, so that gold keeps its enchanting identity.
89. As a player, I want the table to show Library Progress, so that I can see how close the Library is to Full.
90. As a completionist, I want Mending only from a Full Library, so that it is the final reward for doing everything.
91. As a member of a group, I want Mending access to follow the books, so that moving the Library moves Mending, while gear that already has Mending keeps it until it is next saved at a table without a Full Library.
92. As a player, I want enchanted books to resist fire and explosions, so that an accident doesn't destroy hours of exploration.
93. As a player, I want the anvil to refuse applying books to gear, so that the table is the only way to enchant.
94. As a player, I want items no longer to combine with each other in the anvil, crafting grid or grindstone, so that found books are the only source of levels and repairs use materials.

### Repairs and materials

95. As a player, I want a full repair to take one fewer material unit than the recipe, at least 1, with each unit restoring an equal share, so that repairing is always a discount on crafting.
96. As a player, I want one Netherite Ingot to fully repair Netherite gear and one Alloy ingot to fully repair its own Alloy gear, so that endgame upkeep is clear.
97. As a player, I want items without an ingot to use a clear material (phantom membrane for Elytra, breeze rod for the Mace, string for bows, crossbows and fishing rods, prismarine shard for tridents, iron ingot for shears and flint and steel), so that every item can be enchanted and repaired.
98. As a player, I want smithing upgrades to refuse enchanted gear, so that a cheap material deposit can't turn into a better ingot.
99. As a player, I want to grind, upgrade and re-enchant my gear, so that upgrading is a short, cheap detour.

### New and removed enchantments

100. As a player, I want Zephyr boots to give a double jump that goes higher at each level, so that I move better in the End before I have an Elytra.
101. As a player, I want a Zephyr jump during a fall to Clutch, cutting the distance fallen so far by 10%, 15% or 25%, so that a late jump is a skill with no timing window.
102. As a player, I want the height a Zephyr jump regains not to add fall damage, so that using it is never a trap.
103. As a player, I want Zephyr not to replace Feather Falling, so that both keep a role.
104. As a player, I want Blight on Fire Aspect's weapons, incompatible with Fire Aspect, giving Wither I for 4 seconds per level, so that I have a choice that works on fire-immune mobs and in water.
105. As a player, I want Curse of Binding and Curse of Vanishing removed, so that no dead enchantments remain.

### Golden apples and villagers

106. As a player, I want a plain golden apple to keep its food value and lose its effects, so that it is the base for the table.
107. As a player, I want to enchant a golden apple at the table with effects that unlock with Library Progress, so that the Library also rewards survival.
108. As a player, I want each apple effect to take one Enchantment Slot and cost 1 lapis, plus a block of gold on the first save, so that strong apples come late.
109. As a player, I want the apple's effects to be Regeneration II for 20 seconds at 15%, Fire Resistance for 5 minutes at 30%, Absorption IV for 2 minutes at 50% and Resistance for 5 minutes at 75% Library Progress, so that each Library step gives something.
110. As a player, I want the grindstone to turn an enchanted golden apple back into a plain one and return the block of gold, so that apples follow the same deposit rule as gear.
111. As a player, I want every enchanted golden apple in loot to be plain, so that a chest can't skip the table.
112. As a player, I want every village to be an Abandoned Village with zombie villagers and no iron golems, so that finding a village doesn't skip progression.
113. As a player, I want villages to be about 5.5 times rarer, so that a village is a find, not a given.
114. As a player, I want village crops, beds, bells and workstations to stay, so that a village is a slow start for my settlement.
115. As a player, I want hay bales and brewing stands removed from villages, so that one village doesn't give stacks of bread and early brewing.
116. As a player, I want village chests to hold no diamonds, obsidian or iron gear, so that an Abandoned Village feels picked over.
117. As a player, I want to cure a zombie villager with a splash potion of Weakness and any enchanted golden apple, so that earning villagers needs the Nether, a modest Library and level 30.
118. As a player, I want Weakness brewed only from Strength, so that the cure needs the Nether.
119. As an advanced player, I want Witches to still throw Weakness, so that skyblock and other unusual routes stay open.
120. As a player, I want the igloo basement villager to be a zombie villager, so that no structure hands out a living villager.
121. As a player who never finds a village, I want natural zombie villagers and zombie conversion to stay vanilla, so that I can still earn villagers.
122. As a farm builder, I want breeding, professions, workstations and cure discounts to stay vanilla once I have villagers, so that iron farms, raid farms and trading halls still work.
123. As a player, I want villagers to sell plain gear wherever vanilla sells enchanted gear, so that trades don't skip the Library.
124. As a player, I want no villager to sell Pith, Crystal Hearts or raw diamonds, so that Crystal Heart crafting keeps its limit.

### Early tiers and Reinforcement

125. As a new player, I want a wooden pickaxe to mine copper ore, deepslate copper ore and raw copper blocks, so that copper takes stone's place.
126. As a player, I want stone tools removed, including the stone spear, so that the ladder is wood, copper, iron, diamond, Netherite, then Alloys.
127. As a player, I want everything that needed a stone tool to need copper or better, so that the ladder has no gap.
128. As a player, I want trades, chest loot and the Bonus Chest to give copper tools wherever they gave stone ones, so that nothing hands out a removed item.
129. As a player, I want furnaces, smokers, blast furnaces and campfires to stay vanilla, so that there are no early steps I forget an hour later.
130. As a player, I want to Reinforce an armor piece at a Smithing Table with a chainmail piece of any slot and a Reinforcement template for +1 armor toughness, so that my armor holds up before Protection.
131. As a player, I want every armor piece, including the turtle helmet and the Armored Elytra, to accept Reinforcement once, so that every set can use it.
132. As a player, I want plain Elytra, carved pumpkins, heads and mob armor to refuse Reinforcement, so that it stays an armor upgrade.
133. As a player, I want Reinforcement to carry through smithing upgrades and between Alloys, stack with trims and Adamant, and work on enchanted armor, so that it is never wasted.
134. As a player, I want a "Reinforced" tooltip line and a chainmail overlay drawn like a trim, so that I can see which pieces are Reinforced.
135. As a player, I want to buy the Reinforcement template from a Master Armorer for 32 emeralds, so that Reinforcement is an early reward for earning villagers.
136. As a player, I want to copy the template with 7 diamonds and a block of iron, so that I don't need a trade for every piece.
137. As a player, I want chainmail to stay uncraftable, so that its mob drops, chests and Armorer trades keep a use.

### Netherite and Alloys

138. As a player, I want a Netherite Ingot to need 4 Netherite Scrap and 4 Pith, so that Netherite ties to the dangers that give Pith.
139. As a player, I want an Alloy Furnace crafted from 5 gold ingots, a blast furnace and 3 polished blackstone bricks, so that the endgame workstation looks and builds like a better blast furnace.
140. As a player, I want the Alloy Furnace to turn 1 Netherite Ingot and 1 storage block into 1 Alloy ingot in 10 seconds, burning 1 Ancient Ember, so that each Alloy ingot is a clear cost.
141. As a farm builder, I want hoppers to work with the Alloy Furnace, so that its inputs can be automated.
142. As a player, I want Piglin Brutes to drop an Ancient Ember on player kills at 25% plus 5% per Looting level, so that Bastions stay worth raiding.
143. As a player, I want Steel (iron) gear to have 50% more durability, with more weapon knockback, more knockback resistance and 10% less sonic boom and explosion damage per armor piece, and a full set to give knockback and Levitation immunity, so that iron has a sturdy endgame role.
144. As a player, I want Electrum (gold) gear to get 1 extra Enchantment Slot, with 15% less fire damage per armor piece and Piglins treating it as gold, and a full set to give fire and lava immunity and slow Withered Hearts to one every 10 seconds, so that gold keeps its enchanting and Nether role.
145. As a player, I want Shakudo (copper) tools to mine 30% faster, so that a Shakudo pickaxe with Efficiency V insta-mines stone without Haste, and attack 10% faster, its armor to add 2.5% movement speed per piece, and a full set to stop slowdown from soul sand, honey, cobwebs and powder snow, jump full height off honey and walk on powder snow like leather boots, so that copper has a speed role.
146. As a player, I want Adamant (diamond) weapons and tools to deal 1 extra damage, its tools to mine hard blocks such as obsidian and ancient debris as if they were deepslate, an Adamant pickaxe with Efficiency V and Haste II to break bedrock, its armor to add 1 armor toughness per piece, and a full set to make 25% of my melee damage ignore armor points but not Protection, so that diamond has a raw-power role.
147. As a player, I want Orichalcum (emerald) tools to add a flat 50% to drops that Fortune or Looting affect, excluding Ancient Embers, Pith and enchanted books, so that emerald has a wealth role.
148. As a player wearing any Orichalcum armor, I want 10% less magic damage per piece and neutral Illagers until a raid, an attack or a book alert, so that wealth buys safety.
149. As a player with a full Orichalcum set, I want to survive a lethal hit with Totem effects, recharging over 30 minutes of play, before any held Totem or Crystal Heart is used, so that I have a lasting safety net, even in Hardcore.
150. As a player, I want to upgrade any Netherite gear to an Alloy at a Smithing Table with one ingot and no template, so that the step is simple.
151. As a player, I want to change an Alloy item to another Alloy, losing the old one, so that I can change my mind at a cost.
152. As a player, I want an unused Alloy ingot to smelt back into a Netherite Ingot, losing the block and Ember, so that a mistake isn't a total loss.
153. As a builder, I want Alloy blocks that store 9 ingots, craft back into 9 and work as beacon bases, so that Alloys have a use beyond gear.
154. As a player, I want an Armored Elytra that permanently combines an Elytra with an Alloy chestplate, keeping its armor and effects, so that I can fly with armor.
155. As a player, I want the Armored Elytra to count as the chest piece for its Alloy's set bonus, so that I don't lose my set to fly.
156. As a player, I want the Armored Elytra to glide worse and boost 25% less with rockets than a plain Elytra, so that plain Elytra stays the travel choice.
157. As a player, I want no Alloy to be strictly best, so that my material choice is a real trade-off.

### Mobs

158. As a player, I want adult zombies on Normal and Hard to be fast and frail and step up 1 block, so that combat is dangerous without being slow.
159. As a player, I want baby zombies, baby husks, baby drowned and baby zombie villagers to have low health, so that they are scary but quick to kill.
160. As a player, I want skeletons, strays, bogged and parched, creepers, cave spiders and silverfish to have lower health, so that fights end quickly.
161. As a player, I want spiders on Normal and Hard to jump higher and take no fall damage, so that they keep their own threat.
162. As a player, I want adult husks on Normal and Hard to be slow, armored brutes, so that deserts feel distinct.
163. As a player curing villagers, I want adult zombie villagers to keep vanilla stats, so that curing isn't a chase.
164. As a builder, I want creepers never to spawn naturally at Y 63 or above in the Overworld, so that my surface builds are safer.
165. As a farm builder, I want creeper farms below Y 63 and spawners to keep working, so that gunpowder stays farmable.

### Boss Subspaces

166. As a player, I want every Warden to open a fixed Subspace with a 50-block radius where it spawns, applying Darkness and Grounded, so that I can't fly away from it.
167. As a player, I want Grounded to stop Elytra gliding, shorten Ender Pearl throws and stop Riptide and Zephyr's jump, so that escape is a journey through the area.
168. As a player, I want Levitation, Slow Falling, Chorus Fruit and short pearl teleports to still work while Grounded, so that clever play still helps.
169. As a player, I want no barrier at a Subspace's edge, so that I can sneak out or hide until the Warden leaves.
170. As a player, I want the Warden to know where every entity in its Subspace is, so that hiding needs real stealth while anger still comes only from sniffing.
171. As a player, I want sculk to spread across the ground, a sculk shell at the Subspace's edge and a sculk sky inside it, so that I can see where it is and that I am in it.
172. As a player, I want shriekers grown inside the Subspace to stop summoning when it ends, so that the city doesn't escalate after each fight.
173. As a player, I want shrieks to count only for players in Warden Territory, so that a Warden switch can't make Ancient Cities safe.
174. As a member of a group, I want a Warden to spawn near the player who caused the shriek, or the nearest player in Warden Territory, so that a friend's mistake can matter to me.
175. As a player, I want each Warden's Subspace to end when that Warden goes away, so that the danger has a clear end.
176. As a player, I want overlapping Subspaces to work without traps and Grounded not to stack, so that two Wardens are harder but never unfair.
177. As a player, I want death inside a Subspace to respawn me as normal, even at a bed or anchor inside it, so that respawning stays predictable.
178. As a player, I want the Wither to open a Subspace with a 100-block radius where Withered Hearts never return, so that the fight can't be won by stepping back to recover.
179. As a player, I want the Wither to stay within 50 blocks of its Subspace's centre, be immune to suffocation and pass through bedrock and obsidian, so that it can't be trapped.
180. As a player, I want the Elder Guardian and Ender Dragon fights to stay vanilla, so that only real cheeses are closed.

### Structures and spawners

181. As a player, I want spawners to ignore light, so that one torch doesn't win a dungeon.
182. As a player, I want a spawner to be unbreakable and immune to explosions until it is Cleared, so that I must deal with it.
183. As a player, I want a spawner Cleared after 12 of its mobs die, plus 2 for each additional player, so that its fight scales with the group.
184. As a member of a group, I want the count shared and the requirement never to drop when someone leaves, so that the rule can't be gamed.
185. As a farm builder, I want any death to count, including traps and lava, so that a farm can Clear its own spawner.
186. As a farm builder, I want a Cleared spawner to stay Cleared and keep spawning, so that I can keep it for a farm.
187. As a player, I want to see a spawner's progress, so that I know how close it is to Cleared.
188. As a player, I want a Cleared spawner to drop 1 Pith when broken, so that breaking it is a one-time reward.
189. As a player, I want trial spawners unchanged, so that Trial Chambers keep their own rules.
190. As a player, I want more dungeons, so that early books and Pith aren't too rare.
191. As an explorer, I want Crystal Hearts in chests guarded by real threats and Pith more widely at low odds, so that dangerous places pay.
192. As an explorer, I want End City chests to roll for Pith and a Crystal Heart, so that End Cities make up for lost enchanted gear.
193. As an explorer, I want End Cities to keep their buildings, Shulkers, ships and guaranteed Elytra, so that the End stays familiar.

### Blood Moons

194. As a player, I want a Blood Moon every 32 in-game days to Restore selected structure mobs, so that Ancient Embers, Pith and Illager books renew slowly.
195. As a player, I want each mob to return only at its original spot, as the mob that spot first rolled, when the spot is clear and the right block is still there, so that I control whether a structure renews.
196. As a player, I want blocks, chests, books, Allays and broken spawners never to return, so that the world never undoes my work.
197. As a player, I want no mob to be Restored within 32 blocks of an online player, so that a Blood Moon never ambushes me.
198. As a player, I want missed Blood Moons not to stack and far chunks not to load for them, so that the server stays light.
199. As a player living in a Bastion, I want my intact Bastion base to still get visitors, so that living there stays lively.
200. As a player, I want a red moon and sky and one chat message in every dimension, so that I know a Blood Moon has risen.
201. As a player, I want to sleep through a Blood Moon and face no extra spawns, so that it is a restock, not a siege.

### Brewing

202. As a player, I want potions, splash potions, lingering potions and water bottles to stack to 16, so that brewing supplies fit in my inventory.
203. As a player, I want splash and lingering throws to share a 1-second cooldown, so that stacks don't make potion spam too strong.
204. As a farm builder, I want dispensers to throw potions at vanilla speed and brewing stands to keep one bottle per slot, so that brewing setups keep their throughput.

### Advancements

205. As a player, I want new advancements in vanilla's own tabs that teach the first step of each system, so that I learn the mod while I play.
206. As a player, I want each system to have at most one goal or challenge beyond its first step, so that the tabs stay clean.
207. As a player, I want advancement descriptions in plain words with no lore, so that they teach the mechanic.
208. As a player, I want Getting an Upgrade to ask for a copper pickaxe, so that it matches the new ladder.
209. As a player, I want vanilla advancements our systems delay to keep their criteria and positions, so that they stay possible.
210. As a player, I want new challenges to give 100 XP and no advancement to give items, except the four boss Crystal Hearts, so that rewards stay in the world.
211. As a player, I want one hidden joke challenge, But why?, for a level 4 beacon built only from Alloy blocks, so that the rare player who does it gets a laugh.
211a. As a player, I want a visible challenge, Breaking the Rules, for breaking bedrock, so that I learn an Adamant pickaxe can do it.

### Worlds and multiplayer

212. As a world creator, I want vanilla's difficulty setting to set the death rules, so that I don't learn a new setting.
213. As a member of a group, I want Max Hearts, the Heart Floor, Remnants and Enchantment Slots per player, while Libraries, structure books and spawner counts are shared, so that we play together and still stand apart.
214. As an explorer, I want a held Echo Shard to let me hear every sculk shrieker near me pulse from where it is, faster the closer I get, so that I can find a keeper hidden in a wall and pick my way around shriekers in the Deep Dark.

## Implementation Decisions

### Platform and approach

- Fabric mod for Minecraft 26.3 on Fabric API, required on both sides. Library and API mods are allowed. Content mods are not required.
- New content is real registered items, blocks, entities, effects and enchantments, never renamed vanilla items.
- Prefer vanilla's data-driven systems wherever they express the rule: recipes, loot tables, tags, advancements and their rewards, enchantment definitions with effect components, worldgen data, and trade lists. Use code only where data can't express the rule.
- Prefer Fabric API events and hooks. Use mixins only where no event exists.
- Per-player and per-block persistent state uses Fabric's data attachment system. Player state that must survive respawn is copied on respawn.
- Balance numbers live in one place per system, so playtest tuning is a single edit.

### Modules

Each module below hides its rules behind a small interface. Other modules call the interface and never reach into its state.

**Hearts.** Owns Max Hearts, the Heart Floor, Withered Hearts and lethal-hit saves.
- Per-player state: Max Hearts (starting at 10, never below 3 through death, at most 30) and the Withered Heart count. Max Hearts apply as a max health attribute modifier. Usable hearts are Max Hearts minus Withered Hearts.
- The Heart Floor is computed, not stored: on Normal and Hard it is 10 minus the Milestone advancements held, minimum 3. On Peaceful and Easy it is 10. Revoking a Milestone raises it again.
- On death: lose 1 heart (2 on Hard), never below the Heart Floor, and never raising Max Hearts. Hardcore uses vanilla's death rule. Keep Hearts on skips the loss.
- A drop in the Heart Floor sends a red chat message with the new floor in bold. The death screen shows the Max Hearts lost, so the client gets that number.
- Lethal-hit order: the Orichalcum set save first, then a held Totem or Crystal Heart in vanilla's hand order. Any save is not a death.
- Withered Hearts: one forms every 5 seconds under the Wither effect (10 with a full Electrum set), down to 1 usable heart. The timer starts when the effect starts and resets whenever the effect ends, so a single short exposure withers nothing. Re-applying Wither while it lasts (such as repeated Blight hits) keeps the effect, and the timer, running; check that vanilla's effect refresh already does this. The interval follows the armor worn at each moment, so swapping Electrum on or off changes it straight away. Timer progress is saved through logout. All return at once when the player has no Wither effect, is not in the Nether and is not inside any Wither Subspace. Death clears them. They persist through logout and sync to the client for darkened rendering.
- Interface: read Max Hearts, usable hearts and Heart Floor; add a Max Heart (refused at 30); a death hook; a lethal-hit hook.

**Crystal Heart and Pith items.** Crystal Heart is a registered item, consumed over about 1.6 seconds for +1 Max Heart, refused at the cap without being used. Held in either hand it saves from a lethal hit with the Totem's effects and is used without adding a heart. Pith is a registered item. Recipes: Crystal Heart is 8 diamonds around 1 Pith. Nether Star crafts into 9 Pith, with no reverse recipe.

**Remnants.** A registered entity owned by one player.
- Created on death holding all of the player's XP points. A death at 0 XP creates none.
- Placement, in order: if the death spot can't be collected from (the void, lava and similar), the Remnant goes to the player's last solid ground in that dimension; if none is recorded there, it goes to the player's respawn point. Otherwise it stays where the player died, including inside a Warden Subspace. Safe placement always wins. The module tracks each player's last safe standing position per dimension.
- With Keep XP on, no Remnant is made.
- Sent only to its owner's client, so only the owner sees its glow. Only the owner can collect it, by walking over it, and gets the points directly, never as orbs.
- Immune to all damage, never despawns, and persists while its owner is offline or its chunk is unloaded.
- Decay must reach Remnants in unloaded chunks and other dimensions, so it is worked out lazily. Each player has a persistent death count. Each Remnant stores its starting points and its owner's death count when it was made. Whenever a Remnant loads, is collected or is checked, its points are reduced once for every later death: each time, the points become 80% of the current points, rounded down. A death counts even at 0 XP. The death that makes a Remnant does not cut that Remnant. A Remnant whose points reach 0 is removed.
- The Recovery Compass stays vanilla and points to the player's recorded last death location. When a Remnant is moved to safe ground, that recorded location is set to the Remnant's spot, so the compass leads to it.

**Milestones and advancements.** Advancement data for the 24 new advancements, the reparented vanilla ones and the changed criteria.
- Milestones are seven advancements: Acquire Hardware, Diamonds!, We Need to Go Deeper and The End?, plus new ones for a copper ingot, a Netherite Ingot and an Eye of Ender. Each description starts with a red broken heart icon, as in Matcha Flavoured, and the chat message when one lowers the floor begins "[Milestone]". The Hearts module reads which Milestones a player holds.
- New advancements, parents, frames and rewards follow the table in the Advancements decision. Custom criteria triggers cover: shelving a book in a Library, a Full Library, making an enchanted golden apple, Reinforcing a piece, getting an Ancient Ember, smelting an Alloy ingot, wearing each full Alloy set at any time, getting a Withered Heart back, an all-Alloy level 4 beacon, getting an Armored Elytra, breaking bedrock, breaking a Cleared spawner (to the breaker), consuming a Crystal Heart, being saved by one, reaching 30 Max Hearts, killing the Wither (every player inside its Subspace), a Wither passing through bedrock or obsidian (every player inside its Subspace), killing an Elder Guardian (every player within 64 blocks), surviving the Warden (every player within the Warden survival range of it when it digs back down or dies), collecting your own Remnant, and being in the Overworld when a Blood Moon rises.
- Crystal Hearts come only from Kill the Wither, Kill an Elder Guardian, Sneak 100 (which becomes Survive the Warden) and Free the End, as advancement reward loot. New challenges give 100 XP. Only But why? and Nice Try are hidden.
- Vanilla changes: Getting an Upgrade asks for a copper pickaxe. Stone Age keeps its criterion, so the stone tool materials tag stays. Postmortal still counts only a Totem. Enchanter still fires on vanilla's enchanted item trigger. Getting an Upgrade, Enchanter, Zombie Doctor, Eye Spy and Cover Me in Debris move under their new parents. Subspace Bubble and Respecting the Remnants keep their titles.

**Enchanting and Library.** Replaces the enchanting table's menu.
- The Library is the enchanted books in Chiseled Bookshelves at the positions vanilla checks for bookshelf power. The table still needs 15 bookshelves of either kind.
- The module computes: the offered enchantments, each at its highest shelved level; Full Library (all 42 enchantments except Mending at max level); and Library Progress (sum of the highest shelved level per enchantment, over 130). Mending is offered only while the Library is Full. Nothing about the Library is recorded; it is read live.
- The table accepts only enchantable gear and golden apples. It refuses plain books and enchanted books, so it can never copy a shelved enchantment onto a new book. Books come only from finds and the anvil.
- The menu is a checklist. The inserted item's enchantments start selected. Save is allowed when the selection has at least one enchantment, every selected enchantment is offered by the current Library at that level or higher, every choice is valid for the item and compatible under vanilla's rules, and the count fits the saving player's Enchantment Slots. This one rule covers enchantments already on the item: if the Library lost a book or a level, the player must remove or lower that enchantment to save, Mending included. A refused save changes nothing. Saving costs 1 lapis per selected enchantment, plus one material unit on the first save of an unenchanted item. The deposit is stored on the item. Saving fires vanilla's enchanted item trigger.
- Enchantment Slots: one per 30 levels, no cap, plus 1 for gold and Electrum tools, weapons and armor. Golden apples never get the extra slot.
- Golden apples: the table offers effects instead of enchantments. Regeneration II for 20 seconds at 15%, Fire Resistance for 5 minutes at 30%, Absorption IV for 2 minutes at 50%, Resistance for 5 minutes at 75%. Each takes a slot and 1 lapis. The first save takes a block of gold. A saved apple becomes the vanilla enchanted golden apple item carrying the chosen effects. The table and grindstone work on one apple at a time, as their slots hold one item. Each apple carries its own block of gold deposit, and apples stack only when their effects match, so splitting or merging stacks never changes a deposit. Any enchanted golden apple cures a zombie villager.
- The grindstone strips everything, returns the material deposit (and turns an enchanted golden apple back into a golden apple with its block of gold), never returns lapis, and gives no XP.
- Enchanted book item entities resist fire and explosions.
- The menu shows Library Progress through menu data, so the client draws the bar.

**Material units.** One shared lookup from an item to its enchanting and repair material: the vanilla repair item where one exists, the main crafting ingredient otherwise, the Netherite Ingot for Netherite gear and the own Alloy ingot for Alloy gear. The overrides are phantom membrane for Elytra, breeze rod for the Mace, string for bows, crossbows and fishing rods, prismarine shard for tridents, iron ingot for shears and flint and steel, and a block of gold for golden apples. It also answers how many units a full repair needs: the number of that material in the item's recipe, minus 1, at least 1, and 1 for Netherite and Alloy gear. Only the chosen material counts, not other ingredients (a shield's 6 planks give 5, ignoring its iron). Items with no recipe: chainmail pieces repair like the matching iron piece (4, 7, 6 and 3 iron ingots), and Elytra and tridents take 4 units, matching vanilla's 25% per phantom membrane.

**Anvil, grindstone and smithing rules.**
- Anvil: combines two books with the same single enchantment and the same level into the next level, up to that enchantment's max, for 50 × the resulting level in XP points. Renaming costs 10 points. Repairs use material units, each restoring an equal share, and cost no XP. No prior-work penalty. Items never combine with each other. Books never apply to gear. No warning is shown when a cost drops a player below a slot threshold.
- Crafting grid and grindstone: no combining of damaged items.
- Smithing: material upgrades refuse enchanted gear. Reinforcement is the exception, as it does not change material.

**Enchantments.** Zephyr and Blight are registered as data-driven enchantments. Blight uses vanilla's effect components to apply Wither I for 4 seconds per level on hit, with 2 levels, on Fire Aspect's weapons, exclusive with Fire Aspect. Zephyr (boots, 3 levels) needs code: a double jump that goes higher per level and needs client input with server checks; it does not reset fall distance, takes the regained height off again, and Clutches by cutting the distance fallen so far by 10%, 15% or 25%. Curse of Binding and Curse of Vanishing are removed from every source and are not counted in the Library.

**Structure books.** A registered block with a block entity that shows a floating level I book and holds its enchantment.
- One per structure per world, placed at generation in a fixed, visible spot. The exception is spawner books: every Dungeon spawner and every Mineshaft cave spider spawner holds a book of its own, besides the Mineshaft's Efficiency book. Structures with several possible enchantments choose one at generation.
- Starting approach, open to experiment during the build: no vanilla template is edited. When a structure's pieces are planned, a small rule for that structure picks the book's spot from its pieces (for example, the Stronghold's portal room or the Bastion's treasure piece). The spot and the enchantment are saved with the structure, and the book is placed when the chunk holding that spot is built. This needs one mixin on structure placement, since Fabric has no event for it. Dungeons are placed features, not structures, so the dungeon room's own placement gives the book to its spawner, which drops it with its Pith once Cleared and broken. The same saved structure data can also hold the Fortress's Blaze spawner record and the Blood Moon mob spots. Hand-edited templates stay an option where exact placement matters most.
- The block can't be mined, blown up, pushed by pistons or replaced. Using it is the only way to take the book.
- Taking the book is one server action. It is refused, with nothing changed and no wake event, while the book is locked or when the player's inventory has no room for the book. Otherwise the player gets the level I book, the block is removed, and the structure's wake event fires: every Piglin in the structure angered for a Bastion; every Illager in the structure turned hostile, ignoring Orichalcum neutrality, for a Stronghold or Woodland Mansion. Other structures, the Ancient City included, have no wake event.
- A Bastion's layout is the template its start piece places, and each layout's book floats just above that layout's own chest: the treasure chest in the treasure room's gold pile, the bridge chest in the bridge layout's gatehouse, the chest in the housing units' central courtyard, and a hoglin stable's chest (or, when no stable with one generated, the chest in the stables' base wall). Taking it sets every loaded Piglin and Piglin Brute standing in one of the Bastion's pieces hunting the player for 10 minutes (vanilla's chest anger lasts 30 seconds), whatever the player wears: each is angry at the player, attacks them from any distance in the dimension, and can sense them and plan a path to them from across the Bastion (its follow range grows by 64 blocks while it hunts). Dying ends the hunt, as Piglins forget a dead target. Vanilla's own anger, from chests and gold, keeps its usual reach. The book is locked, with the Fortress's locked look, until three of the Bastion's Piglin Brutes, its keepers, chosen with equal weight when it's planned (every Brute when it has fewer), are gone for good: any death counts, as does being removed by converting or by Peaceful, but not unloading or changing dimension. Trying to take it while locked says how many keepers are dead, makes each one still alive glow for 60 ticks, as a bell does to raiders, and sends trails to it (or to where it spawned, when it isn't loaded). Each keeper is tagged as its template places it. A level that's Peaceful opens its Brute locks for good, since Peaceful never spawns a keeper and drops a saved one.
- The Nether Fortress's book floats above the lava well in the Castle Entrance courtyard, or at the centre of the start bridge crossing in a fortress without one. It has a visibly locked state: a closed, still book inside the world border's forcefield. Each fortress has a persistent record in world data, made at generation: the positions of the Blaze spawners that generated in it and which of them are Cleared. The Spawners module's Cleared event updates that record whether or not the book's chunk is loaded, and a Cleared spawner that is later broken stays counted. The book unlocks once every recorded spawner is Cleared. Trying to take a locked book says how many are Cleared and sends trails, like the Creaking heart's, from the book to each spawner still to be Cleared. Every new fortress is planned with at least one Blaze spawner: a layout without a spawner room is planned again, up to 16 times. A fortress that still has none starts unlocked.
- The Ancient City's book floats above the altar in front of the portal, on the city's axis. Taking it summons nothing: a Warden comes only from a shriek, under the Warden summoning rules. The book is locked, with the Fortress's locked look, until the city's keepers, three sculk shriekers in a ring near its edge, are broken.
  - Keepers are chosen when the city is planned, from its sculk patch origins: the position of each piece that placed the `sculk_patch_ancient_city` feature from the `ancient_city/sculk` pool. No shrieker exists yet then, but these positions are fixed (see `docs/research/vanilla-ancient-city-sculk.md`). Distances and directions are measured horizontally from the book, and only origins at least 30 blocks from it are eligible.
  - The first keeper is picked with equal weight from the farther half of the eligible origins by distance. The second and third are the farthest eligible origin within 30° of the directions 120° and 240° around the book from the first. A window with none widens to 45°, then 60°, and is left empty if it still has none. No origin is used twice. A city ends up with up to three keepers, and one with none starts unlocked.
  - Each keeper's origin and whether it is broken are saved in the city's record, as the Fortress's spawners are. When the chunk holding a keeper's origin is built, its patch generates as in vanilla, then a sculk shrieker with `can_summon: true` is placed at the origin, or on top of the catalyst when the patch placed one there. Any block already there is replaced.
  - A keeper is broken once its spot no longer holds a sculk shrieker, by any means, and stays counted if a shrieker is put back. Trying to take the locked book says how many keepers are broken and sends trails from the book to each keeper still standing. A standing keeper gives off sculk charge bubbles twice a second, seen within vanilla's 32-block particle range, so it can be found. The operator command `/keepers` lists the keepers of the nearest Ancient City book, for debugging.
  - While the book is in place, every player inside the city's bounds (its whole bounding box, as for Warden Territory) is Grounded. Once it's taken, the city's Grounded ends for everyone. Grounded from a Warden Subspace is separate and keeps applying.
- Source list: Abandoned Camp Lunge; Ancient City Swift Sneak; Bastion treasure room Fire Aspect, bridge Soul Speed, housing units Flame, hoglin stables Thorns; Buried Treasure Aqua Affinity or Respiration; Desert Pyramid Blast Protection; Dungeon Smite for a zombie or skeleton dungeon and Bane of Arthropods for a spider dungeon (read from the mobs those enchantments hit harder, with equal weight for any other mob); End City Feather Falling; Igloo Frost Walker; Jungle Temple Looting; Mineshaft Efficiency, plus Bane of Arthropods held by each cave spider spawner; Nether Fortress Fire Protection; Ocean Ruins Channeling, Impaling, Loyalty or Riptide; Pillager Outpost Multishot, Piercing, Projectile Protection or Quick Charge; Shipwreck Depth Strider; Stronghold Unbreaking; Trail Ruins Silk Touch; Woodland Mansion Fortune.

**Spawners.** Normal spawners (not trial spawners) gain per-block state: a death count of mobs they spawned, the set of players who entered their 16-block activation range since counting began, and a Cleared flag.
- They spawn regardless of light. Until Cleared they spawn twice as fast, waiting half the delay vanilla chooses; once Cleared they wait as long as vanilla.
- They are unbreakable and immune to explosions until Cleared at 12 deaths plus 2 per additional player. The requirement never drops. Any cause of death counts. Cleared stays Cleared.
- Progress shows as the world border's forcefield wrapped around the spawner. It starts half a block out and closes in on the spawner as the count grows, in step with the share of the requirement still to go. A Cleared spawner has no forcefield and looks like any other spawner.
- Breaking a Cleared spawner drops 1 Pith and fires the advancement for the breaker.
- Interface: is this spawner Cleared, and a Cleared event. The Structure books module uses the event to update the Fortress record.

**Subspaces.** A shared module: a Subspace is a fixed sphere with a centre, a radius, a dimension, an owning boss and a kind (Warden or Wither). Interface: which Subspaces contain this position, and start and end events. Subspaces sync to clients for the sculk sky and the shell that marks their edge.
- Lifetime: a Subspace starts when its boss spawns or is summoned. Its record is kept in world data, so its centre survives reloads. It ends when its boss dies, digs back down or is otherwise removed for good, never because a chunk unloads. Its rules for players (Darkness and Grounded, blocked Withered Heart recovery) come from the record and keep working while its boss is unloaded. Only work that needs the loaded boss, such as the Warden's awareness and the sculk spread, pauses. Each Subspace ends on its own and runs only its own cleanup.
- Warden kind: players inside get Darkness and Grounded. The owning Warden is aware of every entity inside and pathfinds to the nearest player; anger and sniffing stay vanilla.
- A weak patch of sculk spreads where the Warden appeared, with vanilla's sculk spreading rules (only blocks a Sculk Catalyst can replace), within about 5 blocks of that spot and only while the Subspace is active and its Warden is loaded. Its size and strength are playtest values.
- Each shrieker that grows during a Subspace records that Subspace and can summon. When the Subspace ends, its shriekers lose the ability to summon. A shrieker in an unloaded chunk is updated the next time it loads.
- Wither kind: blocks Withered Heart recovery. It applies no effect by itself. Clients draw every block and entity inside it at light level 3, from the sky and from blocks alike, while blocks and entities that glow on their own still glow; the server's light, and so mob spawning, is unchanged. A game rule sets the level.
- Overlap is allowed. Grounded does not stack.

**Grounded.** A registered status effect, applied inside a Warden Subspace and inside an Ancient City until its book is taken. It stops Elytra gliding the way Levitation does (no glide starts, and one under way ends), which also leaves Firework Rockets nothing to boost. It greatly increases the gravity of Ender Pearls thrown while Grounded and stops Riptide and Zephyr's double jump. Armored Elytra are Elytra, so it stops them too. Levitation, Slow Falling and Chorus Fruit still work.

**Echo Shards.** A player holding an Echo Shard in either hand hears every sculk shrieker within the Echo Shard shrieker range (15 blocks) pulse like a Warden's heartbeat, from the shrieker's position, so 3D sound and directional subtitles ("Shrieker pulses") lead to it, even through walls. A pulse comes every 40 ticks at the edge of hearing, quickening evenly to every 6 ticks beside the shrieker. Only the holder hears it, and a pulse makes no vibration. Spectators hear nothing.

**Warden summoning.** Replaces the vanilla rule. A shriek gives warning levels and cooldowns only to players in Warden Territory (the Deep Dark biome or an Ancient City's bounds). The Warden spawns 5 to 15 blocks from the player who caused the shriek, or the nearest player in Warden Territory if that player is outside it. With no room, the search widens to 50 blocks from that player. If that fails, the cooldown clears. With nobody in Warden Territory, the summon is cancelled.

**Wither changes.** Its AI stays within 50 blocks of its Subspace's centre. It is immune to suffocation and passes through bedrock and obsidian. It drops a level I Blight book every kill.

**Mobs.** Attribute changes apply on spawn, by the difficulty at spawn time, to every mob of the type. Anything not listed stays vanilla. Health is in points.

| Mob | Easy | Normal | Hard |
| --- | --- | --- | --- |
| Adult zombie | vanilla | 10 health, 0.34 speed, steps up 1 block | 15 health, 0.38 speed, steps up 1 block, 5 damage, 0.6 jump |
| Baby zombie, husk, drowned and zombie villager | 4 health | 4 health | 7 health |
| Skeleton, stray, bogged and parched | 10 health | 10 health | 12 health |
| Creeper | 14 health | 16 health | 18 health |
| Cave spider | 2 health, 0.35 speed | 4 health, 0.40 speed | 6 health, 0.42 speed |
| Silverfish | 2 health | 2 health | 4 health |
| Spider | vanilla | 0.85 jump, no fall damage | 0.85 jump, no fall damage |
| Adult husk | vanilla | 30 health, 0.25 speed, 6 damage, 12 armor, 50 follow range, 0.9 knockback resistance | 40 health, 0.265 speed, 8 damage, 14 armor, 60 follow range, full knockback resistance |

- Adult zombie villagers stay vanilla.
- Creepers never spawn naturally at Y 63 or above in the Overworld. Spawners are not affected.
- Mobs remember which gear they spawned with. That gear keeps vanilla's drop chance and drops with enchantments removed. Picked-up gear drops exactly as it was.
- Wither Skeletons spawn holding a Netherite sword in place of the stone one, with a base attack of 3 instead of 4, so they hit for 10 on Normal instead of 8. The sword they spawn with never drops.
- Drops, in loot data: Pith from Elder Guardians (2, always), Evokers (1 at 25%) and Piglin Brutes (1 at 10%); Ancient Embers from Piglin Brutes (25% plus 5% per Looting level, killed by a player, a separate roll). Book drops, each its own roll, only when killed by a player: Vindicator Sharpness 10% and Sweeping Edge 2.5%; Pillager Captain Power 20%, Punch 5% and Infinity 5%; Evoker Protection 15%, one roll; Ravager Knockback 10%. Each Looting level adds 20% of the base chance, and the Orichalcum bonus doesn't apply. Plain Pillagers drop no books. Illagers spawned by a raid wave drop no books; every other Illager does, including Blood Moon Restores and Illagers that later join a raid. Every Ravager can drop Knockback. The Ender Dragon drops a level I Zephyr book every kill.

**Materials and Alloys.** Registered items and blocks: Ancient Ember; five Alloy ingots (Steel from iron, Electrum from gold, Shakudo from copper, Adamant from diamond, Orichalcum from emerald); five Alloy blocks (9 ingots each way, in the beacon base blocks tag); an Alloy version of every item that has a Netherite version in 26.3; and an Armored Elytra for each Alloy.
- The Netherite Ingot recipe becomes 4 Netherite Scrap and 4 Pith.
- The Alloy Furnace is a registered block with its own menu and recipe type. It takes 1 Netherite Ingot and 1 storage block, burns 1 Ancient Ember per ingot, and takes 10 seconds. Hoppers work with it. Its recipe is 5 gold ingots across the top and sides, a blast furnace in the centre and 3 polished blackstone bricks along the bottom. An Alloy ingot smelts back into a Netherite Ingot in any furnace.
- Smithing: Netherite gear plus an Alloy ingot gives the Alloy version, with no template. Alloy gear plus a different Alloy ingot changes Alloy, losing the old one. An Elytra plus an Alloy chestplate gives that Alloy's Armored Elytra, with the chestplate's armor and effects. It accepts the same enchantments as a chestplate, which already include the Elytra's own (Unbreaking and Mending). Plain Netherite chestplates don't qualify.
- Per-item traits use item components and attribute modifiers where vanilla supports them (durability, mining speed, attack speed, damage, toughness, movement speed, knockback resistance). Damage reductions, Piglin gold, Illager neutrality and the Orichalcum drop bonus use code.
- Shakudo's +30% mining speed multiplies the final break speed, after Efficiency, through the player's block break speed attribute, not the tool's base speed. With Efficiency V that's 35 × 1.3 = 45.5, over stone's insta-mine threshold of 45.
- Adamant's hardness cap uses code. For a block in the held Adamant tool's mineable tag, break speed is worked out as if the block's hardness were at most 3. Blocks outside the tag (trial spawners, vaults, reinforced deepslate) and unbreakable blocks are unaffected.
- Bedrock: a player holding an Adamant pickaxe with Efficiency V or higher, with Haste II or higher, breaks bedrock in a flat 15 seconds, checked on the server. Nothing else changes the time. It drops nothing, and every bedrock block counts, including the bottom layer. Not in Adventure mode.
- The Shakudo mining bonus, the hardness cap and the bedrock time are playtest game rules in `MapleGameRules`.
- Set bonuses are checked on equipment change. The Armored Elytra counts as its Alloy's chest piece. Steel: knockback and Levitation immunity. Electrum: fire and lava immunity, and Withered Hearts form every 10 seconds. Shakudo: no slowdown from soul sand, honey, cobwebs or powder snow, full-height jumps off honey, and walking on powder snow as leather boots allow. Adamant: 25% of melee damage ignores armor points but not Protection. Orichalcum: a lethal-hit save with Totem effects, Hardcore included. It starts ready. After a save, a timer on the player recharges it over 30 minutes of online play, counting whether or not the set is worn. Death doesn't reset it.
- Damage reductions apply after armor, Resistance and Protection, so they also reduce damage that bypasses those, such as a sonic boom. They add up per piece (a full Steel set takes 40% off, a full Electrum set 60% off fire) and count every armor slot, so a horse in Steel horse armor takes 10% less explosion damage.
- The Orichalcum drop bonus adds a flat 50% to Fortune and Looting, only on drops they already affect, and never to Ancient Embers, Pith or enchanted books. It follows the item the enchantment is read from (the tool for Fortune, the killer's main hand for Looting) and applies at any level, Fortune or Looting 0 included. A count gains half the count it had before Fortune or Looting, rounded randomly; a Looting chance gains half its unenchanted chance. An excluded drop's Looting chance sits on a pool of only excluded items or on its own entry, where the bonus can tell it apart.
- Illager neutrality needs one Orichalcum piece and covers an Evoker's Vexes, which stay neutral once the Evoker is gone. Only tools and weapons in the main hand give the Orichalcum drop bonus, not held armor. It ends for an Illager in an active raid. Attacking an Illager angers it and every Illager within 16 blocks that can see it at that player, and a structure book's alert angers the structure's Illagers; anger lasts the playtest game rule `orichalcum_illager_anger_time` (600 ticks, a Piglin's 30 seconds) and renews on each attack.
- Armored Elytra flight: horizontal glide movement gets an extra 0.995 multiplier per tick, and Firework Rocket boost is cut by 25%.
- Gear can't be salvaged.

**Early tiers.** Stone tool recipes are removed, including the stone spear. A wooden pickaxe mines copper ore, deepslate copper ore and raw copper blocks through vanilla's tool tags. Everything else at stone tier needs copper or better. Trades, chest loot and the Bonus Chest give copper tools in place of stone tools. Gold tools, weapons and armor get the extra Enchantment Slot. Golden horse armor is unaffected.

**Reinforcement.** A registered Reinforcement template item and smithing recipe type. Template plus armor piece plus any chainmail piece gives the Reinforced piece, marked by an item component that adds +1 armor toughness. Once per piece. It accepts every armor piece including the turtle helmet and Armored Elytra, and refuses plain Elytra, carved pumpkins, heads and mob armor. The component carries through every smithing upgrade. It works on enchanted armor. The client shows a "Reinforced" tooltip line and a chainmail overlay drawn as a trim-like layer that combines with trims. The template copies with 7 diamonds and a block of iron into 2. A Master Armorer sells it for 32 emeralds. It is in no loot.

**Villages and villagers.** Worldgen and entity changes. Village structure set uses spacing 80 and separation 50. Every village spawns zombie villagers and no iron golems. Hay bales (piles and barn hay) and every brewing stand are removed from village pieces. The igloo basement villager is a zombie villager. Natural zombie villagers and conversion stay vanilla.

**Trades and bartering.** Villagers sell plain gear wherever vanilla sells enchanted gear. Toolsmiths sell copper tools instead of stone ones. Librarians buy enchanted books instead of selling them: every Novice Librarian offers one trade, as one of all 3 Novice trades, that takes any enchanted book and pays per find (2^(level−1)) 4 emeralds, 10 villager XP and 50 player XP points on top of the vanilla trade orb, so level V pays 64 emeralds. It has 12 uses and a price multiplier of 0. The trade list shows the level I price; a mixin on the result slot's fill in `MerchantContainer.updateSellItem` sets the real payout from the placed book. Levels 2 to 4 lose their book slot, and the Master level stays vanilla. The Master Armorer sells the Reinforcement template. No villager sells Pith, Crystal Hearts or raw diamonds. Everything else stays vanilla, including cure discounts. Bartering loses Soul Speed books and iron boots, with no replacements.

**Loot and worldgen data.**
- Every enchanted book from any source (structure books, mob and boss drops, fishing and ominous vaults) holds exactly one enchantment at level I.
- Enchanted books leave every other loot table. Each chest entry becomes 1 diamond. Ominous vaults keep only Breach, Density and Wind Burst books. Fishing's book entry becomes Luck of the Sea and Lure books.
- All gear in loot is plain. Enchanted golden apples in loot become golden apples. Stone tools in loot become copper.
- Village chests lose diamonds, obsidian and iron gear, with no replacements.
- New chest rolls, one item per success unless noted: Ancient City Crystal Heart 9%, Pith 18%; Stronghold corridor 12.5%, 6%; Bastion treasure 25%, 5%; other Bastion chests Pith 5%; Woodland Mansion Crystal Heart 12.5%; Dungeon Pith 5%; Mineshaft Pith 5%; ominous vault Pith 10% per opening; End City Crystal Heart 15%, Pith 50% giving 1 or 2.
- Bastion chests that hold ancient debris (treasure, other and hoglin stable) also hold Ancient Embers: each ancient debris entry gets an Ancient Ember entry beside it, in the same pool with the same count and weight. The bridge chest has no debris and gets no Embers.
- Dungeons: 15 placement attempts per chunk above Y 0 and 8 below, each with a single spawner.
- Strongholds gain Illagers at generation, with their spots recorded for Blood Moons. Only Vindicators, Pillagers and Evokers, never Pillager Captains or Ravagers. By piece: portal room 1 Evoker and 2 Vindicators; library 1 Evoker and 1 Vindicator; room crossing 2 rolls; prison hall 1 Vindicator; five-way crossing 1 roll; corridors, turns and stairs a 25% chance of 1 roll; starting staircase none. A roll is a Vindicator or a Pillager, 50/50. Counts are starting values for playtesting. A spot needs the stone bricks family beneath it and a clear 1×2×1 space above, no two spots share a block, and none sits in a doorway. The Illagers are persistent and wander like mansion Illagers. The silverfish spawner and infested blocks stay vanilla.

**Blood Moons.** World-level state: a fixed schedule on every 4th full moon (every 32 in-game days).
- At generation, Restorable structures record each mob spot and the mob it rolled: Bastion Piglins, Piglin Brutes and Hoglins; Ocean Monument Elder Guardians; End City Shulkers; Woodland Mansion Vindicators and Evokers; Stronghold Illagers.
- Each original mob is tagged with its spot. A spot becomes empty only when its mob is gone for good: killed, converted (such as a Piglin turning into a Zombified Piglin) or despawned. A mob that wanders off or sits in an unloaded chunk still holds its spot. Restored mobs keep the original's persistence.
- A spot that was empty when a Blood Moon rose fills the first time its chunk is loaded while the spot is clear, no online player is within 32 blocks, and the required block holds (Bastion: blackstone family or basalt beneath; Elder Guardian: in water with prismarine family beneath; Shulker: attached to purpur family or end stone bricks; Mansion: birch planks, cobblestone or smooth stone slab beneath; Stronghold: stone bricks family beneath, meaning stone bricks, cracked, mossy and chiseled stone bricks, their slabs and stairs, and the infested variants of all four). Until then the spot stays pending: it retries each time its chunk loads, and every few seconds while its chunk stays loaded, until it fills. A pending spot doesn't expire at dawn; it stays pending until filled. A mob that dies after a Blood Moon leaves a spot that waits for the next Blood Moon. Each spot holds at most one mob, so pending spots never overfill. Missed Blood Moons don't stack. No chunks load for the event.
- The night: a red moon and sky on the client, one chat message in every dimension, sleeping allowed, no extra spawns.

**Potions and brewing.** Potions, splash potions, lingering potions and water bottles stack to 16. Splash and lingering throws by players share a 1-second cooldown. Dispensers are not affected. Brewing stand slots still hold one bottle each. A fermented spider eye turns Strength into Weakness (1:30), Long Strength into Long Weakness (4:00) and Strength II into Weakness, and does nothing to a water bottle.

**Game rules.** Three game rules set what death costs, each on its own:
- **Keep Hearts** (default off): when on, death costs no Max Hearts.
- **Keep XP** (default off): when on, death keeps all XP and leaves no Remnant. When off, XP drops as a Remnant. Vanilla's Keep Inventory no longer decides XP.
- **Keep Inventory** (vanilla's rule, default on in new worlds): decides only items. When off, items drop as in vanilla, and the hearts and XP rules still apply.

Three more game rules hold Warden Subspace playtest values. Each applies to Subspaces that open after it changes:
- **Warden Subspace radius** (default 50): how many blocks a Warden's Subspace reaches.
- **Warden sculk patch radius** (default 5): how many blocks from where a Warden appears its sculk patch may spread.
- **Warden sculk patch charge** (default 150): how much sculk the patch spreads, as a sculk catalyst's charge. 0 spreads none.

One more holds a Warden playtest value, and applies at once:
- **Warden survival range** (default 50): how many blocks from a Warden a player must be, when it digs back down or dies, to get Sneak 100.

Three more hold Wither playtest values:
- **Wither Subspace radius** (default 100): how many blocks a Wither's Subspace reaches. It applies to Subspaces that open after it changes.
- **Wither leash radius** (default 50): how many blocks from its Subspace's centre a Wither can move. It applies at once.
- **Wither Subspace light level** (default 3): the light level clients draw everything inside a Wither's Subspace at. It applies at once.

One more holds an Echo Shard playtest value:
- **Echo Shard shrieker range** (default 15): how many blocks away a held Echo Shard hears sculk shriekers.

## Testing Decisions

### The seam

There is one automated test seam: Fabric's server GameTest framework. Tests run on a headless game server started from Gradle, and a failing test fails the build. Every rule the server decides is tested through it: validation of player requests, state changes, items, entities, effects, projectiles and attributes.

The client decides some things, and a headless server can't see them. These are checked by hand in a development client, not automated:

- Visuals: darkened Withered Hearts, the Max Hearts line on the death screen, the Remnant glow, the Reinforced overlay and tooltip, the enchanting table checklist and Library Progress bar, the locked Fortress book, spawner progress feedback, the sculk sky and Subspace shell, the Wither Subspace's dim light, and the Blood Moon sky.
- Player movement and input: Zephyr's double jump and Clutch, Grounded ending a glide, the Armored Elytra's glide and Firework Rocket penalties, Riptide being blocked, and Shakudo's movement speed.
- Sync: that the client shows the server's Max Hearts, Withered Hearts, Remnants, Subspaces and Blood Moon.

Server GameTests still cover the server side of these: the Zephyr request's checks and fall-distance results, the Grounded effect being applied, Ender Pearl gravity, and attribute values. Each build ticket that touches a hand-checked item lists what to check.

### What a good test looks like

- It drives the game the way a player or the world would: a mock player dies, eats, uses an item, opens a menu and clicks its buttons, breaks a block, or crosses a Subspace edge; mobs spawn and die; loot tables roll; recipes craft; time advances; structures are placed with the game's own structure placement.
- It asserts what a player could observe: Max Hearts, usable hearts, items in an inventory or on the ground, XP points, menu results, entity attributes and effects, block state, advancements granted.
- It never asserts private fields, mixin internals or how a module stores its state.
- It states the spec's numbers literally, so a balance change is a deliberate edit to both the value and its test.
- It uses several mock players wherever the rule depends on a group: spawner requirements, shared Libraries, owner-only Remnants and boss credit.
- Chance rules roll many times with a fixed seed and check the rate falls within a tolerance band.
- Rules tied to the calendar set the world's day count to the right night instead of waiting.
- Worldgen frequency (village spacing and separation, dungeon attempts) is checked against the game's loaded worldgen data, since sampling real worlds is slow and flaky. A fresh world is also checked by hand.

### Modules tested

Every module above: Hearts, Crystal Heart and Pith, Remnants, Milestones and advancements, Enchanting and Library, Material units, anvil, grindstone and smithing rules, Enchantments, Structure books, Spawners, Subspaces, Grounded, Warden summoning, Wither changes, Mobs, Materials and Alloys, Early tiers, Reinforcement, Villages and villagers, Trades and bartering, Loot and worldgen data, Blood Moons, Potions and brewing, and Game rules.

### Prior art

The repo has no tests yet; it is a scaffold. The first build ticket sets up the GameTest source set and Gradle task. Fabric API's own GameTests and vanilla's test instances are the reference for style.

## Out of Scope

- Horror elements and secrets.
- Custom music and discs.
- Translations beyond English.
- Fishing as a progression track. Fishing only changes its enchanted-book entry.
- Deeper End changes, which belong to a separate End Update mod that requires this one: vault-based End City chests guarded by trial spawners, the Elytra from an ominous vault on the End Ship, End City keys, and End spawners for Endermen, Endermites and Phantoms.
- New structures. Maple adds none.
- Storage changes. Bundles, Ender Chests, shulker boxes and shulker farms stay vanilla.
- A new healing or food system. Hunger, saturation, natural regeneration and food values stay vanilla until a better design turns up.
- Subspaces for the Elder Guardian and Ender Dragon, unless playtesting finds a cheap way to avoid their fights.
- Changes to armor and combat beyond Reinforcement and Alloys.
- Upgrading existing worlds. The mod is for new worlds.
- Player-facing configuration of the design.
- Automated tests of client-only visuals.
- Build order. It is decided after this spec, when the spec is broken into tickets.

## Further Notes

### Sources

The design map and its resolved tickets in `.scratch/maple-flavored-design/` hold the reasoning behind every rule, and win where any older document differs. The opinions behind them are in `docs/manifestos/`, the research in `docs/research/`, and the summary for players in `docs/project-overview.md`. Terms follow `GLOSSARY.md`. There are no ADRs yet.

### Spec defaults

The design left these small points open. This spec picks a default for each, and Jackson can overturn any of them:

- A Stronghold's or Mansion's book alerts every Illager in the structure, matching the Bastion and the manifesto's "every Illager", rather than a radius.
- Saving at the table needs at least one selected enchantment or effect. The grindstone is the way to strip an item.
- The anvil combines only two books with the same single enchantment and the same level.
- Death never raises Max Hearts, even when the Heart Floor sits above them after a difficulty change or a revoked Milestone.
- A Crystal Heart save gives the Totem's effects and follows vanilla's hand order against a held Totem.
- The Armored Elytra is enchanted and repaired with its own Alloy ingot.
- The structure's floating book is a registered block with a block entity.
- The table refuses plain and enchanted books. This follows from "a level n book takes 2^(n-1) finds", but no ticket says it outright.
- Every book from any source is a single level I enchantment, including fishing and ominous vaults, whose vanilla loot can roll higher levels.
- An Ancient City keeper window that finds no origin widens in two steps, to 45° and then 60°, so each keeper stays in its own third of the ring. A keeper goes on top of its patch's catalyst rather than replacing it.
- With an odd number of eligible origins, the farther half the first Ancient City keeper is picked from includes the middle one, so a single eligible origin is still a keeper.
- A keeper counts as broken when a summoning shrieker leaves its origin or the block above it, where a catalyst lifts it. Player-placed shriekers can't summon, so they never count.
- An Ancient City's Grounded skips creative and spectator players, as a Warden Subspace's does, and a city whose book never generated, such as a datapack layout's, Grounds nobody.
- A Subspace's rules for players keep working while its boss is unloaded, until the Subspace ends.
- Taking any structure book is refused, with nothing changed, when the player's inventory is full.
- A Remnant from a death in an uncollectable spot goes to the player's respawn point when no solid ground is recorded in that dimension.
- Remnant decay rounds the remaining points down at each death, so small Remnants run out. The table in The role of XP rounded to the nearest point, so it differs by 1 point after two deaths.
- Repair counts for items with no recipe: chainmail like iron, and 4 units for Elytra and tridents.
- A Blood Moon spot that can't fill yet (a player nearby, the spot blocked) stays pending past dawn and fills the next time it can.
- Attacking an Illager while wearing Orichalcum ends neutrality the way attacking a Piglin does with gold: the Illager you hit and those nearby that see it turn hostile.
- Structures with several possible enchantments choose between them with equal weight.
- Starting tuning values: Zephyr's second jump reaches 1.5, 2.25 and 3 blocks at levels I to III; Steel weapons add 0.5 attack knockback; Steel armor adds 0.1 knockback resistance per piece on top of Netherite's; the spawner forcefield's starting distance and colour.

### Values left to playtesting or build time

- Grounded's Ender Pearl gravity multiplier, which Mob changes left to playtesting.
- The Warden Subspace's radius and its sculk patch's size and strength, set by playtest game rules.
- The Wither Subspace's radius and light level, and the Wither's leash, set by playtest game rules.
- The Ancient City keeper ring: the 30-block minimum distance from the book and the 30° windows.
- The exact book spot in each structure other than the Stronghold, whose book is in the portal room, chosen in that structure's build ticket by the manifesto's rule: the structure's most important spot, fixed and visible. Buried Treasure, Trail Ruins, Abandoned Camps and Shipwrecks need a spot chosen with care.
- The Warden spawn search: the tickets say it widens "within the Subspace", which doesn't exist until the Warden spawns. This spec reads it as up to 50 blocks from the player.

### Unverified facts

Several tickets say their numbers were never tested in a world, and Brewing's vanilla durations came from memory, not the 26.3 jar. The first ticket in each area should confirm the vanilla facts it relies on.

### Glossary gaps

The floating structure book and the Reinforcement template have no glossary entry. Add them through `/domain-modeling` when the first build ticket names them.

### Credits and license

Mob numbers, the Crystal Heart recipe, Nether Star into 9 Pith, Dungeon frequency, village spacing and the heart system's shape come from Matcha Flavoured and need credit. Any reused Matcha asset must be listed in `CREDITS.md` with its original path, its author and whether it was changed. The mod stays under CC BY-NC-SA 4.0.
