package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ExampleModClient implements ClientModInitializer {
    public static boolean showHud = true;
    public static boolean showKeystrokes = true;
    public static boolean autoSprint = true;
    public static boolean fullBright = true;
    public static boolean fpsBoost = true;

    private static final List<Long> leftClicks = new ArrayList<>();
    private static boolean wasAttackPressed = false;

    @Override
    public void onInitializeClient() {
        // 1. Prime Client Engine: FPS Boost, Auto-Sprint, FullBright, CPS & Live HUD
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Auto-Sprint
            if (autoSprint && client.options.keyUp.isDown()) {
                client.player.setSprinting(true);
            }

            // FullBright
            if (fullBright) {
                client.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            } else {
                client.player.removeEffect(MobEffects.NIGHT_VISION);
            }

            // Mobile FPS Booster
            if (fpsBoost) {
                client.options.entityShadows().set(false);
            }

            // Touch & Mouse CPS Counter
            boolean attackPressed = client.options.keyAttack.isDown();
            if (attackPressed && !wasAttackPressed) {
                leftClicks.add(System.currentTimeMillis());
            }
            wasAttackPressed = attackPressed;
            long now = System.currentTimeMillis();
            leftClicks.removeIf(time -> now - time > 1000);

            // Live In-Game HUD Overlay
            if (showHud) {
                String keys = "";
                if (showKeystrokes) {
                    String w = client.options.keyUp.isDown() ? "§b[W]" : "§7W";
                    String a = client.options.keyLeft.isDown() ? "§b[A]" : "§7A";
                    String s = client.options.keyDown.isDown() ? "§b[S]" : "§7S";
                    String d = client.options.keyRight.isDown() ? "§b[D]" : "§7D";
                    keys = " §8| " + w + " " + a + " " + s + " " + d;
                }

                String hudText = String.format(
                    "§b§lPRIME CLIENT §8| §f%d FPS §8| §e%d CPS §8| §aXYZ: %d, %d, %d%s",
                    client.getFps(),
                    leftClicks.size(),
                    (int) client.player.getX(),
                    (int) client.player.getY(),
                    (int) client.player.getZ(),
                    keys
                );
                showInGameHud(client, Component.literal(hudText));
            }
        });

        // 2. Touch Controls Mod Toggles on Pause Screen & Title Screen
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                addButton(screen, Button.builder(getBtnText("Prime HUD", showHud), b -> {
                    showHud = !showHud;
                    b.setMessage(getBtnText("Prime HUD", showHud));
                }).bounds(6, 6, 95, 18).build());

                addButton(screen, Button.builder(getBtnText("Keystrokes", showKeystrokes), b -> {
                    showKeystrokes = !showKeystrokes;
                    b.setMessage(getBtnText("Keystrokes", showKeystrokes));
                }).bounds(105, 6, 95, 18).build());

                addButton(screen, Button.builder(getBtnText("Auto-Sprint", autoSprint), b -> {
                    autoSprint = !autoSprint;
                    b.setMessage(getBtnText("Auto-Sprint", autoSprint));
                }).bounds(204, 6, 95, 18).build());

                addButton(screen, Button.builder(getBtnText("FullBright", fullBright), b -> {
                    fullBright = !fullBright;
                    b.setMessage(getBtnText("FullBright", fullBright));
                }).bounds(6, 26, 95, 18).build());

                addButton(screen, Button.builder(getBtnText("FPS Boost", fpsBoost), b -> {
                    fpsBoost = !fpsBoost;
                    b.setMessage(getBtnText("FPS Boost", fpsBoost));
                }).bounds(105, 26, 95, 18).build());
            }
        });
    }

    private static Component getBtnText(String name, boolean state) {
        return Component.literal(name + ": " + (state ? "§aON" : "§cOFF"));
    }

    private static void addButton(Screen screen, Button button) {
        try {
            for (Method m : Screen.class.getDeclaredMethods()) {
                if (m.getName().equals("addRenderableWidget") && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    m.invoke(screen, button);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void showInGameHud(Minecraft client, Component text) {
        try {
            for (Method m : client.player.getClass().getMethods()) {
                if (m.getName().equals("displayClientMessage") && m.getParameterCount() == 2) {
                    m.invoke(client.player, text, true);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }
}
