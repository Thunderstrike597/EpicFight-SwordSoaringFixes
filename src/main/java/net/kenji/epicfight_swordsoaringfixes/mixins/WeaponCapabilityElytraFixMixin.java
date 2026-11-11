package net.kenji.epicfight_swordsoaringfixes.mixins;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.p1nero.ss.gameassets.SwordSoaringDatakeys;
import net.p1nero.ss.gameassets.SwordSoaringSkillSlots;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

// priority > 1000 so our RETURN callback is applied after Sword Soaring's (default 1000)
@Mixin(value = WeaponCapability.class, remap = false, priority = 1500)
public abstract class WeaponCapabilityElytraFixMixin {

    @Shadow @Final
    protected Map<Style, Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> livingMotionModifiers;

    @Shadow public abstract Style getStyle(LivingEntityPatch<?> entitypatch);

    /** Hand back a copy so nobody (SS or EF's own putIfAbsent) can mutate the shared per-style map. */
    @ModifyVariable(method = "getLivingMotionModifier", at = @At("STORE"), ordinal = 0)
    private Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> ssfixes$copyMotions(
            Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> motions) {
        return new HashMap<>(motions);
    }

    /** Undo SS's FLY override unless the sword soaring skill is actually active. */
    @Inject(method = "getLivingMotionModifier", at = @At("RETURN"))
    private void ssfixes$restoreElytraFly(LivingEntityPatch<?> entityPatch, InteractionHand hand,
            CallbackInfoReturnable<Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> cir) {
        if (!(entityPatch instanceof PlayerPatch<?> playerPatch)) return;
        if (ssfixes$isSoaring(playerPatch)) return;

        Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> result = cir.getReturnValue();
        AnimationAccessor<? extends StaticAnimation> original = ssfixes$originalFly(entityPatch, hand);
        if (original != null) {
            result.put(LivingMotions.FLY, original);
        } else {
            result.remove(LivingMotions.FLY); // fall back to the animator's default elytra anim
        }
    }

    private static boolean ssfixes$isSoaring(PlayerPatch<?> playerPatch) {
        SkillContainer container = playerPatch.getSkill(SwordSoaringSkillSlots.SWORD_SOARING);
        if (container == null || !container.hasSkill()) return false;
        var manager = container.getDataManager();
        return manager.hasData(SwordSoaringDatakeys.FLYING.get())
                && Boolean.TRUE.equals(manager.getDataValue(SwordSoaringDatakeys.FLYING.get()));
    }

    private AnimationAccessor<? extends StaticAnimation> ssfixes$originalFly(LivingEntityPatch<?> patch, InteractionHand hand) {
        if (this.livingMotionModifiers == null || hand == InteractionHand.OFF_HAND) return null;
        var styleMap = this.livingMotionModifiers.get(this.getStyle(patch));
        if (styleMap != null && styleMap.containsKey(LivingMotions.FLY)) return styleMap.get(LivingMotions.FLY);
        var common = this.livingMotionModifiers.get(Styles.COMMON);
        return common != null ? common.get(LivingMotions.FLY) : null;
    }
}