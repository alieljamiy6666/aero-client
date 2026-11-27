package net.aeroclient.event;

import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;

/**
 * Sent when a chat message arrives, before it's shown. Modules can change
 * the message (for example, add a timestamp) or hide it with
 * {@link #setCancelled}.
 */
public final class ChatReceiveEvent extends Event {

	private final ChatType type;
	private ITextComponent message;
	private boolean cancelled;

	public ChatReceiveEvent(ChatType type, ITextComponent message) {
		this.type = type;
		this.message = message;
	}

	/** Normal chat, a system message, or the text above the hotbar. */
	public ChatType getType() {
		return type;
	}

	public ITextComponent getMessage() {
		return message;
	}

	public void setMessage(ITextComponent message) {
		this.message = message;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/** Hides the message. */
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
