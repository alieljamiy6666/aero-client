package net.aeroclient.gui.clickgui;

import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.theme.Animation;
import net.minecraft.client.gui.FontRenderer;

/** An on/off setting: click anywhere on the row to flip the switch. */
final class SwitchRow extends SettingRow {

	private final BooleanSetting setting;
	private final Animation.Fader on = new Animation.Fader(150.0F);

	SwitchRow(SettingsPanel panel, BooleanSetting setting) {
		super(panel, setting.getName(), setting.getDescription(), setting);
		this.setting = setting;
	}

	@Override
	float getHeight() {
		return LINE_H;
	}

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		drawBase(font, mx, my, alpha, this.w - Widgets.SWITCH_W - 8);
		float hovered = this.panel.isInteractive() && isOverLine(mx, my) ? 1.0F : 0.0F;
		if (hovered > 0.0F) {
			Widgets.handCursor();
		}
		Widgets.drawSwitch(this.x + this.w - Widgets.SWITCH_W, this.y + 5, this.on.update(this.setting.isOn()), hovered,
				alpha);
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && isOverLine(mx, my)) {
			this.setting.toggle();
			Widgets.click(this.setting.isOn() ? 1.1F : 0.9F);
			return true;
		}
		return false;
	}
}
