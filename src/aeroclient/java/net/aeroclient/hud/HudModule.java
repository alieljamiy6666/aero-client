package net.aeroclient.hud;

import org.json.JSONObject;

import net.aeroclient.AeroClient;
import net.aeroclient.config.ConfigManager;
import net.aeroclient.event.Render2DEvent;
import net.aeroclient.gui.HudEditorScreen;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;

/**
 * A module that draws something on the HUD (FPS, coordinates, keystrokes...)
 * and can be moved and resized in the HUD editor.
 *
 * Every HUD module gets, for free:
 * <ul>
 * <li>a position: an {@link Anchor} plus a distance from it, stored as a
 * fraction of the screen size so the layout survives window resizes,</li>
 * <li>a Scale setting (also changed with the scroll wheel or corner handle in the editor),</li>
 * <li>a Background on/off setting and a Background Color.</li>
 * </ul>
 *
 * To make one, extend this class and fill in three methods:
 * {@link #getContentWidth()}, {@link #getContentHeight()} and
 * {@link #renderContent(boolean)}. Draw your content starting at (0, 0);
 * moving, scaling and the background are handled here.
 */
public abstract class HudModule extends Module {

	/** Space between the background's edge and the content. */
	public static final float PADDING = 4.0F;

	protected final NumberSetting scale = add(
			new NumberSetting("Scale", "How big it is on screen. You can also scroll over it in the HUD editor.", 1.0D,
					0.5D, 3.0D, 0.05D));
	protected final BooleanSetting background = add(
			new BooleanSetting("Background", "Draw a dark box behind it.", true));
	protected final ColorSetting backgroundColor = add(
			new ColorSetting("Background Color", "The color of the box behind it.", 0x99101419, true));

	private final Anchor defaultAnchor;
	private final float defaultX, defaultY;
	private Anchor anchor;
	private float offsetX, offsetY; // distance from the anchor, as a fraction of the screen size

	/**
	 * @param anchor   where it starts (for example {@link Anchor#TOP_LEFT})
	 * @param offsetX  starting distance from that anchor, as a fraction of the screen width (0.01 = 1%)
	 * @param offsetY  starting distance from that anchor, as a fraction of the screen height
	 */
	protected HudModule(String name, String description, int keybind, Anchor anchor, float offsetX, float offsetY) {
		super(name, description, Category.HUD, keybind);
		this.defaultAnchor = anchor;
		this.defaultX = offsetX;
		this.defaultY = offsetY;
		resetPosition();
		this.backgroundColor.visibleWhen(this.background::isOn);
		listen(Render2DEvent.class, e -> {
			// The HUD editor draws HUD elements itself, and they step aside for the F3 screen.
			if (!(this.mc.currentScreen instanceof HudEditorScreen) && !this.mc.gameSettings.showDebugInfo) {
				render(e.getWidth(), e.getHeight(), false);
			}
		});
	}

	// ---------------- what each HUD module fills in ----------------

	/** Width of the content, before scaling, in GUI units. */
	protected abstract float getContentWidth();

	/** Height of the content, before scaling, in GUI units. */
	protected abstract float getContentHeight();

	/**
	 * Draws the content with its top-left corner at (0, 0).
	 *
	 * @param editing true in the HUD editor, where there may be no world or
	 *                player yet: show example values instead of crashing
	 */
	protected abstract void renderContent(boolean editing);

	/** Draws the box behind the content. HUD modules can change this (e.g. to add a border). */
	protected void drawBackground(float width, float height) {
		RenderUtil.roundedRect(0, 0, width, height, 3.0F, this.backgroundColor.getColor());
	}

	// ---------------- size and position ----------------

	public float getScale() {
		return this.scale.getFloat();
	}

	public void setScale(double value) {
		this.scale.set(value);
	}

	/** Size on screen (after scaling), in GUI units. */
	public float getWidth() {
		return (getContentWidth() + PADDING * 2.0F) * getScale();
	}

	public float getHeight() {
		return (getContentHeight() + PADDING * 2.0F) * getScale();
	}

	public Anchor getAnchor() {
		return this.anchor;
	}

	/** Left edge on a screen this wide, kept fully on screen. */
	public float getX(float screenW) {
		float w = getWidth();
		float x;
		if (this.anchor.column == 0) {
			x = this.offsetX * screenW;
		} else if (this.anchor.column == 1) {
			x = screenW / 2.0F + this.offsetX * screenW - w / 2.0F;
		} else {
			x = screenW - this.offsetX * screenW - w;
		}
		return Math.max(0.0F, Math.min(x, screenW - w));
	}

