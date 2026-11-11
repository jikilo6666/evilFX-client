package dev.evilfx.mixin;

import dev.evilfx.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional distance culling for particles. Every particle spawn goes through ParticleManager#addParticle;
 * when the option is on and the spawn point is farther than the configured distance from you, we return
 * early with null (vanilla callers already accept a null result), so the particle object is never created,
 * ticked or rendered. Particles near you are untouched, so visuals around the fight stay vanilla.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true)
    private void evilfx$cullFarParticles(ParticleEffect effect, double x, double y, double z,
                                         double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.perfParticleCull) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        double max = cfg.perfParticleDistance;
        if (player.squaredDistanceTo(x, y, z) > max * max) cir.setReturnValue(null);
    }
}
