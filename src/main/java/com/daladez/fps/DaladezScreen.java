package com.daladez.fps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DaladezScreen extends Screen {
    private final Screen parent;
    private Button targetLabel;

    public DaladezScreen(Screen parent) {
        super(Component.literal("Daladez FPS"));
        this.parent = parent;
    }

    private static Component onOff(String label, boolean v) {
        return Component.literal(label + ": " + (v ? "ON" : "OFF"));
    }

    private void toggle(int x, int y, int w, String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        Button b = Button.builder(onOff(label, get.get()), btn -> {
            set.accept(!get.get());
            btn.setMessage(onOff(label, get.get()));
        }).bounds(x, y, w, 20).build();
        addRenderableWidget(b);
    }

    @Override
    protected void init() {
        Config c = Config.get();
        int w = 150;
        int left = this.width / 2 - w - 2;
        int right = this.width / 2 + 2;
        int y = 42;

        toggle(left, y, w, "Auto-tune", () -> c.enabled, v -> {
            c.enabled = v;
            if (v) FpsTuner.forceReapply();
        });
        toggle(right, y, w, "AI Brain", () -> c.brain, v -> c.brain = v);
        y += 24;

        // quality level buttons
        int lw = 74;
        int x0 = this.width / 2 - (4 * lw + 3 * 4) / 2;
        for (int i = 0; i < FpsTuner.LEVEL_COUNT; i++) {
            final int lvl = i;
            addRenderableWidget(Button.builder(Component.literal(FpsTuner.levelName(i)),
                btn -> FpsTuner.setLevel(lvl)).bounds(x0 + i * (lw + 4), y, lw, 20).build());
        }
        y += 24;

        // target FPS
        addRenderableWidget(Button.builder(Component.literal("-5"), btn -> {
            c.targetFps = Math.max(20, c.targetFps - 5);
            refreshTarget();
        }).bounds(left, y, 40, 20).build());
        targetLabel = Button.builder(Component.literal("Target FPS: " + c.targetFps), btn -> {})
            .bounds(left + 44, y, 2 * w - 88 + 4, 20).build();
        targetLabel.active = false;
        addRenderableWidget(targetLabel);
        addRenderableWidget(Button.builder(Component.literal("+5"), btn -> {
            c.targetFps = Math.min(240, c.targetFps + 5);
            refreshTarget();
        }).bounds(right + w - 40, y, 40, 20).build());
        y += 24;

        toggle(left, y, w, "Distances", () -> c.manageDistances, v -> c.manageDistances = v);
        toggle(right, y, w, "Effects", () -> c.manageEffects, v -> c.manageEffects = v);
        y += 24;

        toggle(left, y, w, "Graphics mode", () -> c.manageGraphicsMode, v -> c.manageGraphicsMode = v);
        toggle(right, y, w, "Vsync off", () -> c.disableVsync, v -> c.disableVsync = v);
        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Reset AI memory"), btn -> Brain.reset())
            .bounds(left, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
            .bounds(right, y, w, 20).build());
    }

    private void refreshTarget() {
        targetLabel.setMessage(Component.literal("Target FPS: " + Config.get().targetFps));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        String status = "FPS " + Minecraft.getInstance().getFps()
            + "  |  Level: " + FpsTuner.levelName()
            + "  |  AI knows " + Brain.knownSituations() + " situations";
        g.drawCenteredString(this.font, Component.literal(status), this.width / 2, 26, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Config.save();
        Brain.save();
        this.minecraft.setScreen(parent);
    }
}
