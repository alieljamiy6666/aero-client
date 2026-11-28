package net.aeroclient.module.performance;

import net.aeroclient.module.Category;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.BooleanSetting;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Skips drawing mobs, players and block entities (chests, signs, banners...)
 * that are completely hidden behind solid blocks. You couldn't see them
 * anyway, so nothing looks different, but busy places like a server spawn
 * get a lot faster.
 *
 * How it works: from your camera it traces straight lines to a grid of points
 * on the sides of the thing that face you, block by block. If every line runs
 * into a solid block (stone, dirt, wood...) the thing is hidden. Glass,
 * leaves, fences, slabs and other see-through blocks never hide anything.
 * Answers are reused for a few frames, and block entities (which never move)
 * are only checked again after the camera moves, so the checking stays cheap.
 *
 * Fair play: this only ever skips things you can't see. It never shows
 * anything extra. Names that normal Minecraft shows through walls (players,
 * named mobs) still show, unless you turn Keep Nametags off.
 */
public class EntityCullingModule extends Module {

	private static EntityCullingModule instance;

	private final BooleanSetting blockEntities = add(new BooleanSetting("Block Entities",
			"Also skip hidden chests, signs, banners, heads and other block entities.", true));
	private final BooleanSetting keepNametags = add(new BooleanSetting("Keep Nametags",
			"Still show the names of hidden players and named mobs through walls, like normal Minecraft does.", true));

	/** Frames to keep a "visible" answer for a mob or player before checking again. */
	private static final int RECHECK_VISIBLE = 4;
	/** Frames to keep a "hidden" answer, short so things don't pop in late when you walk around a corner. */
	private static final int RECHECK_HIDDEN = 2;
	/** Block entities are checked again at least this often, in case a block was broken or a door opened. */
	private static final int RECHECK_BLOCK_ENTITY = 30;
	/** Lines longer than this many block steps count as "visible" (far things are cheap to draw anyway). */
	private static final int MAX_STEPS = 160;
	/** Block steps one frame may spend checking; after that, older answers are reused a little longer. */
	private static final int STEP_BUDGET = 20000;
	/** Points per side of the grid on each face (3 = corners, edge middles and center). */
	private static final int GRID = 3;

	private static int frame;
	private static boolean frameActive;
	private static double camX, camY, camZ;
	/** Goes up every time the camera moves half a block, so still block entities don't need checking again. */
	private static int cameraMoves;
	private static long lastCameraCell = Long.MIN_VALUE;
	private static World world;
	private static int stepsLeft;
	private static int hiddenEntities, hiddenBlockEntities;
	private static int shownHiddenEntities, shownHiddenBlockEntities;

	// the chunk the last block check landed in, so most checks skip the chunk lookup
	private static Chunk cachedChunk;
	private static int cachedChunkX, cachedChunkZ;

	public EntityCullingModule() {
		super("Entity Culling", "Skips drawing mobs, players and chests you can't see because they're behind blocks.",
				Category.RENDER, 0);
		instance = this;
		enabledByDefault();
	}

	@Override
	protected void onDisable() {
		frameActive = false;
		cachedChunk = null;
		world = null;
		shownHiddenEntities = shownHiddenBlockEntities = 0;
	}

	/**
	 * Called by the game at the start of drawing entities each frame. Works out
	 * where the camera is and resets the per-frame budget.
	 */
	public static void beginFrame(Entity viewer, float partialTicks) {
		shownHiddenEntities = hiddenEntities;
		shownHiddenBlockEntities = hiddenBlockEntities;
		hiddenEntities = hiddenBlockEntities = 0;
		frameActive = false;
		if (instance == null || !instance.isEnabled() || viewer == null || viewer.world == null) {
			return;
		}
		if (instance.mc.player != null && instance.mc.player.isSpectator()) {
			return; // spectators fly through walls, so walls don't hide anything
		}
		Vec3d cam = ActiveRenderInfo.projectViewFromEntity(viewer, partialTicks);
		camX = cam.xCoord;
		camY = cam.yCoord;
		camZ = cam.zCoord;
		world = viewer.world;
		cachedChunk = null; // chunks can load and unload between frames
		if (isOpaque(MathHelper.floor(camX), MathHelper.floor(camY), MathHelper.floor(camZ))) {
			return; // camera is inside a block (e.g. third person against a wall): don't guess
		}
		long cell = ((long) MathHelper.floor(camX * 2) & 0x1FFFFF) << 42 | ((long) MathHelper.floor(camY * 2) & 0x1FFFFF) << 21
				| ((long) MathHelper.floor(camZ * 2) & 0x1FFFFF);
		if (cell != lastCameraCell) {
			lastCameraCell = cell;
			++cameraMoves;
		}
		++frame;
		stepsLeft = STEP_BUDGET;
		frameActive = true;
	}

