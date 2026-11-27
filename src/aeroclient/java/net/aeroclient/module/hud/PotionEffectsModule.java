package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.HudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.theme.Theme;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Lists your active potion effects with their icon, name, level and time
 * left (longest first). Effects about to run out blink. The list is checked
 * 10 times a second, and text is rebuilt only when it changes.
 *
 * By default it hides the small vanilla effect icons in the top-right corner,
 * so they aren't shown twice.
 */
public class PotionEffectsModule extends HudModule {

	private static final int MAX = 16;
	private static final long REFRESH_MS = 100L;
	private static final float ICON = 18.0F;
	private static PotionEffectsModule instance;

	private final BooleanSetting showNames = add(
			new BooleanSetting("Show Names", "Show each effect's name and level, not just the time.", true));
	private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla Icons",
			"Hide Minecraft's own effect icons in the top-right corner while this is on.", true));
	private final BooleanSetting textShadow = add(
			new BooleanSetting("Text Shadow", "Draw a soft shadow under the text.", true));

	/** One shown effect. These are made once and reused, so refreshing makes almost no garbage. */
	private static final class Entry {
		Potion potion;
		int amplifier, duration;
		boolean endless;
		String name = "", time = "";
		Potion namedPotion;
		int namedAmplifier = -1, shownSeconds = -1;
		float textWidth;
	}

	private final Entry[] entries = new Entry[MAX];
	private int count = 0;
	private long lastRefresh = -1L;
	private float width = 0.0F;
	private PotionEffect[] examples;

	public PotionEffectsModule() {
		super("Potion Effects", "Shows your active effects with icons and the time left.", 0, Anchor.TOP_RIGHT, 0.008F,
				0.2F);
		for (int i = 0; i < MAX; i++) {
			this.entries[i] = new Entry();
		}
		instance = this;
	}

	/** True when the game should skip its own effect icons (this module is on with "Hide Vanilla Icons"). */
	public static boolean hidesVanilla() {
		return instance != null && instance.isEnabled() && instance.hideVanilla.isOn();
	}

	private PotionEffect[] examples() {
		if (this.examples == null) {
			this.examples = new PotionEffect[] { new PotionEffect(MobEffects.SPEED, 1800, 1),
					new PotionEffect(MobEffects.STRENGTH, 900, 0), new PotionEffect(MobEffects.POISON, 180, 0) };
		}
		return this.examples;
	}

	private void refresh() {
		long now = Minecraft.getSystemTime();
		if (this.lastRefresh >= 0L && now - this.lastRefresh < REFRESH_MS) {
			return;
		}
		this.lastRefresh = now;
		this.count = 0;
		if (this.mc.player != null && this.mc.world != null) {
			for (PotionEffect effect : this.mc.player.getActivePotionEffects()) {
				addEffect(effect);
			}
		}
		if (this.count == 0 && (isPreview() || isEditing())) {
			for (PotionEffect effect : examples()) {
				addEffect(effect); // nothing active: show examples so it can be placed
			}
		}
		updateTexts();
	}

	/** Adds an effect, keeping the list sorted with the longest time first. */
	private void addEffect(PotionEffect effect) {
		if (this.count >= MAX || effect.getPotion() == null || !effect.getPotion().hasStatusIcon()) {
			return;
		}
		// reuse the entry that showed this effect last time, so its text doesn't have to be rebuilt
		for (int j = this.count; j < MAX; j++) {
			if (this.entries[j].namedPotion == effect.getPotion()) {
				Entry swap = this.entries[this.count];
				this.entries[this.count] = this.entries[j];
				this.entries[j] = swap;
				break;
			}
		}
		Entry e = this.entries[this.count];
		e.potion = effect.getPotion();
		e.amplifier = effect.getAmplifier();
		e.endless = effect.getIsPotionDurationMax();
		e.duration = e.endless ? Integer.MAX_VALUE : effect.getDuration();
		int i = this.count++;
		while (i > 0 && this.entries[i - 1].duration < e.duration) { // slide it up into place
			this.entries[i] = this.entries[i - 1];
			i--;
		}
		this.entries[i] = e;
	}

	private void updateTexts() {
		FontRenderer font = this.mc.fontRendererObj;
		float w = 0.0F;
		for (int i = 0; i < this.count; i++) {
			Entry e = this.entries[i];
			boolean changed = false;
			if (e.potion != e.namedPotion || e.amplifier != e.namedAmplifier) {
				e.namedPotion = e.potion;
				e.namedAmplifier = e.amplifier;
				String name = I18n.format(e.potion.getName());
				if (e.amplifier > 0) {
					name = name + " " + I18n.format("enchantment.level." + (e.amplifier + 1));
				}
				e.name = name;
				changed = true;
			}
			int seconds = e.endless ? -2 : e.duration / 20;
			if (seconds != e.shownSeconds) {
				e.shownSeconds = seconds;
				e.time = e.endless ? "**:**" : (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
				changed = true;
			}
			if (changed || e.textWidth <= 0.0F) {
				e.textWidth = Math.max(font.getStringWidth(e.name), font.getStringWidth(e.time));
			}
			w = Math.max(w, this.showNames.isOn() ? e.textWidth : font.getStringWidth(e.time));
		}
		this.width = w;
	}

	private float rowHeight() {
		return this.showNames.isOn() ? 20.0F : ICON + 1.0F;
	}

	@Override
	protected boolean hasContent() {
		refresh();
		return this.count > 0;
	}

	@Override
	protected float getContentWidth() {
		refresh();
		return ICON + 3.0F + this.width + 1.0F;
	}

	@Override
	protected float getContentHeight() {
		return Math.max(1, this.count) * rowHeight() - 2.0F;
	}

	@Override
	protected void renderContent(boolean editing) {
		refresh();
		FontRenderer font = this.mc.fontRendererObj;
		boolean shadow = this.textShadow.isOn(), names = this.showNames.isOn();
		boolean right = getAnchor().column == 2; // icons on the right when it's on the right side
		float total = ICON + 3.0F + this.width;

		this.mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < this.count; i++) {
			Entry e = this.entries[i];
			int index = e.potion.getStatusIconIndex();
			int iconX = right ? (int) (total - ICON) : 0;
			Gui.drawModalRectWithCustomSizedTexture(iconX, (int) (i * rowHeight()), index % 8 * 18,
					198 + index / 8 * 18, 18, 18, 256.0F, 256.0F);
		}

		float blink = 0.6F + 0.4F * (float) Math.cos(Minecraft.getSystemTime() / 160.0D);
		for (int i = 0; i < this.count; i++) {
			Entry e = this.entries[i];
			float y = i * rowHeight();
			int nameColor = e.potion.isBadEffect() ? 0xFFFCA5A5 : Theme.text;
			int timeColor = !e.endless && e.duration <= 200 ? Theme.withAlpha(Theme.danger, blink) : Theme.textFaded;
			if (names) {
				float nx = right ? total - ICON - 3.0F - font.getStringWidth(e.name) : ICON + 3.0F;
				float tx = right ? total - ICON - 3.0F - font.getStringWidth(e.time) : ICON + 3.0F;
				font.drawString(e.name, nx, y + 1.0F, nameColor, shadow);
				font.drawString(e.time, tx, y + 10.0F, timeColor, shadow);
			} else {
				float tx = right ? total - ICON - 3.0F - font.getStringWidth(e.time) : ICON + 3.0F;
				font.drawString(e.time, tx, y + 5.0F, timeColor, shadow);
			}
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
