package net.aeroclient.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.aeroclient.gui.clickgui.Widgets;
import net.aeroclient.module.misc.ScreenshotManagerModule;
import net.aeroclient.screenshot.Screenshots;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.aeroclient.util.TextUtil;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.EaglercraftGPU;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.ImageData;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * The screenshot gallery: a grid of your screenshots, newest first. Click one
 * to see it big, then use the arrows (or Left/Right) to flip through them,
 * Download to save it as a file again, or Delete (click twice) to remove it.
 */
public class AeroScreenshotsScreen extends GuiScreen {

	private static final int ID_DONE = 1, ID_PREV = 2, ID_NEXT = 3, ID_DOWNLOAD = 4, ID_DELETE = 5, ID_BACK = 6;
	private static final float GAP = 6.0F, LABEL_H = 11.0F;

	/** A loaded picture and its size. */
	private static final class Picture {
		final DynamicTexture texture;
		final int width, height;

		Picture(ImageData img) {
			this.texture = new DynamicTexture(img);
			this.width = img.width;
			this.height = img.height;
		}
	}

	private final GuiScreen parent;
	private final Animation.Fader appear = new Animation.Fader(250.0F);
	private final Map<String, Picture> thumbs = new HashMap<>();
	private final Map<String, Boolean> failed = new HashMap<>();
	private List<String> names;
	private long totalBytes;

	private float panelX, panelY, panelW, panelH, gridTop, gridBottom;
	private int columns;
	private float cellW, cellH, scroll, scrollTarget, maxScroll;

	/** The screenshot shown big, or -1 for the grid. */
	private int viewing = -1;
	private Picture full;
	private String fullName;
	private long deleteArmedAt = 0L;
	private String status = "";

	public AeroScreenshotsScreen(GuiScreen parent) {
		this.parent = parent;
	}

	private void reload() {
		this.names = Screenshots.list();
		this.totalBytes = 0L;
		for (String name : this.names) {
			this.totalBytes += Screenshots.size(name);
		}
	}

	@Override
	public void initGui() {
		if (this.names == null) {
			reload();
		}
		this.buttonList.clear();
		this.panelW = Math.min(440, this.width - 16);
		this.panelH = Math.min(300, this.height - 44);
		this.panelX = this.width / 2.0F - this.panelW / 2.0F;
		this.panelY = Math.max(4, (this.height - this.panelH - 28) / 2.0F);
		this.gridTop = this.panelY + 34;
		this.gridBottom = this.panelY + this.panelH - 8;
		this.columns = this.panelW >= 330 ? 3 : 2;
		this.cellW = (this.panelW - 20 - GAP * (this.columns - 1)) / this.columns;
		this.cellH = this.cellW * 9.0F / 16.0F + LABEL_H;
		int cx = this.width / 2;
		if (this.viewing < 0) {
			this.buttonList.add(new GuiButton(ID_DONE, cx - 100, (int) (this.panelY + this.panelH) + 6, 200, 20, "Done"));
		} else {
			int y = this.height - 26;
			this.buttonList.add(new GuiButton(ID_PREV, cx - 160, y, 30, 20, "<"));
			this.buttonList.add(new GuiButton(ID_DOWNLOAD, cx - 126, y, 80, 20, "Download"));
			this.buttonList.add(new GuiButton(ID_DELETE, cx - 42, y, 84, 20, deleteLabel()));
			this.buttonList.add(new GuiButton(ID_BACK, cx + 46, y, 80, 20, "Back"));
			this.buttonList.add(new GuiButton(ID_NEXT, cx + 130, y, 30, 20, ">"));
			updateArrows();
		}
	}

	private String deleteLabel() {
		return this.deleteArmedAt != 0L ? "Really delete?" : "Delete";
	}

	private void updateArrows() {
		for (GuiButton b : this.buttonList) {
			if (b.id == ID_PREV) {
				b.enabled = this.viewing > 0;
			} else if (b.id == ID_NEXT) {
				b.enabled = this.viewing < this.names.size() - 1;
			}
		}
	}

