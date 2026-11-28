package net.aeroclient.module.performance;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.NumberSetting;

/**
 * Keeps the number of particles (smoke, flames, crit stars, block bits...)
 * under a limit, removing the oldest ones first, and doesn't build particles
 * that are behind you. Normal Minecraft allows tens of thousands, which can
 * freeze the game near big farms, fireworks or explosions.
 */
public class ParticleLimitModule extends Module {

	private static ParticleLimitModule instance;

	private final NumberSetting max = add(new NumberSetting("Max Particles",
			"Most particles at once. When there are more, the oldest disappear first.", 1500, 100, 5000, 100));

	public ParticleLimitModule() {
		super("Particle Limit", "Caps how many particles can exist so effects can't freeze the game.",
				Category.RENDER, 0);
		instance = this;
		enabledByDefault();
	}

	/** Most particles allowed right now (no limit when the module is off). */
	public static int limit() {
		return instance != null && instance.isEnabled() ? instance.max.getInt() : Integer.MAX_VALUE;
	}

	/** True if particles behind the camera can be skipped while drawing (first person only). */
	public static boolean skipBehindCamera() {
		return instance != null && instance.isEnabled() && instance.mc.gameSettings.thirdPersonView == 0;
	}

	/** Used by the Low-end preset. */
	void applyLowEnd() {
		this.max.set(400.0D);
	}
}
