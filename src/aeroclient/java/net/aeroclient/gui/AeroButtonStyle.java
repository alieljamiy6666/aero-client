package net.aeroclient.gui;

import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Draws every Minecraft button in Aero Client's rounded style: a dark glass
 * pill that smoothly lights up when the mouse is over it (it grows a little,
 * the border turns blue, a soft glow appears and a streak of light sweeps
 * across once), and squishes slightly when pressed. Sliders get a blue fill
 * and a round handle instead.
 *
 * Minecraft's own button class calls {@link #begin} and {@link #finish}, so
 * every screen that uses normal buttons gets this look automatically.
 */
public final class AeroButtonStyle {

	/** Turn this off to get the vanilla Minecraft buttons back. */
	public static boolean enabled = true;

	/** Buttons that are sliders implement this, so they don't grow or sweep. */
	public interface Slider {
	}

	private static final long SWEEP_MS = 520L;

	/** Per-button animation state, kept in {@code GuiButton.aeroState}. */
	private static final class State {
		final Animation.Fader hover = new Animation.Fader(160.0F);
		final Animation.Fader press = new Animation.Fader(70.0F);
		boolean wasHovered = false;
		long hoverStart = 0L;
		float h = 0.0F;
	}

	private AeroButtonStyle() {
	}

	private static State state(GuiButton b) {
		if (!(b.aeroState instanceof State)) {
			b.aeroState = new State();
		}
		return (State) b.aeroState;
	}

	/**
	 * Draws the button's body (everything except its text) and returns whether
	 * the mouse is over it. Always follow with {@link #finish}.
	 */
	public static boolean begin(GuiButton b, Minecraft mc, int mouseX, int mouseY, int height) {
		State s = state(b);
		boolean slider = b instanceof Slider;
		boolean hovered = mouseX >= b.xPosition && mouseY >= b.yPosition && mouseX < b.xPosition + b.width
				&& mouseY < b.yPosition + height;
		boolean active = hovered && b.enabled;
		float h = s.hover.update(active);
		float p = s.press.update(active && !slider && Mouse.isButtonDown(0));
		if (active && !s.wasHovered) {
			s.hoverStart = Minecraft.getSystemTime();
		}
		s.wasHovered = active;
		s.h = h;

		float x = b.xPosition, y = b.yPosition, w = b.width, ht = height;
		float radius = ht / 2.0F;

		GlStateManager.pushMatrix();
		if (!slider) { // grow a little while hovered, squish while pressed
			float scale = 1.0F + 0.04F * h - 0.05F * p;
			GlStateManager.translate(x + w / 2.0F, y + ht / 2.0F, 0.0F);
			GlStateManager.scale(scale, scale, 1.0F);
			GlStateManager.translate(-(x + w / 2.0F), -(y + ht / 2.0F), 0.0F);
		}

		RenderUtil.shadow(x, y + 1.0F, w, ht, radius, 5.0F, Theme.withAlpha(0xFF000000, 0.45F));
		if (h > 0.01F) {
			RenderUtil.glow(x, y, w, ht, radius, 11.0F, Theme.withAlpha(Theme.accent, (slider ? 0.45F : 0.8F) * h));
		}

		int borderTop = b.enabled ? Theme.mix(0xFF3B4B61, Theme.accentLight, h) : Theme.withAlpha(Theme.border, 0.6F);
		int borderBottom = b.enabled ? Theme.mix(0xFF1E2733, Theme.accent, h) : Theme.withAlpha(Theme.border, 0.6F);
		RenderUtil.roundedRectGradient(x, y, w, ht, radius, borderTop, borderBottom);
		int top = b.enabled ? Theme.mix(0xFF1C2430, 0xFF22324A, h) : 0xFF151A21;
		int bottom = b.enabled ? Theme.mix(0xFF12171F, 0xFF162132, h) : 0xFF11151B;
		RenderUtil.roundedRectGradient(x + 1.0F, y + 1.0F, w - 2.0F, ht - 2.0F, radius - 1.0F, top, bottom);
		RenderUtil.roundedRect(x + radius * 0.6F, y + 1.5F, w - radius * 1.2F, 1.0F, 0.5F,
				Theme.withAlpha(0xFFFFFFFF, 0.06F + 0.08F * h));

		long sweep = Minecraft.getSystemTime() - s.hoverStart;
		if (active && !slider && sweep < SWEEP_MS && w > ht * 2.0F) {
			drawSweep(x, y, w, ht, radius, Animation.easeInOut((float) sweep / SWEEP_MS));
		}
		return hovered;
	}

	/** Draws the button's text (or icon) and undoes the grow effect from {@link #begin}. */
	public static void finish(GuiButton b, Minecraft mc, int height) {
		State s = state(b);
		float x = b.xPosition, y = b.yPosition, w = b.width;
		int icon = b instanceof AeroButton ? ((AeroButton) b).getIcon() : AeroButton.ICON_NONE;
		if (icon == AeroButton.ICON_GLOBE) {
			drawGlobe(x + w / 2.0F, y + height / 2.0F, Math.min(w, height) * 0.28F, s.h);
		} else {
			int color = !b.enabled ? 0xFF6B7280 : Theme.mix(Theme.text, 0xFFFFFFFF, s.h);
			TextUtil.drawCenteredTrimmed(mc.fontRendererObj, b.displayString, x + w / 2.0F,
					y + (height - 8) / 2.0F, b.width - 12, color);
		}
		GlStateManager.popMatrix();
	}

