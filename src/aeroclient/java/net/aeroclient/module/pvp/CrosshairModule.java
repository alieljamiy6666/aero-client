package net.aeroclient.module.pvp;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.util.RenderUtil;
import net.minecraft.client.Minecraft;

/**
 * Replaces the crosshair with one you design: cross, T, dot, circle or
 * square, with your own size, gap, thickness, color and an outline so it's
 * easy to see on any background. Purely visual: it doesn't change where you
 * aim or what you hit.
 *
 * The game calls {@link #drawCustom} where it would draw its own crosshair.
 */
public class CrosshairModule extends Module {

	private static CrosshairModule instance;

	private final ModeSetting shape = add(new ModeSetting("Shape", "The crosshair's shape.", "Cross", "Cross", "T-Shape",
			"Dot", "Circle", "Square"));
	private final NumberSetting size = add(new NumberSetting("Size", "How long the lines are (or how big the shape is).",
			5, 1, 15, 0.5));
	private final NumberSetting gap = add(new NumberSetting("Gap", "Empty space in the middle.", 2, 0, 8, 0.5));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "How thick the lines are.", 1, 0.5, 4,
			0.5));
	private final ColorSetting color = add(new ColorSetting("Color", "The crosshair's color.", 0xFFFFFFFF, true));
	private final BooleanSetting centerDot = add(new BooleanSetting("Center Dot", "Add a small dot in the middle.", false));
	private final BooleanSetting outline = add(
			new BooleanSetting("Outline", "A dark edge so it's easy to see on bright and dark backgrounds.", true));
	private final ColorSetting outlineColor = add(
			new ColorSetting("Outline Color", "The edge's color.", 0xCC000000, true));
	private final BooleanSetting thirdPerson = add(
			new BooleanSetting("Show in Third Person", "Also show it when the camera is behind or in front of you.",
					false));

	public CrosshairModule() {
		super("Custom Crosshair", "Your own crosshair: shape, size, gap, thickness, color and outline.", Category.PVP, 0);
		instance = this;
		this.outlineColor.visibleWhen(this.outline::isOn);
		this.gap.visibleWhen(() -> this.shape.is("Cross") || this.shape.is("T-Shape"));
	}

	/** True if the custom crosshair should also be drawn in third person. */
	public static boolean showsInThirdPerson() {
		return instance != null && instance.isEnabled() && instance.thirdPerson.isOn();
	}

	/**
	 * Draws the custom crosshair in the middle of a screen this size, if the
	 * module is on. Returns false (drawing nothing) when it's off, so the game
	 * draws its normal one.
	 */
	public static boolean drawCustom(int screenW, int screenH) {
		if (instance == null || !instance.isEnabled()) {
			return false;
		}
		instance.draw(screenW, screenH);
		return true;
	}

	private void draw(int screenW, int screenH) {
		Minecraft mc = this.mc;
		float px = screenW / (float) Math.max(1, mc.displayWidth); // one screen pixel in GUI units
		// the exact middle, snapped to the pixel grid so thin lines stay sharp
		float cx = Math.round(mc.displayWidth / 2.0F) * px, cy = Math.round(mc.displayHeight / 2.0F) * px;
		float t = Math.max(px, Math.round(this.thickness.getFloat() / px) * px);
		if (Math.round(t / px) % 2 == 1) { // odd pixel widths: center on a pixel, not between two
			cx += px / 2.0F;
			cy += px / 2.0F;
		}
		float o = this.outline.isOn() ? Math.max(px, Math.round(0.5F / px) * px) : 0.0F;
		if (o > 0.0F) {
			drawShape(cx, cy, t, o, this.outlineColor.getColor());
		}
		drawShape(cx, cy, t, 0.0F, this.color.getColor());
	}

	/** The shape, grown by {@code grow} on every side (used to draw the outline behind it). */
	private void drawShape(float cx, float cy, float t, float grow, int c) {
		float s = this.size.getFloat(), g = this.gap.getFloat(), h = t / 2.0F;
		String shape = this.shape.get();
		if (shape.equals("Cross") || shape.equals("T-Shape")) {
			RenderUtil.beginBatch(false);
			if (!shape.equals("T-Shape")) {
				RenderUtil.batchRect(cx - h - grow, cy - g - s - grow, cx + h + grow, cy - g + grow, c); // up
			}
			RenderUtil.batchRect(cx - h - grow, cy + g - grow, cx + h + grow, cy + g + s + grow, c); // down
			RenderUtil.batchRect(cx - g - s - grow, cy - h - grow, cx - g + grow, cy + h + grow, c); // left
			RenderUtil.batchRect(cx + g - grow, cy - h - grow, cx + g + s + grow, cy + h + grow, c); // right
			RenderUtil.endBatch();
		} else if (shape.equals("Dot")) {
			float r = Math.max(1.0F, s * 0.35F) + grow;
			RenderUtil.circle(cx, cy, r, c);
		} else if (shape.equals("Circle")) {
			float r = s + grow;
			RenderUtil.roundedOutline(cx - r, cy - r, r * 2.0F, r * 2.0F, r, t + grow * 2.0F, c);
		} else { // Square
			float r = s + grow;
			float w = t + grow * 2.0F;
			RenderUtil.beginBatch(false);
			RenderUtil.batchRect(cx - r, cy - r, cx + r, cy - r + w, c);
			RenderUtil.batchRect(cx - r, cy + r - w, cx + r, cy + r, c);
			RenderUtil.batchRect(cx - r, cy - r + w, cx - r + w, cy + r - w, c);
			RenderUtil.batchRect(cx + r - w, cy - r + w, cx + r, cy + r - w, c);
			RenderUtil.endBatch();
		}
		if (this.centerDot.isOn() && !shape.equals("Dot")) {
			float d = h + grow;
			RenderUtil.rect(cx - d, cy - d, cx + d, cy + d, c);
		}
	}
}
