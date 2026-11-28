package net.aeroclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import net.aeroclient.AeroClient;
import net.aeroclient.config.ConfigManager;
import net.aeroclient.event.Event;
import net.aeroclient.event.EventBus;
import net.aeroclient.module.setting.Setting;
import net.minecraft.client.Minecraft;

/**
 * One Aero Client feature that can be turned on and off, like Zoom or
 * Keystrokes.
 *
 * To make a module: extend this class, pass a name, description, category and
 * default key to the constructor, and use {@link #listen} to react to game
 * events. Listeners only receive events while the module is on, so a module
 * that's off costs nothing. Options go in settings made with {@link #add}.
 *
 * <pre>
 * public class MyModule extends Module {
 *     public MyModule() {
 *         super("My Module", "Does something cool.", Category.MISC, 0);
 *         listen(TickEvent.class, e -&gt; doSomethingEveryTick());
 *     }
 * }
 * </pre>
 */
public abstract class Module {

	/** Shortcut to the game. */
	protected final Minecraft mc = Minecraft.getMinecraft();

	private final String name;
	private final String description;
	private final Category category;
	private int keybind;
	private final int defaultKeybind;
	private boolean enabled = false;
	private boolean enabledByDefault = false;

	private final List<Subscription<?>> subscriptions = new ArrayList<>();
	private final List<Setting<?>> settings = new ArrayList<>();
	private final List<Setting<?>> settingsView = Collections.unmodifiableList(settings);

	/**
	 * @param keybind default toggle key (a Minecraft key code, e.g. 37 for K),
	 *                or 0 for no key
	 */
	protected Module(String name, String description, Category category, int keybind) {
		if (category == Category.ALL) {
			throw new IllegalArgumentException("ALL is a view, not a category: " + name);
		}
		this.name = name;
		this.description = description;
		this.category = category;
		this.keybind = keybind;
		this.defaultKeybind = keybind;
	}

	/** Adds an option to this module and returns it, e.g. {@code scale = add(new NumberSetting(...))}. */
	protected final <S extends Setting<?>> S add(S setting) {
		for (Setting<?> s : this.settings) {
			if (s.getName().equalsIgnoreCase(setting.getName())) {
				throw new IllegalArgumentException(this.name + " has two settings named " + setting.getName());
			}
		}
		this.settings.add(setting);
		return setting;
	}

	/** This module's options, in the order they were added. */
	public final List<Setting<?>> getSettings() {
		return this.settingsView;
	}

	/** Finds one of this module's settings by name, or null. */
	public final Setting<?> getSetting(String name) {
		for (Setting<?> s : this.settings) {
			if (s.getName().equalsIgnoreCase(name)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Makes this module start turned on for new players and new profiles (used
	 * by the performance modules). Call it from the constructor.
	 */
	protected final void enabledByDefault() {
		this.enabledByDefault = true;
	}

	/** True if this module starts turned on for new players and new profiles. */
	public final boolean isEnabledByDefault() {
		return this.enabledByDefault;
	}

	/**
	 * Turns the module off and puts its key and every setting back to default
	 * (used when switching profiles). The config then turns it back on if it
	 * was saved on, or if it's on by default and nothing was saved.
	 */
	public void resetToDefaults() {
		setEnabled(false);
		this.keybind = this.defaultKeybind;
		for (Setting<?> s : this.settings) {
			s.restoreDefault();
		}
	}

	/**
	 * Puts every setting and the toggle key back to default and saves, but
	 * leaves the module on or off as it is (the menu's "Reset to defaults").
	 */
	public void resetSettings() {
		for (Setting<?> s : this.settings) {
			s.reset();
		}
		setKeybind(this.defaultKeybind);
	}

	/** Saves anything extra a module needs (HUD modules save their position here). */
	public void saveExtra(JSONObject json) {
	}

	/** Loads what {@link #saveExtra} saved. Must cope with missing or broken values. */
	public void loadExtra(JSONObject json) {
	}

	/** Called when the module turns on. */
	protected void onEnable() {
	}

	/** Called when the module turns off. */
	protected void onDisable() {
	}

	/** React to an event of this type, but only while the module is on. */
	protected final <E extends Event> void listen(Class<E> type, EventBus.Listener<E> listener) {
		Subscription<E> sub = new Subscription<>(type, listener);
		this.subscriptions.add(sub);
		if (this.enabled) {
			sub.subscribe();
		}
	}

	public final void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		ConfigManager.markDirty();
		try {
			if (enabled) {
				for (Subscription<?> sub : this.subscriptions) {
					sub.subscribe();
				}
				onEnable();
			} else {
				onDisable();
				for (Subscription<?> sub : this.subscriptions) {
					sub.unsubscribe();
				}
			}
		} catch (Throwable t) {
			AeroClient.LOGGER.error("Module " + this.name + " failed while turning " + (enabled ? "on" : "off"));
			AeroClient.LOGGER.error(t);
		}
	}

	public final void toggle() {
		setEnabled(!this.enabled);
	}

	public final boolean isEnabled() {
		return this.enabled;
	}

	public final String getName() {
		return this.name;
	}

	public final String getDescription() {
		return this.description;
	}

	public final Category getCategory() {
		return this.category;
	}

	/** The toggle key (a Minecraft key code), or 0 for none. */
	public final int getKeybind() {
		return this.keybind;
	}

	/** The toggle key this module comes with. */
	public final int getDefaultKeybind() {
		return this.defaultKeybind;
	}

	public final void setKeybind(int keybind) {
		if (this.keybind != keybind) {
			this.keybind = keybind;
			ConfigManager.markDirty();
		}
	}

	/** One event type + listener pair, remembered so it can be turned on and off. */
	private static final class Subscription<E extends Event> {
		private final Class<E> type;
		private final EventBus.Listener<E> listener;

		Subscription(Class<E> type, EventBus.Listener<E> listener) {
			this.type = type;
			this.listener = listener;
		}

		void subscribe() {
			AeroClient.EVENTS.subscribe(type, listener);
		}

		void unsubscribe() {
			AeroClient.EVENTS.unsubscribe(type, listener);
		}
	}
}
