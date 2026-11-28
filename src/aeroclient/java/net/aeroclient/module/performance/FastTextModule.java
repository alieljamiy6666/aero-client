package net.aeroclient.module.performance;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.lax1dude.eaglercraft.minecraft.EaglerFontRenderer;
import net.minecraft.client.gui.FontRenderer;

/**
 * Draws text with far fewer draw calls, which is what slows browsers down
 * the most.
 *
 * - Aero's HUD text (FPS, Ping, Coordinates, Clock...) is kept ready between
 *   frames and each HUD element's text is drawn in one go, instead of one or
 *   two draws for every line.
 * - Text with special symbols (like the stars, hearts and arrows many servers
 *   use in ranks, the tab list and the scoreboard) normally draws every single
 *   letter on its own. Fast Text draws the whole line at once.
 *
 * The text looks exactly the same.
 */
public class FastTextModule extends Module {

	private static FastTextModule instance;

	public FastTextModule() {
		super("Fast Text", "Draws text in far fewer steps. Looks the same, runs faster.", Category.RENDER, 0);
		instance = this;
		enabledByDefault();
	}

	/** True while the module is on. */
	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/**
	 * Starts a text batch on this font if Fast Text is on. Everything drawn
	 * until {@link #endBatch} (with the same GL matrix) becomes one draw call.
	 * Returns true if a batch was started.
	 */
	public static boolean beginBatch(FontRenderer font) {
		if (active() && font instanceof EaglerFontRenderer) {
			((EaglerFontRenderer) font).aeroBeginBatch();
			return true;
		}
		return false;
	}

	/** Draws the text collected since {@link #beginBatch}. */
	public static void endBatch(FontRenderer font) {
		((EaglerFontRenderer) font).aeroEndBatch();
	}
}
