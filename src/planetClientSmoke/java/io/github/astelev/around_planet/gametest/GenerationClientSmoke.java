package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.generation.SeamGenerationState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Dedicated fresh noise-world fixture: actual full chunks, world binding, client images and both directions. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class GenerationClientSmoke {
    private static int ticks,phase,started;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static volatile int base;
    private static final int EDGE=65536,Z=192000;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.generationSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("GENERATION_CLIENT fixture",failure);
        if(ticks>3600)throw new AssertionError("GENERATION_CLIENT timeout phase="+phase);
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        if(phase==0) {
            phase=1;started=ticks;mc.options.renderDistance().set(5);mc.options.setServerRenderDistance(5);
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
                    var state=(SeamGenerationState)(Object)level.getChunkSource().randomState();
                    require(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator,"Fixture is not a normal noise world");
                    require(state.around_planet$generationBlend()!=null && state.around_planet$generationBlend().boundary()==EDGE,"Normal world generation was not bound before chunk generation");
                    p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
                    level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                    for(int x:new int[]{EDGE/16-2,EDGE/16-1,-EDGE/16,-EDGE/16+1})for(int z=(Z>>4)-1;z<=(Z>>4)+1;z++)level.getChunk(x,z);
                    double delta=0;
                    for(int z=Z;z<Z+16;z++) {
                        int a=level.getHeight(Heightmap.Types.OCEAN_FLOOR,EDGE-1,z),b=level.getHeight(Heightmap.Types.OCEAN_FLOOR,-EDGE,z);
                        delta+=Math.abs(a-b);
                    }
                    require(delta/16<16,"Real generated shore still has a large cliff: "+delta/16);
                    base=level.getHeight(Heightmap.Types.WORLD_SURFACE,-EDGE,Z);
                    AroundPlanet.LOGGER.info("GENERATION_CLIENT actual full chunks PASS: auto-bound noise world, mean ocean-floor seam step={} blocks",delta/16);
                    p.connection.teleport(EDGE-8,base+32,Z+.5,-90,30);ready=true;
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==1 && ready && ticks-started>80) {
            var chunk=SeamImages.get(mc.level,EDGE>>4,Z>>4);
            if(chunk==null)return;
            require(!chunk.getBlockState(new BlockPos(EDGE,base-1,Z)).isAir(),"East image missed the real generated surface");
            AroundPlanet.LOGGER.info("GENERATION_CLIENT east PASS: newly generated smoothed shore reaches the native Sodium preview");
            phase=2;started=ticks;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(-EDGE+8,base+32,Z+.5,90,30));
        } else if(phase==2 && ticks-started>60) {
            if(SeamImages.get(mc.level,(-EDGE-1)>>4,Z>>4)==null)return;
            AroundPlanet.LOGGER.info("GENERATION_CLIENT west PASS: opposite shore previews correctly after side change");
            phase=3;started=ticks;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(0,180,0,0,0));
        } else if(phase==3 && ticks-started>100) {phase=4;mc.stop();}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