	/** True if this entity is completely behind solid blocks and can be skipped this frame. */
	public static boolean isHidden(Entity e) {
		if (!frameActive || e.isGlowing() || e.ignoreFrustumCheck) {
			return false; // glowing outlines show through walls in normal Minecraft
		}
		int age = frame - e.aeroCullFrame;
		int interval = e.aeroCulled ? RECHECK_HIDDEN : RECHECK_VISIBLE + (e.getEntityId() & 3);
		if (age >= 0 && (age < interval || (stepsLeft <= 0 && age < 20))) {
			if (e.aeroCulled) {
				++hiddenEntities;
			}
			return e.aeroCulled;
		}
		AxisAlignedBB box = e.getRenderBoundingBox();
		// a little bigger than the hitbox, for swinging arms, held items and hats
		boolean hidden = isBoxHidden(box.minX - 0.2D, box.minY, box.minZ - 0.2D, box.maxX + 0.2D, box.maxY + 0.3D,
				box.maxZ + 0.2D);
		e.aeroCulled = hidden;
		e.aeroCullFrame = frame;
		if (hidden) {
			++hiddenEntities;
		}
		return hidden;
	}

	/** True if this block entity (chest, sign...) is completely behind solid blocks. */
	public static boolean isHidden(TileEntity te) {
		if (!frameActive || !instance.blockEntities.isOn() || te instanceof TileEntityBeacon
				|| te instanceof TileEntityEndGateway) {
			return false; // beacon and gateway beams reach far outside their block
		}
		int age = frame - te.aeroCullFrame;
		boolean fresh = age >= 0 && te.aeroCullMoves == cameraMoves && age < RECHECK_BLOCK_ENTITY;
		if (fresh || (stepsLeft <= 0 && age >= 0)) {
			if (te.aeroCulled) {
				++hiddenBlockEntities;
			}
			return te.aeroCulled;
		}
		BlockPos p = te.getPos();
		double x = p.getX(), y = p.getY(), z = p.getZ();
		// points just inside the block, so a sign on a wall isn't "seen" through the wall it hangs on
		double grow = te instanceof TileEntityChest ? 0.95D : -0.05D; // double chests are drawn from one block
		double up = te instanceof TileEntityBanner ? 0.95D : -0.05D; // banners stick out of their block
		boolean hidden = isBoxHidden(x - grow, y - up, z - grow, x + 1 + grow, y + 1 + up, z + 1 + grow);
		te.aeroCulled = hidden;
		te.aeroCullFrame = frame;
		te.aeroCullMoves = cameraMoves;
		if (hidden) {
			++hiddenBlockEntities;
		}
		return hidden;
	}

	/** True if hidden players and named mobs should still show their name tag. */
	public static boolean keepNametags() {
		return instance.keepNametags.isOn();
	}

	/** For the F3 screen: how many entities and block entities were skipped last frame, or null when off. */
	public static String debugLine() {
		if (instance == null || !instance.isEnabled()) {
			return null;
		}
		return "Aero Culling: " + shownHiddenEntities + " entities, " + shownHiddenBlockEntities
				+ " block entities hidden";
	}

	// ---------------- the actual check ----------------

