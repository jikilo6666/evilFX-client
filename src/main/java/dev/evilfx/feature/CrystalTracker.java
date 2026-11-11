package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashSet;
import java.util.Set;

/**
 * Passive crystal-PvP statistics. Everything is derived from events the client already sees:
 *  - your own attacks on end crystals (AttackEntityCallback) -> "crystals hit" + gap between hits
 *  - your own right-clicks with obsidian / end crystals in hand (UseBlockCallback) -> placement attempts
 *  - end crystal entities disappearing near you -> approximate "last blast" timing
 *  - hotbar stock of obsidian / crystals -> low-stock warning
 * Counts are placement ATTEMPTS and the blast timer is approximate (a crystal leaving render range also counts).
 */
public final class CrystalTracker {
    public static int obsidianPlaced;
    public static int crystalsPlaced;
    public static int crystalsBroken;
    public static int lastBreakTick = -1;
    public static int breakGap = -1;
    public static int lastBlastTick = -1;

    public static int obsidianInHotbar;
    public static int crystalsInHotbar;
    public static boolean lowObsidian;
    public static boolean lowCrystals;

    private static int peakObsidian;
    private static int peakCrystals;
    private static Set<Integer> seen = new HashSet<>();
    private static Set<Integer> now = new HashSet<>();

    private CrystalTracker() {}

    public static void resetStats() {
        obsidianPlaced = 0;
        crystalsPlaced = 0;
        crystalsBroken = 0;
        lastBreakTick = -1;
        breakGap = -1;
        lastBlastTick = -1;
    }

    public static void resetSession() {
        resetStats();
        peakObsidian = 0;
        peakCrystals = 0;
        obsidianInHotbar = 0;
        crystalsInHotbar = 0;
        lowObsidian = false;
        lowCrystals = false;
        seen.clear();
        now.clear();
    }

    public static void onAttack(Entity target) {
        if (target instanceof EndCrystalEntity) {
            crystalsBroken++;
            if (lastBreakTick >= 0) breakGap = Clock.ticks - lastBreakTick;
            lastBreakTick = Clock.ticks;
        }
    }

    public static void onUse(ItemStack stack) {
        if (stack.isOf(Items.OBSIDIAN)) obsidianPlaced++;
        else if (stack.isOf(Items.END_CRYSTAL)) crystalsPlaced++;
    }

    public static void tick(MinecraftClient mc) {
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        // Crystal disappearance detection (cheap: runs 20x/s over loaded entities, ids only).
        now.clear();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity && e.squaredDistanceTo(p) < 24 * 24) now.add(e.getId());
        }
        for (Integer id : seen) {
            if (!now.contains(id)) lastBlastTick = Clock.ticks;
        }
        Set<Integer> tmp = seen;
        seen = now;
        now = tmp;

        // Low-stock warnings: only armed once you have carried at least `threshold` of the item this session.
        ModConfig c = ModConfig.get();
        obsidianInHotbar = InventoryUtil.hotbar(p, Items.OBSIDIAN);
        crystalsInHotbar = InventoryUtil.hotbar(p, Items.END_CRYSTAL);
        peakObsidian = Math.max(peakObsidian, obsidianInHotbar);
        peakCrystals = Math.max(peakCrystals, crystalsInHotbar);
        lowObsidian = c.warnEnabled && c.warnObsidianThreshold > 0
                && peakObsidian >= c.warnObsidianThreshold && obsidianInHotbar < c.warnObsidianThreshold;
        lowCrystals = c.warnEnabled && c.warnCrystalThreshold > 0
                && peakCrystals >= c.warnCrystalThreshold && crystalsInHotbar < c.warnCrystalThreshold;
    }
}
