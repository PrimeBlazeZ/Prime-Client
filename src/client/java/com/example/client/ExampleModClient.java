package com.example;

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
        // 1. Engine: Auto-Sprint, FullBright, CPS & FPS Booster
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

        // 2. Touch Button on Main Menu & Pause Menu
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                Screens.getButtons(screen).add(
                    Button.builder(Component.literal("Prime Client"), btn -> {
                        client.setScreen(new PrimeDawnMenu(screen));
                    }).bounds(10, 10, 95, 20).build()
                );
            }
        });

        // 3. Render HUD & Keystrokes
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            renderPrimeHud(guiGraphics, Minecraft.getInstance());
        });
    }

    public static void renderPrimeHud(GuiGraphics context, Minecraft mc) {
        if (mc.options.hideGui) return;

        if (showHud) {
            String badge = "PRIME CLIENT | " + mc.getFps() + " FPS | " + leftClicks.size() + " CPS";
            int width = mc.font.width(badge) + 12;
            context.fill(10, 10, 10 + width, 28, 0x90101015);
            context.fill(10, 10, 13, 28, 0xFF00E5FF);
            context.drawString(mc.font, badge, 17, 15, 0xFFFFFFFF);

            if (mc.player != null) {
                String coords = String.format("XYZ: %d, %d, %d",
                    (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ());
                int cWidth = mc.font.width(coords) + 12;
                context.fill(10, 31, 10 + cWidth, 47, 0x90101015);
                context.fill(10, 31, 13, 47, 0xFF00E5FF);
                context.drawString(mc.font, coords, 17, 35, 0xFFCCCCCC);
            }
        }

        if (showKeystrokes) {
            drawKeyBox(context, mc, "W", 32, 55, 20, 20, mc.options.keyUp.isDown());
            drawKeyBox(context, mc, "A", 10, 77, 20, 20, mc.options.keyLeft.isDown());
            drawKeyBox(context, mc, "S", 32, 77, 20, 20, mc.options.keyDown.isDown());
            drawKeyBox(context, mc, "D", 54, 77, 20, 20, mc.options.keyRight.isDown());
            drawKeyBox(context, mc, "LMB", 10, 99, 31, 18, mc.options.keyAttack.isDown());
            drawKeyBox(context, mc, "RMB", 43, 99, 31, 18, mc.options.keyUse.isDown());
        }
    }

    private static void drawKeyBox(GuiGraphics context, Minecraft mc, String label, int x, int y, int w, int h, boolean pressed) {
        int bgColor = pressed ? 0xB000E5FF : 0x85101015;
        int txtColor = pressed ? 0xFF000000 : 0xFFFFFFFF;
        context.fill(x, y, x + w, y + h, bgColor);
        int textW = mc.font.width(label);
        context.drawString(mc.font, label, x + (w - textW) / 2, y + (h - 8) / 2, txtColor);
    }

    public static class PrimeDawnMenu extends Screen {
        private final Screen parent;

        protected PrimeDawnMenu(Screen parent) {
            super(Component.literal("Prime Client Dashboard"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int cx = this.width / 2 - 75;
            int cy = this.height / 2 - 55;

            this.addRenderableWidget(Button.builder(getBtnText("HUD & Coords", showHud), b -> {
                showHud = !showHud;
                b.setMessage(getBtnText("HUD & Coords", showHud));
            }).bounds(cx, cy, 150, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("Keystrokes", showKeystrokes), b -> {
                showKeystrokes = !showKeystrokes;
                b.setMessage(getBtnText("Keystrokes", showKeystrokes));
            }).bounds(cx, cy + 24, 150, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("Auto-Sprint", autoSprint), b -> {
                autoSprint = !autoSprint;
                b.setMessage(getBtnText("Auto-Sprint", autoSprint));
            }).bounds(cx, cy + 48, 150, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("FullBright", fullBright), b -> {
                fullBright = !fullBright;
                b.setMessage(getBtnText("FullBright", fullBright));
            }).bounds(cx, cy + 72, 150, 20).build());

            this.addRenderableWidget(Button.builder(getBtnText("FPS Booster", fpsBoost), b -> {
                fpsBoost = !fpsBoost;
                b.setMessage(getBtnText("FPS Booster", fpsBoost));
            }).bounds(cx, cy + 96, 150, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> {
                Minecraft.getInstance().setScreen(this.parent);
            }).bounds(cx, cy + 124, 150, 20).build());
        }

        private Component getBtnText(String name, boolean state) {
            return Component.literal(name + ": " + (state ? "ON" : "OFF"));
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredString(this.font, "PRIME CLIENT MODS", this.width / 2, this.height / 2 - 75, 0xFF00E5FF);
        }
    }
										   }
