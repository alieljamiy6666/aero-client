package net.aeroclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.aeroclient.AeroClient;
import net.aeroclient.event.KeyEvent;
import net.aeroclient.module.hud.ArmorStatusModule;
import net.aeroclient.module.hud.ClockModule;
import net.aeroclient.module.hud.CoordinatesModule;
import net.aeroclient.module.hud.CpsModule;
import net.aeroclient.module.hud.FpsModule;
import net.aeroclient.module.hud.KeystrokesModule;
import net.aeroclient.module.hud.PingModule;
import net.aeroclient.module.hud.PotionEffectsModule;
import net.aeroclient.module.hud.ServerInfoModule;
import net.aeroclient.module.hud.SpeedModule;
import net.aeroclient.module.misc.AutoGGModule;
import net.aeroclient.module.misc.BetterChatModule;
import net.aeroclient.module.misc.ChatCopyModule;
import net.aeroclient.module.misc.ChatStackingModule;
import net.aeroclient.module.misc.ChatTimestampsModule;
import net.aeroclient.module.misc.NameMentionModule;
import net.aeroclient.module.misc.ScreenshotManagerModule;
import net.aeroclient.module.misc.TestModule;
import net.aeroclient.module.performance.BackgroundFpsModule;
import net.aeroclient.module.performance.EntityCullingModule;
import net.aeroclient.module.performance.FastTextModule;
import net.aeroclient.module.performance.ParticleLimitModule;
import net.aeroclient.module.performance.SmartChunkUpdatesModule;
import net.aeroclient.module.pvp.ComboCounterModule;
import net.aeroclient.module.pvp.CrosshairModule;
import net.aeroclient.module.pvp.HitColorModule;
import net.aeroclient.module.pvp.LowFireShieldModule;
import net.aeroclient.module.pvp.OldAnimationsModule;
import net.aeroclient.module.pvp.ToggleSprintModule;
import net.aeroclient.module.render.BlockOutlineModule;
import net.aeroclient.module.render.FullbrightModule;
import net.aeroclient.module.render.NametagsModule;
import net.aeroclient.module.render.ParticleMultiplierModule;
import net.aeroclient.module.render.TimeChangerModule;
import net.aeroclient.module.render.WeatherToggleModule;
import net.aeroclient.module.render.ZoomModule;
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
		register(new KeystrokesModule());
		register(new CpsModule());
		// PvP modules (fair play: visuals and comfort only)
		register(new ToggleSprintModule());
		register(new CrosshairModule());
		register(new HitColorModule());
		register(new ComboCounterModule());
		register(new OldAnimationsModule());
		register(new LowFireShieldModule());
		// Render modules (how the world looks, on your screen only)
		register(new ZoomModule());
		register(new FullbrightModule());
		register(new BlockOutlineModule());
		register(new TimeChangerModule());
		register(new WeatherToggleModule());
		register(new ParticleMultiplierModule());
		register(new NametagsModule());
		// Performance modules (on by default; the Low-end preset in Aero Settings turns them all on)
		register(new EntityCullingModule());
		register(new ParticleLimitModule());
		register(new SmartChunkUpdatesModule());
		register(new FastTextModule());
		register(new BackgroundFpsModule());
		// Misc modules (chat and screenshots)
		register(new ChatTimestampsModule());
		register(new NameMentionModule());
		register(new ChatStackingModule());
		register(new ChatCopyModule());
		register(new BetterChatModule());
		register(new AutoGGModule());
		register(new ScreenshotManagerModule());

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
