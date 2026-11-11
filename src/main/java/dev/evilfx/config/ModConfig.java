package dev.evilfx.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.evilfx.EvilFXClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON config in .minecraft/config/evilfx.json. Fields missing from the file keep their defaults. */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("evilfx.json");
    private static ModConfig instance = new ModConfig();

    public static ModConfig get() {
        return instance;
    }

    /** Layout/appearance shared by every HUD module. Color is RGB (alpha is added at draw time). */
    public static class HudSettings {
        public boolean enabled = true;
        public float scale = 1.0f;
        public Anchor anchor = Anchor.TOP_LEFT;
        public int offsetX = 4;
        public int offsetY = 4;
        public int color = 0xFFFFFF;
        public boolean shadow = true;
        public boolean background = true;

        public HudSettings() {}

        public HudSettings(Anchor anchor, int offsetX, int offsetY) {
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        public HudSettings off() {
            this.enabled = false;
            return this;
        }
    }

    /** Original vanilla values, kept so the performance tweaks can be undone even after a restart. */
    public static class Backup {
        public String particles;
        public String clouds;
        public Double entityDistance;
    }

    // --- General HUD
    public HudSettings stats = new HudSettings(Anchor.TOP_LEFT, 4, 4);
    public boolean showFps = true;
    public boolean showPing = true;
    public boolean showFrameTime = true;

    public HudSettings keystrokes = new HudSettings(Anchor.BOTTOM_LEFT, 8, 8);
    public boolean showCps = true;

    public HudSettings armor = new HudSettings(Anchor.CENTER_RIGHT, 8, 0);
    public boolean armorShowHeld = true;
    public boolean armorPercent = false;

    // --- View
    public boolean zoomEnabled = true;
    public float zoomMagnification = 4.0f;
    public float zoomSpeed = 0.35f;
    public boolean hitboxes = false;           // vanilla F3+B renderer
    public boolean hitboxCustom = false;       // evilFX custom hitbox renderer
    public boolean hitPlayers = true;
    public boolean hitMobs = true;
    public boolean hitOthers = true;
    public int hitDistance = 48;
    public int hitPlayerColor = 0xFFFFFF;
    public int hitPlayerOpacity = 100;
    public int hitMobColor = 0xFFFFFF;
    public int hitMobOpacity = 65;
    public int hitOtherColor = 0xC8C8C8;
    public int hitOtherOpacity = 65;
    public boolean hitTargetEnabled = true;    // separate color for the entity under your crosshair
    public int hitTargetColor = 0xC8102E;
    public int hitTargetOpacity = 100;

    // --- Crystal PvP
    public HudSettings crystal = new HudSettings(Anchor.TOP_RIGHT, 4, 4);
    public boolean warnEnabled = true;
    public int warnObsidianThreshold = 16; // 0 = off
    public int warnCrystalThreshold = 8;   // 0 = off
    public int warnColor = 0xFF2040;

    public HudSettings totems = new HudSettings(Anchor.TOP_RIGHT, 4, 70);
    public boolean popAlertEnabled = true;
    public boolean popSound = true;

    public HudSettings timing = new HudSettings(Anchor.BOTTOM_CENTER, 0, 70).off();
    public int timingInterval = 4; // ticks between beats
    public boolean timingSound = false;

    // --- Performance
    public boolean perfDecreasedParticles = false;
    public boolean perfNoClouds = false;
    public boolean perfEntityDistance = false;
    public float perfEntityDistanceValue = 0.75f;
    public boolean perfParticleCull = false;
    public int perfParticleDistance = 32;
    public Backup backup = new Backup();

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                ModConfig loaded = GSON.fromJson(r, ModConfig.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                EvilFXClient.LOGGER.error("Could not read config, using defaults", e);
            }
        }
        instance.save();
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception e) {
            EvilFXClient.LOGGER.error("Could not save config", e);
        }
    }
}
