package net.aeroclient.module.setting;

/**
 * A number picked with a slider, between {@code min} and {@code max}, moving
 * in jumps of {@code step} (for example 0.5 to 2.0 in steps of 0.1).
 */
public class NumberSetting extends Setting<Double> {

	private final double min, max, step;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	public float getFloat() {
		return get().floatValue();
	}

	public int getInt() {
		return (int) Math.round(get());
	}

	/** Where the value sits between min and max, from 0 to 1 (for drawing sliders). */
	public double getFraction() {
		return max > min ? (get() - min) / (max - min) : 0.0D;
	}

	/** Sets the value from a slider position between 0 and 1. */
	public void setFraction(double fraction) {
		set(min + Math.max(0.0D, Math.min(1.0D, fraction)) * (max - min));
	}

	/** The value as text, without ".0" for whole-number settings. */
	public String format() {
		if (step >= 1.0D && step == Math.rint(step)) {
			return Long.toString(Math.round(get()));
		}
		int decimals = step >= 0.1D ? 1 : (step >= 0.01D ? 2 : 3);
		double p = Math.pow(10, decimals);
		return Double.toString(Math.round(get() * p) / p);
	}

	@Override
	protected Double sanitize(Double value) {
		if (value == null || value.isNaN() || value.isInfinite()) {
			return null;
		}
		double v = Math.max(min, Math.min(max, value));
		if (step > 0.0D) {
			v = min + Math.round((v - min) / step) * step;
			v = Math.max(min, Math.min(max, v));
			v = Math.round(v * 1.0E6D) / 1.0E6D; // tidy floating-point leftovers like 0.30000000004
		}
		return v;
	}

	@Override
	public Object toJson() {
		return get();
	}

	@Override
	public void fromJson(Object json) {
		if (json instanceof Number) {
			setQuietly(((Number) json).doubleValue());
		}
	}
}
