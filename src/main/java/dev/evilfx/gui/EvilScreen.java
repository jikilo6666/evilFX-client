package dev.evilfx.gui;

import dev.evilfx.Branding;
import dev.evilfx.EvilFXClient;
import dev.evilfx.config.Anchor;
import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import dev.evilfx.feature.CrystalTracker;
import dev.evilfx.feature.PerformanceTweaks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The evilFX menu: dark glass panel, animated sidebar tabs, scrollable module settings.
 *
 * How it works
 *  - Look: translucent rounded panels over vanilla's blurred menu background (so the "glass" is real blur
 *    when Menu Background Blurriness is on), 1px light borders and a soft top highlight.
 *  - Animation: a fade-in on open, an eased highlight that slides between tabs, per-row hover blending and
 *    animated toggle switches. All easing is frame-time based, so it looks the same at any FPS.
 *  - Input: rows are laid out every frame, and mouse presses/drags are derived by polling the left button
 *    inside render(). That keeps the screen independent of the Screen mouse-event signatures, which changed
 *    in 1.21.9. Only mouseScrolled (unchanged) is overridden.
 */
public final class EvilScreen extends Screen {
    private record Tab(String name, List<Rows.Row> rows) {}

    private static final int HEADER = 54;
    private static final int FOOTER = 24;
    private static final int TAB_H = 18;
    private static final int TAB_GAP = 2;
    private static final int SIDE_W = 100;

    private final Screen parent;
    private final List<Tab> tabs = new ArrayList<>();
    private float[] tabHover;
    private int current;
    private float tabY = -1f;
    private float scroll;
    private float scrollTarget;
    private long openMs;
    private long lastNs = System.nanoTime();
    private boolean prevDown;
    private Rows.Row active;
    private float discordHover;
    private String toast = "";
    private long toastMs;

    private int px, py, pw, ph, sx, sy, cx, cy, cw, ch;

    public EvilScreen(Screen parent) {
        super(Text.literal(Branding.NAME));
        this.parent = parent;
        buildTabs();
    }

    // ------------------------------------------------------------------ tabs

