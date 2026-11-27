package net.aeroclient.module.setting;

/** An on/off switch. */
public class BooleanSetting extends Setting<Boolean> {

	public BooleanSetting(String name, String description, boolean defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean isOn() {
		return get();
	}

	public void toggle() {
		set(!get());
	}

	@Override
	public Object toJson() {
		return get();
	}

	@Override
	public void fromJson(Object json) {
		if (json instanceof Boolean) {
			setQuietly((Boolean) json);
		}
	}
}
