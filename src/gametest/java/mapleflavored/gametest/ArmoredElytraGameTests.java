package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.GearKind;
import mapleflavored.enchanting.EnchantmentSlots;
import mapleflavored.grounded.Grounded;
import mapleflavored.materialunits.MaterialUnits;
import mapleflavored.smithing.UpgradeRefusalMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Armored Elytra: an Alloy chestplate and an Elytra smithed into one piece that protects, enchants, repairs and counts
 * for set bonuses as the chestplate, and glides worse than an Elytra.
 */
public class ArmoredElytraGameTests {
	private static final BlockPos STATION_POS = new BlockPos(1, 2, 1);
	private static final BlockPos SKY = new BlockPos(1, 40, 1);
	private static final float FACING_EAST = -90.0F;
	private static final float GLIDING_PITCH = 10.0F;
	private static final Vec3 GLIDING_MOVEMENT = new Vec3(1.2, -0.3, 0.1);
	private static final double TOLERANCE = 1.0E-9;
	// Components that name, draw and fly the Armored Elytra, where it differs from its chestplate.
	private static final Set<DataComponentType<?>> ELYTRA_COMPONENTS = Set.of(
		DataComponents.ITEM_NAME,
		DataComponents.ITEM_MODEL,
		DataComponents.EQUIPPABLE,
		DataComponents.GLIDER,
		DataComponents.RARITY
	);
	private static final List<GearKind> SET_KINDS = List.of(GearKind.HELMET, GearKind.LEGGINGS, GearKind.BOOTS);
	private static final Identifier ADVANCEMENT = Identifier.parse("maple_flavored:end/obtain_armored_elytra");

