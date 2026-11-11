package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import dev.evilfx.feature.ClickTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;

/**
 * WASD + mouse buttons + space bar. Key state comes from the player's own key bindings, so it
 * follows any rebinding. CPS is a passive counter (see ClickTracker): it only observes your clicks.
 */
public final class KeystrokesHud extends AbstractHud {
    private static final int KEY = 20;
    private static final int GAP = 2;
    private static final int WIDTH = KEY * 3 + GAP * 2;
    private static final int MOUSE_W = (WIDTH - GAP) / 2;

    @Override
    protected HudSettings settings() {
        return ModConfig.get().keystrokes;
    }

    @Override
    protected boolean prepare() {
        ClickTracker.poll(mc); // sample mouse buttons once per rendered frame
        w = WIDTH;
        h = KEY * 3 + GAP * 3 + 10;
        return true;
    }

    @Override
    protected void draw(DrawContext ctx, HudSettings s) {
        GameOptions o = mc.options;
        boolean cps = ModConfig.get().showCps;

        key(ctx, s, KEY + GAP, 0, KEY, KEY, "W", o.forwardKey.isPressed(), null);
        key(ctx, s, 0, KEY + GAP, KEY, KEY, "A", o.leftKey.isPressed(), null);
        key(ctx, s, KEY + GAP, KEY + GAP, KEY, KEY, "S", o.backKey.isPressed(), null);
        key(ctx, s, (KEY + GAP) * 2, KEY + GAP, KEY, KEY, "D", o.rightKey.isPressed(), null);

        int my = (KEY + GAP) * 2;
        key(ctx, s, 0, my, MOUSE_W, KEY, "LMB", ClickTracker.isDown(0), cps ? String.valueOf(ClickTracker.cps(0)) : null);
        key(ctx, s, MOUSE_W + GAP, my, MOUSE_W, KEY, "RMB", ClickTracker.isDown(1), cps ? String.valueOf(ClickTracker.cps(1)) : null);

        // Space bar drawn as a plain bar (no label) to keep the HUD compact.
        int sy = my + KEY + GAP;
        boolean jump = o.jumpKey.isPressed();
        if (jump) ctx.fill(0, sy, WIDTH, sy + 10, 0xA0FFFFFF);
        else if (s.background) ctx.fill(0, sy, WIDTH, sy + 10, 0x90000000);
        ctx.fill(WIDTH / 2 - 10, sy + 4, WIDTH / 2 + 10, sy + 5, jump ? 0xFF000000 : argb(s));
    }

    /** One key box: filled white when pressed (text flips to black), optional small CPS line under the label. */
    private void key(DrawContext ctx, HudSettings s, int x, int y, int bw, int bh, String label, boolean down, String sub) {
        if (down) ctx.fill(x, y, x + bw, y + bh, 0xA0FFFFFF);
        else if (s.background) ctx.fill(x, y, x + bw, y + bh, 0x90000000);

        int color = down ? 0xFF000000 : argb(s);
        boolean shadow = s.shadow && !down;
        int ly = sub == null ? y + (bh - 8) / 2 : y + 2;
        text(ctx, label, x + (bw - mc.textRenderer.getWidth(label)) / 2, ly, color, shadow);
        if (sub != null) {
            text(ctx, sub, x + (bw - mc.textRenderer.getWidth(sub)) / 2, y + 11, color, shadow);
        }
    }
}
