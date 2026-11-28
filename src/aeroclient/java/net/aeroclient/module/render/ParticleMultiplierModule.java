package net.aeroclient.module.render;

import java.util.Random;

import net.aeroclient.event.AttackEvent;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;

/**
 * More (or fewer) hit particles: the critical-hit stars and the blue
 * enchantment sparkles you see when hitting something. Visual only.
 *
 * The game asks {@link #emitterCount} how many bursts to make each time it
 * would make one burst of hit particles.
 */
public class ParticleMultiplierModule extends Module {

	private static ParticleMultiplierModule instance;

	private final NumberSetting multiplier = add(new NumberSetting("Multiplier",
			"How many hit particles compared to normal (0 = none, 2 = twice as many).", 2, 0, 10, 0.5));
	private final BooleanSetting alwaysCrits = add(new BooleanSetting("Crits On Every Hit",
			"Show critical-hit stars on every hit, not just real critical hits (looks only).", false));

	private final Random random = new Random();

	public ParticleMultiplierModule() {
		super("Particle Multiplier", "More or fewer hit particles.", Category.RENDER, 0);
		instance = this;
		listen(AttackEvent.class, e -> {
			if (this.alwaysCrits.isOn() && e.getTarget() instanceof EntityLivingBase && this.mc.effectRenderer != null) {
				this.mc.effectRenderer.emitParticleAtEntity(e.getTarget(), EnumParticleTypes.CRIT);
			}
		});
	}

	/** How many particle bursts to make where the game would make one. */
	public static int emitterCount(EnumParticleTypes type) {
		if (instance == null || !instance.isEnabled()
				|| (type != EnumParticleTypes.CRIT && type != EnumParticleTypes.CRIT_MAGIC)) {
			return 1;
		}
		double m = instance.multiplier.get();
		int whole = (int) Math.floor(m);
		return whole + (instance.random.nextDouble() < m - whole ? 1 : 0);
	}
}
