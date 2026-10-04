package com.daladez.fps;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

/**
 * Uses only vanilla Options (no mixins), so it can't clash with Sodium, Lithium, Iris, etc.
 */
public final class FpsTuner {
    // particles: 0 = all, 1 = decreased, 2 = minimal
    private record Preset(String name, int render, int sim, double entity, int particles,
                          CloudStatus clouds, boolean ao, boolean shadows, int blend) {}

    // Tuned for integrated graphics / old dual-core CPUs.
    private static final Preset[] PRESETS = {
        new Preset("Balanced", 8, 6, 0.8, 1, CloudStatus.OFF, false, true,  1),
        new Preset("Low",      6, 5, 0.6, 1, CloudStatus.OFF, false, false, 0),
        new Preset("Potato",   4, 5, 0.5, 2, CloudStatus.OFF, false, false, 0),
        new Preset("Boom",     3, 5, 0.5, 2, CloudStatus.OFF, false, false, 0),
    };
    public static final int LEVEL_COUNT = PRESETS.length;

    private static int level = -1;
    private static boolean needsApply = true;
    private static int samples = 0;
    private static long sum = 0;
    private static int secTicks = 0;
    private static long secSum = 0;
    private static int lowSeconds = 0;
    private static int settle = 0;
    private static int raiseCooldown = 0;
    private static String lastCtx = null;

    private FpsTuner() {}

    public static int getLevel() { return level; }
    public static String levelName() { return level < 0 ? "-" : PRESETS[level].name(); }
    public static String levelName(int i) { return PRESETS[i].name(); }

    public static void setLevel(int newLevel) {
        level = Math.max(0, Math.min(newLevel, LEVEL_COUNT - 1));
        needsApply = true;
        resetWindow();
        settle = 60; // ~3s for chunks to reload before measuring again
    }

    public static void forceReapply() {
        needsApply = true;
    }

    private static void resetWindow() {
        samples = 0;
        sum = 0;
        secTicks = 0;
        secSum = 0;
    }

    /** Describes the situation: dimension + outside/cave + rain. Used by the brain. */
    private static String context(Minecraft mc) {
        String d = mc.level.dimension().toString();
        String dim = d.contains("the_nether") ? "nether" : d.contains("the_end") ? "end" : "overworld";
        boolean sky = mc.level.canSeeSky(mc.player.blockPosition());
        return dim + (sky ? "/outside" : "/cave") + (mc.level.isRaining() ? "/rain" : "");
    }

    public static void tick(Minecraft mc) {
        Config c = Config.get();
        if (!c.enabled) return;

        if (level < 0) setLevel(c.startLevel);

        // only tune while actually playing in a focused window
        if (mc.level == null || mc.player == null || mc.isPaused() || !mc.isWindowActive()) {
            resetWindow();
            lastCtx = null;
            return;
        }

        if (needsApply) {
            apply(mc, c);
            needsApply = false;
        }

        if (raiseCooldown > 0) raiseCooldown--;
        if (settle > 0) {
            settle--;
            return;
        }

        int fps = mc.getFps();
        sum += fps;
        samples++;
        secSum += fps;
        secTicks++;

        // ---- once per second ----
        if (secTicks >= 20) {
            double secAvg = (double) secSum / secTicks;
            secTicks = 0;
            secSum = 0;

            String ctx = context(mc);
            if (c.brain && !ctx.equals(lastCtx)) {
                lastCtx = ctx;
                int rec = Brain.recommend(ctx, level, c.targetFps);
                if (rec != level) {
                    setLevel(rec);
                    notify(mc, "Brain: " + ctx + " -> " + PRESETS[level].name());
                    return;
                }
            } else {
                lastCtx = ctx;
            }

            // emergency: FPS crashed for 2 seconds straight, don't wait for the full window
            lowSeconds = secAvg < c.targetFps * 0.5 ? lowSeconds + 1 : 0;
            if (lowSeconds >= 2 && level < LEVEL_COUNT - 1) {
                lowSeconds = 0;
                setLevel(level + 2);
                raiseCooldown = 20 * 40;
                notify(mc, "FPS crash " + Math.round(secAvg) + " -> " + PRESETS[level].name());
                return;
            }
        }

        if (samples < Math.max(1, c.checkSeconds) * 20) return;

        double avg = (double) sum / samples;
        resetWindow();

        if (c.brain && lastCtx != null) Brain.record(lastCtx, level, avg);

        if (avg < c.targetFps - 2 && level < LEVEL_COUNT - 1) {
            int step = avg < c.targetFps * 0.6 ? 2 : 1;
            setLevel(level + step);
            raiseCooldown = 20 * 40;
            notify(mc, "FPS " + Math.round(avg) + " -> " + PRESETS[level].name());
        } else if (avg > c.targetFps * 1.6 && level > 0 && raiseCooldown == 0 && canMeasureHighFps(mc, c)) {
            setLevel(level - 1);
            raiseCooldown = 20 * 40;
            notify(mc, "FPS " + Math.round(avg) + " -> " + PRESETS[level].name());
        }
    }

    private static boolean canMeasureHighFps(Minecraft mc, Config c) {
        if (mc.options.enableVsync().get()) return false;
        return mc.options.framerateLimit().get() >= c.targetFps * 1.6;
    }

    private static void apply(Minecraft mc, Config c) {
        Preset l = PRESETS[level];
        Options o = mc.options;

        if (c.disableVsync && o.enableVsync().get()) o.enableVsync().set(false);

        if (c.manageDistances) {
            int rd = Math.min(l.render(), c.maxRenderDistance);
            if (o.renderDistance().get() != rd) o.renderDistance().set(rd);
            if (o.simulationDistance().get() != l.sim()) o.simulationDistance().set(l.sim());
            o.entityDistanceScaling().set(l.entity());
        }
        if (c.manageEffects) {
            o.particles().set(pick(o.particles().get(), l.particles()));
            o.cloudStatus().set(l.clouds());
            o.ambientOcclusion().set(l.ao());
            o.entityShadows().set(l.shadows());
            o.biomeBlendRadius().set(l.blend());
        }
    }

    /** Picks an enum constant by position without naming its class (names moved around in 1.21.11). */
    private static <E extends Enum<E>> E pick(E current, int ordinal) {
        E[] all = current.getDeclaringClass().getEnumConstants();
        return all[Math.max(0, Math.min(ordinal, all.length - 1))];
    }

    private static void notify(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Daladez FPS] " + msg), true);
        }
    }
}
