package net.aeroclient.module.render;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Restyles the outline around the block you're looking at: your own color,
 * a thicker line, and an optional see-through filled box.
 *
 * Browsers can only draw 1-pixel lines, so thick outlines are made of thin
 * boxes along each edge instead. The game calls {@link #draw} where it would
 * draw its normal outline.
 */
public class BlockOutlineModule extends Module {

	private static BlockOutlineModule instance;

	private final ColorSetting color = add(
			new ColorSetting("Outline Color", "The outline's color and see-through amount.", 0xCC000000, true));
	private final NumberSetting thickness = add(
			new NumberSetting("Thickness", "How thick the outline is.", 2, 1, 8, 0.5));
	private final BooleanSetting fill = add(
			new BooleanSetting("Filled Box", "Also shade the whole block.", false));
	private final ColorSetting fillColor = add(
			new ColorSetting("Fill Color", "The filled box's color and see-through amount.", 0x3338BDF8, true));

	public BlockOutlineModule() {
		super("Block Outline", "Custom color, thickness and an optional filled box for the block outline.",
				Category.RENDER, 0);
		instance = this;
		this.fillColor.visibleWhen(this.fill::isOn);
	}

	public static boolean isActive() {
		return instance != null && instance.isEnabled();
	}

	/** Draws the outline (and fill) for a block's box, already moved to be relative to the camera. */
	public static void draw(AxisAlignedBB box) {
		instance.drawBox(box);
	}

	private void drawBox(AxisAlignedBB b) {
		GlStateManager.disableTexture2D();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getBuffer();

		if (this.fill.isOn()) {
			wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
			box(wr, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, this.fillColor.getColor());
			tessellator.draw();
		}

		double h = this.thickness.get() * 0.0025D; // half the line thickness, in blocks
		int c = this.color.getColor();
		double x0 = b.minX, y0 = b.minY, z0 = b.minZ, x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		// 4 edges along X
		box(wr, x0 - h, y0 - h, z0 - h, x1 + h, y0 + h, z0 + h, c);
		box(wr, x0 - h, y1 - h, z0 - h, x1 + h, y1 + h, z0 + h, c);
		box(wr, x0 - h, y0 - h, z1 - h, x1 + h, y0 + h, z1 + h, c);
		box(wr, x0 - h, y1 - h, z1 - h, x1 + h, y1 + h, z1 + h, c);
		// 4 edges along Y
		box(wr, x0 - h, y0 + h, z0 - h, x0 + h, y1 - h, z0 + h, c);
		box(wr, x1 - h, y0 + h, z0 - h, x1 + h, y1 - h, z0 + h, c);
		box(wr, x0 - h, y0 + h, z1 - h, x0 + h, y1 - h, z1 + h, c);
		box(wr, x1 - h, y0 + h, z1 - h, x1 + h, y1 - h, z1 + h, c);
		// 4 edges along Z
		box(wr, x0 - h, y0 - h, z0 + h, x0 + h, y0 + h, z1 - h, c);
		box(wr, x1 - h, y0 - h, z0 + h, x1 + h, y0 + h, z1 - h, c);
		box(wr, x0 - h, y1 - h, z0 + h, x0 + h, y1 + h, z1 - h, c);
		box(wr, x1 - h, y1 - h, z0 + h, x1 + h, y1 + h, z1 - h, c);
		tessellator.draw();

		GlStateManager.enableCull();
	}

	/** Adds the 6 sides of a box. */
	private static void box(WorldRenderer wr, double x0, double y0, double z0, double x1, double y1, double z1,
			int color) {
		float a = ((color >>> 24) & 255) / 255.0F, r = ((color >> 16) & 255) / 255.0F,
				g = ((color >> 8) & 255) / 255.0F, bl = (color & 255) / 255.0F;
		// bottom and top
		wr.pos(x0, y0, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y0, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y0, z1).color(r, g, bl, a).endVertex();
		wr.pos(x0, y0, z1).color(r, g, bl, a).endVertex();
		wr.pos(x0, y1, z0).color(r, g, bl, a).endVertex();
		wr.pos(x0, y1, z1).color(r, g, bl, a).endVertex();
		wr.pos(x1, y1, z1).color(r, g, bl, a).endVertex();
		wr.pos(x1, y1, z0).color(r, g, bl, a).endVertex();
		// north and south
		wr.pos(x0, y0, z0).color(r, g, bl, a).endVertex();
		wr.pos(x0, y1, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y1, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y0, z0).color(r, g, bl, a).endVertex();
		wr.pos(x0, y0, z1).color(r, g, bl, a).endVertex();
		wr.pos(x1, y0, z1).color(r, g, bl, a).endVertex();
		wr.pos(x1, y1, z1).color(r, g, bl, a).endVertex();
		wr.pos(x0, y1, z1).color(r, g, bl, a).endVertex();
		// west and east
		wr.pos(x0, y0, z0).color(r, g, bl, a).endVertex();
		wr.pos(x0, y0, z1).color(r, g, bl, a).endVertex();
		wr.pos(x0, y1, z1).color(r, g, bl, a).endVertex();
		wr.pos(x0, y1, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y0, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y1, z0).color(r, g, bl, a).endVertex();
		wr.pos(x1, y1, z1).color(r, g, bl, a).endVertex();
		wr.pos(x1, y0, z1).color(r, g, bl, a).endVertex();
	}
}
