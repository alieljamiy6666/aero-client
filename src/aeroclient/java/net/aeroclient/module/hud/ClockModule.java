package net.aeroclient.module.hud;

import java.util.Calendar;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.theme.Theme;

/**
 * Shows the real-world time from your computer's clock, in 12-hour
 * (6:03 PM) or 24-hour (18:03) format, with optional seconds and date.
 * The text only changes when the time on screen would change.
 */
public class ClockModule extends TextHudModule {

	private static final String[] DAYS = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	private static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
			"Nov", "Dec" };

	private final ModeSetting format = add(
			new ModeSetting("Format", "12-hour (6:03 PM) or 24-hour (18:03).", "12-hour", "12-hour", "24-hour"));
	private final BooleanSetting showSeconds = add(new BooleanSetting("Show Seconds", "Add the seconds.", false));
	private final BooleanSetting showDate = add(
			new BooleanSetting("Show Date", "Add a line with the day and date.", false));

	private final Calendar calendar = Calendar.getInstance();
	private long shownKey = -1L;
	private String timeText = "", dateText = "";

	public ClockModule() {
		super("Clock", "Shows the real-world time.", 0, Anchor.BOTTOM_RIGHT, 0.008F, 0.012F);
	}

	@Override
	protected long refreshInterval() {
		return 250L;
	}

	@Override
	protected void refresh() {
		long now = System.currentTimeMillis();
		boolean twelve = this.format.is("12-hour"), seconds = this.showSeconds.isOn();
		// one number that changes whenever the shown text would: the second (or minute) plus the options
		long key = (seconds ? now / 1000L : now / 60000L) * 4 + (twelve ? 2 : 0) + (seconds ? 1 : 0);
		if (key != this.shownKey) {
			this.shownKey = key;
			this.calendar.setTimeInMillis(now);
			int hour = this.calendar.get(Calendar.HOUR_OF_DAY);
			int minute = this.calendar.get(Calendar.MINUTE);
			int second = this.calendar.get(Calendar.SECOND);
			StringBuilder sb = new StringBuilder(12);
			if (twelve) {
				int h = hour % 12;
				sb.append(h == 0 ? 12 : h);
			} else {
				if (hour < 10) {
					sb.append('0');
				}
				sb.append(hour);
			}
			sb.append(':');
			if (minute < 10) {
				sb.append('0');
			}
			sb.append(minute);
			if (seconds) {
				sb.append(':');
				if (second < 10) {
					sb.append('0');
				}
				sb.append(second);
			}
			if (twelve) {
				sb.append(hour < 12 ? " AM" : " PM");
			}
			this.timeText = sb.toString();
			this.dateText = DAYS[(this.calendar.get(Calendar.DAY_OF_WEEK) + 6) % 7] + ", "
					+ MONTHS[this.calendar.get(Calendar.MONTH)] + " " + this.calendar.get(Calendar.DAY_OF_MONTH);
		}
		setLine(0, "", this.timeText);
		if (this.showDate.isOn()) {
			setLine(1, "", this.dateText, Theme.textFaded);
			setLineCount(2);
		} else {
			setLineCount(1);
		}
	}
}
