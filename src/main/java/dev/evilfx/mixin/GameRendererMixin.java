package dev.evilfx.mixin;

import dev.evilfx.feature.ZoomHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only mixin in the mod. GameRenderer#getFov computes the world field of view each frame.
 * We let vanilla finish (sprint FOV, potion effects, the FOV slider...) and then narrow the result
 * at RETURN. The `changingFov` flag is false when the held-item model asks for its FOV, so we skip
 * that call and the hand does not get distorted while zoomed.
 *
 * Zooming scales tan(fov/2) rather than the raw angle, which gives true optical magnification:
 * a 4x zoom really shows a quarter of the view width, instead of a quarter of the angle.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void evilfx$applyZoom(Camera camera, float tickProgress, boolean changingFov,
                                       CallbackInfoReturnable<Float> cir) {
        if (!changingFov || !ZoomHandler.isActive()) return;

        double halfAngle = Math.toRadians(cir.getReturnValue()) / 2.0;
        double zoomed = 2.0 * Math.atan(Math.tan(halfAngle) * ZoomHandler.multiplier(tickProgress));
        cir.setReturnValue((float) Math.toDegrees(zoomed));
    }
}
