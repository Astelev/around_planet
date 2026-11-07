package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.AroundPlanet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
/** A render-frame stamp: opaque and transparent DH passes share a single coverage snapshot. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamFrameClock {
    private static long frame;
    public static long frame(){return frame;}
    @SubscribeEvent public static void begin(RenderFrameEvent.Pre event){frame++;}
    private SeamFrameClock(){}
}
