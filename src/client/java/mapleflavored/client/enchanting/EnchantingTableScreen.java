package mapleflavored.client.enchanting;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import mapleflavored.MapleFlavored;
import mapleflavored.enchanting.Checklist;
import mapleflavored.enchanting.EnchantingTableMenu;
import mapleflavored.enchanting.Library;
import mapleflavored.enchanting.LibraryCheckpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Enchanting Table's checklist screen. It lists what the Library offers for the item, enchantments for gear or
 * effects for a golden apple, and what the item already has; clicking a row ticks or unticks it, and Save is enabled
 * once the server would accept the save. Hovering Save or a red row explains what blocks it. A bar under the list shows
 * the Library's progress toward Full, with each {@link LibraryCheckpoint}'s reward marked where it unlocks. A table with
 * too few bookshelves shows no reward as unlocked, and says so wherever it explains itself.
 */
public class EnchantingTableScreen extends AbstractContainerScreen<EnchantingTableMenu> {
	private static final Identifier BACKGROUND = MapleFlavored.id("textures/gui/container/enchanting_table.png");
	private static final Component SAVE = Component.translatable(Checklist.KEY_PREFIX + "save");
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 226;
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

	// The track is part of the background; the fill sprite sits below the panel in the same texture. Each checkpoint
	// gets a notch in the bar and its reward's icon above.
	private static final int PROGRESS_X = 7;
	private static final int PROGRESS_Y = 123;
	private static final int PROGRESS_WIDTH = 162;
	private static final int PROGRESS_HEIGHT = 5;
	private static final int PROGRESS_HOVER_MARGIN = 1;
	private static final int PROGRESS_FILL_V = 226;
	private static final int FILL_X = PROGRESS_X + 1;
	private static final int FILL_Y = PROGRESS_Y + 1;
	private static final int FILL_WIDTH = PROGRESS_WIDTH - 2;
	private static final int FILL_HEIGHT = PROGRESS_HEIGHT - 2;
	private static final int CHECKPOINT_ICON_Y = 113;
	private static final int CHECKPOINT_ICON_SIZE = 8;
	private static final float CHECKPOINT_ICON_SCALE = 0.5F;
	private static final int UNLOCKED_NOTCH_COLOR = 0xFF1E3E96;
	private static final int LOCKED_NOTCH_COLOR = 0xFF8B8B8B;
	private static final int LOCKED_ICON_SHADE = 0xA0C6C6C6;
	private static final Component FULL_LIBRARY = Component.translatable(Checklist.KEY_PREFIX + "full_library");
	private static final Component CHECKPOINT_UNLOCKED = Component.translatable(Checklist.KEY_PREFIX + "checkpoint_reached")
		.withStyle(ChatFormatting.GREEN);

	private static final int LABEL_COLOR = 0xFF404040;
	private static final int UNTICKED_COLOR = 0xFFA8A8A8;
	private static final int TICKED_COLOR = 0xFF80FF20;
	private static final int PROBLEM_COLOR = 0xFFFF5555;
	private static final int HOVER_COLOR = 0x30FFFFFF;

