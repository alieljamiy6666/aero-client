package net.aeroclient.gui.clickgui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.aeroclient.AeroClient;
import net.aeroclient.event.KeyEvent;
import net.aeroclient.gui.AeroBranding;
import net.aeroclient.gui.AeroMenuBackground;
import net.aeroclient.gui.AeroProfilesScreen;
import net.aeroclient.gui.AeroSettingsScreen;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/**
 * The Aero Client menu, opened with Right Shift.
 *
 * <pre>
 * +-----------+-----------------------------------------------+
 * | A  Aero   | [ search modules...      ]   (hud)(pro)(set) X |
 * +-----------+-----------------------------------------------+
 * | All     3 | All Modules  3                                 |
 * | Render  0 | +----------+ +----------+ +----------+          |
 * | HUD     0 | | card     | | card     | | card     |          |
 * | Misc    3 | +----------+ +----------+ +----------+          |
 * | PvP     0 |                       (settings slide in here)|
 * +-----------+-----------------------------------------------+
 * </pre>
 *
 * It fades and grows in when it opens and does the reverse when it closes,
 * darkens the game behind it, and fits small screens (the sidebar shrinks to
 * icons, and the settings panel covers the grid when there's no room).
 */
public class ClickGuiScreen extends GuiScreen {

	/** Right Shift opens and closes the menu. */
	public static final int MENU_KEY = 54;

	private static final float HEADER_H = 30.0F;
	private static final float PAD = 10.0F;
	private static final float GAP = 6.0F;
	private static final float CARD_MIN_W = 128.0F;
	private static final float ITEM_H = 20.0F, ITEM_STEP = 22.0F;
	private static final long TOOLTIP_DELAY_MS = 450L;
	private static final long TOAST_MS = 2200L;

	private static final Category[] CATEGORIES = Category.values();
	private static final PixelIcons.Icon[] CATEGORY_ICONS = { PixelIcons.ALL, PixelIcons.RENDER, PixelIcons.HUD,
			PixelIcons.MISC, PixelIcons.PVP };

	private static final int BTN_HUD = 0, BTN_PROFILES = 1, BTN_SETTINGS = 2, BTN_CLOSE = 3;
	private static final PixelIcons.Icon[] BUTTON_ICONS = { PixelIcons.MOVE, PixelIcons.PROFILE, PixelIcons.GEAR,
			PixelIcons.CLOSE };
	private static final String[] BUTTON_TIPS = { "HUD Editor", "Profiles", "Client Settings", "Close (Right Shift)" };

	private static final String VERSION_TEXT = "v" + AeroClient.VERSION;
	private static final String CREDIT_TEXT = AeroClient.getDisplayName() + " - Made by " + AeroClient.AUTHOR;

	/** The category you were looking at last time, so the menu reopens there. */
	private static Category lastCategory = Category.ALL;
	private static boolean installed = false;

	private final GuiScreen parent;
	private final List<ModuleCard> allCards = new ArrayList<>();
	private final List<ModuleCard> shownCards = new ArrayList<>();
	private final SettingsPanel panel = new SettingsPanel(this);
	private final Animation.Fader appear = new Animation.Fader(170.0F);
	private final Animation.Fader[] categoryHover = new Animation.Fader[CATEGORIES.length];
	private final Animation.Fader[] buttonHover = new Animation.Fader[BUTTON_ICONS.length];
	private final Animation.Fader searchFocus = new Animation.Fader(150.0F);
	private final String[] categoryCounts = new String[CATEGORIES.length];

	private Category category = lastCategory;
	private GuiTextField search;
	private String query = "";
	private boolean closing = false, finished = false;

	// layout (GUI units), worked out in initGui
	private float winX, winY, winW, winH, sideW, contentX, contentY, contentW;
	private float gridTop, gridBottom, searchX, searchY, searchW;
	private boolean narrow;
	private int columns = 1;
	private float cardW, gridHeight;

	// scrolling and animation
	private float scroll, scrollTarget, maxScroll;
	private boolean draggingBar = false;
	private float barGrab;
	private long lastFrame = -1L;
	private float frameSeconds = 0.0F;
	private float selectionY = -1.0F;
	private long contentChangedAt = 0L;
	private float scale = 1.0F, centerX, centerY;

	// headline text, rebuilt only when the list changes
	private String title = "", subtitle = "";