	@GameTest
	public void eachAlloyChestplateAndAnElytraSmithIntoItsArmoredElytra(GameTestHelper helper) {
		for (Alloy alloy : Alloy.values()) {
			ItemStack chestplate = new ItemStack(AlloyGear.of(alloy, GearKind.CHESTPLATE));
			ItemStack result = assemble(helper, chestplate, new ItemStack(Items.ELYTRA));

			helper.assertValueEqual(result.getItem(), AlloyGear.armoredElytra(alloy), alloy.path() + " chestplate with an Elytra");
		}

		ItemStack fromNetherite = assemble(helper, new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.ELYTRA));
		ItemStack swapped = assemble(helper, new ItemStack(Items.ELYTRA), new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.CHESTPLATE)));

		helper.assertTrue(fromNetherite.isEmpty(), "a Netherite chestplate with an Elytra makes something");
		helper.assertTrue(swapped.isEmpty(), "an Elytra with a Steel chestplate as the addition makes something");
		helper.succeed();
	}

	@GameTest
	public void smithingAnArmoredElytraUsesUpTheChestplateAndTheElytra(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		SmithingMenu smithing = smithing(helper, player, new ItemStack(AlloyGear.of(Alloy.SHAKUDO, GearKind.CHESTPLATE)), new ItemStack(Items.ELYTRA));

		smithing.setCarried(ItemStack.EMPTY);
		smithing.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		helper.assertValueEqual(smithing.getCarried().getItem(), AlloyGear.armoredElytra(Alloy.SHAKUDO), "the smithed Armored Elytra");
		helper.assertTrue(smithing.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(), "the chestplate is used up");
		helper.assertTrue(smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "the Elytra is used up");
		helper.succeed();
	}

	@GameTest
	public void theArmoredElytraKeepsTheChestplatesTrimNameAndWear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack chestplate = new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.CHESTPLATE));
		ArmorTrim trim = new ArmorTrim(
			level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.QUARTZ),
			level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.COAST)
		);

		chestplate.set(DataComponents.TRIM, trim);
		chestplate.set(DataComponents.CUSTOM_NAME, Component.literal("Wings"));
		chestplate.setDamageValue(100);

		ItemStack armoredElytra = assemble(helper, chestplate, new ItemStack(Items.ELYTRA));

		helper.assertValueEqual(armoredElytra.get(DataComponents.TRIM), trim, "the trim");
		helper.assertValueEqual(armoredElytra.get(DataComponents.CUSTOM_NAME), Component.literal("Wings"), "the name");
		helper.assertValueEqual(armoredElytra.getDamageValue(), 100, "the wear");
		helper.succeed();
	}

	@GameTest
	public void anArmoredElytraChangesAlloyWithADifferentIngotOnly(GameTestHelper helper) {
		ItemStack steel = new ItemStack(AlloyGear.armoredElytra(Alloy.STEEL));
		ItemStack changed = assemble(helper, steel, new ItemStack(Alloy.ORICHALCUM.ingot()));
		ItemStack same = assemble(helper, steel, new ItemStack(Alloy.STEEL.ingot()));

		helper.assertValueEqual(changed.getItem(), AlloyGear.armoredElytra(Alloy.ORICHALCUM), "a Steel Armored Elytra with an Orichalcum ingot");
		helper.assertTrue(same.isEmpty(), "a Steel Armored Elytra with a Steel ingot makes something");
		helper.succeed();
	}

	@GameTest
	public void anEnchantedChestplateOrElytraIsRefused(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		ItemStack chestplate = new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.CHESTPLATE));
		ItemStack enchantedChestplate = enchanted(helper, chestplate.copy(), Enchantments.PROTECTION);
		ItemStack enchantedElytra = enchanted(helper, new ItemStack(Items.ELYTRA), Enchantments.UNBREAKING);

		for (SmithingMenu refused : List.of(
			smithing(helper, player, enchantedChestplate, new ItemStack(Items.ELYTRA)),
			smithing(helper, player, chestplate, enchantedElytra)
		)) {
			helper.assertTrue(refused.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty(), "enchanted inputs make an Armored Elytra");
			helper.assertTrue(((UpgradeRefusalMenu) refused).mapleflavored$refusedEnchantedInput(), "the menu says the enchantment is why");
		}

		helper.succeed();
	}

	@GameTest
	public void anArmoredElytraIsItsChestplateThatGlides(GameTestHelper helper) {
		for (Alloy alloy : Alloy.values()) {
			Item chestplate = AlloyGear.of(alloy, GearKind.CHESTPLATE);
			Item armoredElytra = AlloyGear.armoredElytra(alloy);
			Set<DataComponentType<?>> types = new HashSet<>(chestplate.components().keySet());

			types.addAll(armoredElytra.components().keySet());
			types.removeAll(ELYTRA_COMPONENTS);

			for (DataComponentType<?> type : types) {
				boolean same = Objects.equals(armoredElytra.components().get(type), chestplate.components().get(type));

				helper.assertTrue(same, alloy.path() + " Armored Elytra has its chestplate's " + type);
			}

			Equippable equippable = armoredElytra.components().get(DataComponents.EQUIPPABLE);
			ItemStack stack = new ItemStack(armoredElytra);

			helper.assertValueEqual(equippable.slot(), EquipmentSlot.CHEST, alloy.path() + " Armored Elytra's slot");
			helper.assertTrue(equippable.damageOnHurt(), alloy.path() + " Armored Elytra wears down when hit");
			helper.assertTrue(LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST), alloy.path() + " Armored Elytra glides");
			helper.assertValueEqual(AlloyGear.alloyOf(stack), Optional.of(alloy), alloy.path() + " Armored Elytra's Alloy");
		}

		helper.succeed();
	}

	@GameTest
	public void anArmoredElytraProtectsItsWearerLikeItsChestplate(GameTestHelper helper) {
		ServerPlayer withChestplate = survivalPlayer(helper);
		ServerPlayer withArmoredElytra = survivalPlayer(helper);

		withChestplate.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.CHESTPLATE)));
		withArmoredElytra.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AlloyGear.armoredElytra(Alloy.ADAMANT)));
		withChestplate.doTick();
		withArmoredElytra.doTick();

		helper.assertValueEqual(withArmoredElytra.getAttributeValue(Attributes.ARMOR), 8.0, "armor in an Adamant Armored Elytra");
		helper.assertValueEqual(
			withArmoredElytra.getAttributeValue(Attributes.ARMOR_TOUGHNESS), withChestplate.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness"
		);
		helper.assertValueEqual(
			withArmoredElytra.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), withChestplate.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "knockback resistance"
		);
		helper.succeed();
	}

	@GameTest
	public void anArmoredElytraEnchantsAndRepairsAsItsChestplate(GameTestHelper helper) {
		var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList();
		var recipes = helper.getLevel().getServer().getRecipeManager();

		for (Alloy alloy : Alloy.values()) {
			ItemStack chestplate = new ItemStack(AlloyGear.of(alloy, GearKind.CHESTPLATE));
			ItemStack armoredElytra = new ItemStack(AlloyGear.armoredElytra(alloy));
			String label = alloy.path() + " Armored Elytra";

			for (Holder<Enchantment> enchantment : enchantments) {
				boolean chestplateTakes = enchantment.value().canEnchant(chestplate);

				helper.assertValueEqual(enchantment.value().canEnchant(armoredElytra), chestplateTakes, label + " takes " + enchantment.getRegisteredName());
			}

			helper.assertValueEqual(
				EnchantmentSlots.available(30, armoredElytra), EnchantmentSlots.available(30, chestplate), label + "'s Enchantment Slots at level 30"
			);
			helper.assertTrue(MaterialUnits.materialOf(armoredElytra).orElseThrow().test(new ItemStack(alloy.ingot())), label + "'s material is its ingot");
			helper.assertValueEqual(MaterialUnits.fullRepairUnits(armoredElytra, recipes), 1, label + "'s full repair units");
		}

		helper.assertValueEqual(
			EnchantmentSlots.available(30, new ItemStack(AlloyGear.armoredElytra(Alloy.ELECTRUM))), 2, "Electrum Armored Elytra's slots at level 30"
		);
		helper.succeed();
	}

	@GameTest
	public void anArmoredElytraIsItsAlloysChestPieceInASet(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSetWithArmoredElytra(player, Alloy.STEEL, Alloy.STEEL);
		helper.assertValueEqual(AlloySets.fullSet(player), Optional.of(Alloy.STEEL), "a Steel set with a Steel Armored Elytra");
		helper.assertValueEqual(AlloyGear.piecesWorn(player, Alloy.STEEL), 4, "Steel pieces worn");
		helper.assertValueEqual(player.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), 1.0, "the full Steel set's explosion knockback resistance");

		wearSetWithArmoredElytra(player, Alloy.STEEL, Alloy.ADAMANT);
		helper.assertValueEqual(AlloySets.fullSet(player), Optional.empty(), "a Steel set with an Adamant Armored Elytra");
		helper.succeed();
	}

	@GameTest
	public void anArmoredElytraGlidesWithMoreHorizontalDrag(GameTestHelper helper) {
		ServerPlayer plain = glidingPlayer(helper, Items.ELYTRA, 1);
		ServerPlayer armored = glidingPlayer(helper, AlloyGear.armoredElytra(Alloy.STEEL), 4);

		plain.travel(Vec3.ZERO);
		armored.travel(Vec3.ZERO);

		Vec3 plainMovement = plain.getDeltaMovement();
		Vec3 armoredMovement = armored.getDeltaMovement();

		helper.assertTrue(Math.abs(armoredMovement.x - plainMovement.x * 0.995) < TOLERANCE, "east speed " + armoredMovement.x + " against " + plainMovement.x);
		helper.assertTrue(Math.abs(armoredMovement.z - plainMovement.z * 0.995) < TOLERANCE, "south speed " + armoredMovement.z + " against " + plainMovement.z);
		helper.assertTrue(Math.abs(armoredMovement.y - plainMovement.y) < TOLERANCE, "vertical speed " + armoredMovement.y + " against " + plainMovement.y);
		helper.succeed();
	}

	@GameTest
	public void aFireworkRocketBoostsAnArmoredElytraLess(GameTestHelper helper) {
		ServerPlayer plain = glidingPlayer(helper, Items.ELYTRA, 1);
		ServerPlayer armored = glidingPlayer(helper, AlloyGear.armoredElytra(Alloy.STEEL), 4);

		Vec3 plainBoost = rocketBoost(helper, plain);
		Vec3 armoredBoost = rocketBoost(helper, armored);

		helper.assertTrue(plainBoost.length() > 0.1, "the rocket boosts a plain Elytra");
		helper.assertTrue(armoredBoost.subtract(plainBoost.scale(0.75)).length() < TOLERANCE, "boost " + armoredBoost + " against " + plainBoost);
		helper.succeed();
	}

	@GameTest
	public void aGroundedPlayerCantGlideInAnArmoredElytra(GameTestHelper helper) {
		ServerPlayer grounded = playerInTheAir(helper, AlloyGear.armoredElytra(Alloy.ELECTRUM), 1);
		ServerPlayer free = playerInTheAir(helper, AlloyGear.armoredElytra(Alloy.ELECTRUM), 4);

		grounded.addEffect(new MobEffectInstance(Grounded.EFFECT, MobEffectInstance.INFINITE_DURATION));

		helper.assertTrue(free.tryToStartFallFlying(), "the other player starts gliding");
		helper.assertFalse(grounded.tryToStartFallFlying(), "the Grounded player can't start gliding");
		helper.succeed();
	}

	@GameTest
	public void gettingAnArmoredElytraEarnsItsAdvancement(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(ADVANCEMENT);

		helper.assertTrue(advancement != null, ADVANCEMENT + " exists");
		helper.assertValueEqual(advancement.value().parent(), Optional.of(Identifier.withDefaultNamespace("end/elytra")), "its parent");
		helper.assertValueEqual(advancement.value().display().map(DisplayInfo::type), Optional.of(AdvancementType.GOAL), "its frame");

		// A ticking player sends inventory changes to advancement triggers each tick; a mock player is never ticked.
		player.getInventory().add(new ItemStack(Items.ELYTRA));
		player.inventoryMenu.broadcastChanges();
		helper.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(), "earned with a plain Elytra");

		player.getInventory().add(new ItemStack(AlloyGear.armoredElytra(Alloy.SHAKUDO)));
		player.inventoryMenu.broadcastChanges();
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "earned with an Armored Elytra");
		helper.succeed();
	}

	private static void wearSetWithArmoredElytra(ServerPlayer player, Alloy set, Alloy armoredElytra) {
		for (GearKind kind : SET_KINDS) player.setItemSlot(kind.slot(), new ItemStack(AlloyGear.of(set, kind)));

		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AlloyGear.armoredElytra(armoredElytra)));
		player.doTick();
	}

	/** The change one tick of a Firework Rocket attached to the gliding player makes to its movement. */
	private static Vec3 rocketBoost(GameTestHelper helper, ServerPlayer glider) {
		Vec3 before = glider.getDeltaMovement();
		FireworkRocketEntity rocket = new FireworkRocketEntity(helper.getLevel(), new ItemStack(Items.FIREWORK_ROCKET), glider);

		rocket.tick();

		return glider.getDeltaMovement().subtract(before);
	}

	/** A player in the air wearing the glider, gliding east and slightly down at the same speed in every test. */
	private static ServerPlayer glidingPlayer(GameTestHelper helper, Item glider, int lane) {
		ServerPlayer player = playerInTheAir(helper, glider, lane);

		helper.assertTrue(player.tryToStartFallFlying(), "the player starts gliding");
		player.setXRot(GLIDING_PITCH);
		player.setDeltaMovement(GLIDING_MOVEMENT);

		return player;
	}

	/** A loaded survival mock player in open air wearing the glider. A mock is never ticked, so it stays put. */
	private static ServerPlayer playerInTheAir(GameTestHelper helper, Item glider, int lane) {
		ServerPlayer player = MockPlayers.playerWithMaxHearts(helper, 10);
		Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(SKY.offset(0, 0, lane)));

		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(position.x, position.y, position.z, FACING_EAST, 0.0F);
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(glider));
		player.setOnGround(false);

		return player;
	}

	private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> enchantment) {
		stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), 1);

		return stack;
	}

	private static ItemStack assemble(GameTestHelper helper, ItemStack base, ItemStack addition) {
		ServerLevel level = helper.getLevel();
		SmithingRecipeInput input = new SmithingRecipeInput(ItemStack.EMPTY, base, addition);

		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
			.map(recipe -> recipe.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}

	private static SmithingMenu smithing(GameTestHelper helper, ServerPlayer player, ItemStack base, ItemStack addition) {
		helper.setBlock(STATION_POS, Blocks.SMITHING_TABLE);

		SmithingMenu smithing = new SmithingMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(STATION_POS)));

		smithing.getSlot(SmithingMenu.BASE_SLOT).set(base);
		smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(addition);

		return smithing;
	}
}
