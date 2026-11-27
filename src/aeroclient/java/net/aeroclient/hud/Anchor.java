package net.aeroclient.hud;

/**
 * Which part of the screen a HUD element is attached to: one of 9 spots in a
 * 3 by 3 grid (corners, edge middles and the center).
 *
 * An element remembers its distance from its anchor instead of from the
 * top-left corner. That way, something placed in the bottom-right corner
 * stays in the bottom-right corner when the window gets bigger or smaller.
 * The anchor is picked automatically from where you drop the element.
 */
public enum Anchor {

	TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
	MIDDLE_LEFT(0, 1), CENTER(1, 1), MIDDLE_RIGHT(2, 1),
	BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);

	private static final Anchor[] VALUES = values();

	/** 0 = left, 1 = center, 2 = right. */
	public final int column;
	/** 0 = top, 1 = middle, 2 = bottom. */
	public final int row;

	Anchor(int column, int row) {
		this.column = column;
		this.row = row;
	}

	/** The anchor for an element whose middle is at (centerX, centerY): whichever third of the screen it's in. */
	public static Anchor pick(float centerX, float centerY, float screenW, float screenH) {
		int column = centerX < screenW / 3.0F ? 0 : (centerX > screenW * 2.0F / 3.0F ? 2 : 1);
		int row = centerY < screenH / 3.0F ? 0 : (centerY > screenH * 2.0F / 3.0F ? 2 : 1);
		return VALUES[row * 3 + column];
	}

	/** Reads an anchor saved by name, or returns {@code fallback} if the name is unknown. */
	public static Anchor fromName(String name, Anchor fallback) {
		for (Anchor a : VALUES) {
			if (a.name().equals(name)) {
				return a;
			}
		}
		return fallback;
	}
}
