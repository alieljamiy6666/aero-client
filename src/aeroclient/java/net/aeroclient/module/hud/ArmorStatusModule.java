package net.aeroclient.module.hud;

import net.aeroclient.hud.Anchor;
import net.aeroclient.hud.HudModule;
import net.aeroclient.module.setting.BooleanSetting;
import net.aeroclient.module.setting.ModeSetting;
import net.aeroclient.theme.Animation;
import net.aeroclient.theme.Theme;
import net.aeroclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;

/**
 * Shows the armor you're wearing (and optionally the item in your hand) and
 * how much durability each piece has left.
 *
 * Two styles:
 * <ul>
 * <li><b>Slots</b>: a row of inventory-style slots. Empty slots show a faint
 * outline of the missing piece, a thin bar under each item shows its
 * durability, and a red "!!!" badge warns when a piece is about to break.</li>
 * <li><b>Classic</b>: items with numbers, a percentage or a bar next to them.</li>
 * </ul>
 * Durability colors go green, then yellow, then red as items wear out.
 */
public class ArmorStatusModule extends HudModule {

	private static final int MAX = 5; // helmet, chestplate, leggings, boots, held item
	private static final EntityEquipmentSlot[] ARMOR = { EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
			EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET };
	/** Index into {@link ItemArmor#EMPTY_SLOT_NAMES} (boots first there) for helmet, chest, legs, boots. */
	private static final int[] EMPTY_ICON = { 3, 2, 1, 0 };
	private static final int YELLOW = 0xFFFACC15;
	private static final float WARN_AT = 0.1F; // warn at 10% durability or less
	private static final float ITEM = 16.0F, TILE = 20.0F, GAP = 2.0F, BAR_W = 24.0F;
	private static final int TILE_BORDER = 0xE09A9A9A, TILE_FILL = 0x99000000, BADGE = 0xFFE53935;

	private final ModeSetting position = add(new ModeSetting("Position",
			"Next to Hotbar keeps it right beside your hotbar on any screen size. Dragging it in the HUD editor switches to Free.",
			"Next to Hotbar", "Next to Hotbar", "Free"));
	private final ModeSetting style = add(new ModeSetting("Style",
			"Slots: inventory-style slots with bars. Classic: items with numbers.", "Slots", "Slots", "Classic"));
	private final ModeSetting shape = add(new ModeSetting("Slot Shape", "The shape of each slot in Slots style.",
			"Rounded", "Rounded", "Circle", "Square"));
	private final ModeSetting layout = add(
			new ModeSetting("Layout", "Items in a row or in a column.", "Horizontal", "Horizontal", "Vertical"));
	private final ModeSetting durability = add(new ModeSetting("Durability", "How to show how worn out items are.",
			"Bar", "Bar", "Numbers", "Percent", "None"));
	private final BooleanSetting showEmpty = add(
			new BooleanSetting("Show Empty Slots", "Show a faint outline where a piece of armor is missing.", true));
	private final BooleanSetting warning = add(new BooleanSetting("Low Durability Warning",
			"Show a red !!! badge when something has 10% durability or less.", true));
	private final BooleanSetting showHeld = add(
			new BooleanSetting("Show Held Item", "Also show the item in your hand.", false));
	private final BooleanSetting textShadow = add(
			new BooleanSetting("Text Shadow", "Draw a soft shadow under the numbers.", true));

	// this frame's entries: the item (null for an empty slot) and which kind of slot it is
	private final ItemStack[] stacks = new ItemStack[MAX];
	private final int[] kinds = new int[MAX]; // 0-3 = helmet..boots, 4 = held item
	private int count = 0;

	// cached text and color per entry, rebuilt only when the item or its damage changes
	private final ItemStack[] cachedStack = new ItemStack[MAX];
	private final int[] cachedDamage = new int[MAX], cachedCount = new int[MAX];
	private final String[] texts = new String[MAX];
	private final int[] colors = new int[MAX];
	private final float[] fractions = new float[MAX];
	private String cachedMode = "";

	private ItemStack[] examples;

