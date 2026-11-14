package io.github.astelev.around_planet.gametest;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import io.github.astelev.around_planet.space.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class RendezvousReloadSmoke {
    private static int ticks;private static boolean checking;private static volatile boolean passed;private static volatile Throwable failure;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("around_planet.rendezvousReloadSmoke"))return;var mc=Minecraft.getInstance();if(++ticks>1800)throw new AssertionError("Rendezvous reload timeout");if(failure!=null)throw new AssertionError("Rendezvous reload",failure);if(passed){io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_RENDEZVOUS_RELOAD PASS merged frame, orbit catalog, native ticket decoding and loaded arrivals");mc.stop();return;}if(mc.player==null||mc.level==null||ticks<160||ticks%20!=0||checking)return;if(!SpaceCells.contains(mc.player))return;checking=true;mc.getSingleplayerServer().execute(()->{try{var level=mc.getSingleplayerServer().overworld();var c=SubLevelContainer.getContainer(level);var cells=SpaceCells.get(level);if(cells.all().size()!=1)throw new AssertionError("Merged cell did not persist");var host=cells.all().getFirst();if(!SpaceOrbitData.get(level.getServer()).ids().equals(java.util.Set.of(host.id())))throw new AssertionError("Retired source orbit came back");long tickets=c.collectForceLoadTickets().values().stream().flatMap(java.util.Collection::stream).filter(t->t.type().name().toString().equals("around_planet:rendezvous")).count();if(tickets<2)throw new AssertionError("Arrival load tickets did not survive reload: "+tickets);if(c.getAllSubLevels().size()<3)throw new AssertionError("Arrivals absent after reload");passed=true;}catch(Throwable x){failure=x;}finally{checking=false;}});}
}
