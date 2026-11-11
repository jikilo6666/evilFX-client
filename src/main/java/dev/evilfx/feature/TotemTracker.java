package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Totem counter + pop feed. Pops are read from the "entity status 35" packet the server already
 * sends to every client (see ClientPlayNetworkHandlerMixin); we only observe it.
 */
public final class TotemTracker {
    public record PopEvent(String name, int total, long atMs) {}

    private static final Map<UUID, Integer> POPS = new HashMap<>();
    private static final ArrayDeque<PopEvent> FEED = new ArrayDeque<>();

    public static long selfPopMs = -1;
    public static int totems;

    private TotemTracker() {}

    public static void reset() {
        POPS.clear();
        FEED.clear();
        selfPopMs = -1;
        totems = 0;
    }

    public static Iterable<PopEvent> feed() {
        return FEED;
    }

    public static void tick(MinecraftClient mc) {
        totems = InventoryUtil.count(mc.player, Items.TOTEM_OF_UNDYING);
    }

    public static void onPop(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return;
        int n = POPS.merge(player.getUuid(), 1, Integer::sum);
        long now = Util.getMeasuringTimeMs();
        FEED.addFirst(new PopEvent(player.getName().getString(), n, now));
        while (FEED.size() > 5) FEED.removeLast();

        if (entity == MinecraftClient.getInstance().player) {
            selfPopMs = now;
            if (ModConfig.get().popSound) Sfx.play(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.7f);
        }
    }
}
