package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.HudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.ClickTracker;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/**
 * Shows your movement keys, mouse buttons and space bar, lighting up in the
 * accent color while they're held:
 *
 * <pre>
 *        [ W ]
 *  [ A ] [ S ] [ D ]
 *  [  LMB  ] [  RMB  ]
 *  [      ----      ]
 * </pre>
 *
 * Keys are read straight from the keyboard and mouse every frame, so they
 * react instantly (not once per game tick). The letters follow your own key
 * bindings, so a moved key shows its new letter. Under the mouse buttons it
 * shows your clicks per second.
 */
public class KeystrokesModule extends HudModule {

	private static final int W = 0, A = 1, S = 2, D = 3, LMB = 4, RMB = 5, SPACE = 6, KEYS = 7;
	private static final float PRESS_MS = 50.0F, RELEASE_MS = 160.0F; // light up fast, fade out a bit slower

	private final NumberSetting size = add(new NumberSetting("Key Size", "How big each key is.", 22, 14, 32, 1));
	private final NumberSetting gap = add(new NumberSetting("Gap", "Space between the keys.", 2, 0, 6, 1));
	private final ModeSetting shape = add(
			new ModeSetting("Key Shape", "Rounded or square keys.", "Rounded", "Rounded", "Square"));
	private final BooleanSetting showMouse = add(
			new BooleanSetting("Show Mouse Buttons", "Show the left and right mouse buttons.", true));
	private final BooleanSetting showCps = add(
			new BooleanSetting("Show CPS", "Show clicks per second under the mouse buttons.", true));
	private final BooleanSetting showSpace = add(new BooleanSetting("Show Space Bar", "Show the jump key.", true));
	private final ColorSetting keyColor = add(
			new ColorSetting("Key Color", "The keys' color when not pressed (lower alpha = more see-through).",
					0x99101419, true));
	private final BooleanSetting useAccent = add(
			new BooleanSetting("Use Accent Color", "Pressed keys use your Aero accent color.", true));
	private final ColorSetting pressedColor = add(
			new ColorSetting("Pressed Color", "The keys' color while pressed.", 0xFFFFFFFF, true));
	private final ColorSetting textColor = add(
			new ColorSetting("Text Color", "The letters' color.", 0xFFFFFFFF, false));
	private final ColorSetting pressedTextColor = add(
			new ColorSetting("Pressed Text Color", "The letters' color while pressed.", 0xFF0E1116, false));
	private final BooleanSetting textShadow = add(
			new BooleanSetting("Text Shadow", "Draw a soft shadow under the letters.", false));

	private final float[] glow = new float[KEYS]; // 0 = up, 1 = fully lit
	private long lastFrame = -1L;

	// key names follow the player's key bindings; rebuilt only when a binding changes
	private final int[] boundCodes = new int[4];
	private final String[] labels = { "W", "A", "S", "D" };
	private int shownLeft = -1, shownRight = -1;
	private String leftCpsText = "0 CPS", rightCpsText = "0 CPS";

	public KeystrokesModule() {
		super("Keystrokes", "Shows W, A, S, D, Space and mouse clicks lighting up as you press them, with CPS.", 0,
				Anchor.MIDDLE_LEFT, 0.008F, 0.16F, false);
		this.pressedColor.visibleWhen(() -> !this.useAccent.isOn());
		this.showCps.visibleWhen(this.showMouse::isOn);
		for (int i = 0; i < 4; i++) {
			this.boundCodes[i] = Integer.MIN_VALUE;
		}
	}

	@Override
	protected float getPadding() {
		return this.background.isOn() ? 3.0F : 0.0F;
	}

	// ---------------- layout ----------------

	private float key() {
		return this.size.getFloat();
	}

	private float gapSize() {
		return this.gap.getFloat();
	}

	private float spaceHeight() {
		return Math.max(8.0F, Math.round(key() * 0.5F));
	}

	@Override
	protected float getContentWidth() {
		return key() * 3.0F + gapSize() * 2.0F;
	}

	@Override
	protected float getContentHeight() {
		float h = key() * 2.0F + gapSize();
		if (this.showMouse.isOn()) {
			h += gapSize() + key();
		}
		if (this.showSpace.isOn()) {
			h += gapSize() + spaceHeight();
		}
		return h;
	}

	// ---------------- which keys are down ----------------

	/** True while a key binding's key or mouse button is held (read live, every frame). */
	private static boolean isDown(int code) {
		if (code < 0) {
			return Mouse.isButtonDown(code + 100); // mouse buttons are stored as -100, -99, ...
		}
		return code > 0 && code < 256 && Keyboard.isKeyDown(code);
	}

	private void updateLabels() {
		GameSettings gs = this.mc.gameSettings;
		updateLabel(0, gs.keyBindForward);
		updateLabel(1, gs.keyBindLeft);
		updateLabel(2, gs.keyBindBack);
		updateLabel(3, gs.keyBindRight);
	}

	private void updateLabel(int i, KeyBinding binding) {
		int code = binding.getKeyCode();
		if (code != this.boundCodes[i]) {
			this.boundCodes[i] = code;
			String name = GameSettings.getKeyDisplayString(code);
			this.labels[i] = name == null || name.isEmpty() ? "?" : (name.length() > 4 ? name.substring(0, 4) : name);
		}
	}

