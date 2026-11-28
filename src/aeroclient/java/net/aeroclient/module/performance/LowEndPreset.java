package net.aeroclient.module.performance;

import net.aeroclient.AeroClient;
import net.aeroclient.config.ConfigManager;
import net.aeroclient.gui.AeroMenuBackground;
import net.aeroclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

/**
 * The "Low-end Preset" button in Aero Settings: one click to make the game as
 * light as possible for slow Chromebooks and old laptops.
 */
public final class LowEndPreset {

	/** What the button changes, shown as its tooltip. */
	public static final String[] CHANGES = {
			"Turns on every performance option: Entity Culling, Particle Limit (400), Smart Chunk Updates,"
					+ " Fast Text and Background FPS Limit (5 FPS).",
			"Render distance: 4 chunks at most",
			"Graphics: Fast",
			"Smooth Lighting: Off",
			"Clouds: Off",
			"Particles: Decreased",
			"Entity Shadows: Off",
			"Menu background: still image" };

	private LowEndPreset() {
	}

	public static void apply() {
		for (Module m : AeroClient.MODULES.getModules()) {
			if (m instanceof EntityCullingModule || m instanceof FastTextModule) {
				m.setEnabled(true);
			} else if (m instanceof ParticleLimitModule) {
				((ParticleLimitModule) m).applyLowEnd();
				m.setEnabled(true);
			} else if (m instanceof SmartChunkUpdatesModule) {
				((SmartChunkUpdatesModule) m).applyLowEnd();
				m.setEnabled(true);
			} else if (m instanceof BackgroundFpsModule) {
				((BackgroundFpsModule) m).applyLowEnd();
				m.setEnabled(true);
			}
		}
		AeroMenuBackground.animated = false;
		ConfigManager.markDirty();

		Minecraft mc = Minecraft.getMinecraft();
		GameSettings gs = mc.gameSettings;
		gs.renderDistanceChunks = Math.min(gs.renderDistanceChunks, 4);
		gs.fancyGraphics = false;
		gs.ambientOcclusion = 0;
		gs.clouds = 0;
		gs.particleSetting = Math.max(gs.particleSetting, 1);
		gs.entityShadows = false;
		gs.saveOptions();
		if (mc.renderGlobal != null && mc.world != null) {
			mc.renderGlobal.loadRenderers(); // rebuild the world with the new settings
		}
		AeroClient.LOGGER.info("Low-end preset applied");
	}
}
