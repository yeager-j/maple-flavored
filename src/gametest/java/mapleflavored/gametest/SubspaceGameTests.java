package mapleflavored.gametest;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import mapleflavored.grounded.Grounded;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Warden Subspaces: every Warden opens a fixed 50-block Subspace where it appears, which darkens and Grounds the
 * players inside, outlasts its Warden unloading and ends when its Warden dies or digs down, each end saying what caused
 * it. The Warden is aware of everything inside, but only targets inside vanilla's awareness box keep it from digging
 * down.
 *
 * <p>A Subspace reaches far past the few blocks between tests, so each test runs alone, in its own environment's batch,
 * in a {@link SubspaceArena} high above the test grid that it first clears of whatever earlier batches left there.
 */
public class SubspaceGameTests {
	private static final double RADIUS = SubspaceArena.RADIUS;
	// The Subspace's effects last 60 ticks once a player stops being topped up.
	private static final int WEAR_OFF_TICKS = 61;
	// Shortened from the 1200 ticks a Warden spawns with, so a test can watch it dig down. A summoned Warden's first sniff
	// can end almost 200 ticks in, and a later one up to about 300 ticks after that, so this leaves room for a sniff to
	// hold it before it digs.
	private static final int DIG_COOLDOWN_TICKS = 600;
	// How long a Warden's digging animation lasts before it's removed.
	private static final int DIGGING_TICKS = 100;
	private static final int DARKNESS_FADE_TICKS = 22;

	private static final List<UUID> STARTED = new CopyOnWriteArrayList<>();
	private static final List<End> ENDED = new CopyOnWriteArrayList<>();

	static {
		Subspaces.STARTED.register((level, subspace) -> STARTED.add(subspace.boss()));
		Subspaces.ENDED.register((level, subspace, cause) -> ENDED.add(new End(subspace.boss(), cause)));
	}

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void playersInsideAreDarkenedAndGroundedUntilTheyLeave(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Warden warden = arena.warden(Vec3.ZERO);
		ServerPlayer inside = arena.player(new Vec3(49.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer creative = arena.player(new Vec3(49.0, 0.0, 2.0), GameType.CREATIVE);
		ServerPlayer outside = arena.player(new Vec3(0.0, 0.0, 51.0), GameType.SURVIVAL);
		List<Subspace> opened = Subspaces.containing(helper.getLevel(), arena.centre());

		helper.assertValueEqual(opened.size(), 1, "the Subspaces at the Warden's feet");
		assertOpenedBy(helper, opened.getFirst(), warden, arena.centre());

		helper.startSequence()
			.thenExecuteAfter(1, () -> {
				assertSubspaceEffects(helper, inside, true, "a player 49 blocks out");
				assertSubspaceEffects(helper, creative, false, "a creative player 49 blocks out");
				assertSubspaceEffects(helper, outside, false, "a player 51 blocks out");
				arena.moveTo(inside, new Vec3(51.0, 0.0, 0.0));
			})
			.thenExecuteAfter(WEAR_OFF_TICKS, () -> assertSubspaceEffects(helper, inside, false, "a player who left"))
			.thenSucceed();
	}

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 300)
	public void overlappingSubspacesEachEndOnTheirOwnAndNeverStackGrounded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Warden first = arena.warden(Vec3.ZERO);
		Warden second = arena.warden(new Vec3(60.0, 0.0, 0.0));
		ServerPlayer player = arena.player(new Vec3(30.0, 0.0, 0.0), GameType.SURVIVAL);

		helper.startSequence()
			// The second Warden's Subspace opens once its chunk, outside the test's own, has loaded.
			.thenWaitUntil(() -> helper.assertValueEqual(bossesAround(level, player.position()), List.of(first.getUUID(), second.getUUID()), "the Subspaces around the player"))
			.thenExecuteAfter(1, () -> {
				List<Subspace> around = Subspaces.containing(level, player.position());

				helper.assertFalse(around.get(0).id().equals(around.get(1).id()), "Two Subspaces share an id");
				helper.assertValueEqual(player.getEffect(Grounded.EFFECT).getAmplifier(), 0, "Grounded's amplifier inside both");
				first.kill(level);
				helper.assertValueEqual(bossesAround(level, player.position()), List.of(second.getUUID()), "the Subspaces once the first Warden dies");
			})
			.thenExecuteFor(WEAR_OFF_TICKS, () -> assertEffectsNeverRunningOut(helper, player, "a player still inside the second Subspace"))
			.thenExecute(() -> {
				second.kill(level);
				helper.assertValueEqual(bossesAround(level, player.position()), List.of(), "the Subspaces once both Wardens die");
			})
			.thenExecuteAfter(WEAR_OFF_TICKS, () -> {
				assertSubspaceEffects(helper, player, false, "a player once both Subspaces end");

				for (Warden warden : List.of(first, second)) {
					helper.assertValueEqual(Collections.frequency(STARTED, warden.getUUID()), 1, "starts of a Warden's Subspace");
					helper.assertValueEqual(endsOf(warden), List.of(Subspaces.EndCause.BOSS_DIED), "what ended a killed Warden's Subspace");
				}
			})
			.thenSucceed();
	}

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void aSubspaceOutlastsItsWardenUnloadingAndEndsWhenItDigsDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Warden warden = arena.warden(Vec3.ZERO);
		ServerPlayer player = arena.player(new Vec3(40.0, 0.0, 0.0), GameType.SURVIVAL);
		Subspace opened = Subspaces.containing(level, arena.centre()).getFirst();
		Vec3 wandered = arena.at(new Vec3(10.0, 0.0, 0.0));

