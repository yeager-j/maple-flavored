package mapleflavored.client.enchanting;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import mapleflavored.MapleFlavored;
import mapleflavored.enchanting.Checklist;
import mapleflavored.enchanting.EnchantingTableMenu;
import mapleflavored.enchanting.EnchantmentSlots;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Enchanting Table's checklist screen. It lists what the Library offers for the item and what the item already
 * has; clicking a row ticks or unticks it, and Save is enabled once the server would accept the save. Hovering Save or
 * a red row explains what blocks it.
 */
public class EnchantingTableScreen extends AbstractContainerScreen<EnchantingTableMenu> {
	private static final Identifier BACKGROUND = MapleFlavored.id("textures/gui/container/enchanting_table.png");
	private static final Component SAVE = Component.translatable("container.maple_flavored.enchanting_table.save");
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 206;
	private static final int TEXTURE_SIZE = 256;

	private static final int SAVE_X = 64;
	private static final int SAVE_Y = 17;
	private static final int SAVE_WIDTH = 48;
	private static final int SAVE_HEIGHT = 18;
	private static final int SLOT_COUNTER_X = 116;
	private static final int SLOT_COUNTER_Y = 22;

	private static final int LIST_X = 8;
	private static final int LIST_Y = 39;
	private static final int LIST_WIDTH = 146;
	private static final int ROW_HEIGHT = 14;
	private static final int VISIBLE_ROWS = 5;
	private static final int CHECKBOX_SIZE = 9;
	private static final int CHECKBOX_U = 176;
	private static final int TICKED_CHECKBOX_U = 185;
	private static final int NAME_X = 15;

	private static final int SCROLLER_X = 157;
	private static final int SCROLLER_Y = 39;
	private static final int SCROLLER_WIDTH = 12;
	private static final int SCROLLER_HEIGHT = 15;
	private static final int SCROLL_TRACK_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
	private static final int SCROLLER_U = 176;
	private static final int DISABLED_SCROLLER_U = 188;
	private static final int SCROLLER_V = 9;

	private static final int LABEL_COLOR = 0xFF404040;
	private static final int UNTICKED_COLOR = 0xFFA8A8A8;
	private static final int TICKED_COLOR = 0xFF80FF20;
	private static final int PROBLEM_COLOR = 0xFFFF5555;
	private static final int HOVER_COLOR = 0x30FFFFFF;

	private Button saveButton;
	private float scrollOffset;
	private boolean scrolling;

	public EnchantingTableScreen(EnchantingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		saveButton = addRenderableWidget(
			Button.builder(SAVE, button -> click(EnchantingTableMenu.SAVE_BUTTON))
				.bounds(leftPos + SAVE_X, topPos + SAVE_Y, SAVE_WIDTH, SAVE_HEIGHT)
				.build()
		);
	}

