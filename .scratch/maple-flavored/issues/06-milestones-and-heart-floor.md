# 06: Milestones and Heart Floor

**What to build:** On Normal and Hard, each Milestone a player holds lowers their Heart Floor by 1, down to 3. Milestones are advancements: four vanilla ones and three new ones.

**Blocked by:** 05 (Max Hearts and death cost)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] New Milestone advancements: get a copper ingot (Story, under Stone Age; Getting an Upgrade moves under it), get a Netherite Ingot (Nether, under Hidden in the Depths; Cover Me in Debris moves under it), get an Eye of Ender (Story, under We Need to Go Deeper; Eye Spy moves under it)
- [x] Item Milestones count as soon as the item is in the inventory, however it got there
- [x] Acquire Hardware, Diamonds!, We Need to Go Deeper and The End? and the three new ones start their description with a red broken heart, as Matcha Flavoured marks its Milestones
- [x] Heart Floor is 10 minus Milestones held, minimum 3, on Normal and Hard; 10 on Peaceful and Easy; revoking a Milestone raises it again
- [x] A red chat message announces each new floor: "[Milestone] Your Heart Floor is now **9**"
- [x] GameTests cover granting, revoking and difficulty, and a death after Milestones
- [x] A GameTest covers a death with Max Hearts below the Heart Floor (after a revoked Milestone or a switch to Easy): it costs nothing and never raises Max Hearts
- [ ] Hand check: the floor message in chat after a Milestone and after switching Easy to Normal, and the Milestone descriptions in the advancement screen
