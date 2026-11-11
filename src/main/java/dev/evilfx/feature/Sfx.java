package dev.evilfx.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

/** Plays a local UI-style sound. Purely client-side audio; no packets involved. */
public final class Sfx {
    private Sfx() {}

    public static void play(RegistryEntry<SoundEvent> sound, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }
}
