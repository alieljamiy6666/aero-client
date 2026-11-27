package net.aeroclient.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import net.aeroclient.AeroClient;
import net.aeroclient.event.TickEvent;
import net.aeroclient.gui.AeroButtonStyle;
import net.aeroclient.gui.AeroMainMenu;
import net.aeroclient.gui.AeroMenuBackground;
import net.aeroclient.module.Module;
import net.aeroclient.module.setting.Setting;
import net.aeroclient.theme.Theme;
import net.lax1dude.eaglercraft.EagRuntime;
import net.minecraft.client.Minecraft;

/**
 * Saves and loads everything Aero Client remembers, as JSON text in the
 * browser's storage (the same place Eaglercraft keeps your game options).
 *
 * What's saved:
 * - client settings: accent color, rounded buttons, animated background
 * - profiles (like "PvP" or "Low-end PC"), each with every module's on/off
 *   state, key, settings and extra data (HUD modules keep their position there)
 *
 * Saving happens automatically about a second after the last change, so
 * dragging a slider doesn't save on every frame.
 *
 * Broken or unreadable data never stops the game: it's copied to a backup
 * slot and Aero Client starts fresh with defaults. Values that don't fit (an
 * unknown mode, a number out of range, a module that no longer exists) are
 * skipped one by one, so an old config still loads everything it can.
 */
public final class ConfigManager {

	public static final int VERSION = 1;
	public static final String[] STARTER_PROFILES = { "Default", "PvP", "Building", "Low-end PC" };
	public static final int MAX_PROFILE_NAME = 24;
	public static final int MAX_PROFILES = 8;

	private static final String STORAGE_KEY = "aeroclient";
	private static final String BACKUP_KEY = "aeroclient_backup";
	private static final String EXPORT_MARKER = "aeroClientProfile";
	private static final long SAVE_DELAY_MS = 1000L; // wait this long after the last change...
	private static final long SAVE_MAX_WAIT_MS = 5000L; // ...but never longer than this after the first

	private static final Map<String, JSONObject> profiles = new LinkedHashMap<>();
	private static String activeProfile = STARTER_PROFILES[0];

	private static boolean ready = false;
	private static boolean loading = false;
	private static boolean dirty = false;
	private static long firstChange = 0L, lastChange = 0L;

	private ConfigManager() {
	}

	// ---------------- startup and auto-save ----------------

	/** Loads the saved config and starts auto-saving. Call after every module is registered. */
	public static void init() {
		load();
		ready = true;
		AeroClient.EVENTS.subscribe(TickEvent.class, e -> tick());
	}

	/** Something changed: save soon. Safe to call any time, as often as you like. */
	public static void markDirty() {
		if (!ready || loading) {
			return;
		}
		long now = Minecraft.getSystemTime();
		if (!dirty) {
			dirty = true;
			firstChange = now;
		}
		lastChange = now;
	}

	private static void tick() {
		if (!dirty) {
			return;
		}
		long now = Minecraft.getSystemTime();
		if (now - lastChange >= SAVE_DELAY_MS || now - firstChange >= SAVE_MAX_WAIT_MS) {
			save();
		}
	}

	// ---------------- saving ----------------

	/** Saves right now. Usually you want {@link #markDirty()} instead. */
	public static void save() {
		dirty = false;
		try {
			profiles.put(activeProfile, captureModules());
			EagRuntime.setStorage(STORAGE_KEY, toJson().toString().getBytes(StandardCharsets.UTF_8));
		} catch (Throwable t) {
			AeroClient.LOGGER.error("Couldn't save the Aero Client config");
			AeroClient.LOGGER.error(t);
		}
	}

