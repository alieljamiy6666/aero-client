package net.aeroclient.gui.clickgui;

import java.util.List;

import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.internal.EnumCursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.SoundEvents;

/**
 * Small drawing helpers shared by the Right Shift menu: on/off switches,
 * see-through colors, text that fades, tooltips and click sounds.
 *
 * Almost everything takes an {@code alpha} from 0 to 1, so the whole menu can
 * fade in and out smoothly when it opens and closes.
 */
public final class Widgets {

	public static final float SWITCH_W = 18.0F, SWITCH_H = 10.0F;
	private static final int OFF_TRACK = 0xFF364152;

	private Widgets() {
	}

	/** The color with its see-through-ness multiplied by {@code alpha}. */
	public static int fade(int color, float alpha) {
		int a = Math.round(((color >>> 24) & 255) * Math.max(0.0F, Math.min(1.0F, alpha)));
		return (a << 24) | (color & 0x00FFFFFF);
	}

	/**
	 * Text that can fade. Minecraft treats almost-invisible text colors as solid,
	 * so text that's nearly faded out is skipped instead.
	 */
	public static void text(FontRenderer font, String s, float x, float y, int color, float alpha) {
		int c = fade(color, alpha);
		if ((c >>> 24) >= 10) {
			font.drawStringWithShadow(s, x, y, c);
		}
	}

	public static void textCentered(FontRenderer font, String s, float centerX, float y, int color, float alpha) {
		text(font, s, centerX - font.getStringWidth(s) / 2.0F, y, color, alpha);
	}

	public static void textRight(FontRenderer font, String s, float rightX, float y, int color, float alpha) {
		text(font, s, rightX - font.getStringWidth(s), y, color, alpha);
	}

	public static boolean inside(float mx, float my, float x, float y, float w, float h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	/** Shows the pointing-hand mouse cursor this frame (in browsers that allow it). */
	public static void handCursor() {
		Mouse.showCursor(EnumCursorType.HAND);
	}

	public static void textCursor() {
		Mouse.showCursor(EnumCursorType.TEXT);
	}

	/** The menu's soft click. A higher {@code pitch} for "on", lower for "off". */
	public static void click(float pitch) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.getSoundHandler() != null) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, pitch));
		}
	}

	/**
	 * An on/off switch. {@code on} goes from 0 (off) to 1 (on) and can be in
	 * between while it slides; {@code hover} makes it a little brighter.
	 */
	public static void drawSwitch(float x, float y, float on, float hover, float alpha) {
		int track = Theme.mix(OFF_TRACK, Theme.accent, on);
		track = Theme.mix(track, 0xFFFFFFFF, 0.08F * hover);
		if (on > 0.01F) {
			RenderUtil.glow(x, y, SWITCH_W, SWITCH_H, SWITCH_H / 2.0F, 3.0F, fade(Theme.accent, 0.35F * on * alpha));
		}
		RenderUtil.pill(x, y, SWITCH_W, SWITCH_H, fade(track, alpha));
		float knobX = x + 5.0F + on * (SWITCH_W - 10.0F);
		RenderUtil.circle(knobX, y + SWITCH_H / 2.0F, 3.5F, fade(0xFFF8FAFC, alpha));
	}

	/**
	 * A rounded panel with a 1-unit border, a soft shadow and (optionally) a
	 * glow. While it's fully visible, the hidden middles of the border, shadow
	 * and glow are skipped, so big panels cost much less to draw on slow
	 * graphics chips. While it's fading, everything is drawn whole so it still
	 * looks right when see-through.
	 */
	public static void panel(float x, float y, float w, float h, float r, int border, int top, int bottom, float shadow,
			int glow, float alpha) {
		boolean solid = alpha >= 0.99F;
		if ((glow >>> 24) > 0) {
			if (solid) {
				RenderUtil.glowOuter(x, y, w, h, r, 6.0F, fade(glow, alpha));
			} else {
				RenderUtil.glow(x, y, w, h, r, 6.0F, fade(glow, alpha));
			}
		}
		if (shadow > 0.0F) {
			int shade = fade(0x88000000, alpha);
			if (solid) {
				RenderUtil.shadowOuter(x, y + 2, w, h, r, shadow, shade);
			} else {
				RenderUtil.shadow(x, y + 2, w, h, r, shadow, shade);
			}
		}
		if (solid) {
			RenderUtil.roundedRectEdge(x, y, w, h, r, fade(border, alpha));
		} else {
			RenderUtil.roundedRect(x, y, w, h, r, fade(border, alpha));
		}
		RenderUtil.roundedRectGradient(x + 1, y + 1, w - 2, h - 2, r - 1, fade(top, alpha), fade(bottom, alpha));
	}

	/** A dark box with a description, drawn next to the mouse and kept on screen. */
	public static void tooltip(FontRenderer font, String text, int mouseX, int mouseY, int screenW, int screenH, boolean below,
			float alpha) {
		if (text == null || text.isEmpty() || alpha <= 0.02F) {
			return;
		}
		List<String> lines = font.listFormattedStringToWidth(text, 170);
		int w = 0;
		for (int i = 0; i < lines.size(); i++) {
			w = Math.max(w, font.getStringWidth(lines.get(i)));
		}
		int h = lines.size() * 10 - 2;
		float x = mouseX + 10, y = mouseY - 4;
		if (below) {
			x = mouseX - (w + 10) / 2.0F;
			y = mouseY + 14;
		}
		if (x + w + 12 > screenW) {
			x = below ? screenW - w - 14 : mouseX - w - 14;
		}
		y = Math.max(4, Math.min(y, screenH - h - 10));
		RenderUtil.shadow(x, y, w + 10, h + 8, 5, 5, fade(0x99000000, alpha));
		RenderUtil.roundedRect(x, y, w + 10, h + 8, 5, fade(Theme.border, alpha));
		RenderUtil.roundedRect(x + 1, y + 1, w + 8, h + 6, 4, fade(0xF50B0F15, alpha));
		for (int i = 0; i < lines.size(); i++) {
			text(font, lines.get(i), x + 5, y + 4 + i * 10, Theme.text, alpha);
		}
	}
}