	public ArmorStatusModule() {
		super("Armor Status", "Shows your armor and held item, and how much durability they have left.", 0,
				Anchor.BOTTOM_CENTER, 0.0F, 0.17F);
		this.showEmpty.visibleWhen(() -> this.style.is("Slots"));
		this.shape.visibleWhen(() -> this.style.is("Slots"));
		for (int i = 0; i < MAX; i++) {
			this.texts[i] = "";
		}
	}

	private boolean slots() {
		return this.style.is("Slots");
	}

	private boolean attached() {
		return this.position.is("Next to Hotbar");
	}

	// ---------------- "Next to Hotbar" position ----------------

	/** Where the hotbar ends on the right, plus room for anything vanilla draws there. */
	private float hotbarRight(float screenW) {
		float right = screenW / 2.0F + 91.0F;
		EntityPlayer p = this.mc.player;
		if (p != null) {
			EnumHandSide offSide = p.getPrimaryHand().opposite();
			if (offSide == EnumHandSide.RIGHT && !p.getHeldItemOffhand().func_190926_b()) {
				right += 29.0F; // the off-hand slot sits on the right for left-handed players
			} else if (offSide == EnumHandSide.LEFT && this.mc.gameSettings.attackIndicator == 2) {
				right += 24.0F; // the "hotbar" attack indicator sits on the right
			}
		}
		return right + 4.0F;
	}

	private boolean fitsBesideHotbar(float screenW) {
		return hotbarRight(screenW) + getWidth() <= screenW;
	}

	private float lastScreenW = 10000.0F; // the screen width from getX, for getY

	@Override
	public float getX(float screenW) {
		this.lastScreenW = screenW;
		if (!attached()) {
			return super.getX(screenW);
		}
		if (!fitsBesideHotbar(screenW)) {
			return Math.max(0.0F, screenW / 2.0F + 91.0F - getWidth()); // small screen: above the hotbar's right end
		}
		return hotbarRight(screenW);
	}

	@Override
	public float getY(float screenH) {
		if (!attached()) {
			return super.getY(screenH);
		}
		float h = getHeight();
		if (!fitsBesideHotbar(this.lastScreenW)) {
			return Math.max(0.0F, screenH - 42.0F - h); // above the hotbar and health bars
		}
		getContentWidth(); // makes sure this frame's items are counted
		if (hasUnderText()) {
			return screenH - h; // numbers under the slots: keep them on screen
		}
		// line the slots up with the hotbar's slots (its row is centered 11 units above the bottom)
		float rowCenter = vertical() ? getContentHeight() - cell() / 2.0F : cell() / 2.0F;
		return screenH - 11.0F - (PADDING + rowCenter) * getScale();
	}

	@Override
	public void setPosition(float x, float y, float screenW, float screenH) {
		if (attached()) {
			this.position.set("Free"); // dragged in the HUD editor: it's placed by hand now
		}
		super.setPosition(x, y, screenW, screenH);
	}

	@Override
	public void resetLayout() {
		super.resetLayout();
		this.position.set("Next to Hotbar");
	}

	private boolean vertical() {
		return this.layout.is("Vertical");
	}

	private boolean bars() {
		return this.durability.is("Bar");
	}

	private boolean numbers() {
		return this.durability.is("Numbers") || this.durability.is("Percent");
	}

	// ---------------- what to show ----------------

	/** Fills {@link #stacks} with this frame's items. */
	private void collect() {
		this.count = 0;
		boolean keepEmpty = slots() && this.showEmpty.isOn();
		EntityPlayer player = this.mc.player;
		boolean anything = false;
		if (player != null && this.mc.world != null) {
			for (int i = 0; i < ARMOR.length; i++) {
				anything |= add(player.getItemStackFromSlot(ARMOR[i]), i, keepEmpty);
			}
			if (this.showHeld.isOn()) {
				anything |= add(player.getHeldItemMainhand(), 4, false);
			}
		}
		if (!anything && (isPreview() || isEditing())) {
			this.count = 0; // nothing equipped: show examples so it can be placed and styled
			ItemStack[] ex = examples();
			for (int i = 0; i < ex.length; i++) {
				if (i < 4 || this.showHeld.isOn()) {
					add(ex[i], i, false);
				}
			}
		}
	}