	private static JSONObject toJson() {
		JSONObject root = new JSONObject();
		root.put("version", VERSION);
		root.put("client", captureClient());
		root.put("activeProfile", activeProfile);
		JSONObject all = new JSONObject();
		JSONArray order = new JSONArray(); // JSON objects don't keep their order, so save it separately
		for (Map.Entry<String, JSONObject> e : profiles.entrySet()) {
			all.put(e.getKey(), e.getValue());
			order.put(e.getKey());
		}
		root.put("profiles", all);
		root.put("profileOrder", order);
		return root;
	}

	private static JSONObject captureClient() {
		JSONObject client = new JSONObject();
		client.put("accent", colorToText(Theme.accent));
		client.put("roundedButtons", AeroButtonStyle.enabled);
		client.put("animatedBackground", AeroMenuBackground.animated);
		client.put("classicMenu", AeroMainMenu.classicMode);
		return client;
	}

	/** Every module's state, keys, settings and extra data, as one JSON profile. */
	private static JSONObject captureModules() {
		JSONObject modules = new JSONObject();
		for (Module m : AeroClient.MODULES.getModules()) {
			JSONObject mod = new JSONObject();
			mod.put("enabled", m.isEnabled());
			mod.put("key", m.getKeybind());
			JSONObject settings = new JSONObject();
			for (Setting<?> s : m.getSettings()) {
				settings.put(s.getName(), s.toJson());
			}
			mod.put("settings", settings);
			JSONObject extra = new JSONObject();
			try {
				m.saveExtra(extra);
			} catch (Throwable t) {
				AeroClient.LOGGER.error("Module " + m.getName() + " couldn't save its extra data");
			}
			if (extra.length() > 0) {
				mod.put("extra", extra);
			}
			modules.put(m.getName(), mod);
		}
		JSONObject profile = new JSONObject();
		profile.put("modules", modules);
		return profile;
	}

	// ---------------- loading ----------------

	private static void load() {
		loading = true;
		try {
			profiles.clear();
			byte[] data = null;
			try {
				data = EagRuntime.getStorage(STORAGE_KEY);
			} catch (Throwable t) {
				AeroClient.LOGGER.error("Couldn't read browser storage");
			}
			JSONObject root = null;
			if (data != null && data.length > 0) {
				try {
					root = new JSONObject(new String(data, StandardCharsets.UTF_8));
				} catch (Throwable t) {
					AeroClient.LOGGER.error("The Aero Client config was broken. Keeping a backup and starting fresh.");
					try {
						EagRuntime.setStorage(BACKUP_KEY, data);
					} catch (Throwable t2) {
						// nothing more we can do
					}
				}
			}
			if (root == null) {
				startFresh();
			} else {
				readRoot(root);
			}
			applyModules(profiles.get(activeProfile));
			AeroClient.LOGGER.info("Config loaded: profile \"" + activeProfile + "\" (" + profiles.size() + " profiles)");
		} catch (Throwable t) {
			AeroClient.LOGGER.error("Couldn't load the Aero Client config, using defaults");
			AeroClient.LOGGER.error(t);
			startFresh();
		} finally {
			loading = false;
		}
	}

	private static void startFresh() {
		profiles.clear();
		for (String name : STARTER_PROFILES) {
			profiles.put(name, new JSONObject());
		}
		activeProfile = STARTER_PROFILES[0];
	}

	private static void readRoot(JSONObject root) {
		int version = root.optInt("version", 0);
		if (version > VERSION) {
			AeroClient.LOGGER.warn("This config is from a newer Aero Client (v" + version + "). Loading what we can.");
		}
		// (Later versions convert older configs here, before reading.)

		JSONObject client = root.optJSONObject("client");
		if (client != null) {
			applyClient(client);
		}

		JSONObject all = root.optJSONObject("profiles");
		JSONArray order = root.optJSONArray("profileOrder");
		if (all != null && order != null) { // saved order first...
			for (int i = 0; i < order.length(); i++) {
				String name = order.optString(i, "");
				JSONObject profile = all.optJSONObject(name);
				if (profile != null && isValidName(name) && profiles.size() < MAX_PROFILES) {
					profiles.put(name, profile);
				}
			}
		}
		if (all != null) { // ...then any profile the order didn't mention
			Iterator<String> names = all.keys();
			while (names.hasNext()) {
				String name = names.next();
				JSONObject profile = all.optJSONObject(name);
				if (profile != null && isValidName(name) && !profiles.containsKey(name) && profiles.size() < MAX_PROFILES) {
					profiles.put(name, profile);
				}
			}
		}
		if (profiles.isEmpty()) {
			for (String name : STARTER_PROFILES) {
				profiles.put(name, new JSONObject());
			}
		}
		String active = root.optString("activeProfile", "");
		activeProfile = profiles.containsKey(active) ? active : profiles.keySet().iterator().next();
	}

