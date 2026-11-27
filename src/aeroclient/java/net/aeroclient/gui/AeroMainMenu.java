package net.aeroclient.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.lax1dude.eaglercraft.profile.GuiScreenEditProfile;
import net.lax1dude.eaglercraft.profile.SkinModel;
import net.lax1dude.eaglercraft.sp.gui.GuiScreenIntegratedServerStartup;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenFirstLoad;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.peyton.eagler.gui.GuiCredits;

/**
 * Aero Client's main menu: the animated logo and background, the main
 * buttons, a player card with your skin's face and name (click it to edit
 * your profile), and small Credits and Classic Menu buttons.
 *
 * It extends the classic menu so any game code that checks "is this the main
 * menu?" still works. The game swaps it in automatically (see
 * {@link #replace}); picking Classic Menu switches back to the original
 * layout until you choose the Aero menu again.
 */
public class AeroMainMenu extends GuiMainMenu {

	/** True while the player has chosen the classic menu instead. */
	public static boolean classicMode = false;

	private static final int ID_OPTIONS = 0, ID_SINGLEPLAYER = 1, ID_MULTIPLAYER = 2, ID_LANGUAGE = 5,
			ID_CREDITS = 14, ID_AERO_SETTINGS = 301, ID_CLASSIC = 302;
	private static final ResourceLocation SPLASHES = new ResourceLocation("aeroclient/splashes.txt");
	private static final Random RANDOM = new Random();

	private final String splash;
	private final Animation.Fader appear = new Animation.Fader(420.0F);
	private final Animation.Fader cardHover = new Animation.Fader(150.0F);
	private int mojangTextX, mojangTextWidth;

	public AeroMainMenu() {
		this.splash = pickSplash();
	}

	/**
	 * Called by the game whenever it's about to show a screen: swaps the
	 * classic main menu for the Aero one, unless Classic Menu was picked.
	 */
	public static GuiScreen replace(GuiScreen screen) {
		if (screen != null && screen.getClass() == GuiMainMenu.class && !classicMode) {
			return new AeroMainMenu();
		}
		return screen;
	}

	@Override
	public void initGui() {
		if (!this.mc.gameSettings.hasSeenFirstLoad) { // the one-time credits notice
			this.mc.displayGuiScreen(new GuiScreenFirstLoad(this.mc.gameSettings));
			return;
		}
		this.buttonList.clear();
		int cx = this.width / 2;
		int y = this.height / 4 + 48;
		this.buttonList.add(new GuiButton(ID_SINGLEPLAYER, cx - 100, y, I18n.format("menu.singleplayer")));
		this.buttonList.add(new GuiButton(ID_MULTIPLAYER, cx - 100, y + 24, I18n.format("menu.multiplayer")));
		this.buttonList.add(new GuiButton(ID_AERO_SETTINGS, cx - 100, y + 48, 98, 20, "Aero Settings"));
		this.buttonList.add(new GuiButton(ID_OPTIONS, cx + 2, y + 48, 98, 20, I18n.format("menu.options")));
		this.buttonList.add(new AeroButton(ID_LANGUAGE, cx - 124, y + 48, 20, 20, "", AeroButton.ICON_GLOBE));
		this.buttonList.add(new GuiButton(ID_CREDITS, this.width - 152, 6, 62, 16, I18n.format("menu.credits")));
		this.buttonList.add(new GuiButton(ID_CLASSIC, this.width - 86, 6, 80, 16, "Classic Menu"));

		this.mojangTextWidth = this.fontRendererObj.getStringWidth("Resources copyright Mojang AB");
		this.mojangTextX = this.width - this.mojangTextWidth - 2;
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case ID_SINGLEPLAYER:
			this.mc.displayGuiScreen(new GuiScreenIntegratedServerStartup(this));
			break;
		case ID_MULTIPLAYER:
			this.mc.displayGuiScreen(new GuiMultiplayer(this));
			break;
		case ID_AERO_SETTINGS:
			this.mc.displayGuiScreen(new AeroSettingsScreen(this));
			break;
		case ID_OPTIONS:
			this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
			break;
		case ID_LANGUAGE:
			this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
			break;
		case ID_CREDITS:
			this.mc.displayGuiScreen(new GuiCredits(this, "/assets/eagler/credits.txt"));
			break;
		case ID_CLASSIC:
			classicMode = true;
			net.aeroclient.config.ConfigManager.markDirty();
			this.mc.displayGuiScreen(new GuiMainMenu());
			break;
		default:
			break;
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 20) { // T opens the theme test screen
			this.mc.displayGuiScreen(new AeroThemeTestScreen(this));
		} else if (keyCode == net.aeroclient.gui.clickgui.ClickGuiScreen.MENU_KEY
				&& !net.lax1dude.eaglercraft.Keyboard.isRepeatEvent()) {
			// Right Shift opens the module menu here too, to set things up before playing
			net.aeroclient.gui.clickgui.ClickGuiScreen.open(this);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton); // the buttons
		if (mouseButton == 0 && mouseX > this.mojangTextX && mouseX < this.mojangTextX + this.mojangTextWidth
				&& mouseY > this.height - 10 && mouseY < this.height) { // Mojang credits, like the classic menu
			this.mc.displayGuiScreen(new GuiWinGame(false, null));
			return;
		}
		if (mouseButton == 0 && isOverCard(mouseX, mouseY)) {
			this.mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord
					.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.mc.displayGuiScreen(new GuiScreenEditProfile(this));
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		float a = this.appear.update(true);
		AeroMenuBackground.draw(this.mc, this.width, this.height, mouseX, mouseY);

		AeroBranding.drawTitleLogo(this.mc, this.width, this.height);
		drawSplash();
		drawPlayerCard(mouseX, mouseY);

		// The buttons (drawn by GuiScreen, skipping the classic menu's own drawing).
		for (GuiButton button : this.buttonList) {
			button.func_191745_a(this.mc, mouseX, mouseY, partialTicks);
		}

		AeroBranding.drawVersionText(this.fontRendererObj, this.height);
		this.fontRendererObj.drawStringWithShadow("Resources copyright Mojang AB", this.mojangTextX,
				this.height - 10, 0xFFB0B0B0);

		if (a < 1.0F) { // gentle fade-in when the menu opens
			Gui.drawRect(0, 0, this.width, this.height, Theme.withAlpha(Theme.background, 1.0F - a));
		}
	}

