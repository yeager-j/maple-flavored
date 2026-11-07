package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.playerReadingChat;

import java.util.ArrayList;
import java.util.List;
import mapleflavored.bloodmoons.BloodMoonState;
import mapleflavored.bloodmoons.BloodMoons;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Blood Moon nights: which nights are Blood Moons, the spores in the air, the one chat message in every dimension, the
 * advancement for being in the Overworld as one rises, and announcing each Blood Moon once, across restarts and jumps in
 * time.
 *
 * <p>Each test moves the Overworld clock itself, so it doesn't depend on time passing in the test server. Each has its
 * own environment, which puts it in a batch of its own and restores the clock afterwards. The Overworld remembers which
 * Blood Moon last rose, whichever test raised it, so each test uses Blood Moons no other test does.
 */
public class BloodMoonGameTests {
	private static final int TICKS_PER_DAY = 24_000;
	private static final int FIRST_BLOOD_MOON_DAY = 24;
	private static final int DAYS_BETWEEN_BLOOD_MOONS = 32;
	// A thousand ticks before sunset, when a Blood Moon would start to rise.
	private static final int EVE = 11_000;
	// Two hundred ticks after sunset, once the Blood Moon has begun to rise.
	private static final int EARLY_NIGHT = 12_200;
	private static final int MIDNIGHT = 18_000;
	private static final int SETTLE_TICKS = 5;
	private static final String MESSAGE = "chat.maple_flavored.blood_moon_rises";
	private static final String ADVANCEMENT = "maple_flavored:adventure/blood_moon";

