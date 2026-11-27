package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

/**
 * Shows frames per second. Optionally also the lowest and highest FPS from
 * the last 10 seconds, which shows stutters better than the current number.
 *
 * While it's on, it replaces the small built-in FPS line in the top-left corner.
 */
public class FpsModule extends TextHudModule {

	private static final int WINDOW = 10; // seconds of history for low/high
	private static FpsModule instance;

	private final BooleanSetting showLowHigh = add(new BooleanSetting("Show Low & High",
			"Also show the lowest and highest FPS from the last 10 seconds.", false));

	private final int[] history = new int[WINDOW];
	private int historySize = 0, historyNext = 0;
	private long lastSecond = -1L;

	private int shownFps = -1, shownLow = -1, shownHigh = -1;
	private String fpsText = "", lowHighText = "";

	public FpsModule() {
		super("FPS", "Shows your frames per second.", 0, Anchor.TOP_LEFT, 0.008F, 0.012F);
		instance = this;
	}

	/** True while this module is on, so the game skips its own FPS line. */
	public static boolean hidesVanilla() {
		return instance != null && instance.isEnabled();
	}

	@Override
	protected long refreshInterval() {
		return 250L;
	}

	@Override
	protected void refresh() {
		int fps = Minecraft.getDebugFPS(); // the game counts this once a second
		long second = Minecraft.getSystemTime() / 1000L;
		if (second != this.lastSecond) {
			this.lastSecond = second;
			this.history[this.historyNext] = fps;
			this.historyNext = (this.historyNext + 1) % WINDOW;
			this.historySize = Math.min(WINDOW, this.historySize + 1);
		}
		if (fps != this.shownFps) {
			this.shownFps = fps;
			this.fpsText = Integer.toString(fps);
		}
		setLine(0, "FPS: ", this.fpsText);

		if (this.showLowHigh.isOn()) {
			int low = Integer.MAX_VALUE, high = 0;
			for (int i = 0; i < this.historySize; i++) {
				low = Math.min(low, this.history[i]);
				high = Math.max(high, this.history[i]);
			}
			if (this.historySize == 0) {
				low = high = fps;
			}
			if (low != this.shownLow || high != this.shownHigh) {
				this.shownLow = low;
				this.shownHigh = high;
				this.lowHighText = low + " / " + high;
			}
			setLine(1, "Low/High: ", this.lowHighText);
			setLineCount(2);
		} else {
			setLineCount(1);
		}
	}

	@Override
	protected void onEnable() {
		this.historySize = 0;
		this.historyNext = 0;
	}
}
