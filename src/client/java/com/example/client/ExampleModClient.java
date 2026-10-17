package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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
        // 1. Prime Client Engine: Auto-Sprint, FullBright, FPS Booster & CPS
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Auto-Sprint
            if (autoSprint && client.options.keyUp.isDown()) {
                client.player.setSprinting(true);
            }

            // FullBright
            if (fullBright) {
                client.options.gamma().set(16.0);
            }

            // Mobile FPS Booster (Disables entity shadows)
            if (fpsBoost) {
                client.options.entityShadows().set(false);
            }

            // CPS Tracker
            boolean attackPressed = client.options.keyAttack.isDown();
            if (attackPressed && !wasAttackPressed) {
                leftClicks.add(System.currentTimeMillis());
            }
            wasAttackPressed = attackPressed;
            long now = System.currentTimeMillis();
            leftClicks.removeIf(time -> now - time > 1000);
        });

        // 2. Render In-Game HUD, FPS, CPS, Coords & Keystrokes
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || !showHud) return;

            // Watermark & FPS Box
            String badge = "PRIME CLIENT  |  " + mc.getFps() + " FPS  |  " + leftClicks.size() + " CPS";
            int width = mc.font.width(badge) + 16;
            graphics.fill(10, 10, 10 + width, 28, 0xC0101015);
            graphics.fill(10, 10, 13, 28, 0xFF00E5FF); // Cyan Accent Line
            graphics.drawString(mc.font, badge, 18, 16, 0xFFFFFFFF, true);

            // Coordinates Box
            if (mc.player != null) {
                String coords = String.format("XYZ: %d, %d, %d", (int)mc.player.getX(), (int)mc.player.getY(), (int)mc.player.getZ());
                int cWidth = mc.font.width(coords) + 16;
                graphics.fill(10, 31, 10 + cWidth, 47, 0xC0101015);
                graphics.fill(10, 31, 13, 47, 0xFF00E5FF);
                graphics.drawString(mc.font, coords, 18, 37, 0xFFCCCCCC, true);
            }

            // Keystrokes UI (W, A, S, D)
            if (showKeystrokes) {
                int kx = 10;
                int ky = 53;
                drawKey(graphics, mc, "W", kx + 22, ky, 20, 20, mc.options.keyUp.isDown());
                drawKey(graphics, mc, "A", kx, ky + 22, 20, 20, mc.options.keyLeft.isDown());
                drawKey(graphics, mc, "S", kx + 22, ky + 22, 20, 20, mc.options.keyDown.isDown());
                drawKey(graphics, mc, "D", kx + 44, ky + 22, 20, 20, mc.options.keyRight.isDown());
            }
        });
    }

    private static void drawKey(GuiGraphics graphics, Minecraft mc, String label, int x, int y, int w, int h, boolean pressed) {
        int bg = pressed ? 0xB000E5FF : 0xC0101015;
        int fg = pressed ? 0xFF000000 : 0xFFFFFFFF;
        graphics.fill(x, y, x + w, y + h, bg);
        int tw = mc.font.width(label);
        graphics.drawString(mc.font, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, true);
    }
}
