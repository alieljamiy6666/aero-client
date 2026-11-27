package net.aeroclient.theme;

/**
 * One place for every Aero Client color, so all screens match.
 *
 * Colors are 0xAARRGGBB numbers: AA = see-through-ness (FF = solid), then
 * red, green and blue. The accent is not final, so a later step can let
 * players pick their own accent color.
 */
public final class Theme {

	/** Near-black page background. */
	public static int background = 0xFF0E1116;
	/** Panels and buttons. */
	public static int panel = 0xFF161B22;
	/** Panels and buttons under the mouse. */
	public static int panelHover = 0xFF1E2733;
	/** Thin borders around panels and buttons. */
	public static int border = 0xFF2B3646;
	/** Normal text. */
	public static int text = 0xFFE5E7EB;
	/** Less important text. */
	public static int textFaded = 0xFF8B949E;
	/** The one bright color that makes Aero Client feel like Aero Client. */
	public static int accent = 0xFF38BDF8;
	/** A lighter version of the accent, for highlights. */
	public static int accentLight = 0xFF7DD3FC;
	/** "It worked" green. */
	public static int success = 0xFF4ADE80;
	/** "Careful" red. */
	public static int danger = 0xFFF87171;

	/** Ready-made accent colors players can pick from. */
	public static final String[] ACCENT_NAMES = { "Sky", "Purple", "Mint", "Pink", "Orange", "Red" };
	public static final int[] ACCENT_COLORS = { 0xFF38BDF8, 0xFFA78BFA, 0xFF34D399, 0xFFF472B6, 0xFFFB923C,
			0xFFF87171 };

	private Theme() {
	}

	/** Changes the accent color everywhere (the light version is worked out automatically). */
	public static void setAccent(int color) {
		accent = color | 0xFF000000;
		accentLight = mix(accent, 0xFFFFFFFF, 0.38F);
	}

	/** Which preset the current accent is, or -1 if it's a custom color. */
	public static int accentPresetIndex() {
		for (int i = 0; i < ACCENT_COLORS.length; i++) {
			if (ACCENT_COLORS[i] == accent) {
				return i;
			}
		}
		return -1;
	}

	/** The same color with a new see-through-ness (0 = invisible, 1 = solid). */
	public static int withAlpha(int color, float alpha) {
		int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
		return (a << 24) | (color & 0x00FFFFFF);
	}

	/** Mixes color {@code a} into color {@code b}: t = 0 gives a, t = 1 gives b. */
	public static int mix(int a, int b, float t) {
		t = Math.max(0.0F, Math.min(1.0F, t));
		int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
				| (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}
}
