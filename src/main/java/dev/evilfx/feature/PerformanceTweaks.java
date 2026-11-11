package dev.evilfx.feature;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;

/**
 * Optional FPS helpers built purely on vanilla options (nothing is hidden that matters for play).
 * When a tweak is switched on, the player's original value is stored in the config so that
 * switching it off (even after a restart) puts the vanilla setting back.
 * Runs on client start and whenever the config screen is saved.
 */
public final class PerformanceTweaks {
    private PerformanceTweaks() {}

    public static void apply(MinecraftClient mc) {
        ModConfig c = ModConfig.get();
        GameOptions o = mc.options;

        // Particles
        if (c.perfDecreasedParticles) {
            if (c.backup.particles == null) c.backup.particles = o.getParticles().getValue().name();
            o.getParticles().setValue(ParticlesMode.DECREASED);
        } else if (c.backup.particles != null) {
            try {
                o.getParticles().setValue(ParticlesMode.valueOf(c.backup.particles));
            } catch (IllegalArgumentException ignored) {
                // unknown stored value: leave the current setting alone
            }
            c.backup.particles = null;
        }

        // Clouds
        if (c.perfNoClouds) {
            if (c.backup.clouds == null) c.backup.clouds = o.getCloudRenderMode().getValue().name();
            o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        } else if (c.backup.clouds != null) {
            try {
                o.getCloudRenderMode().setValue(CloudRenderMode.valueOf(c.backup.clouds));
            } catch (IllegalArgumentException ignored) {
                // unknown stored value: leave the current setting alone
            }
            c.backup.clouds = null;
        }

        // Entity render distance (vanilla slider, 50%-500%)
        if (c.perfEntityDistance) {
            if (c.backup.entityDistance == null) c.backup.entityDistance = o.getEntityDistanceScaling().getValue();
            o.getEntityDistanceScaling().setValue((double) c.perfEntityDistanceValue);
        } else if (c.backup.entityDistance != null) {
            o.getEntityDistanceScaling().setValue(c.backup.entityDistance);
            c.backup.entityDistance = null;
        }

        c.save();
    }
}
