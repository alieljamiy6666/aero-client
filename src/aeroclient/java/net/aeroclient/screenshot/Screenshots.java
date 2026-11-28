package net.aeroclient.screenshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import net.aeroclient.AeroClient;
import net.aeroclient.util.PngEncoder;
import net.lax1dude.eaglercraft.Display;
import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.PlatformOpenGL;
import net.lax1dude.eaglercraft.internal.buffer.ByteBuffer;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;

/**
 * Saves screenshots in the browser's own storage (the same place worlds are
 * kept), so the in-game gallery can show them later, even after a reload.
 *
 * Each screenshot is two files: the full picture and a small preview for the
 * gallery grid. Taking one reads the screen right away; turning it into a PNG
 * file happens bit by bit over the next frames, so the game doesn't freeze.
 *
 * Screenshots are only ever deleted when you delete them in the gallery.
 */
public final class Screenshots {

	private static final String FOLDER = "aeroclient/screenshots";
	private static final String THUMB = ".thumb.png";
	/** Width of the gallery previews, in pixels. */
	public static final int THUMB_W = 256;

	/** A screenshot still being turned into a PNG file. */
	private static final class Job {
		final String name;
		final PngEncoder png;

		Job(String name, PngEncoder png) {
			this.name = name;
			this.png = png;
		}
	}

	private static final List<Job> jobs = new ArrayList<>();
	private static final Calendar calendar = Calendar.getInstance();

	private Screenshots() {
	}

	/**
	 * Reads what's on screen right now and starts saving it. Returns the name
	 * it will be saved as, or null if it failed.
	 */
	public static String capture() {
		int w = Display.getWidth(), h = Display.getHeight();
		if (w <= 0 || h <= 0) {
			return null;
		}
		byte[] rgb = new byte[w * h * 3];
		ByteBuffer buf = EagRuntime.allocateByteBuffer(w * h * 4);
		try {
			PlatformOpenGL._wglBindFramebuffer(0x8D40, null); // the screen itself
			PlatformOpenGL._wglReadPixels(0, 0, w, h, 0x1908, 0x1401, buf); // RGBA, unsigned bytes
			// the screen is stored bottom row first: flip it while dropping the alpha
			for (int y = 0; y < h; ++y) {
				int src = (h - 1 - y) * w * 4, dst = y * w * 3;
				for (int x = 0; x < w; ++x) {
					rgb[dst++] = buf.get(src);
					rgb[dst++] = buf.get(src + 1);
					rgb[dst++] = buf.get(src + 2);
					src += 4;
				}
			}
		} finally {
			EagRuntime.freeByteBuffer(buf);
		}
		String name = newName();
		try {
			new VFile2(FOLDER, name + THUMB).setAllBytes(new PngEncoder(THUMB_W, thumbHeight(w, h),
					shrink(rgb, w, h, THUMB_W, thumbHeight(w, h))).encodeNow());
		} catch (Throwable t) {
			AeroClient.LOGGER.error("Couldn't save the screenshot preview");
			AeroClient.LOGGER.error(t);
		}
		jobs.add(new Job(name, new PngEncoder(w, h, rgb)));
		return name;
	}

	private static int thumbHeight(int w, int h) {
		return Math.max(1, Math.round(THUMB_W * (float) h / w));
	}

	/** A name from the date and time, like 2026-11-27_22-41-09 (sorted oldest to newest). */
	private static String newName() {
		calendar.setTimeInMillis(System.currentTimeMillis());
		String base = calendar.get(Calendar.YEAR) + "-" + two(calendar.get(Calendar.MONTH) + 1) + "-"
				+ two(calendar.get(Calendar.DAY_OF_MONTH)) + "_" + two(calendar.get(Calendar.HOUR_OF_DAY)) + "-"
				+ two(calendar.get(Calendar.MINUTE)) + "-" + two(calendar.get(Calendar.SECOND));
		String name = base;
		for (int i = 2; new VFile2(FOLDER, name + THUMB).exists() || isSaving(name); ++i) {
			name = base + "_" + i;
		}
		return name;
	}

	private static String two(int n) {
		return n < 10 ? "0" + n : Integer.toString(n);
	}

