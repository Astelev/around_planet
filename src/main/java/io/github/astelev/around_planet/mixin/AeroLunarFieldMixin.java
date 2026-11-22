package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo @Mixin(targets="net.aste.aeroworks.wind.WindFields",remap=false)
public abstract class AeroLunarFieldMixin {
 @Unique private static final ClassValue<java.lang.reflect.Method> LEVEL=new ClassValue<>(){protected java.lang.reflect.Method computeValue(Class<?> c){try{return c.getMethod("level");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}};
 @Inject(method="sampleForStep",at=@At("HEAD"),cancellable=true,require=0)
 private static void moonField(@Coerce Object sample,org.joml.Vector3d wind,org.joml.Vector3d torque,CallbackInfoReturnable<org.joml.Vector3d> ci){try{if(LEVEL.get(sample.getClass()).invoke(sample) instanceof net.minecraft.world.level.Level l&&l.dimension().equals(io.github.astelev.around_planet.space.MoonSurface.dimension())){if(torque!=null)torque.zero();ci.setReturnValue(wind.zero());}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
