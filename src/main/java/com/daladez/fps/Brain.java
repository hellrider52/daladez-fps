package com.daladez.fps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny online-learning "brain". For every situation (dimension, outside/cave, rain) it keeps a
 * running average of the FPS each quality level actually gave you on THIS pc. When you enter a
 * situation it already knows, it jumps straight to the best level instead of stuttering first.
 * Costs almost nothing: a few numbers in a map, saved to config/daladez_fps_brain.json.
 */
public final class Brain {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("daladez_fps_brain.json");
    private static final double ALPHA = 0.3; // how fast new data replaces old
    private static Map<String, double[]> data = new HashMap<>();
    private static int unsaved = 0;

    private Brain() {}

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                Type t = new TypeToken<Map<String, double[]>>() {}.getType();
                Map<String, double[]> m = GSON.fromJson(Files.readString(PATH), t);
                if (m != null) {
                    data = m;
                    data.replaceAll((k, v) -> Arrays.copyOf(v, FpsTuner.LEVEL_COUNT));
                }
            }
        } catch (Exception e) {
            System.err.println("[Daladez FPS] Could not read brain, starting fresh: " + e);
            data = new HashMap<>();
        }
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(data));
            unsaved = 0;
        } catch (Exception e) {
            System.err.println("[Daladez FPS] Could not save brain: " + e);
        }
    }

    public static void record(String ctx, int level, double fps) {
        double[] a = data.computeIfAbsent(ctx, k -> new double[FpsTuner.LEVEL_COUNT]);
        a[level] = a[level] == 0 ? fps : a[level] * (1 - ALPHA) + fps * ALPHA;
        if (++unsaved >= 20) save();
    }

    /** Best quality level known to hit the target in this situation, or the current one if unsure. */
    public static int recommend(String ctx, int current, int target) {
        double[] a = data.get(ctx);
        if (a == null) return current;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >= target * 1.05) return i;
        }
        return current;
    }

    public static int knownSituations() {
        return data.size();
    }

    public static void reset() {
        data.clear();
        save();
    }
}
