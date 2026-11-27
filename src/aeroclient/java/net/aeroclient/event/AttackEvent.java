package net.aeroclient.event;

import net.minecraft.entity.Entity;

/**
 * Sent when you attack (left-click) an entity. It's only a notice for
 * modules like the Combo Counter: it can't change or cancel the attack.
 * Reused for every attack, so it costs nothing to send.
 */
public final class AttackEvent extends Event {

	public static final AttackEvent INSTANCE = new AttackEvent();

	private Entity target;

	private AttackEvent() {
	}

	/** Fills in this attack's target. Only the game should call this. */
	public AttackEvent set(Entity target) {
		this.target = target;
		return this;
	}

	/** The entity you hit. */
	public Entity getTarget() {
		return target;
	}
}
