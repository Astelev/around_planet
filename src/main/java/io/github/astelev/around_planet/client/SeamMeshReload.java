package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SeamMeshReload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{
            SeamMeshCache.clear();var level=Minecraft.getInstance().level;
            if(level!=null)for(var pos:SeamImages.positions(level))SeamMeshCache.changed(level,pos);
        }));
    }
    private SeamMeshReload() {}
}
