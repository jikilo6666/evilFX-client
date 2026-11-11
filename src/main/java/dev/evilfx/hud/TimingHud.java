package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import dev.evilfx.feature.TimingTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Rhythm bar. Fills once per beat (interval in ticks, counted from your last attack) and flashes white
 * just before the beat lands. The thin bar underneath is the vanilla attack cooldown. Display only.
 */
public final class TimingHud extends AbstractHud {
    @Override
    protected HudSettings settings() {
        return ModConfig.get().timing;
    }

    @Override
    protected boolean prepare() {
        w = 96;
        h = 30;
        return true;
    }

    @Override
    protected void draw(DrawContext ctx, HudSettings s) {
        background(ctx, s);
        boolean active = TimingTracker.active();
        float p = active ? TimingTracker.progress(partial) : 0f;
        int bw = w - PAD * 2;

        String label = active ? "RHYTHM " + ModConfig.get().timingInterval + "t" : "RHYTHM  ready";
        text(ctx, label, PAD, PAD, argb(s), s.shadow);

        ctx.fill(PAD, 15, PAD + bw, 21, 0x80000000);
        ctx.fill(PAD, 15, PAD + (int) (bw * p), 21, p > 0.8f ? 0xFFFFFFFF : argb(s));

        float cd = mc.player == null ? 1f : MathHelper.clamp(mc.player.getAttackCooldownProgress(0.5f), 0f, 1f);
        ctx.fill(PAD, 24, PAD + bw, 27, 0x80000000);
        ctx.fill(PAD, 24, PAD + (int) (bw * cd), 27, cd >= 1f ? 0xFF55FF55 : 0xFFFFAA00);
    }
}
