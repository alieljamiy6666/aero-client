package net.aeroclient.module.hud;

import net.aeroclient.event.TickEvent;
import net.aeroclient.event.WorldLoadEvent;
import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.minecraft.entity.Entity;

/**
 * Shows how fast you're moving, in blocks per second. It measures how far
 * you moved each game tick (20 per second) and averages the last half second,
 * so the number is steady instead of jumping around.
 */
public class SpeedModule extends TextHudModule {

	private static final int SAMPLES = 10; // ticks to average (half a second)

	private final BooleanSetting includeVertical = add(new BooleanSetting("Include Vertical",
			"Count up and down movement too (falling, jumping, flying).", false));
	private final ModeSetting precision = add(
			new ModeSetting("Precision", "How many decimal places to show.", "1 Decimal", "1 Decimal", "2 Decimals"));

	private final double[] samples = new double[SAMPLES];
	private int next = 0;
	private double lastX, lastY, lastZ;
	private boolean hasLast = false;

	private long shownValue = Long.MIN_VALUE;
	private String speedText = "";

	public SpeedModule() {
		super("Speed", "Shows how fast you're moving, in blocks per second.", 0, Anchor.TOP_LEFT, 0.008F, 0.4F);
		listen(TickEvent.class, e -> sample());
		listen(WorldLoadEvent.class, e -> reset());
	}

	private void reset() {
		this.hasLast = false;
		for (int i = 0; i < SAMPLES; i++) {
			this.samples[i] = 0.0D;
		}
	}

	@Override
	protected void onEnable() {
		reset();
	}

	/** Once per tick: how far did we move since the last tick? */
	private void sample() {
		Entity e = this.mc.getRenderViewEntity();
		if (e == null || this.mc.world == null) {
			this.hasLast = false;
			return;
		}
		if (this.hasLast) {
			double dx = e.posX - this.lastX, dy = e.posY - this.lastY, dz = e.posZ - this.lastZ;
			double distance = this.includeVertical.isOn() ? Math.sqrt(dx * dx + dy * dy + dz * dz)
					: Math.sqrt(dx * dx + dz * dz);
			if (distance > 50.0D) {
				distance = 0.0D; // a teleport, not real movement
			}
			this.samples[this.next] = distance * 20.0D; // blocks per tick -> blocks per second
			this.next = (this.next + 1) % SAMPLES;
		}
		this.lastX = e.posX;
		this.lastY = e.posY;
		this.lastZ = e.posZ;
		this.hasLast = true;
	}

	@Override
	protected void refresh() {
		double speed = 0.0D;
		if (isPreview()) {
			speed = 4.32D;
		} else {
			for (int i = 0; i < SAMPLES; i++) {
				speed += this.samples[i];
			}
			speed /= SAMPLES;
		}
		boolean two = this.precision.is("2 Decimals");
		long value = Math.round(speed * (two ? 100.0D : 10.0D)) * 2 + (two ? 1 : 0);
		if (value != this.shownValue) {
			this.shownValue = value;
			this.speedText = (two ? twoDecimals(speed) : oneDecimal(speed)) + " b/s";
		}
		setLine(0, "Speed: ", this.speedText);
		setLineCount(1);
	}
}
