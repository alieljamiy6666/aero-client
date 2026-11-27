package net.aeroclient.event;

/**
 * Sent when a key is pressed or released while playing (not while a menu is
 * open). Key numbers are Minecraft's keyboard codes, e.g. 37 is K.
 */
public final class KeyEvent extends Event {

	private final int key;
	private final boolean pressed;
	private final boolean repeat;

	public KeyEvent(int key, boolean pressed, boolean repeat) {
		this.key = key;
		this.pressed = pressed;
		this.repeat = repeat;
	}

	public int getKey() {
		return key;
	}

	/** True when the key went down, false when it was let go. */
	public boolean isPressed() {
		return pressed;
	}

	/** True for the automatic repeats while a key is held down. */
	public boolean isRepeat() {
		return repeat;
	}
}