    private void buildTabs() {
        ModConfig c = ModConfig.get();

        List<Rows.Row> stats = new ArrayList<>();
        stats.add(new Rows.Header("FPS & PING"));
        stats.add(new Rows.Toggle("Show FPS", () -> c.showFps, v -> c.showFps = v));
        stats.add(new Rows.Toggle("Show frame time (ms)", () -> c.showFrameTime, v -> c.showFrameTime = v));
        stats.add(new Rows.Toggle("Show ping", () -> c.showPing, v -> c.showPing = v));
        hud(stats, c.stats);
        tabs.add(new Tab("Stats", stats));

        List<Rows.Row> keys = new ArrayList<>();
        keys.add(new Rows.Header("KEYSTROKES & CPS"));
        keys.add(new Rows.Toggle("Show CPS on mouse buttons", () -> c.showCps, v -> c.showCps = v));
        hud(keys, c.keystrokes);
        tabs.add(new Tab("Keystrokes", keys));

        List<Rows.Row> armor = new ArrayList<>();
        armor.add(new Rows.Header("ARMOR & ITEMS"));
        armor.add(new Rows.Toggle("Show held items", () -> c.armorShowHeld, v -> c.armorShowHeld = v));
        armor.add(new Rows.Toggle("Durability as percent", () -> c.armorPercent, v -> c.armorPercent = v));
        hud(armor, c.armor);
        tabs.add(new Tab("Armor", armor));

        List<Rows.Row> view = new ArrayList<>();
        view.add(new Rows.Header("ZOOM (key: see Controls, default C)"));
        view.add(new Rows.Toggle("Enable zoom", () -> c.zoomEnabled, v -> c.zoomEnabled = v));
        view.add(new Rows.Slider("Magnification", 1.2f, 10f, 0.1f, "x%.1f", () -> c.zoomMagnification, v -> c.zoomMagnification = v));
        view.add(new Rows.Slider("Zoom speed", 5, 100, 1, "%.0f%%", () -> c.zoomSpeed * 100f, v -> c.zoomSpeed = v / 100f));
        tabs.add(new Tab("View", view));

        List<Rows.Row> hit = new ArrayList<>();
        hit.add(new Rows.Header("HITBOX VIEWER"));
        hit.add(new Rows.Toggle("Custom hitboxes (evilFX renderer)", () -> c.hitboxCustom, v -> c.hitboxCustom = v));
        hit.add(new Rows.Toggle("Vanilla renderer instead (F3+B)", () -> c.hitboxes, v -> c.hitboxes = v));
        hit.add(new Rows.Slider("Max distance", 8, 128, 1, "%.0f blocks", () -> (float) c.hitDistance, v -> c.hitDistance = Math.round(v)));
        hit.add(new Rows.Toggle("Players", () -> c.hitPlayers, v -> c.hitPlayers = v));
        hit.add(new Rows.Toggle("Mobs", () -> c.hitMobs, v -> c.hitMobs = v));
        hit.add(new Rows.Toggle("Other entities (crystals, items...)", () -> c.hitOthers, v -> c.hitOthers = v));
        hit.add(new Rows.Header("PLAYER COLOR"));
        hit.add(new Rows.ColorRow("RGB", () -> c.hitPlayerColor, v -> c.hitPlayerColor = v));
        hit.add(new Rows.Slider("Opacity", 0, 100, 1, "%.0f%%", () -> (float) c.hitPlayerOpacity, v -> c.hitPlayerOpacity = Math.round(v)));
        hit.add(new Rows.Header("MOB COLOR"));
        hit.add(new Rows.ColorRow("RGB", () -> c.hitMobColor, v -> c.hitMobColor = v));
        hit.add(new Rows.Slider("Opacity", 0, 100, 1, "%.0f%%", () -> (float) c.hitMobOpacity, v -> c.hitMobOpacity = Math.round(v)));
        hit.add(new Rows.Header("OTHER ENTITY COLOR"));
        hit.add(new Rows.ColorRow("RGB", () -> c.hitOtherColor, v -> c.hitOtherColor = v));
        hit.add(new Rows.Slider("Opacity", 0, 100, 1, "%.0f%%", () -> (float) c.hitOtherOpacity, v -> c.hitOtherOpacity = Math.round(v)));
        hit.add(new Rows.Header("TARGET HIGHLIGHT (entity under your crosshair)"));
        hit.add(new Rows.Toggle("Highlight target", () -> c.hitTargetEnabled, v -> c.hitTargetEnabled = v));
        hit.add(new Rows.ColorRow("RGB", () -> c.hitTargetColor, v -> c.hitTargetColor = v));
        hit.add(new Rows.Slider("Opacity", 0, 100, 1, "%.0f%%", () -> (float) c.hitTargetOpacity, v -> c.hitTargetOpacity = Math.round(v)));
        hit.add(new Rows.Note("Line thickness follows vanilla and is not adjustable."));
        tabs.add(new Tab("Hitboxes", hit));

        List<Rows.Row> crystal = new ArrayList<>();
        crystal.add(new Rows.Header("CRYSTAL PVP COUNTERS"));
        crystal.add(new Rows.Button("Reset crystal stats", CrystalTracker::resetStats));
        hud(crystal, c.crystal);
        crystal.add(new Rows.Header("LOW STOCK WARNING (hotbar)"));
        crystal.add(new Rows.Toggle("Enable warning", () -> c.warnEnabled, v -> c.warnEnabled = v));
        crystal.add(new Rows.Slider("Obsidian below", 0, 64, 1, "%.0f", () -> (float) c.warnObsidianThreshold, v -> c.warnObsidianThreshold = Math.round(v)));
        crystal.add(new Rows.Slider("Crystals below", 0, 64, 1, "%.0f", () -> (float) c.warnCrystalThreshold, v -> c.warnCrystalThreshold = Math.round(v)));
        crystal.add(new Rows.ColorRow("Alert color", () -> c.warnColor, v -> c.warnColor = v));
        crystal.add(new Rows.Note("Armed once you have carried at least that many this session."));
        tabs.add(new Tab("Crystal", crystal));

        List<Rows.Row> totems = new ArrayList<>();
        totems.add(new Rows.Header("TOTEM COUNTER & POP ALERTS"));
        totems.add(new Rows.Toggle("Big alert when you pop", () -> c.popAlertEnabled, v -> c.popAlertEnabled = v));
        totems.add(new Rows.Toggle("Pop sound", () -> c.popSound, v -> c.popSound = v));
        hud(totems, c.totems);
        tabs.add(new Tab("Totems", totems));

        List<Rows.Row> timing = new ArrayList<>();
        timing.add(new Rows.Header("ATTACK RHYTHM (display only)"));
        timing.add(new Rows.Slider("Beat interval", 2, 20, 1, "%.0f ticks", () -> (float) c.timingInterval, v -> c.timingInterval = Math.round(v)));
        timing.add(new Rows.Toggle("Beat sound", () -> c.timingSound, v -> c.timingSound = v));
        hud(timing, c.timing);
        tabs.add(new Tab("Rhythm", timing));

        List<Rows.Row> perf = new ArrayList<>();
        perf.add(new Rows.Header("VANILLA OPTION TWEAKS"));
        perf.add(new Rows.Toggle("Decreased particles", () -> c.perfDecreasedParticles, v -> c.perfDecreasedParticles = v));
        perf.add(new Rows.Toggle("Disable clouds", () -> c.perfNoClouds, v -> c.perfNoClouds = v));
        perf.add(new Rows.Toggle("Reduce entity render distance", () -> c.perfEntityDistance, v -> c.perfEntityDistance = v));
        perf.add(new Rows.Slider("Entity distance", 50, 100, 1, "%.0f%%", () -> c.perfEntityDistanceValue * 100f, v -> c.perfEntityDistanceValue = v / 100f));
        perf.add(new Rows.Header("PARTICLE CULLING"));
        perf.add(new Rows.Toggle("Skip far-away particles", () -> c.perfParticleCull, v -> c.perfParticleCull = v));
        perf.add(new Rows.Slider("Cull beyond", 16, 64, 1, "%.0f blocks", () -> (float) c.perfParticleDistance, v -> c.perfParticleDistance = Math.round(v)));
        perf.add(new Rows.Note("For big FPS gains also install Sodium."));
        tabs.add(new Tab("Performance", perf));

        List<Rows.Row> about = new ArrayList<>();
        about.add(new Rows.Header("ABOUT"));
        about.add(new Rows.Note(Branding.NAME + " " + Branding.VERSION));
        about.add(new Rows.Note(Branding.CREDIT));
        about.add(new Rows.Button("Join the Discord (also copies the link)", this::openDiscord));
        about.add(new Rows.Note(Branding.DISCORD_URL));
        tabs.add(new Tab("About", about));

        tabHover = new float[tabs.size()];
    }

