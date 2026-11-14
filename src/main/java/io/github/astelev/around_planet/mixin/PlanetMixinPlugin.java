package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.extensibility.*;
import org.objectweb.asm.tree.ClassNode;
import java.util.*;
/** Optional API interfaces must not be resolved on a standalone installation. */
public final class PlanetMixinPlugin implements IMixinConfigPlugin {
    public boolean shouldApplyMixin(String target,String mixin){if(mixin.endsWith("SensorGogglesMixin"))return net.neoforged.fml.loading.FMLLoader.getLoadingModList().getMods().stream().anyMatch(m->m.getModId().equals("create"));return true;}
    public void onLoad(String p){}public String getRefMapperConfig(){return null;}public void acceptTargets(Set<String> own,Set<String> other){}public List<String> getMixins(){return null;}
    public void preApply(String t,ClassNode n,String m,IMixinInfo i){}public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
