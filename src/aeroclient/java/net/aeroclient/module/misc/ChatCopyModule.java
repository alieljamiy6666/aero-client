package net.aeroclient.module.misc;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;

/**
 * Open chat (T) and click a message to copy it. Links and clickable server
 * messages (like "[Click to join]") still work as normal: only clicks on plain
 * text copy. The whole message is copied, even if it's split over two lines.
 */
public class ChatCopyModule extends Module {

	private static ChatCopyModule instance;

	private final BooleanSetting hover = add(new BooleanSetting("Highlight On Hover",
			"Lightly highlight the message under your mouse, so you can see what will be copied.", true));
	private final BooleanSetting keepColors = add(new BooleanSetting("Copy Color Codes",
			"Copy the § color codes too (handy for server owners), instead of plain text.", false));

	private long copiedAt = 0L;

	public ChatCopyModule() {
		super("Chat Copy", "Click a chat message to copy it.", Category.MISC, 0);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** True if the message under the mouse should be highlighted while chat is open. */
	public static boolean highlightsHover() {
		return active() && instance.hover.isOn();
	}

	/**
	 * Called when chat is clicked and the click didn't hit a link or other
	 * clickable text. Copies the message under the mouse; true if it did.
	 */
	public static boolean tryCopy(GuiNewChat chat) {
		if (!active()) {
			return false;
		}
		ChatLine line = chat.aeroLineAt(Mouse.getX(), Mouse.getY());
		if (line == null || line.aeroSource == null) {
			return false;
		}
		String text = instance.keepColors.isOn() ? line.aeroSource.getFormattedText()
				: TextFormatting.getTextWithoutFormattingCodes(line.aeroSource.getUnformattedText());
		if (text == null || text.isEmpty()) {
			return false;
		}
		if (instance.keepColors.isOn() && text.endsWith("§r")) {
			text = text.substring(0, text.length() - 2);
		}
		GuiScreen.setClipboardString(text);
		instance.copiedAt = Minecraft.getSystemTime();
		instance.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.4F));
		return true;
	}

	/** Draws a small "Copied!" bubble by the mouse for a moment after copying. */
	public static void drawFeedback(FontRenderer font, int mouseX, int mouseY) {
		if (instance == null) {
			return;
		}
		long age = Minecraft.getSystemTime() - instance.copiedAt;
		if (age < 0 || age > 1100) {
			return;
		}
		float a = age < 900 ? 1.0F : 1.0F - (age - 900) / 200.0F;
		float rise = Math.min(1.0F, age / 150.0F) * 4.0F;
		String text = "Copied!";
		float w = font.getStringWidth(text) + 10, x = mouseX + 8, y = mouseY - 16 - rise;
		RenderUtil.roundedRect(x, y, w, 13, 4, Theme.withAlpha(0xFF0B0F15, 0.92F * a));
		RenderUtil.roundedOutline(x, y, w, 13, 4, 1.0F, Theme.withAlpha(Theme.accent, a));
		font.drawString(text, x + 5, y + 3, Theme.withAlpha(Theme.accentLight, Math.max(0.05F, a)), false);
	}
}
