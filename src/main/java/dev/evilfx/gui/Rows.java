package dev.evilfx.gui;

import dev.evilfx.Branding;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Menu widgets. They are "immediate" rows: EvilScreen places them every frame, renders them, and forwards
 * press/drag/release events that it derives from polling the left mouse button (so we do not depend on the
 * Screen mouse-callback signatures, which changed in 1.21.9).
 */
public final class Rows {
    private Rows() {}

    static final int TEXT = 0xFFE6E6F0;
    static final int MUTED = 0xFF9494AA;
    static final int TRACK = 0xFF2A2A3A;
    static final int WHITE = 0xFFFFFFFF;

    public abstract static class Row {
        protected int x, y, w;
        protected float hover;

        public void place(int x, int y, int w) {
            this.x = x;
            this.y = y;
            this.w = w;
        }

        public int height() {
            return 23;
        }

        public abstract void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha);

        /** Returns true when the row wants to receive drag events (sliders). */
        public boolean press(int mx, int my) {
            return false;
        }

        public void drag(int mx) {}

        public void release() {}

        protected boolean hit(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + 20;
        }

        protected void animateHover(boolean over, float dt) {
            hover += ((over ? 1f : 0f) - hover) * (1f - (float) Math.exp(-dt * 14f));
        }

        protected void background(DrawContext ctx, float alpha) {
            Gfx.roundRect(ctx, x, y, w, 20, 4, Gfx.a(Gfx.mix(0x20FFFFFF, 0x3CFFFFFF, hover), alpha));
        }

