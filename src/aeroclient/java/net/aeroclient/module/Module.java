package net.aeroclient.module;

import java.util.ArrayList;
import java.util.List;

import net.aeroclient.AeroClient;
import net.aeroclient.event.Event;
import net.aeroclient.event.EventBus;
import net.minecraft.client.Minecraft;

/**
 * One Aero Client feature that can be turned on and off, like Zoom or
 * Keystrokes.
 *
 * To make a module: extend this class, pass a name, description, category and
 * default key to the constructor, and use {@link #listen} to react to game
 * events. Listeners only receive events while the module is on, so a module
 * that's off costs nothing.
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
	private boolean enabled = false;

	private final List<Subscription<?>> subscriptions = new ArrayList<>();

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

	public final void setKeybind(int keybind) {
		this.keybind = keybind;
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
