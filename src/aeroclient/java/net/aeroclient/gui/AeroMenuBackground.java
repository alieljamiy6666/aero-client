package net.aeroclient.gui;

import java.util.Random;

import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

/**
 * The animated title-screen background: a night sky with a glowing aurora,
 * twinkling stars, a square Minecraft moon, the odd shooting star, glowing
 * motes drifting up, and two layers of blocky hills. Moving the mouse shifts
 * the layers by different amounts, which gives a feeling of depth.
 *
 * Everything is plain colored shapes drawn in a handful of batches, so it's
 * much lighter than the vanilla panorama (which redraws the sky dozens of
 * times per frame to blur it).
 */
public final class AeroMenuBackground {

	private static final int STAR_COUNT = 150;
	private static final int MOTE_COUNT = 46;

	// Star data, made once: x, y (0..1 of the screen), size, twinkle speed, phase, depth.
	private static float[][] stars;
	// Mote data: x (0..1), speed, sway, phase, size.
	private static float[][] motes;
	private static float smoothMouseX = 0.5F, smoothMouseY = 0.5F;

	private AeroMenuBackground() {
	}

	public static void draw(Minecraft mc, int width, int height, int mouseX, int mouseY) {
		if (stars == null) {
			makeData();
		}
		float t = Animation.seconds();

		// Glide toward the mouse so the parallax feels smooth, not twitchy.
		smoothMouseX += ((float) mouseX / Math.max(1, width) - smoothMouseX) * 0.08F;
		smoothMouseY += ((float) mouseY / Math.max(1, height) - smoothMouseY) * 0.08F;
		float px = smoothMouseX - 0.5F, py = smoothMouseY - 0.5F;

		// 1. Sky: deep navy at the top, fading to the page background.
		RenderUtil.gradientV(0, 0, width, height * 0.62F, 0xFF060A16, 0xFF0B1426);
		RenderUtil.gradientV(0, height * 0.62F, width, height, 0xFF0B1426, Theme.background);

		drawStars(width, height, t, px, py);
		drawMoon(width, height, px, py);
		drawAurora(width, height, t, px);
		drawShootingStar(width, height, t);
		drawHills(width, height, t, px, py);
		drawMotes(width, height, t, px);

		// Soft blue glow behind the logo.
		int lx = AeroBranding.titleLogoX(width, height), ly = AeroBranding.titleLogoY(width, height);
		int ls = AeroBranding.titleLogoScale(width, height);
		RenderUtil.glow(lx + 10, ly + 6, AeroBranding.LOGO_W * ls - 20, AeroBranding.LOGO_H * ls - 12, 10, 34,
				Theme.withAlpha(Theme.accent, 0.10F + 0.03F * MathHelper.sin(t * 1.3F)));

		// Darker edges (a vignette) pull your eye to the middle.
		RenderUtil.gradientV(0, 0, width, height * 0.22F, 0x66000000, 0x00000000);
		RenderUtil.gradientV(0, height * 0.78F, width, height, 0x00000000, 0x80000000);
		RenderUtil.gradientH(0, 0, width * 0.16F, height, 0x55000000, 0x00000000);
		RenderUtil.gradientH(width * 0.84F, 0, width, height, 0x00000000, 0x55000000);
	}

	private static void makeData() {
		Random r = new Random(0xAE20L);
		stars = new float[STAR_COUNT][];
		for (int i = 0; i < STAR_COUNT; i++) {
			float y = r.nextFloat();
			stars[i] = new float[] { r.nextFloat(), y * y * 0.68F, r.nextFloat() < 0.12F ? 2.0F : 1.0F,
					0.6F + r.nextFloat() * 2.2F, r.nextFloat() * 6.28F, 0.3F + r.nextFloat() * 0.7F };
		}
		motes = new float[MOTE_COUNT][];
		for (int i = 0; i < MOTE_COUNT; i++) {
			motes[i] = new float[] { r.nextFloat(), 6.0F + r.nextFloat() * 14.0F, 3.0F + r.nextFloat() * 9.0F,
					r.nextFloat() * 100.0F, r.nextFloat() < 0.25F ? 2.0F : 1.0F };
		}
	}

