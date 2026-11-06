package mapleflavored.client.alloys;

import mapleflavored.MapleFlavored;
import mapleflavored.alloys.AlloyFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** The Alloy Furnace's screen: vanilla's furnace with two input slots, a flame while it smelts and a progress arrow. */
public class AlloyFurnaceScreen extends AbstractContainerScreen<AlloyFurnaceMenu> {
	private static final Identifier BACKGROUND = MapleFlavored.id("textures/gui/container/alloy_furnace.png");
	private static final Identifier FLAME_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 166;
	private static final int TEXTURE_SIZE = 256;

	private static final int FLAME_X = 47;
	private static final int FLAME_Y = 36;
	private static final int FLAME_SIZE = 14;
	private static final int ARROW_X = 79;
	private static final int ARROW_Y = 34;
	private static final int ARROW_WIDTH = 24;
	private static final int ARROW_HEIGHT = 16;

	public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

		// The Ember burns as one piece for the whole smelt, so the flame stays full while it lasts.
		if (menu.isSmelting()) {
			graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED, FLAME_SPRITE, FLAME_SIZE, FLAME_SIZE, 0, 0, leftPos + FLAME_X, topPos + FLAME_Y, FLAME_SIZE, FLAME_SIZE
			);
		}

		int arrowWidth = Mth.ceil(menu.progress() * ARROW_WIDTH);

		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, ARROW_WIDTH, ARROW_HEIGHT, 0, 0, leftPos + ARROW_X, topPos + ARROW_Y, arrowWidth, ARROW_HEIGHT
		);
	}
}
