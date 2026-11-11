package dev.evilfx.hud;

import dev.evilfx.EvilFXClient;
import dev.evilfx.config.ModConfig.HudSettings;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

/**
 * Shared plumbing for HUD modules.
 *
 * Each frame: prepare() measures the content (sets w/h), the 2D matrix stack is translated + scaled so the
 * module lands on its anchor, then draw() paints at (0,0). Since 1.21.6 the GUI matrix stack is a 2D
 * Matrix3x2fStack (pushMatrix/popMatrix).
 *
 * Safety: nothing draws without a player AND a world (join/respawn/disconnect), and a module that throws is
 * switched off for the session (logged once) instead of crashing the game or leaving the matrix unbalanced.
 */
public abstract class AbstractHud implements HudElement {
    protected static final int PAD = 3;
    protected static final int LINE = 10;

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected int w;
    protected int h;
    protected float partial;

    private boolean failed;
    private long lastRefreshNs;

    protected abstract HudSettings settings();

    /** Measure content, fill w/h. Return false to skip drawing this frame. */
    protected abstract boolean prepare();

    protected abstract void draw(DrawContext ctx, HudSettings s);

    @Override
    public final void render(DrawContext ctx, RenderTickCounter tickCounter) {
        HudSettings s = settings();
        if (failed || s == null || !s.enabled) return;
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return; // hudHidden = F1

        partial = tickCounter.getTickProgress(false);
        Matrix3x2fStack m = ctx.getMatrices();
        boolean pushed = false;
        try {
            if (!prepare()) return;
            int x = s.anchor.resolveX(ctx.getScaledWindowWidth(), Math.round(w * s.scale), s.offsetX);
            int y = s.anchor.resolveY(ctx.getScaledWindowHeight(), Math.round(h * s.scale), s.offsetY);
            m.pushMatrix();
            pushed = true;
            m.translate(x, y);
            m.scale(s.scale, s.scale);
            draw(ctx, s);
        } catch (RuntimeException e) {
            failed = true;
            EvilFXClient.LOGGER.error("HUD module {} disabled after an error", getClass().getSimpleName(), e);
        } finally {
            if (pushed) m.popMatrix();
        }
    }

    /** True at most once per interval: lets modules rebuild their text a few times per second instead of every frame. */
    protected final boolean refreshDue(long intervalMs) {
        long n = System.nanoTime();
        if (n - lastRefreshNs >= intervalMs * 1_000_000L) {
            lastRefreshNs = n;
            return true;
        }
        return false;
    }

    /** Since 1.21.6 text colors must include alpha, otherwise they render fully transparent. */
    protected static int argb(HudSettings s) {
        return 0xFF000000 | s.color;
    }

    protected void background(DrawContext ctx, HudSettings s) {
        if (s.background) ctx.fill(0, 0, w, h, 0x90000000);
    }

    protected void text(DrawContext ctx, String str, int x, int y, int argb, boolean shadow) {
        ctx.drawText(mc.textRenderer, str, x, y, argb, shadow);
    }
}
