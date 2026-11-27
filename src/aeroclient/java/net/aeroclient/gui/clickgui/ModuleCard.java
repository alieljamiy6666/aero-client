package net.aeroclient.gui.clickgui;

import java.util.List;

import net.aeroclient.module.Module;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.minecraft.client.gui.FontRenderer;

/**
 * One module's card in the menu grid: its name, a short description, an
 * on/off switch and a gear button for its settings.
 *
 * Click the card to turn the module on or off. Click the gear (or right-click
 * the card) to open its settings.
 */
final class ModuleCard {

	static final float HEIGHT = 60.0F;

	final Module module;
	float x, y, w; // y is the position before scrolling

	private final Animation.Fader hover = new Animation.Fader(140.0F);
	private final Animation.Fader on = new Animation.Fader(160.0F);
	private final Animation.Fader gearHover = new Animation.Fader(120.0F);

	private List<String> descLines;
	private float descWidth = -1.0F;
	private boolean descCut = false;
	private int cachedKey = -1;
	private String cachedKeyName = null;

	ModuleCard(Module module) {
		this.module = module;
		this.on.snap(module.isEnabled());
	}

	private float gearX(float x) {
		return x + this.w - 15.0F;
	}

	boolean isOver(float mx, float my, float scroll) {
		return Widgets.inside(mx, my, this.x, this.y - scroll, this.w, HEIGHT);
	}

	boolean isOverGear(float mx, float my, float scroll) {
		float gy = this.y - scroll + HEIGHT - 13.0F;
		float dx = mx - gearX(this.x), dy = my - gy;
		return dx * dx + dy * dy <= 9.0F * 9.0F;
	}

	/** True if the description doesn't fit on the card, so the full text should be a tooltip. */
	boolean isDescriptionCut() {
		return this.descCut;
	}

	void draw(FontRenderer font, float scroll, float mx, float my, boolean interactive, float alpha) {
		boolean over = interactive && isOver(mx, my, scroll);
		boolean overGear = over && isOverGear(mx, my, scroll);
		float h = this.hover.update(over);
		float e = this.on.update(this.module.isEnabled());
		float g = this.gearHover.update(overGear);
		if (over) {
			Widgets.handCursor();
		}

		float cx = this.x, cy = this.y - scroll - h * 1.5F, cw = this.w, ch = HEIGHT;

		// card body: a glow and colored edge when on, a lighter edge on hover
		int edge = Theme.mix(Theme.mix(0xFF29323F, 0xFF3D4A5E, h), Theme.accent, 0.8F * e);
		int top = Theme.mix(Theme.mix(0xFF19212C, 0xFF1F2936, h), Theme.accent, 0.07F * e);
		int bottom = Theme.mix(0xFF131921, 0xFF18202B, h);
		Widgets.panel(cx, cy, cw, ch, 8.0F, edge, top, bottom, 5.0F, Widgets.fade(Theme.accent, 0.2F * e), alpha);

		// key badge in the top-right corner, if the module has a key
		float nameRoom = cw - 16;
		int key = this.module.getKeybind();
		if (key != this.cachedKey) {
			this.cachedKey = key;
			String name = key == 0 ? null : Keyboard.getKeyName(key);
			this.cachedKeyName = name == null || name.isEmpty() ? null : name;
		}
		if (this.cachedKeyName != null) {
			float kw = font.getStringWidth(this.cachedKeyName) + 8;
			float kx = cx + cw - kw - 6;
			RenderUtil.roundedRect(kx, cy + 5, kw, 11, 4.0F, Widgets.fade(0x1FFFFFFF, alpha));
			Widgets.text(font, this.cachedKeyName, kx + 4, cy + 7, Theme.textFaded, alpha);
			nameRoom -= kw + 4;
		}

		Widgets.text(font, TextUtil.trim(font, this.module.getName(), (int) nameRoom), cx + 8, cy + 7,
				Theme.mix(0xFFFFFFFF, Theme.accentLight, e), alpha);

		// description: up to two lines, wrapped again only when the card's width changes
		float textW = cw - 16;
		if (textW != this.descWidth) {
			this.descWidth = textW;
			this.descLines = font.listFormattedStringToWidth(this.module.getDescription(), (int) textW);
			this.descCut = this.descLines.size() > 2;
			if (this.descCut) {
				String second = TextUtil.trim(font, this.descLines.get(1) + "...", (int) textW);
				this.descLines = new java.util.ArrayList<>(this.descLines.subList(0, 2));
				this.descLines.set(1, second);
			}
		}
		for (int i = 0; i < this.descLines.size(); i++) {
			Widgets.text(font, this.descLines.get(i), cx + 8, cy + 19 + i * 9, 0xFF8792A2, alpha);
		}

		// bottom row: switch with ON/OFF, and the gear
		float by = cy + ch - 13.0F;
		Widgets.drawSwitch(cx + 8, by - 5, e, h, alpha);
		Widgets.text(font, this.module.isEnabled() ? "ON" : "OFF", cx + 8 + Widgets.SWITCH_W + 5, by - 4,
				this.module.isEnabled() ? Theme.accentLight : 0xFF6B7585, alpha);
		float gx = gearX(cx);
		if (g > 0.01F) {
			RenderUtil.circle(gx, by, 8.0F, Widgets.fade(0x24FFFFFF, g * alpha));
		}
		PixelIcons.GEAR.drawCentered(gx, by, 1.0F, Widgets.fade(Theme.mix(0xFF7D889A, 0xFFFFFFFF, g), alpha));
	}
}
