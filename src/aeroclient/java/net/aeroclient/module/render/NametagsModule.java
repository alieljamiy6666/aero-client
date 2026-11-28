package net.aeroclient.module.render;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.EaglercraftGPU;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Cleaner name tags above players and named mobs: a softer, rounded
 * background (or none), optional text shadow and your own size. It can also
 * show your own name tag in third person.
 *
 * The game calls {@link #draw} instead of drawing its own name tag while this
 * is on, and asks {@link #showOwn} whether to draw yours.
 */
public class NametagsModule extends Module {

	private static NametagsModule instance;
	private static final int SEE_THROUGH_TEXT = 0x20FFFFFF; // the faint text you see through walls

	private final BooleanSetting background = add(
			new BooleanSetting("Background", "Draw a box behind the name.", true));
	private final NumberSetting opacity = add(
			new NumberSetting("Background Opacity", "How dark the box is.", 0.3, 0.05, 1.0, 0.05));
	private final BooleanSetting rounded = add(new BooleanSetting("Rounded", "Round the box's corners.", true));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Draw a shadow under the name.", true));
	private final NumberSetting size = add(new NumberSetting("Size", "How big name tags are.", 1, 0.5, 2, 0.1));
	private final BooleanSetting own = add(
			new BooleanSetting("Show Own", "Show your own name tag in third person.", false));

	public NametagsModule() {
		super("Nametags", "Cleaner name tags, with an option to show your own.", Category.RENDER, 0);
		instance = this;
		this.opacity.visibleWhen(this.background::isOn);
		this.rounded.visibleWhen(this.background::isOn);
	}

	public static boolean isActive() {
		return instance != null && instance.isEnabled();
	}

	/** True when your own name tag should be drawn (third person only). */
	public static boolean showOwn() {
		return isActive() && instance.own.isOn() && Minecraft.getMinecraft().gameSettings.thirdPersonView != 0;
	}

	/** Draws one name tag. Same inputs as the game's own name tag drawing. */
	public static void draw(FontRenderer font, String text, float x, float y, float z, int verticalShift,
			float viewerYaw, float viewerPitch, boolean thirdPersonFrontal, boolean sneaking) {
		NametagsModule m = instance;
		float scale = 0.025F * m.size.getFloat();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		EaglercraftGPU.glNormal3f(0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate((thirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-scale, -scale, scale);
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		if (!sneaking) {
			GlStateManager.disableDepth(); // visible through walls, like vanilla
		}

		int half = font.getStringWidth(text) / 2;
		if (m.background.isOn()) {
			int bg = Math.round(m.opacity.getFloat() * 255.0F) << 24;
			float bx = -half - 2, by = verticalShift - 2, bw = half * 2 + 4, bh = 11;
			if (m.rounded.isOn()) {
				RenderUtil.roundedRect(bx, by, bw, bh, 3.0F, bg);
			} else {
				RenderUtil.rect(bx, by, bx + bw, by + bh, bg);
			}
		}
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableTexture2D();
		if (!sneaking) {
			font.drawString(text, -half, verticalShift, SEE_THROUGH_TEXT, false);
			GlStateManager.enableDepth();
		}
		GlStateManager.depthMask(true);
		font.drawString(text, -half, verticalShift, sneaking ? SEE_THROUGH_TEXT : 0xFFFFFFFF,
				m.shadow.isOn() && !sneaking);
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}
}