        protected void label(DrawContext ctx, TextRenderer tr, String s, float alpha) {
            ctx.drawText(tr, s, x + 8, y + 6, Gfx.a(TEXT, alpha), false);
        }
    }

    /** Small accent-colored section title. */
    public static final class Header extends Row {
        private final String title;

        public Header(String title) {
            this.title = title;
        }

        @Override
        public int height() {
            return 18;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            ctx.drawText(tr, title, x + 4, y + 7, Gfx.a(Branding.ACCENT, alpha), false);
        }
    }

    /** Plain muted text line. */
    public static final class Note extends Row {
        private final String text;

        public Note(String text) {
            this.text = text;
        }

        @Override
        public int height() {
            return 16;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            ctx.drawText(tr, text, x + 4, y + 4, Gfx.a(MUTED, alpha), false);
        }
    }

    /** Animated on/off switch. */
    public static final class Toggle extends Row {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float t = -1f;

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            boolean v = get.getAsBoolean();
            if (t < 0) t = v ? 1f : 0f;
            t += ((v ? 1f : 0f) - t) * (1f - (float) Math.exp(-dt * 16f));
            animateHover(hit(mx, my), dt);
            background(ctx, alpha);
            label(ctx, tr, label, alpha);

            int tx = x + w - 8 - 26;
            Gfx.roundRect(ctx, tx, y + 4, 26, 12, 6, Gfx.a(Gfx.mix(0xFF3A3A4C, Branding.ACCENT, t), alpha));
            Gfx.roundRect(ctx, tx + 2 + Math.round(t * 14f), y + 6, 8, 8, 4, Gfx.a(WHITE, alpha));
        }

        @Override
        public boolean press(int mx, int my) {
            if (hit(mx, my)) set.accept(!get.getAsBoolean());
            return false;
        }
    }

    /** Horizontal slider with a value readout. */
    public static final class Slider extends Row {
        private final String label;
        private final float min, max, step;
        private final String format;
        private final Supplier<Float> get;
        private final Consumer<Float> set;

        public Slider(String label, float min, float max, float step, String format,
                      Supplier<Float> get, Consumer<Float> set) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.get = get;
            this.set = set;
        }

        private int bx() {
            return x + (int) (w * 0.40f);
        }

        private int bw() {
            return Math.max(20, (int) (w * 0.60f) - 58);
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            animateHover(hit(mx, my), dt);
            background(ctx, alpha);
            label(ctx, tr, label, alpha);

            float v = get.get();
            float t = MathHelper.clamp((v - min) / (max - min), 0f, 1f);
            int fill = Math.round(bw() * t);
            Gfx.roundRect(ctx, bx(), y + 8, bw(), 4, 2, Gfx.a(TRACK, alpha));
            if (fill > 0) Gfx.roundRect(ctx, bx(), y + 8, Math.max(fill, 2), 4, 2, Gfx.a(Branding.ACCENT, alpha));
            Gfx.roundRect(ctx, bx() + fill - 3, y + 5, 6, 10, 3, Gfx.a(WHITE, alpha));

            String s = String.format(Locale.ROOT, format, v);
            ctx.drawText(tr, s, x + w - 8 - tr.getWidth(s), y + 6, Gfx.a(MUTED, alpha), false);
        }

        @Override
        public boolean press(int mx, int my) {
            if (!hit(mx, my) || mx < bx() - 6 || mx > bx() + bw() + 6) return false;
            apply(mx);
            return true;
        }

        @Override
        public void drag(int mx) {
            apply(mx);
        }

        private void apply(int mx) {
            float t = MathHelper.clamp((mx - bx()) / (float) bw(), 0f, 1f);
            float v = min + t * (max - min);
            if (step > 0) v = Math.round(v / step) * step;
            set.accept(MathHelper.clamp(v, min, max));
        }
    }

    /** Click to cycle through the values of an enum. */
    public static final class Cycle<E extends Enum<E>> extends Row {
        private final String label;
        private final E[] values;
        private final Supplier<E> get;
        private final Consumer<E> set;

        public Cycle(String label, E[] values, Supplier<E> get, Consumer<E> set) {
            this.label = label;
            this.values = values;
            this.get = get;
            this.set = set;
        }

        private static String pretty(Enum<?> e) {
            String s = e.name().replace('_', ' ').toLowerCase(Locale.ROOT);
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            animateHover(hit(mx, my), dt);
            background(ctx, alpha);
            label(ctx, tr, label, alpha);
            String s = pretty(get.get());
            int pw = tr.getWidth(s) + 14;
            int px = x + w - 8 - pw;
            Gfx.roundRect(ctx, px, y + 3, pw, 14, 6, Gfx.a(Gfx.mix(0xFF2E2E40, 0xFF40405A, hover), alpha));
            ctx.drawText(tr, s, px + 7, y + 6, Gfx.a(TEXT, alpha), false);
        }

        @Override
        public boolean press(int mx, int my) {
            if (hit(mx, my)) set.accept(values[(get.get().ordinal() + 1) % values.length]);
            return false;
        }
    }

    /** Three compact R/G/B sliders plus a live swatch. Value is a 24-bit RGB int. */
    public static final class ColorRow extends Row {
        private static final int[] CHANNEL_COLORS = {0xFFFF5555, 0xFF55FF55, 0xFF5577FF};
        private final String label;
        private final IntSupplier get;
        private final IntConsumer set;
        private int channel = -1;

        public ColorRow(String label, IntSupplier get, IntConsumer set) {
            this.label = label;
            this.get = get;
            this.set = set;
        }

        private int sx() {
            return x + (int) (w * 0.36f);
        }

        private int segW() {
            return Math.max(12, (x + w - 8 - 12 - 8 - sx() - 8) / 3);
        }

        private int segX(int i) {
            return sx() + i * (segW() + 4);
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            animateHover(hit(mx, my), dt);
            background(ctx, alpha);
            label(ctx, tr, label, alpha);

            int rgb = get.getAsInt() & 0xFFFFFF;
            for (int i = 0; i < 3; i++) {
                int v = (rgb >> (16 - 8 * i)) & 0xFF;
                int fill = Math.round(segW() * v / 255f);
                Gfx.roundRect(ctx, segX(i), y + 8, segW(), 4, 2, Gfx.a(TRACK, alpha));
                if (fill > 0) Gfx.roundRect(ctx, segX(i), y + 8, Math.max(fill, 2), 4, 2, Gfx.a(CHANNEL_COLORS[i], alpha));
                Gfx.roundRect(ctx, segX(i) + fill - 2, y + 5, 4, 10, 2, Gfx.a(WHITE, alpha));
            }
            int sw = x + w - 8 - 12;
            Gfx.roundRect(ctx, sw, y + 4, 12, 12, 3, Gfx.a(0xFF000000 | rgb, alpha));
            Gfx.roundBorder(ctx, sw, y + 4, 12, 12, 3, Gfx.a(0x66FFFFFF, alpha));
        }

        @Override
        public boolean press(int mx, int my) {
            if (!hit(mx, my)) return false;
            for (int i = 0; i < 3; i++) {
                if (mx >= segX(i) - 3 && mx <= segX(i) + segW() + 3) {
                    channel = i;
                    apply(mx);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void drag(int mx) {
            if (channel >= 0) apply(mx);
        }

        @Override
        public void release() {
            channel = -1;
        }

        private void apply(int mx) {
            int v = Math.round(MathHelper.clamp((mx - segX(channel)) / (float) segW(), 0f, 1f) * 255f);
            int shift = 16 - 8 * channel;
            int rgb = (get.getAsInt() & 0xFFFFFF & ~(0xFF << shift)) | (v << shift);
            set.accept(rgb);
        }
    }

    /** Full-width action button. */
    public static final class Button extends Row {
        private final String label;
        private final Runnable action;

        public Button(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int mx, int my, float dt, float alpha) {
            animateHover(hit(mx, my), dt);
            Gfx.roundRect(ctx, x, y, w, 20, 4, Gfx.a(Gfx.mix(0x30C8102E, 0x66C8102E, hover), alpha));
            Gfx.roundBorder(ctx, x, y, w, 20, 4, Gfx.a(Branding.ACCENT, alpha * 0.7f));
            ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + 6, Gfx.a(TEXT, alpha), false);
        }

        @Override
        public boolean press(int mx, int my) {
            if (hit(mx, my)) action.run();
            return false;
        }
    }
}
