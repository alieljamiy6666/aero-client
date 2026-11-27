package net.aeroclient.gui.clickgui;

import net.aeroclient.util.RenderUtil;

/**
 * Tiny pixel-art icons for the Right Shift menu, drawn from text pictures:
 * '#' is a lit pixel, '.' is empty. Drawing an icon is one batch of small
 * squares, so no image files are needed and they stay sharp at any GUI scale.
 *
 * Want a new icon? Draw it below with # and . (up to 8 by 8) and add a
 * constant for it.
 */
public final class PixelIcons {

	public static final Icon ALL = new Icon(
			"###..###",
			"###..###",
			"###..###",
			"........",
			"........",
			"###..###",
			"###..###",
			"###..###");

	public static final Icon RENDER = new Icon(
			"........",
			"..####..",
			".#....#.",
			"#..##..#",
			"#..##..#",
			".#....#.",
			"..####..",
			"........");

	public static final Icon HUD = new Icon(
			"########",
			"#......#",
			"#.##...#",
			"#......#",
			"#.####.#",
			"########",
			"...##...",
			"..####..");

	public static final Icon MISC = new Icon(
			"...#...",
			"...#...",
			"..###..",
			"#######",
			"..###..",
			"...#...",
			"...#...");

	public static final Icon PVP = new Icon(
			"......##",
			".....###",
			"....###.",
			"#..###..",
			".####...",
			"..##....",
			".#.##...",
			"#...#...");

	public static final Icon GEAR = new Icon(
			"..#..#..",
			".######.",
			"###..###",
			".#....#.",
			".#....#.",
			"###..###",
			".######.",
			"..#..#..");

	public static final Icon PROFILE = new Icon(
			"...##...",
			"..####..",
			"..####..",
			"...##...",
			"........",
			".######.",
			"########",
			"########");

	public static final Icon MOVE = new Icon(
			"...#...",
			"..###..",
			".#.#.#.",
			"#######",
			".#.#.#.",
			"..###..",
			"...#...");

	public static final Icon SEARCH = new Icon(
			".####...",
			"#....#..",
			"#....#..",
			"#....#..",
			"#....#..",
			".#####..",
			".....##.",
			"......##");

	public static final Icon CLOSE = new Icon(
			"##...##",
			"###.###",
			".#####.",
			"..###..",
			".#####.",
			"###.###",
			"##...##");

	public static final Icon BACK = new Icon(
			"...##",
			"..##.",
			".##..",
			"##...",
			"##...",
			".##..",
			"..##.",
			"...##");

	public static final Icon DOWN = new Icon(
			"##...##",
			".##.##.",
			"..###..",
			"...#...");

	public static final Icon CHECK = new Icon(
			"......#",
			".....##",
			"#...##.",
			"##.##..",
			".###...",
			"..#....");

	private PixelIcons() {
	}

	/** One icon: which pixels are lit, worked out once when the game starts. */
	public static final class Icon {
		private final int width, height;
		private final boolean[] pixels;

		Icon(String... rows) {
			this.height = rows.length;
			this.width = rows[0].length();
			this.pixels = new boolean[this.width * this.height];
			for (int y = 0; y < this.height; y++) {
				for (int x = 0; x < this.width && x < rows[y].length(); x++) {
					this.pixels[y * this.width + x] = rows[y].charAt(x) == '#';
				}
			}
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		/** Draws the icon with its top-left corner at (x, y); each icon pixel is {@code size} GUI units. */
		public void draw(float x, float y, float size, int color) {
			if ((color >>> 24) == 0) {
				return;
			}
			RenderUtil.beginBatch(false);
			for (int py = 0; py < this.height; py++) {
				for (int px = 0; px < this.width; px++) {
					if (this.pixels[py * this.width + px]) {
						float x0 = x + px * size, y0 = y + py * size;
						RenderUtil.batchRect(x0, y0, x0 + size, y0 + size, color);
					}
				}
			}
			RenderUtil.endBatch();
		}

		/** Draws the icon centered on (centerX, centerY), snapped to whole pixels so it stays crisp. */
		public void drawCentered(float centerX, float centerY, float size, int color) {
			draw(Math.round(centerX - this.width * size / 2.0F), Math.round(centerY - this.height * size / 2.0F), size,
					color);
		}
	}
}
