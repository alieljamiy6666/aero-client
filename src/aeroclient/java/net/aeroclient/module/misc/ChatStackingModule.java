package net.aeroclient.module.misc;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * When the same message arrives several times in a row, chat shows it once
 * with a counter, like "Welcome to the server! (x3)", instead of filling the
 * chat box with copies.
 */
public class ChatStackingModule extends Module {

	private static ChatStackingModule instance;

	public ChatStackingModule() {
		super("Chat Stacking", "Repeated messages show once with a counter, like (x3).", Category.MISC, 0);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** The gray " (x3)" piece added after a repeated message. */
	public static ITextComponent counter(int count) {
		ITextComponent c = new TextComponentString(" (x" + count + ")");
		c.getStyle().setColor(TextFormatting.GRAY);
		return c;
	}
}
