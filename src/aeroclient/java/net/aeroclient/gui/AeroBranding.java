package net.aeroclient.gui;

import net.aeroclient.AeroClient;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * Aero Client's look on the title screen and loading screen: the animated
 * logo, the loading bar, the version text and the port credit.
 *
 * The logo lives in one 128x128 texture (assets/minecraft/aeroclient/logo.png)
 * that holds several parts: the "AERO CLIENT" wordmark, the "A" badge, and
 * white masks used for the shine effect.
 *
 * The colors here are temporary. Step 4 moves them into the Theme class so
 * every screen shares one set of colors.
 */
public final class AeroBranding {

	public static final ResourceLocation LOGO = new ResourceLocation("aeroclient/logo.png");
	private static final float TEXTURE_SIZE = 128.0F;

	// Parts of the logo texture: {x, y, width, height} in art pixels.
	private static final int[] WORDMARK = { 0, 0, 63, 30 };
	private static final int[] WORDMARK_SHINE = { 0, 31, 63, 30 };
	private static final int[] EMBLEM = { 0, 62, 28, 28 };
	private static final int[] EMBLEM_SHINE = { 30, 62, 28, 28 };

	/** Size of the whole logo (badge + wordmark), in art pixels. */
	public static final int LOGO_W = 93;
	public static final int LOGO_H = 30;

	// Where each part sits inside the whole logo.
	private static final int EMBLEM_X = 0, EMBLEM_Y = 1;
	private static final int WORDMARK_X = 30, WORDMARK_Y = 0;

	// Columns of each shine mask that actually contain shape (the rest is glow padding).
	private static final int EMBLEM_SHINE_FROM = 3, EMBLEM_SHINE_TO = 25;
	private static final int WORDMARK_SHINE_FROM = 3, WORDMARK_SHINE_TO = 60;

	// Animation timing.
	private static final long FLOAT_PERIOD_MS = 3000L; // the badge floats up and down
	private static final long SHINE_PERIOD_MS = 5000L; // a shine sweeps across every 5 seconds...
	private static final long SHINE_SWEEP_MS = 900L; // ...and takes 0.9 seconds to cross
	private static final float SHINE_HALF_WIDTH = 4.0F; // in art pixels
	private static final float SHINE_STRENGTH = 0.55F;

	/** Colors, as 0xAARRGGBB. */
	public static final int BACKGROUND = 0xFF0E1116;
	public static final int PANEL = 0xFF1F2630;
	public static final int ACCENT = 0xFF38BDF8;
	public static final int ACCENT_LIGHT = 0xFF7DD3FC;
	public static final int TEXT = 0xFFE5E7EB;
	public static final int TEXT_FADED = 0xFF8B949E;

	/** Credit for the Eaglercraft 1.12 port that Aero Client is built on. */
	public static final String CREDIT_LINE = "Based on Eaglercraft 1.12 by PeytonPlayz595";

	private AeroBranding() {
	}

	/**
	 * Draws the whole logo with its top-left corner at (x, y). Each art pixel
	 * becomes {@code scale} screen pixels, so whole numbers keep it sharp.
	 * With {@code animate} on, the badge floats and a shine sweeps across.
	 */
	public static void drawLogo(Minecraft mc, int x, int y, int scale, boolean animate) {
		long time = Minecraft.getSystemTime();
		mc.getTextureManager().bindTexture(LOGO);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); // normal see-through blending

		float bob = 0.0F;
		if (animate) {
			float wave = (float) (time % FLOAT_PERIOD_MS) / (float) FLOAT_PERIOD_MS;
			bob = MathHelper.sin(wave * (float) (Math.PI * 2.0D)) * scale;
		}

