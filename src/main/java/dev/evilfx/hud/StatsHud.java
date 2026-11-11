package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * FPS, frame time and ping. Frame time is measured with System.nanoTime() between rendered frames
 * (microsecond resolution, shown with 3 decimals) and smoothed with a light moving average.
 * The text is rebuilt 10x per second, not every frame, to keep the overlay itself nearly free.
 */
public final class StatsHud extends AbstractHud {
    private final List<String> lines = new ArrayList<>();
    private long lastNs;
    private double emaMs;

    @Override
    protected HudSettings settings() {
        return ModConfig.get().stats;
    }

    @Override
    protected boolean prepare() {
        long now = System.nanoTime();
        if (lastNs != 0) {
            double ms = (now - lastNs) / 1_000_000.0;
            emaMs = emaMs == 0 ? ms : emaMs + (ms - emaMs) * 0.1;
        }
        lastNs = now;

        if (refreshDue(100)) rebuild();
        return !lines.isEmpty();
    }

    private void rebuild() {
        ModConfig c = ModConfig.get();
        lines.clear();
        if (c.showFps) lines.add("FPS: " + mc.getCurrentFps());
        if (c.showFrameTime) lines.add(String.format("Frame: %.3f ms", emaMs));
        if (c.showPing) lines.add("Ping: " + pingText());

        int max = 0;
        for (String l : lines) max = Math.max(max, mc.textRenderer.getWidth(l));
        w = max + PAD * 2;
        h = lines.size() * LINE + PAD * 2 - 1;
    }

    private String pingText() {
        if (mc.isInSingleplayer()) return "local";
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (net == null || mc.player == null) return "--";
        PlayerListEntry entry = net.getPlayerListEntry(mc.player.getUuid());
        return entry == null ? "--" : entry.getLatency() + " ms";
    }

    @Override
    protected void draw(DrawContext ctx, HudSettings s) {
        background(ctx, s);
        for (int i = 0; i < lines.size(); i++) {
            text(ctx, lines.get(i), PAD, PAD + i * LINE, argb(s), s.shadow);
        }
    }
}
