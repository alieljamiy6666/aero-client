package net.aeroclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.aeroclient.event.EventBus;
import net.aeroclient.module.ModuleManager;

/**
 * The main Aero Client class. Everything Aero Client does starts here.
 *
 * The game calls {@link #init()} once while it starts up, after the window and
 * the game's resources (textures, fonts, sounds) are ready. Later steps will
 * set up the theme, modules, settings and menus from here.
 */
public final class AeroClient {

	/** The client's name, shown in titles, menus and logs. */
	public static final String NAME = "Aero Client";

	/** The client's version. Raise it for every release: 1.0.1, 1.1.0, and so on. */
	public static final String VERSION = "1.0.0";

	/** Who made Aero Client. */
	public static final String AUTHOR = "AliEljamiy6666";

	/** Prints messages to the browser console, tagged [AeroClient]. */
	public static final Logger LOGGER = LogManager.getLogger("AeroClient");

	/** Passes game events (ticks, frames, keys, chat...) to modules. */
	public static final EventBus EVENTS = new EventBus();

	/** Every Aero Client module. */
	public static final ModuleManager MODULES = new ModuleManager();

	private static boolean initialized = false;

	private AeroClient() {
		// Everything here is static, so nobody needs to create an AeroClient object.
	}

	/**
	 * Starts Aero Client. Safe to call more than once: only the first call does
	 * anything. If something goes wrong in here, the error is logged and the
	 * game keeps running without Aero Client instead of crashing.
	 */
	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		try {
			MODULES.init();
			// Later steps add their setup here: config, menus...
			LOGGER.info(NAME + " " + VERSION + " loaded");
		} catch (Throwable t) {
			LOGGER.error("Aero Client failed to start. The game will keep running without it.");
			LOGGER.error(t);
		}
	}

	/** True once {@link #init()} has run. */
	public static boolean isInitialized() {
		return initialized;
	}

	/** "Aero Client v1.0.0", for menus and the title screen. */
	public static String getDisplayName() {
		return NAME + " v" + VERSION;
	}
}