		int emblemX = x + EMBLEM_X * scale, emblemY = y + EMBLEM_Y * scale;
		int wordX = x + WORDMARK_X * scale, wordY = y + WORDMARK_Y * scale;

		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, bob, 0.0F);
		drawPart(EMBLEM, emblemX, emblemY, scale);
		GlStateManager.popMatrix();
		drawPart(WORDMARK, wordX, wordY, scale);

		if (animate) {
			long phase = time % SHINE_PERIOD_MS;
			if (phase < SHINE_SWEEP_MS) {
				float travel = LOGO_W + SHINE_HALF_WIDTH * 2.0F;
				float pos = -SHINE_HALF_WIDTH + travel * phase / (float) SHINE_SWEEP_MS;
				GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0); // add light on top
				GlStateManager.pushMatrix();
				GlStateManager.translate(0.0F, bob, 0.0F);
				drawShine(EMBLEM_SHINE, emblemX, emblemY, scale, pos - EMBLEM_X, EMBLEM_SHINE_FROM, EMBLEM_SHINE_TO);
				GlStateManager.popMatrix();
				drawShine(WORDMARK_SHINE, wordX, wordY, scale, pos - WORDMARK_X, WORDMARK_SHINE_FROM,
						WORDMARK_SHINE_TO);
				GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			}
		}

		GlStateManager.disableBlend();
	}

	private static void drawPart(int[] part, int x, int y, int scale) {
		Gui.drawScaledCustomSizeModalRect(x, y, part[0], part[1], part[2], part[3], part[2] * scale,
				part[3] * scale, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	/** Draws a soft band of light, one art-pixel column at a time, centered on {@code pos}. */
	private static void drawShine(int[] mask, int x, int y, int scale, float pos, int from, int to) {
		int first = Math.max(from, (int) Math.floor(pos - SHINE_HALF_WIDTH));
		int last = Math.min(to - 1, (int) Math.ceil(pos + SHINE_HALF_WIDTH));
		for (int col = first; col <= last; col++) {
			float strength = 1.0F - Math.abs(col + 0.5F - pos) / SHINE_HALF_WIDTH;
			if (strength <= 0.0F) {
				continue;
			}
			GlStateManager.color(1.0F, 1.0F, 1.0F, strength * SHINE_STRENGTH);
			Gui.drawScaledCustomSizeModalRect(x + col * scale, y, mask[0] + col, mask[1], 1, mask[3], scale,
					mask[3] * scale, TEXTURE_SIZE, TEXTURE_SIZE);
		}
	}

	// ---------------- title screen ----------------

	/** Logo size for the title screen: as big as fits above the buttons, at most 3. */
	public static int titleLogoScale(int width, int height) {
		int room = height / 4 + 48 - 24;
		int scale = Math.min((width - 20) / LOGO_W, room / LOGO_H);
		return Math.max(1, Math.min(3, scale));
	}

	public static int titleLogoX(int width, int height) {
		return width / 2 - LOGO_W * titleLogoScale(width, height) / 2;
	}

	public static int titleLogoY(int width, int height) {
		int scale = titleLogoScale(width, height);
		return Math.max(6, (height / 4 + 48 - LOGO_H * scale) / 2 - 2);
	}

	/** Draws the animated logo where the Minecraft title used to be. */
	public static void drawTitleLogo(Minecraft mc, int width, int height) {
		drawLogo(mc, titleLogoX(width, height), titleLogoY(width, height), titleLogoScale(width, height), true);
	}

	/** Where the yellow splash text sits: just off the bottom-right corner of the logo. */
	public static float splashX(int width, int height) {
		return titleLogoX(width, height) + (LOGO_W - 4) * titleLogoScale(width, height);
	}

	public static float splashY(int width, int height) {
		return titleLogoY(width, height) + (LOGO_H + 1) * titleLogoScale(width, height);
	}

	/** Bottom-left of the title screen: "Aero Client v1.0.0" and the port credit. */
	public static void drawVersionText(FontRenderer font, int screenHeight) {
		font.drawStringWithShadow(AeroClient.getDisplayName(), 2, screenHeight - 20, ACCENT_LIGHT);
		font.drawStringWithShadow(CREDIT_LINE, 2, screenHeight - 10, 0xFFB0B0B0);
	}

	// ---------------- shapes ----------------

	/** Picks a whole-number logo size for a loading screen of this size (between 1 and 4). */
	public static int pickLogoScale(int screenWidth, int screenHeight) {
		int scale = Math.min((screenWidth - 40) / LOGO_W, screenHeight / (LOGO_H * 3));
		return Math.max(1, Math.min(4, scale));
	}

	/** A filled bar with its corners cut by one pixel, which reads as rounded in pixel art. */
	public static void drawPill(int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (width < 3 || height < 3) {
			Gui.drawRect(x, y, x + width, y + height, color);
			return;
		}
		Gui.drawRect(x + 1, y, x + width - 1, y + height, color);
		Gui.drawRect(x, y + 1, x + width, y + height - 1, color);
	}

	/**
	 * The loading bar: a dark track, the blue fill with a lighter top edge, and
	 * a glowing tip at the front of the fill.
	 */
	public static void drawLoadingBar(int x, int y, int width, int height, float progress) {
		progress = Math.max(0.0F, Math.min(1.0F, progress));
		drawPill(x, y, width, height, PANEL);
		int fill = Math.round(width * progress);
		if (fill < 2) {
			return;
		}
		drawPill(x, y, fill, height, ACCENT);
		Gui.drawRect(x + 1, y + 1, x + fill - 1, y + 2, ACCENT_LIGHT); // shine along the top
		int tip = x + fill;
		Gui.drawRect(tip - 4, y - 2, tip + 3, y + height + 2, 0x2638BDF8); // outer glow
		Gui.drawRect(tip - 3, y - 1, tip + 2, y + height + 1, 0x4D7DD3FC); // inner glow
		Gui.drawRect(tip - 2, y, tip, y + height, 0xFFF0F9FF); // bright head
	}
}
