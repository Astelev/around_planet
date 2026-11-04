package io.github.astelev.around_planet.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.Config;

import java.util.List;
import java.util.Set;

/** Runs before mixin attachment, where a normal injection would already be too late. */
public final class AeronauticsMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // All configurations have been prepared at this point, but overwrite attachment
        // (and its visibility check) has not started. Config.create reuses the cached handle.
        var handle = Config.create("imm_ptl.mixins.json", MixinEnvironment.getDefaultEnvironment());
        if (handle == null) throw new IllegalStateException("Immersive Aeronautics mixin config is missing");
        var config = handle.getConfig();
        try {
            // This is Mixin's existing overwrites.conformVisibility option. It widens
            // overwrites to match access-transformed targets instead of rejecting them.
            // Only IP's common configuration is changed; no global setting or JAR edit.
            var optionsField = config.getClass().getDeclaredField("overwriteOptions");
            optionsField.setAccessible(true);
            var options = optionsField.get(config);
            var conformField = options.getClass().getDeclaredField("conformAccessModifiers");
            conformField.setAccessible(true);
            conformField.setBoolean(options, true);
            System.getLogger("around_planet").log(System.Logger.Level.INFO,
                    "Enabled Aeronautics overwrite visibility conformance (original JAR supported)");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // The internal option is deliberately version-specific: fail explicitly if
            // Mixin changes, rather than pretending the crash fix was installed.
            throw new IllegalStateException("Cannot enable Aeronautics visibility compatibility for this Mixin version", exception);
        }
    }

    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".SablePortalCollisionMixin")) SablePortalCollisionPatch.apply(targetClass);
    }
}
