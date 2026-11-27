package net.aeroclient.gui.clickgui;

import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.KeybindSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.module.setting.Setting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * One line in a module's settings panel: a label on the left and a control
 * on the right (a switch, slider, dropdown, color or key button).
 *
 * The panel sets {@link #x}, {@link #y} and {@link #w} every frame before
 * drawing, so rows simply draw themselves where they're told.
 */
abstract class SettingRow {

	static final float LINE_H = 20.0F;

	protected final SettingsPanel panel;
	protected final String label;
	protected final String tooltip;
	protected final Setting<?> setting; // null for rows that aren't a setting (like "Toggle Key")
	protected final Animation.Fader hover = new Animation.Fader(120.0F);

	float x, y, w;

	SettingRow(SettingsPanel panel, String label, String description, Setting<?> setting) {
		this.panel = panel;
		this.label = label;
		this.setting = setting;
		String tip = description == null ? "" : description;
		if (setting != null) {
			tip = tip + (tip.isEmpty() ? "" : "\n") + "§7Right-click to reset";
		}
		this.tooltip = tip;
	}

	/** Makes the right kind of row for a setting. */
	static SettingRow create(SettingsPanel panel, Setting<?> setting) {
		if (setting instanceof BooleanSetting) {
			return new SwitchRow(panel, (BooleanSetting) setting);
		} else if (setting instanceof NumberSetting) {
			return new SliderRow(panel, (NumberSetting) setting);
		} else if (setting instanceof ModeSetting) {
			return new DropdownRow(panel, (ModeSetting) setting);
		} else if (setting instanceof ColorSetting) {
			return new ColorRow(panel, (ColorSetting) setting);
		} else if (setting instanceof KeybindSetting) {
			KeybindSetting key = (KeybindSetting) setting;
			return new KeybindRow(panel, key.getName(), key.getDescription(), key, key::getKey, key::set);
		}
		return null; // a kind of setting the menu doesn't know yet
	}

	boolean isVisible() {
		return this.setting == null || this.setting.isVisible();
	}

	abstract float getHeight();

	abstract void draw(FontRenderer font, float mx, float my, float alpha);

	/** Returns true if the click was used. */
	boolean mouseClicked(float mx, float my, int button) {
		if (button == 1 && this.setting != null && isOverLine(mx, my)) {
			this.setting.reset();
			Widgets.click(0.8F);
			return true;
		}
		return false;
	}

	void mouseDragged(float mx, float my) {
	}

	void mouseReleased() {
	}

	/** The label line of this row (the top 20 units), used for hovering and tooltips. */
	boolean isOverLine(float mx, float my) {
		return Widgets.inside(mx, my, this.x - 4, this.y, this.w + 8, LINE_H);
	}

	String getTooltip() {
		return this.tooltip;
	}

	/** The soft highlight behind a hovered row, plus its label. */
	protected void drawBase(FontRenderer font, float mx, float my, float alpha, float labelRoom) {
		float h = this.hover.update(this.panel.isInteractive() && isOverLine(mx, my));
		if (h > 0.01F) {
			RenderUtil.roundedRect(this.x - 4, this.y, this.w + 8, LINE_H, 5.0F, Widgets.fade(0x12FFFFFF, h * alpha));
		}
		Widgets.text(font, TextUtil.trim(font, this.label, (int) labelRoom), this.x, this.y + 6,
				Theme.mix(0xFFCBD5E1, 0xFFFFFFFF, h), alpha);
	}
}
