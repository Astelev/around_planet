package io.github.astelev.around_planet.client;
import net.minecraft.world.level.ColorResolver;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
/** ClientLevel allocates tint caches only for registered resolver identities. */
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class MoonBlockColors {
 public static final ColorResolver REGOLITH=(biome,x,z)->{int color=biome.getGrassColor(x,z);return switch(color){case 0xaaa8a3,0x676972,0xc0bcb4,0x8b8884->color;default->0xaaa8a3;};};
 @SubscribeEvent public static void resolvers(RegisterColorHandlersEvent.ColorResolvers e){e.register(REGOLITH);}
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){e.register((state,level,pos,tint)->level!=null&&pos!=null?level.getBlockTint(pos,REGOLITH):0xaaa8a3,io.github.astelev.around_planet.moon.MoonRegistry.REGOLITH.get());}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item e){e.register((stack,tint)->0xaaa8a3,io.github.astelev.around_planet.moon.MoonRegistry.REGOLITH.get());}
}