	private static void drawStars(int width, int height, float t, float px, float py) {
		RenderUtil.beginBatch(true);
		for (float[] s : stars) {
			float x = s[0] * width - px * 10.0F * s[5];
			float y = s[1] * height - py * 6.0F * s[5];
			float twinkle = 0.5F + 0.5F * MathHelper.sin(t * s[3] + s[4]);
			float alpha = (0.25F + 0.75F * twinkle) * (0.45F + 0.55F * s[5]);
			int color = Theme.withAlpha(s[2] > 1.0F ? 0xFFE0F2FE : 0xFFFFFFFF, alpha);
			RenderUtil.batchRect(x, y, x + s[2], y + s[2], color);
			if (s[2] > 1.0F && twinkle > 0.85F) { // bright stars get a little sparkle cross
				int faint = Theme.withAlpha(0xFFBAE6FD, (twinkle - 0.85F) * 3.0F);
				RenderUtil.batchRect(x - 2.0F, y + 0.5F, x + 4.0F, y + 1.5F, faint);
				RenderUtil.batchRect(x + 0.5F, y - 2.0F, x + 1.5F, y + 4.0F, faint);
			}
		}
		RenderUtil.endBatch();
	}

	private static void drawMoon(int width, int height, float px, float py) {
		float size = Math.max(10.0F, Math.min(width, height) * 0.055F);
		float x = width * 0.80F - px * 6.0F, y = height * 0.12F - py * 4.0F;
		RenderUtil.glow(x, y, size, size, 2.0F, size * 1.4F, Theme.withAlpha(0xFF7DD3FC, 0.28F));
		RenderUtil.rect(x, y, x + size, y + size, 0xFFE6EEF8);
		float c = size / 8.0F; // little square craters, like the Minecraft moon
		int crater = 0xFFC3CFDD;
		RenderUtil.rect(x + c * 1, y + c * 2, x + c * 3, y + c * 4, crater);
		RenderUtil.rect(x + c * 5, y + c * 1, x + c * 6, y + c * 2, crater);
		RenderUtil.rect(x + c * 4, y + c * 5, x + c * 7, y + c * 7, crater);
		RenderUtil.rect(x + c * 2, y + c * 6, x + c * 3, y + c * 7, crater);
	}

	/** Three glowing ribbons that ripple slowly, with faint vertical "curtain" streaks. */
	private static void drawAurora(int width, int height, float t, float px) {
		int[] colors = { 0xFF38BDF8, 0xFF2DD4BF, 0xFF818CF8 };
		float[] strength = { 0.50F, 0.34F, 0.28F };
		float[] baseY = { 0.30F, 0.22F, 0.38F };
		RenderUtil.beginBatch(true);
		int steps = Math.max(24, width / 6);
		for (int k = 0; k < 3; k++) {
			float thick = height * (0.16F + 0.04F * k);
			float shift = -px * (14.0F + k * 6.0F);
			float prevX = 0, prevY = 0, prevA = 0;
			for (int i = 0; i <= steps; i++) {
				float x = (float) i / steps * (width + 40) - 20 + shift;
				float y = height * baseY[k] + MathHelper.sin(x * 0.010F + t * (0.35F + 0.1F * k) + k * 2.1F) * height * 0.06F
						+ MathHelper.sin(x * 0.023F - t * (0.5F + 0.07F * k)) * height * 0.025F;
				float curtain = 0.55F + 0.45F * MathHelper.sin(x * 0.09F + t * 0.8F + k);
				float fadeEdges = Math.min(1.0F, Math.min(i, steps - i) / (steps * 0.18F));
				float a = strength[k] * curtain * fadeEdges * (0.75F + 0.25F * MathHelper.sin(t * 0.6F + k));
				if (i > 0) {
					int c0 = Theme.withAlpha(colors[k], prevA), c1 = Theme.withAlpha(colors[k], a);
					int clear = colors[k] & 0x00FFFFFF;
					// tall soft glow fading upward from the bright lower edge
					RenderUtil.batchPoly(prevX, prevY, c0, x, y, c1, x, y - thick, clear, prevX, prevY - thick, clear);
					// short fade just below the bright edge
					RenderUtil.batchPoly(prevX, prevY + 5.0F, clear, x, y + 5.0F, clear, x, y, c1, prevX, prevY, c0);
					// a thin, brighter core along the edge gives the ribbon a crisp glowing line
					int core0 = Theme.withAlpha(0xFFE0F2FE, prevA * 0.55F), core1 = Theme.withAlpha(0xFFE0F2FE, a * 0.55F);
					RenderUtil.batchPoly(prevX, prevY + 1.0F, core0, x, y + 1.0F, core1, x, y - 1.0F, core1, prevX, prevY - 1.0F, core0);
				}
				prevX = x;
				prevY = y;
				prevA = a;
			}
		}
		RenderUtil.endBatch();
	}

