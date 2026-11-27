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
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentString;

/**
 * A test module that proves the module system and every event work. Press K
 * in game to turn it on: it says so in chat and shows a small panel counting
 * ticks, 3D frames, chat messages, keys, world loads and server joins.
 */
public class TestModule extends Module {

	private static final int KEY_K = 37;

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
			}
		});
		listen(Render2DEvent.class, this::drawPanel);
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
		if (this.mc.ingameGUI != null && this.mc.world != null) {
			this.mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(text));
		}
	}

	private void drawPanel(Render2DEvent event) {
		if (this.mc.gameSettings.showDebugInfo) {
			return; // stay out of the way of the F3 screen
		}
		FontRenderer font = this.mc.fontRendererObj;
		String line1 = "Ticks: " + this.ticks + "   3D frames: " + this.frames3d;
		String line2 = "Chat: " + this.chats + "   Last key: " + this.lastKey;
		String line3 = "Worlds: " + this.worlds + "   Joins: " + this.joins;
		int w = Math.max(font.getStringWidth(line1), Math.max(font.getStringWidth(line2), font.getStringWidth(line3))) + 12;
		int x = 4, y = 4, h = 50;
		RenderUtil.shadow(x, y + 1, w, h, 6, 5, Theme.withAlpha(0xFF000000, 0.4F));
		RenderUtil.roundedRect(x, y, w, h, 6, Theme.withAlpha(Theme.accent, 0.9F));
		RenderUtil.roundedRect(x + 1, y + 1, w - 2, h - 2, 5, 0xE6111720);
		font.drawStringWithShadow("Test Module", x + 6, y + 5, Theme.accentLight);
		font.drawStringWithShadow(line1, x + 6, y + 16, Theme.text);
		font.drawStringWithShadow(line2, x + 6, y + 27, Theme.text);
		font.drawStringWithShadow(line3, x + 6, y + 38, Theme.textFaded);
	}
}
