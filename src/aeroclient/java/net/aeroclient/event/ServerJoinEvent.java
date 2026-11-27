package net.aeroclient.event;

/**
 * Sent once when you've joined a server or a singleplayer world and the
 * server has said hello.
 */
public final class ServerJoinEvent extends Event {

	private final String address;
	private final boolean singleplayer;

	public ServerJoinEvent(String address, boolean singleplayer) {
		this.address = address;
		this.singleplayer = singleplayer;
	}

	/** The server's address, or null in singleplayer. */
	public String getAddress() {
		return address;
	}

	public boolean isSingleplayer() {
		return singleplayer;
	}
}
