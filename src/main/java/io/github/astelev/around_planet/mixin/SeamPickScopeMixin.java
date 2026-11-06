package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.client.SeamInteractionScope;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(GameRenderer.class)
public abstract class SeamPickScopeMixin {
    @WrapMethod(method="pick(F)V")
    private void around_planet$pick(float partial,Operation<Void> original) {
        SeamInteractionScope.enter();try{original.call(partial);}finally{SeamInteractionScope.exit();}
    }
}
