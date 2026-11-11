package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

/**
 * Attack rhythm helper. It is a metronome: after each of YOUR attacks it counts client ticks and shows
 * (and optionally beeps) when the next beat of the chosen interval arrives. It never attacks, clicks or
 * changes packet timing; you still press the button yourself.
 */
public final class TimingTracker {
    private static int lastAttackTick = -1;

    private TimingTracker() {}

    public static void reset() {
        lastAttackTick = -1;
    }

    public static void onAttack() {
        lastAttackTick = Clock.ticks;
    }

    /** Active for 3 seconds after your last attack, then it goes idle. */
    public static boolean active() {
        return lastAttackTick >= 0 && Clock.ticks - lastAttackTick <= 60;
    }

    public static float progress(float partialTick) {
        int interval = Math.max(2, ModConfig.get().timingInterval);
        float elapsed = (Clock.ticks - lastAttackTick) + partialTick;
        return (elapsed % interval) / interval;
    }

    public static void tick(MinecraftClient mc) {
        ModConfig c = ModConfig.get();
        if (!c.timing.enabled || !c.timingSound || !active()) return;
        int elapsed = Clock.ticks - lastAttackTick;
        if (elapsed > 0 && elapsed % Math.max(2, c.timingInterval) == 0) {
            Sfx.play(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 1.5f);
        }
    }
}
