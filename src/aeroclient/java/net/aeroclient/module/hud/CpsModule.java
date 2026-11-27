package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.util.ClickTracker;

/**
 * A small display of your clicks per second, left and right. Clicks are
 * counted from their real times over the last second, so the number matches
 * how fast you actually click.
 */
public class CpsModule extends TextHudModule {

	private final BooleanSetting showRight = add(
			new BooleanSetting("Show Right Click", "Also show right clicks, as \"left | right\".", true));

	private int shownLeft = -1, shownRight = -1;
	private boolean shownBoth;
	private String text = "";

	public CpsModule() {
		super("CPS Counter", "Counts your left and right clicks per second.", 0, Anchor.MIDDLE_RIGHT, 0.008F, 0.12F);
	}

	@Override
	protected void refresh() {
		int left = ClickTracker.getLeftCps(), right = ClickTracker.getRightCps();
		boolean both = this.showRight.isOn();
		if (left != this.shownLeft || right != this.shownRight || both != this.shownBoth) {
			this.shownLeft = left;
			this.shownRight = right;
			this.shownBoth = both;
			this.text = both ? left + " | " + right : Integer.toString(left);
		}
		setLine(0, "CPS: ", this.text);
		setLineCount(1);
	}
}