	// Its environment starts the clock at day 0, far from any Blood Moon, and the test sets the eve itself once its
	// players are in. The clock runs while a test waits for its structure to load, which can take long enough to pass the
	// eve, and a Blood Moon that rose before the players joined wouldn't rise again for them.
	@GameTest(environment = "maple_flavored_gametest:blood_moon_first", maxTicks = 100)
	public void aBloodMoonRisesOnTheFourthFullMoonWithOneMessageInEveryDimension(GameTestHelper helper) {
		List<Component> overworldChat = new ArrayList<>();
		List<Component> netherChat = new ArrayList<>();
		ServerPlayer overworldPlayer = playerReadingChat(helper, overworldChat);
		ServerPlayer netherPlayer = playerReadingChat(helper, netherChat);
		List<Float> earlyDensity = new ArrayList<>();

		moveToNether(helper, netherPlayer);
		setTime(helper, bloodMoonDay(0), EVE);

		helper.startSequence()
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(bloodMoonMessages(overworldChat), 0L, "Blood Moon messages before day 24's night");
				setTime(helper, bloodMoonDay(0), EARLY_NIGHT);
			})
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(bloodMoonMessages(overworldChat), 1L, "Blood Moon messages in the Overworld");
				helper.assertValueEqual(bloodMoonMessages(netherChat), 1L, "Blood Moon messages in the Nether");
				helper.assertTrue(hasAdvancement(helper, overworldPlayer), "the Overworld player has the advancement");
				helper.assertFalse(hasAdvancement(helper, netherPlayer), "the Nether player has the advancement");
			})
			.thenExecuteAfter(20, () -> {
				helper.assertTrue(BloodMoons.strength(helper.getLevel()) > 0.0F, "the Blood Moon is still up");
				helper.assertTrue(sporeDensity(helper) > 0.0F, "crimson spores drift in the Overworld air");
				helper.assertValueEqual(bloodMoonMessages(overworldChat), 1L, "Blood Moon messages once it has risen");
				earlyDensity.add(sporeDensity(helper));
				setTime(helper, bloodMoonDay(0), MIDNIGHT);
			})
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				float midnightDensity = sporeDensity(helper);

				helper.assertTrue(midnightDensity > 3 * earlyDensity.getFirst(), "spores are much denser at midnight than as it rises");
				helper.assertValueEqual(bloodMoonMessages(overworldChat), 1L, "Blood Moon messages at its midnight");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:blood_moon_ordinary_full_moon", maxTicks = 100)
	public void anEarlierFullMoonIsNoBloodMoon(GameTestHelper helper) {
		List<Component> chat = new ArrayList<>();
		ServerPlayer player = playerReadingChat(helper, chat);

		setTime(helper, 16, EARLY_NIGHT);

		helper.startSequence()
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(BloodMoons.strength(helper.getLevel()), 0.0F, "Blood Moon strength on day 16's night");
				helper.assertValueEqual(bloodMoonMessages(chat), 0L, "Blood Moon messages on day 16's night");
				helper.assertValueEqual(sporeDensity(helper), 0.0F, "crimson spore density on day 16's night");
				helper.assertFalse(hasAdvancement(helper, player), "the player has the advancement");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:blood_moon_back_to_back", maxTicks = 100)
	public void theNextBloodMoonRises32DaysLaterEvenWhenTimeJumpsStraightToIt(GameTestHelper helper) {
		List<Component> chat = new ArrayList<>();
		playerReadingChat(helper, chat);
		List<Component> laterChat = new ArrayList<>();
		List<ServerPlayer> latecomer = new ArrayList<>();

		setTime(helper, bloodMoonDay(20), EARLY_NIGHT);

		helper.startSequence()
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(bloodMoonMessages(chat), 1L, "Blood Moon messages on the first night");
				setTime(helper, bloodMoonDay(21), EARLY_NIGHT);
			})
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(bloodMoonMessages(chat), 2L, "Blood Moon messages by the next one");
				// Joined after the last rising, so only the next one can credit them.
				latecomer.add(playerReadingChat(helper, laterChat));
				helper.assertFalse(hasAdvancement(helper, latecomer.getFirst()), "a latecomer has the advancement before a rising");
				setTime(helper, bloodMoonDay(22), EARLY_NIGHT);
			})
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(bloodMoonMessages(laterChat), 1L, "the latecomer's Blood Moon messages by the one after");
				helper.assertTrue(hasAdvancement(helper, latecomer.getFirst()), "the latecomer has the advancement");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:blood_moon_restart", maxTicks = 100)
	public void aRestartDuringABloodMoonDoesNotAnnounceItAgain(GameTestHelper helper) {
		List<Component> chat = new ArrayList<>();
		playerReadingChat(helper, chat);

		setTime(helper, bloodMoonDay(10), EARLY_NIGHT);

		helper.startSequence()
			.thenExecuteAfter(SETTLE_TICKS, () -> {
				helper.assertValueEqual(bloodMoonMessages(chat), 1L, "Blood Moon messages as it rises");
				reloadBloodMoonState(helper.getLevel().getServer().overworld());
			})
			.thenExecuteAfter(SETTLE_TICKS, () -> helper.assertValueEqual(bloodMoonMessages(chat), 1L, "Blood Moon messages after a restart"))
			.thenSucceed();
	}

	// Saves the Overworld's Blood Moon state and loads it back in its place, as a restart would.
	private static void reloadBloodMoonState(ServerLevel overworld) {
		RegistryOps<Tag> ops = overworld.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		BloodMoonState live = overworld.getDataStorage().computeIfAbsent(BloodMoonState.TYPE);
		Tag saved = BloodMoonState.TYPE.codec().encodeStart(ops, live).getOrThrow();

		overworld.getDataStorage().set(BloodMoonState.TYPE, BloodMoonState.TYPE.codec().parse(ops, saved).getOrThrow());
	}

	// The day of the given Blood Moon, counting the first as 0.
	private static int bloodMoonDay(int bloodMoon) {
		return FIRST_BLOOD_MOON_DAY + bloodMoon * DAYS_BETWEEN_BLOOD_MOONS;
	}

	// Sets the Overworld clock to the given time of the given day, counting from day 0.
	private static void setTime(GameTestHelper helper, int day, int timeOfDay) {
		MinecraftServer server = helper.getLevel().getServer();
		var overworldClock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);

		server.clockManager().setTotalTicks(overworldClock, (long) day * TICKS_PER_DAY + timeOfDay);
	}

	private static void moveToNether(GameTestHelper helper, ServerPlayer player) {
		var nether = helper.getLevel().getServer().getLevel(Level.NETHER);

		player.teleport(new TeleportTransition(nether, new Vec3(0.5, 100.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
	}

	private static long bloodMoonMessages(List<Component> chat) {
		return chat.stream()
			.filter(message -> message.getContents() instanceof TranslatableContents contents && contents.getKey().equals(MESSAGE))
			.count();
	}

	// How likely crimson spores are to appear around the test, which is underground, where only the air shows a Blood Moon.
	private static float sporeDensity(GameTestHelper helper) {
		List<AmbientParticle> particles = helper.getLevel()
			.environmentAttributes()
			.getValue(EnvironmentAttributes.AMBIENT_PARTICLES, helper.absolutePos(BlockPos.ZERO));

		return (float) particles.stream()
			.filter(particle -> particle.particle().getType() == ParticleTypes.CRIMSON_SPORE)
			.mapToDouble(AmbientParticle::probability)
			.sum();
	}

	private static boolean hasAdvancement(GameTestHelper helper, ServerPlayer player) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(ADVANCEMENT));

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
