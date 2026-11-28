package net.aeroclient.module.render;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;

/**
 * Hides rain and snow on your screen: no falling rain, no snow, no gray rainy
 * sky and no rain sounds. The weather itself is unchanged (crops, mobs and
 * everyone else still get rain), and turning it off brings the rain right back.
 *
 * The game asks {@link #hidesWeather} when your world reports how hard it's raining.
 */
public class WeatherToggleModule extends Module {

	private static WeatherToggleModule instance;

	public WeatherToggleModule() {
		super("Weather Toggle", "Turn off rain and snow visuals just for you.", Category.RENDER, 0);
		instance = this;
	}

	/** True while rain and snow should be hidden on your screen. */
	public static boolean hidesWeather() {
		return instance != null && instance.isEnabled();
	}
}
