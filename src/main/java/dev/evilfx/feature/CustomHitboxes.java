package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * evilFX hitbox viewer: draws wireframe boxes around entities with your own RGB + opacity per category
 * (players, mobs, others) and a separate color for the entity under your crosshair.
 *
 * It runs after entities are drawn and writes plain line geometry into the vanilla LINES layer. It only reads
 * entity bounding boxes the client already knows; it changes nothing about the game. Boxes are interpolated
 * between ticks so they stay glued to moving targets at any frame rate.
 *
 * NOTE: 1.21.11 reworked world rendering (extract/draw split). If this file fails to compile or throws, delete it
 * together with the CustomHitboxes.register() line in EvilFXClient; everything else keeps working.
 */
public final class CustomHitboxes {
    private CustomHitboxes() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(CustomHitboxes::render);
    }

    private static void render(WorldRenderContext context) {
        ModConfig c = ModConfig.get();
        if (!c.hitboxCustom) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cam = context.worldState().cameraRenderState.pos;
        float tick = mc.getRenderTickCounter().getTickProgress(false);
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        double maxSq = (double) c.hitDistance * c.hitDistance;
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        Entity target = mc.targetedEntity;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player && firstPerson) continue;
            if (e.squaredDistanceTo(mc.player) > maxSq) continue;

            boolean isTarget = c.hitTargetEnabled && e == target;
            int rgb;
            int opacity;
            if (isTarget) {
                rgb = c.hitTargetColor;
                opacity = c.hitTargetOpacity;
            } else if (e instanceof PlayerEntity) {
                if (!c.hitPlayers) continue;
                rgb = c.hitPlayerColor;
                opacity = c.hitPlayerOpacity;
            } else if (e instanceof LivingEntity) {
                if (!c.hitMobs) continue;
                rgb = c.hitMobColor;
                opacity = c.hitMobOpacity;
            } else {
                if (!c.hitOthers) continue;
                rgb = c.hitOtherColor;
                opacity = c.hitOtherOpacity;
            }
            if (opacity <= 0) continue;

            Vec3d lerped = e.getLerpedPos(tick);
            Box box = e.getBoundingBox()
                    .offset(lerped.x - e.getX(), lerped.y - e.getY(), lerped.z - e.getZ())
                    .offset(-cam.x, -cam.y, -cam.z);
            VertexRendering.drawBox(matrices, lines, box,
                    ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f,
                    opacity / 100f);
        }
    }
}