	/** Adds one entry. Returns true if it's a real item. */
	private boolean add(ItemStack stack, int kind, boolean keepEmpty) {
		boolean real = stack != null && !stack.func_190926_b();
		if (real || keepEmpty) {
			this.stacks[this.count] = real ? stack : null;
			this.kinds[this.count] = kind;
			this.count++;
		}
		return real;
	}

	private ItemStack[] examples() {
		if (this.examples == null) {
			this.examples = new ItemStack[] { new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
					new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS),
					new ItemStack(Items.DIAMOND_SWORD) };
			int[] damage = { 30, 120, 260, 410, 900 }; // the boots are nearly broken, to show the warning
			for (int i = 0; i < this.examples.length; i++) {
				this.examples[i].setItemDamage(damage[i]);
			}
		}
		return this.examples;
	}

	/** Works out text, color and bar fill for every entry (only for items that changed). */
	private void describeAll() {
		String mode = this.durability.get();
		boolean modeChanged = !mode.equals(this.cachedMode);
		this.cachedMode = mode;
		for (int i = 0; i < this.count; i++) {
			ItemStack s = this.stacks[i];
			if (s == null) {
				this.cachedStack[i] = null;
				this.fractions[i] = -1.0F;
				this.texts[i] = "";
				continue;
			}
			int damage = s.getItemDamage(), stackCount = s.func_190916_E();
			if (!modeChanged && s == this.cachedStack[i] && damage == this.cachedDamage[i]
					&& stackCount == this.cachedCount[i]) {
				continue;
			}
			this.cachedStack[i] = s;
			this.cachedDamage[i] = damage;
			this.cachedCount[i] = stackCount;
			if (s.isItemStackDamageable()) {
				int max = s.getMaxDamage(), left = Math.max(0, max - damage);
				float f = max > 0 ? left / (float) max : 1.0F;
				this.fractions[i] = f;
				this.colors[i] = f >= 0.5F ? Theme.mix(YELLOW, Theme.success, (f - 0.5F) * 2.0F)
						: Theme.mix(Theme.danger, YELLOW, f * 2.0F);
				this.texts[i] = mode.equals("Percent") ? Math.round(f * 100.0F) + "%" : Integer.toString(left);
			} else {
				this.fractions[i] = -1.0F; // doesn't wear out: show how many there are instead
				this.colors[i] = Theme.text;
				this.texts[i] = stackCount > 1 ? Integer.toString(stackCount) : "";
			}
		}
	}

	/** Width of the numbers (or classic bar) for one entry. */
	private float extraWidth(FontRenderer font, int i) {
		if (this.fractions[i] >= 0.0F) {
			if (bars()) {
				return slots() ? 0.0F : BAR_W; // in Slots style the bar is inside the slot
			}
			if (!numbers()) {
				return 0.0F;
			}
		}
		return this.texts[i].isEmpty() ? 0.0F : font.getStringWidth(this.texts[i]);
	}

	private float cell() {
		return slots() ? TILE : ITEM;
	}

	/** Width of one column in a row layout: the slot, or the number under it if that's wider. */
	private float columnWidth(FontRenderer font, int i) {
		return Math.max(cell(), extraWidth(font, i));
	}

	private boolean hasUnderText() {
		if (numbers()) {
			return true;
		}
		for (int i = 0; i < this.count; i++) {
			if (this.fractions[i] < 0.0F && !this.texts[i].isEmpty()) {
				return true; // a stack count
			}
		}
		return false;
	}

	// ---------------- size ----------------

	@Override
	protected boolean hasContent() {
		collect();
		for (int i = 0; i < this.count; i++) {
			if (this.stacks[i] != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected float getContentWidth() {
		collect();
		describeAll();
		FontRenderer font = this.mc.fontRendererObj;
		float w = 0.0F;
		if (vertical()) {
			for (int i = 0; i < this.count; i++) {
				float extra = extraWidth(font, i);
				w = Math.max(w, cell() + (extra > 0.0F ? 3.0F + extra : 0.0F));
			}
		} else {
			for (int i = 0; i < this.count; i++) {
				w += columnWidth(font, i) + (i > 0 ? GAP : 0.0F);
			}
		}
		return Math.max(cell(), w);
	}

	@Override
	protected float getContentHeight() {
		int n = Math.max(1, this.count);
		if (vertical()) {
			return n * cell() + (n - 1) * GAP;
		}
		float under = hasUnderText() ? 10.0F : (!slots() && bars() ? 4.0F : 0.0F);
		return cell() + under;
	}

	// ---------------- drawing ----------------

	/** Top-left corner of entry i's slot (or item, in Classic style). */
	private float cellX(FontRenderer font, int i) {
		if (vertical()) {
			return 0.0F;
		}
		float x = 0.0F;
		for (int j = 0; j < i; j++) {
			x += columnWidth(font, j) + GAP;
		}
		return x + (columnWidth(font, i) - cell()) / 2.0F;
	}

	private float cellY(int i) {
		return vertical() ? i * (cell() + GAP) : 0.0F;
	}

	@Override
	protected void renderContent(boolean editing) {
		collect();
		describeAll();
		FontRenderer font = this.mc.fontRendererObj;
		boolean slots = slots();
		float inset = slots ? (TILE - ITEM) / 2.0F : 0.0F;

		if (slots) {
			drawTiles(font);
		}

		// the items (3D items need the item lighting)
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		RenderHelper.enableGUIStandardItemLighting();
		for (int i = 0; i < this.count; i++) {
			if (this.stacks[i] != null) {
				this.mc.getRenderItem().renderItemAndEffectIntoGUI(this.stacks[i],
						(int) (cellX(font, i) + inset), (int) (cellY(i) + inset));
			}
		}
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableDepth();

		drawDurability(font, slots, inset);
		if (this.warning.isOn()) {
			drawWarnings(font);
		}
		GlStateManager.enableDepth();
	}

	/** How round the slots are: 0 = square corners, half the slot = a circle. */
	private float slotRadius() {
		return this.shape.is("Circle") ? TILE / 2.0F : (this.shape.is("Square") ? 0.0F : 6.0F);
	}

	@Override
	protected void drawBackground(float width, float height) {
		float r = !slots() || this.shape.is("Square") ? 3.0F : (this.shape.is("Circle") ? 13.0F : 9.0F);
		RenderUtil.roundedRect(0, 0, width, height, r, this.backgroundColor.getColor());
	}

	/** Inventory-style slots, with a faint outline of the missing piece in empty ones. */
	private void drawTiles(FontRenderer font) {
		float r = slotRadius();
		if (r <= 0.0F) { // square: plain pixel-sharp boxes
			RenderUtil.beginBatch(false);
			for (int i = 0; i < this.count; i++) {
				float x = cellX(font, i), y = cellY(i);
				RenderUtil.batchRect(x, y, x + TILE, y + 1, TILE_BORDER);
				RenderUtil.batchRect(x, y + TILE - 1, x + TILE, y + TILE, TILE_BORDER);
				RenderUtil.batchRect(x, y + 1, x + 1, y + TILE - 1, TILE_BORDER);
				RenderUtil.batchRect(x + TILE - 1, y + 1, x + TILE, y + TILE - 1, TILE_BORDER);
				RenderUtil.batchRect(x + 1, y + 1, x + TILE - 1, y + TILE - 1, TILE_FILL);
			}
			RenderUtil.endBatch();
		} else { // rounded or circle: a see-through fill with a smooth border on top
			for (int i = 0; i < this.count; i++) {
				float x = cellX(font, i), y = cellY(i);
				RenderUtil.roundedRect(x, y, TILE, TILE, r, TILE_FILL);
				RenderUtil.roundedOutline(x, y, TILE, TILE, r, 1.0F, TILE_BORDER);
			}
		}

		boolean bound = false;
		for (int i = 0; i < this.count; i++) {
			if (this.stacks[i] != null || this.kinds[i] > 3) {
				continue;
			}
			if (!bound) {
				this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				GlStateManager.enableBlend();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
				bound = true;
			}
			TextureAtlasSprite sprite = this.mc.getTextureMapBlocks()
					.getAtlasSprite(ItemArmor.EMPTY_SLOT_NAMES[EMPTY_ICON[this.kinds[i]]]);
			float x = cellX(font, i) + 2, y = cellY(i) + 2;
			WorldRenderer wr = Tessellator.getInstance().getBuffer();
			wr.begin(7, DefaultVertexFormats.POSITION_TEX);
			wr.pos(x, y + ITEM, 0.0D).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
			wr.pos(x + ITEM, y + ITEM, 0.0D).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
			wr.pos(x + ITEM, y, 0.0D).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
			wr.pos(x, y, 0.0D).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
			Tessellator.getInstance().draw();
		}
		if (bound) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	/** Bars or numbers for every entry. */
	private void drawDurability(FontRenderer font, boolean slots, float inset) {
		boolean shadow = this.textShadow.isOn();
		for (int i = 0; i < this.count; i++) {
			if (this.stacks[i] == null) {
				continue;
			}
			float cx = cellX(font, i), cy = cellY(i);
			float f = this.fractions[i];
			if (bars() && f >= 0.0F) {
				float bx, by, bw;
				if (slots) { // a thin bar along the bottom of the slot, like the vanilla durability bar
					float side = this.shape.is("Circle") ? 5.0F : 3.0F;
					bx = cx + side;
					by = cy + TILE - 4;
					bw = TILE - side * 2;
				} else if (vertical()) {
					bx = cx + ITEM + 3;
					by = cy + 7;
					bw = BAR_W;
				} else {
					bx = cx;
					by = cy + ITEM + 1;
					bw = ITEM;
				}
				if (slots && slotRadius() > 0.0F) { // rounded bars to match rounded slots
					RenderUtil.pill(bx, by, bw, 2, 0xE6000000);
					RenderUtil.pill(bx, by, Math.max(2.0F, bw * f), 2, this.colors[i]);
				} else {
					RenderUtil.rect(bx, by, bx + bw, by + 2, 0xFF000000);
					RenderUtil.rect(bx, by, bx + Math.max(1.0F, bw * f), by + 1, this.colors[i]);
				}
			} else if (!this.texts[i].isEmpty() && (numbers() || f < 0.0F)) {
				float tw = font.getStringWidth(this.texts[i]);
				float tx, ty;
				if (vertical()) {
					tx = cx + cell() + 3;
					ty = cy + (cell() - 8) / 2.0F;
				} else {
					tx = cx + (cell() - tw) / 2.0F;
					ty = cy + cell() + 2;
				}
				font.drawString(this.texts[i], tx, ty, this.colors[i], shadow);
			}
		}
	}

	/** A pulsing red "!!!" badge on anything that's about to break. */
	private void drawWarnings(FontRenderer font) {
		float pulse = 0.5F + 0.5F * (float) Math.sin(Animation.seconds() * 6.0F);
		for (int i = 0; i < this.count; i++) {
			float f = this.fractions[i];
			if (this.stacks[i] == null || f < 0.0F || f > WARN_AT) {
				continue;
			}
			float cx, cy;
			if (vertical() || !slots()) {
				cx = cellX(font, i) + 2.0F;
				cy = cellY(i) + 2.0F;
			} else {
				cx = cellX(font, i) + TILE / 2.0F;
				cy = cellY(i) - 3.0F;
			}
			float r = 5.0F + 0.6F * pulse;
			RenderUtil.glow(cx - r, cy - r, r * 2, r * 2, r, 3.0F, Theme.withAlpha(BADGE, 0.35F + 0.3F * pulse));
			RenderUtil.circle(cx, cy, r, 0xFF7F1D1D);
			RenderUtil.circle(cx, cy, r - 0.8F, BADGE);
			font.drawString("!!!", cx - 2.5F, cy - 3.5F, 0xFFFFFFFF, false);
		}
	}
}
