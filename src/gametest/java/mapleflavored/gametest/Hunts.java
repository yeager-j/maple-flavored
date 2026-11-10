package mapleflavored.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Moves the end of a mob's saved hunt, so a test can reach it without waiting ten minutes or turning the level's clock. */
final class Hunts {
	/** Where a Piglin's hunt for the player who took its Bastion's book is saved. */
	static final Identifier PIGLIN_HUNT = Identifier.fromNamespaceAndPath("maple_flavored", "hunt");
	/** Where an Illager's hunt for the player who took its structure's book is saved. */
	static final Identifier ILLAGER_HUNT = Identifier.fromNamespaceAndPath("maple_flavored", "illager_hunt");

	private static final String ATTACHMENTS = "fabric:attachments";

	private Hunts() {
	}

	/**
	 * Saves the hunting mob as its chunk would on unloading and loads it back in its place with its hunt ending at game
	 * time {@code until}. Everything else it saved is kept. Fails the test if it saved no hunt.
	 *
	 * @param hunt where the mob's hunt is saved
	 * @return the mob loaded back, which replaces {@code hunter} in the level
	 */
	@SuppressWarnings("unchecked")
	static <M extends Mob> M reloadedWithHuntUntil(GameTestHelper helper, M hunter, Identifier hunt, long until) {
		ServerLevel level = helper.getLevel();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());

		hunter.save(output);
		hunter.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

		CompoundTag saved = output.buildResult();
		CompoundTag savedHunt = saved.getCompoundOrEmpty(ATTACHMENTS).getCompoundOrEmpty(hunt.toString());

		helper.assertTrue(savedHunt.contains("until"), "the hunter saved its hunt");
		savedHunt.putLong("until", until);

		Entity loaded = EntityType.loadEntityRecursive(
			TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved),
			level,
			EntitySpawnReason.LOAD,
			entity -> entity
		);

		helper.assertTrue(hunter.getClass().isInstance(loaded), "the saved hunter loads");
		helper.assertTrue(level.addFreshEntity(loaded), "the saved hunter is added back");

		return (M) loaded;
	}
}
