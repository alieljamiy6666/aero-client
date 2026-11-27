package net.aeroclient.util;

import net.minecraft.client.gui.FontRenderer;

/**
 * Text helpers for Aero Client screens: centered text, text with a shadow,
 * and text that cuts off with "..." when it doesn't fit.
 */
public final class TextUtil {

	private static final String ELLIPSIS = "...";

	private TextUtil() {
	}

	/** Shortens {@code text} with "..." so it fits in {@code maxWidth} pixels. */
	public static String trim(FontRenderer font, String text, int maxWidth) {
		if (text == null) {
			return "";
		}
		if (font.getStringWidth(text) <= maxWidth) {
			return text;
		}
		int room = maxWidth - font.getStringWidth(ELLIPSIS);
		return room <= 0 ? ELLIPSIS : font.trimStringToWidth(text, room) + ELLIPSIS;
	}

	/** Text with a soft shadow, starting at (x, y). */
	public static void drawShadow(FontRenderer font, String text, float x, float y, int color) {
		font.drawStringWithShadow(text, x, y, color);
	}

	/** Text centered on {@code centerX}, with a shadow. */
	public static void drawCentered(FontRenderer font, String text, float centerX, float y, int color) {
		font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2.0F, y, color);
	}

	/** Centered text that cuts off with "..." if it's wider than {@code maxWidth}. */
	public static void drawCenteredTrimmed(FontRenderer font, String text, float centerX, float y, int maxWidth,
			int color) {
		drawCentered(font, trim(font, text, maxWidth), centerX, y, color);
	}

	/** Left-aligned text that cuts off with "..." if it's wider than {@code maxWidth}. */
	public static void drawTrimmed(FontRenderer font, String text, float x, float y, int maxWidth, int color) {
		font.drawStringWithShadow(trim(font, text, maxWidth), x, y, color);
	}
}
