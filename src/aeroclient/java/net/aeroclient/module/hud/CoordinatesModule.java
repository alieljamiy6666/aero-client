package net.aeroclient.module.hud;

import net.aeroclient.event.WorldLoadEvent;
import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.TextHudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * Shows where you are: X, Y and Z, which way you're facing and the biome
 * you're standing in. Text is only rebuilt when the numbers change.
 *
 * While it's on, it replaces the small built-in coordinates line in the
 * top-left corner.
 */
public class CoordinatesModule extends TextHudModule {

	private static final String[] DIRECTIONS = { "South (+Z)", "West (-X)", "North (-Z)", "East (+X)" };
	private static CoordinatesModule instance;

	private final ModeSetting layout = add(new ModeSetting("Layout", "One line per number, or X Y Z on one line.",
			"Lines", "Lines", "One Line"));
	private final ModeSetting precision = add(new ModeSetting("Precision", "Whole blocks, or one decimal place.",
			"Whole", "Whole", "1 Decimal"));
	private final BooleanSetting showDirection = add(
			new BooleanSetting("Show Direction", "Show which way you're facing.", true));
	private final BooleanSetting showBiome = add(new BooleanSetting("Show Biome", "Show the biome you're in.", true));

	private final BlockPos pos = new BlockPos(0, 0, 0); // reused, so looking up the biome makes no garbage

	// last shown values (in tenths of a block) and their text
	private long shownX = Long.MIN_VALUE, shownY = Long.MIN_VALUE, shownZ = Long.MIN_VALUE;
	private boolean shownDecimals;
	private String xText = "", yText = "", zText = "", xyzText = "";
	private int shownBlockX = Integer.MIN_VALUE, shownBlockY, shownBlockZ;
	private String biomeText = "";

	public CoordinatesModule() {
		super("Coordinates", "Shows X, Y, Z, the way you're facing and the biome you're in.", 0, Anchor.TOP_LEFT,
				0.008F, 0.15F);
		instance = this;
		listen(WorldLoadEvent.class, e -> this.shownBlockX = Integer.MIN_VALUE); // look the biome up again
	}

	/** True while this module is on, so the game skips its own coordinates line. */
	public static boolean hidesVanilla() {
		return instance != null && instance.isEnabled();
	}

	@Override
	protected void refresh() {
		double x, y, z;
		int facing;
		Entity e = this.mc.getRenderViewEntity();
		boolean preview = isPreview() || e == null;
		if (preview) {
			x = 120.5D;
			y = 64.0D;
			z = -38.2D;
			facing = 2;
		} else {
			x = e.posX;
			y = e.getEntityBoundingBox().minY;
			z = e.posZ;
			facing = e.getHorizontalFacing().getHorizontalIndex();
		}

		boolean decimals = this.precision.is("1 Decimal");
		long tx = decimals ? Math.round(x * 10.0D) : (long) Math.floor(x);
		long ty = decimals ? Math.round(y * 10.0D) : (long) Math.floor(y);
		long tz = decimals ? Math.round(z * 10.0D) : (long) Math.floor(z);
		if (tx != this.shownX || ty != this.shownY || tz != this.shownZ || decimals != this.shownDecimals) {
			this.shownX = tx;
			this.shownY = ty;
			this.shownZ = tz;
			this.shownDecimals = decimals;
			this.xText = decimals ? oneDecimal(tx / 10.0D) : Long.toString(tx);
			this.yText = decimals ? oneDecimal(ty / 10.0D) : Long.toString(ty);
			this.zText = decimals ? oneDecimal(tz / 10.0D) : Long.toString(tz);
			this.xyzText = this.xText + " / " + this.yText + " / " + this.zText;
		}

		int line = 0;
		if (this.layout.is("One Line")) {
			setLine(line++, "XYZ: ", this.xyzText);
		} else {
			setLine(line++, "X: ", this.xText);
			setLine(line++, "Y: ", this.yText);
			setLine(line++, "Z: ", this.zText);
		}
		if (this.showDirection.isOn()) {
			setLine(line++, "Facing: ", DIRECTIONS[facing & 3]);
		}
		if (this.showBiome.isOn()) {
			setLine(line++, "Biome: ", preview ? "Plains" : biomeName(x, y, z));
		}
		setLineCount(line);
	}

	/** The biome's name, looked up again only when you move to a different block. */
	private String biomeName(double x, double y, double z) {
		int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
		if (bx != this.shownBlockX || by != this.shownBlockY || bz != this.shownBlockZ) {
			this.shownBlockX = bx;
			this.shownBlockY = by;
			this.shownBlockZ = bz;
			this.pos.setPos(bx, by, bz);
			if (this.mc.world != null && this.mc.world.isBlockLoaded(this.pos)) {
				Biome biome = this.mc.world.getBiome(this.pos);
				this.biomeText = biome == null ? "-" : biome.getBiomeName();
			} else {
				this.biomeText = "-";
			}
		}
		return this.biomeText;
	}
}
