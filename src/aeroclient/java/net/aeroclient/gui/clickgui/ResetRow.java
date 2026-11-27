package net.aeroclient.gui.clickgui;

import net.aeroclient.module.Module;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * "Reset to defaults" at the bottom of a module's settings. It asks first:
 * the first click turns it red ("Click again to reset"), and only a second
 * click within 3 seconds actually resets.
 */
final class ResetRow extends SettingRow {

	private static final long CONFIRM_MS = 3000L;

	private final Module module;
	private long armedAt = -1L;

	ResetRow(SettingsPanel panel, Module module) {
		super(panel, "Reset to defaults", "Puts every setting and the toggle key back to how they started.", null);
		this.module = module;
	}

	@Override
	float getHeight() {
		return 26.0F;
	}

	private boolean isArmed() {
		return this.armedAt >= 0L && Minecraft.getSystemTime() - this.armedAt < CONFIRM_MS;
	}

	@Override
	boolean isOverLine(float mx, float my) {
		return Widgets.inside(mx, my, this.x, this.y + 6, this.w, 18);
	}

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		boolean armed = isArmed();
		float h = this.hover.update(this.panel.isInteractive() && isOverLine(mx, my));
		if (h > 0.0F && this.panel.isInteractive() && isOverLine(mx, my)) {
			Widgets.handCursor();
		}
		int edge = armed ? Theme.danger : Theme.mix(0xFF2E3A4B, Theme.danger, 0.6F * h);
		RenderUtil.roundedRect(this.x, this.y + 6, this.w, 18, 9.0F, Widgets.fade(edge, alpha));
		RenderUtil.roundedRect(this.x + 1, this.y + 7, this.w - 2, 16, 8.0F,
				Widgets.fade(armed ? 0xFF3A1518 : Theme.mix(0xFF121820, 0xFF1D1417, h), alpha));
		String text = armed ? "Click again to reset" : this.label;
		Widgets.textCentered(font, text, this.x + this.w / 2.0F, this.y + 11,
				armed ? 0xFFFECACA : Theme.mix(Theme.textFaded, 0xFFFECACA, h), alpha);
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (button != 0 || !isOverLine(mx, my)) {
			return false;
		}
		if (isArmed()) {
			this.module.resetSettings();
			this.armedAt = -1L;
			Widgets.click(0.8F);
		} else {
			this.armedAt = Minecraft.getSystemTime();
			Widgets.click(1.0F);
		}
		return true;
	}
}
