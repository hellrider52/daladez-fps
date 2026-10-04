package com.daladez.fps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    public boolean enabled = true;
    public boolean brain = true;        // learning "AI" that remembers what works where
    public int targetFps = 60;          // the FPS you want to hold
    public int checkSeconds = 3;        // how often the tuner checks your FPS
    public int startLevel = 1;          // 0 = Balanced, 1 = Low, 2 = Potato, 3 = Boom
    public int maxRenderDistance = 8;   // render distance never goes above this

    // Turn off any group you want other mods (or you) to control instead.
    public boolean manageDistances = true;     // render / simulation / entity distance
    public boolean manageEffects = true;       // particles, clouds, AO, shadows, biome blend
    public boolean manageGraphicsMode = true;  // Fast / Fancy
    public boolean disableVsync = true;        // vsync halves FPS on weak GPUs

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("daladez_fps.json");
    private static Config instance = new Config();

    public static Config get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                Config c = GSON.fromJson(Files.readString(PATH), Config.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            System.err.println("[Daladez FPS] Could not read config, using defaults: " + e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[Daladez FPS] Could not save config: " + e);
        }
    }
}
