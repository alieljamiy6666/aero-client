package net.aeroclient.gui.clickgui;

import java.util.ArrayList;
import java.util.List;

import net.aeroclient.module.Module;
import net.aeroclient.module.setting.Setting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * The panel that slides in from the right when you click a module's gear.
 * It shows the module's name, an on/off switch, its description, and one row
 * per setting, plus "Toggle Key" at the top and "Reset to defaults" at the
 * bottom. Long lists scroll smoothly with the mouse wheel.
 *
 * Settings with a "show only when" rule appear and disappear as you change
 * the setting they depend on.
 */
final class SettingsPanel {

	private static final float HEADER_H = 26.0F;
	private static final float ROW_GAP = 2.0F;

	private final ClickGuiScreen gui;
	private final List<SettingRow> rows = new ArrayList<>();
	private final Animation.Fader slide = new Animation.Fader(200.0F);
	private final Animation.Fader backHover = new Animation.Fader(120.0F);
	private final Animation.Fader switchHover = new Animation.Fader(120.0F);
	private final Animation.Fader moduleOn = new Animation.Fader(150.0F);

	private Module module;
	private boolean open = false;

	// where the gui wants the panel (set every frame) and where it actually is (while sliding)
	private float x, y, w, h, drawX;
	private float rowsTop, rowsBottom;
	private float scroll, scrollTarget, maxScroll;

	private List<String> descLines = new ArrayList<>();
	private float descWidth = -1.0F;

	private SettingRow dragging;
	private KeybindRow listening;
	private DropdownRow openDropdown;

	SettingsPanel(ClickGuiScreen gui) {
		this.gui = gui;
	}

	// ---------------- opening and closing ----------------

	void open(Module module) {
		if (this.module != module) {
			this.module = module;
			this.rows.clear();
			this.rows.add(new KeybindRow(this, "Toggle Key", "The key that turns " + module.getName() + " on and off.",
					null, module::getKeybind, module::setKeybind));
			for (Setting<?> s : module.getSettings()) {
				SettingRow row = SettingRow.create(this, s);
				if (row != null) {
					this.rows.add(row);
				}
			}
			this.rows.add(new ResetRow(this, module));
			this.scroll = this.scrollTarget = 0.0F;
			this.descWidth = -1.0F;
			this.moduleOn.snap(module.isEnabled());
		}
		this.open = true;
		cancelInput();
	}

	void close() {
		this.open = false;
		cancelInput();
	}

	/** Stops any drag, key listening or open dropdown. */
	void cancelInput() {
		if (this.dragging != null) {
			this.dragging.mouseReleased();
			this.dragging = null;
		}
		this.listening = null;
		setOpenDropdown(null);
	}

	boolean isOpen() {
		return this.open;
	}

	/** True while the panel is open or still sliding away. */
	boolean isShowing() {
		return this.module != null && (this.open || this.slide.get() > 0.0F);
	}

	float getProgress() {
		return this.slide.get();
	}

	Module getModule() {
		return this.module;
	}

	/** True when the rows should react to the mouse (open, not animating away, nothing on top). */
	boolean isInteractive() {
		return this.open && this.listening == null && this.openDropdown == null && !this.gui.isClosing();
	}

	// ---------------- state the rows use ----------------

	void setDragging(SettingRow row) {
		this.dragging = row;
	}

	KeybindRow getListening() {
		return this.listening;
	}

	void setListening(KeybindRow row) {
		this.listening = row;
	}

	DropdownRow getOpenDropdown() {
		return this.openDropdown;
	}

	void setOpenDropdown(DropdownRow row) {
		if (this.openDropdown != null && this.openDropdown != row) {
			this.openDropdown.resetAnimation();
		}
		this.openDropdown = row;
	}

	float getTop() {
		return this.rowsTop;
	}

	float getBottom() {
		return this.rowsBottom;
	}

	/** How far to move toward a target this frame, for smooth (time-based) gliding. */
	float smoothing(float speed) {
		return this.gui.smoothing(speed);
	}

