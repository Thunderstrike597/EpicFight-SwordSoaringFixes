package net.kenji.epicfight_swordsoaringfixes.mixins;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import yesman.epicfight.api.client.event.EpicFightClientHooks;

@Mixin(Camera.class)
public interface AccessorCamera {
    @Accessor("eyeHeightOld")
    float accessorEyeHeightOld();
    @Accessor("eyeHeight")
    float accessorEyeHeight();
    @Invoker("setPosition")
    void invokeSetPos(double pX, double pY, double pZ);
}
