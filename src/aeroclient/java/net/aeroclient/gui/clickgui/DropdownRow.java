package net.aeroclient.gui.clickgui;

import java.util.List;

import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * A "pick one" setting shown as a dropdown box. Clicking the box opens a
 * list of choices on top of everything; clicking a choice picks it.
 */
final class DropdownRow extends SettingRow {

	private static final float BOX_H = 14.0F;
	private static final float ITEM_H = 13.0F;

	private final ModeSetting setting;
	private final Animation.Fader boxHover = new Animation.Fader(120.0F);
	private final Animation.Fader opening = new Animation.Fader(130.0F);

	DropdownRow(SettingsPanel panel, ModeSetting setting) {
		super(panel, setting.getName(), setting.getDescription(), setting);
		this.setting = setting;
	}

	@Override
	float getHeight() {
		return LINE_H;
	}

	private float boxW() {
		return Math.min(100.0F, Math.max(60.0F, this.w * 0.52F));
	}

	private float boxX() {
		return this.x + this.w - boxW();
	}

	private float boxY() {
		return this.y + (LINE_H - BOX_H) / 2.0F;
	}

	private boolean isOverBox(float mx, float my) {
		return Widgets.inside(mx, my, boxX(), boxY(), boxW(), BOX_H);
	}

	private boolean isOpen() {
		return this.panel.getOpenDropdown() == this;
	}

	@Override
	void draw(FontRenderer font, float mx, float my, float alpha) {
		drawBase(font, mx, my, alpha, this.w - boxW() - 6);
		boolean over = this.panel.isInteractive() && isOverLine(mx, my);
		if (over) {
			Widgets.handCursor();
		}
		float h = this.boxHover.update(over || isOpen());
		float bx = boxX(), by = boxY(), bw = boxW();
		int border = Theme.mix(0xFF2E3A4B, Theme.accent, isOpen() ? 1.0F : 0.45F * h);
		RenderUtil.roundedRect(bx, by, bw, BOX_H, 5.0F, Widgets.fade(border, alpha));
		RenderUtil.roundedRect(bx + 1, by + 1, bw - 2, BOX_H - 2, 4.0F,
				Widgets.fade(Theme.mix(0xFF0F141B, 0xFF151C26, h), alpha));
		Widgets.text(font, TextUtil.trim(font, this.setting.get(), (int) bw - 18), bx + 5, by + 3, Theme.text, alpha);
		PixelIcons.DOWN.draw(bx + bw - 11, by + 5, 1.0F, Widgets.fade(Theme.mix(Theme.textFaded, Theme.text, h), alpha));
	}

	// ---------------- the open list ----------------

	private float listH() {
		return this.setting.getModes().size() * ITEM_H + 4.0F;
	}

	/** The list opens below the box, or above it if there's no room below. */
	private float listY() {
		float below = boxY() + BOX_H + 2.0F;
		if (below + listH() > this.panel.getBottom() && boxY() - listH() - 2.0F > this.panel.getTop()) {
			return boxY() - listH() - 2.0F;
		}
		return below;
	}

	/** Draws the open list on top of the whole panel. */
	void drawList(FontRenderer font, float mx, float my, float alpha) {
		float o = this.opening.update(true);
		float a = alpha * o;
		float lx = boxX(), lw = boxW(), ly = listY() + (1.0F - o) * -3.0F, lh = listH();
		RenderUtil.shadow(lx, ly + 1, lw, lh, 5.0F, 6.0F, Widgets.fade(0x99000000, a));
		RenderUtil.roundedRect(lx, ly, lw, lh, 5.0F, Widgets.fade(Theme.mix(0xFF2E3A4B, Theme.accent, 0.4F), a));
		RenderUtil.roundedRect(lx + 1, ly + 1, lw - 2, lh - 2, 4.0F, Widgets.fade(0xFC10151D, a));
		List<String> modes = this.setting.getModes();
		for (int i = 0; i < modes.size(); i++) {
			float iy = ly + 2 + i * ITEM_H;
			boolean selected = this.setting.is(modes.get(i));
			boolean over = Widgets.inside(mx, my, lx, iy, lw, ITEM_H);
			if (over) {
				RenderUtil.roundedRect(lx + 2, iy, lw - 4, ITEM_H, 4.0F, Widgets.fade(0x1FFFFFFF, a));
				Widgets.handCursor();
			}
			int color = selected ? Theme.accentLight : (over ? 0xFFFFFFFF : Theme.text);
			Widgets.text(font, TextUtil.trim(font, modes.get(i), (int) lw - 20), lx + 6, iy + 3, color, a);
			if (selected) {
				PixelIcons.CHECK.draw(lx + lw - 12, iy + 4, 1.0F, Widgets.fade(Theme.accentLight, a));
			}
		}
	}

	/** A click while the list is open. Returns true if it landed on the list. */
	boolean listClicked(float mx, float my) {
		float lx = boxX(), lw = boxW(), ly = listY();
		List<String> modes = this.setting.getModes();
		for (int i = 0; i < modes.size(); i++) {
			if (Widgets.inside(mx, my, lx, ly + 2 + i * ITEM_H, lw, ITEM_H)) {
				this.setting.set(modes.get(i));
				Widgets.click(1.0F);
				return true;
			}
		}
		return Widgets.inside(mx, my, lx, ly, lw, listH());
	}

	/** Called when the list closes, so it animates open again next time. */
	void resetAnimation() {
		this.opening.snap(false);
	}

	@Override
	boolean mouseClicked(float mx, float my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && isOverLine(mx, my)) {
			this.panel.setOpenDropdown(isOpen() ? null : this);
			Widgets.click(1.0F);
			return true;
		}
		return false;
	}
}
