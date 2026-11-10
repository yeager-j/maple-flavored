package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.dieAndRespawn;
import static mapleflavored.gametest.MockPlayers.logIn;
import static mapleflavored.gametest.MockPlayers.logOut;
import static mapleflavored.gametest.MockPlayers.playerWithMaxHearts;
import static mapleflavored.gametest.MockPlayers.runCommand;

import java.util.List;
import java.util.Optional;
import mapleflavored.experience.ExperiencePoints;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.hearts.Hearts;
import mapleflavored.remnants.Remnant;
import mapleflavored.remnants.Remnants;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Remnants: death drops all XP as a Remnant only its owner can see and collect, which waits through logouts and
 * unloads, shrinks with each later death and moves to safe ground when the death spot can't be reached.
 *
 * <p>Mock players start with no XP, so their points after collecting are exactly the points collected. Points are read
 * from level and progress, which is what a player sees and spends.
 */
public class RemnantsGameTests {
	private static final String COLLECT_ADVANCEMENT = "maple_flavored:adventure/collect_remnant";
	private static final BlockPos DEATH_SPOT = new BlockPos(2, 2, 2);
	private static final BlockPos SOLID_GROUND = new BlockPos(5, 2, 5);
	private static final BlockPos RESPAWN_POINT = new BlockPos(7, 2, 2);
	private static final int WAIT_TICKS = 20;

	@GameTest
	public void deathDropsAllXpAsARemnantThatItsOwnerCollects(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);
		Remnant remnant = onlyRemnant(helper);

		helper.assertValueEqual(ExperiencePoints.total(respawned), 0, "XP after respawn");
		helper.assertValueEqual(respawned.experienceLevel, 0, "level after respawn");
		helper.assertValueEqual(remnant.blockPosition(), helper.absolutePos(DEATH_SPOT), "the Remnant's spot");

