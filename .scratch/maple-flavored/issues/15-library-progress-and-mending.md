# 15: Library Progress and Mending

**What to build:** The table shows how close its Library is to Full, and offers Mending only while the Library is Full.

**Blocked by:** 14 (Library and checklist table), 12 (Zephyr), 13 (Blight and removed curses)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Library Progress is the sum of the highest shelved level per enchantment over 130, read live
- [ ] The table shows it as a progress bar through menu data
- [ ] Mending is offered only while all 42 other enchantments, including Zephyr and Blight, are shelved at max; moving books moves access; gear with Mending keeps it until it is next saved without a Full Library
- [ ] Advancement: Full Library (Story goal, under Enchanter)
- [ ] GameTests cover progress counting (two level I books count 1; I and II count 2) and Mending appearing and disappearing with the full roster
- [ ] Hand check: the progress bar
