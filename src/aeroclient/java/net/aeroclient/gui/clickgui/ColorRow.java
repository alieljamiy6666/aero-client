package net.aeroclient.gui.clickgui;

import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * A color setting. The row shows a small swatch; clicking it opens a color
 * picker underneath:
 * <ul>
 * <li>a big square for how strong (left/right) and how bright (up/down) the color is,</li>
 * <li>a rainbow bar to pick the hue,</li>
 * <li>a see-through bar (only for colors that allow it),</li>
 * <li>and quick swatches with the Aero accent colors, white and black.</li>
 * </ul>
 * Colors are picked as HSV (hue, saturation, value), which is how people
 * think about color, then turned into normal RGB for the setting.
 */
final class ColorRow extends SettingRow {

	private static final float SV_H = 40.0F;
	private static final float BAR_H = 6.0F;
	private static final int[] HUE_STOPS = { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF,
			0xFFFF0000 };
	private static final int[] QUICK_COLORS = { 0xFF38BDF8, 0xFFA78BFA, 0xFF34D399, 0xFFF472B6, 0xFFFB923C, 0xFFF87171,
			0xFFFFFFFF, 0xFF111111 };
	private static final int DRAG_NONE = 0, DRAG_SV = 1, DRAG_HUE = 2, DRAG_ALPHA = 3;

	private final ColorSetting setting;
	private final Animation.Fader expand = new Animation.Fader(180.0F);
	private boolean expanded = false;
	private int dragging = DRAG_NONE;

	// the picked color as hue / saturation / value / alpha, all 0 to 1
	private float hue, sat, val, alpha;
	private int syncedColor;
	private String hexText = "";

	ColorRow(SettingsPanel panel, ColorSetting setting) {
		super(panel, setting.getName(), setting.getDescription(), setting);
		this.setting = setting;
		syncFromSetting();
	}

	private float pickerHeight() {
		return this.setting.allowsAlpha() ? 82.0F : 72.0F;
	}

	@Override
	float getHeight() {
		return LINE_H + pickerHeight() * this.expand.get();
	}

	// ---------------- layout of the picker ----------------

	private float pickerTop() {
		return this.y + LINE_H + 2.0F;
	}

	private float hueY() {
		return pickerTop() + SV_H + 5.0F;
	}

	private float alphaY() {
		return hueY() + BAR_H + 4.0F;
	}

	private float infoY() {
		return (this.setting.allowsAlpha() ? alphaY() : hueY()) + BAR_H + 4.0F;
	}

	private int quickCount(FontRenderer font) {
		int room = (int) (this.w - font.getStringWidth(this.hexText) - 8);
		return Math.max(0, Math.min(QUICK_COLORS.length, room / 11));
	}

	private float quickX(int i, int count) {
		return this.x + this.w - 4 - (count - 1 - i) * 11.0F;
	}

	// ---------------- drawing ----------------

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		if (this.dragging == DRAG_NONE && this.setting.getColor() != this.syncedColor) {
			syncFromSetting(); // changed somewhere else, e.g. reset or a profile switch
		}
		float e = this.expand.update(this.expanded);
		drawBase(font, mx, my, alpha, this.w - 34);
		if (this.panel.isInteractive() && isOverLine(mx, my)) {
			Widgets.handCursor();
		}

		// the swatch on the right of the row
		float sx = this.x + this.w - 24, sy = this.y + 4;
		RenderUtil.roundedRect(sx, sy, 24, 12, 4.0F, Widgets.fade(this.expanded ? Theme.accent : 0xFF3A4658, alpha));
		if (this.setting.allowsAlpha()) {
			drawChecker(sx + 1, sy + 1, 22, 10, alpha);
		}
		RenderUtil.roundedRect(sx + 1, sy + 1, 22, 10, 3.0F, Widgets.fade(this.setting.getColor(), alpha));

		if (e <= 0.01F) {
			return;
		}
		// only show the part of the picker that fits while it slides open
		RenderUtil.pushClip(this.x - 4, this.y + LINE_H, this.x + this.w + 4, this.y + getHeight());
		float a = alpha * e;
		float top = pickerTop();

