# 06: Milestones and Heart Floor

**What to build:** On Normal and Hard, each Milestone a player holds lowers their Heart Floor by 1, down to 3. Milestones are advancements: four vanilla ones and three new ones.

**Blocked by:** 05 (Max Hearts and death cost)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] New Milestone advancements: get a copper ingot (Story, under Stone Age; Getting an Upgrade moves under it), get a Netherite Ingot (Nether, under Hidden in the Depths; Cover Me in Debris moves under it), get an Eye of Ender (Story, under We Need to Go Deeper; Eye Spy moves under it)
- [ ] Item Milestones count as soon as the item is in the inventory, however it got there
- [ ] Acquire Hardware, Diamonds!, We Need to Go Deeper and The End? and the three new ones end their description with "Milestone: lowers your Heart Floor by 1 on Normal and Hard"
- [ ] Heart Floor is 10 minus Milestones held, minimum 3, on Normal and Hard; 10 on Peaceful and Easy; revoking a Milestone raises it again
- [ ] A plain chat message announces each new floor
- [ ] GameTests cover granting, revoking and difficulty, and a death after Milestones
