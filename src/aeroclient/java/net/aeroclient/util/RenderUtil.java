package net.aeroclient.util;

import net.aeroclient.theme.Theme;
import net.lax1dude.eaglercraft.opengl.EaglercraftGPU;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Fast, smooth shape drawing for Aero Client screens: rounded rectangles,
 * pills, circles, soft glows and gradients.
 *
 * How rounded corners work: we make two small textures once (a smooth white
 * circle and a soft glow). A rounded rectangle is drawn as 9 pieces: the 4
 * corners use the 4 quarters of the circle, and the edges and middle are
 * stretched from its solid center. That's only 9 quads per shape, and the
 * edges stay smooth at any size, which matters on slow Chromebooks.
 *
 * Positions are in GUI coordinates (the same ones buttons use) and can be
 * fractions. Colors are 0xAARRGGBB.
 */
public final class RenderUtil {

	private static final int TEX_SIZE = 64;
	private static DynamicTexture circleTexture;
	private static DynamicTexture glowTexture;

	private RenderUtil() {
	}

	// ---------------- rounded shapes ----------------

	/** A filled rectangle with rounded corners of radius {@code r}. */
	public static void roundedRect(float x, float y, float w, float h, float r, int color) {
		roundedRectGradient(x, y, w, h, r, color, color);
	}

	/** A rounded rectangle that fades from {@code top} color to {@code bottom} color. */
	public static void roundedRectGradient(float x, float y, float w, float h, float r, int top, int bottom) {
		ensureTextures();
		nineSlice(circleTexture.getGlTextureId(), x, y, w, h, r, top, bottom, false);
	}

	/** A pill: a rounded rectangle whose ends are full half-circles. */
	public static void pill(float x, float y, float w, float h, int color) {
		roundedRect(x, y, w, h, Math.min(w, h) / 2.0F, color);
	}

	/** A filled circle. */
	public static void circle(float centerX, float centerY, float radius, int color) {
		roundedRect(centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F, radius, color);
	}

	/**
	 * A soft glow of light around a rounded rectangle (it adds brightness).
	 * {@code size} is how far the glow spreads past the shape's edge.
	 */
	public static void glow(float x, float y, float w, float h, float r, float size, int color) {
		ensureTextures();
		nineSlice(glowTexture.getGlTextureId(), x - size, y - size, w + size * 2.0F, h + size * 2.0F, r + size,
				color, color, true);
	}

	/** A soft shadow around a rounded rectangle (it darkens what's behind). */
	public static void shadow(float x, float y, float w, float h, float r, float size, int color) {
		ensureTextures();
		nineSlice(glowTexture.getGlTextureId(), x - size, y - size, w + size * 2.0F, h + size * 2.0F, r + size,
				color, color, false);
	}

