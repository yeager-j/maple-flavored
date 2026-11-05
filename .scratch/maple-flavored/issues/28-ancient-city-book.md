# 28: Ancient City book

**What to build:** The Ancient City's Swift Sneak book sits in front of the portal, and taking it always summons an angry Warden.

**Blocked by:** 18 (Structure book framework, with Mineshafts), 27 (Warden summoning in Warden Territory)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder. Plan: `../plans/28-ancient-city-book.md`.

- [x] The book sits in the centre piece in front of the portal, with room for a Warden
- [x] Taking it finds a Warden spot through the summoning rules, skipping shrieker level and nearby-Warden checks, else uses the book's spot, rechecked at that moment; if neither is free, the book stays and the player gets a short message
- [x] The Warden is angry and opens its Subspace
- [x] GameTests cover taking the book, a blocked search and a fully blocked spot
- [ ] Hand check: in an Ancient City, the book floats in front of the portal; taking it brings a Warden out of the ground near you that comes straight for you
