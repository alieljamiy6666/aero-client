package net.aeroclient.module.pvp;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;

/**
 * Optional 1.7-style animations in first person. These only change how your
 * own hand looks on your screen. They send nothing to the server and don't
 * change attacking, blocking, damage, reach or timing in any way.
 *
 * <ul>
 * <li>Sword Block: holding right-click with a sword shows the old "blocking"
 * pose, and you can still swing while in it (the classic blockhit look).
 * In 1.12 this doesn't block anything; it's only the look.</li>
 * <li>No Cooldown Dip: your held item doesn't dip down after every swing
 * (the 1.9+ attack-cooldown animation), like in 1.7. The cooldown itself is
 * unchanged.</li>
 * </ul>
 */
public class OldAnimationsModule extends Module {

	private static OldAnimationsModule instance;

	private final BooleanSetting swordBlock = add(new BooleanSetting("Sword Block",
			"Holding right-click with a sword shows the old blocking pose (looks only).", true));
	private final BooleanSetting noCooldownDip = add(new BooleanSetting("No Cooldown Dip",
			"Your held item doesn't dip after each swing (looks only; the cooldown still works the same).", true));

	public OldAnimationsModule() {
		super("Old Animations", "1.7-style sword block and item animations. Visual only.", Category.PVP, 0);
		instance = this;
	}

	/**
	 * True when this hand should be drawn in the 1.7 sword-block pose: a sword
	 * in the main hand, right-click held, and nothing else being used.
	 */
	public static boolean isSwordBlocking(EntityPlayer player, EnumHand hand, ItemStack stack) {
		if (instance == null || !instance.isEnabled() || !instance.swordBlock.isOn() || hand != EnumHand.MAIN_HAND
				|| !(stack.getItem() instanceof ItemSword) || player.isHandActive()) {
			return false;
		}
		Minecraft mc = Minecraft.getMinecraft();
		return mc.currentScreen == null && mc.gameSettings.keyBindUseItem.isKeyDown();
	}

	/** Turns the sword sideways into the blocking pose (after the normal hand position and swing). */
	public static void applyBlockPose(boolean rightHand) {
		float side = rightHand ? 1.0F : -1.0F;
		GlStateManager.translate(side * -0.1F, 0.14F, 0.12F);
		GlStateManager.rotate(-98.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(side * 16.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(side * 77.0F, 0.0F, 0.0F, 1.0F);
	}

	/** True when the held item shouldn't dip after swinging. */
	public static boolean noCooldownDip() {
		return instance != null && instance.isEnabled() && instance.noCooldownDip.isOn();
	}
}
