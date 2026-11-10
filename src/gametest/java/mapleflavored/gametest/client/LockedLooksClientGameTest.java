package mapleflavored.gametest.client;

import mapleflavored.structurebooks.BookLocks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;

/**
 * Screenshots of spawners at each stage of cooling and of structure books as their cages break away, lit and in the
 * dark, for the hand check. Run with {@code gradlew runClientGameTest}; the shots land in
 * {@code build/run/clientGameTest/screenshots/}.
 */
public class LockedLooksClientGameTest implements FabricClientGameTest {
	// A room deep underground. Spawners line its south wall and books its north.
	private static final int STAND_Y = -44;
	private static final int SPAWNER_Z = 5;
	private static final int BOOK_Y = STAND_Y + 1;
	private static final int BOOK_Z = -5;
	// The deaths counted at each spawner along the row. The last is Cleared.
	private static final int[] DEATHS = {0, 3, 6, 9, 11, 12};
	private static final int REQUIREMENT = 12;
	// Each locked book has this many keys, and one more done than the book before it.
	private static final int KEYS = 3;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();

			world.getServer().runCommand("difficulty peaceful");
			world.getServer().runCommand("fill -8 -45 -8 8 -36 8 mossy_cobblestone");
			world.getServer().runCommand("fill -7 -44 -7 7 -37 7 air");
			world.getServer().runCommand("fill -7 -37 -7 7 -37 7 light[level=15] replace air");

			placeSpawners(world);
			placeBooks(world);

			shoot(context, world, "0 -44 -2 0 5", "spawner_heat_lit");
			shoot(context, world, "-4 -44 2.5 0 10", "spawner_heat_close");
			shoot(context, world, "0 -44 1 180 0", "book_cage_lit");
			shoot(context, world, "-2 -44 -2.5 180 5", "book_cage_close");

			world.getServer().runCommand("fill -7 -37 -7 7 -37 7 air replace light");

			shoot(context, world, "0 -44 -2 0 5", "spawner_heat_dark");
			shoot(context, world, "0 -44 1 180 0", "book_cage_dark");
		}
	}

	private static void placeSpawners(TestSingleplayerContext world) {
		for (int i = 0; i < DEATHS.length; i++) {
			String clearing = "{deaths:%d,cleared:%s}".formatted(DEATHS[i], DEATHS[i] >= REQUIREMENT ? "1b" : "0b");

			world.getServer().runCommand(
				"setblock %d %d %d spawner{\"maple_flavored:clearing\":%s}".formatted(rowX(i, DEATHS.length), STAND_Y, SPAWNER_Z, clearing)
			);
		}
	}

	// Books with none, one and two of their three keys done, then one that was never locked.
	private static void placeBooks(TestSingleplayerContext world) {
		int books = KEYS + 1;

		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
			ListTag locks = new ListTag();

			for (int done = 0; done < KEYS; done++) {
				locks.add(lock(new BlockPos(rowX(done, books), BOOK_Y, BOOK_Z), done));
			}

			level.getDataStorage().set(BookLocks.TYPE, BookLocks.TYPE.codec().parse(ops, locks).getOrThrow());
		});

		for (int i = 0; i < books; i++) {
			world.getServer().runCommand("setblock %d %d %d maple_flavored:structure_book".formatted(rowX(i, books), BOOK_Y, BOOK_Z));
		}
	}

	// A lock as BookLocks saves it, with the first {@code done} of its keys done. Its keys are spots with no spawner.
	private static CompoundTag lock(BlockPos book, int done) {
		CompoundTag lock = new CompoundTag();
		ListTag keys = new ListTag();
		ListTag doneKeys = new ListTag();

		for (int key = 0; key < KEYS; key++) {
			Tag position = position(new BlockPos(1000 + key, 0, 1000));

			keys.add(position);

			if (key < done) doneKeys.add(position);
		}

		lock.put("book", position(book));
		lock.putString("kind", BookLocks.Kind.SPAWNERS.getSerializedName());
		lock.put("keys", keys);
		lock.put("done", doneKeys);

		return lock;
	}

	private static Tag position(BlockPos pos) {
		return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
	}

	// Where the {@code index}th of {@code count} blocks stands in a row centred on the room, a block apart.
	private static int rowX(int index, int count) {
		return 2 * index - (count - 1);
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, String standAndLook, String name) {
		world.getServer().runCommand("tp @a " + standAndLook);
		context.waitTicks(40);
		world.getConnection().waitForChunksRender();
		context.takeScreenshot("locked_looks_" + name);
	}
}
