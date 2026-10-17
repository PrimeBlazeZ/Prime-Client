package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ExampleModClient implements ClientModInitializer {
    public static boolean showHud = true;
    public static boolean showKeystrokes = true;
    public static boolean autoSprint = true;
    public static boolean fullBright = true;
    public static boolean fpsBoost = true;

    private static KeyBinding openMenuKey;
    private static final List<Long> leftClicks = new ArrayList<>();
    private static boolean wasAttackPressed = false;

    @Override
    public void onInitializeClient() {
        // Right Shift Keybind for Opening Dawn Client Menu
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Prime Client Menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "Prime Client"
        ));

        // Ticks: Auto-Sprint, FullBright, FPS Booster & CPS
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (autoSprint && client.options.forwardKey.isPressed()) {
                client.player.setSprinting(true);
            }

            if (fullBright) {
                client.options.getGamma().setValue(16.0);
            }

            if (fpsBoost) {
                client.options.getEntityShadows().setValue(false);
            }

            // Open Menu on Right Shift
            while (openMenuKey.wasPressed()) {
                client.setScreen(new PrimeDawnMenu(null));
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

        // Add Button to Main Menu & Pause Menu
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(
                    ButtonWidget.builder(Text.literal("Prime Client"), btn -> {
                        client.setScreen(new PrimeDawnMenu(screen));
                    }).dimensions(10, 10, 95, 20).build()
                );
            }
        });

        // Render In-Game HUD, FPS, CPS & Keystrokes
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || !showHud) return;

            // Watermark & FPS Box
            String badge = "PRIME CLIENT  |  " + mc.getCurrentFps() + " FPS  |  " + leftClicks.size() + " CPS";
            int width = mc.textRenderer.getWidth(badge) + 16;
            context.fill(10, 10, 10 + width, 28, 0xC0101015);
            context.fill(10, 10, 13, 28, 0xFF00E5FF); // Cyan Accent Line
            context.drawText(mc.textRenderer, badge, 18, 15, 0xFFFFFFFF, true);

            // Coordinates Box
            if (mc.player != null) {
                String coords = String.format("XYZ: %d, %d, %d", (int)mc.player.getX(), (int)mc.player.getY(), (int)mc.player.getZ());
                int cWidth = mc.textRenderer.getWidth(coords) + 16;
                context.fill(10, 31, 10 + cWidth, 47, 0xC0101015);
                context.fill(10, 31, 13, 47, 0xFF00E5FF);
                context.drawText(mc.textRenderer, coords, 18, 36, 0xFFCCCCCC, true);
            }

            // Keystrokes UI (W, A, S, D, LMB, RMB)
            if (showKeystrokes) {
                int kx = 10;
                int ky = 53;
                drawKey(context, mc, "W", kx + 22, ky, 20, 20, mc.options.forwardKey.isPressed());
                drawKey(context, mc, "A", kx, ky + 22, 20, 20, mc.options.leftKey.isPressed());
                drawKey(context, mc, "S", kx + 22, ky + 22, 20, 20, mc.options.backKey.isPressed());
                drawKey(context, mc, "D", kx + 44, ky + 22, 20, 20, mc.options.rightKey.isPressed());
                drawKey(context, mc, "LMB", kx, ky + 44, 31, 18, mc.options.attackKey.isPressed());
                drawKey(context, mc, "RMB", kx + 33, ky + 44, 31, 18, mc.options.useKey.isPressed());
            }
        });
    }

    private static void drawKey(DrawContext context, MinecraftClient mc, String label, int x, int y, int w, int h, boolean pressed) {
        int bg = pressed ? 0xB000E5FF : 0xC0101015;
        int fg = pressed ? 0xFF000000 : 0xFFFFFFFF;
        context.fill(x, y, x + w, y + h, bg);
        int tw = mc.textRenderer.getWidth(label);
        context.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, true);
    }

    // Dawn Client Style Settings Menu (Opened via Right Shift or Menu Button)
    public static class PrimeDawnMenu extends Screen {
        private final Screen parent;

        protected PrimeDawnMenu(Screen parent) {
            super(Text.literal("Prime Client Dashboard"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int cx = this.width / 2 - 80;
            int cy = this.height / 2 - 60;

            this.addDrawableChild(ButtonWidget.builder(getBtnText("HUD Overlay", showHud), b -> {
                showHud = !showHud;
                b.setMessage(getBtnText("HUD Overlay", showHud));
            }).dimensions(cx, cy, 160, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("Keystrokes", showKeystrokes), b -> {
                showKeystrokes = !showKeystrokes;
                b.setMessage(getBtnText("Keystrokes", showKeystrokes));
            }).dimensions(cx, cy + 24, 160, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("Auto-Sprint", autoSprint), b -> {
                autoSprint = !autoSprint;
                b.setMessage(getBtnText("Auto-Sprint", autoSprint));
            }).dimensions(cx, cy + 48, 160, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getGameModeText("FullBright", fullBright), b -> {
                fullBright = !fullBright;
                b.setMessage(getGameModeText("FullBright", fullBright));
            }).dimensions(cx, cy + 72, 160, 20).build());

            this.addDrawableChild(ButtonWidget.builder(getBtnText("FPS Booster", fpsBoost), b -> {
                fpsBoost = !fpsBoost;
                b.setMessage(getBtnText("FPS Booster", fpsBoost));
            }).dimensions(cx, cy + 96, 160, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), b -> {
                this.client.setScreen(this.parent);
            }).dimensions(cx, cy + 124, 160, 20).build());
        }

        private Text getBtnText(String name, boolean state) {
            return Text.literal(name + ": " + (state ? "§aON" : "§cOFF"));
        }

        private Text getGameModeText(String name, boolean state) {
            return Text.literal(name + ": " + (state ? "§aON" : "§cOFF"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(0, 0, this.width, this.height, 0xB0000000); // Dark blur background
            int bw = 200, bh = 165;
            int bx = this.width / 2 - bw / 2;
            int by = this.height / 2 - bh / 2;
            context.fill(bx, by, bx + bw, by + bh, 0xF014141B); // Dawn Panel Box
            context.fill(bx, by, bx + bw, by + 3, 0xFF00E5FF);   // Cyan Header Line

            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, "PRIME CLIENT DASHBOARD", this.width / 2, by + 12, 0x00E5FF);
        }
    }
}
