package net.aeroclient.module.misc;

import net.aeroclient.event.KeyEvent;
import net.aeroclient.event.Render3DEvent;
import net.aeroclient.event.TickEvent;
import net.aeroclient.gui.AeroScreenshotsScreen;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.KeybindSetting;
import net.aeroclient.screenshot.Screenshots;
import net.lax1dude.eaglercraft.Keyboard;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Screenshots you take with F2 are also kept inside the game, and a small
 * gallery (press G, or Aero Settings, Screenshots) lets you look through them,
 * download one again or delete ones you don't want. Normal F2 downloads still
 * happen too, unless you turn that off.
 */
public class ScreenshotManagerModule extends Module {

	private static ScreenshotManagerModule instance;

	private final KeybindSetting galleryKey = add(new KeybindSetting("Gallery Key",
			"Press this while playing to open your screenshots.", 34)); // G
	private final BooleanSetting download = add(new BooleanSetting("Also Download",
			"Also download each screenshot as a file, like normal F2 does.", true));

	public ScreenshotManagerModule() {
		super("Screenshot Manager", "Keeps your F2 screenshots in an in-game gallery.", Category.MISC, 0);
		instance = this;
		enabledByDefault();
		listen(KeyEvent.class, e -> {
			if (e.isPressed() && !e.isRepeat() && e.getKey() == this.galleryKey.getKey() && e.getKey() != 0
					&& this.mc.currentScreen == null) {
				this.mc.displayGuiScreen(new AeroScreenshotsScreen(null));
			}
		});
		// saving a screenshot is spread over a few frames so the game doesn't freeze
		listen(Render3DEvent.class, e -> Screenshots.work(3000000L));
		listen(TickEvent.class, e -> Screenshots.work(3000000L));
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/**
	 * F2 was pressed. Returns true if Screenshot Manager took the screenshot
	 * (false: the game does its normal screenshot).
	 */
	public static boolean onScreenshotKey() {
		if (!active()) {
			return false;
		}
		String name = Screenshots.capture();
		if (name == null) {
			return false;
		}
		if (instance.download.isOn()) {
			instance.mc.ingameGUI.getChatGUI().printChatMessage(ScreenShotHelper.saveScreenshot());
		}
		int key = instance.galleryKey.getKey();
		String keyName = key == 0 ? null : Keyboard.getKeyName(key);
		ITextComponent msg = new TextComponentString("Screenshot saved to your Aero gallery"
				+ (keyName == null ? "." : " (press " + keyName + " to see it)."));
		msg.getStyle().setColor(TextFormatting.AQUA);
		instance.mc.ingameGUI.getChatGUI().printChatMessage(msg);
		return true;
	}

	/** Keeps saving screenshots while a menu is open (the gallery calls this). */
	public static void keepSaving() {
		Screenshots.work(3000000L);
	}
}
