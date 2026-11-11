package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import dev.evilfx.feature.TotemTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/** Totem count (inventory + offhand) and a short feed of recent totem pops, yours and other players'. */
public final class TotemHud extends AbstractHud {
    private final List<String> texts = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();

    @Override
    protected HudSettings settings() {
        return ModConfig.get().totems;
    }

    @Override
    protected boolean prepare() {
        if (refreshDue(100)) rebuild();
        return !texts.isEmpty();
    }

    private void rebuild() {
        HudSettings s = settings();
        texts.clear();
        colors.clear();
        texts.add("Totems: " + TotemTracker.totems);
        colors.add(TotemTracker.totems == 0 ? 0xFFFF5555 : argb(s));

        long now = Util.getMeasuringTimeMs();
        for (TotemTracker.PopEvent ev : TotemTracker.feed()) {
            if (now - ev.atMs() > 8000) continue;
            texts.add(ev.name() + " popped (" + ev.total() + ")");
            colors.add(0xFFFFAA33);
        }

        int max = 0;
        for (String l : texts) max = Math.max(max, mc.textRenderer.getWidth(l));
        w = max + PAD * 2;
        h = texts.size() * LINE + PAD * 2 - 1;
    }

    @Override
    protected void draw(DrawContext ctx, HudSettings s) {
        background(ctx, s);
        for (int i = 0; i < texts.size(); i++) {
            text(ctx, texts.get(i), PAD, PAD + i * LINE, colors.get(i), s.shadow);
        }
    }
}