	// ---------------- layout and drawing ----------------

	void setBounds(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	boolean contains(float mx, float my) {
		return isShowing() && Widgets.inside(mx, my, this.drawX, this.y, this.w, this.h);
	}

	private float backX() {
		return this.drawX + 14.0F;
	}

	private float switchX() {
		return this.drawX + this.w - Widgets.SWITCH_W - 10.0F;
	}

	private boolean overBack(float mx, float my) {
		return Widgets.inside(mx, my, this.drawX + 4, this.y + 4, 20, 18);
	}

	private boolean overSwitch(float mx, float my) {
		return Widgets.inside(mx, my, switchX() - 4, this.y + 4, Widgets.SWITCH_W + 8, 18);
	}

	void draw(FontRenderer font, float mx, float my, float alpha) {
		float p = this.slide.update(this.open);
		if (this.module == null || p <= 0.0F) {
			if (!this.open) {
				this.module = null;
			}
			return;
		}
		float a = alpha * p;
		this.drawX = this.x + (1.0F - p) * 26.0F;
		float px = this.drawX, py = this.y, pw = this.w, ph = this.h;

		Widgets.panel(px, py, pw, ph, 9.0F, 0xFF2B3646, 0xFF171E29, 0xFF10151C, 12.0F, 0, a);

		// header: back arrow, name, on/off switch
		boolean interactive = isInteractive();
		float bh = this.backHover.update(interactive && overBack(mx, my));
		if (bh > 0.01F) {
			RenderUtil.circle(backX(), py + 13, 8.0F, Widgets.fade(0x1FFFFFFF, bh * a));
		}
		PixelIcons.BACK.drawCentered(backX(), py + 13, 1.0F, Widgets.fade(Theme.mix(Theme.textFaded, 0xFFFFFFFF, bh), a));
		float on = this.moduleOn.update(this.module.isEnabled());
		int nameColor = Theme.mix(0xFFFFFFFF, Theme.accentLight, on);
		Widgets.text(font, TextUtil.trim(font, this.module.getName(), (int) (pw - 64)), px + 26, py + 9, nameColor, a);
		float sh = this.switchHover.update(interactive && overSwitch(mx, my));
		Widgets.drawSwitch(switchX(), py + 8, on, sh, a);
		if (interactive && (overBack(mx, my) || overSwitch(mx, my))) {
			Widgets.handCursor();
		}

		// description, wrapped to the panel's width (worked out again only when the width changes)
		float textW = pw - 24;
		if (textW != this.descWidth) {
			this.descWidth = textW;
			this.descLines = font.listFormattedStringToWidth(this.module.getDescription(), (int) textW);
			if (this.descLines.size() > 3) {
				this.descLines = new ArrayList<>(this.descLines.subList(0, 3));
			}
		}
		float dy = py + HEADER_H;
		for (int i = 0; i < this.descLines.size(); i++) {
			Widgets.text(font, this.descLines.get(i), px + 12, dy + i * 10, Theme.textFaded, a);
		}
		float divY = dy + this.descLines.size() * 10 + 3;
		RenderUtil.rect(px + 10, divY, px + pw - 10, divY + 1, Widgets.fade(0x1AFFFFFF, a));

		// rows (scrolling, clipped to the panel)
		this.rowsTop = divY + 2;
		this.rowsBottom = py + ph - 4;
		float viewH = this.rowsBottom - this.rowsTop;
		float total = contentHeight();
		this.maxScroll = Math.max(0.0F, total - viewH);
		this.scrollTarget = Math.max(0.0F, Math.min(this.maxScroll, this.scrollTarget));
		this.scroll += (this.scrollTarget - this.scroll) * smoothing(16.0F);
		boolean bar = this.maxScroll > 0.5F;
		float rowX = px + 12, rowW = pw - 24 - (bar ? 4 : 0);

		RenderUtil.pushClip(px + 1, this.rowsTop, px + pw - 1, this.rowsBottom);
		float ry = this.rowsTop + 4 - this.scroll;
		boolean mouseInRows = my >= this.rowsTop && my < this.rowsBottom;
		float rmx = mouseInRows ? mx : -10000.0F, rmy = mouseInRows ? my : -10000.0F;
		for (int i = 0; i < this.rows.size(); i++) {
			SettingRow row = this.rows.get(i);
			if (!row.isVisible()) {
				continue;
			}
			row.x = rowX;
			row.y = ry;
			row.w = rowW;
			float rh = row.getHeight();
			if (ry + rh > this.rowsTop - 2 && ry < this.rowsBottom + 2) {
				row.draw(font, rmx, rmy, a);
				if (interactive && row.isOverLine(rmx, rmy)) {
					this.gui.offerTooltip(row, row.getTooltip());
				}
			}
			ry += rh + ROW_GAP;
		}
		RenderUtil.popClip();

		if (bar) {
			float trackH = viewH - 8;
			float handleH = Math.max(14.0F, trackH * viewH / total);
			float handleY = this.rowsTop + 4 + (trackH - handleH) * (this.scroll / this.maxScroll);
			RenderUtil.pill(px + pw - 8, this.rowsTop + 4, 3, trackH, Widgets.fade(0x1AFFFFFF, a));
			RenderUtil.pill(px + pw - 8, handleY, 3, handleH, Widgets.fade(Theme.accent, 0.8F * a));
		}

		if (this.openDropdown != null) {
			this.openDropdown.drawList(font, mx, my, a);
		}
	}

	private float contentHeight() {
		float total = 8.0F;
		for (int i = 0; i < this.rows.size(); i++) {
			SettingRow row = this.rows.get(i);
			if (row.isVisible()) {
				total += row.getHeight() + ROW_GAP;
			}
		}
		return total;
	}

	// ---------------- input ----------------

	/**
	 * Clicks that must be handled before anything else: while a dropdown is
	 * open or a key button is waiting for a key. Returns true if the click was used.
	 */
	boolean overlayClicked(float mx, float my) {
		if (this.openDropdown != null) {
			this.openDropdown.listClicked(mx, my);
			setOpenDropdown(null); // any click closes the list (picking a choice or clicking away)
			return true;
		}
		if (this.listening != null) {
			this.listening = null; // clicking anywhere cancels "press a key"
			return true;
		}
		return false;
	}

	void mouseClicked(float mx, float my, int button) {
		if (!this.open) {
			return;
		}
		if (button == 0 && overBack(mx, my)) {
			close();
			Widgets.click(1.0F);
			return;
		}
		if (button == 0 && overSwitch(mx, my)) {
			this.module.toggle();
			Widgets.click(this.module.isEnabled() ? 1.1F : 0.9F);
			return;
		}
		if (my < this.rowsTop || my >= this.rowsBottom) {
			return;
		}
		for (int i = 0; i < this.rows.size(); i++) {
			SettingRow row = this.rows.get(i);
			if (row.isVisible() && row.mouseClicked(mx, my, button)) {
				return;
			}
		}
	}

	void mouseDragged(float mx, float my) {
		if (this.dragging != null) {
			this.dragging.mouseDragged(mx, my);
		}
	}

	void mouseReleased() {
		if (this.dragging != null) {
			this.dragging.mouseReleased();
			this.dragging = null;
		}
	}

	/** Mouse wheel over the panel. Returns true if the panel used it. */
	boolean mouseWheel(float mx, float my, int direction) {
		if (!this.open || !contains(mx, my)) {
			return false;
		}
		if (this.openDropdown == null) {
			this.scrollTarget = Math.max(0.0F, Math.min(this.maxScroll, this.scrollTarget - direction * 28.0F));
		}
		return true;
	}

	/** Keys go to a key button that's waiting for one. Returns true if used. */
	boolean keyTyped(int key) {
		if (this.listening != null) {
			this.listening.keyPressed(key);
			return true;
		}
		return false;
	}
}
