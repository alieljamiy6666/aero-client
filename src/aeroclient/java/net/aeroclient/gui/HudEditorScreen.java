package net.aeroclient.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.aeroclient.AeroClient;
import net.aeroclient.config.ConfigManager;
import net.aeroclient.gui.clickgui.ClickGuiScreen;
import net.aeroclient.gui.clickgui.Widgets;
import net.aeroclient.hud.HudModule;
import net.aeroclient.module.Module;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * The HUD editor: every HUD module that's turned on can be dragged anywhere
 * on the screen and resized.
 *
 * <ul>
 * <li>Drag an element to move it. It snaps to the screen edges, the center
 * lines and other elements, with a guide line showing what it snapped to.
 * Hold Shift (or turn Snapping off) to place it freely.</li>
 * <li>Scroll over an element, or drag its corner handle, to resize it.</li>
 * <li>Right-click an element to open its settings. Arrow keys nudge the
 * selected element by one step.</li>
 * <li>"Reset Layout" puts every element back where it started (it asks first).</li>
 * </ul>
 *
 * Positions are saved as a corner or edge to stick to plus a fraction of the
 * screen size, so the layout still fits after the window is resized.
 */
public class HudEditorScreen extends GuiScreen {

	/** Snap elements while dragging (saved with the client settings). */
	public static boolean snapping = true;

	private static final float SNAP_DISTANCE = 4.0F; // how close (in GUI units) before something snaps
	private static final float EDGE_GAP = 2.0F; // elements snap this far from the screen edges
	private static final float HANDLE = 7.0F; // size of the resize handle
	private static final long CONFIRM_MS = 3000L;
	private static final long SCALE_LABEL_MS = 900L;
	private static final int BTN_RESET = 0, BTN_SNAP = 1, BTN_DONE = 2, BUTTONS = 3;
	private static final String HINT_1 = "Drag to move  -  Scroll or drag the corner to resize";
	private static final String HINT_2 = "Right-click for settings  -  Hold Shift to move freely";

	private final GuiScreen parent;
	private final List<HudModule> elements = new ArrayList<>();
	private final List<Animation.Fader> elementHover = new ArrayList<>();
	private final Animation.Fader appear = new Animation.Fader(200.0F);
	private final Animation.Fader toolbar = new Animation.Fader(160.0F);
	private final Animation.Fader[] buttonHover = new Animation.Fader[BUTTONS];
	private final float[] buttonX = new float[BUTTONS], buttonW = new float[BUTTONS];
	private float buttonY;

	private HudModule dragging, resizing, selected;
	private float grabX, grabY; // where inside the element the mouse grabbed it
	private float resizeLeft, resizeTop, resizeStartScale, resizeStartDistance;
	private float guideX = Float.NaN, guideY = Float.NaN; // snap lines to draw while dragging
	private long resetArmedAt = -1L;
	private HudModule scaleLabelFor;
	private long scaleLabelAt;

	public HudEditorScreen(GuiScreen parent) {
		this.parent = parent;
		for (int i = 0; i < BUTTONS; i++) {
			this.buttonHover[i] = new Animation.Fader(120.0F);
		}
	}

	// ---------------- setup ----------------

	@Override
	public void initGui() {
		this.elements.clear();
		this.elementHover.clear();
		for (Module m : AeroClient.MODULES.getModules()) {
			if (m instanceof HudModule && m.isEnabled()) {
				this.elements.add((HudModule) m);
				this.elementHover.add(new Animation.Fader(120.0F));
			}
		}
		if (this.selected != null && !this.elements.contains(this.selected)) {
			this.selected = null;
		}

		// toolbar: three pill buttons in a row, just under the middle of the screen
		this.buttonW[BTN_RESET] = Math.max(this.fontRendererObj.getStringWidth("Reset Layout"),
				this.fontRendererObj.getStringWidth("Click again to reset")) + 20;
		this.buttonW[BTN_SNAP] = this.fontRendererObj.getStringWidth("Snapping: Off") + 20;
		this.buttonW[BTN_DONE] = Math.max(60, this.fontRendererObj.getStringWidth("Done") + 20);
		float total = this.buttonW[0] + this.buttonW[1] + this.buttonW[2] + 12;
		float x = (this.width - total) / 2.0F;
		for (int i = 0; i < BUTTONS; i++) {
			this.buttonX[i] = Math.round(x);
			x += this.buttonW[i] + 6;
		}
		this.buttonY = Math.round(this.height / 2.0F + 10);
	}