	/** Makes a small copy of the picture by averaging blocks of pixels (smooth, not jagged). */
	private static byte[] shrink(byte[] rgb, int w, int h, int tw, int th) {
		byte[] out = new byte[tw * th * 3];
		for (int ty = 0; ty < th; ++ty) {
			int y0 = ty * h / th, y1 = Math.max(y0 + 1, (ty + 1) * h / th);
			for (int tx = 0; tx < tw; ++tx) {
				int x0 = tx * w / tw, x1 = Math.max(x0 + 1, (tx + 1) * w / tw);
				int r = 0, g = 0, b = 0, n = 0;
				for (int y = y0; y < y1; ++y) {
					int i = (y * w + x0) * 3;
					for (int x = x0; x < x1; ++x) {
						r += rgb[i] & 0xFF;
						g += rgb[i + 1] & 0xFF;
						b += rgb[i + 2] & 0xFF;
						i += 3;
						++n;
					}
				}
				int o = (ty * tw + tx) * 3;
				out[o] = (byte) (r / n);
				out[o + 1] = (byte) (g / n);
				out[o + 2] = (byte) (b / n);
			}
		}
		return out;
	}

	/**
	 * Keeps saving screenshots in the background. Called every frame and tick
	 * with a small time budget.
	 */
	public static void work(long budgetNanos) {
		if (jobs.isEmpty()) {
			return;
		}
		Job job = jobs.get(0);
		if (job.png.step(budgetNanos)) {
			jobs.remove(0);
			byte[] file = job.png.getBytes();
			if (file != null) {
				try {
					new VFile2(FOLDER, job.name + ".png").setAllBytes(file);
				} catch (Throwable t) {
					AeroClient.LOGGER.error("Couldn't save screenshot " + job.name);
					AeroClient.LOGGER.error(t);
				}
			}
		}
	}

	/** True while this screenshot is still being saved. */
	public static boolean isSaving(String name) {
		for (int i = 0; i < jobs.size(); ++i) {
			if (jobs.get(i).name.equals(name)) {
				return true;
			}
		}
		return false;
	}

	/** Every saved screenshot's name, newest first. */
	public static List<String> list() {
		List<String> names = new ArrayList<>();
		try {
			for (String path : new VFile2(FOLDER).listFilenames(false)) {
				String file = VFile2.getNameFromPath(path);
				if (file.endsWith(THUMB)) {
					names.add(file.substring(0, file.length() - THUMB.length()));
				}
			}
		} catch (Throwable t) {
			AeroClient.LOGGER.error("Couldn't list screenshots");
		}
		Collections.sort(names, Collections.reverseOrder());
		return names;
	}

	/** The small preview PNG, or null. */
	public static byte[] readThumbnail(String name) {
		return read(name + THUMB);
	}

	/** The full PNG, or null (also while it's still being saved). */
	public static byte[] readFull(String name) {
		return read(name + ".png");
	}

	private static byte[] read(String file) {
		try {
			VFile2 f = new VFile2(FOLDER, file);
			return f.exists() ? f.getAllBytes() : null;
		} catch (Throwable t) {
			return null;
		}
	}

	/** Size of the full PNG in bytes (0 if missing). */
	public static int size(String name) {
		try {
			return new VFile2(FOLDER, name + ".png").length();
		} catch (Throwable t) {
			return 0;
		}
	}

	/** Deletes a screenshot and its preview. */
	public static void delete(String name) {
		new VFile2(FOLDER, name + ".png").delete();
		new VFile2(FOLDER, name + THUMB).delete();
	}

	/** Downloads the full picture as a normal PNG file. */
	public static boolean download(String name) {
		byte[] data = readFull(name);
		if (data == null) {
			return false;
		}
		EagRuntime.downloadFileWithName("aero_" + name + ".png", data);
		return true;
	}

	/** A friendly date for a screenshot name, like "Nov 27, 2026  10:41 PM". */
	public static String prettyDate(String name) {
		try {
			String[] parts = name.split("_");
			String[] d = parts[0].split("-"), t = parts[1].split("-");
			int month = Integer.parseInt(d[1]), hour = Integer.parseInt(t[0]);
			String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
			return months[month - 1] + " " + Integer.parseInt(d[2]) + ", " + d[0] + "  "
					+ (hour % 12 == 0 ? 12 : hour % 12) + ":" + t[1] + (hour < 12 ? " AM" : " PM");
		} catch (Throwable t) {
			return name;
		}
	}
}
