package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.fuel.CryogenicFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CryogenicFluidAppearance {
 @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerFluidType(appearance(0xC9E6FFFF),CryogenicFluids.HYDROGEN_TYPE.get());e.registerFluidType(appearance(0xD095CFFF),CryogenicFluids.OXYGEN_TYPE.get());}
 private static IClientFluidTypeExtensions appearance(int color){return new IClientFluidTypeExtensions(){public ResourceLocation getStillTexture(){return ResourceLocation.withDefaultNamespace("block/water_still");}public ResourceLocation getFlowingTexture(){return ResourceLocation.withDefaultNamespace("block/water_flow");}public int getTintColor(){return color;}};}
 @SubscribeEvent public static void buckets(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e){e.register(new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors(),CryogenicFluids.HYDROGEN_BUCKET.get(),CryogenicFluids.OXYGEN_BUCKET.get());}
 @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{for(var fluid:java.util.List.of(CryogenicFluids.HYDROGEN.get(),CryogenicFluids.HYDROGEN_FLOWING.get(),CryogenicFluids.OXYGEN.get(),CryogenicFluids.OXYGEN_FLOWING.get()))net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(fluid,net.minecraft.client.renderer.RenderType.translucent());});}
}
