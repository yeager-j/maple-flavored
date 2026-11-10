package mapleflavored.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Screenshots of each Deep variant beside its base mob in a deepslate room, lit and in the dark, for the hand check.
 * Run with {@code gradlew runClientGameTest}; the shots land in {@code build/run/clientGameTest/screenshots/}.
 */
public class DeepVariantLooksClientGameTest implements FabricClientGameTest {
	// Still mobs that face the way they were summoned and stay for every shot.
	private static final String STILL = "NoAI:1b,Silent:1b,PersistenceRequired:1b";
	private static final String FACING_NORTH = "Rotation:[180f,0f]";
	private static final String FACING_SOUTH = "Rotation:[0f,0f]";
	private static final String BOW = "equipment:{mainhand:{id:\"minecraft:bow\",count:1}}";
	private static final int FLOOR_Y = -44;
	// The humanoids and creepers line the south wall, and the spiders, which are wider, the north.
	private static final int SOUTH_ROW_Z = 4;
	private static final int NORTH_ROW_Z = -4;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();

			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("fill -8 -45 -8 8 -36 8 deepslate");
			world.getServer().runCommand("fill -7 -44 -7 7 -37 7 air");
			world.getServer().runCommand("fill -7 -37 -7 7 -37 7 light[level=15] replace air");

			summon(world, "zombie", -6, SOUTH_ROW_Z, FACING_NORTH);
			summon(world, "maple_flavored:deep_zombie", -4, SOUTH_ROW_Z, FACING_NORTH);
			summon(world, "maple_flavored:deep_zombie", -2, SOUTH_ROW_Z, FACING_NORTH + ",IsBaby:1b");
			summon(world, "skeleton", 0, SOUTH_ROW_Z, FACING_NORTH + "," + BOW);
			summon(world, "maple_flavored:deep_skeleton", 2, SOUTH_ROW_Z, FACING_NORTH + "," + BOW);
			summon(world, "creeper", 4, SOUTH_ROW_Z, FACING_NORTH);
			summon(world, "maple_flavored:deep_creeper", 6, SOUTH_ROW_Z, FACING_NORTH);
			summon(world, "spider", -2, NORTH_ROW_Z, FACING_SOUTH);
			summon(world, "maple_flavored:deep_spider", 2, NORTH_ROW_Z, FACING_SOUTH);

			shoot(context, world, "0 -44 -3 0 0", "row_lit");
			shoot(context, world, "-4 -44 0.5 0 0", "zombies_lit");
			shoot(context, world, "1 -44 0.5 0 0", "skeletons_lit");
			shoot(context, world, "5 -44 0.5 0 0", "creepers_lit");
			shoot(context, world, "0 -44 0 180 10", "spiders_lit");

			world.getServer().runCommand("fill -7 -37 -7 7 -37 7 air replace light");

			shoot(context, world, "0 -44 -3 0 0", "row_dark");
			shoot(context, world, "0 -44 0 180 10", "spiders_dark");
		}
	}

	private static void summon(TestSingleplayerContext world, String mob, int x, int z, String extra) {
		world.getServer().runCommand("summon %s %d %d %d {%s,%s}".formatted(mob, x, FLOOR_Y, z, STILL, extra));
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, String standAndLook, String name) {
		world.getServer().runCommand("tp @a " + standAndLook);
		context.waitTicks(40);
		world.getConnection().waitForChunksRender();
		context.takeScreenshot("deep_variant_looks_" + name);
	}
}
