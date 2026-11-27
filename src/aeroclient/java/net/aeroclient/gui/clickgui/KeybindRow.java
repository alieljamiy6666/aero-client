package net.aeroclient.gui.clickgui;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.aeroclient.module.setting.Setting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.minecraft.client.gui.FontRenderer;

/**
 * A key button: click it, then press the key you want. Backspace or Delete
 * clears it (no key), and Escape cancels. Used for a module's toggle key and
 * for key settings like "Zoom Key".
 */
final class KeybindRow extends SettingRow {

	private static final int KEY_ESCAPE = 1, KEY_BACKSPACE = 14, KEY_DELETE = 211;

	private final IntSupplier getter;
	private final IntConsumer setter;
	private final Animation.Fader boxHover = new Animation.Fader(120.0F);

	private int cachedKey = -1;
	private String cachedName = "";

	KeybindRow(SettingsPanel panel, String label, String description, Setting<?> setting, IntSupplier getter,
			IntConsumer setter) {
		super(panel, label, (description == null || description.isEmpty() ? "" : description + "\n")
				+ "§7Click, then press a key. Backspace clears it.", setting);
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	float getHeight() {
		return LINE_H;
	}

	private boolean isListening() {
		return this.panel.getListening() == this;
	}

	private String keyText() {
		if (isListening()) {
			return "Press a key...";
		}
		int key = this.getter.getAsInt();
		if (key != this.cachedKey) {
			this.cachedKey = key;
			String name = key == 0 ? null : Keyboard.getKeyName(key);
			this.cachedName = name == null || name.isEmpty() ? "None" : name;
		}
		return this.cachedName;
	}

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		String text = keyText();
		float bw = Math.max(40.0F, font.getStringWidth(text) + 12.0F);
		drawBase(font, mx, my, alpha, this.w - bw - 6);
		boolean over = this.panel.isInteractive() && isOverLine(mx, my);
		if (over) {
			Widgets.handCursor();
		}
		float h = this.boxHover.update(over || isListening());
		float bx = this.x + this.w - bw, by = this.y + 3;
		float pulse = isListening() ? 0.6F + 0.4F * (float) Math.sin(Animation.seconds() * 7.0F) : 0.45F * h;
		RenderUtil.roundedRect(bx, by, bw, 14, 5.0F, Widgets.fade(Theme.mix(0xFF2E3A4B, Theme.accent, pulse), alpha));
		RenderUtil.roundedRect(bx + 1, by + 1, bw - 2, 12, 4.0F,
				Widgets.fade(Theme.mix(0xFF0F141B, 0xFF151C26, h), alpha));
		int color = isListening() ? Theme.accentLight : (this.getter.getAsInt() == 0 ? Theme.textFaded : Theme.text);
		Widgets.textCentered(font, text, bx + bw / 2.0F, by + 3, color, alpha);
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (button == 1 && this.setting == null && isOverLine(mx, my)) {
			this.setter.accept(0); // right-click clears a toggle key
			Widgets.click(0.8F);
			return true;
		}
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && isOverLine(mx, my)) {
			this.panel.setListening(isListening() ? null : this);
			Widgets.click(1.0F);
			return true;
		}
		return false;
	}

	/** The next key pressed while listening. */
	void keyPressed(int key) {
		if (key == KEY_BACKSPACE || key == KEY_DELETE) {
			this.setter.accept(0);
		} else if (key != KEY_ESCAPE && key != ClickGuiScreen.MENU_KEY && key > 0) {
			this.setter.accept(key); // Right Shift is kept for the menu itself
		}
		this.panel.setListening(null);
		Widgets.click(1.1F);
	}
}