	// ---------------- splash text ----------------

	private void drawSplash() {
		GlStateManager.pushMatrix();
		GlStateManager.translate(AeroBranding.splashX(this.width, this.height),
				AeroBranding.splashY(this.width, this.height), 0.0F);
		GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
		float pulse = 1.8F - MathHelper.abs(MathHelper
				.sin((float) (net.minecraft.client.Minecraft.getSystemTime() % 1000L) / 1000.0F * ((float) Math.PI * 2.0F))
				* 0.1F);
		pulse = pulse * 100.0F / (float) (this.fontRendererObj.getStringWidth(this.splash) + 32);
		GlStateManager.scale(pulse, pulse, pulse);
		this.drawCenteredString(this.fontRendererObj, this.splash, 0, -8, 0xFFFFFF00);
		GlStateManager.popMatrix();
	}

	private String pickSplash() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int month = calendar.get(Calendar.MONTH) + 1, day = calendar.get(Calendar.DAY_OF_MONTH);
		if (month == 12 && day == 24) {
			return "Merry Christmas!";
		} else if (month == 1 && day == 1) {
			return "Happy New Year!";
		} else if (month == 10 && day == 31) {
			return "Happy Halloween!";
		}
		List<String> lines = new ArrayList<>();
		try (InputStream in = net.minecraft.client.Minecraft.getMinecraft().getResourceManager().getResource(SPLASHES)
				.getInputStream()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					lines.add(line);
				}
			}
		} catch (Throwable t) {
			// no splash file: fall through to the default below
		}
		return lines.isEmpty() ? "Light as air!" : lines.get(RANDOM.nextInt(lines.size()));
	}

	// ---------------- player card ----------------

	private int cardWidth() {
		String name = EaglerProfile.getName();
		return 36 + Math.max(this.fontRendererObj.getStringWidth(name), this.fontRendererObj.getStringWidth("Edit profile")) + 10;
	}

	private boolean isOverCard(int mouseX, int mouseY) {
		return mouseX >= 6 && mouseY >= 6 && mouseX < 6 + cardWidth() && mouseY < 6 + 30;
	}

	private void drawPlayerCard(int mouseX, int mouseY) {
		int x = 6, y = 6, w = cardWidth(), h = 30;
		float hover = this.cardHover.update(isOverCard(mouseX, mouseY));
		RenderUtil.shadow(x, y + 1, w, h, 8, 6, Theme.withAlpha(0xFF000000, 0.45F));
		if (hover > 0.01F) {
			RenderUtil.glow(x, y, w, h, 8, 10, Theme.withAlpha(Theme.accent, 0.6F * hover));
		}
		RenderUtil.roundedRect(x, y, w, h, 8, Theme.mix(0xFF2B3646, Theme.accent, hover));
		RenderUtil.roundedRectGradient(x + 1, y + 1, w - 2, h - 2, 7, 0xF01A2230, 0xF0111720);

		drawFace(x + 5, y + 5, 20);

		TextUtil.drawTrimmed(this.fontRendererObj, EaglerProfile.getName(), x + 31, y + 6, w - 36, Theme.text);
		this.fontRendererObj.drawStringWithShadow("Edit profile", x + 31, y + 17,
				Theme.mix(Theme.textFaded, Theme.accentLight, hover));
	}

	/** Your skin's face (plus its hat layer), or your initial if the skin has an unusual shape. */
	private void drawFace(int x, int y, int size) {
		RenderUtil.roundedRect(x - 1, y - 1, size + 2, size + 2, 3, 0xFF0B1018);
		SkinModel model = EaglerProfile.getActiveSkinModel();
		ResourceLocation skin = EaglerProfile.getActiveSkinResourceLocation();
		if (model == null || model.highPoly != null || skin == null) {
			RenderUtil.roundedRect(x, y, size, size, 3, Theme.accent);
			String initial = EaglerProfile.getName().isEmpty() ? "?" : EaglerProfile.getName().substring(0, 1).toUpperCase();
			TextUtil.drawCentered(this.fontRendererObj, initial, x + size / 2.0F, y + (size - 8) / 2.0F, 0xFF0E1116);
			return;
		}
		this.mc.getTextureManager().bindTexture(skin);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.enableAlpha();
		float tw = model.width, th = model.height;
		Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, size, size, tw, th); // face
		Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, size, size, tw, th); // hat layer
		GlStateManager.disableBlend();
	}
}
