package net.aeroclient.module.performance;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.NumberSetting;
import net.lax1dude.eaglercraft.Display;

/**
 * Drops to a low frame rate while you're not looking at the game (another
 * window or tab is in front), so a Chromebook's battery and fan get a rest.
 * The game keeps running and stays connected; it just draws fewer frames.
 * Full speed comes back the moment you click back in.
 */
public class BackgroundFpsModule extends Module {

	private static BackgroundFpsModule instance;

	private final NumberSetting fps = add(new NumberSetting("Background FPS",
			"Frames per second while the game isn't focused.", 10, 1, 60, 1));

	public BackgroundFpsModule() {
		super("Background FPS Limit", "Saves battery by drawing fewer frames while the game isn't focused.",
				Category.RENDER, 0);
		instance = this;
		enabledByDefault();
	}

	/** The frame limit to use right now, or 0 when the game is focused or the module is off. */
	public static int limit() {
		if (instance == null || !instance.isEnabled() || Display.isActive()) {
			return 0;
		}
		return instance.fps.getInt();
	}

	/** Used by the Low-end preset. */
	void applyLowEnd() {
		this.fps.set(5.0D);
	}
}
