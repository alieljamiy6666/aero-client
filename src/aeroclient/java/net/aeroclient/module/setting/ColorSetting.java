package net.aeroclient.module.setting;

/**
 * A color, optionally with see-through-ness (alpha). Stored as 0xAARRGGBB
 * and saved as readable text like "#CC38BDF8".
 */
public class ColorSetting extends Setting<Integer> {

	private final boolean allowAlpha;

	public ColorSetting(String name, String description, int defaultColor, boolean allowAlpha) {
		super(name, description, allowAlpha ? defaultColor : (defaultColor | 0xFF000000));
		this.allowAlpha = allowAlpha;
	}

	public boolean allowsAlpha() {
		return allowAlpha;
	}

	public int getColor() {
		return get();
	}

	public int getAlpha() {
		return (get() >>> 24) & 255;
	}

	@Override
	protected Integer sanitize(Integer value) {
		if (value == null) {
			return null;
		}
		return allowAlpha ? value : (value | 0xFF000000);
	}

	@Override
	public Object toJson() {
		String hex = Integer.toHexString(get()).toUpperCase();
		while (hex.length() < 8) {
			hex = "0" + hex;
		}
		return "#" + hex;
	}

	@Override
	public void fromJson(Object json) {
		if (!(json instanceof String)) {
			return;
		}
		String s = ((String) json).trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		try {
			if (s.length() == 6) {
				setQuietly(0xFF000000 | Integer.parseInt(s, 16));
			} else if (s.length() == 8) {
				setQuietly((int) Long.parseLong(s, 16));
			}
		} catch (NumberFormatException e) {
			// not a color: keep the current one
		}
	}
}
