package net.aeroclient.theme;

import net.minecraft.client.Minecraft;

/**
 * Smooth movement helpers. "Easing" makes things start fast and slow down
 * at the end (or the other way round), which feels natural instead of robotic.
 *
 * All functions take t from 0 (start) to 1 (end) and return 0 to 1.
 */
public final class Animation {

	private Animation() {
	}

	public static float clamp01(float t) {
		return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
	}

	/** Fast start, gentle stop. Good for things appearing. */
	public static float easeOut(float t) {
		t = 1.0F - clamp01(t);
		return 1.0F - t * t * t;
	}

	/** Gentle start and gentle stop. Good for things sliding around. */
	public static float easeInOut(float t) {
		t = clamp01(t);
		return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3) / 2.0F;
	}

	/** Seconds since the game started, kept small so sine waves stay smooth. */
	public static float seconds() {
		return (float) ((Minecraft.getSystemTime() % 3600000L) / 1000.0D);
	}

	/**
	 * A value that glides between 0 and 1 over a set time, for hover effects,
	 * fades and slides. Call {@link #update} once per frame.
	 */
	public static final class Fader {
		private final float durationMs;
		private float progress = 0.0F;
		private long lastTime = -1L;

		public Fader(float durationMs) {
			this.durationMs = durationMs;
		}

		/** Moves toward 1 while {@code on} is true, toward 0 otherwise. Returns the eased value. */
		public float update(boolean on) {
			long now = Minecraft.getSystemTime();
			if (lastTime < 0L) {
				lastTime = now;
			}
			float step = Math.min(100L, now - lastTime) / durationMs; // cap big gaps (tab switches)
			lastTime = now;
			progress = clamp01(progress + (on ? step : -step));
			return easeInOut(progress);
		}
	}
}
