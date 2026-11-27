package net.aeroclient.module;

/**
 * The groups modules are sorted into in the Right Shift menu.
 *
 * {@link #ALL} isn't a real group: it's a view that shows every module. No
 * module should use ALL as its own category.
 */
public enum Category {

	ALL("All"),
	RENDER("Render"),
	HUD("HUD"),
	MISC("Misc"),
	PVP("PvP");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	/** The name shown in menus, like "PvP". */
	public String getDisplayName() {
		return displayName;
	}

	/** True for ALL, which shows every module instead of being a group itself. */
	public boolean isView() {
		return this == ALL;
	}
}
