package net.aeroclient.chat;

import java.util.List;

import net.aeroclient.module.misc.BetterChatModule;
import net.aeroclient.module.misc.ChatCopyModule;
import net.aeroclient.module.misc.ChatStackingModule;
import net.aeroclient.module.misc.ChatTimestampsModule;
import net.aeroclient.module.misc.NameMentionModule;
import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * Where the chat box asks the chat modules what to do. Every method is safe
 * to call with all chat modules off: the chat then works exactly like normal
 * Minecraft.
 *
 * Server colors and rank prefixes are never changed. A timestamp or an
 * "(x3)" counter is added as a separate piece next to the message, so the
 * message keeps its own colors.
 */
public final class ChatHooks {

	/** Line ids Aero gives messages so a repeat can replace the earlier copy (normal messages use 0). */
	private static final int FIRST_ID = 0x4AE00000;

	private static String lastKey;
	private static int lastCount, lastId, nextId = FIRST_ID;

	private ChatHooks() {
	}

	/**
	 * A message is about to be added to chat. Returns true if Aero added it
	 * (with a timestamp, counter or highlight), false to let the game add it
	 * normally.
	 */
	public static boolean print(GuiNewChat chat, ITextComponent message, int id) {
		boolean stacking = ChatStackingModule.active();
		boolean stamps = ChatTimestampsModule.active();
		boolean mention = NameMentionModule.active() && NameMentionModule.mentionsYou(message);
		if (!stacking) {
			lastKey = null;
		}
		if (!stacking && !stamps && !mention) {
			return false;
		}
		int count = 1;
		int lineId = id;
		if (stacking && id == 0) {
			String key = message.getFormattedText();
			if (key.equals(lastKey)) {
				count = ++lastCount;
				lineId = lastId; // the earlier copy has this id, so adding this one replaces it
			} else {
				lastKey = key;
				lastCount = 1;
				lastId = lineId = newId();
			}
		} else {
			lastKey = null;
		}
		ITextComponent shown = message;
		if (stamps || count > 1) {
			// an unstyled holder, so the message keeps exactly its own colors
			ITextComponent holder = new TextComponentString("");
			if (stamps) {
				holder.appendSibling(ChatTimestampsModule.stamp());
			}
			holder.appendSibling(message);
			if (count > 1) {
				holder.appendSibling(ChatStackingModule.counter(count));
			}
			shown = holder;
		}
		chat.aeroAddMessage(shown, lineId, mention, message);
		if (mention && count == 1) {
			NameMentionModule.ding();
		}
		return true;
	}

	private static int newId() {
		if (++nextId > FIRST_ID + 0xFFFFF) {
			nextId = FIRST_ID + 1;
		}
		return nextId;
	}

	/** Chat was cleared: nothing to stack with any more. */
	public static void reset() {
		lastKey = null;
	}

	/** A new line went into chat (for Better Chat's animation). */
	public static void onMessageAdded() {
		BetterChatModule.onMessageAdded();
	}

	/** How many messages chat keeps. */
	public static int historyLimit() {
		return BetterChatModule.historyLimit();
	}

	/** How far down to draw the chat so the newest line can slide up into place. */
	public static float slideOffset(List<ChatLine> drawnLines, long now) {
		if (drawnLines.isEmpty()) {
			return 0.0F;
		}
		return (1.0F - BetterChatModule.progress(drawnLines.get(0).aeroTime, now)) * 9.0F;
	}

	/** How visible a line is while it slides in (1 = fully). */
	public static float fadeIn(ChatLine line, long now) {
		return BetterChatModule.progress(line.aeroTime, now);
	}

	/** The message under the mouse while chat is open, if Chat Copy highlights it. */
	public static ITextComponent hoveredSource(GuiNewChat chat) {
		if (!ChatCopyModule.highlightsHover()) {
			return null;
		}
		ChatLine line = chat.aeroLineAt(Mouse.getX(), Mouse.getY());
		return line == null ? null : line.aeroSource;
	}

	/** Draws the Name Mention highlight and the Chat Copy hover highlight behind one chat line. */
	public static void drawLineExtras(ChatLine line, int x0, int y0, int x1, int y1, int alpha, ITextComponent hovered) {
		if (line.aeroMention && NameMentionModule.active()) {
			int c = NameMentionModule.highlightColor();
			int a = (c >>> 24) * alpha / 255;
			Gui.drawRect(x0, y0, x1, y1, (a << 24) | (c & 0xFFFFFF));
			Gui.drawRect(x0, y0, x0 + 1, y1, (alpha << 24) | (c & 0xFFFFFF)); // a solid bar on the left
		}
		if (hovered != null && line.aeroSource == hovered) {
			Gui.drawRect(x0, y0, x1, y1, (0x40 * alpha / 255) << 24 | 0xFFFFFF);
		}
	}

	/**
	 * True when the HUD should be redrawn every frame instead of a few times a
	 * second (Eaglercraft normally caches it): while a new message slides in,
	 * or while chat is open with Better Chat or Chat Copy on.
	 */
	public static boolean wantsSmoothChat() {
		if (BetterChatModule.isAnimating()) {
			return true;
		}
		Minecraft mc = Minecraft.getMinecraft();
		return mc.ingameGUI != null && mc.ingameGUI.getChatGUI().getChatOpen()
				&& (BetterChatModule.active() || ChatCopyModule.highlightsHover());
	}
}
