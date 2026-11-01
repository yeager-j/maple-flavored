# Hearts and death

Type: grilling
Blocked by: 01
Status: resolved

## Question

What should Maple Flavored's heart system be: what you lose on death, what floor protects you and how it moves, what restores or raises hearts, and how much of Matcha's version carries over?

## Answer

Numbers are starting values that balance work can change. The rules are fixed. Terms are in `GLOSSARY.md`.

### Death

- Death costs hearts instead of items. Keep Inventory is on by default in new worlds. Being saved by a Crystal Heart or Totem isn't a death and costs nothing.
- Players start at 10 Max Hearts, and the cap is 30.
- Each player has a Heart Floor that starts at 10 and falls by 1 per Milestone, down to 3. Lowering it removes no hearts by itself.
- Milestones count in any order, as soon as the item is in the player's inventory, however it got there: copper ingot, iron ingot, diamond, netherite ingot, Eye of Ender. Entering the Nether and entering the End also count.
- Peaceful and Easy cost 1 heart per death with the Heart Floor fixed at 10. Normal costs 1 and Hard costs 2, both following Milestones. Hardcore stays vanilla.
- A plain chat message announces each Heart Floor drop, with no lore text, and the death screen shows the Max Hearts lost.

### Crystal Hearts and Pith

- A Crystal Heart is a registered item, consumed in about 1.6 seconds for +1 Max Heart. It's refused at the cap without being used up.
- Held in hand, it saves its holder from a lethal hit like a Totem, and is used up without adding a heart.
- It's crafted from 8 diamonds around 1 Pith. Pith is a registered item that drops from spawners, Elder Guardians, Piglin Brutes and Evokers. Evoker farms are fine because diamonds aren't renewable.
- Each player earns one Crystal Heart for their first kill of the Elder Guardian, Wither, Warden and Ender Dragon, and everyone nearby gets credit.
- Fishing never gives one. Chest odds belong to Structures, and trades belong to Getting villagers and removing villages.

### Withered Hearts

- Under the Wither effect, one heart becomes a Withered Heart every 5 seconds, down to a minimum of 1 usable heart. Max Hearts don't change, so death and Crystal Hearts act as normal.
- All return when the player is in the Overworld without the Wither effect, unless an enemy's aura blocks recovery. Death also clears them.
- They show as darkened hearts and persist through logging out.
