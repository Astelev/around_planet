package io.github.astelev.around_planet.mixin;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=KeyboardHandler.class,priority=2000)
public abstract class OrbitMapKeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
    private void space$mapFirst(long window,int key,int scan,int action,int modifiers,CallbackInfo ci){if(io.github.astelev.around_planet.client.OrbitMapKeys.handle(window,key,scan,action))ci.cancel();}
}
