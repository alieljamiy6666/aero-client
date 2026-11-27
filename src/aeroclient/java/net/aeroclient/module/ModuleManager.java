package net.aeroclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.aeroclient.AeroClient;
import net.aeroclient.event.KeyEvent;
import net.aeroclient.module.hud.ArmorStatusModule;
import net.aeroclient.module.hud.ClockModule;
import net.aeroclient.module.hud.CoordinatesModule;
import net.aeroclient.module.hud.FpsModule;
import net.aeroclient.module.hud.PingModule;
import net.aeroclient.module.hud.PotionEffectsModule;
import net.aeroclient.module.hud.ServerInfoModule;
import net.aeroclient.module.hud.SpeedModule;
import net.aeroclient.module.misc.TestModule;
import net.minecraft.client.Minecraft;

/**
 * Keeps the list of every Aero Client module, finds them by name, category or
 * type, and turns them on and off when their keys are pressed.
 */
public final class ModuleManager {

	private final List<Module> modules = new ArrayList<>();
	private final List<Module> readOnly = Collections.unmodifiableList(modules);

	/** Registers every module and starts listening for keybinds. Called once at startup. */
	public void init() {
		register(new TestModule());
		// HUD modules
		register(new FpsModule());
		register(new PingModule());
		register(new CoordinatesModule());
		register(new ArmorStatusModule());
		register(new PotionEffectsModule());
		register(new ClockModule());
		register(new ServerInfoModule());
		register(new SpeedModule());

		AeroClient.EVENTS.subscribe(KeyEvent.class, this::onKey);
		AeroClient.LOGGER.info("Registered " + modules.size() + " module(s)");
	}

	public void register(Module module) {
		for (Module m : modules) {
			if (m.getName().equalsIgnoreCase(module.getName())) {
				throw new IllegalArgumentException("Two modules are named " + module.getName());
			}
		}
		modules.add(module);
	}

	/** Every module (this list can't be changed). */
	public List<Module> getModules() {
		return readOnly;
	}

	/** Every module in a category. {@link Category#ALL} gives every module. */
	public List<Module> getModules(Category category) {
		if (category.isView()) {
			return readOnly;
		}
		List<Module> result = new ArrayList<>();
		for (Module m : modules) {
			if (m.getCategory() == category) {
				result.add(m);
			}
		}
		return result;
	}

	/** Finds a module by name, ignoring upper/lower case. Returns null if there isn't one. */
	public Module getModule(String name) {
		for (Module m : modules) {
			if (m.getName().equalsIgnoreCase(name)) {
				return m;
			}
		}
		return null;
	}

	/** Finds a module by its class, e.g. {@code getModule(TestModule.class)}. */
	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> type) {
		for (Module m : modules) {
			if (m.getClass() == type) {
				return (T) m;
			}
		}
		return null;
	}

	/** Toggles every module whose key was just pressed (only while playing, not in menus). */
	private void onKey(KeyEvent event) {
		if (!event.isPressed() || event.isRepeat() || event.getKey() == 0
				|| Minecraft.getMinecraft().currentScreen != null) {
			return;
		}
		for (Module m : modules) {
			if (m.getKeybind() == event.getKey()) {
				m.toggle();
			}
		}
	}
}
