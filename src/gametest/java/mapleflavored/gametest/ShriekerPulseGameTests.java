package mapleflavored.gametest;

import java.util.List;
import mapleflavored.echoshard.ShriekerPulses;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Holding an Echo Shard lets a player hear sculk shriekers within 15 blocks pulse from where they are, faster the closer
 * they are. Only the holder hears them.
 */
public class ShriekerPulseGameTests {
	// Above the test grid, away from other tests' blocks and well below the Subspace arenas.
	private static final int ABOVE = 100;
	private static final int LISTENING_TICKS = 80;
	private static final int RANGE = 15;

	@GameTest(maxTicks = 300)
	public void anEchoShardHearsNearbyShriekersPulseFasterTheCloserTheyAre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(1, ABOVE, 1));
		BlockPos near = feet.offset(3, 1, 0);
		BlockPos middling = feet.offset(0, 1, 10);
		BlockPos tooFar = feet.offset(-20, 1, 0);
		MockPlayers.Listening holder = MockPlayers.survivalPlayerWithChannel(helper);
		MockPlayers.Listening offhand = MockPlayers.survivalPlayerWithChannel(helper);
		MockPlayers.Listening bystander = MockPlayers.survivalPlayerWithChannel(helper);

		List<BlockPos> shriekers = List.of(near, middling, tooFar);

		// A shrieker in a chunk that isn't loaded can't be heard, and setting a block loads its chunk only for a moment.
		// The runner releases forced chunks as the batch ends.
		for (BlockPos pos : List.of(feet, near, middling, tooFar)) {
			level.setChunkForced(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), true);
		}

		for (BlockPos shrieker : shriekers) {
			level.setBlock(shrieker, Blocks.SCULK_SHRIEKER.defaultBlockState(), Block.UPDATE_ALL);
		}

		for (MockPlayers.Listening listening : List.of(holder, offhand, bystander)) {
			listening.player().snapTo(Vec3.atBottomCenterOf(feet));
			listening.sent(Object.class);
		}

		holder.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ECHO_SHARD));
		offhand.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ECHO_SHARD));

		helper.startSequence()
			.thenExecuteAfter(LISTENING_TICKS, () -> {
				List<ClientboundSoundPacket> heard = pulses(holder);
				long fromNear = heard.stream().filter(pulse -> from(pulse, near)).count();
				long fromMiddling = heard.stream().filter(pulse -> from(pulse, middling)).count();

				helper.assertTrue(fromMiddling >= 2, "The shrieker 10 blocks away pulsed " + fromMiddling + " times");
				helper.assertTrue(fromNear > fromMiddling, "The nearer shrieker pulsed " + fromNear + " times, no faster than the farther");
				helper.assertTrue(heard.stream().noneMatch(pulse -> from(pulse, tooFar)), "A shrieker 20 blocks away pulsed");
				helper.assertValueEqual(heard.size(), (int) (fromNear + fromMiddling), "pulses from anywhere else");
				helper.assertFalse(pulses(offhand).isEmpty(), "An Echo Shard in the offhand heard nothing");
				helper.assertValueEqual(pulses(bystander), List.of(), "the pulses a player without a shard heard");

				holder.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			})
			.thenExecuteAfter(LISTENING_TICKS, () -> helper.assertValueEqual(pulses(holder), List.of(), "the pulses heard once the shard was put away"))
			.thenExecute(() -> {
				List.of(holder, offhand, bystander).forEach(listening -> MockPlayers.logOut(helper, listening.player()));
				shriekers.forEach(shrieker -> level.removeBlock(shrieker, false));
			})
			.thenSucceed();
	}

	@GameTest
	public void aPulseQuickensFromTheEdgeOfHearingToBesideTheShrieker(GameTestHelper helper) {
		helper.assertValueEqual(ShriekerPulses.beatTicks(RANGE, RANGE), 40, "ticks between pulses at the edge of hearing");
		helper.assertValueEqual(ShriekerPulses.beatTicks(0.0, RANGE), 6, "ticks between pulses beside the shrieker");
		helper.assertTrue(ShriekerPulses.beatTicks(5.0, RANGE) < ShriekerPulses.beatTicks(10.0, RANGE), "A nearer shrieker doesn't pulse faster");
		helper.succeed();
	}

	private static List<ClientboundSoundPacket> pulses(MockPlayers.Listening listening) {
		return listening.sent(ClientboundSoundPacket.class).stream().filter(packet -> packet.getSound().equals(ShriekerPulses.PULSE)).toList();
	}

	private static boolean from(ClientboundSoundPacket pulse, BlockPos shrieker) {
		return Vec3.atCenterOf(shrieker).distanceToSqr(pulse.getX(), pulse.getY(), pulse.getZ()) < 0.01;
	}
}
