package kassuk.addon.blackout.mixins.accessor;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RaycastContext.class)
public interface RaycastContextAccessor {
    @Mutable
    @Accessor("start")
    void blackoutAddon$setStart(Vec3d start);

    @Mutable
    @Accessor("end")
    void blackoutAddon$setEnd(Vec3d end);
}
