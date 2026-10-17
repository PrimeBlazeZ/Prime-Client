package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;

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

            if (autoSprint && client.options.forwardKey.isPressed()) {
                client.player.setSprinting(true);
            }

            if (fullBright) {
                client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false));
            } else {
                client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }

            if (fpsBoost) {
                client.options.getEntityShadows().setValue(false);
            }

            boolean attackPressed = client.options.attackKey.isPressed();
            if (attackPressed && !wasAttackPressed) {
                leftClicks.add(System.currentTimeMillis());
            }
            wasAttackPressed = attackPressed;
            long now = System.currentTimeMillis();
            leftClicks.removeIf(time -> now - time > 1000);
        });

        // 2. Touch Button on Main Menu & Pause Menu
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(
                    ButtonWidget.builder(Text.literal("Prime Client"), btn -> {
                        client.setScreen(new PrimeDawnMenu(screen));
                    }).dimensions(10, 10, 95, 20).build()
                );
            }
        });

        // 3. Render HUD & Keystrokes
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            renderPrimeHud(context, MinecraftClient.getInstance());
        });
    }

    public static void renderPrimeHud(DrawContext context, MinecraftClient mc) {
        if (mc.options.hudHidden) return;

        if (showHud) {
            String badge = "PRIME CLIENT | " + mc.getCurrentFps() + " FPS | " + leftClicks.size() + " CPS";
            int width = mc.textRenderer.getWidth(badge) + 12;
            context.fill(10, 10, 10 + width, 28, 0x90101015);
            context.fill(10, 10, 13, 28, 0xFF00E5FF);
            context.drawTextWithShadow(mc.textRenderer, badge, 17, 15, 0xFFFFFF);

            if (mc.player != null) {
                String coords = String.format("XYZ: %d, %d, %d",
                    (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ());
                int cWidth = mc.textRenderer.getWidth(coords) + 12;
                context.fill(10, 31, 10 + cWidth, 47, 0x90101015);
                context.fill(10, 31, 13, 47, 0xFF00E5FF);
                context.drawTextWithShadow(mc.textRenderer, coords, 17, 35, 0xCCCCCC);
            }
        }

        if (showKeystrokes) {
            drawKeyBox(context, mc, "W", 32, 55, 20, 20, mc.options.forwardKey.isPressed());
            drawKeyBox(context, mc, "A", 10, 77, 20, 20, mc.options.leftKey.isPressed());
            drawKeyBox(context, mc, "S", 32, 77, 20, 20, mc.options.backKey.isPressed());
            drawKeyBox(context, mc, "D", 54, 77, 20, 20, mc.options.rightKey.isPressed());
            drawKeyBox(context, mc, "LMB", 10, 99, 31, 18, mc.options.attackKey.isPressed());
            drawKeyBox(context, mc, "RMB", 43, 99, 31, 18, mc.options.useKey.isPressed());
        }
    }

    private static void drawKeyBox(DrawContext context, MinecraftClient mc, String label, int x, int y, int w, int h, boolean pressed) {
        int bgColor = pressed ? 0xB000E5FF : 0x85101015;
        int txtColor = pressed ? 0xFF000000 : 0xFFFFFFFF;
        context.fill(x, y, x + w, y + h, bgColor);
        int textW = mc.textRenderer.getWidth(label);
        context.drawTextWithShadow(mc.textRenderer, label, x + (w - textW) / 2, y + (h - 8) / 2, txtColor);
    }

    public static class PrimeDawnMenu extends Screen {
        private final Screen parent;

        protected PrimeDawnMenu(Screen parent) {
            super(Text.literal("Prime Client Dashboard"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int cx = this.width / 2 - 75;
            int cy = this.height / 2 - 55;

            this.addDrawableChild(ButtonWidget.builder(getBtnText("HUD & Coords", showHud), b -> {
                showHud = !showHud;
                b.setMessage(getBtnText("HUD & Coords", showHud));
            }).dimensions(cx, cy, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("Keystrokes", showKeystrokes), b -> {
                showKeystrokes = !showKeystrokes;
                b.setMessage(getBtnText("Keystrokes", showKeystrokes));
            }).dimensions(cx, cy + 24, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("Auto-Sprint", autoSprint), b -> {
                autoSprint = !autoSprint;
                b.setMessage(getBtnText("Auto-Sprint", autoSprint));
            }).dimensions(cx, cy + 48, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("FullBright", fullBright), b -> {
                fullBright = !fullBright;
                b.setMessage(getBtnText("FullBright", fullBright));
            }).dimensions(cx, cy + 72, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("FPS Booster", fpsBoost), b -> {
                fpsBoost = !fpsBoost;
                b.setMessage(getBtnText("FPS Booster", fpsBoost));
            }).dimensions(cx, cy + 96, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), b -> {
                this.client.setScreen(this.parent);
            }).dimensions(cx, cy + 124, 150, 20).build());
        }

        private Text getBtnText(String name, boolean state) {
            return Text.literal(name + ": " + (state ? "ON" : "OFF"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, "PRIME CLIENT MODS", this.width / 2, this.height / 2 - 75, 0x00E5FF);
        }
    }
													   }