    /** The eight settings every HUD module shares. */
    private static void hud(List<Rows.Row> out, HudSettings s) {
        out.add(new Rows.Header("LAYOUT & STYLE"));
        out.add(new Rows.Toggle("Module enabled", () -> s.enabled, v -> s.enabled = v));
        out.add(new Rows.Slider("Scale", 50, 300, 5, "%.0f%%", () -> s.scale * 100f, v -> s.scale = v / 100f));
        out.add(new Rows.Cycle<>("Screen position", Anchor.values(), () -> s.anchor, v -> s.anchor = v));
        out.add(new Rows.Slider("Offset X", 0, 400, 1, "%.0f px", () -> (float) s.offsetX, v -> s.offsetX = Math.round(v)));
        out.add(new Rows.Slider("Offset Y", 0, 400, 1, "%.0f px", () -> (float) s.offsetY, v -> s.offsetY = Math.round(v)));
        out.add(new Rows.ColorRow("Text color", () -> s.color, v -> s.color = v));
        out.add(new Rows.Toggle("Text shadow", () -> s.shadow, v -> s.shadow = v));
        out.add(new Rows.Toggle("Background", () -> s.background, v -> s.background = v));
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        layout();
        if (openMs == 0) {
            openMs = Util.getMeasuringTimeMs();
            // Ignore a button that is already held when the menu opens.
            prevDown = leftDown();
        }
    }

