package net.aeroclient.event;

import java.util.HashMap;
import java.util.Map;

import net.aeroclient.AeroClient;

/**
 * Passes game events to whoever is listening, like a mailman: the game
 * {@link #post posts} an event, and every listener that {@link #subscribe
 * subscribed} to that kind of event gets it.
 *
 * Built to be fast enough to call every frame:
 * - Listeners are kept in plain arrays per event type, so sending an event is
 *   one map lookup and a loop. Nothing new is created while sending.
 * - Adding or removing a listener copies the array, so it's safe to do even
 *   while an event is being sent (for example, a module turning itself off).
 * - If a listener throws an error, it's logged and the rest still run, so one
 *   broken module can't crash the game.
 */
public final class EventBus {

	/** Something that wants to hear about one kind of event. */
	public interface Listener<E extends Event> {
		void onEvent(E event);
	}

	private static final Listener<?>[] NONE = new Listener<?>[0];

	private final Map<Class<?>, Listener<?>[]> listeners = new HashMap<>();
	private int errorsLogged = 0;

	/** Start sending events of {@code type} to {@code listener}. */
	public synchronized <E extends Event> void subscribe(Class<E> type, Listener<E> listener) {
		Listener<?>[] old = listeners.getOrDefault(type, NONE);
		for (Listener<?> l : old) {
			if (l == listener) {
				return; // already subscribed
			}
		}
		Listener<?>[] grown = new Listener<?>[old.length + 1];
		System.arraycopy(old, 0, grown, 0, old.length);
		grown[old.length] = listener;
		listeners.put(type, grown);
	}

	/** Stop sending events of {@code type} to {@code listener}. */
	public synchronized <E extends Event> void unsubscribe(Class<E> type, Listener<E> listener) {
		Listener<?>[] old = listeners.get(type);
		if (old == null) {
			return;
		}
		int index = -1;
		for (int i = 0; i < old.length; i++) {
			if (old[i] == listener) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			return;
		}
		if (old.length == 1) {
			listeners.remove(type);
			return;
		}
		Listener<?>[] shrunk = new Listener<?>[old.length - 1];
		System.arraycopy(old, 0, shrunk, 0, index);
		System.arraycopy(old, index + 1, shrunk, index, old.length - index - 1);
		listeners.put(type, shrunk);
	}

	/** Sends {@code event} to everyone listening for its type, then returns it. */
	@SuppressWarnings("unchecked")
	public <E extends Event> E post(E event) {
		Listener<?>[] targets = listeners.get(event.getClass());
		if (targets == null) {
			return event;
		}
		for (Listener<?> listener : targets) {
			try {
				((Listener<E>) listener).onEvent(event);
			} catch (Throwable t) {
				if (errorsLogged < 20) { // don't flood the console if it fails every frame
					errorsLogged++;
					AeroClient.LOGGER.error("A listener failed while handling " + event.getClass().getSimpleName());
					AeroClient.LOGGER.error(t);
				}
			}
		}
		return event;
	}

	/** How many listeners are waiting for this kind of event. */
	public int countListeners(Class<? extends Event> type) {
		Listener<?>[] targets = listeners.get(type);
		return targets == null ? 0 : targets.length;
	}
}
