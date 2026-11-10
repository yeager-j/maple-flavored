package mapleflavored.client.hearts;

import mapleflavored.hearts.MaxHeartsLostPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Adds a "Max Hearts lost" line under the score on the death screen. */
public final class DeathScreenMaxHearts {
	// Just below vanilla's score line at y 100.
	private static final int LINE_Y = 112;
	private static final int WHITE = 0xFFFFFFFF;

	private static @Nullable Component line;

	private DeathScreenMaxHearts() {
	}

	/** Registers the payload receiver and the death screen line. Call once from client init. */
	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(MaxHeartsLostPayload.TYPE, (payload, context) -> {
			// The combat kill packet that opens the death screen is handled before this payload. With no death screen
			// open, as with immediate respawn, the number is dropped so it can't show on a later death.
			if (context.client().gui.screen() instanceof DeathScreen) line = lineFor(payload.lost());
		});

		ScreenEvents.AFTER_INIT.register(DeathScreenMaxHearts::onScreenInit);
	}

	private static void onScreenInit(Minecraft client, Screen screen, int width, int height) {
		if (!(screen instanceof DeathScreen)) return;

		ScreenEvents.afterExtract(screen).register(DeathScreenMaxHearts::drawLine);
		ScreenEvents.remove(screen).register(removed -> line = null);
	}

	private static void drawLine(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
		if (line == null) return;

		graphics.centeredText(Screens.getFont(screen), line, screen.width / 2, LINE_Y, WHITE);
	}

	private static Component lineFor(int lost) {
		Component count = Component.literal(Integer.toString(lost)).withStyle(ChatFormatting.YELLOW);

		return Component.translatable("deathScreen.maple_flavored.max_hearts_lost", count);
	}
}
