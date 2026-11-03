package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamVisualSmoke {
    private static int ticks,phase,started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.visualSmoke")) return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null) throw new AssertionError("SEAM_VISUAL_SMOKE fixture failure",failure);
        if(ticks>2400) throw new AssertionError("SEAM_VISUAL_SMOKE timeout, phase="+phase+" images="+SeamImages.size());
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);mc.player.setNoGravity(true);
        if(phase==0) {
            mc.options.renderDistance().set(12);mc.options.setServerRenderDistance(12);
            phase=1;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    p.getServer().getPlayerList().setViewDistance(12);
                    p.stopRiding();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);
                    p.getAbilities().flying=true;p.onUpdateAbilities();
                    var level=p.serverLevel();level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                    for(int x:new int[]{-17,-16,-15,14,15,16})for(int z=-2;z<=2;z++)level.getChunk(x,z);
                    wall(level,-246,Blocks.RED_CONCRETE);wall(level,245,Blocks.AIR);
                    level.setBlockAndUpdate(new BlockPos(252,82,18),Blocks.WHITE_CONCRETE.defaultBlockState());
                    // Deliberately different real terrain beyond each seam must never be the rendered source.
                    wall(level,266,Blocks.BLUE_CONCRETE);wall(level,-267,Blocks.BLUE_CONCRETE);
                    level.setBlockAndUpdate(new BlockPos(-245,80,13),Blocks.WATER.defaultBlockState());
                    p.connection.teleport(210.5,82.5,8.5,-90,0);prepared=true;
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==1 && prepared && ticks-started>60) {
            var image=SeamImages.get(mc.level,16,0);
            if(image==null || !image.getBlockState(new BlockPos(266,82,8)).is(Blocks.RED_CONCRETE))return;
            var region=new RenderRegionCache().createRegion(mc.level,SectionPos.of(16,5,0),false);
            require(region.getBlockState(new BlockPos(266,82,8)).is(Blocks.RED_CONCRETE),"Render region did not use opposite coast");
            require(!mc.level.getBlockState(new BlockPos(266,82,8)).is(Blocks.RED_CONCRETE),"Visual snapshot leaked into gameplay cache");
            require(!image.getFluidState(new BlockPos(267,80,13)).isEmpty(),"Water snapshot missing");
            boolean imageReady=compiled(mc,new BlockPos(266,82,8)),edgeReady=compiled(mc,new BlockPos(252,82,18));
            if(ticks%100==0) AroundPlanet.LOGGER.info("SEAM visual compile: image={}, edge={}, view={}",imageReady,edgeReady,mc.options.getEffectiveRenderDistance());
            if(!imageReady || !edgeReady || !pixel(mc,0))return;
            AroundPlanet.LOGGER.info("SEAM_VISUAL_SMOKE east PASS: opposite red wall beyond border, native compiled GPU buffer, water and isolated gameplay cache");
            phase=2;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                wall(p.serverLevel(),245,Blocks.LIME_CONCRETE);
                p.connection.teleport(-247.5,82.5,8.5,90,0);
            });
        } else if(phase==2 && ticks-started>60) {
            var image=SeamImages.get(mc.level,-17,0);
            if(image==null || !image.getBlockState(new BlockPos(-267,82,8)).is(Blocks.LIME_CONCRETE))return;
            if(!compiled(mc,new BlockPos(-267,82,8)) || !pixel(mc,1))return;
            require(SeamImages.get(mc.level,16,0)==null,"Old coast image was not evicted after transfer");
            phase=3;started=ticks;
            mc.getSingleplayerServer().execute(()->wall(mc.getSingleplayerServer().overworld(),245,Blocks.GOLD_BLOCK));
        } else if(phase==3 && ticks-started>60) {
            var image=SeamImages.get(mc.level,-17,0);
            if(image==null || !image.getBlockState(new BlockPos(-267,82,8)).is(Blocks.GOLD_BLOCK) || !pixel(mc,2))return;
            verifyOptionalDh(mc);
            verifyOptionalSable(mc);
            AroundPlanet.LOGGER.info("SEAM_VISUAL_SMOKE PASS: both directions, real GPU pixels, block updates, water, lighting, cache eviction and no gameplay aliases");
            phase=4;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.connection.teleport(0,200,0,0,0);
            });
        } else if(phase==4 && ticks-started>120) {
            phase=5;
            // Continue with the existing real-player/vehicle transition regression in the same warmed client.
            System.setProperty("around_planet.visualSmoke","false");
        }
    }
    private static void wall(net.minecraft.server.level.ServerLevel level,int x,net.minecraft.world.level.block.Block block) {
        for(int dx=0;dx<3;dx++)for(int y=80;y<=84;y++)for(int z=5;z<=11;z++)
            level.setBlockAndUpdate(new BlockPos(x+dx,y,z),block.defaultBlockState());
    }
    private static boolean compiled(Minecraft mc,BlockPos pos) {
        var alternative=io.github.astelev.around_planet.client.AlternativeRendererBridge.isReady(pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4);
        if(alternative!=null)return alternative;
        try {
            var f=mc.levelRenderer.getClass().getDeclaredField("viewArea");f.setAccessible(true);var view=f.get(mc.levelRenderer);
            var m=view.getClass().getDeclaredMethod("getRenderSectionAt",BlockPos.class);m.setAccessible(true);
            var section=(net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection)m.invoke(view,pos);
            return section!=null && !section.getCompiled().isEmpty(RenderType.solid());
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static boolean pixel(Minecraft mc,int color) {
        mc.getMainRenderTarget().bindWrite(false);
        var bytes=BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(mc.getWindow().getWidth()/2+10,mc.getWindow().getHeight()/2-10,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
        int r=bytes.get(0)&255,g=bytes.get(1)&255,b=bytes.get(2)&255;
        if(ticks%100==0) AroundPlanet.LOGGER.info("SEAM visual pixel: phase={}, rgb={},{},{}",phase,r,g,b);
        return color==0 ? r>40 && r>g*1.4 && r>b*1.4 : color==1 ? g>40 && g>r*1.3 && g>b*1.2 : r>60 && g>60 && r>b*1.5 && g>b*1.5;
    }
    private static void verifyOptionalDh(Minecraft mc) {
        if(!net.neoforged.fml.ModList.get().isLoaded("distanthorizons")) return;
        try {
            var loader=SeamVisualSmoke.class.getClassLoader();
            var generatorType=Class.forName("com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator");
            var serverType=Class.forName("com.seibel.distanthorizons.core.level.IDhServerLevel");
            var wrapperType=Class.forName("com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper");
            Object wrapper=java.lang.reflect.Proxy.newProxyInstance(loader,new Class[]{wrapperType},(p,m,a)->
                m.getName().equals("getWrappedMcObject") ? mc.getSingleplayerServer().overworld() : null);
            Object dhLevel=java.lang.reflect.Proxy.newProxyInstance(loader,new Class[]{serverType},(p,m,a)->
                m.getName().equals("getServerLevelWrapper") ? wrapper : null);
            Object generator=java.lang.reflect.Proxy.newProxyInstance(loader,new Class[]{generatorType},(p,m,a)->{
                throw new AssertionError("Outside DH request reached the generator: "+m.getName());
            });
            var queueType=Class.forName("com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue");
            var queue=queueType.getConstructor(generatorType,serverType).newInstance(generator,dhLevel);
            try {
                long outside=6L | (4L<<8);
                var future=(java.util.concurrent.CompletableFuture<?>)queueType.getMethod("submitRetrievalTask",long.class,byte.class)
                    .invoke(queue,outside,(byte)0);
                require(future.isCompletedExceptionally(),"DH still queued generation beyond the seam");
            } finally {
                ((java.util.concurrent.CompletableFuture<?>)queueType.getMethod("startClosingAsync",boolean.class,boolean.class).invoke(queue,true,true)).join();
                var field=queueType.getDeclaredField("DEBUG_RENDERER");field.setAccessible(true);var renderer=field.get(null);
                var config=Class.forName("com.seibel.distanthorizons.core.config.Config$Client$Advanced$Debugging$DebugWireframe").getField("showWorldGenQueue").get(null);
                for(var method:renderer.getClass().getMethods()) if(method.getName().equals("unregister") && method.getParameterCount()==2){method.invoke(renderer,queue,config);break;}
            }
            var clientType=Class.forName("com.seibel.distanthorizons.core.level.IDhClientLevel");
            Object client=java.lang.reflect.Proxy.newProxyInstance(loader,new Class[]{clientType},(p,m,a)->null);
            var sectionType=Class.forName("com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection");
            var constructor=sectionType.getConstructors()[0];
            var outsideSection=constructor.newInstance(6L|(4L<<8),null,client,null);
            var insideSection=constructor.newInstance(6L,null,client,null);
            try {
                sectionType.getMethod("setRenderingEnabled",boolean.class).invoke(outsideSection,true);
                sectionType.getMethod("setRenderingEnabled",boolean.class).invoke(insideSection,true);
                require(!(Boolean)sectionType.getMethod("getRenderingEnabled").invoke(outsideSection),"Outside DH section is still rendered");
                require((Boolean)sectionType.getMethod("getRenderingEnabled").invoke(insideSection),"Canonical DH section was hidden");
            } finally {((AutoCloseable)outsideSection).close();((AutoCloseable)insideSection).close();}
            if(net.neoforged.fml.ModList.get().isLoaded("cruising")) {
                var mask=Class.forName("io.github.astelev.cruising.client.SurfaceTerrainMask");
                require((Boolean)mask.getMethod("coveredColumn",net.minecraft.client.multiplayer.ClientLevel.class,int.class,int.class).invoke(null,mc.level,16,0),
                    "Cruising approximation still extends beyond the seam");
            }
            AroundPlanet.LOGGER.info("SEAM_DH_SMOKE PASS: outside generation cancelled, outside LOD hidden, canonical LOD retained, Cruising surface clipped");
        } catch(Exception e){throw new AssertionError("Native DH seam checks failed",e);}
    }
    private static void verifyOptionalSable(Minecraft mc) {
        if(!net.neoforged.fml.ModList.get().isLoaded("sable"))return;
        try {
            var type=Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            var container=type.getMethod("getContainer",net.minecraft.world.level.Level.class).invoke(null,mc.level);
            var origin=(org.joml.Vector2i)type.getMethod("getOrigin").invoke(container);
            int plot=(Integer)type.getMethod("getLogPlotSize").invoke(container);
            int x=origin.x<<plot,z=origin.y<<plot;
            require((Boolean)type.getMethod("inBounds",int.class,int.class).invoke(container,x,z),"Invalid Sable plot probe");
            require(io.github.astelev.around_planet.compat.ShipyardSpace.reserved(mc.level,x,z),"Sable private space was not recognized");
            require(!io.github.astelev.around_planet.seam.SeamGeometry.clipped(mc.level,x,z),"Planet clipping hides Sable private render space");
            AroundPlanet.LOGGER.info("SEAM_SABLE_SMOKE PASS: native private plot coordinates remain outside geographical clipping");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
