package net.aeroclient.module.setting;

import java.util.function.BooleanSupplier;

import net.aeroclient.config.ConfigManager;

/**
 * One option on a module, like "Show Background" or "Scale". Every setting
 * has a name, a short description, a default value and a current value.
 *
 * A setting can be hidden until another setting is turned on, with
 * {@link #visibleWhen}, for example "Background Color" only shows when
 * "Show Background" is on.
 *
 * Changing a value tells the {@link ConfigManager}, which saves it a moment
 * later, so options survive closing the browser tab.
 *
 * @param <T> the kind of value: true/false, a number, a color...
 */
public abstract class Setting<T> {

	private final String name;
	private final String description;
	private final T defaultValue;
	private T value;
	private BooleanSupplier visibility = () -> true;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public final String getName() {
		return name;
	}

	public final String getDescription() {
		return description;
	}

	public final T get() {
		return value;
	}

	public final T getDefault() {
		return defaultValue;
	}

	/** Changes the value (cleaned up to be valid first) and schedules a save. */
	public final void set(T newValue) {
		T clean = sanitize(newValue);
		if (clean == null || clean.equals(this.value)) {
			return;
		}
		this.value = clean;
		ConfigManager.markDirty();
	}

	public final void reset() {
		set(defaultValue);
	}

	/** Puts the default back without scheduling a save (used when switching profiles). */
	public final void restoreDefault() {
		this.value = defaultValue;
	}

	/** Only show this setting while {@code condition} is true. Returns this, for chaining. */
	public final Setting<T> visibleWhen(BooleanSupplier condition) {
		this.visibility = condition;
		return this;
	}

	public final boolean isVisible() {
		return visibility.getAsBoolean();
	}

	/** Turns an invalid value into the nearest valid one (or null to ignore it). */
	protected T sanitize(T value) {
		return value;
	}

	/** The value as something JSON can store (Boolean, Number or String). */
	public abstract Object toJson();

	/**
	 * Loads a value saved by {@link #toJson}. Anything broken or of the wrong
	 * kind is ignored, so the setting keeps its current value.
	 */
	public abstract void fromJson(Object json);

	/** Sets the value without scheduling a save, used while loading a config. */
	protected final void setQuietly(T newValue) {
		T clean = sanitize(newValue);
		if (clean != null) {
			this.value = clean;
		}
	}
}