	/** Every 9 seconds a shooting star zips across the top of the sky. */
	private static void drawShootingStar(int width, int height, float t) {
		float cycle = t % 9.0F;
		if (cycle > 0.9F) {
			return;
		}
		int which = (int) (t / 9.0F);
		float startX = width * (0.15F + 0.5F * ((which * 37) % 10) / 10.0F);
		float startY = height * (0.05F + 0.1F * ((which * 53) % 10) / 10.0F);
		float p = cycle / 0.9F;
		float headX = startX + p * width * 0.35F, headY = startY + p * height * 0.18F;
		float fade = MathHelper.sin(p * (float) Math.PI);
		RenderUtil.beginBatch(true);
		for (int i = 0; i < 14; i++) {
			float k = i / 14.0F;
			float x = headX - k * width * 0.08F, y = headY - k * height * 0.04F;
			RenderUtil.batchRect(x, y, x + 1.5F, y + 1.5F, Theme.withAlpha(0xFFE0F2FE, fade * (1.0F - k)));
		}
		RenderUtil.endBatch();
	}

	/** Two rows of blocky hills (and a few trees) drawn as stacked squares. */
	private static void drawHills(int width, int height, float t, float px, float py) {
		float block = Math.max(4.0F, Math.round(height / 60.0F));
		drawHillLayer(width, height, block, height * 0.70F, height * 0.07F, 11.3F, -px * 8.0F, 0xFF10203A, false, 0);
		// thin blue haze between the layers adds depth
		RenderUtil.gradientV(0, height * 0.70F, width, height * 0.86F, 0x0038BDF8, 0x1238BDF8);
		drawHillLayer(width, height, block * 1.5F, height * 0.82F, height * 0.06F, 4.7F, -px * 16.0F, 0xFF0A1322,
				true, 0xFF1C4466);
	}

	private static void drawHillLayer(int width, int height, float block, float baseY, float amp, float seed,
			float shift, int color, boolean trees, int rimColor) {
		RenderUtil.beginBatch(false);
		int start = (int) Math.floor((-shift - block * 4) / block);
		int end = (int) Math.ceil((width - shift + block * 4) / block);
		for (int col = start; col <= end; col++) {
			float wave = MathHelper.sin(col * 0.21F + seed) * 0.6F + MathHelper.sin(col * 0.07F + seed * 2.0F) * 0.9F
					+ MathHelper.sin(col * 0.53F + seed * 3.0F) * 0.25F;
			float top = baseY - Math.round(wave * amp / block) * block;
			float x = col * block + shift;
			RenderUtil.batchRect(x, top, x + block, height, color);
			if (rimColor != 0) { // moonlit edge on the top block
				RenderUtil.batchRect(x, top, x + block, top + 1.0F, rimColor);
			}
			if (trees && hash(col, seed) % 9 == 0) { // a little tree: trunk and leaves
				RenderUtil.batchRect(x + block * 0.33F, top - block * 2, x + block * 0.67F, top, color);
				RenderUtil.batchRect(x - block, top - block * 3.5F, x + block * 2, top - block * 1.5F, color);
				RenderUtil.batchRect(x - block * 0.5F, top - block * 4.5F, x + block * 1.5F, top - block * 3.5F, color);
			}
		}
		RenderUtil.endBatch();
	}

	private static int hash(int col, float seed) {
		int h = col * 73856093 ^ Float.floatToIntBits(seed) * 19349663;
		h ^= (h >>> 13);
		return Math.abs(h * 0x5bd1e995);
	}

	/** Tiny glowing specks drifting up and swaying, like dust in moonlight. */
	private static void drawMotes(int width, int height, float t, float px) {
		RenderUtil.beginBatch(true);
		float travel = height + 20.0F;
		for (float[] m : motes) {
			float life = ((t * m[1] + m[3] * travel) % travel) / travel; // 0 at bottom, 1 at top
			float y = height + 10.0F - life * travel;
			float x = m[0] * width + MathHelper.sin(t * 0.7F + m[3]) * m[2] - px * 20.0F;
			float alpha = MathHelper.sin(life * (float) Math.PI) * 0.7F;
			RenderUtil.batchRect(x, y, x + m[4], y + m[4], Theme.withAlpha(Theme.accentLight, alpha));
		}
		RenderUtil.endBatch();
	}
}
