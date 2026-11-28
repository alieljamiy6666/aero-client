package net.aeroclient.module.misc;

import java.util.Calendar;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Shows the time each message arrived, like "[3:45 PM]", in front of it.
 * The message itself keeps all its server colors and rank prefixes.
 */
public class ChatTimestampsModule extends Module {

	private static ChatTimestampsModule instance;

	private final ModeSetting format = add(new ModeSetting("Format", "12-hour (3:45 PM) or 24-hour (15:45).", "12-hour",
			"12-hour", "24-hour"));
	private final BooleanSetting seconds = add(new BooleanSetting("Show Seconds", "Add seconds, like 3:45:09 PM.", false));
	private final ModeSetting color = add(new ModeSetting("Color", "The color of the time.", "Gray", "Gray", "Dark Gray",
			"White", "Aqua", "Gold", "Yellow"));

	private final Calendar calendar = Calendar.getInstance();

	public ChatTimestampsModule() {
		super("Chat Timestamps", "Shows the time before each chat message.", Category.MISC, 0);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** A "[3:45 PM] " piece in the chosen color, for the time right now. */
	public static ITextComponent stamp() {
		ChatTimestampsModule m = instance;
		m.calendar.setTimeInMillis(System.currentTimeMillis());
		int hour = m.calendar.get(Calendar.HOUR_OF_DAY), minute = m.calendar.get(Calendar.MINUTE);
		boolean twelve = m.format.is("12-hour");
		StringBuilder sb = new StringBuilder(16).append('[');
		if (twelve) {
			sb.append(hour % 12 == 0 ? 12 : hour % 12);
		} else {
			sb.append(hour < 10 ? "0" : "").append(hour);
		}
		sb.append(':').append(minute < 10 ? "0" : "").append(minute);
		if (m.seconds.isOn()) {
			int second = m.calendar.get(Calendar.SECOND);
			sb.append(':').append(second < 10 ? "0" : "").append(second);
		}
		if (twelve) {
			sb.append(hour < 12 ? " AM" : " PM");
		}
		sb.append("] ");
		ITextComponent stamp = new TextComponentString(sb.toString());
		stamp.getStyle().setColor(m.textColor());
		return stamp;
	}

	private TextFormatting textColor() {
		switch (this.color.get()) {
		case "Dark Gray":
			return TextFormatting.DARK_GRAY;
		case "White":
			return TextFormatting.WHITE;
		case "Aqua":
			return TextFormatting.AQUA;
		case "Gold":
			return TextFormatting.GOLD;
		case "Yellow":
			return TextFormatting.YELLOW;
		default:
			return TextFormatting.GRAY;
		}
	}
}