		// saturation (left to right) and brightness (top to bottom)
		int pure = hsvToRgb(this.hue, 1.0F, 1.0F);
		RenderUtil.roundedRect(this.x - 1, top - 1, this.w + 2, SV_H + 2, 3.0F, Widgets.fade(0xFF3A4658, a));
		RenderUtil.gradient(this.x, top, this.x + this.w, top + SV_H, Widgets.fade(0xFFFFFFFF, a), Widgets.fade(pure, a),
				Widgets.fade(pure, a), Widgets.fade(0xFFFFFFFF, a), false);
		RenderUtil.gradient(this.x, top, this.x + this.w, top + SV_H, 0, 0, Widgets.fade(0xFF000000, a),
				Widgets.fade(0xFF000000, a), false);
		float px = this.x + this.sat * this.w, py = top + (1.0F - this.val) * SV_H;
		RenderUtil.circle(px, py, 3.6F, Widgets.fade(0xFFFFFFFF, a));
		RenderUtil.circle(px, py, 2.4F, Widgets.fade(hsvToRgb(this.hue, this.sat, this.val), a));

		// hue rainbow
		float hy = hueY();
		float seg = this.w / (HUE_STOPS.length - 1);
		for (int i = 0; i < HUE_STOPS.length - 1; i++) {
			RenderUtil.gradientH(this.x + i * seg, hy, this.x + (i + 1) * seg, hy + BAR_H, Widgets.fade(HUE_STOPS[i], a),
					Widgets.fade(HUE_STOPS[i + 1], a));
		}
		drawMarker(this.x + this.hue * this.w, hy, a);

		// see-through-ness
		if (this.setting.allowsAlpha()) {
			float ay = alphaY();
			drawChecker(this.x, ay, this.w, BAR_H, a);
			int solid = hsvToRgb(this.hue, this.sat, this.val);
			RenderUtil.gradientH(this.x, ay, this.x + this.w, ay + BAR_H, Widgets.fade(solid & 0x00FFFFFF, a),
					Widgets.fade(solid, a));
			drawMarker(this.x + this.alpha * this.w, ay, a);
		}

