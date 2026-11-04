# Plan: 12 Zephyr

## End goal

Zephyr boots give one extra jump in the air, a movement upgrade for the End before an Elytra. It must never change fall damage except through the Clutch. Fall damage counts the whole fall, from where it started to where the player lands, as if the jump never happened. The only discount is the Clutch: the distance fallen before the jump is cut by 10%, 15% or 25%, so a later jump saves more. The client feels the jump at once. The server decides what the jump does to fall damage, so only an equipped, once-per-air-time jump gets the Clutch. That is the extent of the guarantee: vanilla already trusts the client's movement and on-ground claims, so a modified client can skip fall damage without Zephyr, and that stays out of scope. Closing the ticket means the enchantment exists (books come from tickets 17 and 30). The jump works in a dev client, and GameTests pin the server's checks and fall-damage results with the spec's numbers.

## What vanilla does (sources, 26.3)

- The server takes player movement from move packets. `ServerGamePacketListenerImpl.handlePlayerPositionChange` calls `player.doCheckFallDamage(delta…, onGround)`. This goes to `ServerPlayer.checkFallDamage`, then `Entity.checkFallDamage`, which adds `-ya` while moving down and calls `fallOn` (the damage) and resets the fall distance on landing. After that, **`if (movedUpwards) player.resetFallDistance()`**. So on the server, any upward packet sets the fall distance to 0. With no changes, a mid-air jump would wipe the whole fall: a jump just before landing would cancel all fall damage. That is the opposite of the spec.
- Server players aren't local-authoritative, so `Entity.move` doesn't count fall distance for them. Only `doCheckFallDamage` does.
- Air physics per tick: move by `vy`, then `vy = (vy − gravity) × 0.98`, with gravity from the `GRAVITY` attribute (0.08).
- `LocalPlayer.aiStep` uses a jump press in the air to start Elytra gliding (`tryToStartFallFlying`) before anything we add.

## Decisions

1. **Enchantment.** `data/maple_flavored/enchantment/zephyr.json`: `supported_items #minecraft:enchantable/foot_armor`, slots `feet`, max level 3, weight 1, anvil cost 4, Feather Falling's costs. It has no effects and no exclusive set, so it works alongside Feather Falling. It is in no enchantment tags, so it comes from no vanilla table, loot or trade. Its sources come from later tickets. Lang: `enchantment.maple_flavored.zephyr` = "Zephyr".
2. **Heights.** Levels I to III give 1.5, 2.25 and 3.0 blocks. The launch speed is solved from that height and the player's `GRAVITY` attribute, using vanilla's per-tick air rule (bisection on a simulated rise). Upward speed becomes `max(launch, current vy)`, and horizontal speed is kept.
3. **Who decides what.** The client decides the motion. It is client-authoritative in vanilla, and only the client can make the jump feel instant. The server decides the fall distance, which is all that fall damage depends on. A request the server refuses still moves a cheating client, but the fall-damage change is skipped, so refusals never help the player.
4. **Can jump** (one shared predicate, used by both sides): alive, not a spectator, not riding, not on the ground, not in water or lava, not on a climbable, not creative-flying, not gliding, Zephyr level > 0 on the feet slot, and no Zephyr jump used yet this air time. Grounded (ticket 24) will add its check here.
5. **Air time.** It ends on the ground, in water or lava, on a climbable, while creative-flying, or while riding. Each side clears its own per-player "used" flag when air time ends. The flag lives in a non-persistent, non-synced Fabric attachment on the player. Each side keeps its own copy, matching its own view of the player.
6. **Fall-distance result (server).** On an accepted request, at the server's current position `jumpY` and fall distance `F`: `carried = F × (1 − clutch)`, where clutch is 0.10, 0.15 or 0.25 (and `F ≤ 0` is carried unchanged). The attachment moves to `Rising(jumpY, carried)`. In a `ServerPlayer.checkFallDamage` HEAD mixin, the first fall check while Rising that moves down (`ya < 0`) or that lands (a stationary move while hovering keeps Rising) sets `fallDistance = carried + (jumpY − startY)`, where `startY = getY() − ya` is where this move began. Vanilla then adds this move's drop and, on landing, applies damage. The state becomes Spent. Result: counted fall = `carried + (jumpY − landingY)`, exactly "the whole fall, minus the Clutch". The value can be negative while the player is above `jumpY`. That is credit for the regained height and is used up as they drop back past it. Vanilla's own resets (landing, water, upward pushes after the apex) still apply afterwards.
6a. **Teleports.** Any server teleport of the player (`ServerGamePacketListenerImpl.teleport`, which pearls, chorus fruit, `/tp` and dimension changes all go through) turns Rising into Spent, so the jump's fall rules never count movement across a teleport. The launch-speed solver stops after 200 ticks of rise, so gravity at or below zero can't stall the client.
7. **Request.** A serverbound unit payload `ZephyrJumpPayload`, sent from the client's END tick right after it applies the motion. Fabric runs play payload handlers on the server thread in packet order, so the request lands between the jump tick's move packet and the first upward one.
8. **Client input.** On `ClientTickEvents.END_CLIENT_TICK`, the jump key's rising edge (pressed now, not pressed last tick) while the player was airborne at the end of the last tick and still is, and while the predicate holds. If a press opened an Elytra glide that tick, `isFallFlying` refuses the jump, so the Elytra comes first. A press made on the ground is a vanilla jump, since "airborne last tick" is false. Holding the key from the ground never fires, since there is no rising edge.

