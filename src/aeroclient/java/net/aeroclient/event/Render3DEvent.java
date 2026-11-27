package net.aeroclient.event;

/**
 * Sent every frame after the world is drawn but before your hand, so modules
 * can draw things in the world (like a custom block outline). Reused every
 * frame, so it costs nothing to send.
 */
public final class Render3DEvent extends Event {

	public static final Render3DEvent INSTANCE = new Render3DEvent();

	private float partialTicks;

	private Render3DEvent() {
	}

	/** Fills in this frame's values. Only the game should call this. */
	public Render3DEvent set(float partialTicks) {
		this.partialTicks = partialTicks;
		return this;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}
