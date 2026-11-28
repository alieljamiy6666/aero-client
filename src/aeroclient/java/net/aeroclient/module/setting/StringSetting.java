package net.aeroclient.module.setting;

/**
 * A short piece of text, like the message Auto GG sends. Color codes (§) and
 * invisible control characters are removed, because servers kick players who
 * send them.
 */
public class StringSetting extends Setting<String> {

	private final int maxLength;

	public StringSetting(String name, String description, String defaultValue, int maxLength) {
		super(name, description, defaultValue);
		this.maxLength = maxLength;
	}

	/** The longest text allowed. */
	public int getMaxLength() {
		return this.maxLength;
	}

	@Override
	protected String sanitize(String value) {
		if (value == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(Math.min(value.length(), this.maxLength));
		for (int i = 0; i < value.length() && sb.length() < this.maxLength; ++i) {
			char c = value.charAt(i);
			if (c >= ' ' && c != 127 && c != '§') {
				sb.append(c);
			}
		}
		return sb.toString();
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
