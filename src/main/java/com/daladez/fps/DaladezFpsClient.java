package com.daladez.fps;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class DaladezFpsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        Brain.load();

        // Press K (changeable in Controls) to open the Daladez FPS menu
        KeyMapping openMenu = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.daladez_fps.open", GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.consumeClick()) {
                mc.setScreen(new DaladezScreen(mc.screen));
            }
            FpsTuner.tick(mc);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Brain.save());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("daladezfps")
                .executes(ctx -> {
                    Config c = Config.get();
                    ctx.getSource().sendFeedback(Component.literal(
                        "Daladez FPS: " + (c.enabled ? "ON" : "OFF")
                            + " | AI " + (c.brain ? "ON" : "OFF")
                            + " | target " + c.targetFps
                            + " | level " + FpsTuner.getLevel() + " (" + FpsTuner.levelName() + ")"
                            + " | now " + Minecraft.getInstance().getFps() + " fps"));
                    return 1;
                })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    Config.get().enabled = true;
                    Config.save();
                    FpsTuner.forceReapply();
                    ctx.getSource().sendFeedback(Component.literal("Daladez FPS enabled"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    Config.get().enabled = false;
                    Config.save();
                    ctx.getSource().sendFeedback(Component.literal("Daladez FPS disabled (your current settings stay as they are)"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("level")
                    .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(0, 3))
                        .executes(ctx -> {
                            FpsTuner.setLevel(IntegerArgumentType.getInteger(ctx, "n"));
                            ctx.getSource().sendFeedback(Component.literal("Level set to " + FpsTuner.getLevel() + " (" + FpsTuner.levelName() + ")"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("target")
                    .then(ClientCommandManager.argument("fps", IntegerArgumentType.integer(20, 240))
                        .executes(ctx -> {
                            Config.get().targetFps = IntegerArgumentType.getInteger(ctx, "fps");
                            Config.save();
                            ctx.getSource().sendFeedback(Component.literal("Target FPS set to " + Config.get().targetFps));
                            return 1;
                        })))
            ));
    }
}