## API sketch

```java
package mapleflavored.zephyr;

/** Zephyr: a boots enchantment whose mid-air jump keeps fall damage neutral, apart from its Clutch. */
public final class Zephyr {
	public static final ResourceKey<Enchantment> ENCHANTMENT;
	public static void register();                       // payload type, server receiver
	public static int level(Player player);              // Zephyr level on the feet, 0 without
	public static boolean canJump(Player player);        // decision 4, both sides
	public static double launchSpeed(Player player);     // decision 2
	public static void markJumped(Player player);        // client: spends the air time
	public static void tryJump(ServerPlayer player);     // server: checks, Clutch, Rising
	public static void onFallCheck(ServerPlayer player, double ya, boolean onGround); // mixin hook, decision 6
	public static void endAirTimeIfOver(Player player);  // both sides
}
```

The client's `ZephyrInput.register()` consumes `canJump`, `launchSpeed` and `markJumped`, then sends `ZephyrJumpPayload`. `ServerPlayerMixin` calls `onFallCheck`.

## Files

- `src/main/java/mapleflavored/zephyr/Zephyr.java`, `ZephyrJumpPayload.java`, `AirJump.java` (sealed state: Rising, Spent)
- `src/main/java/mapleflavored/mixin/ServerPlayerMixin.java` and an entry in `maple_flavored.mixins.json`
- `src/client/java/mapleflavored/client/zephyr/ZephyrInput.java`, registered in `MapleFlavoredClient`
- `src/main/resources/data/maple_flavored/enchantment/zephyr.json`, plus the lang entry
- `src/gametest/java/mapleflavored/gametest/ZephyrGameTests.java`, plus the entrypoint
- `CREDITS.md`: Zephyr's idea and name come from Matcha Flavoured (`data/matcha/enchantment/zephyr.json`)

## Tests (GameTests drive real move packets, one per tick, and the custom payload through `player.connection`)

A loaded mock player in survival wearing iron boots with Zephyr is teleported above a stone floor that sits over the test's enclosure. They fall in 1-block steps, one per tick (the server allows one position per client tick and measures each tick's movement). Fall damage is read as health lost.

- Baseline: a 10-block fall with no jump deals 7.
- Neutral jump: a jump at fall 0, 5 above the floor, rising to 8, deals 2 (vanilla would count 8, for 5).
- Clutch at each level: 20 blocks down, jump, rise by that level's height, land 4 below the jump point. 22, 21 and 19 counted give 19, 18 and 16.
- A pause while rising (a stationary move) still counts the whole fall: 13, for 10.
- Landing on a ledge above the jump point takes the regained height off: 9 − 2 = 7, for 4.
- Refused: no Zephyr (16 counted, for 13), on the ground (doesn't spend the air time), and a second jump in one air time (no second Clutch).
- Landing gives the jump back.
- Negative control: with the server hook disabled, every jump test fails and only the two vanilla baselines pass.

## Hand check (dev client)

Jump feel, heights I to III (count blocks against a pillar), one jump per air time, holding space from the ground doesn't fire, an Elytra press glides instead, and Clutch timing: a late jump from a deadly fall survives at III with Feather Falling.

## Out of scope

Grounded (24), the Dragon drop (30), the Library (14/15), particles and sounds (a jump sound is a hand-check nicety, left for playtest).
