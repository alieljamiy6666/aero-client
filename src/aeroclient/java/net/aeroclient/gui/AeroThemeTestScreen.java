package net.aeroclient.gui;

import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * A test screen for the theme system (Step 4): every color token, rounded
 * panels, pills, circles, glows, trimmed text and animated buttons. The
 * "Accent" button cycles through the accent presets so you can see the whole
 * client change color. Open it by pressing T on the title screen.
 */
public class AeroThemeTestScreen extends GuiScreen {

	private static final int ID_ACCENT = 1, ID_DONE = 2, ID_DISABLED = 3;

	private final GuiScreen parent;
	private final Animation.Fader appear = new Animation.Fader(350.0F);

	public AeroThemeTestScreen(GuiScreen parent) {
		this.parent = parent;
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		int cx = this.width / 2;
		int y = this.height - 30;
		this.buttonList.add(new GuiButton(ID_ACCENT, cx - 154, y, 100, 20, accentLabel()));
		GuiButton disabled = new GuiButton(ID_DISABLED, cx - 50, y, 100, 20, "Disabled");
		disabled.enabled = false;
		this.buttonList.add(disabled);
		this.buttonList.add(new GuiButton(ID_DONE, cx + 54, y, 100, 20, "Done"));
	}

	private static String accentLabel() {
		int i = Theme.accentPresetIndex();
		return "Accent: " + (i < 0 ? "Custom" : Theme.ACCENT_NAMES[i]);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == ID_ACCENT) {
			int next = (Theme.accentPresetIndex() + 1) % Theme.ACCENT_COLORS.length;
			Theme.setAccent(Theme.ACCENT_COLORS[next]);
			button.displayString = accentLabel();
		} else if (button.id == ID_DONE) {
			this.mc.displayGuiScreen(this.parent);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		float a = this.appear.update(true); // the panel slides up and fades in when the screen opens
		int cx = this.width / 2;
		float panelW = Math.min(320, this.width - 20), panelH = Math.min(152, this.height - 50);
		float px = cx - panelW / 2.0F, py = 12 + (1.0F - a) * 20.0F;

		RenderUtil.shadow(px, py + 2, panelW, panelH, 8, 10, Theme.withAlpha(0xFF000000, 0.5F * a));
		RenderUtil.roundedRect(px, py, panelW, panelH, 8, Theme.withAlpha(Theme.border, a));
		RenderUtil.roundedRect(px + 1, py + 1, panelW - 2, panelH - 2, 7, Theme.withAlpha(Theme.background, 0.96F * a));

		TextUtil.drawCentered(this.fontRendererObj, "Aero Client Theme", cx, py + 8, Theme.accentLight);

		// Color tokens
		String[] names = { "Background", "Panel", "Panel Hover", "Border", "Text", "Text Faded", "Accent",
				"Accent Light", "Success", "Danger" };
		int[] colors = { Theme.background, Theme.panel, Theme.panelHover, Theme.border, Theme.text, Theme.textFaded,
				Theme.accent, Theme.accentLight, Theme.success, Theme.danger };
		float colW = (panelW - 20) / 2.0F;
		for (int i = 0; i < names.length; i++) {
			float sx = px + 10 + (i % 2) * colW, sy = py + 24 + (i / 2) * 14;
			RenderUtil.roundedRect(sx, sy, 22, 10, 4, 0xFF3B4B61);
			RenderUtil.roundedRect(sx + 1, sy + 1, 20, 8, 3, colors[i]);
			TextUtil.drawTrimmed(this.fontRendererObj, names[i], sx + 27, sy + 1, (int) colW - 32, Theme.text);
		}

		// Shapes
		float sy = py + 24 + 5 * 14 + 8;
		float t = Animation.seconds();
		RenderUtil.glow(px + 14, sy, 60, 22, 11, 8, Theme.withAlpha(Theme.accent, 0.5F + 0.2F * (float) Math.sin(t * 2)));
		RenderUtil.pill(px + 14, sy, 60, 22, Theme.accent);
		this.fontRendererObj.drawString("Pill", (int) (px + 44 - this.fontRendererObj.getStringWidth("Pill") / 2.0F),
				(int) (sy + 7), 0xFF0E1116); // dark text on a bright pill: no shadow
		RenderUtil.roundedRect(px + 84, sy, 60, 22, 6, Theme.panel);
		TextUtil.drawCentered(this.fontRendererObj, "Panel", px + 114, sy + 7, Theme.text);
		RenderUtil.circle(px + 166, sy + 11, 11, Theme.success);
		RenderUtil.circle(px + 194, sy + 11, 11, Theme.danger);
		float bounce = Animation.easeInOut((float) (Math.sin(t * 1.5) * 0.5 + 0.5));
		RenderUtil.roundedRect(px + 214, sy + 4, 14, 14, 4, Theme.accentLight);
		RenderUtil.roundedRect(px + 214 + bounce * (panelW - 244), sy + 4, 14, 14, 4, Theme.accent);

		TextUtil.drawCenteredTrimmed(this.fontRendererObj,
				"Long text gets cut off with dots when it does not fit in the space it has", cx, sy + 30,
				(int) panelW - 30, Theme.textFaded);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