	// ---------------- picture loading ----------------

	private static Picture load(byte[] png) {
		if (png == null) {
			return null;
		}
		ImageData img = ImageData.loadImageFile(png, "image/png");
		if (img == null) {
			return null;
		}
		Picture p = new Picture(img);
		// smooth scaling instead of blocky pixels
		GlStateManager.bindTexture(p.texture.getGlTextureId());
		EaglercraftGPU.glTexParameteri(0x0DE1, 0x2801, 0x2601); // min filter: linear
		EaglercraftGPU.glTexParameteri(0x0DE1, 0x2800, 0x2601); // mag filter: linear
		return p;
	}

	private Picture thumb(String name, int[] budget) {
		Picture p = this.thumbs.get(name);
		if (p == null && budget[0] > 0 && !this.failed.containsKey(name)) {
			--budget[0];
			p = load(Screenshots.readThumbnail(name));
			if (p == null) {
				this.failed.put(name, Boolean.TRUE);
			} else {
				this.thumbs.put(name, p);
			}
		}
		return p;
	}

	private void open(int index) {
		this.viewing = index;
		this.deleteArmedAt = 0L;
		this.status = "";
		freeFull();
		initGui();
	}

	private void freeFull() {
		if (this.full != null) {
			this.full.texture.deleteGlTexture();
			this.full = null;
		}
		this.fullName = null;
	}

	@Override
	public void onGuiClosed() {
		freeFull();
		for (Picture p : this.thumbs.values()) {
			p.texture.deleteGlTexture();
		}
		this.thumbs.clear();
	}

	// ---------------- input ----------------

	@Override
	protected void actionPerformed(GuiButton button) {
		switch (button.id) {
		case ID_DONE:
			this.mc.displayGuiScreen(this.parent);
			break;
		case ID_PREV:
			if (this.viewing > 0) {
				open(this.viewing - 1);
			}
			break;
		case ID_NEXT:
			if (this.viewing < this.names.size() - 1) {
				open(this.viewing + 1);
			}
			break;
		case ID_BACK:
			freeFull();
			this.viewing = -1;
			initGui();
			break;
		case ID_DOWNLOAD:
			this.status = Screenshots.download(this.names.get(this.viewing)) ? "Downloaded." : "Still saving, try again in a moment.";
			break;
		case ID_DELETE:
			if (this.deleteArmedAt == 0L) {
				this.deleteArmedAt = System.currentTimeMillis();
				button.displayString = deleteLabel();
			} else {
				deleteViewing();
			}
			break;
		default:
			break;
		}
	}

