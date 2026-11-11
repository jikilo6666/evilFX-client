package dev.evilfx;

import dev.evilfx.config.ModConfig;
import dev.evilfx.feature.Clock;
import dev.evilfx.feature.CustomHitboxes;
import dev.evilfx.feature.CrystalTracker;
import dev.evilfx.feature.HitboxController;
import dev.evilfx.feature.PerformanceTweaks;
import dev.evilfx.feature.TimingTracker;
import dev.evilfx.feature.TotemTracker;
import dev.evilfx.feature.ZoomHandler;
import dev.evilfx.gui.EvilScreen;
import dev.evilfx.hud.ArmorHud;
import dev.evilfx.hud.CrystalHud;
import dev.evilfx.hud.KeystrokesHud;
import dev.evilfx.hud.StatsHud;
import dev.evilfx.hud.TimingHud;
import dev.evilfx.hud.TotemHud;
import dev.evilfx.hud.WarningHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entrypoint. Registers keybinds, HUD elements, passive event listeners and the tick loop.
 * Everything here only READS game state or draws on screen: nothing sends packets or injects input.
 */
public class EvilFXClient implements ClientModInitializer {
    public static final String MOD_ID = "evilfx";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openMenuKey;
    private static KeyBinding hitboxKey;
    private static KeyBinding resetCrystalKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openMenuKey = register("open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        KeyBinding zoomKey = register("zoom", GLFW.GLFW_KEY_C, category);
        hitboxKey = register("toggle_hitboxes", GLFW.GLFW_KEY_UNKNOWN, category);
        resetCrystalKey = register("reset_crystal_stats", GLFW.GLFW_KEY_UNKNOWN, category);
        ZoomHandler.setKey(zoomKey);

        // HUD elements go through Fabric's HudElementRegistry (the successor of HudRenderCallback since 1.21.6).
        // Every element null-checks player/world itself, so joining, respawning and dimension changes are safe.
        Identifier after = VanillaHudElements.MISC_OVERLAYS;
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "stats"), new StatsHud());
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "keystrokes"), new KeystrokesHud());
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "armor"), new ArmorHud());
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "crystal"), new CrystalHud());
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "totems"), new TotemHud());
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "timing"), new TimingHud());
        HudElementRegistry.attachElementAfter(after, Identifier.of(MOD_ID, "warnings"), new WarningHud());

        CustomHitboxes.register();

        ClientTickEvents.END_CLIENT_TICK.register(EvilFXClient::onTick);
        ClientLifecycleEvents.CLIENT_STARTED.register(PerformanceTweaks::apply);

        // Reset per-session stats on join/leave so nothing stale survives a respawn into another server.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetSession());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetSession());

        // Passive observers: they only count what YOU do, they never change it.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) {
                CrystalTracker.onAttack(entity);
                TimingTracker.onAttack();
            }
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) {
                CrystalTracker.onUse(player.getStackInHand(hand));
            }
            return ActionResult.PASS;
        });

        // "evilFX client" button in the Escape menu, bottom-left corner.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(
                        ButtonWidget.builder(Text.literal("evilFX client"), b -> client.setScreen(new EvilScreen(screen)))
                                .dimensions(6, height - 26, 90, 20)
                                .build());
            }
        });
    }

    private static KeyBinding register(String name, int key, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key." + MOD_ID + "." + name, InputUtil.Type.KEYSYM, key, category));
    }

    private static void resetSession() {
        Clock.reset();
        CrystalTracker.resetSession();
        TotemTracker.reset();
        TimingTracker.reset();
    }

    private static void onTick(MinecraftClient mc) {
        while (openMenuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new EvilScreen(null));
        }
        while (hitboxKey.wasPressed()) {
            ModConfig cfg = ModConfig.get();
            cfg.hitboxCustom = !cfg.hitboxCustom;
            cfg.save();
            if (mc.player != null) mc.player.sendMessage(Text.literal("Hitboxes: " + (cfg.hitboxCustom ? "ON" : "OFF")), true);
        }
        while (resetCrystalKey.wasPressed()) CrystalTracker.resetStats();

        ZoomHandler.tick(mc);
        HitboxController.tick(mc);

        // Trackers only run with a live world + player (null-safe on join, respawn and disconnect).
        if (mc.world != null && mc.player != null) {
            Clock.ticks++;
            CrystalTracker.tick(mc);
            TotemTracker.tick(mc);
            TimingTracker.tick(mc);
        }
    }
}
