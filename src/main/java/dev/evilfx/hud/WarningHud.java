package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.feature.CrystalTracker;
import dev.evilfx.feature.TotemTracker;
import dev.evilfx.gui.Gfx;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;

/**
 * Full-screen alerts: a pulsing red glow on the top/bottom screen edges with a message when obsidian or
 * crystals run low in the hotbar, and a fading "TOTEM POPPED" banner when your own totem pops.
 * Not tied to a movable box, so it implements HudElement directly.
 */
public final class WarningHud implements HudElement {
    private static final long POP_BANNER_MS = 1600;

    @Override
    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

        ModConfig c = ModConfig.get();
        long now = Util.getMeasuringTimeMs();
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        boolean lowObs = CrystalTracker.lowObsidian;
        boolean lowCry = CrystalTracker.lowCrystals;
        if (c.warnEnabled && (lowObs || lowCry)) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(now / 160.0);
            int base = 0xFF000000 | c.warnColor;
            int strong = Gfx.a(base, 0.55f * pulse);
            int clear = base & 0x00FFFFFF;
            ctx.fillGradient(0, 0, sw, 34, strong, clear);
            ctx.fillGradient(0, sh - 34, sw, sh, clear, strong);

            String msg = lowObs && lowCry ? "LOW OBSIDIAN + CRYSTALS"
                    : lowObs ? "LOW OBSIDIAN (" + CrystalTracker.obsidianInHotbar + ")"
                    : "LOW CRYSTALS (" + CrystalTracker.crystalsInHotbar + ")";
            int tw = mc.textRenderer.getWidth(msg);
            ctx.drawText(mc.textRenderer, msg, (sw - tw) / 2, 38, Gfx.a(base, 0.7f + 0.3f * pulse), true);
        }

        if (c.popAlertEnabled && TotemTracker.selfPopMs > 0) {
            long age = now - TotemTracker.selfPopMs;
            if (age >= 0 && age < POP_BANNER_MS) {
                float a = 1f - age / (float) POP_BANNER_MS;
                if (a > 0.05f) {
                    String msg = "TOTEM POPPED  |  " + TotemTracker.totems + " left";
                    Matrix3x2fStack m = ctx.getMatrices();
                    m.pushMatrix();
                    m.translate(sw / 2f, sh / 3f);
                    m.scale(2f, 2f);
                    int tw = mc.textRenderer.getWidth(msg);
                    ctx.drawText(mc.textRenderer, msg, -tw / 2, 0, Gfx.a(0xFFFFAA33, a), true);
                    m.popMatrix();
                }
            }
        }
    }
}
