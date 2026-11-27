package net.aeroclient.event;

/**
 * Sent at the start of every game tick (20 times per second, while the game
 * is running). There's only one of these, reused every tick, so it costs
 * nothing to send.
 */
public final class TickEvent extends Event {

	public static final TickEvent INSTANCE = new TickEvent();

	private TickEvent() {
	}
}
