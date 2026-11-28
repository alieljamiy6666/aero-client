package net.aeroclient.util;

import java.io.IOException;
import java.io.OutputStream;

import net.lax1dude.eaglercraft.EaglerOutputStream;
import net.lax1dude.eaglercraft.EaglerZLIB;

/**
 * Turns an RGB picture into a PNG file, a few rows at a time, so a big
 * screenshot can be saved over several frames instead of freezing the game.
 *
 * <pre>
 * PngEncoder png = new PngEncoder(width, height, rgb); // rgb: 3 bytes per pixel, top row first
 * while (!png.step(3_000_000L)) { ...next frame... }
 * byte[] file = png.getBytes();
 * </pre>
 */
public final class PngEncoder {

	private static final int[] CRC_TABLE = new int[256];

	static {
		for (int n = 0; n < 256; ++n) {
			int c = n;
			for (int k = 0; k < 8; ++k) {
				c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
			}
			CRC_TABLE[n] = c;
		}
	}

	private final int width, height;
	private final byte[] rgb;
	private final byte[] row;
	private final EaglerOutputStream compressed = new EaglerOutputStream(65536);
	private OutputStream deflater;
	private int nextRow = 0;
	private byte[] result;

	public PngEncoder(int width, int height, byte[] rgb) {
		this.width = width;
		this.height = height;
		this.rgb = rgb;
		this.row = new byte[width * 3 + 1];
	}

	/**
	 * Compresses rows for up to {@code budgetNanos}. Returns true when the PNG
	 * is finished (or failed: then {@link #getBytes} returns null).
	 */
	public boolean step(long budgetNanos) {
		if (this.result != null || this.nextRow < 0) {
			return true;
		}
		long end = System.nanoTime() + budgetNanos;
		try {
			if (this.deflater == null) {
				this.deflater = EaglerZLIB.newDeflaterOutputStream(this.compressed);
			}
			int stride = this.width * 3;
			while (this.nextRow < this.height) {
				// "Sub" filter: each byte minus the same color of the pixel to its left (compresses better)
				int base = this.nextRow * stride;
				this.row[0] = 1;
				for (int i = 0; i < stride; ++i) {
					this.row[i + 1] = (byte) (this.rgb[base + i] - (i >= 3 ? this.rgb[base + i - 3] : 0));
				}
				this.deflater.write(this.row);
				++this.nextRow;
				if ((this.nextRow & 7) == 0 && System.nanoTime() > end) {
					return false;
				}
			}
			this.deflater.close();
			this.result = buildFile(this.compressed.toByteArray());
			return true;
		} catch (IOException e) {
			this.nextRow = -1;
			return true;
		}
	}

	/** Encodes the whole picture right away (fine for small ones like thumbnails). */
	public byte[] encodeNow() {
		while (!step(Long.MAX_VALUE / 4)) {
			// keeps going until done
		}
		return this.result;
	}

	/** The finished PNG file, or null if it isn't done or failed. */
	public byte[] getBytes() {
		return this.result;
	}

	private byte[] buildFile(byte[] idat) throws IOException {
		EaglerOutputStream out = new EaglerOutputStream(idat.length + 64);
		out.write(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });
		byte[] ihdr = new byte[13];
		putInt(ihdr, 0, this.width);
		putInt(ihdr, 4, this.height);
		ihdr[8] = 8; // 8 bits per channel
		ihdr[9] = 2; // RGB
		writeChunk(out, "IHDR", ihdr);
		writeChunk(out, "IDAT", idat);
		writeChunk(out, "IEND", new byte[0]);
		return out.toByteArray();
	}

	private static void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
		byte[] head = new byte[8];
		putInt(head, 0, data.length);
		for (int i = 0; i < 4; ++i) {
			head[4 + i] = (byte) type.charAt(i);
		}
		out.write(head);
		out.write(data);
		int crc = 0xFFFFFFFF;
		for (int i = 4; i < 8; ++i) {
			crc = CRC_TABLE[(crc ^ head[i]) & 0xFF] ^ (crc >>> 8);
		}
		for (int i = 0; i < data.length; ++i) {
			crc = CRC_TABLE[(crc ^ data[i]) & 0xFF] ^ (crc >>> 8);
		}
		byte[] tail = new byte[4];
		putInt(tail, 0, ~crc);
		out.write(tail);
	}

	private static void putInt(byte[] b, int off, int v) {
		b[off] = (byte) (v >>> 24);
		b[off + 1] = (byte) (v >>> 16);
		b[off + 2] = (byte) (v >>> 8);
		b[off + 3] = (byte) v;
	}
}
