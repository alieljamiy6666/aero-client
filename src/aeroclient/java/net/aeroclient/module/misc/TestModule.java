package net.aeroclient.module.misc;

import net.aeroclient.event.ChatReceiveEvent;
import net.aeroclient.event.KeyEvent;
import net.aeroclient.event.Render3DEvent;
import net.aeroclient.event.ServerJoinEvent;
import net.aeroclient.event.TickEvent;
import net.aeroclient.event.WorldLoadEvent;
import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.HudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ColorSetting;
import net.aeroclient.module.setting.KeybindSetting;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentString;

/**
 * A test module that proves the module system, events and the HUD editor
 * work. Press K in game to turn it on: it says so in chat and shows a small
 * panel counting ticks, 3D frames, chat messages, keys, world loads and
 * server joins. Move and resize the panel in the HUD editor.
 *
 * It also has settings of several kinds, to test saving and the settings menu.
 */
public class TestModule extends HudModule {

	private static final int KEY_K = 37;
	private static final String TITLE = "Test Module";

	private final BooleanSetting showBorder = add(new BooleanSetting("Show Border", "Draw a colored border.", true));
	private final ColorSetting borderColor = add(
			new ColorSetting("Border Color", "The border's color.", 0xFF38BDF8, true));
	private final KeybindSetting resetKey = add(
			new KeybindSetting("Reset Key", "Press this to reset the counters.", 36)); // J

	private int ticks, frames3d, chats, worlds, joins;
	private String lastKey = "-";

	// the three text lines, rebuilt only when a number changes (not every frame)
	private final StringBuilder sb = new StringBuilder(48);
	private String line1 = "", line2 = "", line3 = "";
	private int shownTicks = -1, shownFrames = -1, shownChats = -1, shownWorlds = -1, shownJoins = -1;
	private String shownKey = null;

	public TestModule() {
		super("Test Module", "Shows that modules, events and the HUD editor work. Toggle it with K.", KEY_K,
				Anchor.TOP_LEFT, 0.008F, 0.012F);
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

	private void updateLines() {
		if (this.ticks != this.shownTicks || this.frames3d != this.shownFrames) {
			this.shownTicks = this.ticks;
			this.shownFrames = this.frames3d;
			this.sb.setLength(0);
			this.line1 = this.sb.append("Ticks: ").append(this.ticks).append("   3D frames: ").append(this.frames3d)
					.toString();
		}
		if (this.chats != this.shownChats || this.lastKey != this.shownKey) {
			this.shownChats = this.chats;
			this.shownKey = this.lastKey;
			this.sb.setLength(0);
			this.line2 = this.sb.append("Chat: ").append(this.chats).append("   Last key: ").append(this.lastKey)
					.toString();
		}
		if (this.worlds != this.shownWorlds || this.joins != this.shownJoins) {
			this.shownWorlds = this.worlds;
			this.shownJoins = this.joins;
			this.sb.setLength(0);
			this.line3 = this.sb.append("Worlds: ").append(this.worlds).append("   Joins: ").append(this.joins)
					.toString();
		}
	}

	@Override
	protected float getContentWidth() {
		updateLines();
		FontRenderer font = this.mc.fontRendererObj;
		return Math.max(font.getStringWidth(this.line1),
				Math.max(font.getStringWidth(this.line2), font.getStringWidth(this.line3))) + 2;
	}

	@Override
	protected float getContentHeight() {
		return 41.0F;
	}

	@Override
	protected void drawBackground(float width, float height) {
		super.drawBackground(width, height);
		if (this.showBorder.isOn()) {
			// a 1-pixel pixel-art border drawn on top, so a see-through background stays see-through
			int c = this.borderColor.getColor();
			float w = width, h = height;
			RenderUtil.beginBatch(false);
			RenderUtil.batchRect(2, 0, w - 2, 1, c);
			RenderUtil.batchRect(2, h - 1, w - 2, h, c);
			RenderUtil.batchRect(0, 2, 1, h - 2, c);
			RenderUtil.batchRect(w - 1, 2, w, h - 2, c);
			RenderUtil.batchRect(1, 1, 2, 2, c);
			RenderUtil.batchRect(w - 2, 1, w - 1, 2, c);
			RenderUtil.batchRect(1, h - 2, 2, h - 1, c);
			RenderUtil.batchRect(w - 2, h - 2, w - 1, h - 1, c);
			RenderUtil.endBatch();
		}
	}

	@Override
	protected void renderContent(boolean editing) {
		updateLines();
		FontRenderer font = this.mc.fontRendererObj;
		font.drawStringWithShadow(TITLE, 1, 0, Theme.accentLight);
		font.drawStringWithShadow(this.line1, 1, 11, Theme.text);
		font.drawStringWithShadow(this.line2, 1, 22, Theme.text);
		font.drawStringWithShadow(this.line3, 1, 33, Theme.textFaded);
	}
}
