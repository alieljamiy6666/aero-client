package net.aeroclient.module.pvp;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.NumberSetting;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;

/**
 * Lowers the fire overlay you see while burning, and the shield in your
 * hand, so they cover less of the screen. Only how they're drawn changes;
 * the shield blocks exactly the same.
 */
public class LowFireShieldModule extends Module {

	private static LowFireShieldModule instance;

	private final NumberSetting fireHeight = add(new NumberSetting("Fire Lower By",
			"How far to lower the burning overlay (0 = normal).", 0.25, 0.0, 0.5, 0.05));
	private final NumberSetting shieldHeight = add(new NumberSetting("Shield Lower By",
			"How far to lower your shield (0 = normal).", 0.12, 0.0, 0.5, 0.02));

	public LowFireShieldModule() {
		super("Low Fire & Shield", "Lowers the fire overlay and your shield so they block less of the screen.",
				Category.PVP, 0);
		instance = this;
	}

	/** How far down to move the fire overlay (0 when the module is off). */
	public static float fireOffset() {
		return instance != null && instance.isEnabled() ? instance.fireHeight.getFloat() : 0.0F;
	}

	/** How far down to move this held item: only shields, and 0 when the module is off. */
	public static float shieldOffset(ItemStack stack) {
		if (instance == null || !instance.isEnabled() || stack == null || !(stack.getItem() instanceof ItemShield)) {
			return 0.0F;
		}
		return instance.shieldHeight.getFloat();
	}
}