	private static void nineSlice(int textureId, float x, float y, float w, float h, float corner, int top,
			int bottom, boolean additive) {
		if (w <= 0.0F || h <= 0.0F) {
			return;
		}
		corner = Math.max(0.0F, Math.min(corner, Math.min(w, h) / 2.0F));
		float[] xs = { x, x + corner, x + w - corner, x + w };
		float[] ys = { y, y + corner, y + h - corner, y + h };
		float[] uv = { 0.0F, 0.5F, 0.5F, 1.0F };

		beginState(true, additive);
		GlStateManager.bindTexture(textureId);
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				float x0 = xs[col], x1 = xs[col + 1], y0 = ys[row], y1 = ys[row + 1];
				if (x1 <= x0 || y1 <= y0) {
					continue;
				}
				int c0 = Theme.mix(top, bottom, (y0 - y) / h);
				int c1 = Theme.mix(top, bottom, (y1 - y) / h);
				vertex(wr, x0, y1, uv[col], uv[row + 1], c1);
				vertex(wr, x1, y1, uv[col + 1], uv[row + 1], c1);
				vertex(wr, x1, y0, uv[col + 1], uv[row], c0);
				vertex(wr, x0, y0, uv[col], uv[row], c0);
			}
		}
		Tessellator.getInstance().draw();
		endState();
	}

	// ---------------- plain rectangles and gradients ----------------

	public static void rect(float x0, float y0, float x1, float y1, int color) {
		gradient(x0, y0, x1, y1, color, color, color, color, false);
	}

	/** Top-to-bottom fade. */
	public static void gradientV(float x0, float y0, float x1, float y1, int top, int bottom) {
		gradient(x0, y0, x1, y1, top, top, bottom, bottom, false);
	}

	/** Left-to-right fade. */
	public static void gradientH(float x0, float y0, float x1, float y1, int left, int right) {
		gradient(x0, y0, x1, y1, left, right, right, left, false);
	}

	/** Corner colors in order: top-left, top-right, bottom-right, bottom-left. */
	public static void gradient(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl,
			boolean additive) {
		beginState(false, additive);
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		colorVertex(wr, x0, y1, bl);
		colorVertex(wr, x1, y1, br);
		colorVertex(wr, x1, y0, tr);
		colorVertex(wr, x0, y0, tl);
		Tessellator.getInstance().draw();
		endState();
	}

	// ---------------- batches: many small quads in one draw ----------------

	private static boolean batching = false;

	/** Start a batch of plain colored quads. Finish with {@link #endBatch()}. */
	public static void beginBatch(boolean additive) {
		beginState(false, additive);
		Tessellator.getInstance().getBuffer().begin(7, DefaultVertexFormats.POSITION_COLOR);
		batching = true;
	}

	public static void batchRect(float x0, float y0, float x1, float y1, int color) {
		batchQuad(x0, y0, x1, y1, color, color, color, color);
	}

	public static void batchGradientV(float x0, float y0, float x1, float y1, int top, int bottom) {
		batchQuad(x0, y0, x1, y1, top, top, bottom, bottom);
	}

	/** Corner colors in order: top-left, top-right, bottom-right, bottom-left. */
	public static void batchQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
		if (!batching) {
			return;
		}
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		colorVertex(wr, x0, y1, bl);
		colorVertex(wr, x1, y1, br);
		colorVertex(wr, x1, y0, tr);
		colorVertex(wr, x0, y0, tl);
	}

	/** A four-sided shape with any corner positions (for slanted ribbons). */
	public static void batchPoly(float ax, float ay, int ac, float bx, float by, int bc, float cx, float cy, int cc,
			float dx, float dy, int dc) {
		if (!batching) {
			return;
		}
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		colorVertex(wr, ax, ay, ac);
		colorVertex(wr, bx, by, bc);
		colorVertex(wr, cx, cy, cc);
		colorVertex(wr, dx, dy, dc);
	}

	public static void endBatch() {
		if (!batching) {
			return;
		}
		batching = false;
		Tessellator.getInstance().draw();
		endState();
	}

	// ---------------- internals ----------------

	private static void beginState(boolean textured, boolean additive) {
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha(); // keep soft, nearly see-through edges
		GlStateManager.shadeModel(RealOpenGLEnums.GL_SMOOTH);
		if (additive) {
			GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE,
					RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		} else {
			GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
					RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		}
		if (textured) {
			GlStateManager.enableTexture2D();
		} else {
			GlStateManager.disableTexture2D();
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void endState() {
		GlStateManager.shadeModel(RealOpenGLEnums.GL_FLAT);
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		GlStateManager.enableTexture2D();
		GlStateManager.enableAlpha();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void vertex(WorldRenderer wr, float x, float y, float u, float v, int color) {
		wr.pos(x, y, 0.0D).tex(u, v).color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255)
				.endVertex();
	}

	private static void colorVertex(WorldRenderer wr, float x, float y, int color) {
		wr.pos(x, y, 0.0D).color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255)
				.endVertex();
	}

	/** Makes the circle and glow textures the first time they're needed. */
	private static void ensureTextures() {
		if (circleTexture != null) {
			return;
		}
		circleTexture = makeTexture(false);
		glowTexture = makeTexture(true);
	}

	private static DynamicTexture makeTexture(boolean glow) {
		DynamicTexture texture = new DynamicTexture(TEX_SIZE, TEX_SIZE);
		int[] pixels = texture.getTextureData();
		float center = TEX_SIZE / 2.0F;
		for (int y = 0; y < TEX_SIZE; y++) {
			for (int x = 0; x < TEX_SIZE; x++) {
				float dx = x + 0.5F - center, dy = y + 0.5F - center;
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				float alpha;
				if (glow) {
					float t = Math.max(0.0F, 1.0F - d / center);
					alpha = (float) Math.pow(t, 1.6D); // soft falloff
				} else {
					alpha = Math.max(0.0F, Math.min(1.0F, center - d)); // smooth 1-pixel edge
				}
				pixels[y * TEX_SIZE + x] = (Math.round(alpha * 255.0F) << 24) | 0x00FFFFFF;
			}
		}
		texture.updateDynamicTexture();
		// Smooth (linear) scaling and no wrap-around, so edges stay clean at any size.
		GlStateManager.bindTexture(texture.getGlTextureId());
		EaglercraftGPU.glTexParameteri(RealOpenGLEnums.GL_TEXTURE_2D, RealOpenGLEnums.GL_TEXTURE_MIN_FILTER,
				RealOpenGLEnums.GL_LINEAR);
		EaglercraftGPU.glTexParameteri(RealOpenGLEnums.GL_TEXTURE_2D, RealOpenGLEnums.GL_TEXTURE_MAG_FILTER,
				RealOpenGLEnums.GL_LINEAR);
		EaglercraftGPU.glTexParameteri(RealOpenGLEnums.GL_TEXTURE_2D, RealOpenGLEnums.GL_TEXTURE_WRAP_S,
				RealOpenGLEnums.GL_CLAMP_TO_EDGE);
		EaglercraftGPU.glTexParameteri(RealOpenGLEnums.GL_TEXTURE_2D, RealOpenGLEnums.GL_TEXTURE_WRAP_T,
				RealOpenGLEnums.GL_CLAMP_TO_EDGE);
		return texture;
	}
}
