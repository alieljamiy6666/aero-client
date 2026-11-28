package net.aeroclient.gui;

import java.io.IOException;

import net.aeroclient.config.ConfigManager;
import net.aeroclient.module.performance.LowEndPreset;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * Aero Client's own settings: the accent color, the rounded button style and
 * the animated background (turn it off on slow computers).
 *
 * Changes are saved automatically and kept after the page reloads.
 */
public class AeroSettingsScreen extends GuiScreen {

	private static final int ID_BUTTONS = 1, ID_BACKGROUND = 2, ID_PREVIEW = 3, ID_DONE = 4, ID_PROFILES = 5,
			ID_LOW_END = 6, ID_SCREENSHOTS = 7;

	private final GuiScreen parent;
	private final Animation.Fader appear = new Animation.Fader(300.0F);
	private final Animation.Fader[] swatchFaders = new Animation.Fader[Theme.ACCENT_COLORS.length];

	private float panelX, panelY, panelW, panelH;
	private GuiButton lowEndButton;
	/** When the Low-end Preset button was clicked once (it needs a second click), or 0. */
	private long lowEndArmedAt = 0L;

	public AeroSettingsScreen(GuiScreen parent) {
		this.parent = parent;
		for (int i = 0; i < swatchFaders.length; i++) {
			swatchFaders[i] = new Animation.Fader(140.0F);
		}
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		this.panelW = Math.min(300, this.width - 20);
		this.panelH = 198;
		this.panelX = this.width / 2.0F - this.panelW / 2.0F;
		this.panelY = Math.max(8, this.height / 2.0F - this.panelH / 2.0F - 14);
		int cx = this.width / 2;
		int y = (int) this.panelY + 82;
		this.buttonList.add(new GuiButton(ID_BUTTONS, cx - 100, y, 200, 20, buttonsLabel()));
		this.buttonList.add(new GuiButton(ID_BACKGROUND, cx - 100, y + 24, 200, 20, backgroundLabel()));
		this.buttonList.add(new GuiButton(ID_PROFILES, cx - 100, y + 48, 98, 20, "Profiles..."));
		this.buttonList.add(new GuiButton(ID_PREVIEW, cx + 2, y + 48, 98, 20, "Theme Preview..."));
		this.buttonList.add(new GuiButton(ID_SCREENSHOTS, cx - 100, y + 72, 98, 20, "Screenshots..."));
		this.lowEndButton = new GuiButton(ID_LOW_END, cx + 2, y + 72, 98, 20, "Low-end Preset...");
		this.buttonList.add(this.lowEndButton);
		this.lowEndArmedAt = 0L;
		this.buttonList.add(new GuiButton(ID_DONE, cx - 100, (int) (this.panelY + this.panelH) + 10, 200, 20, "Done"));
	}

	private static String buttonsLabel() {
		return "Rounded Buttons: " + (AeroButtonStyle.enabled ? "ON" : "OFF");
	}

	private static String backgroundLabel() {
		return "Animated Background: " + (AeroMenuBackground.animated ? "ON" : "OFF");
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		switch (button.id) {
		case ID_BUTTONS:
			AeroButtonStyle.enabled = !AeroButtonStyle.enabled;
			button.displayString = buttonsLabel();
			ConfigManager.markDirty();
			break;
		case ID_BACKGROUND:
			AeroMenuBackground.animated = !AeroMenuBackground.animated;
			button.displayString = backgroundLabel();
			ConfigManager.markDirty();
			break;
		case ID_PROFILES:
			this.mc.displayGuiScreen(new AeroProfilesScreen(this));
			break;
		case ID_PREVIEW:
			this.mc.displayGuiScreen(new AeroThemeTestScreen(this));
			break;
		case ID_LOW_END:
			// two clicks, because it also changes some normal video settings
			if (this.lowEndArmedAt == 0L) {
				this.lowEndArmedAt = System.currentTimeMillis();
				button.displayString = "Click again";
			} else {
				LowEndPreset.apply();
				this.lowEndArmedAt = 0L;
				button.displayString = "Applied!";
				this.buttonList.get(1).displayString = backgroundLabel();
			}
			break;
		case ID_SCREENSHOTS:
			this.mc.displayGuiScreen(new AeroScreenshotsScreen(this));
			break;
		case ID_DONE:
			this.mc.displayGuiScreen(this.parent);
			break;
		default:
			break;
		}
	}

	// ---------------- accent swatches ----------------