	private boolean isResetArmed() {
		return this.resetArmedAt >= 0L && Minecraft.getSystemTime() - this.resetArmedAt < CONFIRM_MS;
	}

	private String buttonLabel(int i) {
		switch (i) {
		case BTN_RESET:
			return isResetArmed() ? "Click again to reset" : "Reset Layout";
		case BTN_SNAP:
			return snapping ? "Snapping: On" : "Snapping: Off";
		default:
			return "Done";
		}
	}

	private boolean isToolbarUsable() {
		return this.dragging == null && this.resizing == null && this.toolbar.get() > 0.5F;
	}

	private int buttonAt(float mx, float my) {
		if (!isToolbarUsable()) {
			return -1;
		}
		for (int i = 0; i < BUTTONS; i++) {
			if (Widgets.inside(mx, my, this.buttonX[i], this.buttonY, this.buttonW[i], 18)) {
				return i;
			}
		}
		return -1;
	}

	// ---------------- finding elements under the mouse ----------------

	private boolean isOver(HudModule e, float mx, float my) {
		float x = e.getX(this.width), y = e.getY(this.height);
		return Widgets.inside(mx, my, x, y, e.getWidth(), e.getHeight());
	}

	private boolean isOverHandle(HudModule e, float mx, float my) {
		float right = e.getX(this.width) + e.getWidth(), bottom = e.getY(this.height) + e.getHeight();
		return Widgets.inside(mx, my, right - HANDLE, bottom - HANDLE, HANDLE + 2, HANDLE + 2);
	}

	/** The top-most element under the mouse (later ones are drawn on top), or null. */
	private HudModule elementAt(float mx, float my) {
		for (int i = this.elements.size() - 1; i >= 0; i--) {
			if (isOver(this.elements.get(i), mx, my)) {
				return this.elements.get(i);
			}
		}
		return null;
	}

	// ---------------- drawing ----------------

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		float a = this.appear.update(true);
		if (this.mc.world == null) {
			AeroMenuBackground.drawForScreen(this.mc, this.width, this.height);
		} else {
			RenderUtil.rect(0, 0, this.width, this.height, Widgets.fade(0x66000000, a));
		}
		float px = pixel();
		float cx = this.width / 2.0F, cy = this.height / 2.0F;
		RenderUtil.rect(cx - px / 2, 0, cx + px / 2, this.height, Widgets.fade(0x1FFFFFFF, a));
		RenderUtil.rect(0, cy - px / 2, this.width, cy + px / 2, Widgets.fade(0x1FFFFFFF, a));

		HudModule active = this.dragging != null ? this.dragging
				: (this.resizing != null ? this.resizing : elementAt(mouseX, mouseY));
		boolean overHandle = false;
		for (int i = 0; i < this.elements.size(); i++) {
			HudModule e = this.elements.get(i);
			boolean lit = e == active || e == this.selected;
			float h = this.elementHover.get(i).update(lit);
			float x = snap(e.getX(this.width)), y = snap(e.getY(this.height)), w = e.getWidth(), eh = e.getHeight();
			if (h > 0.01F) {
				RenderUtil.rect(x, y, x + w, y + eh, Widgets.fade(Theme.accent, 0.12F * h * a));
			}
			e.render(this.width, this.height, true);
			outline(x, y, w, eh, px, Widgets.fade(Theme.mix(0x55FFFFFF, Theme.accent, h), a));
			if (h > 0.01F) { // resize handle in the bottom-right corner
				RenderUtil.roundedRect(x + w - HANDLE + 1, y + eh - HANDLE + 1, HANDLE, HANDLE, 2.0F,
						Widgets.fade(Theme.accent, h * a));
				RenderUtil.roundedRect(x + w - HANDLE + 3, y + eh - HANDLE + 3, HANDLE - 4, HANDLE - 4, 1.0F,
						Widgets.fade(0xFFFFFFFF, h * a));
			}
			if (e == active && this.dragging == null && isOverHandle(e, mouseX, mouseY)) {
				overHandle = true;
			}
		}

