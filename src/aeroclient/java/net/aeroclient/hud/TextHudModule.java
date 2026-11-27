package net.aeroclient.hud;

import net.aeroclient.module.Category;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * A HUD module that shows a few lines of text like "FPS: 60". Each line has
 * a label (in the accent color) and a value (in the normal text color, or any
 * color the module picks, like green/yellow/red for ping).
 *
 * Fast by design: the text is only rebuilt every {@link #refreshInterval()}
 * milliseconds, and modules only make new strings when a value actually
 * changes. Drawing a frame just draws the strings that are already there.
 *
 * Text lines up with the side of the screen the element is on: left, center
 * or right, so a right-corner element grows toward the middle of the screen.
 */
public abstract class TextHudModule extends HudModule {

	private static final int MAX_LINES = 8;
	private static final float LINE_H = 10.0F;

	protected final BooleanSetting textShadow = add(
			new BooleanSetting("Text Shadow", "Draw a soft shadow under the text.", true));

	private final String[] labels = new String[MAX_LINES];
	private final String[] values = new String[MAX_LINES];
	private final int[] valueColors = new int[MAX_LINES];
	private final float[] labelWidths = new float[MAX_LINES];
	private final float[] lineWidths = new float[MAX_LINES];
	private int lineCount = 0;
	private float width = 0.0F;
	private boolean widthDirty = true;
	private long lastRefresh = -1L;

	protected TextHudModule(String name, String description, int keybind, Anchor anchor, float offsetX,
			float offsetY) {
		this(name, description, Category.HUD, keybind, anchor, offsetX, offsetY);
	}

	protected TextHudModule(String name, String description, Category category, int keybind, Anchor anchor,
			float offsetX, float offsetY) {
		super(name, description, category, keybind, anchor, offsetX, offsetY, true);
		for (int i = 0; i < MAX_LINES; i++) {
			this.labels[i] = "";
			this.values[i] = "";
		}
	}

	/** How often (in milliseconds) {@link #refresh()} runs. */
	protected long refreshInterval() {
		return 50L;
	}

	/**
	 * Updates the lines with {@link #setLineCount} and {@link #setLine}. Runs
	 * every {@link #refreshInterval()} ms. Only build new strings when a value
	 * changed; {@link #isPreview()} says when to show example values.
	 */
	protected abstract void refresh();

	/** How many lines to show (up to 8). */
	protected final void setLineCount(int count) {
		count = Math.max(0, Math.min(MAX_LINES, count));
		if (count != this.lineCount) {
			this.lineCount = count;
			this.widthDirty = true;
		}
	}

	/** Sets one line. The label is drawn in the accent color, the value in {@code valueColor}. */
	protected final void setLine(int index, String label, String value, int valueColor) {
		if (index < 0 || index >= MAX_LINES) {
			return;
		}
		if (!label.equals(this.labels[index]) || !value.equals(this.values[index])) {
			this.labels[index] = label;
			this.values[index] = value;
			this.widthDirty = true;
		}
		this.valueColors[index] = valueColor;
	}

	/** Shortcut: a line with the value in the normal text color. */
	protected final void setLine(int index, String label, String value) {
		setLine(index, label, value, Theme.text);
	}

	private void update() {
		long now = Minecraft.getSystemTime();
		if (this.lastRefresh < 0L || now - this.lastRefresh >= refreshInterval()) {
			this.lastRefresh = now;
			refresh();
		}
		if (this.widthDirty) {
			this.widthDirty = false;
			FontRenderer font = this.mc.fontRendererObj;
			float w = 0.0F;
			for (int i = 0; i < this.lineCount; i++) {
				this.labelWidths[i] = font.getStringWidth(this.labels[i]);
				this.lineWidths[i] = this.labelWidths[i] + font.getStringWidth(this.values[i]);
				w = Math.max(w, this.lineWidths[i]);
			}
			this.width = w;
		}
	}

	@Override
	protected boolean hasContent() {
		update();
		return this.lineCount > 0;
	}

	@Override
	protected float getContentWidth() {
		update();
		return Math.max(8.0F, this.width + 1.0F); // +1 for the text shadow
	}

	@Override
	protected float getContentHeight() {
		return Math.max(1, this.lineCount) * LINE_H - 2.0F;
	}

	@Override
	protected void renderContent(boolean editing) {
		update();
		FontRenderer font = this.mc.fontRendererObj;
		boolean shadow = this.textShadow.isOn();
		int column = getAnchor().column;
		for (int i = 0; i < this.lineCount; i++) {
			float x = column == 0 ? 0.0F
					: (column == 1 ? (this.width - this.lineWidths[i]) / 2.0F : this.width - this.lineWidths[i]);
			float y = i * LINE_H;
			if (!this.labels[i].isEmpty()) {
				font.drawString(this.labels[i], x, y, Theme.accentLight, shadow);
			}
			font.drawString(this.values[i], x + this.labelWidths[i], y, this.valueColors[i], shadow);
		}
	}

	// ---------------- number formatting without making garbage every frame ----------------

	/** A number with one decimal place, like "-38.2" (no floating-point oddities like 0.30000000004). */
	protected static String oneDecimal(double value) {
		long tenths = Math.round(value * 10.0D);
		boolean negative = tenths < 0;
		tenths = Math.abs(tenths);
		return (negative ? "-" : "") + (tenths / 10) + "." + (tenths % 10);
	}

	/** A number with two decimal places, like "4.32". */
	protected static String twoDecimals(double value) {
		long hundredths = Math.round(value * 100.0D);
		boolean negative = hundredths < 0;
		hundredths = Math.abs(hundredths);
		long frac = hundredths % 100;
		return (negative ? "-" : "") + (hundredths / 100) + "." + (frac < 10 ? "0" : "") + frac;
	}
}
