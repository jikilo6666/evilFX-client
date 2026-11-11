package dev.evilfx.mixin;

import dev.evilfx.feature.TotemTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Read-only listener for the "entity status" packet. Status 35 means "totem of undying activated".
 * We inject at TAIL so vanilla has fully handled the packet first, and we only read from it:
 * the packet is neither modified nor cancelled. The work is queued onto the client thread.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void evilfx$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.world == null) return;
            Entity entity = packet.getEntity(mc.world);
            if (entity != null) TotemTracker.onPop(entity);
        });
    }
}