	private void deleteViewing() {
		String name = this.names.get(this.viewing);
		Screenshots.delete(name);
		Picture t = this.thumbs.remove(name);
		if (t != null) {
			t.texture.deleteGlTexture();
		}
		freeFull();
		int index = this.viewing;
		reload();
		if (this.names.isEmpty()) {
			this.viewing = -1;
			initGui();
		} else {
			open(Math.min(index, this.names.size() - 1));
			this.status = "Deleted.";
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (this.viewing >= 0) {
			if (keyCode == 1) { // Escape: back to the grid
				actionPerformed(new GuiButton(ID_BACK, 0, 0, ""));
				return;
			}
			if (keyCode == 203 && this.viewing > 0) { // Left
				open(this.viewing - 1);
				return;
			}
			if (keyCode == 205 && this.viewing < this.names.size() - 1) { // Right
				open(this.viewing + 1);
				return;
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (this.viewing < 0 && mouseButton == 0) {
			int index = cellAt(mouseX, mouseY);
			if (index >= 0) {
				Widgets.click(1.0F);
				open(index);
			}
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0 && this.viewing < 0) {
			this.scrollTarget = Math.max(0, Math.min(this.maxScroll, this.scrollTarget - Integer.signum(wheel) * 40.0F));
		}
	}

	private float cellX(int i) {
		return this.panelX + 10 + (i % this.columns) * (this.cellW + GAP);
	}

	private float cellY(int i) {
		return this.gridTop + 4 + (i / this.columns) * (this.cellH + GAP) - this.scroll;
	}

	private int cellAt(int mx, int my) {
		if (my < this.gridTop || my >= this.gridBottom) {
			return -1;
		}
		for (int i = 0; i < this.names.size(); i++) {
			if (Widgets.inside(mx, my, cellX(i), cellY(i), this.cellW, this.cellH)) {
				return i;
			}
		}
		return -1;
	}

	// ---------------- drawing ----------------

	/** Draws a picture filling a rectangle, trimming its edges if the shapes differ (so it never looks squashed). */
	private static void drawPictureFilled(Picture p, float x, float y, float w, float h, float alpha) {
		float box = w / h, pic = (float) p.width / p.height;
		float u0 = 0.0F, v0 = 0.0F, u1 = 1.0F, v1 = 1.0F;
		if (pic > box) {
			float keep = box / pic; // picture is wider: trim the left and right
			u0 = (1.0F - keep) / 2.0F;
			u1 = 1.0F - u0;
		} else {
			float keep = pic / box; // picture is taller: trim the top and bottom
			v0 = (1.0F - keep) / 2.0F;
			v1 = 1.0F - v0;
		}
		drawPicture(p, x, y, w, h, u0, v0, u1, v1, alpha);
	}

	/** Draws a picture stretched over a rectangle. */
	private static void drawPicture(Picture p, float x, float y, float w, float h, float alpha) {
		drawPicture(p, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, alpha);
	}

	private static void drawPicture(Picture p, float x, float y, float w, float h, float u0, float v0, float u1,
			float v1, float alpha) {
		GlStateManager.enableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
		GlStateManager.bindTexture(p.texture.getGlTextureId());
		Tessellator t = Tessellator.getInstance();
		WorldRenderer wr = t.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_TEX);
		wr.pos(x, y + h, 0.0D).tex(u0, v1).endVertex();
		wr.pos(x + w, y + h, 0.0D).tex(u1, v1).endVertex();
		wr.pos(x + w, y, 0.0D).tex(u1, v0).endVertex();
		wr.pos(x, y, 0.0D).tex(u0, v0).endVertex();
		t.draw();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void updateScreen() {
		ScreenshotManagerModule.keepSaving(); // finish saving a just-taken screenshot while the gallery is open
		if (this.deleteArmedAt != 0L && System.currentTimeMillis() - this.deleteArmedAt > 4000L) {
			this.deleteArmedAt = 0L;
			for (GuiButton b : this.buttonList) {
				if (b.id == ID_DELETE) {
					b.displayString = deleteLabel();
				}
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		float a = this.appear.update(true);
		if (this.viewing >= 0) {
			drawViewer(mouseX, mouseY, a);
		} else {
			drawGrid(mouseX, mouseY, a);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawGrid(int mouseX, int mouseY, float a) {
		float py = this.panelY + (1.0F - a) * 12.0F;
		RenderUtil.shadow(this.panelX, py + 2, this.panelW, this.panelH, 10, 12, Theme.withAlpha(0xFF000000, 0.5F * a));
		RenderUtil.roundedRect(this.panelX, py, this.panelW, this.panelH, 10, Theme.withAlpha(0xFF2B3646, a));
		RenderUtil.roundedRectGradient(this.panelX + 1, py + 1, this.panelW - 2, this.panelH - 2, 9,
				Theme.withAlpha(0xFF151B24, 0.97F * a), Theme.withAlpha(0xFF0E1218, 0.97F * a));
		TextUtil.drawCentered(this.fontRendererObj, "Screenshots", this.width / 2.0F, py + 9, Theme.accentLight);
		String info = this.names.isEmpty() ? "Press F2 while playing to take one"
				: this.names.size() + (this.names.size() == 1 ? " screenshot" : " screenshots") + "  -  "
						+ megabytes(this.totalBytes) + " MB";
		TextUtil.drawCentered(this.fontRendererObj, info, this.width / 2.0F, py + 20, Theme.textFaded);

		if (this.names.isEmpty()) {
			TextUtil.drawCentered(this.fontRendererObj, "No screenshots yet", this.width / 2.0F,
					(this.gridTop + this.gridBottom) / 2.0F - 4, Theme.text);
			return;
		}

		int rows = (this.names.size() + this.columns - 1) / this.columns;
		this.maxScroll = Math.max(0.0F, rows * (this.cellH + GAP) + 4 - (this.gridBottom - this.gridTop));
		this.scrollTarget = Math.max(0.0F, Math.min(this.maxScroll, this.scrollTarget));
		this.scroll += (this.scrollTarget - this.scroll) * 0.35F;

		int hovered = cellAt(mouseX, mouseY);
		int[] budget = { 2 }; // load at most two previews per frame, so opening the gallery stays smooth
		RenderUtil.pushClip(this.panelX + 1, this.gridTop, this.panelX + this.panelW - 1, this.gridBottom);
		for (int i = 0; i < this.names.size(); i++) {
			float x = cellX(i), y = cellY(i);
			if (y + this.cellH < this.gridTop || y > this.gridBottom) {
				continue;
			}
			String name = this.names.get(i);
			boolean over = i == hovered;
			float imgH = this.cellH - LABEL_H;
			RenderUtil.roundedRect(x - 1, y - 1, this.cellW + 2, imgH + 2, 4,
					Theme.withAlpha(over ? Theme.accent : 0xFF2E3A4B, a));
			Picture p = thumb(name, budget);
			if (p != null) {
				drawPictureFilled(p, x, y, this.cellW, imgH, a);
			} else {
				RenderUtil.rect(x, y, x + this.cellW, y + imgH, Theme.withAlpha(0xFF0B0F15, a));
				TextUtil.drawCentered(this.fontRendererObj, this.failed.containsKey(name) ? "?" : "...", x + this.cellW / 2,
						y + imgH / 2 - 4, Theme.textFaded);
			}
			String label = TextUtil.trim(this.fontRendererObj, Screenshots.prettyDate(name), (int) this.cellW);
			this.fontRendererObj.drawString(label, x, y + imgH + 2, over ? Theme.text : Theme.textFaded, false);
		}
		RenderUtil.popClip();
		if (hovered >= 0) {
			Widgets.handCursor();
		}
	}

	/** Bytes as megabytes with one decimal, like "1.4". */
	private static String megabytes(long bytes) {
		long tenths = Math.round(bytes / 104857.6D);
		return (tenths / 10) + "." + (tenths % 10);
	}

	private void drawViewer(int mouseX, int mouseY, float a) {
		RenderUtil.rect(0, 0, this.width, this.height, 0xC0000000);
		String name = this.names.get(this.viewing);
		if (this.full == null || !name.equals(this.fullName)) {
			freeFull();
			byte[] png = Screenshots.readFull(name); // null while it's still being saved
			if (png != null) {
				this.full = load(png);
				this.fullName = name;
			}
		}
		Picture p = this.full != null ? this.full : this.thumbs.get(name);
		if (p == null) {
			p = thumb(name, new int[] { 1 });
		}
		float maxW = this.width - 24, maxH = this.height - 64;
		if (p != null) {
			float scale = Math.min(maxW / p.width, maxH / p.height);
			float w = p.width * scale, h = p.height * scale;
			float x = (this.width - w) / 2.0F, y = 30 + (maxH - h) / 2.0F;
			RenderUtil.roundedRect(x - 2, y - 2, w + 4, h + 4, 4, 0xFF2E3A4B);
			drawPicture(p, x, y, w, h, 1.0F);
		}
		TextUtil.drawCentered(this.fontRendererObj, Screenshots.prettyDate(name), this.width / 2.0F, 8, Theme.accentLight);
		String sub = (this.viewing + 1) + " of " + this.names.size()
				+ (this.full == null && Screenshots.isSaving(name) ? "  -  still saving..." : "");
		if (!this.status.isEmpty()) {
			sub = sub + "  -  " + this.status;
		}
		TextUtil.drawCentered(this.fontRendererObj, sub, this.width / 2.0F, 18, Theme.textFaded);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return true;
	}
}