	/** Moves each key's light toward on or off, timed by the real time between frames. */
	private void updateGlow() {
		long now = Minecraft.getSystemTime();
		float dt = this.lastFrame < 0L ? 0.0F : Math.min(100.0F, now - this.lastFrame);
		this.lastFrame = now;
		boolean live = this.mc.currentScreen == null && this.mc.world != null; // not while typing in chat
		GameSettings gs = this.mc.gameSettings;
		set(W, live && isDown(gs.keyBindForward.getKeyCode()), dt);
		set(A, live && isDown(gs.keyBindLeft.getKeyCode()), dt);
		set(S, live && isDown(gs.keyBindBack.getKeyCode()), dt);
		set(D, live && isDown(gs.keyBindRight.getKeyCode()), dt);
		set(LMB, live && Mouse.isButtonDown(0), dt);
		set(RMB, live && Mouse.isButtonDown(1), dt);
		set(SPACE, live && isDown(gs.keyBindJump.getKeyCode()), dt);
	}

	private void set(int key, boolean down, float dt) {
		float g = this.glow[key];
		g += down ? dt / PRESS_MS : -dt / RELEASE_MS;
		this.glow[key] = g < 0.0F ? 0.0F : (g > 1.0F ? 1.0F : g);
		if (down && dt == 0.0F) {
			this.glow[key] = 1.0F;
		}
	}

	private void updateCps() {
		int left = ClickTracker.getLeftCps(), right = ClickTracker.getRightCps();
		if (left != this.shownLeft) {
			this.shownLeft = left;
			this.leftCpsText = left + " CPS";
		}
		if (right != this.shownRight) {
			this.shownRight = right;
			this.rightCpsText = right + " CPS";
		}
	}

	// ---------------- drawing ----------------

	@Override
	protected void renderContent(boolean editing) {
		updateLabels();
		updateGlow();
		if (this.showMouse.isOn() && this.showCps.isOn()) {
			updateCps();
		}
		FontRenderer font = this.mc.fontRendererObj;
		float k = key(), g = gapSize();
		float y = 0.0F;

		drawKey(font, W, this.labels[0], k + g, y, k, k);
		y += k + g;
		drawKey(font, A, this.labels[1], 0.0F, y, k, k);
		drawKey(font, S, this.labels[2], k + g, y, k, k);
		drawKey(font, D, this.labels[3], (k + g) * 2.0F, y, k, k);
		y += k + g;
		float fullW = k * 3.0F + g * 2.0F;
		if (this.showMouse.isOn()) {
			float half = (fullW - g) / 2.0F;
			drawMouseKey(font, LMB, "LMB", this.leftCpsText, 0.0F, y, half, k);
			drawMouseKey(font, RMB, "RMB", this.rightCpsText, half + g, y, half, k);
			y += k + g;
		}
		if (this.showSpace.isOn()) {
			float sh = spaceHeight();
			drawBox(SPACE, 0.0F, y, fullW, sh);
			float lineW = fullW * 0.35F;
			int c = textColorFor(SPACE);
			RenderUtil.pill((fullW - lineW) / 2.0F, y + sh / 2.0F - 1.0F, lineW, 2.0F, c);
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private int pressedFill() {
		return this.useAccent.isOn() ? Theme.accent : this.pressedColor.getColor();
	}

	private int textColorFor(int key) {
		return Theme.mix(this.textColor.getColor(), this.pressedTextColor.getColor(), this.glow[key]);
	}

	private void drawBox(int key, float x, float y, float w, float h) {
		int fill = Theme.mix(this.keyColor.getColor(), pressedFill(), this.glow[key]);
		if (this.shape.is("Rounded")) {
			RenderUtil.roundedRect(x, y, w, h, Math.min(4.0F, Math.min(w, h) * 0.25F), fill);
		} else {
			RenderUtil.rect(x, y, x + w, y + h, fill);
		}
	}

	private void drawKey(FontRenderer font, int key, String label, float x, float y, float w, float h) {
		drawBox(key, x, y, w, h);
		float tx = x + (w - font.getStringWidth(label)) / 2.0F;
		font.drawString(label, tx, y + (h - 7.0F) / 2.0F, textColorFor(key), this.textShadow.isOn());
	}

	private void drawMouseKey(FontRenderer font, int key, String label, String cps, float x, float y, float w,
			float h) {
		drawBox(key, x, y, w, h);
		int color = textColorFor(key);
		boolean shadow = this.textShadow.isOn();
		if (!this.showCps.isOn()) {
			font.drawString(label, x + (w - font.getStringWidth(label)) / 2.0F, y + (h - 7.0F) / 2.0F, color, shadow);
			return;
		}
		// label on top, smaller CPS underneath
		float small = 0.65F;
		float labelY = y + (h - 7.0F - 1.0F - 7.0F * small) / 2.0F;
		font.drawString(label, x + (w - font.getStringWidth(label)) / 2.0F, labelY, color, shadow);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w / 2.0F, labelY + 8.5F, 0.0F);
		GlStateManager.scale(small, small, 1.0F);
		font.drawString(cps, -font.getStringWidth(cps) / 2.0F, 0.0F, color, shadow);
		GlStateManager.popMatrix();
	}
}