	private static void applyClient(JSONObject client) {
		Integer accent = textToColor(client.optString("accent", ""));
		if (accent != null) {
			Theme.setAccent(accent);
		}
		Object rounded = client.opt("roundedButtons");
		if (rounded instanceof Boolean) {
			AeroButtonStyle.enabled = (Boolean) rounded;
		}
		Object animated = client.opt("animatedBackground");
		if (animated instanceof Boolean) {
			AeroMenuBackground.animated = (Boolean) animated;
		}
		Object classic = client.opt("classicMenu");
		if (classic instanceof Boolean) {
			AeroMainMenu.classicMode = (Boolean) classic;
		}
	}

	/** Puts every module back to default, then applies whatever this profile has saved. */
	private static void applyModules(JSONObject profile) {
		JSONObject modules = profile == null ? null : profile.optJSONObject("modules");
		for (Module m : AeroClient.MODULES.getModules()) {
			try {
				m.resetToDefaults();
				JSONObject mod = modules == null ? null : modules.optJSONObject(m.getName());
				if (mod == null) {
					continue;
				}
				Object key = mod.opt("key");
				if (key instanceof Number) {
					int k = ((Number) key).intValue();
					if (k >= 0 && k <= 512) {
						m.setKeybind(k);
					}
				}
				JSONObject settings = mod.optJSONObject("settings");
				if (settings != null) {
					for (Setting<?> s : m.getSettings()) {
						if (settings.has(s.getName())) {
							try {
								s.fromJson(settings.get(s.getName()));
							} catch (Throwable t) {
								// one bad value: keep the default for this setting only
							}
						}
					}
				}
				JSONObject extra = mod.optJSONObject("extra");
				if (extra != null) {
					try {
						m.loadExtra(extra);
					} catch (Throwable t) {
						AeroClient.LOGGER.warn("Module " + m.getName() + " couldn't load its extra data");
					}
				}
				if (mod.optBoolean("enabled", false)) {
					m.setEnabled(true); // last, so it turns on with its settings already in place
				}
			} catch (Throwable t) {
				AeroClient.LOGGER.error("Couldn't load module " + m.getName() + ", keeping its defaults");
			}
		}
	}

	// ---------------- profiles ----------------

	public static List<String> getProfileNames() {
		return new ArrayList<>(profiles.keySet());
	}

	public static String getActiveProfile() {
		return activeProfile;
	}

	/** Switches to another profile: modules change to whatever that profile saved. */
	public static void switchProfile(String name) {
		if (!profiles.containsKey(name) || name.equals(activeProfile)) {
			return;
		}
		profiles.put(activeProfile, captureModules());
		activeProfile = name;
		loading = true;
		try {
			applyModules(profiles.get(name));
		} finally {
			loading = false;
		}
		save();
	}

	/**
	 * Makes a new profile. With {@code copyCurrent} it starts as a copy of the
	 * current one; otherwise everything starts at defaults. Returns null if it
	 * worked, or a message explaining what's wrong with the name.
	 */
	public static String createProfile(String name, boolean copyCurrent) {
		name = name == null ? "" : name.trim();
		String problem = nameProblem(name);
		if (problem != null) {
			return problem;
		}
		profiles.put(name, copyCurrent ? captureModules() : new JSONObject());
		save();
		return null;
	}