		assertOpenedBy(helper, opened, warden, arena.centre());
		warden.snapTo(wandered.x, wandered.y, wandered.z);

		CompoundTag savedWarden = SubspaceArena.unload(helper, warden);

		helper.startSequence()
			// The effects arrive at the end of the first tick.
			.thenIdle(1)
			.thenExecuteFor(WEAR_OFF_TICKS, () -> assertEffectsNeverRunningOut(helper, player, "a player inside while the Warden is unloaded"))
			.thenExecute(() -> {
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(opened), "the Subspace while its Warden is unloaded");
				helper.assertValueEqual(savedAndLoaded(level), List.of(opened), "the level's Subspaces once saved and loaded");

				Entity reloaded = arena.track(SubspaceArena.load(helper, savedWarden));

				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(opened), "the Subspaces once the Warden reloads");

				// How a Warden is removed as it digs back down.
				reloaded.remove(Entity.RemovalReason.DISCARDED);
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(), "the Subspaces once it digs down");
				helper.assertValueEqual(endsOf(reloaded), List.of(Subspaces.EndCause.BOSS_REMOVED), "what ended the Subspace of a Warden that dug down");
			})
			.thenSucceed();
	}

	// Peaceful never loads a saved Warden again, so neither its death nor its removal can end its Subspace. Peaceful's own
	// batch is shared, so this test leaves the other tests' players alone.
	@GameTest(environment = "maple_flavored_gametest:peaceful", maxTicks = 100)
	public void peacefulEndsTheSubspaceOfAWardenItWillNeverLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.shared(helper);
		ServerPlayer player = arena.player(new Vec3(20.0, 0.0, 0.0), GameType.SURVIVAL);
		Warden warden = new Warden(EntityTypes.WARDEN, level);

		warden.snapTo(arena.centre().x, arena.centre().y, arena.centre().z);
		level.addFreshEntity(warden);
		helper.assertValueEqual(Subspaces.containing(level, arena.centre()).size(), 1, "the Subspaces as the Warden appears");

		// Unloaded before it ticks, as a Warden saved before the world turned Peaceful would be.
		SubspaceArena.unload(helper, warden);

		helper.startSequence()
			.thenExecuteAfter(WEAR_OFF_TICKS, () -> {
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(), "the Subspaces in Peaceful");
				helper.assertValueEqual(endsOf(warden), List.of(Subspaces.EndCause.PEACEFUL), "what ended the Subspace in Peaceful");
				assertSubspaceEffects(helper, player, false, "a player inside in Peaceful");
			})
			.thenSucceed();
	}

	// A boss that will never load again leaves its Subspace open, and nothing but Peaceful ends it on its own.
	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 20)
	public void endingAbandonedSubspacesEndsOnlyThoseWhoseBossIsntLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Warden unloaded = arena.stillWarden(Vec3.ZERO);
		Warden loaded = arena.stillWarden(new Vec3(4.0, 0.0, 0.0));

		SubspaceArena.unload(helper, unloaded);
		helper.assertValueEqual(bossesAround(level, arena.centre()), List.of(unloaded.getUUID(), loaded.getUUID()), "the Subspaces before any is ended");

		Subspaces.endAbandoned(level);

		helper.assertValueEqual(bossesAround(level, arena.centre()), List.of(loaded.getUUID()), "the Subspaces once the abandoned ones are ended");
		helper.assertValueEqual(endsOf(unloaded), List.of(Subspaces.EndCause.ABANDONED), "what ended the unloaded Warden's Subspace");
		helper.assertValueEqual(endsOf(loaded), List.of(), "what ended the loaded Warden's Subspace");
		helper.succeed();
	}

	// Every target starts outside vanilla's awareness box, about 24 blocks around the Warden, so the Warden can only know
	// of them through its Subspace. A player comes first even when a mob is closer.
	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 600)
	public void theWardenGoesForTheNearestPlayerAnywhereInItsSubspace(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);

		arena.floor(-50, 50);

		Warden warden = arena.warden(Vec3.ZERO);

		ServerPlayer nearer = arena.player(new Vec3(35.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer farther = arena.player(new Vec3(-45.0, 0.0, 0.0), GameType.SURVIVAL);
		double startingDistanceToFarther = warden.distanceTo(farther);

		arena.stillPig(new Vec3(-30.0, 0.0, 0.0));

		helper.succeedWhen(() -> {
			helper.assertTrue(warden.distanceTo(nearer) <= 25.0, "the Warden closed on the nearer player, " + warden.distanceTo(nearer) + " blocks away");
			helper.assertTrue(warden.distanceTo(farther) > startingDistanceToFarther, "the Warden moved away from the farther player");
			helper.assertValueEqual(warden.getAngerManagement().getActiveAnger(nearer), 0, "the Warden's anger at a player it hasn't sniffed up close");
		});
	}

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 600)
	public void theWardenIsAwareOfMobsAnywhereInItsSubspace(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);

		arena.floor(-10, 50);

		Warden warden = arena.warden(Vec3.ZERO);
		Pig pig = arena.stillPig(new Vec3(40.0, 0.0, 0.0));

		helper.succeedWhen(() -> helper.assertTrue(warden.distanceTo(pig) <= 30.0, "the Warden closed on the pig, " + warden.distanceTo(pig) + " blocks away"));
	}

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 1100)
	public void aTargetKnownOnlyThroughTheSubspaceDoesntKeepTheWardenFromDiggingDown(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);

		arena.pen();

		Warden warden = arena.warden(Vec3.ZERO);


		shortenDigCooldown(warden);
		arena.player(new Vec3(40.0, 0.0, 0.0), GameType.SURVIVAL);

		helper.succeedWhen(() -> {
			helper.assertTrue(warden.isRemoved(), "the Warden dug down");
			helper.assertValueEqual(Subspaces.containing(helper.getLevel(), arena.centre()), List.of(), "the Subspaces once it dug down");
		});
	}

	// The Warden's hitbox reaches 0.45 blocks out, and vanilla's box 24 blocks past that. A player's hitbox reaches 0.3
	// blocks back from where they stand, so a player 24.68 blocks out overlaps the box by 0.07, more than the 0.05 the
	// Warden can shift within its pen. The centre of the block they stand in, 25 blocks out, lies outside the box.
	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 1000)
	public void aTargetJustInsideVanillasAwarenessBoxKeepsTheWardenFromDiggingDown(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);

		arena.pen();

		Warden warden = arena.warden(Vec3.ZERO);


		shortenDigCooldown(warden);
		arena.player(new Vec3(24.68, 0.0, 0.0), GameType.SURVIVAL);

		helper.startSequence()
			.thenExecuteAfter(DIG_COOLDOWN_TICKS + DIGGING_TICKS + 100, () -> {
				helper.assertFalse(warden.isRemoved(), "the Warden is still here");
				helper.assertValueEqual(Subspaces.containing(helper.getLevel(), arena.centre()).size(), 1, "the Subspaces while it stays");
			})
			.thenSucceed();
	}

	private static void assertOpenedBy(GameTestHelper helper, Subspace subspace, Warden warden, Vec3 centre) {
		helper.assertValueEqual(subspace.boss(), warden.getUUID(), "the Subspace's boss");
		helper.assertValueEqual(subspace.kind(), Subspace.Kind.WARDEN, "the Subspace's kind");
		helper.assertValueEqual(subspace.centre(), centre, "the Subspace's centre");
		helper.assertValueEqual(subspace.radius(), RADIUS, "the Subspace's radius");
	}

	private static void assertSubspaceEffects(GameTestHelper helper, ServerPlayer player, boolean expected, String who) {
		helper.assertValueEqual(player.hasEffect(MobEffects.DARKNESS), expected, "whether " + who + " has Darkness");
		helper.assertValueEqual(player.hasEffect(Grounded.EFFECT), expected, "whether " + who + " is Grounded");
	}

	// Checked every tick: Darkness fades out over its last 22 ticks, so it must never get that close to ending while the
	// player stays inside.
	private static void assertEffectsNeverRunningOut(GameTestHelper helper, ServerPlayer player, String who) {
		assertSubspaceEffects(helper, player, true, who);
		helper.assertFalse(player.getEffect(MobEffects.DARKNESS).endsWithin(DARKNESS_FADE_TICKS), "whether the Darkness of " + who + " is fading");
		helper.assertFalse(player.getEffect(Grounded.EFFECT).endsWithin(DARKNESS_FADE_TICKS), "whether " + who + " is about to stop being Grounded");
	}

	private static List<UUID> bossesAround(ServerLevel level, Vec3 position) {
		return Subspaces.containing(level, position).stream().map(Subspace::boss).toList();
	}

	private static List<Subspaces.EndCause> endsOf(Entity boss) {
		return ENDED.stream().filter(end -> end.boss().equals(boss.getUUID())).map(End::cause).toList();
	}

	private static void shortenDigCooldown(Warden warden) {
		warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, DIG_COOLDOWN_TICKS);
	}

	// The level's Subspaces, saved as the world saves them and read back as reopening the world would.
	private static List<Subspace> savedAndLoaded(ServerLevel level) {
		Codec<List<Subspace>> codec = Subspace.CODEC.listOf();
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Tag saved = codec.encodeStart(ops, Subspaces.inLevel(level)).getOrThrow();

		return codec.parse(ops, saved).getOrThrow();
	}

	private record End(UUID boss, Subspaces.EndCause cause) {
	}
}
