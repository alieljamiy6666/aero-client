package net.aeroclient.util;

import net.aeroclient.AeroClient;
import net.aeroclient.event.MouseEvent;
import net.minecraft.client.Minecraft;

/**
 * Counts clicks per second (CPS) for the left and right mouse buttons.
 *
 * Every real click the game receives is stored with the time it happened,
 * in milliseconds. CPS is simply how many of those times are within the last
 * 1000 ms, so the number goes up the moment you click and back down exactly
 * one second later. Nothing is guessed from game ticks or held buttons.
 *
 * The times live in small ring buffers (the newest click replaces the
 * oldest), so counting is quick and makes no garbage.
 */
public final class ClickTracker {

	private static final int CAPACITY = 64; // nobody clicks 64 times in one second
	private static final long WINDOW_MS = 1000L;

	private static final long[] LEFT = new long[CAPACITY];
	private static final long[] RIGHT = new long[CAPACITY];
	private static int leftNext = 0, rightNext = 0;
	private static boolean installed = false;

	private ClickTracker() {
	}

	/** Starts listening for clicks. Called once at startup. */
	public static void install() {
		if (installed) {
			return;
		}
		installed = true;
		for (int i = 0; i < CAPACITY; i++) {
			LEFT[i] = Long.MIN_VALUE;
			RIGHT[i] = Long.MIN_VALUE;
		}
		AeroClient.EVENTS.subscribe(MouseEvent.class, e -> {
			if (e.isPressed()) {
				click(e.getButton());
			}
		});
	}

	/** Records one click right now (0 = left, 1 = right). */
	public static void click(int button) {
		long now = Minecraft.getSystemTime();
		if (button == 0) {
			LEFT[leftNext] = now;
			leftNext = (leftNext + 1) % CAPACITY;
		} else if (button == 1) {
			RIGHT[rightNext] = now;
			rightNext = (rightNext + 1) % CAPACITY;
		}
	}

	/** Left clicks in the last second. */
	public static int getLeftCps() {
		return count(LEFT);
	}

	/** Right clicks in the last second. */
	public static int getRightCps() {
		return count(RIGHT);
	}

	private static int count(long[] times) {
		long since = Minecraft.getSystemTime() - WINDOW_MS;
		int n = 0;
		for (int i = 0; i < CAPACITY; i++) {
			if (times[i] > since) {
				n++;
			}
		}
		return n;
	}
}
