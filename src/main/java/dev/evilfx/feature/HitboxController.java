package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

/**
 * Hitbox viewer. Uses Minecraft's own hitbox renderer (the one behind F3+B), so it is purely a
 * client-side visual aid with zero gameplay effect. We only flip it when the config says so, and
 * only turn it off again if we were the ones who turned it on.
 */
public final class HitboxController {
    private static boolean forcedOn;

    private HitboxController() {}

    public static void tick(MinecraftClient mc) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        boolean want = ModConfig.get().hitboxes && !ModConfig.get().hitboxCustom;
        if (want && !dispatcher.shouldRenderHitboxes()) {
            dispatcher.setRenderHitboxes(true);
            forcedOn = true;
        } else if (!want && forcedOn) {
            dispatcher.setRenderHitboxes(false);
            forcedOn = false;
        }
    }
}
