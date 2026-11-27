package net.aeroclient.gui;

import net.aeroclient.AeroClient;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Aero Client's startup loading screen: dark background, the logo, and a
 * progress bar that fills up as the game loads. It replaces the white Mojang
 * splash screen.
 *
 * While the game is loading it can't draw in between, so the bar can't move
 * on its own. Instead, each time the game finishes a loading stage it calls
 * {@link #progress}, and the bar glides to its new spot over a few frames so
 * it looks smooth.
 *
 * If anything here goes wrong, it turns itself off and the game shows the
 * normal Mojang splash instead, so loading never breaks.
 */
public final class AeroLoadingScreen {

	private static final int FRAMES_PER_STEP = 3;
	private static final int GL_PROJECTION = 5889;
	private static final int GL_MODELVIEW = 5888;

	private static float shownProgress = 0.0F;
	private static String status = "Starting up";
	private static boolean disabled = false;

	private AeroLoadingScreen() {
	}

	/**
	 * Draws the first frame. Returns true if it worked, so the game skips the
	 * Mojang splash, or false so the game draws the Mojang splash as usual.
	 */
	public static boolean begin(Minecraft mc) {
		if (disabled) {
			return false;
		}
		shownProgress = 0.0F;
		status = "Starting up";
		try {
			drawFrame(mc);
			return true;
		} catch (Throwable t) {
			fail(t);
			return false;
		}
	}

	/**
	 * Moves the bar to {@code target} (0.0 to 1.0) and shows what's loading next.
	 */
	public static void progress(Minecraft mc, float target, String newStatus) {
		if (disabled) {
			return;
		}
		try {
			status = newStatus;
			float start = shownProgress;
			for (int frame = 1; frame <= FRAMES_PER_STEP; frame++) {
				float t = (float) frame / (float) FRAMES_PER_STEP;
				float eased = 1.0F - (1.0F - t) * (1.0F - t); // ease-out: fast start, gentle stop
				shownProgress = start + (target - start) * eased;
				drawFrame(mc);
			}
		} catch (Throwable t) {
			fail(t);
		}
	}

	private static void fail(Throwable t) {
		disabled = true;
		AeroClient.LOGGER.error("Aero Client loading screen failed, using the normal one instead");
		AeroClient.LOGGER.error(t);
	}

	private static void drawFrame(Minecraft mc) {
		ScaledResolution res = new ScaledResolution(mc);
		int width = res.getScaledWidth();
		int height = res.getScaledHeight();

		// Set up 2D drawing in GUI coordinates, the same way the Mojang splash does.
		GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
		GlStateManager.matrixMode(GL_PROJECTION);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0D, (double) width, (double) height, 0.0D, 1000.0D, 3000.0D);
		GlStateManager.matrixMode(GL_MODELVIEW);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		GlStateManager.disableDepth();
		GlStateManager.enableTexture2D();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);

		Gui.drawRect(0, 0, width, height, AeroBranding.BACKGROUND);

		FontRenderer font = mc.fontRendererObj; // not ready during the first moments of loading
		int scale = AeroBranding.pickLogoScale(width, height);
		int logoW = AeroBranding.LOGO_W * scale;
		int logoH = AeroBranding.LOGO_H * scale;
		int barH = Math.max(4, scale * 2);
		int blockH = logoH + 12 + barH + 18;
		int top = (height - blockH) / 2;

		AeroBranding.drawLogo(mc, (width - logoW) / 2, top, scale, false);

		int barX = (width - logoW) / 2 + 2;
		int barY = top + logoH + 12;
		AeroBranding.drawLoadingBar(barX, barY, logoW - 4, barH, shownProgress);

		if (font != null) {
			String text = status + "...  " + Math.round(Math.max(0.0F, Math.min(1.0F, shownProgress)) * 100.0F) + "%";
			font.drawStringWithShadow(text, (width - font.getStringWidth(text)) / 2.0F, barY + barH + 7,
					AeroBranding.TEXT_FADED);
		}

		// Put things back the way the game expects them, then show the frame.
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableDepth();
		GlStateManager.matrixMode(GL_PROJECTION);
		GlStateManager.loadIdentity();
		GlStateManager.matrixMode(GL_MODELVIEW);
		GlStateManager.loadIdentity();
		mc.updateDisplay();
	}
}
