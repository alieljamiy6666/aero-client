package net.aeroclient.module.pvp;

import net.aeroclient.event.AttackEvent;
import net.aeroclient.event.TickEvent;
import net.aeroclient.event.WorldLoadEvent;
import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.Category;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

/**
 * Counts how many hits in a row you've landed. A hit only counts when the
 * server confirms it (the target flashes red right after your attack), so
 * swings that miss or don't do damage don't count. The combo resets when you
 * get hit, or when you haven't landed a hit for a few seconds.
 */
public class ComboCounterModule extends TextHudModule {

	private static final long CONFIRM_MS = 600L; // how long after an attack the hit flash can still arrive

	private final NumberSetting resetAfter = add(new NumberSetting("Reset After",
			"Seconds without landing a hit before the combo resets.", 3, 1, 10, 0.5));
	private final BooleanSetting hideAtZero = add(
			new BooleanSetting("Hide At Zero", "Only show the counter while you have a combo going.", true));

	private int combo = 0;
	private EntityLivingBase pending; // the entity we just attacked, waiting to see if it flashes
	private int pendingHurt;
	private long pendingAt, lastHitAt;
	private int lastPlayerHurt;

	private int shownCombo = -1;
	private String comboText = "0";

	public ComboCounterModule() {
		super("Combo Counter", "Counts how many hits in a row you've landed.", Category.PVP, 0, Anchor.TOP_CENTER, 0.0F,
				0.25F);
		listen(AttackEvent.class, this::onAttack);
		listen(TickEvent.class, e -> tick());
		listen(WorldLoadEvent.class, e -> reset());
	}

	private void reset() {
		this.combo = 0;
		this.pending = null;
	}

	@Override
	protected void onEnable() {
		reset();
	}

	private void onAttack(AttackEvent e) {
		Entity target = e.getTarget();
		if (target instanceof EntityLivingBase) {
			this.pending = (EntityLivingBase) target;
			this.pendingHurt = this.pending.hurtTime;
			this.pendingAt = Minecraft.getSystemTime();
		}
	}

	private void tick() {
		if (this.mc.player == null) {
			return;
		}
		long now = Minecraft.getSystemTime();
		// you got hit: the combo is over
		int hurt = this.mc.player.hurtTime;
		if (hurt > this.lastPlayerHurt) {
			this.combo = 0;
		}
		this.lastPlayerHurt = hurt;

		// did the entity we attacked just start flashing? Then the hit landed.
		if (this.pending != null) {
			if (this.pending.hurtTime > this.pendingHurt) {
				this.combo++;
				this.lastHitAt = now;
				this.pending = null;
			} else if (now - this.pendingAt > CONFIRM_MS || this.pending.isDead) {
				this.pending = null;
			} else {
				this.pendingHurt = Math.min(this.pendingHurt, this.pending.hurtTime);
			}
		}
		if (this.combo > 0 && now - this.lastHitAt > (long) (this.resetAfter.get() * 1000.0D)) {
			this.combo = 0;
		}
	}

	@Override
	protected long refreshInterval() {
		return 50L;
	}

	@Override
	protected void refresh() {
		int value = isPreview() ? 5 : this.combo;
		if (value == 0 && this.hideAtZero.isOn() && !isEditing()) {
			setLineCount(0);
			return;
		}
		if (value != this.shownCombo) {
			this.shownCombo = value;
			this.comboText = Integer.toString(value);
		}
		setLine(0, "Combo: ", this.comboText);
		setLineCount(1);
	}
}