		// hex code and quick colors
		float iy = infoY();
		Widgets.text(font, this.hexText, this.x, iy + 1, Theme.textFaded, a);
		int count = quickCount(font);
		for (int i = 0; i < count; i++) {
			float cx = quickX(i, count), cy = iy + 5;
			boolean over = this.panel.isInteractive() && Widgets.inside(mx, my, cx - 5, cy - 5, 10, 10);
			if (over) {
				Widgets.handCursor();
			}
			RenderUtil.circle(cx, cy, over ? 5.0F : 4.5F, Widgets.fade(over ? 0xFFFFFFFF : 0xFF3A4658, a));
			RenderUtil.circle(cx, cy, 3.6F, Widgets.fade(QUICK_COLORS[i], a));
		}
		RenderUtil.popClip();
	}

	private static void drawMarker(float x, float y, float alpha) {
		RenderUtil.pill(x - 2.5F, y - 3, 5, BAR_H + 6, Widgets.fade(0x99000000, alpha));
		RenderUtil.pill(x - 1.5F, y - 2, 3, BAR_H + 4, Widgets.fade(0xFFFFFFFF, alpha));
	}

	/** The gray checkerboard behind see-through colors. */
	private static void drawChecker(float x, float y, float w, float h, float alpha) {
		RenderUtil.beginBatch(false);
		RenderUtil.batchRect(x, y, x + w, y + h, Widgets.fade(0xFF9CA3AF, alpha));
		float size = 3.0F;
		int row = 0;
		for (float cy = y; cy < y + h; cy += size, row++) {
			int col = 0;
			for (float cx = x; cx < x + w; cx += size, col++) {
				if (((row + col) & 1) == 0) {
					RenderUtil.batchRect(cx, cy, Math.min(cx + size, x + w), Math.min(cy + size, y + h),
							Widgets.fade(0xFF6B7280, alpha));
				}
			}
		}
		RenderUtil.endBatch();
	}

	// ---------------- mouse ----------------

	private boolean pickerReady() {
		return this.expanded && this.expand.get() > 0.9F;
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button != 0) {
			return false;
		}
		if (isOverLine(mx, my)) {
			this.expanded = !this.expanded;
			Widgets.click(this.expanded ? 1.1F : 0.9F);
			return true;
		}
		if (!pickerReady()) {
			return false;
		}
		float top = pickerTop();
		if (Widgets.inside(mx, my, this.x - 2, top - 2, this.w + 4, SV_H + 4)) {
			startDrag(DRAG_SV, mx, my);
			return true;
		}
		if (Widgets.inside(mx, my, this.x - 3, hueY() - 2, this.w + 6, BAR_H + 4)) {
			startDrag(DRAG_HUE, mx, my);
			return true;
		}
		if (this.setting.allowsAlpha() && Widgets.inside(mx, my, this.x - 3, alphaY() - 2, this.w + 6, BAR_H + 4)) {
			startDrag(DRAG_ALPHA, mx, my);
			return true;
		}
		FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRendererObj;
		int count = quickCount(font);
		for (int i = 0; i < count; i++) {
			if (Widgets.inside(mx, my, quickX(i, count) - 5, infoY(), 10, 10)) {
				int keepAlpha = this.setting.allowsAlpha() ? (this.setting.getAlpha() << 24) : 0xFF000000;
				this.setting.set((QUICK_COLORS[i] & 0x00FFFFFF) | keepAlpha);
				Widgets.click(1.0F);
				return true;
			}
		}
		// a click inside the open picker but not on anything still counts as handled
		return Widgets.inside(mx, my, this.x - 4, this.y, this.w + 8, getHeight());
	}

	private void startDrag(int what, float mx, float my) {
		this.dragging = what;
		this.panel.setDragging(this);
		mouseDragged(mx, my);
	}

	@Override
	void mouseDragged(float mx, float my) {
		if (this.dragging == DRAG_NONE || this.w <= 0.0F) {
			return;
		}
		float fx = Animation.clamp01((mx - this.x) / this.w);
		if (this.dragging == DRAG_SV) {
			this.sat = fx;
			this.val = 1.0F - Animation.clamp01((my - pickerTop()) / SV_H);
		} else if (this.dragging == DRAG_HUE) {
			this.hue = Math.min(fx, 0.999F);
		} else if (this.dragging == DRAG_ALPHA) {
			this.alpha = fx;
		}
		int a = this.setting.allowsAlpha() ? Math.round(this.alpha * 255.0F) : 255;
		int color = (a << 24) | (hsvToRgb(this.hue, this.sat, this.val) & 0x00FFFFFF);
		this.setting.set(color);
		this.syncedColor = this.setting.getColor();
		updateHexText();
	}

	@Override
	void mouseReleased() {
		this.dragging = DRAG_NONE;
	}

	// ---------------- color math ----------------

	private void syncFromSetting() {
		int c = this.setting.getColor();
		float r = ((c >> 16) & 255) / 255.0F, g = ((c >> 8) & 255) / 255.0F, b = (c & 255) / 255.0F;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
		if (d > 0.0F) { // gray colors have no hue, so keep the old one
			float h;
			if (max == r) {
				h = (g - b) / d;
				if (h < 0.0F) {
					h += 6.0F;
				}
			} else if (max == g) {
				h = (b - r) / d + 2.0F;
			} else {
				h = (r - g) / d + 4.0F;
			}
			this.hue = h / 6.0F;
		}
		if (max > 0.0F) {
			this.sat = d / max;
		}
		this.val = max;
		this.alpha = ((c >>> 24) & 255) / 255.0F;
		this.syncedColor = c;
		updateHexText();
	}

	static int hsvToRgb(float h, float s, float v) {
		float hh = (h - (float) Math.floor(h)) * 6.0F;
		int i = (int) hh;
		float f = hh - i;
		float p = v * (1.0F - s), q = v * (1.0F - s * f), t = v * (1.0F - s * (1.0F - f));
		float r, g, b;
		switch (i) {
		case 0: r = v; g = t; b = p; break;
		case 1: r = q; g = v; b = p; break;
		case 2: r = p; g = v; b = t; break;
		case 3: r = p; g = q; b = v; break;
		case 4: r = t; g = p; b = v; break;
		default: r = v; g = p; b = q; break;
		}
		return 0xFF000000 | (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
	}

	private void updateHexText() {
		int c = this.setting.getColor();
		StringBuilder sb = new StringBuilder(12).append('#');
		String hex = Integer.toHexString(c & 0x00FFFFFF).toUpperCase();
		for (int i = hex.length(); i < 6; i++) {
			sb.append('0');
		}
		sb.append(hex);
		if (this.setting.allowsAlpha()) {
			sb.append("  ").append(Math.round(this.setting.getAlpha() * 100.0F / 255.0F)).append('%');
		}
		this.hexText = sb.toString();
	}
}