	/** Ends {@link #begin} without drawing the text, for buttons that draw their own (e.g. with icons). */
	public static void finishWithoutLabel() {
		GlStateManager.popMatrix();
	}

	/**
	 * A slider's value: a blue fill up to the handle, then a round handle.
	 * {@code value} goes from 0 (left) to 1 (right).
	 */
	public static void drawSliderKnob(GuiButton b, float value, int height) {
		value = Animation.clamp01(value);
		float x = b.xPosition, y = b.yPosition, w = b.width, ht = height;
		float radius = ht / 2.0F;
		float cx = x + radius + value * (w - radius * 2.0F);
		float fillW = cx - x;
		if (fillW > 2.0F) {
			RenderUtil.roundedRectGradient(x + 1.0F, y + 1.0F, fillW, ht - 2.0F, radius - 1.0F,
					Theme.withAlpha(Theme.accent, 0.42F), Theme.withAlpha(Theme.accent, 0.22F));
		}
		float knob = ht / 2.0F - 3.0F;
		RenderUtil.glow(cx - knob, y + ht / 2.0F - knob, knob * 2.0F, knob * 2.0F, knob, 4.0F,
				Theme.withAlpha(Theme.accent, 0.6F));
		// Dark middle with a bright ring, so the white slider text stays readable on top of it.
		RenderUtil.circle(cx, y + ht / 2.0F, knob, Theme.accentLight);
		RenderUtil.circle(cx, y + ht / 2.0F, knob - 1.5F, 0xFF101722);
		RenderUtil.circle(cx, y + ht / 2.0F, 1.5F, Theme.accentLight);
	}

	/** Text boxes: a rounded dark box whose border glows blue while you're typing in it. */
	public static void drawTextFieldBox(Object field, int x, int y, int w, int h, boolean focused) {
		float f = textFieldFocus(field, focused);
		float r = 4.0F;
		if (f > 0.01F) {
			RenderUtil.glow(x - 1, y - 1, w + 2, h + 2, r, 6.0F, Theme.withAlpha(Theme.accent, 0.45F * f));
		}
		RenderUtil.roundedRect(x - 1, y - 1, w + 2, h + 2, r, Theme.mix(0xFF2B3646, Theme.accent, f));
		RenderUtil.roundedRectGradient(x, y, w, h, r - 1.0F, 0xFF0B1018, 0xFF101722);
	}

	private static final java.util.IdentityHashMap<Object, Animation.Fader> FIELD_FADERS = new java.util.IdentityHashMap<>();

	private static float textFieldFocus(Object field, boolean focused) {
		if (FIELD_FADERS.size() > 64) { // old screens' boxes: start fresh instead of growing forever
			FIELD_FADERS.clear();
		}
		Animation.Fader fader = FIELD_FADERS.get(field);
		if (fader == null) {
			fader = new Animation.Fader(150.0F);
			FIELD_FADERS.put(field, fader);
		}
		return fader.update(focused);
	}

	private static void drawSweep(float x, float y, float w, float ht, float radius, float k) {
		float band = Math.min(28.0F, w * 0.18F);
		float left = x + radius - band, right = x + w - radius;
		float bx = left + (right - left) * k;
		float b0 = Math.max(x + radius * 0.5F, bx), b1 = Math.min(x + w - radius * 0.5F, bx + band);
		if (b1 <= b0) {
			return;
		}
		float mid = bx + band / 2.0F;
		float fade = 1.0F - Math.abs(k - 0.5F) * 1.2F;
		int bright = Theme.withAlpha(0xFFFFFFFF, 0.28F * fade);
		int clear = 0x00FFFFFF;
		RenderUtil.gradient(Math.max(b0, bx), y + 2.0F, Math.min(b1, mid), y + ht - 2.0F, clear, bright, bright, clear, true);
		RenderUtil.gradient(Math.max(b0, mid), y + 2.0F, Math.min(b1, bx + band), y + ht - 2.0F, bright, clear, clear,
				bright, true);
	}

	/** A simple globe: a ring, a narrower ring for the longitude, and the equator. */
	private static void drawGlobe(float cx, float cy, float r, float h) {
		int line = Theme.mix(Theme.text, Theme.accentLight, h);
		int fill = Theme.mix(0xFF1A2230, 0xFF1D2B3F, h);
		RenderUtil.circle(cx, cy, r, line);
		RenderUtil.circle(cx, cy, r - 1.0F, fill);
		RenderUtil.roundedRect(cx - r * 0.45F, cy - r, r * 0.9F, r * 2.0F, r * 0.45F, line);
		RenderUtil.roundedRect(cx - r * 0.45F + 1.0F, cy - r + 1.0F, r * 0.9F - 2.0F, r * 2.0F - 2.0F,
				r * 0.45F - 1.0F, fill);
		RenderUtil.rect(cx - r + 0.5F, cy - 0.5F, cx + r - 0.5F, cy + 0.5F, line);
		RenderUtil.rect(cx - 0.5F, cy - r + 0.5F, cx + 0.5F, cy + r - 0.5F, line);
	}
}
