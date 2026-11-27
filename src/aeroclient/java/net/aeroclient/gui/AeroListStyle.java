package net.aeroclient.gui;

import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Aero Client's look for scrolling lists (servers, worlds, controls and so
 * on): a dark glass panel instead of dirt, a rounded highlight on the
 * selected entry, and a rounded scrollbar.
 *
 * Vanilla hides entries that scroll past the list's edges by painting dirt
 * over the top and bottom of the screen. We want the animated sky to stay
 * visible there, so instead we use the depth buffer as a stencil: invisible
 * "masks" cover the areas above and below the list, entries behind them
 * aren't drawn, and the masks are wiped afterwards.
 */
public final class AeroListStyle {

	private static final int GL_ALWAYS = 519;
	private static final int GL_LEQUAL = 515;
	private static final int GL_DEPTH_BUFFER_BIT = 256;
	private static final double MASK_Z = 950.0D; // closer to the screen than anything in the list

	private AeroListStyle() {
	}

	/** The see-through dark panel behind a list. */
	public static void drawPanel(int left, int top, int right, int bottom) {
		RenderUtil.rect(left, top, right, bottom, 0x990A0E15);
		RenderUtil.rect(left, top, right, top + 1, 0x33FFFFFF);
		RenderUtil.rect(left, bottom - 1, right, bottom, 0x22FFFFFF);
	}

	/** Starts hiding anything drawn above {@code top} or below {@code bottom}. */
	public static void beginClip(int top, int bottom, int screenWidth, int screenHeight) {
		GlStateManager.colorMask(false, false, false, false);
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.depthFunc(GL_ALWAYS);
		GlStateManager.disableTexture2D();
		WorldRenderer wr = Tessellator.getInstance().getBuffer();
		wr.begin(7, DefaultVertexFormats.POSITION);
		maskQuad(wr, 0, -10, screenWidth, top);
		maskQuad(wr, 0, bottom, screenWidth, screenHeight + 10);
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.depthFunc(GL_LEQUAL);
	}

	/** Stops hiding and wipes the masks so nothing else is affected. */
	public static void endClip() {
		GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
		GlStateManager.depthFunc(GL_LEQUAL);
	}

	private static void maskQuad(WorldRenderer wr, double x0, double y0, double x1, double y1) {
		wr.pos(x0, y1, MASK_Z).endVertex();
		wr.pos(x1, y1, MASK_Z).endVertex();
		wr.pos(x1, y0, MASK_Z).endVertex();
		wr.pos(x0, y0, MASK_Z).endVertex();
	}

	/** Rounded highlight behind the selected entry. */
	public static void drawSelection(int left, int top, int right, int bottom) {
		float w = right - left, h = bottom - top;
		RenderUtil.glow(left, top, w, h, 5.0F, 4.0F, Theme.withAlpha(Theme.accent, 0.25F));
		RenderUtil.roundedRect(left, top, w, h, 5.0F, Theme.withAlpha(Theme.accent, 0.9F));
		RenderUtil.roundedRectGradient(left + 1, top + 1, w - 2, h - 2, 4.0F, 0xF0172233, 0xF0111925);
	}

	/** Rounded scrollbar: a faint track and a blue handle. */
	public static void drawScrollbar(int left, int right, int top, int bottom, int handleTop, int handleHeight) {
		float w = right - left;
		RenderUtil.pill(left + 1, top + 2, w - 2, bottom - top - 4, 0x26FFFFFF);
		RenderUtil.pill(left + 1, handleTop + 1, w - 2, handleHeight - 2, Theme.withAlpha(Theme.accent, 0.9F));
	}
}
