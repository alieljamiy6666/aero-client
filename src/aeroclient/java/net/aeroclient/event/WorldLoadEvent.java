package net.aeroclient.event;

import net.minecraft.client.multiplayer.WorldClient;

/**
 * Sent when a world finishes loading (joining a server or singleplayer world,
 * or going through a portal), and when you leave one ({@link #getWorld()} is
 * then null).
 */
public final class WorldLoadEvent extends Event {

	private final WorldClient world;

	public WorldLoadEvent(WorldClient world) {
		this.world = world;
	}

	/** The new world, or null if you just left the world. */
	public WorldClient getWorld() {
		return world;
	}
}
