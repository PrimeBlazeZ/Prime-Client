package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
        // 1. Prime Client Core Engine (FPS Boost, Auto-Sprint, FullBright, CPS)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (autoSprint && client.options.keyUp.isDown()) {
                client.player.setSprinting(true);
            }

            if (fullBright) {
                client.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            } else {
                client.player.removeEffect(MobEffects.NIGHT_VISION);
            }

            if (fpsBoost) {
                client.options.entityShadows().set(false);
            }

            boolean attackPressed = client.options.keyAttack.isDown();
            if (attackPressed && !wasAttackPressed) {
                leftClicks.add(System.currentTimeMillis());
            }
            wasAttackPressed = attackPressed;
            long now = System.currentTimeMillis();
            leftClicks.removeIf(time -> now - time > 1000);
        });

        // 2. Dawn-Style Touch Dashboard Button on Title & Pause Menus
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                addButton(screen, Button.builder(Component.literal("⚙ Prime Client"), btn -> {
                    client.setScreen(new PrimeDawnMenu(screen));
                }).bounds(10, 10, 105, 20).build());
            }
        });

        // 3. Render Real Visual Dawn Client HUD & Keystrokes Overlay on Screen
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui) return;
            renderDawnOverlay(guiGraphics, mc);
        });
    }

    private static void renderDawnOverlay(GuiGraphics g, Minecraft mc) {
        if (!showHud) return;

        // --- DAWN CLIENT WATERMARK & FPS BOX ---
        String text = "PRIME CLIENT  |  " + mc.getFps() + " FPS  |  " + leftClicks.size() + " CPS";
        int width = mc.font.width(text) + 16;
        
        // Semi-transparent dark background box with Dawn Cyan accent line on left
        g.fill(10, 10, 10 + width, 30, 0xC0101015);
        g.fill(10, 10, 13, 30, 0xFF00E5FF); 
        g.drawString(mc.font, text, 18, 16, 0xFFFFFFFF, true);

        // --- COORDINATES BOX ---
        if (mc.player != null) {
            String coords = String.format("XYZ: %d, %d, %d", (int)mc.player.getX(), (int)mc.player.getY(), (int)mc.player.getZ());
            int cWidth = mc.font.width(coords) + 16;
            g.fill(10, 33, 10 + cWidth, 53, 0xC0101015);
            g.fill(10, 33, 13, 53, 0xFF00E5FF);
            g.drawString(mc.font, coords, 18, 39, 0xFFCCCCCC, true);
        }

        // --- DAWN KEYSTROKES UI ---
        if (showKeystrokes) {
            int kx = 10;
            int ky = 60;
            drawKey(g, mc, "W", kx + 22, ky, 20, 20, mc.options.keyUp.isDown());
            drawKey(g, mc, "A", kx, ky + 22, 20, 20, mc.options.keyLeft.isDown());
            drawKey(g, mc, "S", kx + 22, ky + 22, 20, 20, mc.options.keyDown.isDown());
            drawKey(g, mc, "D", kx + 44, ky + 22, 20, 20, mc.options.keyRight.isDown());
            drawKey(g, mc, "LMB", kx, ky + 44, 31, 18, mc.options.keyAttack.isDown());
            drawKey(g, mc, "RMB", kx + 33, ky + 44, 31, 18, mc.options.keyUse.isDown());
        }
    }

    private static void drawKey(GuiGraphics g, Minecraft mc, String label, int x, int y, int w, int h, boolean pressed) {
        int bg = pressed ? 0xB000E5FF : 0xC0101015;
        int fg = pressed ? 0xFF000000 : 0xFFFFFFFF;
        g.fill(x, y, x + w, y + h, bg);
        int tw = mc.font.width(label);
        g.drawString(mc.font, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, false);
    }

    // --- DAWN CLIENT SETTINGS DASHBOARD SCREEN ---
    public static class PrimeDawnMenu extends Screen {
        private final Screen parent;

        protected PrimeDawnMenu(Screen parent) {
            super(Component.literal("Prime Client Dashboard"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int cx = this.width / 2 - 80;
            int cy = this.height / 2 - 60;

            this.addRenderableWidget(Button.builder(getBtnText("HUD Overlay", showHud), b -> {
                showHud = !showHud;
                b.setMessage(getBtnText("HUD Overlay", showHud));
            }).bounds(cx, cy, 160, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("Keystrokes", showKeystrokes), b -> {
                showKeystrokes = !showKeystrokes;
                b.setMessage(getBtnText("Keystrokes", showKeystrokes));
            }).bounds(cx, cy + 24, 160, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("Auto-Sprint", autoSprint), b -> {
                autoSprint = !autoSprint;
                b.setMessage(getBtnText("Auto-Sprint", autoSprint));
            }).bounds(cx, cy + 48, 160, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("FullBright", fullBright), b -> {
                fullBright = !fullBright;
                b.setMessage(getBtnText("FullBright", fullBright));
            }).bounds(cx, cy + 72, 160, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("FPS Booster", fpsBoost), b -> {
                fpsBoost = !fpsBoost;
                b.setMessage(getBtnText("FPS Booster", fpsBoost));
            }).bounds(cx, cy + 96, 160, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> {
                Minecraft.getInstance().setScreen(this.parent);
            }).bounds(cx, cy + 124, 160, 20).build());
        }

        private Component getBtnText(String name, boolean state) {
            return Component.literal(name + ": " + (state ? "§aON" : "§cOFF"));
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, this.width, this.height, 0xB0000000); // Dark blur overlay look
            int bw = 200, bh = 165;
            int bx = this.width / 2 - bw / 2;
            int by = this.height / 2 - bh / 2;
            g.fill(bx, by, bx + bw, by + bh, 0xF014141B); // Dawn panel background
            g.fill(bx, by, bx + bw, by + 3, 0xFF00E5FF);   // Cyan top border line
            
            super.render(g, mouseX, mouseY, delta);
            g.drawCenteredString(this.font, "PRIME CLIENT DASHBOARD", this.width / 2, by + 12, 0xFF00E5FF);
        }
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
													}
