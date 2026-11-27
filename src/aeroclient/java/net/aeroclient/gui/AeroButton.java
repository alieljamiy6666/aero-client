package net.aeroclient.gui;

import net.minecraft.client.gui.GuiButton;

/**
 * A normal Minecraft button that can also show an icon instead of text.
 *
 * Every button in the game is already drawn in the rounded Aero style (see
 * {@link AeroButtonStyle}); this class only adds icons, like the globe on the
 * title screen's language button.
 */
public class AeroButton extends GuiButton {

	/** Plain text button. */
	public static final int ICON_NONE = 0;
	/** Round button with a globe, for picking the language. */
	public static final int ICON_GLOBE = 1;

	private final int icon;

	public AeroButton(int id, int x, int y, String text) {
		this(id, x, y, 200, 20, text, ICON_NONE);
	}

	public AeroButton(int id, int x, int y, int width, int height, String text) {
		this(id, x, y, width, height, text, ICON_NONE);
	}

	public AeroButton(int id, int x, int y, int width, int height, String text, int icon) {
		super(id, x, y, width, height, text);
		this.icon = icon;
	}

	public int getIcon() {
		return this.icon;
	}
}
