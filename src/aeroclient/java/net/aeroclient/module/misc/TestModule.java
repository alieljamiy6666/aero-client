package net.aeroclient.module.misc;

import net.aeroclient.event.ChatReceiveEvent;
import net.aeroclient.event.KeyEvent;
import net.aeroclient.event.Render2DEvent;
import net.aeroclient.event.Render3DEvent;
import net.aeroclient.event.ServerJoinEvent;
import net.aeroclient.event.TickEvent;
import net.aeroclient.event.WorldLoadEvent;
import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.KeybindSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentString;

/**
 * A test module that proves the module system and every event work. Press K
 * in game to turn it on: it says so in chat and shows a small panel counting
 * ticks, 3D frames, chat messages, keys, world loads and server joins.
 *
 * It also has one setting of every kind, to test saving and the settings menu.
 */
public class TestModule extends Module {

	private static final int KEY_K = 37;

	private final BooleanSetting showPanel = add(new BooleanSetting("Show Panel", "Show the event counter panel.", true));
	private final ModeSetting corner = add(
			new ModeSetting("Corner", "Which corner the panel sits in.", "Top Left", "Top Left", "Top Right"));
	private final NumberSetting opacity = add(
			new NumberSetting("Opacity", "How solid the panel's background is.", 0.9D, 0.2D, 1.0D, 0.05D));
	private final BooleanSetting showBorder = add(new BooleanSetting("Show Border", "Draw a colored border.", true));
	private final ColorSetting borderColor = add(
			new ColorSetting("Border Color", "The border's color.", 0xFF38BDF8, true));
	private final KeybindSetting resetKey = add(
			new KeybindSetting("Reset Key", "Press this to reset the counters.", 36)); // J

	private int ticks, frames3d, chats, worlds, joins;
	private String lastKey = "-";

	public TestModule() {
		super("Test Module", "Shows that modules and events work. Toggle it with K.", Category.MISC, KEY_K);
		listen(TickEvent.class, e -> this.ticks++);
		listen(Render3DEvent.class, e -> this.frames3d++);
		listen(ChatReceiveEvent.class, e -> this.chats++);
		listen(WorldLoadEvent.class, e -> this.worlds++);
		listen(ServerJoinEvent.class, e -> this.joins++);
		listen(KeyEvent.class, e -> {
			if (e.isPressed()) {
				this.lastKey = Keyboard.getKeyName(e.getKey());
				if (e.getKey() == this.resetKey.getKey() && !e.isRepeat()) {
					this.ticks = this.frames3d = this.chats = this.worlds = this.joins = 0;
				}
			}
		});
		listen(Render2DEvent.class, this::drawPanel);
		this.borderColor.visibleWhen(this.showBorder::isOn); // only shown while the border is on
	}

	@Override
	protected void onEnable() {
		this.ticks = this.frames3d = this.chats = this.worlds = this.joins = 0;
		this.lastKey = "-";
		say("§b[Aero Client]§r Test Module §aenabled§r - press K again to turn it off");
	}

	@Override
	protected void onDisable() {
		say("§b[Aero Client]§r Test Module §cdisabled");
	}

	private void say(String text) {
		if (this.mc != null && this.mc.ingameGUI != null && this.mc.world != null) {
			this.mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(text));
		}
	}

	private void drawPanel(Render2DEvent event) {
		if (this.mc.gameSettings.showDebugInfo || !this.showPanel.isOn()) {
			return; // stay out of the way of the F3 screen
		}
		FontRenderer font = this.mc.fontRendererObj;
		String line1 = "Ticks: " + this.ticks + "   3D frames: " + this.frames3d;
		String line2 = "Chat: " + this.chats + "   Last key: " + this.lastKey;
		String line3 = "Worlds: " + this.worlds + "   Joins: " + this.joins;
		int w = Math.max(font.getStringWidth(line1), Math.max(font.getStringWidth(line2), font.getStringWidth(line3))) + 12;
		int h = 50;
		int x = this.corner.is("Top Right") ? event.getWidth() - w - 4 : 4, y = 4;
		RenderUtil.shadow(x, y + 1, w, h, 6, 5, Theme.withAlpha(0xFF000000, 0.4F));
		if (this.showBorder.isOn()) {
			RenderUtil.roundedRect(x, y, w, h, 6, this.borderColor.getColor());
		}
		RenderUtil.roundedRect(x + 1, y + 1, w - 2, h - 2, 5, Theme.withAlpha(0xFF111720, this.opacity.getFloat()));
		font.drawStringWithShadow("Test Module", x + 6, y + 5, Theme.accentLight);
		font.drawStringWithShadow(line1, x + 6, y + 16, Theme.text);
		font.drawStringWithShadow(line2, x + 6, y + 27, Theme.text);
		font.drawStringWithShadow(line3, x + 6, y + 38, Theme.textFaded);
	}
}
