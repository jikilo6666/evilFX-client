package dev.evilfx.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Tiny 2D drawing toolkit for the menu. Vanilla has no rounded rectangles, so corners are built from
 * one-pixel rows whose inset follows a circle. All colors are ARGB, so translucency gives the "glass" look.
 */
public final class Gfx {
    private Gfx() {}

    /** Multiplies the alpha channel of an ARGB color by `alpha` (0..1). */
    public static int a(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * MathHelper.clamp(alpha, 0f, 1f));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Linear blend of two ARGB colors (alpha included). */
    public static int mix(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int a = (int) (((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    public static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            c.fill(x + in, y + i, x + w - in, y + i + 1, color);
            c.fill(x + in, y + h - i - 1, x + w - in, y + h - i, color);
        }
    }

    /** 1px rounded outline. Pixels never overlap, so translucent borders keep a uniform alpha. */
    public static void roundBorder(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        c.fill(x + r, y, x + w - r, y + 1, color);
        c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            int to = i == 0 ? r : Math.max(in + 1, inset(r, i - 1));
            c.fill(x + in, y + i, x + to, y + i + 1, color);
            c.fill(x + w - to, y + i, x + w - in, y + i + 1, color);
            c.fill(x + in, y + h - i - 1, x + to, y + h - i, color);
            c.fill(x + w - to, y + h - i - 1, x + w - in, y + h - i, color);
        }
    }
}