	/** Top edge on a screen this tall, kept fully on screen. */
	public float getY(float screenH) {
		float h = getHeight();
		float y;
		if (this.anchor.row == 0) {
			y = this.offsetY * screenH;
		} else if (this.anchor.row == 1) {
			y = screenH / 2.0F + this.offsetY * screenH - h / 2.0F;
		} else {
			y = screenH - this.offsetY * screenH - h;
		}
		return Math.max(0.0F, Math.min(y, screenH - h));
	}

	/**
	 * Moves the element so its top-left corner is at (x, y). Picks the nearest
	 * anchor and remembers the distance from it as a fraction of the screen.
	 */
	public void setPosition(float x, float y, float screenW, float screenH) {
		if (screenW <= 0.0F || screenH <= 0.0F) {
			return;
		}
		float w = getWidth(), h = getHeight();
		x = Math.max(0.0F, Math.min(x, screenW - w));
		y = Math.max(0.0F, Math.min(y, screenH - h));
		this.anchor = Anchor.pick(x + w / 2.0F, y + h / 2.0F, screenW, screenH);
		if (this.anchor.column == 0) {
			this.offsetX = x / screenW;
		} else if (this.anchor.column == 1) {
			this.offsetX = (x + w / 2.0F - screenW / 2.0F) / screenW;
		} else {
			this.offsetX = (screenW - x - w) / screenW;
		}
		if (this.anchor.row == 0) {
			this.offsetY = y / screenH;
		} else if (this.anchor.row == 1) {
			this.offsetY = (y + h / 2.0F - screenH / 2.0F) / screenH;
		} else {
			this.offsetY = (screenH - y - h) / screenH;
		}
		ConfigManager.markDirty();
	}

	/** Puts the element back where it started (scale is a setting, so it's reset separately). */
	public void resetPosition() {
		this.anchor = this.defaultAnchor;
		this.offsetX = this.defaultX;
		this.offsetY = this.defaultY;
	}

	@Override
	public void resetToDefaults() {
		super.resetToDefaults();
		resetPosition();
	}

	@Override
	public void resetSettings() {
		super.resetSettings();
		resetPosition();
		ConfigManager.markDirty();
	}

	// ---------------- drawing ----------------

	/**
	 * Draws the element where it belongs on a screen of this size: background
	 * first, then the content, scaled. Snapped to whole screen pixels so text
	 * stays sharp. Problems in a module's drawing are logged, not crashes.
	 */
	public final void render(float screenW, float screenH, boolean editing) {
		float s = getScale();
		float x = getX(screenW), y = getY(screenH);
		float pixels = this.mc.displayWidth / Math.max(1.0F, screenW); // screen pixels per GUI unit
		x = Math.round(x * pixels) / pixels;
		y = Math.round(y * pixels) / pixels;

		GlStateManager.pushMatrix();
		try {
			GlStateManager.translate(x, y, 0.0F);
			GlStateManager.scale(s, s, 1.0F);
			if (this.background.isOn()) {
				drawBackground(getContentWidth() + PADDING * 2.0F, getContentHeight() + PADDING * 2.0F);
			}
			GlStateManager.translate(PADDING, PADDING, 0.0F);
			renderContent(editing);
		} catch (Throwable t) {
			AeroClient.LOGGER.error("HUD module " + getName() + " failed to draw");
			AeroClient.LOGGER.error(t);
		} finally {
			GlStateManager.popMatrix();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	// ---------------- saving ----------------

	@Override
	public void saveExtra(JSONObject json) {
		json.put("anchor", this.anchor.name());
		json.put("x", Math.round(this.offsetX * 100000.0D) / 100000.0D);
		json.put("y", Math.round(this.offsetY * 100000.0D) / 100000.0D);
	}

	@Override
	public void loadExtra(JSONObject json) {
		this.anchor = Anchor.fromName(json.optString("anchor", ""), this.defaultAnchor);
		this.offsetX = cleanOffset(json.optDouble("x", this.defaultX), this.defaultX);
		this.offsetY = cleanOffset(json.optDouble("y", this.defaultY), this.defaultY);
	}

	/** Keeps a saved offset sane: a number between -1 and 1 (broken values use the default). */
	private static float cleanOffset(double value, float fallback) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return fallback;
		}
		return (float) Math.max(-1.0D, Math.min(1.0D, value));
	}
}
