package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class EmptyWorldPerformanceSmoke {
    private static int ticks;private static Object generationEntry,previousPlan;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!Boolean.getBoolean("around_planet.emptyPerfSmoke"))return;var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==100){server(mc,()->{PlanetConfig.ENABLED.set(true);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(0,200,0);});AroundPlanet.LOGGER.info("EMPTY_PERF stage=enabled pid={}",ProcessHandle.current().pid());}
        else if(ticks==500){server(mc,()->PlanetConfig.ENABLED.set(false));AroundPlanet.LOGGER.info("EMPTY_PERF stage=disabled");}
        else if(ticks==900){pauseDh();server(mc,()->PlanetConfig.ENABLED.set(true));AroundPlanet.LOGGER.info("EMPTY_PERF stage=enabled_dh_paused");}
        else if(ticks==1300){server(mc,()->PlanetConfig.ENABLED.set(false));AroundPlanet.LOGGER.info("EMPTY_PERF stage=disabled_dh_paused");}
        else if(ticks==1700){restoreDh();AroundPlanet.LOGGER.info("EMPTY_PERF PASS");mc.stop();}
    }
    private static void server(Minecraft mc,Runnable r){mc.getSingleplayerServer().execute(r);}
    private static void pauseDh(){try{var cls=Class.forName("com.seibel.distanthorizons.core.config.Config$Common$WorldGenerator");var entry=cls.getField("generatorPlan").get(null);generationEntry=entry;previousPlan=entry.getClass().getMethod("get").invoke(entry);entry.getClass().getMethod("set",Object.class).invoke(entry,Class.forName("com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiGeneratorPlan").getField("DISABLED").get(null));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static void restoreDh(){try{if(generationEntry!=null)generationEntry.getClass().getMethod("set",Object.class).invoke(generationEntry,previousPlan);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
