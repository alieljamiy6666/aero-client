package net.aeroclient.gui;

import java.io.IOException;
import java.util.List;

import net.aeroclient.config.ConfigManager;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;

/**
 * Profiles: separate sets of module settings, like "PvP" or "Low-end PC".
 * Click a profile to switch to it, make new ones, delete them, and share them
 * as text (Export copies the active profile, Import adds one you pasted).
 */
public class AeroProfilesScreen extends GuiScreen implements GuiYesNoCallback {

	private static final int ID_DONE = 1, ID_CREATE = 2, ID_IMPORT = 3, ID_EXPORT = 4, ID_DELETE = 5, ID_PROFILE = 100;

	private final GuiScreen parent;
	private final Animation.Fader appear = new Animation.Fader(300.0F);
	private GuiTextField nameField, importField;
	private String status = "";
	private int statusColor;
	private float panelX, panelY, panelW, panelH;

	public AeroProfilesScreen(GuiScreen parent) {
		this.parent = parent;
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		this.panelW = Math.min(300, this.width - 16);
		this.panelH = 222;
		this.panelX = this.width / 2.0F - this.panelW / 2.0F;
		this.panelY = Math.max(4, (this.height - this.panelH - 28) / 2.0F);
		int left = (int) this.panelX + 10, inner = (int) this.panelW - 20;
		int top = (int) this.panelY;

		List<String> names = ConfigManager.getProfileNames();
		int colW = (inner - 4) / 2;
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			boolean active = name.equals(ConfigManager.getActiveProfile());
			GuiButton b = new GuiButton(ID_PROFILE + i, left + (i % 2) * (colW + 4), top + 34 + (i / 2) * 24, colW, 20,
					active ? name + " (active)" : name);
			this.buttonList.add(b);
		}

		int fieldW = inner - 84;
		this.nameField = new GuiTextField(0, this.fontRendererObj, left + 1, top + 136, fieldW - 2, 18);
		this.nameField.setMaxStringLength(ConfigManager.MAX_PROFILE_NAME);
		this.importField = new GuiTextField(1, this.fontRendererObj, left + 1, top + 162, fieldW - 2, 18);
		this.importField.setMaxStringLength(32767);
		this.buttonList.add(new GuiButton(ID_CREATE, left + fieldW + 4, top + 135, 80, 20, "Create"));
		this.buttonList.add(new GuiButton(ID_IMPORT, left + fieldW + 4, top + 161, 80, 20, "Import"));

		int third = (inner - 8) / 3;
		this.buttonList.add(new GuiButton(ID_EXPORT, left, top + 196, third, 20, "Export"));
		GuiButton delete = new GuiButton(ID_DELETE, left + third + 4, top + 196, third, 20, "Delete");
		delete.enabled = names.size() > 1;
		this.buttonList.add(delete);
		this.buttonList.add(new GuiButton(ID_DONE, left + (third + 4) * 2, top + 196, third, 20, "Done"));
	}

	private void setStatus(String text, boolean good) {
		this.status = text;
		this.statusColor = good ? Theme.success : Theme.danger;
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id >= ID_PROFILE) {
			List<String> names = ConfigManager.getProfileNames();
			int i = button.id - ID_PROFILE;
			if (i < names.size()) {
				ConfigManager.switchProfile(names.get(i));
				setStatus("Switched to " + names.get(i), true);
				refresh();
			}
			return;
		}
		switch (button.id) {
		case ID_CREATE: {
			String problem = ConfigManager.createProfile(this.nameField.getText(), true);
			if (problem == null) {
				setStatus("Made \"" + this.nameField.getText().trim() + "\" (a copy of your current settings)", true);
				this.nameField.setText("");
				refresh();
			} else {
				setStatus(problem, false);
			}
			break;
		}
		case ID_IMPORT:
			try {
				String name = ConfigManager.importProfile(this.importField.getText());
				setStatus("Imported as \"" + name + "\". Click it to use it.", true);
				this.importField.setText("");
				refresh();
			} catch (IllegalArgumentException e) {
				setStatus(e.getMessage(), false);
			}
			break;
		case ID_EXPORT:
			setClipboardString(ConfigManager.exportProfile(ConfigManager.getActiveProfile()));
			setStatus("Copied \"" + ConfigManager.getActiveProfile() + "\" - paste it anywhere to share it", true);
			break;
		case ID_DELETE:
			this.mc.displayGuiScreen(new GuiYesNo(this, "Delete the profile \"" + ConfigManager.getActiveProfile() + "\"?",
					"This can't be undone.", 0));
			break;
		case ID_DONE:
			this.mc.displayGuiScreen(this.parent);
			break;
		default:
			break;
		}
	}

	@Override
	public void confirmClicked(boolean result, int id) {
		if (result) {
			String name = ConfigManager.getActiveProfile();
			if (ConfigManager.deleteProfile(name)) {
				setStatus("Deleted \"" + name + "\"", true);
			}
		}
		this.mc.displayGuiScreen(this);
	}

	/** Rebuilds the buttons (profile names changed) but keeps whatever was typed. */
	private void refresh() {
		String name = this.nameField.getText(), imported = this.importField.getText();
		initGui();
		this.nameField.setText(name);
		this.importField.setText(imported);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (this.nameField.textboxKeyTyped(typedChar, keyCode) || this.importField.textboxKeyTyped(typedChar, keyCode)) {
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
		this.importField.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen() {
		this.nameField.updateCursorCounter();
		this.importField.updateCursorCounter();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		float a = this.appear.update(true);
		float x = this.panelX, y = this.panelY, w = this.panelW, h = this.panelH;

		RenderUtil.shadow(x, y + 2, w, h, 10, 12, Theme.withAlpha(0xFF000000, 0.5F * a));
		RenderUtil.roundedRect(x, y, w, h, 10, Theme.withAlpha(0xFF2B3646, a));
		RenderUtil.roundedRectGradient(x + 1, y + 1, w - 2, h - 2, 9, Theme.withAlpha(0xFF151B24, 0.97F * a),
				Theme.withAlpha(0xFF0E1218, 0.97F * a));

		TextUtil.drawCentered(this.fontRendererObj, "Profiles", this.width / 2.0F, y + 8, Theme.accentLight);
		TextUtil.drawCentered(this.fontRendererObj, "Click one to switch. Your changes save on their own.",
				this.width / 2.0F, y + 20, Theme.textFaded);

		float labelY = y + 126;
		this.fontRendererObj.drawStringWithShadow("New profile (copies your current settings):", x + 11, labelY, Theme.textFaded);
		this.nameField.drawTextBox();
		this.importField.drawTextBox();
		if (this.importField.getText().isEmpty() && !this.importField.isFocused()) {
			this.fontRendererObj.drawStringWithShadow("Paste a shared profile here (Ctrl+V)", x + 15, y + 167, 0xFF6B7280);
		}
		if (!this.status.isEmpty()) {
			TextUtil.drawCenteredTrimmed(this.fontRendererObj, this.status, this.width / 2.0F, y + 184, (int) w - 20,
					this.statusColor);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