	/**
	 * True if no line from the camera reaches the box. Tries the middle first,
	 * then a grid of points on each side of the box that faces the camera.
	 */
	private static boolean isBoxHidden(double x0, double y0, double z0, double x1, double y1, double z1) {
		// the camera is inside or right next to it: always visible
		if (camX > x0 - 1 && camX < x1 + 1 && camY > y0 - 1 && camY < y1 + 1 && camZ > z0 - 1 && camZ < z1 + 1) {
			return false;
		}
		if (lineClear((x0 + x1) * 0.5D, (y0 + y1) * 0.5D, (z0 + z1) * 0.5D)) {
			return false;
		}
		// each side that faces the camera: the X sides, then Y, then Z
		if (camX < x0 ? faceClear(0, x0, y0, z0, y1, z1) : (camX > x1 && faceClear(0, x1, y0, z0, y1, z1))) {
			return false;
		}
		if (camY < y0 ? faceClear(1, y0, x0, z0, x1, z1) : (camY > y1 && faceClear(1, y1, x0, z0, x1, z1))) {
			return false;
		}
		if (camZ < z0 ? faceClear(2, z0, x0, y0, x1, y1) : (camZ > z1 && faceClear(2, z1, x0, y0, x1, y1))) {
			return false;
		}
		return true;
	}

	/** Tries a GRID x GRID set of points on one side of the box. axis: 0 = X side, 1 = Y side, 2 = Z side. */
	private static boolean faceClear(int axis, double fixed, double a0, double b0, double a1, double b1) {
		for (int i = 0; i < GRID; ++i) {
			double a = a0 + (a1 - a0) * i / (GRID - 1);
			for (int j = 0; j < GRID; ++j) {
				double b = b0 + (b1 - b0) * j / (GRID - 1);
				boolean clear;
				if (axis == 0) {
					clear = lineClear(fixed, a, b);
				} else if (axis == 1) {
					clear = lineClear(a, fixed, b);
				} else {
					clear = lineClear(a, b, fixed);
				}
				if (clear) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Walks block by block from the camera to the point (a 3D version of
	 * drawing a line on graph paper). True if no solid block is in the way,
	 * counting the point's own block (a point inside a wall can't be seen).
	 */
	private static boolean lineClear(double tx, double ty, double tz) {
		double dx = tx - camX, dy = ty - camY, dz = tz - camZ;
		int x = MathHelper.floor(camX), y = MathHelper.floor(camY), z = MathHelper.floor(camZ);
		int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
		int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
		int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);
		double deltaX = stepX == 0 ? Double.MAX_VALUE : 1.0D / Math.abs(dx);
		double deltaY = stepY == 0 ? Double.MAX_VALUE : 1.0D / Math.abs(dy);
		double deltaZ = stepZ == 0 ? Double.MAX_VALUE : 1.0D / Math.abs(dz);
		double nextX = stepX > 0 ? (x + 1 - camX) * deltaX : (stepX < 0 ? (camX - x) * deltaX : Double.MAX_VALUE);
		double nextY = stepY > 0 ? (y + 1 - camY) * deltaY : (stepY < 0 ? (camY - y) * deltaY : Double.MAX_VALUE);
		double nextZ = stepZ > 0 ? (z + 1 - camZ) * deltaZ : (stepZ < 0 ? (camZ - z) * deltaZ : Double.MAX_VALUE);
		for (int steps = 0; steps < MAX_STEPS; ++steps) {
			if (nextX <= nextY && nextX <= nextZ) {
				if (nextX > 1.0D) {
					return true; // the line ends inside this (already checked) block
				}
				x += stepX;
				nextX += deltaX;
			} else if (nextY <= nextZ) {
				if (nextY > 1.0D) {
					return true;
				}
				y += stepY;
				nextY += deltaY;
			} else {
				if (nextZ > 1.0D) {
					return true;
				}
				z += stepZ;
				nextZ += deltaZ;
			}
			--stepsLeft;
			if (isOpaque(x, y, z)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isOpaque(int x, int y, int z) {
		if (y < 0 || y > 255) {
			return false;
		}
		int cx = x >> 4, cz = z >> 4;
		Chunk chunk = cachedChunk;
		if (chunk == null || cx != cachedChunkX || cz != cachedChunkZ) {
			chunk = world.getChunkFromChunkCoords(cx, cz);
			cachedChunk = chunk;
			cachedChunkX = cx;
			cachedChunkZ = cz;
		}
		return chunk.getBlockState(x, y, z).isOpaqueCube();
	}
}
