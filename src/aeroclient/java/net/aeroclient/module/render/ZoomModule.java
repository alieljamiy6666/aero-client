package net.aeroclient.module.render;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.KeybindSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;

/**
 * Hold C (or your chosen key) to zoom in, like a spyglass. The zoom glides
 * in and out smoothly, the scroll wheel zooms further in or out while held,
 * and the mouse slows down while zoomed so aiming stays steady.
 *
 * The game asks {@link #applyZoom} for the field of view, {@link #sensitivity}
 * for how fast the mouse turns, and {@link #onScroll} before changing the
 * hotbar slot with the scroll wheel.
 */
public class ZoomModule extends Module {

	private static ZoomModule instance;

	private final KeybindSetting zoomKey = add(new KeybindSetting("Zoom Key", "Hold this key to zoom.", 46)); // C
	private final NumberSetting amount = add(
			new NumberSetting("Zoom Amount", "How far it zooms when you start (2 = twice as close).", 4, 2, 10, 0.5));
	private final BooleanSetting scroll = add(
			new BooleanSetting("Scroll to Zoom", "While zooming, the scroll wheel zooms in and out.", true));
	private final BooleanSetting smooth = add(
			new BooleanSetting("Smooth Zoom", "Glide in and out instead of jumping.", true));
	private final BooleanSetting slowMouse = add(
			new BooleanSetting("Lower Sensitivity", "Turn slower while zoomed so it's easier to aim.", true));

	private float level = 4.0F; // how far in we want to be while the key is held
	private float current = 1.0F; // how far in we are right now (glides toward the target)
	private boolean held = false;
	private long lastUpdate = -1L;

	public ZoomModule() {
		super("Zoom", "Hold C to zoom in smoothly. Scroll to change how far.", Category.RENDER, 0);
		instance = this;
	}

	private static boolean on() {
		return instance != null && instance.isEnabled();
	}

	/** True while the zoom key is held during play (no menu open). */
	private boolean keyHeld() {
		Minecraft mc = this.mc;
		int key = this.zoomKey.getKey();
		if (mc.currentScreen != null || mc.world == null || key == 0) {
			return false;
		}
		return key < 0 ? Mouse.isButtonDown(key + 100) : key < 256 && Keyboard.isKeyDown(key);
	}

	/** Moves the zoom toward where it should be, based on real time since the last frame. */
	private void update() {
		long now = Minecraft.getSystemTime();
		float dt = this.lastUpdate < 0L ? 0.0F : Math.min(0.1F, (now - this.lastUpdate) / 1000.0F);
		this.lastUpdate = now;
		boolean down = isEnabled() && keyHeld();
		if (down && !this.held) {
			this.level = this.amount.getFloat(); // every new zoom starts at the chosen amount
		}
		this.held = down;
		float target = down ? this.level : 1.0F;
		if (!this.smooth.isOn()) {
			this.current = target;
		} else {
			this.current += (target - this.current) * (1.0F - (float) Math.exp(-14.0F * dt));
			if (Math.abs(this.current - target) < 0.001F) {
				this.current = target;
			}
		}
	}

	/** The field of view with zoom applied (unchanged when not zooming). */
	public static float applyZoom(float fov) {
		if (instance == null) {
			return fov;
		}
		instance.update();
		return instance.current > 1.0001F ? fov / instance.current : fov;
	}

	/** A multiplier for mouse turning speed: smaller while zoomed in. */
	public static float sensitivity() {
		if (instance == null || instance.current <= 1.0001F || !instance.slowMouse.isOn()) {
			return 1.0F;
		}
		return 1.0F / instance.current;
	}

	/**
	 * Called with the scroll wheel amount. Returns true if zoom used it (so
	 * the hotbar slot doesn't change).
	 */
	public static boolean onScroll(int wheel) {
		if (!on() || !instance.held || !instance.scroll.isOn()) {
			return false;
		}
		float next = wheel > 0 ? instance.level * 1.2F : instance.level / 1.2F;
		instance.level = Math.max(1.5F, Math.min(30.0F, next));
		return true;
	}

	@Override
	protected void onDisable() {
		this.held = false;
		this.current = 1.0F; // no zoom left behind
	}
}
