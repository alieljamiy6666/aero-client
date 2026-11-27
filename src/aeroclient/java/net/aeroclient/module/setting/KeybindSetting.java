package net.aeroclient.module.setting;

import net.lax1dude.eaglercraft.Keyboard;

/** A key on the keyboard (a Minecraft key code, 0 = none), for example "Zoom Key". */
public class KeybindSetting extends Setting<Integer> {

	public KeybindSetting(String name, String description, int defaultKey) {
		super(name, description, defaultKey);
	}

	public int getKey() {
		return get();
	}

	/** The key's name for menus, like "C", or "None". */
	public String getKeyName() {
		return get() == 0 ? "None" : Keyboard.getKeyName(get());
	}

	@Override
	protected Integer sanitize(Integer value) {
		return value == null || value < 0 || value > 512 ? null : value;
	}

	@Override
	public Object toJson() {
		return get();
	}

	@Override
	public void fromJson(Object json) {
		if (json instanceof Number) {
			setQuietly(((Number) json).intValue());
		}
	}
}
