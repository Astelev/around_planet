package io.github.astelev.around_planet.client;
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class MoonBlockColors {
 @net.neoforged.bus.api.SubscribeEvent public static void blocks(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e){e.register((state,level,pos,tint)->level!=null&&pos!=null?level.getBlockTint(pos,(biome,x,z)->{int color=biome.getGrassColor(x,z);return switch(color){case 0xaaa8a3,0x676972,0xc0bcb4,0x8b8884->color;default->0xaaa8a3;};}):0xaaa8a3,io.github.astelev.around_planet.moon.MoonRegistry.REGOLITH.get());}
 @net.neoforged.bus.api.SubscribeEvent public static void items(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e){e.register((stack,tint)->0xaaa8a3,io.github.astelev.around_planet.moon.MoonRegistry.REGOLITH.get());}
}
