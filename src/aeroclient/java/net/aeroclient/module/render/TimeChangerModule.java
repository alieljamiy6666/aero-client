package net.aeroclient.module.render;

import net.aeroclient.event.TickEvent;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.module.setting.NumberSetting;

/**
 * Pick a time of day just for yourself: always noon, always sunset, always
 * night... Only your screen changes; the server's time (and everyone
 * else's) stays the same. Turning it off puts the real time straight back.
 *
 * The game tells this module the server's time with {@link #receiveServerTime}
 * instead of applying it while the module is on.
 */
public class TimeChangerModule extends Module {

	private static TimeChangerModule instance;

	private final ModeSetting time = add(new ModeSetting("Time", "Which time of day to show.", "Noon", "Morning",
			"Noon", "Sunset", "Night", "Midnight", "Custom"));
	private final NumberSetting customHour = add(
			new NumberSetting("Custom Hour", "The hour to show, from 0 to 24 (13.5 = 1:30 PM).", 12, 0, 24, 0.5));

	private long serverTime = -1L; // the real time from the server, kept for when this turns off

	public TimeChangerModule() {
		super("Time Changer", "Pick a time of day just for yourself.", Category.RENDER, 0);
		instance = this;
		this.customHour.visibleWhen(() -> this.time.is("Custom"));
		listen(TickEvent.class, e -> apply());
	}

	/** Minecraft time (0 = 6 AM, 6000 = noon, 18000 = midnight) for the chosen setting. */
	private long chosenTime() {
		switch (this.time.get()) {
		case "Morning":
			return 1000L;
		case "Sunset":
			return 12500L;
		case "Night":
			return 15000L;
		case "Midnight":
			return 18000L;
		case "Custom":
			return (long) ((((this.customHour.get() - 6.0D) % 24.0D) + 24.0D) % 24.0D * 1000.0D);
		default:
			return 6000L;
		}
	}

	private void apply() {
		if (this.mc.world != null) {
			if (this.serverTime < 0L) {
				this.serverTime = this.mc.world.getWorldTime();
			}
			this.mc.world.setWorldTime(chosenTime());
		}
	}

	@Override
	protected void onEnable() {
		this.serverTime = -1L;
		apply();
	}

	@Override
	protected void onDisable() {
		if (this.mc.world != null && this.serverTime >= 0L) {
			this.mc.world.setWorldTime(this.serverTime); // back to the real time right away
		}
		this.serverTime = -1L;
	}

	/**
	 * The server sent its time. Returns true if the game should use it, or
	 * false while this module is choosing the time (it's remembered instead).
	 */
	public static boolean receiveServerTime(long worldTime) {
		if (instance == null || !instance.isEnabled()) {
			return true;
		}
		instance.serverTime = worldTime;
		return false;
	}
}
