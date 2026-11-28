package net.aeroclient.module.misc;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

/**
 * Highlights chat messages that include your username, and can play a short
 * sound, so you notice when someone talks to you. The message text and its
 * colors stay the same: only the background of the line changes.
 */
public class NameMentionModule extends Module {

	private static NameMentionModule instance;

	private final ColorSetting color = add(new ColorSetting("Highlight Color",
			"The color behind messages that mention you.", 0x5538BDF8, true));
	private final BooleanSetting sound = add(new BooleanSetting("Play Sound", "Play a short ding when you're mentioned.", true));
	private final NumberSetting volume = add(new NumberSetting("Sound Volume", "How loud the ding is.", 80, 10, 100, 5));
	private final BooleanSetting ignoreOwn = add(new BooleanSetting("Ignore Your Own Messages",
			"Don't highlight messages you sent yourself.", true));

	public NameMentionModule() {
		super("Name Mention", "Highlights messages with your username, with an optional sound.", Category.MISC, 0);
		instance = this;
		this.volume.visibleWhen(this.sound::isOn);
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** The highlight color (with transparency). */
	public static int highlightColor() {
		return instance.color.get();
	}

	/** Plays the mention sound, if it's turned on. */
	public static void ding() {
		if (instance.sound.isOn() && instance.mc.getSoundHandler() != null) {
			float v = instance.volume.getFloat() / 100.0F;
			// pitch 1.6 (a bright ding), volume from the slider
			instance.mc.getSoundHandler().playSound(PositionedSoundRecord.func_194007_a(SoundEvents.BLOCK_NOTE_PLING, 1.6F, v));
		}
	}

	/** True if the message contains your username as a whole word. */
	public static boolean mentionsYou(ITextComponent message) {
		String name = instance.mc.getSession() == null ? null : instance.mc.getSession().getUsername();
		if (name == null || name.isEmpty()) {
			return false;
		}
		String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText());
		if (text == null) {
			return false;
		}
		int from = 0;
		if (instance.ignoreOwn.isOn()) {
			// skip the sender part ("<Name> ", "[Rank] Name: ", "Name » ") when it's you
			int sep = senderEnd(text);
			if (sep > 0 && indexOfWord(text.substring(0, sep), name, 0) >= 0) {
				from = sep;
			}
		}
		return indexOfWord(text, name, from) >= 0;
	}

	/** Where the sender part of a chat line ends, or -1 if it doesn't look like a player message. */
	private static int senderEnd(String text) {
		int limit = Math.min(text.length(), 64);
		for (int i = 0; i < limit; ++i) {
			char c = text.charAt(i);
			if ((c == '>' || c == ':' || c == '»') && i + 1 < text.length() && text.charAt(i + 1) == ' ') {
				return i + 1;
			}
		}
		return -1;
	}

	/** Finds the name ignoring case, only where it isn't part of a longer name (letters, digits, _). */
	private static int indexOfWord(String text, String name, int from) {
		int n = name.length();
		for (int i = from; i + n <= text.length(); ++i) {
			if (text.regionMatches(true, i, name, 0, n)
					&& (i == 0 || !isNameChar(text.charAt(i - 1)))
					&& (i + n == text.length() || !isNameChar(text.charAt(i + n)))) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isNameChar(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
	}
}
