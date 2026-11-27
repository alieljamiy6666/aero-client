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
		nineSlice(circleTexture.getGlTextureId(), x, y, w, h, r, top, bottom, false, false);
	}

	/**
	 * Only the outer ring of a rounded rectangle, without its middle. Draw a
	 * slightly smaller filled shape on top to get a border: it looks the same
	 * as two full shapes but skips painting the big hidden middle twice, which
	 * helps slow graphics chips with large panels.
	 */
	public static void roundedRectEdge(float x, float y, float w, float h, float r, int color) {
		ensureTextures();
		nineSlice(circleTexture.getGlTextureId(), x, y, w, h, r, color, color, false, true);
	}

	/**
	 * Just the outline of a rounded rectangle: a smooth border {@code thickness}
	 * units wide with nothing inside, so a see-through fill under it stays
	 * see-through. Rings for each thickness-to-radius ratio are made once and kept.
	 */
	public static void roundedOutline(float x, float y, float w, float h, float r, float thickness, int color) {
		r = Math.min(r, Math.min(w, h) / 2.0F);
		if (r <= thickness + 0.01F) {
			roundedRect(x, y, w, h, r, color); // too thin to have a hole
			return;
		}
		int key = Math.max(1, Math.min(RING_STEPS - 1, Math.round(thickness / r * RING_STEPS)));
		DynamicTexture ring = RING_TEXTURES.get(key);
		if (ring == null) {
			ring = makeTexture(false, TEX_SIZE / 2.0F * (1.0F - key / (float) RING_STEPS));
			RING_TEXTURES.put(key, ring);
		}
		nineSlice(ring.getGlTextureId(), x, y, w, h, r, color, color, false, true);
	}

	private static final int RING_STEPS = 40;
	private static final java.util.Map<Integer, DynamicTexture> RING_TEXTURES = new java.util.HashMap<>();

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
				color, color, true, false);
	}

	/** Like {@link #glow}, but skips the part hidden under the shape (use it under solid shapes). */
	public static void glowOuter(float x, float y, float w, float h, float r, float size, int color) {
		ensureTextures();
		nineSlice(glowTexture.getGlTextureId(), x - size, y - size, w + size * 2.0F, h + size * 2.0F, r + size,
				color, color, true, true);
	}

	/** A soft shadow around a rounded rectangle (it darkens what's behind). */
	public static void shadow(float x, float y, float w, float h, float r, float size, int color) {
		ensureTextures();
		nineSlice(glowTexture.getGlTextureId(), x - size, y - size, w + size * 2.0F, h + size * 2.0F, r + size,
				color, color, false, false);
	}

	/** Like {@link #shadow}, but skips the part hidden under the shape (use it under solid shapes). */
	public static void shadowOuter(float x, float y, float w, float h, float r, float size, int color) {
		ensureTextures();
		nineSlice(glowTexture.getGlTextureId(), x - size, y - size, w + size * 2.0F, h + size * 2.0F, r + size,
				color, color, false, true);
	}

	// reused every call, so drawing shapes doesn't create garbage each frame
	private static final float[] XS = new float[4], YS = new float[4];
	private static final float[] UV = { 0.0F, 0.5F, 0.5F, 1.0F };
	private static final float BIG_CENTER = 1600.0F; // GUI units squared

	private static void nineSlice(int textureId, float x, float y, float w, float h, float corner, int top,
			int bottom, boolean additive, boolean skipCenter) {
		if (w <= 0.0F || h <= 0.0F) {
			return;
		}
		corner = Math.max(0.0F, Math.min(corner, Math.min(w, h) / 2.0F));
		XS[0] = x; XS[1] = x + corner; XS[2] = x + w - corner; XS[3] = x + w;
		YS[0] = y; YS[1] = y + corner; YS[2] = y + h - corner; YS[3] = y + h;
		// The middle piece is one flat color, so a big middle is drawn without the
		// texture afterwards: same look, much less work for slow graphics.
		boolean plainCenter = !skipCenter && (XS[2] - XS[1]) * (YS[2] - YS[1]) > BIG_CENTER;

		beginState(true, additive);
		GlStateManager.bindTexture(textureId);
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				float x0 = XS[col], x1 = XS[col + 1], y0 = YS[row], y1 = YS[row + 1];
				if (x1 <= x0 || y1 <= y0 || (row == 1 && col == 1 && (skipCenter || plainCenter))) {
					continue;
				}
				int c0 = Theme.mix(top, bottom, (y0 - y) / h);
				int c1 = Theme.mix(top, bottom, (y1 - y) / h);
				vertex(wr, x0, y1, UV[col], UV[row + 1], c1);
				vertex(wr, x1, y1, UV[col + 1], UV[row + 1], c1);
				vertex(wr, x1, y0, UV[col + 1], UV[row], c0);
				vertex(wr, x0, y0, UV[col], UV[row], c0);
			}
		}
		Tessellator.getInstance().draw();
		if (plainCenter) {
			int c0 = Theme.mix(top, bottom, (YS[1] - y) / h);
			int c1 = Theme.mix(top, bottom, (YS[2] - y) / h);
			GlStateManager.disableTexture2D();
			wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
			colorVertex(wr, XS[1], YS[2], c1);
			colorVertex(wr, XS[2], YS[2], c1);
			colorVertex(wr, XS[2], YS[1], c0);
			colorVertex(wr, XS[1], YS[1], c0);
			Tessellator.getInstance().draw();
		}
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

	// ---------------- clipping (hide what's outside a box) ----------------

	private static final int GL_ALWAYS = 519;
	private static final int GL_LEQUAL = 515;
	private static final int GL_DEPTH_BUFFER_BIT = 256;
	private static final double CLIP_Z = 950.0D; // closer to the screen than normal GUI drawing
	private static final float FAR = 100000.0F;
	private static final int MAX_CLIPS = 8;
	private static final float[] clipStack = new float[MAX_CLIPS * 4];
	private static int clipCount = 0;

	/**
	 * Hides everything drawn outside the box from (x0, y0) to (x1, y1) until
	 * {@link #popClip()}. Clips can be nested: the inner box is cut down to fit
	 * inside the outer one.
	 *
	 * The browser version has no "scissor" support here, so this uses the depth
	 * buffer: invisible walls are drawn around the box, closer to the screen
	 * than the GUI, and anything behind them is skipped.
	 */
	public static void pushClip(float x0, float y0, float x1, float y1) {
		if (clipCount > 0) {
			int top = (clipCount - 1) * 4;
			x0 = Math.max(x0, clipStack[top]);
			y0 = Math.max(y0, clipStack[top + 1]);
			x1 = Math.min(x1, clipStack[top + 2]);
			y1 = Math.min(y1, clipStack[top + 3]);
		}
		if (clipCount < MAX_CLIPS) {
			int i = clipCount * 4;
			clipStack[i] = x0;
			clipStack[i + 1] = y0;
			clipStack[i + 2] = x1;
			clipStack[i + 3] = Math.max(y0, y1);
		}
		clipCount++;
		applyClip();
	}

	/** Goes back to the clip before the last {@link #pushClip}, or to no clip at all. */
	public static void popClip() {
		if (clipCount <= 0) {
			return;
		}
		clipCount--;
		if (clipCount > 0) {
			applyClip();
		} else {
			GlStateManager.clear(GL_DEPTH_BUFFER_BIT); // wipe the invisible walls
			GlStateManager.depthFunc(GL_LEQUAL);
			GlStateManager.disableDepth();
		}
	}

	private static void applyClip() {
		int i = Math.min(clipCount, MAX_CLIPS) * 4 - 4;
		float x0 = clipStack[i], y0 = clipStack[i + 1], x1 = clipStack[i + 2], y1 = clipStack[i + 3];
		GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
		GlStateManager.colorMask(false, false, false, false);
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.depthFunc(GL_ALWAYS);
		GlStateManager.disableTexture2D();
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		wr.begin(7, DefaultVertexFormats.POSITION);
		clipWall(wr, -FAR, -FAR, FAR, y0); // above
		clipWall(wr, -FAR, y1, FAR, FAR); // below
		clipWall(wr, -FAR, y0, x0, y1); // left
		clipWall(wr, x1, y0, FAR, y1); // right
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.depthFunc(GL_LEQUAL);
	}

	private static void clipWall(WorldRenderer wr, float x0, float y0, float x1, float y1) {
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		wr.pos(x0, y1, CLIP_Z).endVertex();
		wr.pos(x1, y1, CLIP_Z).endVertex();
		wr.pos(x1, y0, CLIP_Z).endVertex();
		wr.pos(x0, y0, CLIP_Z).endVertex();
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
		return makeTexture(glow, -1.0F);
	}

	/** A soft circle, a glow, or (when {@code innerRadius} is 0 or more) a ring with a hole in the middle. */
	private static DynamicTexture makeTexture(boolean glow, float innerRadius) {
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
					if (innerRadius >= 0.0F) {
						alpha = Math.min(alpha, Math.max(0.0F, Math.min(1.0F, d - innerRadius))); // the hole
					}
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
