package net.aeroclient.module.misc;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * New chat messages slide up and fade in smoothly instead of popping in, and
 * chat remembers more messages, so you can scroll further back (normal
 * Minecraft keeps 100).
 */
public class BetterChatModule extends Module {

	private static BetterChatModule instance;

	private final BooleanSetting slide = add(new BooleanSetting("Slide-In Animation",
			"New messages slide up and fade in.", true));
	private final NumberSetting speed = add(new NumberSetting("Animation Time",
			"How long the slide takes, in milliseconds.", 220, 100, 600, 20));
	private final NumberSetting history = add(new NumberSetting("History Length",
			"How many messages chat keeps to scroll back through.", 500, 100, 1000, 50));

	private long lastMessageAt = 0L;

	public BetterChatModule() {
		super("Better Chat", "Smooth slide-in for new messages and a longer chat history.", Category.MISC, 0);
		instance = this;
		this.speed.visibleWhen(this.slide::isOn);
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** How many messages chat keeps. */
	public static int historyLimit() {
		return active() ? instance.history.getInt() : 100;
	}

	public static void onMessageAdded() {
		if (instance != null) {
			instance.lastMessageAt = Minecraft.getSystemTime();
		}
	}

	/** True while a new message is still sliding in. */
	public static boolean isAnimating() {
		return active() && instance.slide.isOn()
				&& Minecraft.getSystemTime() - instance.lastMessageAt < instance.speed.getInt() + 50;
	}

	/**
	 * How far along the slide-in is for a line that appeared at {@code time},
	 * from 0 (just arrived) to 1 (done). Always 1 when the animation is off.
	 */
	public static float progress(long time, long now) {
		if (time == 0L || !active() || !instance.slide.isOn()) {
			return 1.0F;
		}
		float p = (now - time) / instance.speed.getFloat();
		if (p >= 1.0F) {
			return 1.0F;
		}
		if (p <= 0.0F) {
			return 0.0F;
		}
		float inv = 1.0F - p;
		return 1.0F - inv * inv * inv; // fast at first, then gently settles
	}
}
