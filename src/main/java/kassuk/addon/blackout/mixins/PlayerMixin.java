package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.AntiCrawl;
import kassuk.addon.blackout.modules.impl.SoundModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Unique
    private Entity blackoutAddon$attackEntity;

    @Inject(method = "attack", at = @At(value = "HEAD"))
    private void inject(Entity target, CallbackInfo ci) {
        blackoutAddon$attackEntity = target;
    }

    @Redirect(method = "playServerSideSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void modifyCrystalHitSound(Level instance, Entity except, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        SoundModifier m = Modules.get().get(SoundModifier.class);

        if ((Object) this == mc.player && blackoutAddon$attackEntity instanceof EndCrystal && m != null && m.isActive()) {
            if (m.crystalHits.get()) {
                instance.playSound(except, x, y, z, sound, category, (float) (volume * m.crystalHitVolume.get()), (float) (pitch * m.crystalHitPitch.get()));
            }
            return;
        }
        instance.playSound(except, x, y, z, sound, category, volume, pitch);
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void blackoutAddon$clearAttackEntity(Entity target, CallbackInfo ci) {
        blackoutAddon$attackEntity = null;
    }

    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("RETURN"), cancellable = true)
    private void allowStandingPose(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == mc.player && pose == Pose.STANDING && Modules.get().isActive(AntiCrawl.class)) {
            cir.setReturnValue(true);
        }
    }
}
