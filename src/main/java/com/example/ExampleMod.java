package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ExampleMod implements ClientModInitializer {
    public static boolean showHud = true;
    public static boolean autoSprint = true;
    public static boolean fullBright = true;
    public static boolean fpsBoost = true;

    private static final List<Long> leftClicks = new ArrayList<>();
    private static boolean wasAttackPressed = false;

    @Override
    public void onInitializeClient() {
        // 1. Prime Client Engine: Auto-Sprint, FullBright, FPS Booster & CPS
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Auto-Sprint
            if (autoSprint && client.options.forwardKey.isPressed()) {
                client.player.setSprinting(true);
            }

            // FullBright
            if (fullBright) {
                client.options.getGamma().setValue(16.0);
            }

            // Mobile FPS Booster
            if (fpsBoost) {
                client.options.getEntityShadows().setValue(false);
            }

            // CPS Tracker
            boolean attackPressed = client.options.attackKey.isPressed();
            if (attackPressed && !wasAttackPressed) {
                leftClicks.add(System.currentTimeMillis());
            }
            wasAttackPressed = attackPressed;
            long now = System.currentTimeMillis();
            leftClicks.removeIf(time -> now - time > 1000);
        });

        // 2. In-Game HUD Overlay (Watermark, FPS, CPS, Coords & Keystrokes)
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || !showHud) return;

            int fps = mc.getCurrentFps();
            int cps = leftClicks.size();

            // Watermark & Stats Box
            String text = "PRIME CLIENT  |  " + fps + " FPS  |  " + cps + " CPS";
            int width = mc.textRenderer.getWidth(text) + 16;
            context.fill(10, 10, 10 + width, 28, 0xC0101015);
            context.fill(10, 10, 13, 28, 0xFF00E5FF); // Cyan Accent
            context.drawText(mc.textRenderer, text, 18, 15, 0xFFFFFFFF, true);

            // Coordinates
            if (mc.player != null) {
                String coords = String.format("XYZ: %d, %d, %d", (int)mc.player.getX(), (int)mc.player.getY(), (int)mc.player.getZ());
                int cWidth = mc.textRenderer.getWidth(coords) + 16;
                context.fill(10, 31, 10 + cWidth, 47, 0xC0101015);
                context.fill(10, 31, 13, 47, 0xFF00E5FF);
                context.drawText(mc.textRenderer, coords, 18, 36, 0xFFCCCCCC, true);
            }

            // Keystrokes UI (W A S D)
            drawKey(context, mc, "W", 32, 53, 20, 20, mc.options.forwardKey.isPressed());
            drawKey(context, mc, "A", 10, 75, 20, 20, mc.options.leftKey.isPressed());
            drawKey(context, mc, "S", 32, 75, 20, 20, mc.options.backKey.isPressed());
            drawKey(context, mc, "D", 54, 75, 20, 20, mc.options.rightKey.isPressed());
        });
    }

    private static void drawKey(DrawContext context, MinecraftClient mc, String label, int x, int y, int w, int h, boolean pressed) {
        int bg = pressed ? 0xB000E5FF : 0xC0101015;
        int fg = pressed ? 0xFF000000 : 0xFFFFFFFF;
        context.fill(x, y, x + w, y + h, bg);
        int tw = mc.textRenderer.getWidth(label);
        context.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, true);
    }
}
