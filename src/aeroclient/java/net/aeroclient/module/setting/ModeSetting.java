package net.aeroclient.module.setting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Pick one choice from a list, like "12-hour" or "24-hour". */
public class ModeSetting extends Setting<String> {

	private final List<String> modes;

	public ModeSetting(String name, String description, String defaultMode, String... modes) {
		super(name, description, defaultMode);
		this.modes = Collections.unmodifiableList(Arrays.asList(modes));
		if (!this.modes.contains(defaultMode)) {
			throw new IllegalArgumentException(name + ": default \"" + defaultMode + "\" isn't one of the choices");
		}
	}

	public List<String> getModes() {
		return modes;
	}

	public boolean is(String mode) {
		return get().equals(mode);
	}

	/** Moves to the next choice, wrapping around at the end. */
	public void cycle() {
		set(modes.get((modes.indexOf(get()) + 1) % modes.size()));
	}

	@Override
	protected String sanitize(String value) {
		return modes.contains(value) ? value : null; // unknown choices (from an old config) are ignored
	}

	@Override
	public Object toJson() {
		return get();
	}

	@Override
	public void fromJson(Object json) {
		if (json instanceof String) {
			setQuietly((String) json);
		}
	}
}
