package net.aeroclient.gui.clickgui;

import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * A number setting with a slider. Click or drag anywhere on the bar; the
 * value snaps to the setting's steps and is shown on the right.
 */
final class SliderRow extends SettingRow {

	private static final float HEIGHT = 30.0F;
	private static final float TRACK_Y = 21.0F; // where the bar sits inside the row

	private final NumberSetting setting;
	private final Animation.Fader knob = new Animation.Fader(120.0F);
	private boolean dragging = false;
	private float shownFraction = -1.0F; // glides toward the real value so clicks animate

	// The value text only changes when the value does, so it's not rebuilt every frame.
	private double cachedValue = Double.NaN;
	private String cachedText = "";

	SliderRow(SettingsPanel panel, NumberSetting setting) {
		super(panel, setting.getName(), setting.getDescription(), setting);
		this.setting = setting;
	}

	@Override
	float getHeight() {
		return HEIGHT;
	}

	private boolean isOverTrack(float mx, float my) {
		return Widgets.inside(mx, my, this.x - 3, this.y + TRACK_Y - 6, this.w + 6, 13);
	}

	@Override
	boolean isOverLine(float mx, float my) {
		return Widgets.inside(mx, my, this.x - 4, this.y, this.w + 8, HEIGHT);
	}

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		double value = this.setting.get();
		if (value != this.cachedValue) {
			this.cachedValue = value;
			this.cachedText = this.setting.format();
		}
		float valueW = font.getStringWidth(this.cachedText);

		// background highlight covers the whole row (label + bar)
		float h = this.hover.update(this.dragging || (this.panel.isInteractive() && isOverLine(mx, my)));
		if (h > 0.01F) {
			RenderUtil.roundedRect(this.x - 4, this.y, this.w + 8, HEIGHT, 5.0F, Widgets.fade(0x12FFFFFF, h * alpha));
		}
		Widgets.text(font, net.aeroclient.util.TextUtil.trim(font, this.label, (int) (this.w - valueW - 8)), this.x,
				this.y + 5, Theme.mix(0xFFCBD5E1, 0xFFFFFFFF, h), alpha);
		Widgets.textRight(font, this.cachedText, this.x + this.w, this.y + 5, Theme.accentLight, alpha);

		float target = (float) this.setting.getFraction();
		if (this.shownFraction < 0.0F || this.dragging) {
			this.shownFraction = target;
		} else {
			this.shownFraction += (target - this.shownFraction) * this.panel.smoothing(22.0F);
		}

		float trackY = this.y + TRACK_Y;
		float fillW = this.w * this.shownFraction;
		RenderUtil.pill(this.x, trackY - 2, this.w, 4, Widgets.fade(0xFF2A3442, alpha));
		if (fillW > 0.5F) {
			RenderUtil.pill(this.x, trackY - 2, Math.max(4, fillW), 4, Widgets.fade(Theme.accent, alpha));
		}

		boolean overTrack = this.panel.isInteractive() && isOverTrack(mx, my);
		if (overTrack || this.dragging) {
			Widgets.handCursor();
		}
		float k = this.knob.update(overTrack || this.dragging);
		float kx = this.x + fillW;
		if (k > 0.01F) {
			RenderUtil.glow(kx - 4, trackY - 4, 8, 8, 4, 4, Widgets.fade(Theme.accent, 0.45F * k * alpha));
		}
		RenderUtil.circle(kx, trackY, 4.0F + 0.8F * k, Widgets.fade(0xFFF8FAFC, alpha));
		RenderUtil.circle(kx, trackY, 1.8F, Widgets.fade(Theme.accent, alpha * k));
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && isOverTrack(mx, my)) {
			this.dragging = true;
			this.panel.setDragging(this);
			mouseDragged(mx, my);
			return true;
		}
		return false;
	}

	@Override
	void mouseDragged(float mx, float my) {
		if (this.dragging && this.w > 0.0F) {
			this.setting.setFraction((mx - this.x) / this.w);
		}
	}

	@Override
	void mouseReleased() {
		this.dragging = false;
	}
}