	/** Deletes a profile (the last one can't be deleted). Switches away first if it's active. */
	public static boolean deleteProfile(String name) {
		if (!profiles.containsKey(name) || profiles.size() <= 1) {
			return false;
		}
		if (name.equals(activeProfile)) {
			for (String other : profiles.keySet()) {
				if (!other.equals(name)) {
					switchProfile(other);
					break;
				}
			}
		}
		profiles.remove(name);
		save();
		return true;
	}

	private static String nameProblem(String name) {
		if (profiles.size() >= MAX_PROFILES) {
			return "You can have up to " + MAX_PROFILES + " profiles";
		}
		if (name.isEmpty()) {
			return "Type a name first";
		}
		if (name.length() > MAX_PROFILE_NAME) {
			return "Names can be up to " + MAX_PROFILE_NAME + " letters";
		}
		for (String existing : profiles.keySet()) {
			if (existing.equalsIgnoreCase(name)) {
				return "There's already a profile called " + existing;
			}
		}
		return null;
	}

	private static boolean isValidName(String name) {
		return name != null && !name.trim().isEmpty() && name.length() <= MAX_PROFILE_NAME;
	}

	// ---------------- import and export ----------------

	/** The profile as one line of text that can be shared and imported elsewhere. */
	public static String exportProfile(String name) {
		if (name.equals(activeProfile)) {
			profiles.put(activeProfile, captureModules());
		}
		JSONObject profile = profiles.get(name);
		JSONObject out = new JSONObject();
		out.put(EXPORT_MARKER, VERSION);
		out.put("name", name);
		out.put("modules", profile == null ? new JSONObject() : profile.optJSONObject("modules") == null
				? new JSONObject() : profile.optJSONObject("modules"));
		return out.toString();
	}

	/**
	 * Adds a profile from text made by {@link #exportProfile}. Returns the new
	 * profile's name, or throws an IllegalArgumentException with a friendly
	 * message if the text isn't an Aero Client profile.
	 */
	public static String importProfile(String text) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("Paste a profile first (Ctrl+V)");
		}
		JSONObject in;
		try {
			in = new JSONObject(text.trim());
		} catch (Throwable t) {
			throw new IllegalArgumentException("That isn't an Aero Client profile");
		}
		if (!in.has(EXPORT_MARKER) || in.optJSONObject("modules") == null) {
			throw new IllegalArgumentException("That isn't an Aero Client profile");
		}
		String base = in.optString("name", "Imported").trim();
		if (!isValidName(base)) {
			base = "Imported";
		}
		if (profiles.size() >= MAX_PROFILES) {
			throw new IllegalArgumentException("You can have up to " + MAX_PROFILES + " profiles");
		}
		String name = base;
		for (int n = 2; nameProblem(name) != null; n++) {
			String suffix = " (" + n + ")";
			name = (base.length() + suffix.length() > MAX_PROFILE_NAME
					? base.substring(0, MAX_PROFILE_NAME - suffix.length()) : base) + suffix;
		}
		JSONObject profile = new JSONObject();
		profile.put("modules", in.getJSONObject("modules"));
		profiles.put(name, profile);
		save();
		return name;
	}

	// ---------------- helpers ----------------

	private static String colorToText(int color) {
		String hex = Integer.toHexString(color).toUpperCase();
		while (hex.length() < 8) {
			hex = "0" + hex;
		}
		return "#" + hex;
	}

	private static Integer textToColor(String text) {
		String s = text.startsWith("#") ? text.substring(1) : text;
		try {
			if (s.length() == 6) {
				return 0xFF000000 | Integer.parseInt(s, 16);
			} else if (s.length() == 8) {
				return (int) Long.parseLong(s, 16);
			}
		} catch (NumberFormatException e) {
			// fall through
		}
		return null;
	}
}
