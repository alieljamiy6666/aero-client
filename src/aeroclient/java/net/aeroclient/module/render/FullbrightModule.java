package net.aeroclient.module.render;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * Makes everything bright, like a very high brightness setting. It only
 * raises the brightness used to draw blocks you can already see; it never
 * makes blocks see-through, so nothing hidden is revealed.
 *
 * It doesn't touch your saved brightness option: the game asks
 * {@link #gamma} each time it works out the lighting. Turning it on or off
 * fades smoothly over a moment.
 */
public class FullbrightModule extends Module {

	private static FullbrightModule instance;
	private static final float FADE_MS = 350.0F;

	private final NumberSetting brightness = add(
			new NumberSetting("Brightness", "How bright (1 = the game's \"Bright\" setting).", 10, 1, 15, 0.5));

	private float blend = 0.0F; // 0 = your normal brightness, 1 = fully bright
	private long lastUpdate = -1L;

	public FullbrightModule() {
		super("Fullbright", "Makes everything bright without revealing hidden blocks.", Category.RENDER, 0);
		instance = this;
	}

	/** The brightness the game should use, given your normal brightness setting. */
	public static float gamma(float normal) {
		if (instance == null) {
			return normal;
		}
		long now = Minecraft.getSystemTime();
		float dt = instance.lastUpdate < 0L ? 0.0F : Math.min(100.0F, now - instance.lastUpdate);
		instance.lastUpdate = now;
		float step = dt / FADE_MS;
		instance.blend = instance.isEnabled() ? Math.min(1.0F, instance.blend + step)
				: Math.max(0.0F, instance.blend - step);
		if (instance.blend <= 0.0F) {
			return normal;
		}
		float target = Math.max(normal, instance.brightness.getFloat());
		return normal + (target - normal) * instance.blend;
	}
}