		collect(helper, respawned, remnant, () -> {
			helper.assertValueEqual(ExperiencePoints.total(respawned), 100, "XP collected");
			helper.assertTrue(isDone(helper, respawned, COLLECT_ADVANCEMENT), "collecting grants the advancement");
		});
	}

	@GameTest
	public void aRemnantHoldsLevelsSetByCommandAsPoints(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 0);

		// Levels set this way never reach totalExperience. 40 levels are 2,920 points, plus 10 more.
		runCommand(helper, player, "xp set @s 40 levels");
		runCommand(helper, player, "xp add @s 10 points");
		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		collect(helper, respawned, onlyRemnant(helper), () -> helper.assertValueEqual(ExperiencePoints.total(respawned), 2930, "XP collected"));
	}

	@GameTest
	public void aRemnantHoldsLevelsAtTheXpCurvesBends(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 0);

		// Level 16 is 352 points and level 31 is 1,507, where the points per level step up.
		runCommand(helper, player, "xp set @s 16 levels");
		standAt(helper, player, DEATH_SPOT);

		ServerPlayer afterFirstDeath = dieAndRespawn(helper, player);

		runCommand(helper, afterFirstDeath, "xp set @s 31 levels");
		standAt(helper, afterFirstDeath, SOLID_GROUND);

		ServerPlayer afterSecondDeath = dieAndRespawn(helper, afterFirstDeath);
		Remnant level16 = remnantAt(helper, DEATH_SPOT);
		Remnant level31 = remnantAt(helper, SOLID_GROUND);

		// The second death cuts the first Remnant to 352 * 80% = 281 points.
		moveOnto(afterSecondDeath, level16);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(level16.isAlive(), "the first Remnant was collected"))
			.thenExecute(() -> {
				helper.assertValueEqual(ExperiencePoints.total(afterSecondDeath), 281, "XP from the first Remnant");
				moveOnto(afterSecondDeath, level31);
			})
			.thenWaitUntil(() -> helper.assertFalse(level31.isAlive(), "the second Remnant was collected"))
			.thenExecute(() -> helper.assertValueEqual(ExperiencePoints.total(afterSecondDeath), 281 + 1507, "XP from both Remnants"))
			.thenSucceed();
	}

	@GameTest
	public void collectingARemnantLandsExactlyOnLevelThirty(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 0);

		// Level 29 is 1,288 points, so the Remnant holds 1,385.
		runCommand(helper, player, "xp set @s 29 levels");
		runCommand(helper, player, "xp set @s 97 points");
		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		// Level 1 is 7 points, and 10 + 1,385 is the 1,395 of level 30, where the first Enchantment Slot opens. Added
		// level by level in floats, as vanilla adds XP, these points stop a rounding error short, at level 29.
		runCommand(helper, respawned, "xp set @s 1 levels");
		runCommand(helper, respawned, "xp set @s 3 points");

		collect(helper, respawned, onlyRemnant(helper), () -> {
			helper.assertValueEqual(respawned.experienceLevel, 30, "level after collecting");
			helper.assertValueEqual(respawned.experienceProgress, 0.0F, "progress after collecting");
		});
	}

	@GameTest(environment = "maple_flavored_gametest:keep_inventory_off")
	public void deathWithKeepInventoryOffDropsARemnantAndNoOrbs(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertValueEqual(ExperiencePoints.total(respawned), 0, "XP after respawn");
		helper.assertValueEqual(helper.getEntities(Remnants.REMNANT).size(), 1, "Remnants");
		helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void onlyTheOwnerSeesAndCollectsTheirRemnant(GameTestHelper helper) {
		ServerPlayer owner = playerWithXp(helper, 100);
		ServerPlayer other = playerWithMaxHearts(helper, 10);

		standAt(helper, owner, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, owner);
		Remnant remnant = onlyRemnant(helper);

		helper.assertTrue(remnant.broadcastToPlayer(respawned), "the owner is sent the Remnant");
		helper.assertFalse(remnant.broadcastToPlayer(other), "another player isn't sent the Remnant");

		moveOnto(other, remnant);

		helper.startSequence()
			.thenExecuteAfter(WAIT_TICKS, () -> {
				helper.assertValueEqual(ExperiencePoints.total(other), 0, "the other player's XP");
				helper.assertTrue(remnant.isAlive(), "the Remnant is still there");
				moveOnto(respawned, remnant);
			})
			.thenWaitUntil(() -> helper.assertFalse(remnant.isAlive(), "the owner collected the Remnant"))
			.thenExecute(() -> helper.assertValueEqual(ExperiencePoints.total(respawned), 100, "XP collected"))
			.thenSucceed();
	}

	@GameTest
	public void deathAtZeroXpMakesNoRemnant(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 0);

		standAt(helper, player, DEATH_SPOT);
		dieAndRespawn(helper, player);

		helper.assertEntityNotPresent(Remnants.REMNANT);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aTotemSaveMakesNoRemnant(GameTestHelper helper) {
		assertSaveMakesNoRemnant(helper, Items.TOTEM_OF_UNDYING);
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aCrystalHeartSaveMakesNoRemnant(GameTestHelper helper) {
		assertSaveMakesNoRemnant(helper, Hearts.CRYSTAL_HEART);
	}

	@GameTest
	public void keepXpIsOffByDefault(GameTestHelper helper) {
		helper.assertFalse(helper.getLevel().getGameRules().get(MapleGameRules.KEEP_XP), "Keep XP is off");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:keep_xp")
	public void keepXpKeepsAllXpAndMakesNoRemnant(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertValueEqual(ExperiencePoints.total(respawned), 100, "XP after respawn");
		helper.assertValueEqual(respawned.experienceLevel, 7, "level after respawn");
		helper.assertEntityNotPresent(Remnants.REMNANT);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:keep_xp_keep_inventory_off")
	public void keepXpKeepsXpWhileKeepInventoryOffDropsItems(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		player.getInventory().add(new ItemStack(Items.DIAMOND));
		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertValueEqual(ExperiencePoints.total(respawned), 100, "XP after respawn");
		helper.assertFalse(respawned.getInventory().contains(new ItemStack(Items.DIAMOND)), "the diamond is dropped");
		helper.assertEntityNotPresent(Remnants.REMNANT);
		helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void eachLaterDeathCutsEveryRemnantRoundingDownEachTime(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 8);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer afterFirstDeath = dieAndRespawn(helper, player);

		afterFirstDeath.giveExperiencePoints(10);
		standAt(helper, afterFirstDeath, SOLID_GROUND);

		ServerPlayer afterSecondDeath = dieAndRespawn(helper, afterFirstDeath);
		ServerPlayer afterThirdDeath = dieAndRespawn(helper, afterSecondDeath);
		Remnant older = remnantAt(helper, DEATH_SPOT);
		Remnant newer = remnantAt(helper, SOLID_GROUND);

		moveOnto(afterThirdDeath, older);

		// The older Remnant goes 8, 6, 4; rounding only at the end would leave 5. The newer goes 10, 8.
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(older.isAlive(), "the older Remnant was collected"))
			.thenExecute(() -> {
				helper.assertValueEqual(ExperiencePoints.total(afterThirdDeath), 4, "XP from the older Remnant");
				moveOnto(afterThirdDeath, newer);
			})
			.thenWaitUntil(() -> helper.assertFalse(newer.isAlive(), "the newer Remnant was collected"))
			.thenExecute(() -> helper.assertValueEqual(ExperiencePoints.total(afterThirdDeath), 4 + 8, "XP from both Remnants"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void laterDeathsCutAnUnloadedRemnantEvenInAnotherDimension(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);
		CompoundTag savedRemnant = unload(helper, onlyRemnant(helper));

		moveTo(respawned, helper.getLevel().getServer().getLevel(Level.NETHER), new Vec3(0.5, 100.0, 0.5));

		ServerPlayer afterNetherDeath = dieAndRespawn(helper, respawned);
		ServerPlayer afterSecondDeath = dieAndRespawn(helper, afterNetherDeath);
		Remnant reloaded = load(helper, savedRemnant);

		// 100 points, then 80% rounded down after each of the 2 later deaths.
		collect(helper, afterSecondDeath, reloaded, () -> helper.assertValueEqual(ExperiencePoints.total(afterSecondDeath), 64, "XP collected"));
	}

	@GameTest(maxTicks = 100)
	public void aOnePointRemnantRunsOutOnTheNextDeath(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 1);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);
		Remnant remnant = onlyRemnant(helper);

		dieAndRespawn(helper, respawned);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(remnant.isAlive(), "the Remnant ran out"))
			.thenSucceed();
	}

	@GameTest
	public void aVoidDeathSendsTheRemnantToTheLastSolidGround(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, SOLID_GROUND);

		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				fallIntoTheVoid(helper, player);
				assertRemnantMovedTo(helper, dieAndRespawn(helper, player), helper.absolutePos(SOLID_GROUND));
			})
			.thenSucceed();
	}

	@GameTest
	public void aLavaDeathSendsTheRemnantToTheLastSolidGround(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, SOLID_GROUND);

		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				helper.setBlock(DEATH_SPOT, Blocks.LAVA);
				standAt(helper, player, DEATH_SPOT);
				assertRemnantMovedTo(helper, dieAndRespawn(helper, player), helper.absolutePos(SOLID_GROUND));
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void aRemnantMovedToASlabCanBeCollectedFromTheSlab(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standOnSlab(helper, player, SOLID_GROUND);

		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				fallIntoTheVoid(helper, player);

				ServerPlayer respawned = dieAndRespawn(helper, player);

				standOnSlab(helper, respawned, SOLID_GROUND);
			})
			.thenWaitUntil(() -> helper.assertEntityNotPresent(Remnants.REMNANT))
			.thenSucceed();
	}

	@GameTest
	public void groundThatFilledWithLavaSendsTheRemnantToTheRespawnPoint(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, SOLID_GROUND);
		setRespawnPoint(player, helper.absolutePos(RESPAWN_POINT));

		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				helper.setBlock(SOLID_GROUND, Blocks.LAVA);
				fallIntoTheVoid(helper, player);
				assertRemnantMovedTo(helper, dieAndRespawn(helper, player), helper.absolutePos(RESPAWN_POINT));
			})
			.thenSucceed();
	}

	@GameTest
	public void aVoidDeathWithNoSolidGroundSendsTheRemnantToTheRespawnPoint(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		setRespawnPoint(player, helper.absolutePos(RESPAWN_POINT));
		fallIntoTheVoid(helper, player);

		assertRemnantMovedTo(helper, dieAndRespawn(helper, player), helper.absolutePos(RESPAWN_POINT));
		helper.succeed();
	}

	// The far respawn point's chunk has to generate, which takes wall-clock time while the server keeps ticking, so the
	// test waits for the chunk itself once the owner is back.
	@GameTest(maxTicks = 1200)
	public void aRemnantBoundForAnUnloadedChunkSurvivesItsOwnerLoggingOut(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);
		BlockPos farRespawnPoint = TestSites.of(helper).chunk(0).getMiddleBlockPosition(helper.getLevel().getMaxY() - 10);

		setRespawnPoint(player, farRespawnPoint);
		fallIntoTheVoid(helper, player);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		// The far chunk's entities load over the next ticks, so the Remnant is still waiting when its owner leaves.
		logOut(helper, respawned);

		helper.startSequence()
			.thenExecuteAfter(WAIT_TICKS, () -> {
				ServerPlayer loggedIn = logIn(helper, respawned.getGameProfile());

				ForcedChunks.keepLoaded(helper, helper.getLevel(), List.of(ChunkPos.containing(farRespawnPoint)));

				// Back at the respawn point, the owner collects the Remnant as soon as it arrives there.
				helper.startSequence()
					.thenWaitUntil(() -> helper.assertValueEqual(ExperiencePoints.total(loggedIn), 100, "XP collected at the respawn point"))
					.thenSucceed();
			});
	}

	@GameTest(maxTicks = 100)
	public void aRemnantIgnoresDamageAndWaitsForItsOwnerToLogBackIn(GameTestHelper helper) {
		ServerPlayer player = playerWithXp(helper, 100);

		standAt(helper, player, DEATH_SPOT);

		ServerPlayer respawned = dieAndRespawn(helper, player);
		Remnant remnant = onlyRemnant(helper);
		ServerLevel level = helper.getLevel();

		remnant.hurtServer(level, level.damageSources().generic(), 1000.0F);
		level.explode(null, remnant.getX(), remnant.getY(), remnant.getZ(), 4.0F, Level.ExplosionInteraction.NONE);
		logOut(helper, respawned);

		helper.startSequence()
			.thenExecuteAfter(WAIT_TICKS, () -> {
				helper.assertTrue(remnant.isAlive(), "the Remnant survives damage and its owner being offline");
				moveOnto(logIn(helper, respawned.getGameProfile()), remnant);
			})
			.thenWaitUntil(() -> helper.assertFalse(remnant.isAlive(), "the owner collected the Remnant"))
			.thenSucceed();
	}

	private static void assertSaveMakesNoRemnant(GameTestHelper helper, Item savingItem) {
		ServerPlayer player = playerWithXp(helper, 100);

		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(savingItem));
		standAt(helper, player, DEATH_SPOT);
		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000.0F);

		helper.assertTrue(player.isAlive(), "the player is saved");
		helper.assertValueEqual(ExperiencePoints.total(player), 100, "XP after the save");
		helper.assertEntityNotPresent(Remnants.REMNANT);
		helper.succeed();
	}

	private static ServerPlayer playerWithXp(GameTestHelper helper, int points) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		player.giveExperiencePoints(points);

		return player;
	}

	// The server takes a player's on-ground flag from their client's movement, which a mock player never sends.
	private static void standAt(GameTestHelper helper, ServerPlayer player, BlockPos spot) {
		helper.setBlock(spot.below(), Blocks.STONE);
		moveTo(player, helper.getLevel(), helper.absoluteVec(Vec3.atBottomCenterOf(spot)));
		player.setOnGround(true);
	}

	private static void standOnSlab(GameTestHelper helper, ServerPlayer player, BlockPos spot) {
		helper.setBlock(spot, Blocks.STONE_SLAB);
		moveTo(player, helper.getLevel(), helper.absoluteVec(Vec3.atBottomCenterOf(spot).add(0.0, 0.5, 0.0)));
		player.setOnGround(true);
	}

	private static void fallIntoTheVoid(GameTestHelper helper, ServerPlayer player) {
		Vec3 above = helper.absoluteVec(Vec3.atBottomCenterOf(DEATH_SPOT));

		moveTo(player, helper.getLevel(), new Vec3(above.x, helper.getLevel().getMinY() - 10, above.z));
	}

	private static void setRespawnPoint(ServerPlayer player, BlockPos pos) {
		var respawnData = LevelData.RespawnData.of(player.level().dimension(), pos, 0.0F, 0.0F);

		player.setRespawnPosition(new ServerPlayer.RespawnConfig(respawnData, true), false);
	}

	private static void moveOnto(ServerPlayer player, Remnant remnant) {
		moveTo(player, (ServerLevel) remnant.level(), remnant.position());
	}

	// A player changing dimension is invulnerable until their client's first movement arrives, which a mock never sends.
	private static void moveTo(ServerPlayer player, ServerLevel level, Vec3 position) {
		player.teleport(new TeleportTransition(level, position, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		player.hasChangedDimension();
	}

	private static Remnant onlyRemnant(GameTestHelper helper) {
		List<Remnant> remnants = helper.getEntities(Remnants.REMNANT);

		helper.assertValueEqual(remnants.size(), 1, "Remnants");

		return remnants.getFirst();
	}

	private static Remnant remnantAt(GameTestHelper helper, BlockPos spot) {
		List<Remnant> remnants = remnantsAt(helper, helper.absolutePos(spot));

		helper.assertValueEqual(remnants.size(), 1, "Remnants at " + spot);

		return remnants.getFirst();
	}

	private static List<Remnant> remnantsAt(GameTestHelper helper, BlockPos absolutePos) {
		return helper.getLevel().getEntities(Remnants.REMNANT, new AABB(absolutePos), Entity::isAlive);
	}

	private static void assertRemnantMovedTo(GameTestHelper helper, ServerPlayer respawned, BlockPos spot) {
		GlobalPos expectedDeathLocation = GlobalPos.of(helper.getLevel().dimension(), spot);

		helper.assertValueEqual(remnantsAt(helper, spot).size(), 1, "Remnants at " + spot);
		helper.assertValueEqual(respawned.getLastDeathLocation(), Optional.of(expectedDeathLocation), "the Recovery Compass target");
	}

	private static void collect(GameTestHelper helper, ServerPlayer player, Remnant remnant, Runnable afterCollecting) {
		moveOnto(player, remnant);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(remnant.isAlive(), "the Remnant was collected"))
			.thenExecute(afterCollecting)
			.thenSucceed();
	}

	// Saves the Remnant the way its chunk would on unloading, and removes it as unloading does.
	private static CompoundTag unload(GameTestHelper helper, Remnant remnant) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());

		remnant.save(output);
		remnant.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

		return output.buildResult();
	}

	private static Remnant load(GameTestHelper helper, CompoundTag saved) {
		var input = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved);
		Entity loaded = EntityType.loadEntityRecursive(input, helper.getLevel(), EntitySpawnReason.LOAD, entity -> entity);

		helper.assertTrue(loaded instanceof Remnant, "the saved Remnant loads");
		helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "the saved Remnant is added back");

		return (Remnant) loaded;
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(advancement));

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