	@Override
	public void containerTick() {
		super.containerTick();

		List<Checklist.Problem> problems = menu.problems(minecraft.player);

		saveButton.active = problems.isEmpty();
		saveButton.setTooltip(problems.isEmpty() ? null : Tooltip.create(describe(problems)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

		Checklist checklist = menu.checklist();
		List<Holder<Enchantment>> rows = checklist.rows();
		List<Checklist.Problem> problems = menu.problems(minecraft.player);
		int firstRow = firstRow(rows.size());

		for (int visible = 0; visible < VISIBLE_ROWS && firstRow + visible < rows.size(); visible++) {
			extractRow(graphics, checklist, problems, rows.get(firstRow + visible), visible, mouseX, mouseY);
		}

		extractScroller(graphics, rows.size(), mouseX, mouseY);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		Checklist checklist = menu.checklist();
		int chosen = checklist.chosen().size();
		int slots = EnchantmentSlots.available(minecraft.player.experienceLevel, checklist.item());
		boolean overLimit = chosen > slots && !minecraft.player.hasInfiniteMaterials();
		Component counter = Component.translatable("container.maple_flavored.enchanting_table.slots", chosen, slots);

		graphics.text(font, counter, SLOT_COUNTER_X, SLOT_COUNTER_Y, overLimit ? PROBLEM_COLOR : LABEL_COLOR, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		List<Holder<Enchantment>> rows = menu.checklist().rows();
		int row = hoveredRow(rows.size(), mouseX, mouseY);

		if (row < 0) return;

		Holder<Enchantment> enchantment = rows.get(row);
		List<Checklist.Problem> rowProblems = menu.problems(minecraft.player).stream()
			.filter(problem -> problem.concerns(enchantment))
			.toList();

		if (rowProblems.isEmpty()) return;

		graphics.setTooltipForNextFrame(font, font.split(describe(rowProblems), IMAGE_WIDTH), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		List<Holder<Enchantment>> rows = menu.checklist().rows();
		int row = hoveredRow(rows.size(), event.x(), event.y());

		if (row >= 0) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			click(EnchantingTableMenu.toggleButton(rows.get(row), menu.enchantments()));

			return true;
		}

		if (isOverScroller(event.x(), event.y())) scrolling = true;

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (!scrolling || !canScroll(menu.checklist().rows().size())) return super.mouseDragged(event, dragX, dragY);

		float trackTop = topPos + SCROLLER_Y;
		float thumbTravel = SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT;

		scrollOffset = Mth.clamp(((float) event.y() - trackTop - SCROLLER_HEIGHT / 2.0F) / thumbTravel, 0.0F, 1.0F);

		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		scrolling = false;

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

		int rowCount = menu.checklist().rows().size();

		if (canScroll(rowCount)) scrollOffset = Mth.clamp(scrollOffset - (float) scrollY / offscreenRows(rowCount), 0.0F, 1.0F);

		return true;
	}

	private void extractRow(
		GuiGraphicsExtractor graphics,
		Checklist checklist,
		List<Checklist.Problem> problems,
		Holder<Enchantment> enchantment,
		int visibleIndex,
		int mouseX,
		int mouseY
	) {
		int x = leftPos + LIST_X;
		int y = topPos + LIST_Y + visibleIndex * ROW_HEIGHT;
		boolean ticked = checklist.state(enchantment) != Checklist.State.UNSELECTED;
		boolean hasProblem = problems.stream().anyMatch(problem -> problem.concerns(enchantment));
		int color = hasProblem ? PROBLEM_COLOR : ticked ? TICKED_COLOR : UNTICKED_COLOR;
		String name = Enchantment.getFullname(enchantment, checklist.shownLevel(enchantment)).getString();
		int checkboxY = y + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;

		if (isWithin(mouseX, mouseY, x, y, LIST_WIDTH, ROW_HEIGHT)) {
			graphics.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT, HOVER_COLOR);
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		graphics.blit(
			RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 2, checkboxY, ticked ? TICKED_CHECKBOX_U : CHECKBOX_U, 0.0F,
			CHECKBOX_SIZE, CHECKBOX_SIZE, TEXTURE_SIZE, TEXTURE_SIZE
		);
		graphics.text(font, font.plainSubstrByWidth(name, LIST_WIDTH - NAME_X - 2), x + NAME_X, checkboxY + 1, color);
	}

	private void extractScroller(GuiGraphicsExtractor graphics, int rowCount, int mouseX, int mouseY) {
		boolean active = canScroll(rowCount);
		int x = leftPos + SCROLLER_X;
		int y = topPos + SCROLLER_Y + (int) ((SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT) * scrollOffset);

		graphics.blit(
			RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, active ? SCROLLER_U : DISABLED_SCROLLER_U, SCROLLER_V,
			SCROLLER_WIDTH, SCROLLER_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE
		);

		if (active && isOverScroller(mouseX, mouseY)) graphics.requestCursor(scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
	}

	private void click(int buttonId) {
		if (menu.clickMenuButton(minecraft.player, buttonId)) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
	}

	/** The index into the rows of the row under the mouse, or -1 for none. */
	private int hoveredRow(int rowCount, double mouseX, double mouseY) {
		if (!isWithin(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_WIDTH, SCROLL_TRACK_HEIGHT)) return -1;

		int row = firstRow(rowCount) + (int) (mouseY - topPos - LIST_Y) / ROW_HEIGHT;

		return row < rowCount ? row : -1;
	}

	// The row count follows the server's data, so the first row is worked out from the scroll position each time.
	private int firstRow(int rowCount) {
		return canScroll(rowCount) ? Math.round(scrollOffset * offscreenRows(rowCount)) : 0;
	}

	private boolean canScroll(int rowCount) {
		return offscreenRows(rowCount) > 0;
	}

	private static int offscreenRows(int rowCount) {
		return rowCount - VISIBLE_ROWS;
	}

	private boolean isOverScroller(double mouseX, double mouseY) {
		return isWithin(mouseX, mouseY, leftPos + SCROLLER_X, topPos + SCROLLER_Y, SCROLLER_WIDTH, SCROLL_TRACK_HEIGHT);
	}

	private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static Component describe(List<Checklist.Problem> problems) {
		return CommonComponents.joinLines(problems.stream().map(Checklist.Problem::describe).toList());
	}
}