	// tooltip: whatever the mouse is over this frame, and since when
	private Object tipKey, tipKeyThisFrame;
	private String tipText, tipTextThisFrame;
	private long tipSince;

	private String toast = null;
	private long toastAt;

	public ClickGuiScreen(GuiScreen parent) {
		this.parent = parent;
		for (Module m : AeroClient.MODULES.getModules()) {
			this.allCards.add(new ModuleCard(m));
		}
		Collections.sort(this.allCards, (a, b) -> a.module.getName().compareToIgnoreCase(b.module.getName()));
		for (int i = 0; i < CATEGORIES.length; i++) {
			this.categoryHover[i] = new Animation.Fader(120.0F);
			this.categoryCounts[i] = Integer.toString(AeroClient.MODULES.getModules(CATEGORIES[i]).size());
		}
		for (int i = 0; i < BUTTON_ICONS.length; i++) {
			this.buttonHover[i] = new Animation.Fader(120.0F);
		}
	}

	/** Opens the menu on top of {@code parent} (null = back to the game when it closes). */
	public static void open(GuiScreen parent) {
		Minecraft.getMinecraft().displayGuiScreen(new ClickGuiScreen(parent));
	}

	/** Makes Right Shift open the menu while playing. Called once at startup. */
	public static void install() {
		if (installed) {
			return;
		}
		installed = true;
		AeroClient.EVENTS.subscribe(KeyEvent.class, e -> {
			if (e.isPressed() && !e.isRepeat() && e.getKey() == MENU_KEY
					&& Minecraft.getMinecraft().currentScreen == null) {
				open(null);
			}
		});
	}

	// ---------------- layout ----------------

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		this.winW = Math.min(this.width - 12, 480);
		this.winH = Math.min(this.height - 12, 300);
		this.winX = Math.round((this.width - this.winW) / 2.0F);
		this.winY = Math.round((this.height - this.winH) / 2.0F);
		this.narrow = this.winW < 380;
		this.sideW = this.narrow ? 28 : 88;
		this.contentX = this.winX + this.sideW;
		this.contentY = this.winY + HEADER_H;
		this.contentW = this.winW - this.sideW;
		this.gridTop = this.contentY + 24;
		this.gridBottom = this.winY + this.winH - 4;
		this.centerX = this.winX + this.winW / 2.0F;
		this.centerY = this.winY + this.winH / 2.0F;

		float inner = this.contentW - PAD * 2;
		this.columns = Math.max(1, Math.min(4, (int) ((inner + GAP) / (CARD_MIN_W + GAP))));
		this.cardW = (float) Math.floor((inner - GAP * (this.columns - 1)) / this.columns);

		// search box: from the content's left edge to the header buttons
		this.searchX = this.contentX + 8;
		this.searchY = this.winY + 7;
		this.searchW = Math.max(60, Math.min(220, buttonX(BTN_HUD) - 14 - this.searchX));
		String text = this.search == null ? "" : this.search.getText();
		this.search = new GuiTextField(0, this.fontRendererObj, (int) this.searchX + 18, (int) this.searchY + 4,
				(int) this.searchW - 32, 10);
		this.search.setEnableBackgroundDrawing(false);
		this.search.setMaxStringLength(32);
		this.search.setText(text);
		this.search.setFocused(true);
		this.search.setCanLoseFocus(false);

		// the settings panel covers the right side of the grid, or all of it on small screens
		float pw = this.contentW < 330 ? this.contentW - 8 : 212;
		this.panel.setBounds(this.winX + this.winW - pw - 4, this.contentY + 4, pw, this.winH - HEADER_H - 8);

