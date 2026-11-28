package net.aeroclient.module.performance;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.NumberSetting;

/**
 * Stops the game from freezing for a moment while it builds new chunks, for
 * example when you turn around or fly into a new area.
 *
 * The browser runs everything on one thread, so every chunk that gets built
 * (turned into blocks you can see) is built during a frame. Normally the game
 * is allowed to keep building for up to 100 ms past the end of a frame, which
 * shows up as a stutter. This module gives chunk building a small time budget
 * and a limit on how many chunks it builds each frame. Chunks still load, just
 * spread over a few more frames.
 *
 * Blocks you place or break still update straight away: the game builds those
 * separately and this module doesn't touch them.
 */
public class SmartChunkUpdatesModule extends Module {

	private static SmartChunkUpdatesModule instance;

	private final NumberSetting budget = add(new NumberSetting("Time Per Frame",
			"Most milliseconds spent building chunks each frame. Lower = smoother, chunks load a bit slower.", 4, 1, 20, 1));
	private final NumberSetting maxChunks = add(new NumberSetting("Chunks Per Frame",
			"Most chunks built in one frame.", 3, 1, 10, 1));

	public SmartChunkUpdatesModule() {
		super("Smart Chunk Updates", "Builds chunks a little at a time so turning around doesn't stutter.",
				Category.RENDER, 0);
		instance = this;
		enabledByDefault();
	}

	/** True while the module is on. */
	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** The time (System.nanoTime) chunk building should stop at this frame. */
	public static long deadline(long frameStartNanos) {
		return frameStartNanos + (long) (instance.budget.get() * 1000000.0D);
	}

	/** Most chunks to build in one frame. */
	public static int maxPerFrame() {
		return instance.maxChunks.getInt();
	}

	/** Used by the Low-end preset. */
	void applyLowEnd() {
		this.budget.set(3.0D);
		this.maxChunks.set(2.0D);
	}
}
