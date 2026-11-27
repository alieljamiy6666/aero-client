package net.aeroclient.module.pvp;

import net.aeroclient.event.KeyEvent;
import net.aeroclient.event.TickEvent;
import net.aeroclient.event.WorldLoadEvent;
import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.Category;
import net.aeroclient.module.setting.BooleanSetting;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

/**
 * Tap your sprint key once to keep sprinting, and tap it again to stop. The
 * same for sneaking, if Toggle Sneak is on. A small label shows what you're
 * doing, like "Sprinting (Toggled)".
 *
 * Fair play: it just holds the key down for you, exactly like holding it
 * yourself (newer Minecraft versions have this option built in). Sprinting
 * still follows all the normal rules: hunger, walking forward, and so on.
 */
public class ToggleSprintModule extends TextHudModule {

	private final BooleanSetting toggleSprint = add(
			new BooleanSetting("Toggle Sprint", "Tap the sprint key to start or stop sprinting.", true));
	private final BooleanSetting toggleSneak = add(
			new BooleanSetting("Toggle Sneak", "Tap the sneak key to start or stop sneaking.", false));
	private final BooleanSetting showLabel = add(
			new BooleanSetting("Show Label", "Show a small label like \"Sprinting (Toggled)\".", true));

	private boolean sprintOn = false, sneakOn = false;

	public ToggleSprintModule() {
		super("Toggle Sprint", "Tap once to keep sprinting or sneaking, with a label showing your state.",
				Category.PVP, 0, Anchor.BOTTOM_LEFT, 0.008F, 0.2F);
		listen(KeyEvent.class, this::onKey);
		listen(TickEvent.class, e -> holdKeys());
		listen(WorldLoadEvent.class, e -> this.sneakOn = false); // don't arrive in a new world stuck sneaking
	}

	@Override
	protected void onEnable() {
		this.sprintOn = false;
		this.sneakOn = false;
	}

	@Override
	protected void onDisable() {
		release();
	}

	private void onKey(KeyEvent e) {
		if (!e.isPressed() || e.isRepeat() || this.mc.currentScreen != null) {
			return;
		}
		int key = e.getKey();
		if (this.toggleSprint.isOn() && key == this.mc.gameSettings.keyBindSprint.getKeyCode()) {
			this.sprintOn = !this.sprintOn;
			if (!this.sprintOn) {
				KeyBinding.setKeyBindState(key, false);
			}
		} else if (this.toggleSneak.isOn() && key == this.mc.gameSettings.keyBindSneak.getKeyCode()) {
			this.sneakOn = !this.sneakOn;
			if (!this.sneakOn) {
				KeyBinding.setKeyBindState(key, false);
			}
		}
	}

	/** Every tick: keep the toggled keys held down (but not while a menu is open). */
	private void holdKeys() {
		if (!this.toggleSprint.isOn()) {
			this.sprintOn = false;
		}
		if (!this.toggleSneak.isOn()) {
			this.sneakOn = false;
		}
		if (this.mc.currentScreen != null || this.mc.player == null) {
			return;
		}
		if (this.sprintOn) {
			KeyBinding.setKeyBindState(this.mc.gameSettings.keyBindSprint.getKeyCode(), true);
		}
		if (this.sneakOn) {
			KeyBinding.setKeyBindState(this.mc.gameSettings.keyBindSneak.getKeyCode(), true);
		}
	}

	/** Lets go of anything this module was holding. */
	private void release() {
		if (this.sprintOn && !physicallyDown(this.mc.gameSettings.keyBindSprint.getKeyCode())) {
			KeyBinding.setKeyBindState(this.mc.gameSettings.keyBindSprint.getKeyCode(), false);
		}
		if (this.sneakOn && !physicallyDown(this.mc.gameSettings.keyBindSneak.getKeyCode())) {
			KeyBinding.setKeyBindState(this.mc.gameSettings.keyBindSneak.getKeyCode(), false);
		}
		this.sprintOn = false;
		this.sneakOn = false;
	}

	/** True if the real key (or mouse button) is held right now. */
	private static boolean physicallyDown(int code) {
		if (code < 0) {
			return Mouse.isButtonDown(code + 100);
		}
		return code > 0 && code < 256 && Keyboard.isKeyDown(code);
	}

	// ---------------- the label ----------------

	@Override
	protected long refreshInterval() {
		return 100L;
	}

	private String state() {
		if (isPreview()) {
			return "Sprinting (Toggled)";
		}
		EntityPlayerSP p = this.mc.player;
		if (this.sneakOn) {
			return "Sneaking (Toggled)";
		}
		if (physicallyDown(this.mc.gameSettings.keyBindSneak.getKeyCode())) {
			return "Sneaking (Key Held)";
		}
		if (this.sprintOn) {
			return "Sprinting (Toggled)";
		}
		if (physicallyDown(this.mc.gameSettings.keyBindSprint.getKeyCode())) {
			return "Sprinting (Key Held)";
		}
		if (p.isSprinting()) {
			return "Sprinting (Vanilla)";
		}
		return null;
	}

	@Override
	protected void refresh() {
		String text = this.showLabel.isOn() ? state() : null;
		if (text == null && isEditing()) {
			text = "Sprinting (Toggled)"; // an example, so it can be placed in the HUD editor
		}
		if (text == null) {
			setLineCount(0);
		} else {
			setLine(0, "", text);
			setLineCount(1);
		}
	}
}