    private void layout() {
        pw = Math.min(470, width - 16);
        ph = Math.min(300, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        sx = px + 10;
        sy = py + HEADER + 6;
        cx = sx + SIDE_W + 8;
        cy = sy;
        cw = px + pw - 10 - cx;
        ch = ph - HEADER - FOOTER - 10;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void removed() {
        ModConfig.get().save();
        if (client != null) PerformanceTweaks.apply(client);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget = MathHelper.clamp(scrollTarget - (float) verticalAmount * 26f, 0f, maxScroll());
        return true;
    }

    // ------------------------------------------------------------------ input (polled)

    private boolean leftDown() {
        return client != null && GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private void pollMouse(int mx, int my) {
        boolean down = leftDown();
        if (down && !prevDown) onPress(mx, my);
        else if (down && active != null) active.drag(mx);
        else if (!down && active != null) {
            active.release();
            active = null;
        }
        prevDown = down;
    }

    private void onPress(int mx, int my) {
        for (int i = 0; i < tabs.size(); i++) {
            int ty = sy + i * (TAB_H + TAB_GAP);
            if (mx >= sx && mx < sx + SIDE_W && my >= ty && my < ty + TAB_H) {
                if (i != current) {
                    current = i;
                    scroll = 0;
                    scrollTarget = 0;
                }
                return;
            }
        }
        if (inRect(mx, my, discordX(), discordY(), 88, 16)) {
            openDiscord();
            return;
        }
        if (inRect(mx, my, cx, cy, cw, ch)) {
            for (Rows.Row r : rows()) {
                if (r.press(mx, my)) {
                    active = r;
                    break;
                }
            }
        }
    }

    private void openDiscord() {
        if (client == null) return;
        client.keyboard.setClipboard(Branding.DISCORD_URL); // fallback if the browser does not open
        toast = "Link copied to clipboard";
        toastMs = Util.getMeasuringTimeMs();
        try {
            Util.getOperatingSystem().open(URI.create(Branding.DISCORD_URL));
        } catch (Exception e) {
            EvilFXClient.LOGGER.warn("Could not open Discord link", e);
        }
    }

    private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int discordX() {
        return px + pw - 10 - 88;
    }

    private int discordY() {
        return py + ph - FOOTER + 4;
    }

    private List<Rows.Row> rows() {
        return tabs.get(current).rows();
    }

    private float maxScroll() {
        int total = 12;
        for (Rows.Row r : rows()) total += r.height();
        return Math.max(0, total - ch);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta); // vanilla background (blur when enabled)

        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastNs) / 1_000_000_000f);
        lastNs = nowNs;
        long nowMs = Util.getMeasuringTimeMs();
        float t = MathHelper.clamp((nowMs - openMs) / 300f, 0f, 1f);
        float fade = 1f - (1f - t) * (1f - t) * (1f - t);

        scrollTarget = MathHelper.clamp(scrollTarget, 0f, maxScroll());
        scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-dt * 18f));
        pollMouse(mx, my);

        // dim backdrop
        ctx.fillGradient(0, 0, width, height, Gfx.a(0xFF0A0A14, 0.55f * fade), Gfx.a(0xFF000000, 0.75f * fade));

        drawPanel(ctx, fade);
        drawHeader(ctx, fade);
        drawTabs(ctx, mx, my, dt, fade);
        drawContent(ctx, mx, my, dt, fade);
        drawFooter(ctx, mx, my, dt, fade, nowMs);
    }

    private void drawPanel(DrawContext ctx, float fade) {
        Gfx.roundRect(ctx, px - 2, py - 2, pw + 4, ph + 4, 10, Gfx.a(0x30000000, fade));
        Gfx.roundRect(ctx, px, py, pw, ph, 8, Gfx.a(0xC00D0E12, fade));
        ctx.fillGradient(px + 4, py + 1, px + pw - 4, py + ph / 2, Gfx.a(0x1CFFFFFF, fade), 0x00FFFFFF);
        Gfx.roundBorder(ctx, px, py, pw, ph, 8, Gfx.a(0x90FFFFFF, fade));
    }

    private void drawHeader(DrawContext ctx, float fade) {
        int lx = px + 16;
        int ty = py + 15;
        int white = Gfx.a(0xFFFFFFFF, fade);
        int dim = Gfx.a(0x99FFFFFF, fade);

        // Marionette: a control bar with a handle, and three strings dropping onto the lettering.
        ctx.fill(lx, py + 7, lx + 82, py + 8, white);
        ctx.fill(lx + 38, py + 3, lx + 44, py + 7, dim);
        ctx.fill(lx + 4, py + 8, lx + 5, ty, dim);
        ctx.fill(lx + 40, py + 8, lx + 41, ty, dim);
        ctx.fill(lx + 76, py + 8, lx + 77, ty, dim);

        // Wordmark: bold, high-contrast white (vanilla has no serif font; see README to add one).
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(lx, ty);
        m.scale(2f, 2f);
        Text word = Text.literal("EVILFX").formatted(Formatting.BOLD);
        ctx.drawText(textRenderer, word, 0, 0, white, true);
        m.popMatrix();
        int wordW = textRenderer.getWidth(Text.literal("EVILFX").formatted(Formatting.BOLD)) * 2;

        // Small spaced "CLIENT" tag in crimson and the rose.
        ctx.drawText(textRenderer, "C L I E N T", lx + 2, ty + 20, Gfx.a(Branding.ACCENT, fade), false);
        rose(ctx, lx + wordW + 8, ty + 2, fade);

        // crisp white rule that fades out to the right
        for (int i = 0; i < 170; i++) {
            ctx.fill(px + 14 + i, py + 47, px + 15 + i, py + 48, Gfx.a(0xFFFFFFFF, fade * (1f - i / 170f)));
        }
        ctx.drawText(textRenderer, Branding.VERSION, px + pw - 14 - textRenderer.getWidth(Branding.VERSION), py + 14,
                Gfx.a(Rows.MUTED, fade), false);
    }

    /** A small pixel-art rose: the only color in the otherwise monochrome logo. */
    private void rose(DrawContext ctx, int x, int y, float fade) {
        int red = Gfx.a(Branding.ACCENT, fade);
        int dark = Gfx.a(0xFF6E0A18, fade);
        int hi = Gfx.a(0xFFE8394F, fade);
        int stem = Gfx.a(0xFFB0B0B0, fade);
        ctx.fill(x + 5, y + 11, x + 6, y + 19, stem);
        ctx.fill(x + 6, y + 14, x + 9, y + 15, stem);
        Gfx.roundRect(ctx, x, y, 11, 11, 5, red);
        Gfx.roundRect(ctx, x + 2, y + 2, 7, 7, 3, dark);
        Gfx.roundRect(ctx, x + 4, y + 4, 3, 3, 1, hi);
    }

    private int seg(DrawContext ctx, Text text, int x, int color) {
        ctx.drawText(textRenderer, text, x, 0, color, true);
        return x + textRenderer.getWidth(text);
    }

    private void drawTabs(DrawContext ctx, int mx, int my, float dt, float fade) {
        float target = sy + current * (TAB_H + TAB_GAP);
        if (tabY < 0) tabY = target;
        tabY += (target - tabY) * (1f - (float) Math.exp(-dt * 16f));

        int ty0 = Math.round(tabY);
        Gfx.roundRect(ctx, sx, ty0, SIDE_W, TAB_H, 5, Gfx.a(0x40C8102E, fade));
        Gfx.roundBorder(ctx, sx, ty0, SIDE_W, TAB_H, 5, Gfx.a(Branding.ACCENT, fade * 0.8f));

        for (int i = 0; i < tabs.size(); i++) {
            int ty = sy + i * (TAB_H + TAB_GAP);
            boolean over = inRect(mx, my, sx, ty, SIDE_W, TAB_H);
            tabHover[i] += ((over ? 1f : 0f) - tabHover[i]) * (1f - (float) Math.exp(-dt * 14f));
            if (i != current) {
                Gfx.roundRect(ctx, sx, ty, SIDE_W, TAB_H, 5, Gfx.a(0x22FFFFFF, fade * tabHover[i]));
            }
            int color = Gfx.mix(Rows.MUTED, Rows.WHITE, i == current ? 1f : tabHover[i]);
            ctx.drawText(textRenderer, tabs.get(i).name(), sx + 10, ty + 6, Gfx.a(color, fade), false);
        }
    }

    private void drawContent(DrawContext ctx, int mx, int my, float dt, float fade) {
        Gfx.roundRect(ctx, cx, cy, cw, ch, 6, Gfx.a(0x50000000, fade));
        Gfx.roundBorder(ctx, cx, cy, cw, ch, 6, Gfx.a(0x44FFFFFF, fade));

        ctx.enableScissor(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1);
        int rx = cx + 6;
        int rw = cw - 12;
        int y = cy + 6 - Math.round(scroll);
        for (Rows.Row r : rows()) {
            r.place(rx, y, rw);
            if (y + r.height() > cy && y < cy + ch) {
                r.render(ctx, textRenderer, mx, my, dt, fade);
            }
            y += r.height();
        }
        ctx.disableScissor();

        float max = maxScroll();
        if (max > 0) {
            int thumb = Math.max(16, (int) (ch * (ch / (float) (ch + max))));
            int ty = cy + 1 + Math.round((ch - 2 - thumb) * (scroll / max));
            Gfx.roundRect(ctx, cx + cw - 4, ty, 2, thumb, 1, Gfx.a(Branding.ACCENT, fade * 0.7f));
        }
    }

    private void drawFooter(DrawContext ctx, int mx, int my, float dt, float fade, long nowMs) {
        int fy = py + ph - FOOTER + 8;
        ctx.drawText(textRenderer, Branding.CREDIT, px + 14, fy, Gfx.a(Rows.MUTED, fade), false);

        boolean over = inRect(mx, my, discordX(), discordY(), 88, 16);
        discordHover += ((over ? 1f : 0f) - discordHover) * (1f - (float) Math.exp(-dt * 14f));
        Gfx.roundRect(ctx, discordX(), discordY(), 88, 16, 6, Gfx.a(Gfx.mix(0xE0101114, 0xF0C8102E, discordHover), fade));
        Gfx.roundBorder(ctx, discordX(), discordY(), 88, 16, 6, Gfx.a(0xCCFFFFFF, fade));
        String label = "Discord";
        ctx.drawText(textRenderer, label, discordX() + (88 - textRenderer.getWidth(label)) / 2, discordY() + 4,
                Gfx.a(Rows.WHITE, fade), false);

        long age = nowMs - toastMs;
        if (!toast.isEmpty() && age < 2500) {
            float a = fade * (1f - Math.max(0f, (age - 1800) / 700f));
            ctx.drawText(textRenderer, toast, discordX() - 8 - textRenderer.getWidth(toast), fy, Gfx.a(0xFF9AF0B0, a), false);
        }
    }
}
