package net.aeroclient.event;

/**
 * Sent when a mouse button is pressed or released while playing (not while
 * a menu is open). Button 0 is left, 1 is right, 2 is the middle button.
 * Reused for every click, so it costs nothing to send.
 */
public final class MouseEvent extends Event {

	public static final MouseEvent INSTANCE = new MouseEvent();

	private int button;
	private boolean pressed;

	private MouseEvent() {
	}

	/** Fills in this click's values. Only the game should call this. */
	public MouseEvent set(int button, boolean pressed) {
		this.button = button;
		this.pressed = pressed;
		return this;
	}

	public int getButton() {
		return button;
	}

	/** True when the button went down, false when it was let go. */
	public boolean isPressed() {
		return pressed;
	}
}