		// snap guides
		if (this.dragging != null) {
			if (!Float.isNaN(this.guideX)) {
				RenderUtil.rect(this.guideX - px / 2, 0, this.guideX + px / 2, this.height, Widgets.fade(Theme.accent, a));
			}
			if (!Float.isNaN(this.guideY)) {
				RenderUtil.rect(0, this.guideY - px / 2, this.width, this.guideY + px / 2, Widgets.fade(Theme.accent, a));
			}
		}

		// name and size of the element being worked on
		HudModule labelFor = active != null ? active : this.selected;
		if (labelFor == null && this.scaleLabelFor != null
				&& Minecraft.getSystemTime() - this.scaleLabelAt < SCALE_LABEL_MS) {
			labelFor = this.scaleLabelFor;
		}
		if (labelFor != null) {
			drawLabel(labelFor, a);
		}

		// toolbar and hints (they get out of the way while you drag)
		float t = this.toolbar.update(this.dragging == null && this.resizing == null) * a;
		drawToolbar(mouseX, mouseY, t);

		if (active != null || overHandle || buttonAt(mouseX, mouseY) >= 0) {
			Widgets.handCursor();
		}
	}

	/** One screen pixel, in GUI units, so lines are exactly one pixel thick. */
	private float pixel() {
		return this.width / (float) Math.max(1, this.mc.displayWidth);
	}

	private float snap(float v) {
		float p = pixel();
		return Math.round(v / p) * p;
	}

	private static void outline(float x, float y, float w, float h, float t, int color) {
		RenderUtil.beginBatch(false);
		RenderUtil.batchRect(x, y, x + w, y + t, color);
		RenderUtil.batchRect(x, y + h - t, x + w, y + h, color);
		RenderUtil.batchRect(x, y + t, x + t, y + h - t, color);
		RenderUtil.batchRect(x + w - t, y + t, x + w, y + h - t, color);
		RenderUtil.endBatch();
	}

	private void drawLabel(HudModule e, float a) {
		String text = e.getName() + "  " + Math.round(e.getScale() * 100.0F) + "%";
		float tw = this.fontRendererObj.getStringWidth(text) + 10;
		float x = e.getX(this.width) + (e.getWidth() - tw) / 2.0F, y = e.getY(this.height) - 15;
		if (y < 2) {
			y = e.getY(this.height) + e.getHeight() + 3; // no room above: show it underneath
		}
		x = Math.max(2, Math.min(x, this.width - tw - 2));
		RenderUtil.pill(x, y, tw, 12, Widgets.fade(Theme.accent, a));
		RenderUtil.pill(x + 1, y + 1, tw - 2, 10, Widgets.fade(0xF20E1218, a));
		Widgets.text(this.fontRendererObj, text, x + 5, y + 2, Theme.text, a);
	}

	private void drawToolbar(int mouseX, int mouseY, float t) {
		if (t <= 0.01F) {
			return;
		}
		float cx = this.width / 2.0F;
		// a dark card behind the hints and buttons, so they're easy to read over any background
		float cardW = Math.min(this.width - 8, Math.max(this.fontRendererObj.getStringWidth(HINT_1),
				this.buttonW[0] + this.buttonW[1] + this.buttonW[2] + 12) + 24);
		RenderUtil.shadow(cx - cardW / 2.0F, this.buttonY - 42, cardW, 68, 10.0F, 10.0F, Widgets.fade(0x99000000, t));
		RenderUtil.roundedRect(cx - cardW / 2.0F, this.buttonY - 42, cardW, 68, 10.0F, Widgets.fade(0xD90C1016, t));
		if (this.elements.isEmpty()) {
			Widgets.textCentered(this.fontRendererObj, "No HUD modules are on yet.", cx, this.buttonY - 34,
					Theme.text, t);
			Widgets.textCentered(this.fontRendererObj,
					TextUtil.trim(this.fontRendererObj, "Turn one on in the Right Shift menu, then come back.",
							this.width - 16),
					cx, this.buttonY - 22, Theme.textFaded, t);
		} else {
			Widgets.textCentered(this.fontRendererObj, TextUtil.trim(this.fontRendererObj, HINT_1, this.width - 16),
					cx, this.buttonY - 34, Theme.text, t);
			Widgets.textCentered(this.fontRendererObj, TextUtil.trim(this.fontRendererObj, HINT_2, this.width - 16),
					cx, this.buttonY - 22, Theme.textFaded, t);
		}

		int hovered = buttonAt(mouseX, mouseY);
		for (int i = 0; i < BUTTONS; i++) {
			float h = this.buttonHover[i].update(i == hovered);
			boolean armed = i == BTN_RESET && isResetArmed();
			boolean on = i == BTN_SNAP && snapping;
			int edge = armed ? Theme.danger : Theme.mix(0xFF2E3A4B, Theme.accent, i == BTN_DONE ? 0.6F + 0.4F * h : h);
			int fill = armed ? 0xFF3A1518 : Theme.mix(0xFF121820, 0xFF1C2532, h);
			float x = this.buttonX[i], y = this.buttonY, w = this.buttonW[i];
			RenderUtil.shadow(x, y + 1, w, 18, 9.0F, 5.0F, Widgets.fade(0x88000000, t));
			RenderUtil.pill(x, y, w, 18, Widgets.fade(edge, t));
			RenderUtil.pill(x + 1, y + 1, w - 2, 16, Widgets.fade(fill, t));
			int color = armed ? 0xFFFECACA : (on ? Theme.accentLight : Theme.mix(Theme.text, 0xFFFFFFFF, h));
			Widgets.textCentered(this.fontRendererObj, buttonLabel(i), x + w / 2.0F, y + 5, color, t);
		}
	}

	// ---------------- snapping ----------------

	/** Moves x onto a nearby edge, center line or other element, if one is close enough. */
	private float snapX(HudModule e, float x) {
		float w = e.getWidth();
		this.guideX = Float.NaN;
		float[] best = { SNAP_DISTANCE, x };
		trySnapX(best, x, w, EDGE_GAP);
		trySnapX(best, x, w, this.width / 2.0F);
		trySnapX(best, x, w, this.width - EDGE_GAP);
		for (HudModule other : this.elements) {
			if (other != e) {
				float ox = other.getX(this.width), ow = other.getWidth();
				trySnapX(best, x, w, ox);
				trySnapX(best, x, w, ox + ow / 2.0F);
				trySnapX(best, x, w, ox + ow);
			}
		}
		return best[1];
	}

	private float snapY(HudModule e, float y) {
		float h = e.getHeight();
		this.guideY = Float.NaN;
		float[] best = { SNAP_DISTANCE, y };
		trySnapY(best, y, h, EDGE_GAP);
		trySnapY(best, y, h, this.height / 2.0F);
		trySnapY(best, y, h, this.height - EDGE_GAP);
		for (HudModule other : this.elements) {
			if (other != e) {
				float oy = other.getY(this.height), oh = other.getHeight();
				trySnapY(best, y, h, oy);
				trySnapY(best, y, h, oy + oh / 2.0F);
				trySnapY(best, y, h, oy + oh);
			}
		}
		return best[1];
	}

	/**
	 * Tries lining up the element's left edge, middle and right edge with the
	 * line at {@code target}. best[0] is the closest distance so far, best[1]
	 * the position that gives it.
	 */
	private void trySnapX(float[] best, float x, float w, float target) {
		for (int part = 0; part < 3; part++) {
			float offset = w * part / 2.0F;
			float candidate = target - offset;
			float distance = Math.abs(x + offset - target);
			if (distance < best[0] && candidate >= 0.0F && candidate + w <= this.width) {
				best[0] = distance;
				best[1] = candidate;
				this.guideX = target;
			}
		}
	}

	private void trySnapY(float[] best, float y, float h, float target) {
		for (int part = 0; part < 3; part++) {
			float offset = h * part / 2.0F;
			float candidate = target - offset;
			float distance = Math.abs(y + offset - target);
			if (distance < best[0] && candidate >= 0.0F && candidate + h <= this.height) {
				best[0] = distance;
				best[1] = candidate;
				this.guideY = target;
			}
		}
	}

	// ---------------- mouse ----------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (mouseButton == 0) {
			int b = buttonAt(mouseX, mouseY);
			if (b >= 0) {
				Widgets.click(1.0F);
				pressButton(b);
				return;
			}
		}
		// resize handles first (top-most element first), then the elements themselves
		for (int i = this.elements.size() - 1; i >= 0; i--) {
			HudModule e = this.elements.get(i);
			if (mouseButton == 0 && (e == this.selected || isOver(e, mouseX, mouseY))
					&& isOverHandle(e, mouseX, mouseY)) {
				this.resizing = e;
				this.selected = e;
				this.resizeLeft = e.getX(this.width);
				this.resizeTop = e.getY(this.height);
				this.resizeStartScale = e.getScale();
				this.resizeStartDistance = Math.max(6.0F, distance(mouseX, mouseY, this.resizeLeft, this.resizeTop));
				return;
			}
		}
		HudModule e = elementAt(mouseX, mouseY);
		if (e == null) {
			this.selected = null;
			return;
		}
		this.selected = e;
		if (mouseButton == 0) {
			this.dragging = e;
			this.grabX = mouseX - e.getX(this.width);
			this.grabY = mouseY - e.getY(this.height);
		} else if (mouseButton == 1) {
			Widgets.click(1.0F);
			ClickGuiScreen.openSettings(this, e); // its settings, in the Right Shift menu
		}
	}

	private static float distance(float x0, float y0, float x1, float y1) {
		float dx = x0 - x1, dy = y0 - y1;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	private void pressButton(int b) {
		switch (b) {
		case BTN_RESET:
			if (isResetArmed()) {
				for (Module m : AeroClient.MODULES.getModules()) {
					if (m instanceof HudModule) {
						((HudModule) m).resetLayout();
					}
				}
				ConfigManager.markDirty();
				this.resetArmedAt = -1L;
			} else {
				this.resetArmedAt = Minecraft.getSystemTime();
			}
			break;
		case BTN_SNAP:
			snapping = !snapping;
			ConfigManager.markDirty();
			break;
		default:
			this.mc.displayGuiScreen(this.parent);
			break;
		}
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (this.dragging != null) {
			float x = mouseX - this.grabX, y = mouseY - this.grabY;
			this.guideX = this.guideY = Float.NaN;
			if (snapping && !isShiftKeyDown()) {
				x = snapX(this.dragging, x);
				y = snapY(this.dragging, y);
			}
			this.dragging.setPosition(x, y, this.width, this.height);
		} else if (this.resizing != null) {
			float d = distance(mouseX, mouseY, this.resizeLeft, this.resizeTop);
			this.resizing.setScale(this.resizeStartScale * d / this.resizeStartDistance);
			this.resizing.setPosition(this.resizeLeft, this.resizeTop, this.width, this.height); // keep the top-left still
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		if (this.resizing != null) {
			this.scaleLabelFor = this.resizing;
			this.scaleLabelAt = Minecraft.getSystemTime();
		}
		this.dragging = null;
		this.resizing = null;
		this.guideX = this.guideY = Float.NaN;
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0 || this.dragging != null || this.resizing != null) {
			return;
		}
		int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		HudModule e = elementAt(mx, my);
		if (e == null) {
			return;
		}
		// grow or shrink around the element's middle
		float centerX = e.getX(this.width) + e.getWidth() / 2.0F, centerY = e.getY(this.height) + e.getHeight() / 2.0F;
		e.setScale(e.getScale() + (wheel > 0 ? 0.05D : -0.05D));
		e.setPosition(centerX - e.getWidth() / 2.0F, centerY - e.getHeight() / 2.0F, this.width, this.height);
		this.selected = e;
		this.scaleLabelFor = e;
		this.scaleLabelAt = Minecraft.getSystemTime();
	}

	// ---------------- keyboard ----------------

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) { // Escape
			this.mc.displayGuiScreen(this.parent);
			return;
		}
		if (this.selected == null) {
			return;
		}
		int step = isShiftKeyDown() ? 10 : 1;
		float dx = keyCode == 203 ? -step : (keyCode == 205 ? step : 0);
		float dy = keyCode == 200 ? -step : (keyCode == 208 ? step : 0);
		if (dx != 0 || dy != 0) {
			this.selected.setPosition(this.selected.getX(this.width) + dx, this.selected.getY(this.height) + dy,
					this.width, this.height);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
