package net.aeroclient.event;

/**
 * Sent every frame right after the vanilla HUD is drawn, so modules can draw
 * their own HUD elements on top. Coordinates are GUI coordinates (the same
 * ones the hotbar uses). Reused every frame, so it costs nothing to send.
 */
public final class Render2DEvent extends Event {

	public static final Render2DEvent INSTANCE = new Render2DEvent();

	private float partialTicks;
	private int width, height;

	private Render2DEvent() {
	}

	/** Fills in this frame's values. Only the game should call this. */
	public Render2DEvent set(float partialTicks, int width, int height) {
		this.partialTicks = partialTicks;
		this.width = width;
		this.height = height;
		return this;
	}

	/** How far we are between two ticks (0 to 1), for smooth movement. */
	public float getPartialTicks() {
		return partialTicks;
	}

	/** Screen width in GUI coordinates. */
	public int getWidth() {
		return width;
	}

	/** Screen height in GUI coordinates. */
	public int getHeight() {
		return height;
	}
}
