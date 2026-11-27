package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.theme.Theme;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

/**
 * Shows your ping: how long messages take to reach the server and come back,
 * in milliseconds. Green is good, yellow is OK, red is laggy. The server
 * sends this number every few seconds, so it doesn't change every frame.
 */
public class PingModule extends TextHudModule {

	public static final int GOOD = 80, OK = 150; // milliseconds
	private static final int YELLOW = 0xFFFACC15;

	private final BooleanSetting colored = add(
			new BooleanSetting("Colored", "Green, yellow or red depending on how good your ping is.", true));

	private int shownPing = Integer.MIN_VALUE;
	private String pingText = "";

	public PingModule() {
		super("Ping", "Shows your connection delay to the server, colored by how good it is.", 0, Anchor.TOP_LEFT,
				0.008F, 0.08F);
	}

	@Override
	protected long refreshInterval() {
		return 500L;
	}

	/** Your ping in milliseconds, or -1 if it isn't known (no server). */
	private int currentPing() {
		if (isPreview()) {
			return 42; // example value for the HUD editor on the title screen
		}
		NetHandlerPlayClient connection = this.mc.getConnection();
		if (connection == null) {
			return -1;
		}
		NetworkPlayerInfo info = connection.getPlayerInfo(this.mc.player.getUniqueID());
		return info == null ? -1 : info.getResponseTime();
	}

	@Override
	protected void refresh() {
		int ping = currentPing();
		if (ping != this.shownPing) {
			this.shownPing = ping;
			this.pingText = ping < 0 ? "-" : ping + " ms";
		}
		int color = Theme.text;
		if (this.colored.isOn() && ping >= 0) {
			color = ping < GOOD ? Theme.success : (ping < OK ? YELLOW : Theme.danger);
		}
		setLine(0, "Ping: ", this.pingText, color);
		setLineCount(1);
	}
}
