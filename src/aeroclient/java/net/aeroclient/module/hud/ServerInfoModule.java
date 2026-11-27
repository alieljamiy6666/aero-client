package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;

/**
 * Shows which server you're on (its name from your server list, or its
 * address) and how many players are online. In singleplayer it says so.
 */
public class ServerInfoModule extends TextHudModule {

	private final BooleanSetting showAddress = add(new BooleanSetting("Show Address",
			"Show the server's address instead of the name from your server list.", false));
	private final BooleanSetting showPlayers = add(
			new BooleanSetting("Show Players", "Show how many players are online.", true));

	private int shownPlayers = -1;
	private String playersText = "";

	public ServerInfoModule() {
		super("Server Info", "Shows the server name and how many players are online.", 0, Anchor.TOP_RIGHT, 0.008F,
				0.065F);
	}

	@Override
	protected long refreshInterval() {
		return 500L;
	}

	private String serverText() {
		if (isPreview()) {
			return this.showAddress.isOn() ? "play.example.net" : "Example Server";
		}
		ServerData data = this.mc.getCurrentServerData();
		if (data == null) {
			return "Singleplayer";
		}
		String text = this.showAddress.isOn() ? data.serverIP : data.serverName;
		return text == null || text.isEmpty() ? (data.serverIP == null ? "-" : data.serverIP) : text;
	}

	private int playerCount() {
		if (isPreview()) {
			return 8;
		}
		NetHandlerPlayClient connection = this.mc.getConnection();
		return connection == null ? 0 : connection.getPlayerInfoMap().size();
	}

	@Override
	protected void refresh() {
		setLine(0, "Server: ", serverText());
		if (this.showPlayers.isOn()) {
			int players = playerCount();
			if (players != this.shownPlayers) {
				this.shownPlayers = players;
				this.playersText = Integer.toString(players);
			}
			setLine(1, "Players: ", this.playersText);
			setLineCount(2);
		} else {
			setLineCount(1);
		}
	}
}