	private Button saveButton;
	// The menu's checklist and its problems as of the last tick, so every part of a frame draws and clicks the same rows.
	private Checklist checklist;
	private List<Checklist.Problem> problems;
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
		readChecklist();
	}

	@Override
	public void containerTick() {
		super.containerTick();

		readChecklist();

		saveButton.active = problems.isEmpty();
		saveButton.setTooltip(problems.isEmpty() ? null : Tooltip.create(describe(problems)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

		List<Checklist.Row> rows = checklist.rows();
		int firstRow = firstRow(rows.size());

		for (int visible = 0; visible < VISIBLE_ROWS && firstRow + visible < rows.size(); visible++) {
			extractRow(graphics, rows.get(firstRow + visible), visible, mouseX, mouseY);
		}

		extractScroller(graphics, rows.size(), mouseX, mouseY);
		extractProgress(graphics);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		boolean overLimit = problems.stream().anyMatch(Checklist.TooMany.class::isInstance);
		Component counter = Component.translatable(Checklist.KEY_PREFIX + "slots", checklist.chosenCount(), checklist.slots());

		graphics.text(font, counter, SLOT_COUNTER_X, SLOT_COUNTER_Y, overLimit ? PROBLEM_COLOR : LABEL_COLOR, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		Library.Status library = checklist.library();

		for (LibraryCheckpoint checkpoint : LibraryCheckpoint.ALL) {
			if (isWithin(mouseX, mouseY, leftPos + checkpointIconX(checkpoint), topPos + CHECKPOINT_ICON_Y, CHECKPOINT_ICON_SIZE, CHECKPOINT_ICON_SIZE)) {
				graphics.setTooltipForNextFrame(font, font.split(checkpointTooltip(checkpoint, library), IMAGE_WIDTH), mouseX, mouseY);

				return;
			}
		}

		if (isOverProgress(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, font.split(progressTooltip(library), IMAGE_WIDTH), mouseX, mouseY);

			return;
		}

		List<Checklist.Row> rows = checklist.rows();
		int row = hoveredRow(rows.size(), mouseX, mouseY);

		if (row < 0 || rows.get(row).problems().isEmpty()) return;

		graphics.setTooltipForNextFrame(font, font.split(describe(rows.get(row).problems()), IMAGE_WIDTH), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		List<Checklist.Row> rows = checklist.rows();
		int row = hoveredRow(rows.size(), event.x(), event.y());

		if (row >= 0) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			click(EnchantingTableMenu.toggleButton(rows.get(row).id()));

			return true;
		}

		if (isOverScroller(event.x(), event.y())) scrolling = true;

		boolean handled = super.mouseClicked(event, doubleClick);

		// A clicked button keeps focus, which draws it highlighted until something else takes focus. Keyboard presses
		// don't come through here, so Tab and Enter still leave Save focused.
		if (getFocused() == saveButton) setFocused(null);

		return handled;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (!scrolling || !canScroll(checklist.rows().size())) return super.mouseDragged(event, dragX, dragY);

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

		int rowCount = checklist.rows().size();

		if (canScroll(rowCount)) scrollOffset = Mth.clamp(scrollOffset - (float) scrollY / offscreenRows(rowCount), 0.0F, 1.0F);

		return true;
	}

	private void extractRow(GuiGraphicsExtractor graphics, Checklist.Row row, int visibleIndex, int mouseX, int mouseY) {
		int x = leftPos + LIST_X;
		int y = topPos + LIST_Y + visibleIndex * ROW_HEIGHT;
		boolean ticked = row.chosen();
		int color = !row.problems().isEmpty() ? PROBLEM_COLOR : ticked ? TICKED_COLOR : UNTICKED_COLOR;
		String name = row.name().getString();
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

	private void extractProgress(GuiGraphicsExtractor graphics) {
		Library.Status library = checklist.library();
		int filled = Math.round(library.progress().fraction() * FILL_WIDTH);

		graphics.blit(
			RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FILL_X, topPos + FILL_Y, 0.0F, PROGRESS_FILL_V, filled, FILL_HEIGHT,
			TEXTURE_SIZE, TEXTURE_SIZE
		);

		for (LibraryCheckpoint checkpoint : LibraryCheckpoint.ALL) {
			extractCheckpoint(graphics, checkpoint, library.unlocked(checkpoint));
		}

		// The shades must cover the icons, and items draw above everything else in their stratum.
		graphics.nextStratum();

		for (LibraryCheckpoint checkpoint : LibraryCheckpoint.ALL) {
			if (library.unlocked(checkpoint)) continue;

			int x = leftPos + checkpointIconX(checkpoint);
			int y = topPos + CHECKPOINT_ICON_Y;

			graphics.fill(x, y, x + CHECKPOINT_ICON_SIZE, y + CHECKPOINT_ICON_SIZE, LOCKED_ICON_SHADE);
		}
	}

	private void extractCheckpoint(GuiGraphicsExtractor graphics, LibraryCheckpoint checkpoint, boolean unlocked) {
		int notchX = leftPos + checkpointNotchX(checkpoint);

		// A Full Library's checkpoint is the bar's end, which needs no notch.
		if (!checkpoint.needsFullLibrary()) {
			graphics.fill(notchX, topPos + FILL_Y, notchX + 1, topPos + FILL_Y + FILL_HEIGHT, unlocked ? UNLOCKED_NOTCH_COLOR : LOCKED_NOTCH_COLOR);
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(leftPos + checkpointIconX(checkpoint), topPos + CHECKPOINT_ICON_Y);
		graphics.pose().scale(CHECKPOINT_ICON_SCALE);
		graphics.item(checkpoint.icon().create(), 0, 0);
		graphics.pose().popMatrix();
	}

	private static int checkpointNotchX(LibraryCheckpoint checkpoint) {
		return FILL_X + checkpoint.percent() * FILL_WIDTH / 100;
	}

	// Centred over its notch, but kept within the bar.
	private static int checkpointIconX(LibraryCheckpoint checkpoint) {
		return Mth.clamp(checkpointNotchX(checkpoint) - CHECKPOINT_ICON_SIZE / 2, FILL_X, FILL_X + FILL_WIDTH - CHECKPOINT_ICON_SIZE);
	}

	private static Component progressTooltip(Library.Status library) {
		Library.Progress progress = library.progress();
		List<Component> lines = new ArrayList<>();

		lines.add(Component.translatable(Checklist.KEY_PREFIX + "progress", progress.levels(), progress.fullLevels()));

		if (progress.isFull()) lines.add(FULL_LIBRARY);
		if (!library.isPowered()) lines.add(needsBookshelves(library));

		return CommonComponents.joinLines(lines);
	}

	private static Component checkpointTooltip(LibraryCheckpoint checkpoint, Library.Status library) {
		Library.Progress progress = library.progress();
		List<Component> lines = new ArrayList<>();

		lines.add(checkpoint.reward());

		if (library.unlocked(checkpoint)) lines.add(CHECKPOINT_UNLOCKED);

		if (!progress.reached(checkpoint.percent())) {
			Component needed = Component.translatable(
				Checklist.KEY_PREFIX + "checkpoint_needs", checkpoint.percent(), progress.levelsFor(checkpoint.percent())
			);

			lines.add(needed.copy().withStyle(ChatFormatting.GRAY));
		}

		if (!library.isPowered()) lines.add(needsBookshelves(library));

		return CommonComponents.joinLines(lines);
	}

	private static Component needsBookshelves(Library.Status library) {
		return new Checklist.NeedsBookshelves(library.bookshelves()).describe().copy().withStyle(ChatFormatting.RED);
	}

	private boolean isOverProgress(double mouseX, double mouseY) {
		return isWithin(
			mouseX, mouseY, leftPos + PROGRESS_X, topPos + PROGRESS_Y - PROGRESS_HOVER_MARGIN, PROGRESS_WIDTH,
			PROGRESS_HEIGHT + 2 * PROGRESS_HOVER_MARGIN
		);
	}

	private void readChecklist() {
		checklist = menu.checklist();
		problems = checklist.problems();
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
