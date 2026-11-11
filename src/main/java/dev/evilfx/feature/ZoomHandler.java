package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.MathHelper;

/**
 * Hold-to-zoom with smooth easing. tick() eases `current` toward the target once per game tick;
 * GameRendererMixin asks for multiplier(partialTick), which interpolates between the last two
 * ticks so the motion stays smooth at any frame rate.
 */
public final class ZoomHandler {
    private static KeyBinding key;
    private static float prev = 1f;
    private static float current = 1f;

    private ZoomHandler() {}

    public static void setKey(KeyBinding k) {
        key = k;
    }

    public static void tick(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        boolean zooming = cfg.zoomEnabled && key.isPressed() && mc.currentScreen == null;
        float target = zooming ? 1f / cfg.zoomMagnification : 1f;

        prev = current;
        current += (target - current) * cfg.zoomSpeed;
        if (Math.abs(target - current) < 0.001f) current = target;
    }

    /** True while any zoom is applied (including the ease-back after releasing the key). */
    public static boolean isActive() {
        return current < 0.999f || prev < 0.999f;
    }

    /** Field-of-view tangent multiplier, 1.0 = no zoom, 0.25 = 4x magnification. */
    public static float multiplier(float partialTick) {
        return MathHelper.lerp(partialTick, prev, current);
    }
}