		refilter(false);
	}

	/** Center x of a header button (they sit at the right end of the header). */
	private float buttonX(int index) {
		float close = this.winX + this.winW - 15;
		return index == BTN_CLOSE ? close : close - 26 - (BTN_SETTINGS - index) * 20;
	}

	private float buttonY() {
		return this.winY + 15;
	}

	/** Rebuilds the list of cards for the current category and search, and places them in the grid. */
	private void refilter(boolean resetScroll) {
		String q = this.query.trim().toLowerCase();
		this.shownCards.clear();
		for (ModuleCard card : this.allCards) {
			Module m = card.module;
			boolean match = q.isEmpty()
					? (this.category.isView() || m.getCategory() == this.category)
					: (m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q));
			if (match) {
				this.shownCards.add(card);
			}
		}
		for (int i = 0; i < this.shownCards.size(); i++) {
			ModuleCard card = this.shownCards.get(i);
			card.x = this.contentX + PAD + (i % this.columns) * (this.cardW + GAP);
			card.y = this.gridTop + 4 + (i / this.columns) * (ModuleCard.HEIGHT + GAP);
			card.w = this.cardW;
		}
		int rows = (this.shownCards.size() + this.columns - 1) / this.columns;
		this.gridHeight = rows == 0 ? 0 : rows * (ModuleCard.HEIGHT + GAP) - GAP + 10;
		this.maxScroll = Math.max(0, this.gridHeight - (this.gridBottom - this.gridTop));
		if (resetScroll) {
			this.scroll = this.scrollTarget = 0;
			this.contentChangedAt = Minecraft.getSystemTime();
		} else {
			this.scrollTarget = Math.min(this.scrollTarget, this.maxScroll);
		}

		int n = this.shownCards.size();
		this.title = q.isEmpty() ? (this.category.isView() ? "All Modules" : this.category.getDisplayName())
				: "Results for \"" + this.query.trim() + "\"";
		this.subtitle = n + (n == 1 ? " module" : " modules");
	}

	private void selectCategory(Category c) {
		if (c != this.category || !this.query.isEmpty()) {
			this.category = c;
			lastCategory = c;
			this.query = "";
			this.search.setText("");
			refilter(true);
		}
		this.panel.close();
	}

	// ---------------- small helpers used by the other parts ----------------

	boolean isClosing() {
		return this.closing;
	}

	/** Fraction to move toward a target this frame: frame-rate independent smoothing. */
	float smoothing(float speed) {
		return 1.0F - (float) Math.exp(-speed * this.frameSeconds);
	}

	/** Something under the mouse that has a tooltip; it shows after a short wait. */
	void offerTooltip(Object key, String text) {
		this.tipKeyThisFrame = key;
		this.tipTextThisFrame = text;
	}

	private void showToast(String text) {
		this.toast = text;
		this.toastAt = Minecraft.getSystemTime();
	}

	private float toLocalX(float mouseX) {
		return this.centerX + (mouseX - this.centerX) / this.scale;
	}

	private float toLocalY(float mouseY) {
		return this.centerY + (mouseY - this.centerY) / this.scale;
	}

	private void close() {
		if (!this.closing) {
			this.closing = true;
			this.panel.cancelInput();
			this.draggingBar = false;
		}
	}

	// ---------------- drawing ----------------

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		this.frameSeconds = this.lastFrame < 0L ? 0.0F : Math.min(0.1F, (now - this.lastFrame) / 1000.0F);
		this.lastFrame = now;

		float a = this.appear.update(!this.closing);
		if (this.closing && a <= 0.0F) {
			if (!this.finished) {
				this.finished = true;
				this.mc.displayGuiScreen(this.parent);
				GuiScreen next = this.mc.currentScreen;
				if (next != null && next != this) {
					next.drawScreen(mouseX, mouseY, partialTicks); // draw it now, so there's no empty frame
				}
			}
			return;
		}

		// behind the menu: the menu sky (on the title screen) or the game, darkened
		if (this.mc.world == null) {
			AeroMenuBackground.drawForScreen(this.mc, this.width, this.height);
		}
		drawDarkening(a);

		this.scale = 0.94F + 0.06F * a;
		float mx = toLocalX(mouseX), my = toLocalY(mouseY);
		this.tipKeyThisFrame = null;
		this.tipTextThisFrame = null;

		GlStateManager.pushMatrix();
		GlStateManager.translate(this.centerX, this.centerY, 0.0F);
		GlStateManager.scale(this.scale, this.scale, 1.0F);
		GlStateManager.translate(-this.centerX, -this.centerY, 0.0F);

		drawWindow(a);
		drawHeader(mx, my, a);
		drawSidebar(mx, my, a);
		drawGrid(mx, my, a);
		this.panel.draw(this.fontRendererObj, mx, my, a);
		drawToast(a);
		drawTooltip(mx, my, a, now);

		GlStateManager.popMatrix();

		if (this.winY + this.winH + 14 <= this.height) {
			Widgets.textCentered(this.fontRendererObj, CREDIT_TEXT, this.width / 2.0F, this.height - 11,
					Theme.textFaded, a * 0.8F);
		}
	}

	private static final int DARK_TOP = 0x99060A10, DARK_BOTTOM = 0xCC060A10;

	/**
	 * Darkens everything behind the menu. Once the menu is fully open, the part
	 * hidden behind the window is skipped (it would be covered anyway), which
	 * saves a lot of work on slow graphics chips.
	 */
	private void drawDarkening(float a) {
		if (a < 0.99F) {
			RenderUtil.gradientV(0, 0, this.width, this.height, Widgets.fade(DARK_TOP, a), Widgets.fade(DARK_BOTTOM, a));
			return;
		}
		float x0 = this.winX + 12, y0 = this.winY + 12, x1 = this.winX + this.winW - 12, y1 = this.winY + this.winH - 12;
		RenderUtil.beginBatch(false);
		darkStrip(0, 0, this.width, y0); // above
		darkStrip(0, y1, this.width, this.height); // below
		darkStrip(0, y0, x0, y1); // left
		darkStrip(x1, y0, this.width, y1); // right
		RenderUtil.endBatch();
	}

	private void darkStrip(float x0, float y0, float x1, float y1) {
		int top = Theme.mix(DARK_TOP, DARK_BOTTOM, y0 / this.height);
		int bottom = Theme.mix(DARK_TOP, DARK_BOTTOM, y1 / this.height);
		RenderUtil.batchGradientV(x0, y0, x1, y1, top, bottom);
	}

	private void drawWindow(float a) {
		float x = this.winX, y = this.winY, w = this.winW, h = this.winH;
		if (a >= 0.99F) {
			RenderUtil.glowOuter(x, y, w, h, 12.0F, 22.0F, Widgets.fade(Theme.accent, 0.07F));
		} else {
			RenderUtil.glow(x, y, w, h, 12.0F, 22.0F, Widgets.fade(Theme.accent, 0.07F * a));
		}
		Widgets.panel(x, y, w, h, 12.0F, 0xFF2B3646, 0xFF151B24, 0xFF0C1016, 16.0F, 0, a);
		// a thin line of accent light along the top edge
		float mid = x + w / 2.0F;
		RenderUtil.gradientH(x + 24, y + 1, mid, y + 2, 0, Widgets.fade(Theme.accent, 0.8F * a));
		RenderUtil.gradientH(mid, y + 1, x + w - 24, y + 2, Widgets.fade(Theme.accent, 0.8F * a), 0);
		// header and sidebar dividers
		RenderUtil.rect(x + 1, this.contentY, x + w - 1, this.contentY + 1, Widgets.fade(0x16FFFFFF, a));
		RenderUtil.rect(this.contentX, this.contentY + 6, this.contentX + 1, y + h - 6, Widgets.fade(0x12FFFFFF, a));
	}

	private void drawHeader(float mx, float my, float a) {
		boolean interactive = !this.closing;
		// the logo badge and name
		AeroBranding.drawEmblem(this.mc, (int) this.winX + 8, (int) this.winY + 8, 14, a);
		if (!this.narrow) {
			Widgets.text(this.fontRendererObj, "Aero", this.winX + 27, this.winY + 11, Theme.accentLight, a);
			Widgets.text(this.fontRendererObj, "Client", this.winX + 27 + this.fontRendererObj.getStringWidth("Aero "),
					this.winY + 11, Theme.text, a);
		}

		// search box
		boolean overSearch = interactive && Widgets.inside(mx, my, this.searchX, this.searchY, this.searchW, 16);
		if (overSearch) {
			Widgets.textCursor();
		}
		float f = this.searchFocus.update(!this.query.isEmpty() || overSearch);
		RenderUtil.roundedRect(this.searchX, this.searchY, this.searchW, 16, 8.0F,
				Widgets.fade(Theme.mix(0xFF2B3646, Theme.accent, 0.7F * f), a));
		RenderUtil.roundedRect(this.searchX + 1, this.searchY + 1, this.searchW - 2, 14, 7.0F,
				Widgets.fade(0xFF0C1117, a));
		PixelIcons.SEARCH.draw(this.searchX + 6, this.searchY + 4, 1.0F,
				Widgets.fade(Theme.mix(Theme.textFaded, Theme.accentLight, f), a));
		if (this.query.isEmpty()) {
			Widgets.text(this.fontRendererObj,
					TextUtil.trim(this.fontRendererObj, "Search modules...", (int) this.searchW - 34),
					this.searchX + 18, this.searchY + 4, 0xFF5B6575, a);
		} else {
			boolean overClear = interactive && overClear(mx, my);
			if (overClear) {
				Widgets.handCursor();
			}
			PixelIcons.CLOSE.drawCentered(this.searchX + this.searchW - 9, this.searchY + 8, 1.0F,
					Widgets.fade(overClear ? 0xFFFFFFFF : Theme.textFaded, a));
		}
		if (a > 0.05F) {
			this.search.setTextColor(Widgets.fade(0xFFE5E7EB, a));
			this.search.drawTextBox();
		}

		// buttons on the right
		for (int i = 0; i < BUTTON_ICONS.length; i++) {
			float bx = buttonX(i), by = buttonY();
			boolean over = interactive && overButton(i, mx, my);
			float h = this.buttonHover[i].update(over);
			if (over) {
				Widgets.handCursor();
				offerTooltip(BUTTON_TIPS[i], BUTTON_TIPS[i]);
			}
			int bg = i == BTN_CLOSE ? Theme.danger : 0xFFFFFFFF;
			if (h > 0.01F) {
				RenderUtil.circle(bx, by, 9.0F, Widgets.fade(bg, (i == BTN_CLOSE ? 0.22F : 0.1F) * h * a));
			}
			int icon = Theme.mix(0xFF8B95A5, i == BTN_CLOSE ? 0xFFFCA5A5 : 0xFFFFFFFF, h);
			BUTTON_ICONS[i].drawCentered(bx, by, 1.0F, Widgets.fade(icon, a));
		}
		// a small divider before the close button
		float dx = buttonX(BTN_CLOSE) - 13;
		RenderUtil.rect(dx, this.winY + 9, dx + 1, this.winY + 21, Widgets.fade(0x1AFFFFFF, a));
	}

	private boolean overButton(int i, float mx, float my) {
		float dx = mx - buttonX(i), dy = my - buttonY();
		return dx * dx + dy * dy <= 9.5F * 9.5F;
	}

	private boolean overClear(float mx, float my) {
		return !this.query.isEmpty() && Widgets.inside(mx, my, this.searchX + this.searchW - 17, this.searchY, 17, 16);
	}

	private float categoryY(int i) {
		return this.contentY + 8 + i * ITEM_STEP;
	}

	private int categoryAt(float mx, float my) {
		if (mx < this.winX + 4 || mx >= this.contentX - 2) {
			return -1;
		}
		for (int i = 0; i < CATEGORIES.length; i++) {
			if (my >= categoryY(i) && my < categoryY(i) + ITEM_H) {
				return i;
			}
		}
		return -1;
	}

	private void drawSidebar(float mx, float my, float a) {
		int selected = this.category.ordinal();
		float target = categoryY(selected);
		this.selectionY = this.selectionY < 0 ? target
				: this.selectionY + (target - this.selectionY) * smoothing(18.0F);
		float searching = this.query.isEmpty() ? 1.0F : 0.35F;
		float left = this.winX + 6, width = this.sideW - 12;

		RenderUtil.roundedRect(left, this.selectionY, width, ITEM_H, 6.0F,
				Widgets.fade(Theme.accent, 0.16F * searching * a));
		RenderUtil.pill(left, this.selectionY + 5, 2.5F, ITEM_H - 10, Widgets.fade(Theme.accent, searching * a));

		int hovered = this.closing ? -1 : categoryAt(mx, my);
		for (int i = 0; i < CATEGORIES.length; i++) {
			float y = categoryY(i);
			float h = this.categoryHover[i].update(i == hovered);
			if (i == hovered) {
				Widgets.handCursor();
				if (this.narrow) {
					offerTooltip(CATEGORIES[i], CATEGORIES[i].getDisplayName());
				}
			}
			if (h > 0.01F && i != selected) {
				RenderUtil.roundedRect(left, y, width, ITEM_H, 6.0F, Widgets.fade(0x0FFFFFFF, h * a));
			}
			boolean sel = i == selected && this.query.isEmpty();
			int color = sel ? Theme.accentLight : Theme.mix(0xFF8B95A5, 0xFFFFFFFF, h);
			PixelIcons.Icon icon = CATEGORY_ICONS[i];
			float iconX = this.narrow ? this.winX + this.sideW / 2.0F - 4 : this.winX + 13;
			icon.draw(Math.round(iconX + (8 - icon.getWidth()) / 2.0F), Math.round(y + 6 + (8 - icon.getHeight()) / 2.0F),
					1.0F, Widgets.fade(color, a));
			if (!this.narrow) {
				Widgets.text(this.fontRendererObj, CATEGORIES[i].getDisplayName(), this.winX + 27, y + 6, color, a);
				Widgets.textRight(this.fontRendererObj, this.categoryCounts[i], this.contentX - 10, y + 6,
						sel ? Theme.withAlpha(Theme.accentLight, 0.8F) : 0xFF4B5563, a);
			}
		}

		if (!this.narrow) {
			Widgets.text(this.fontRendererObj, VERSION_TEXT, this.winX + 12, this.winY + this.winH - 14,
					0xFF4B5563, a);
		}
	}

	private boolean isInGrid(float mx, float my) {
		return mx >= this.contentX && mx < this.winX + this.winW && my >= this.gridTop && my < this.gridBottom;
	}

	private void drawGrid(float mx, float my, float a) {
		this.scrollTarget = Math.max(0, Math.min(this.maxScroll, this.scrollTarget));
		this.scroll += (this.scrollTarget - this.scroll) * smoothing(16.0F);

		float since = (Minecraft.getSystemTime() - this.contentChangedAt) / 180.0F;
		float ca = Animation.easeOut(since);
		float ga = a * ca;

		String shownTitle = TextUtil.trim(this.fontRendererObj, this.title, (int) this.contentW - 90);
		Widgets.text(this.fontRendererObj, shownTitle, this.contentX + PAD, this.contentY + 9, 0xFFFFFFFF, a);
		Widgets.text(this.fontRendererObj, this.subtitle,
				this.contentX + PAD + this.fontRendererObj.getStringWidth(shownTitle) + 6, this.contentY + 9,
				0xFF5B6575, a);

		if (this.shownCards.isEmpty()) {
			float cy = (this.gridTop + this.gridBottom) / 2.0F - 10;
			float cx = this.contentX + this.contentW / 2.0F;
			String main = this.query.isEmpty() ? "No modules here yet" : "No modules found";
			String sub = this.query.isEmpty() ? "New modules will show up here as they're added."
					: "Try a different word.";
			PixelIcons.SEARCH.drawCentered(cx, cy - 12, 2.0F, Widgets.fade(0xFF2B3646, ga));
			Widgets.textCentered(this.fontRendererObj, main, cx, cy + 6, Theme.text, ga);
			Widgets.textCentered(this.fontRendererObj,
					TextUtil.trim(this.fontRendererObj, sub, (int) this.contentW - 16), cx, cy + 18, Theme.textFaded,
					ga);
			return;
		}

		float offset = (1.0F - ca) * 6.0F;
		boolean interactive = !this.closing && !this.panel.isShowing() && isInGrid(mx, my) && !this.draggingBar;
		RenderUtil.pushClip(this.contentX + 1, this.gridTop, this.winX + this.winW - 1, this.gridBottom);
		for (int i = 0; i < this.shownCards.size(); i++) {
			ModuleCard card = this.shownCards.get(i);
			float top = card.y - this.scroll - offset;
			if (top + ModuleCard.HEIGHT < this.gridTop - 8 || top > this.gridBottom + 8) {
				continue; // scrolled out of view: skip drawing it
			}
			card.draw(this.fontRendererObj, this.scroll + offset, mx, my, interactive, ga);
			if (interactive && card.isOver(mx, my, this.scroll + offset)) {
				if (card.isOverGear(mx, my, this.scroll + offset)) {
					offerTooltip(card, "Settings");
				} else if (card.isDescriptionCut()) {
					offerTooltip(card, card.module.getDescription());
				}
			}
		}
		// while settings are open, sink the cards into the background
		float dim = this.panel.getProgress();
		if (dim > 0.01F) {
			RenderUtil.roundedRect(this.contentX + 1, this.gridTop, this.winX + this.winW - this.contentX - 2,
					this.gridBottom - this.gridTop, 9.0F, Widgets.fade(0xFF0C1016, 0.6F * dim * a));
		}
		RenderUtil.popClip();

		// scrollbar, only when the cards don't all fit
		if (this.maxScroll > 0.5F) {
			float trackX = this.winX + this.winW - 6, trackTop = this.gridTop + 2, trackH = this.gridBottom - this.gridTop - 4;
			float handleH = barHandleHeight(trackH);
			float handleY = trackTop + (trackH - handleH) * (this.scroll / this.maxScroll);
			boolean over = !this.closing && Widgets.inside(mx, my, trackX - 3, trackTop, 9, trackH);
			RenderUtil.pill(trackX, trackTop, 3, trackH, Widgets.fade(0x14FFFFFF, a));
			RenderUtil.pill(trackX, handleY, 3, handleH,
					Widgets.fade(Theme.accent, (over || this.draggingBar ? 1.0F : 0.7F) * a));
		}
	}

	private float barHandleHeight(float trackH) {
		float viewH = this.gridBottom - this.gridTop;
		return Math.max(16.0F, trackH * viewH / Math.max(viewH, this.gridHeight));
	}

	private void drawToast(float a) {
		if (this.toast == null) {
			return;
		}
		long age = Minecraft.getSystemTime() - this.toastAt;
		if (age > TOAST_MS) {
			this.toast = null;
			return;
		}
		float t = Math.min(Animation.easeOut(age / 150.0F), Animation.clamp01((TOAST_MS - age) / 300.0F)) * a;
		float tw = this.fontRendererObj.getStringWidth(this.toast) + 20;
		float tx = this.centerX - tw / 2.0F, ty = this.winY + this.winH - 26 + (1.0F - t) * 6.0F;
		RenderUtil.shadow(tx, ty, tw, 18, 9.0F, 6.0F, Widgets.fade(0x99000000, t));
		RenderUtil.pill(tx, ty, tw, 18, Widgets.fade(Theme.accent, t));
		RenderUtil.pill(tx + 1, ty + 1, tw - 2, 16, Widgets.fade(0xFF101722, t));
		Widgets.textCentered(this.fontRendererObj, this.toast, this.centerX, ty + 5, Theme.text, t);
	}

	private void drawTooltip(float mx, float my, float a, long now) {
		if (this.tipKeyThisFrame != this.tipKey) {
			this.tipKey = this.tipKeyThisFrame;
			this.tipSince = now;
		}
		this.tipText = this.tipTextThisFrame;
		if (this.tipKey == null || this.tipText == null || this.closing) {
			return;
		}
		long shown = now - this.tipSince - TOOLTIP_DELAY_MS;
		if (shown > 0) {
			boolean below = my < this.contentY; // header buttons: show it under the mouse, not over the buttons
			Widgets.tooltip(this.fontRendererObj, this.tipText, (int) mx, (int) my, (int) (this.winX + this.winW),
					(int) (this.winY + this.winH), below, Animation.easeOut(shown / 120.0F) * a);
		}
	}

	// ---------------- mouse ----------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (this.closing) {
			return;
		}
		float mx = toLocalX(mouseX), my = toLocalY(mouseY);

		if (this.panel.overlayClicked(mx, my)) {
			return; // closed a dropdown or cancelled "press a key"
		}

		// header
		if (mouseButton == 0) {
			if (overClear(mx, my)) {
				this.search.setText("");
				onSearchChanged();
				Widgets.click(1.0F);
				return;
			}
			if (Widgets.inside(mx, my, this.searchX, this.searchY, this.searchW, 16)) {
				this.search.mouseClicked((int) mx, (int) my, mouseButton); // move the text cursor
				return;
			}
			for (int i = 0; i < BUTTON_ICONS.length; i++) {
				if (overButton(i, mx, my)) {
					Widgets.click(1.0F);
					headerButton(i);
					return;
				}
			}
		}

		// settings panel: clicks inside go to it, clicks on the grid or sidebar close it
		if (this.panel.isOpen()) {
			if (this.panel.contains(mx, my)) {
				this.panel.mouseClicked(mx, my, mouseButton);
				return;
			}
			int c = categoryAt(mx, my);
			if (c >= 0 && mouseButton == 0) {
				selectCategory(CATEGORIES[c]);
				Widgets.click(1.0F);
			} else if (Widgets.inside(mx, my, this.winX, this.contentY, this.winW, this.winH - HEADER_H)) {
				this.panel.close();
			}
			return;
		}

		// sidebar
		int c = categoryAt(mx, my);
		if (c >= 0) {
			if (mouseButton == 0) {
				selectCategory(CATEGORIES[c]);
				Widgets.click(1.0F);
			}
			return;
		}

		// scrollbar
		if (this.maxScroll > 0.5F && mouseButton == 0) {
			float trackX = this.winX + this.winW - 6, trackTop = this.gridTop + 2, trackH = this.gridBottom - this.gridTop - 4;
			if (Widgets.inside(mx, my, trackX - 3, trackTop, 9, trackH)) {
				float handleH = barHandleHeight(trackH);
				float handleY = trackTop + (trackH - handleH) * (this.scroll / this.maxScroll);
				this.barGrab = (my >= handleY && my < handleY + handleH) ? my - handleY : handleH / 2.0F;
				this.draggingBar = true;
				dragBar(my);
				return;
			}
		}

		// cards
		if (isInGrid(mx, my)) {
			for (int i = 0; i < this.shownCards.size(); i++) {
				ModuleCard card = this.shownCards.get(i);
				if (!card.isOver(mx, my, this.scroll)) {
					continue;
				}
				if (mouseButton == 1 || (mouseButton == 0 && card.isOverGear(mx, my, this.scroll))) {
					this.panel.open(card.module);
					Widgets.click(1.0F);
				} else if (mouseButton == 0) {
					card.module.toggle();
					Widgets.click(card.module.isEnabled() ? 1.1F : 0.9F);
				}
				return;
			}
		}
	}

	private void headerButton(int index) {
		switch (index) {
		case BTN_HUD:
			showToast("The HUD editor is coming soon");
			break;
		case BTN_PROFILES:
			this.mc.displayGuiScreen(new AeroProfilesScreen(this));
			break;
		case BTN_SETTINGS:
			this.mc.displayGuiScreen(new AeroSettingsScreen(this));
			break;
		case BTN_CLOSE:
			close();
			break;
		default:
			break;
		}
	}

	private void dragBar(float my) {
		float trackTop = this.gridTop + 2, trackH = this.gridBottom - this.gridTop - 4;
		float handleH = barHandleHeight(trackH);
		float f = Animation.clamp01((my - this.barGrab - trackTop) / Math.max(1.0F, trackH - handleH));
		this.scrollTarget = this.scroll = f * this.maxScroll;
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		float mx = toLocalX(mouseX), my = toLocalY(mouseY);
		if (this.draggingBar) {
			dragBar(my);
		} else {
			this.panel.mouseDragged(mx, my);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		this.draggingBar = false;
		this.panel.mouseReleased();
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0 || this.closing) {
			return;
		}
		int direction = wheel > 0 ? 1 : -1;
		float mx = toLocalX(Mouse.getEventX() * this.width / (float) this.mc.displayWidth);
		float my = toLocalY(this.height - Mouse.getEventY() * this.height / (float) this.mc.displayHeight - 1);
		if (this.panel.mouseWheel(mx, my, direction)) {
			return;
		}
		if (!this.panel.isOpen() && isInGrid(mx, my)) {
			this.scrollTarget = Math.max(0, Math.min(this.maxScroll, this.scrollTarget - direction * 34.0F));
		}
	}

	// ---------------- keyboard ----------------

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (this.closing) {
			return;
		}
		boolean repeat = Keyboard.isRepeatEvent();
		if (keyCode != 0 && !repeat && this.panel.keyTyped(keyCode)) {
			return; // a key button was waiting for a key
		}
		if (this.panel.getListening() != null) {
			return; // still waiting for a real key (this was just a typed letter)
		}
		if (keyCode == MENU_KEY) {
			if (!repeat) {
				close();
			}
			return;
		}
		if (keyCode == 1) { // Escape steps back one thing at a time
			if (repeat) {
				return;
			}
			if (this.panel.getOpenDropdown() != null) {
				this.panel.setOpenDropdown(null);
			} else if (this.panel.isOpen()) {
				this.panel.close();
			} else if (!this.query.isEmpty()) {
				this.search.setText("");
				onSearchChanged();
			} else {
				close();
			}
			return;
		}
		if (this.search.textboxKeyTyped(typedChar, keyCode)) {
			onSearchChanged();
		}
	}

	private void onSearchChanged() {
		String text = this.search.getText();
		if (!text.equals(this.query)) {
			this.query = text;
			refilter(true);
			if (!text.isEmpty()) {
				this.panel.close();
			}
		}
	}

	@Override
	public void updateScreen() {
		this.search.updateCursorCounter();
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
		this.panel.cancelInput();
		lastCategory = this.category;
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