	private float swatchX(int i) {
		float spacing = 30.0F;
		float start = this.width / 2.0F - spacing * (Theme.ACCENT_COLORS.length - 1) / 2.0F;
		return start + i * spacing;
	}

	private float swatchY() {
		return this.panelY + 46;
	}

	private int swatchAt(int mouseX, int mouseY) {
		for (int i = 0; i < Theme.ACCENT_COLORS.length; i++) {
			float dx = mouseX - swatchX(i), dy = mouseY - swatchY();
			if (dx * dx + dy * dy <= 11 * 11) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		int i = swatchAt(mouseX, mouseY);
		if (mouseButton == 0 && i >= 0) {
			Theme.setAccent(Theme.ACCENT_COLORS[i]);
			ConfigManager.markDirty();
			this.mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord
					.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		float a = this.appear.update(true);
		float py = this.panelY + (1.0F - a) * 16.0F;

		RenderUtil.shadow(this.panelX, py + 2, this.panelW, this.panelH, 10, 12, Theme.withAlpha(0xFF000000, 0.5F * a));
		RenderUtil.roundedRect(this.panelX, py, this.panelW, this.panelH, 10, Theme.withAlpha(0xFF2B3646, a));
		RenderUtil.roundedRectGradient(this.panelX + 1, py + 1, this.panelW - 2, this.panelH - 2, 9,
				Theme.withAlpha(0xFF151B24, 0.97F * a), Theme.withAlpha(0xFF0E1218, 0.97F * a));

		TextUtil.drawCentered(this.fontRendererObj, "Aero Settings", this.width / 2.0F, py + 10, Theme.accentLight);
		TextUtil.drawCentered(this.fontRendererObj, "Accent Color", this.width / 2.0F, py + 26, Theme.textFaded);

		int hovered = swatchAt(mouseX, mouseY);
		int selected = Theme.accentPresetIndex();
		for (int i = 0; i < Theme.ACCENT_COLORS.length; i++) {
			float h = swatchFaders[i].update(i == hovered || i == selected);
			float x = swatchX(i), y = swatchY() + (py - this.panelY);
			float r = 9.0F + 1.5F * h;
			int color = Theme.ACCENT_COLORS[i];
			if (h > 0.01F) {
				RenderUtil.glow(x - r, y - r, r * 2, r * 2, r, 7, Theme.withAlpha(color, 0.55F * h));
			}
			if (i == selected) {
				RenderUtil.circle(x, y, r + 2.5F, 0xFFF8FAFC);
				RenderUtil.circle(x, y, r + 1.0F, 0xFF0E1218);
			}
			RenderUtil.circle(x, y, r, color);
		}
		String name = hovered >= 0 ? Theme.ACCENT_NAMES[hovered] : (selected >= 0 ? Theme.ACCENT_NAMES[selected] : "Custom");
		TextUtil.drawCentered(this.fontRendererObj, name, this.width / 2.0F, swatchY() + (py - this.panelY) + 15, Theme.text);

		TextUtil.drawCentered(this.fontRendererObj, "Profile: " + ConfigManager.getActiveProfile() + "  -  saved automatically", this.width / 2.0F,
				py + this.panelH - 13, Theme.withAlpha(Theme.textFaded, 0.8F * a));
		TextUtil.drawCentered(this.fontRendererObj,
				net.aeroclient.AeroClient.getDisplayName() + " - Made by " + net.aeroclient.AeroClient.AUTHOR,
				this.width / 2.0F, this.height - 12, Theme.withAlpha(Theme.textFaded, a));

		if (this.lowEndArmedAt != 0L && System.currentTimeMillis() - this.lowEndArmedAt > 4000L) {
			this.lowEndArmedAt = 0L; // the second click didn't come: back to normal
			this.lowEndButton.displayString = "Low-end Preset...";
		}

		super.drawScreen(mouseX, mouseY, partialTicks);

		if (this.lowEndButton.isMouseOver()) {
			// centered just above the button, so it doesn't cover the button's own label
			String changes = String.join("\n", LowEndPreset.CHANGES);
			int h = this.fontRendererObj.listFormattedStringToWidth(changes, 170).size() * 10 - 2;
			net.aeroclient.gui.clickgui.Widgets.tooltip(this.fontRendererObj, changes, this.width / 2,
					this.lowEndButton.yPosition - h - 26, this.width, this.height, true, 1.0F);
		}
	}
}
