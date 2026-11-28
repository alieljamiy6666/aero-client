package net.aeroclient.module.misc;

import java.util.Locale;

import net.aeroclient.event.ChatReceiveEvent;
import net.aeroclient.event.TickEvent;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.module.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;

/**
 * Sends a message of your choice (like "gg") once when a game ends. It knows a
 * game ended when the server says something like "Winner:", "won the game" or
 * "VICTORY!" in chat or as a big title.
 *
 * It's off unless you turn it on, sends at most one message per game, and
 * never more than once every 15 seconds, so it can't spam.
 */
public class AutoGGModule extends Module {

	private static AutoGGModule instance;

	/** Phrases servers use when a game ends (checked without colors, ignoring capitals). */
	private static final String[] GAME_END = { "winner:", "winners:", "winner -", "winning team", "won the game",
			"wins the game", "won the match", "wins the match", "1st killer", "1st place", "victory!", "you won!",
			"you win!", "game over!" };

	private static final long COOLDOWN_MS = 15000L;

	private final StringSetting message = add(new StringSetting("Message", "What to send when a game ends.", "gg", 100));
	private final NumberSetting delay = add(new NumberSetting("Delay", "Seconds to wait before sending.", 1.0, 0.0, 5.0, 0.5));

	private long sendAt = -1L;
	private long lastSent = -COOLDOWN_MS;

	public AutoGGModule() {
		super("Auto GG", "Sends a message (like \"gg\") when a game ends.", Category.MISC, 0);
		instance = this;
		listen(ChatReceiveEvent.class, e -> {
			if (e.getType() != ChatType.GAME_INFO && e.getMessage() != null) {
				check(e.getMessage().getUnformattedText());
			}
		});
		listen(TickEvent.class, e -> {
			if (this.sendAt >= 0L && Minecraft.getSystemTime() >= this.sendAt) {
				this.sendAt = -1L;
				send();
			}
		});
	}

	@Override
	protected void onDisable() {
		this.sendAt = -1L;
	}

	/** Titles (the big text in the middle of the screen) are checked too. */
	public static void onTitle(String title) {
		if (instance != null && instance.isEnabled() && title != null && !title.isEmpty()) {
			instance.check(title);
		}
	}

	private void check(String text) {
		if (this.sendAt >= 0L || Minecraft.getSystemTime() - this.lastSent < COOLDOWN_MS) {
			return; // already about to send, or sent one for this game
		}
		String plain = TextFormatting.getTextWithoutFormattingCodes(text);
		if (plain == null) {
			return;
		}
		plain = plain.toLowerCase(Locale.ROOT);
		for (String phrase : GAME_END) {
			if (plain.contains(phrase)) {
				this.sendAt = Minecraft.getSystemTime() + (long) (this.delay.get() * 1000.0D);
				return;
			}
		}
	}

	private void send() {
		String text = this.message.get().trim();
		if (text.isEmpty() || this.mc.player == null || this.mc.getConnection() == null) {
			return;
		}
		this.lastSent = Minecraft.getSystemTime();
		this.mc.player.sendChatMessage(text);
	}
}
