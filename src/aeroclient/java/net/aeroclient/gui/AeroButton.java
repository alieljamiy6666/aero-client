package net.aeroclient.gui;

import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

/**
 * Aero Client's rounded button: a dark glass pill that smoothly lights up
 * when the mouse is over it (it grows a little, the border turns blue and a
 * soft blue glow appears behind it).
 *
 * It works exactly like a normal Minecraft button (same id, position, size
 * and click handling), so a screen can swap {@code new GuiButton(...)} for
 * {@code new AeroButton(...)} and nothing else changes.
 */
public class AeroButton extends GuiButton {

	/** Plain text button. */
	public static final int ICON_NONE = 0;
	/** Round button with a globe, for picking the language. */
	public static final int ICON_GLOBE = 1;

	private final int icon;
	private final Animation.Fader hover = new Animation.Fader(160.0F);
	private final Animation.Fader press = new Animation.Fader(70.0F);
	private boolean wasHovered = false;
	private long hoverStart = 0L;
	private static final long SWEEP_MS = 520L;

	public AeroButton(int id, int x, int y, String text) {
		this(id, x, y, 200, 20, text, ICON_NONE);
	}

	public AeroButton(int id, int x, int y, int width, int height, String text) {
		this(id, x, y, width, height, text, ICON_NONE);
	}

	public AeroButton(int id, int x, int y, int width, int height, String text, int icon) {
		super(id, x, y, width, height, text);
		this.icon = icon;
	}

	/** Draws the button. (Minecraft 1.12's "drawButton", with its original scrambled name.) */
	@Override
	public void func_191745_a(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!this.visible) {
			return;
		}
		this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width
				&& mouseY < this.yPosition + this.height;
		boolean active = this.hovered && this.enabled;
		float h = this.hover.update(active);
		float p = this.press.update(active && Mouse.isButtonDown(0));
		if (active && !this.wasHovered) {
			this.hoverStart = Minecraft.getSystemTime(); // start the light sweep
		}
		this.wasHovered = active;

		float x = this.xPosition, y = this.yPosition, w = this.width, ht = this.height;
		float radius = ht / 2.0F;

		// Grow a little from the center while hovered, and squish while pressed.
		float scale = 1.0F + 0.04F * h - 0.05F * p;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w / 2.0F, y + ht / 2.0F, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.translate(-(x + w / 2.0F), -(y + ht / 2.0F), 0.0F);

		// Soft shadow under every button, plus a blue glow on hover.
		RenderUtil.shadow(x, y + 1.0F, w, ht, radius, 5.0F, Theme.withAlpha(0xFF000000, 0.45F));
		if (h > 0.01F) {
			RenderUtil.glow(x, y, w, ht, radius, 11.0F, Theme.withAlpha(Theme.accent, 0.8F * h));
		}

		// Border (lighter at the top, like light hitting glass), then the body inside it.
		int borderTop = this.enabled ? Theme.mix(0xFF3B4B61, Theme.accentLight, h) : Theme.withAlpha(Theme.border, 0.6F);
		int borderBottom = this.enabled ? Theme.mix(0xFF1E2733, Theme.accent, h) : Theme.withAlpha(Theme.border, 0.6F);
		RenderUtil.roundedRectGradient(x, y, w, ht, radius, borderTop, borderBottom);
		int top = Theme.mix(0xFF1C2430, 0xFF22324A, h);
		int bottom = Theme.mix(0xFF12171F, 0xFF162132, h);
		if (!this.enabled) {
			top = 0xFF151A21;
			bottom = 0xFF11151B;
		}
		RenderUtil.roundedRectGradient(x + 1.0F, y + 1.0F, w - 2.0F, ht - 2.0F, radius - 1.0F, top, bottom);

		// A thin light line along the top makes it look like glass.
		RenderUtil.roundedRect(x + radius * 0.6F, y + 1.5F, w - radius * 1.2F, 1.0F, 0.5F,
				Theme.withAlpha(0xFFFFFFFF, 0.06F + 0.08F * h));

		// When the mouse first arrives, a streak of light sweeps across once.
		long sweep = Minecraft.getSystemTime() - this.hoverStart;
		if (active && sweep < SWEEP_MS && w > ht * 2.0F) {
			float k = Animation.easeInOut((float) sweep / SWEEP_MS);
			float band = Math.min(28.0F, w * 0.18F);
			float left = x + radius - band, right = x + w - radius;
			float bx = left + (right - left) * k;
			float b0 = Math.max(x + radius * 0.5F, bx), b1 = Math.min(x + w - radius * 0.5F, bx + band);
			if (b1 > b0) {
				float mid = bx + band / 2.0F;
				float fade = 1.0F - Math.abs(k - 0.5F) * 1.2F;
				int bright = Theme.withAlpha(0xFFFFFFFF, 0.28F * fade);
				int clear = 0x00FFFFFF;
				RenderUtil.gradient(Math.max(b0, bx), y + 2.0F, Math.min(b1, mid), y + ht - 2.0F, clear, bright, bright, clear,
						true);
				RenderUtil.gradient(Math.max(b0, mid), y + 2.0F, Math.min(b1, bx + band), y + ht - 2.0F, bright, clear, clear,
						bright, true);
			}
		}

		if (this.icon == ICON_GLOBE) {
			drawGlobe(x + w / 2.0F, y + ht / 2.0F, Math.min(w, ht) * 0.28F, h);
		} else {
			drawLabel(mc.fontRendererObj, h);
		}

		GlStateManager.popMatrix();
	}

	private void drawLabel(FontRenderer font, float h) {
		String text = this.displayString;
		int maxWidth = this.width - 12;
		if (font.getStringWidth(text) > maxWidth) {
			text = font.trimStringToWidth(text, maxWidth - font.getStringWidth("...")) + "...";
		}
		int color = !this.enabled ? 0xFF6B7280 : Theme.mix(Theme.text, 0xFFFFFFFF, h);
		float tx = this.xPosition + (this.width - font.getStringWidth(text)) / 2.0F;
		float ty = this.yPosition + (this.height - 8) / 2.0F;
		font.drawStringWithShadow(text, tx, ty, color);
	}

	/** A simple globe: a ring, a narrower ring for the longitude, and a line for the equator. */
	private void drawGlobe(float cx, float cy, float r, float h) {
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
