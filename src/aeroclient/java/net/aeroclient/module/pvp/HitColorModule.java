package net.aeroclient.module.pvp;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.ColorSetting;

/**
 * Changes the red flash an entity gets when it's hit. Pick any color; the
 * color's see-through amount (alpha) is how strong the flash is. Only how it
 * looks on your screen changes.
 *
 * The game asks {@link #red}, {@link #green} and {@link #blue} for the flash
 * color while drawing a hurt entity.
 */
public class HitColorModule extends Module {

	private static HitColorModule instance;

	private final ColorSetting color = add(new ColorSetting("Hit Color",
			"The flash color. Its see-through amount (alpha) sets how strong the flash is.", 0x66FF0000, true));

	public HitColorModule() {
		super("Hit Color", "Change the red flash color when an entity gets hit.", Category.PVP, 0);
		instance = this;
	}

	/** True when the game should use this module's color instead of red. */
	public static boolean isActive() {
		return instance != null && instance.isEnabled();
	}

	private static float channel(int shift, boolean hurt) {
		if (!hurt) {
			return 0.0F;
		}
		int c = instance.color.getColor();
		float strength = ((c >>> 24) & 255) / 255.0F;
		return ((c >> shift) & 255) / 255.0F * strength;
	}

	/** How much red to add to a hurt entity (0 when it isn't hurt). */
	public static float red(boolean hurt) {
		return channel(16, hurt);
	}

	public static float green(boolean hurt) {
		return channel(8, hurt);
	}

	public static float blue(boolean hurt) {
		return channel(0, hurt);
	}
}
