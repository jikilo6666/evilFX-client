package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import dev.evilfx.feature.Clock;
import dev.evilfx.feature.CrystalTracker;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Crystal PvP counters: placements, hits, gap between your crystal hits, and time since the last nearby blast. */
public final class CrystalHud extends AbstractHud {
    private final List<String> lines = new ArrayList<>();

    @Override
    protected HudSettings settings() {
        return ModConfig.get().crystal;
    }

    @Override
    protected boolean prepare() {
        if (refreshDue(100)) rebuild();
        return !lines.isEmpty();
    }

    private void rebuild() {
        lines.clear();
        lines.add("Obsidian placed: " + CrystalTracker.obsidianPlaced);
        lines.add("Crystals placed: " + CrystalTracker.crystalsPlaced);
        lines.add("Crystals hit: " + CrystalTracker.crystalsBroken);
        lines.add("Hit gap: " + (CrystalTracker.breakGap < 0 ? "--" : CrystalTracker.breakGap + "t"));
        lines.add("Last blast: " + (CrystalTracker.lastBlastTick < 0 ? "--" : (Clock.ticks - CrystalTracker.lastBlastTick) + "t ago"));
        lines.add("Hotbar: " + CrystalTracker.obsidianInHotbar + " obs / " + CrystalTracker.crystalsInHotbar + " crys");

        int max = 0;
        for (String l : lines) max = Math.max(max, mc.textRenderer.getWidth(l));
        w = max + PAD * 2;
        h = lines.size() * LINE + PAD * 2 - 1;
    }

    @Override
    protected void draw(DrawContext ctx, HudSettings s) {
        background(ctx, s);
        for (int i = 0; i < lines.size(); i++) {
            text(ctx, lines.get(i), PAD, PAD + i * LINE, argb(s), s.shadow);
        }
    }
}
