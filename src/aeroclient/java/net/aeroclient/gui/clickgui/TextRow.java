package net.aeroclient.gui.clickgui;

import net.aeroclient.module.setting.StringSetting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

/**
 * A text setting: the label, and under it a box you click and type in (like
 * Auto GG's message). Enter or Escape finishes typing; clicking anywhere else
 * does too. The text is saved as you type.
 */
final class TextRow extends SettingRow {

	private static final float BOX_H = 16.0F;
	private static final int KEY_ESCAPE = 1, KEY_ENTER = 28, KEY_NUMPAD_ENTER = 156;

	private final StringSetting setting;
	private final Animation.Fader focus = new Animation.Fader(150.0F);
	private GuiTextField field;
	private int fieldW = -1;
	private long lastBlink;

	TextRow(SettingsPanel panel, StringSetting setting) {
		super(panel, setting.getName(), setting.getDescription(), setting);
		this.setting = setting;
	}

	@Override
	float getHeight() {
		return LINE_H + BOX_H;
	}

	private boolean isEditing() {
		return this.panel.getEditing() == this;
	}

	private float boxY() {
		return this.y + LINE_H - 2;
	}

	boolean isOverBox(float mx, float my) {
		return Widgets.inside(mx, my, this.x, boxY(), this.w, BOX_H);
	}

	private GuiTextField field(FontRenderer font) {
		int w = (int) this.w - 10;
		if (this.field == null || w != this.fieldW) {
			String text = this.field == null ? this.setting.get() : this.field.getText();
			boolean focused = this.field != null && this.field.isFocused();
			this.fieldW = w;
			this.field = new GuiTextField(0, font, 0, 0, w, 10);
			this.field.setEnableBackgroundDrawing(false);
			this.field.setMaxStringLength(this.setting.getMaxLength());
			this.field.setTextColor(0xFFE2E8F0);
			this.field.setText(text);
			this.field.setFocused(focused);
		}
		return this.field;
	}

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		drawBase(font, mx, my, alpha, this.w);
		GuiTextField f = field(font);
		boolean editing = isEditing();
		if (!editing && !f.getText().equals(this.setting.get())) {
			f.setText(this.setting.get()); // changed elsewhere (reset, profile switch)
		}
		boolean over = this.panel.isInteractive() && isOverBox(mx, my);
		if (over) {
			Widgets.handCursor();
		}
		float h = this.focus.update(editing);
		float bx = this.x, by = boxY();
		RenderUtil.roundedRect(bx, by, this.w, BOX_H, 5.0F,
				Widgets.fade(Theme.mix(0xFF2E3A4B, Theme.accent, Math.max(h, over ? 0.35F : 0.0F)), alpha));
		RenderUtil.roundedRect(bx + 1, by + 1, this.w - 2, BOX_H - 2, 4.0F, Widgets.fade(0xFF0F141B, alpha));
		if (alpha < 0.5F) {
			return; // the text box can't fade, so it appears once the panel is mostly in
		}
		long now = Minecraft.getSystemTime();
		if (now - this.lastBlink > 50L) { // the cursor blinks on the game's tick clock
			this.lastBlink = now;
			f.updateCursorCounter();
		}
		f.xPosition = (int) bx + 5;
		f.yPosition = (int) by + 4;
		if (f.getText().isEmpty() && !editing) {
			Widgets.text(font, "Click to type...", bx + 5, by + 4, Theme.textFaded, alpha);
		} else {
			f.drawTextBox();
		}
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (button == 1 && this.setting != null && (isOverLine(mx, my) || isOverBox(mx, my))) {
			this.setting.reset();
			if (this.field != null) {
				this.field.setText(this.setting.get());
			}
			Widgets.click(0.8F);
			return true;
		}
		if (button == 0 && (isOverLine(mx, my) || isOverBox(mx, my))) {
			if (!isEditing()) {
				this.panel.setEditing(this);
				Widgets.click(1.0F);
			}
			if (this.field != null) {
				this.field.setFocused(true);
				if (isOverBox(mx, my)) {
					this.field.mouseClicked((int) mx, (int) my, 0); // put the cursor where you clicked
				} else {
					this.field.setCursorPositionEnd();
				}
			}
			return true;
		}
		return false;
	}

	/** A key while typing in this box. */
	void typed(char c, int key) {
		if (key == KEY_ESCAPE || key == KEY_ENTER || key == KEY_NUMPAD_ENTER) {
			stopEditing();
			return;
		}
		if (this.field != null && this.field.textboxKeyTyped(c, key)) {
			this.setting.set(this.field.getText());
		}
	}

	/** Finishes typing: saves the (cleaned up) text and shows it. */
	void stopEditing() {
		if (this.field != null) {
			this.field.setFocused(false);
			this.setting.set(this.field.getText());
			this.field.setText(this.setting.get());
		}
		if (isEditing()) {
			this.panel.setEditing(null);
		}
	}
}
