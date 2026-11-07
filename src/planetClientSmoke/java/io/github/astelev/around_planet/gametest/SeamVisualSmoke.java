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
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static Object beforeReload;
    private static Object retained,departure;
    private static Object dhPrimary,dhOpposite;
    private static boolean dhFarVerified;
    private static long departureDraws;
    private static long drawsBefore,awaySince;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.visualSmoke")) return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null) throw new AssertionError("SEAM_VISUAL_SMOKE fixture failure",failure);
        if(ticks>4800) throw new AssertionError("SEAM_VISUAL_SMOKE timeout, phase="+phase+" images="+SeamImages.size());
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
                    for(int x=245;x<=246;x++)for(int y=80;y<=84;y++)for(int z=21;z<=27;z++)
                        level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.LIME_CONCRETE.defaultBlockState());
                    if(net.neoforged.fml.ModList.get().isLoaded("distanthorizons")) {
                        foreground(level,Blocks.AIR);
                        for(int x=-64;x<=-57;x++)for(int z=40;z<=71;z++)for(int y=80;y<=127;y++)
                            level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.RED_CONCRETE.defaultBlockState());
                        DhSeamFixture.publish(level);
                    }
                    level.setBlockAndUpdate(new BlockPos(252,82,18),Blocks.WHITE_CONCRETE.defaultBlockState());
                    // Deliberately different real terrain beyond each seam must never be the rendered source.
                    wall(level,266,Blocks.BLUE_CONCRETE);wall(level,-267,Blocks.BLUE_CONCRETE);
                    level.setBlockAndUpdate(new BlockPos(-245,80,13),Blocks.WATER.defaultBlockState());
                    p.connection.teleport(210.5,82.5,8.5,-90,0);prepared=true;
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==1 && prepared && ticks-started>60) {
            if(Math.abs(mc.player.getX()-210.5)>0.05 || Math.abs(mc.player.getZ()-8.5)>0.05
                    || Math.abs(mc.gameRenderer.getMainCamera().getPosition().z-8.5)>0.05)return;
            var image=SeamImages.get(mc.level,16,0);
            if(image==null || !image.getBlockState(new BlockPos(266,82,8)).is(Blocks.RED_CONCRETE))return;
            var region=new RenderRegionCache().createRegion(mc.level,SectionPos.of(16,5,0),false);
            require(region.getBlockState(new BlockPos(266,82,8)).is(Blocks.RED_CONCRETE),"Render region did not use opposite coast");
            require(!mc.level.getBlockState(new BlockPos(266,82,8)).is(Blocks.RED_CONCRETE),"Visual snapshot leaked into gameplay cache");
            require(!image.getFluidState(new BlockPos(267,80,13)).isEmpty(),"Water snapshot missing");
            boolean imageReady=compiled(mc,new BlockPos(266,82,8)),edgeReady=compiled(mc,new BlockPos(252,82,18));
            if(ticks%100==0) AroundPlanet.LOGGER.info("SEAM visual compile: image={}, edge={}, view={}",imageReady,edgeReady,mc.options.getEffectiveRenderDistance());
            if(!imageReady || !edgeReady || !pixel(mc,0))return;
            if(!io.github.astelev.around_planet.client.SeamMeshCache.settled(-16,5,0)
                    || !io.github.astelev.around_planet.client.SeamMeshCache.settled(15,5,1) || ticks-started<140)return;
            if(net.neoforged.fml.ModList.get().isLoaded("distanthorizons")) {
                var dh=io.github.astelev.around_planet.compat.DhSeamViews.snapshot();
                if(ticks%100==0)AroundPlanet.LOGGER.info("DH_SEAM_SMOKE waiting: {}",dh);
                if(dh==null || dh.uploaded()==0 || io.github.astelev.around_planet.compat.DhSeamRenderer.opaqueDraws==0)return;
                if(!dhFarVerified) {
                    phase=11;started=ticks;
                    mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                            .connection.teleport(210.5,100,56,-90,0));
                    return;
                }
                dhPrimary=dh.primary();dhOpposite=dh.opposite();
            }
            departure=io.github.astelev.around_planet.client.SeamMeshCache.identity(15,5,1);
            departureDraws=io.github.astelev.around_planet.client.SeamMeshCache.drawCount(15,5,1);
            retained=io.github.astelev.around_planet.client.SeamMeshCache.identity(-16,5,0);
            drawsBefore=io.github.astelev.around_planet.client.SeamMeshCache.draws;
            AroundPlanet.LOGGER.info("SEAM_VISUAL_SMOKE east PASS: opposite red wall beyond border, native compiled GPU buffer, water and isolated gameplay cache");
            phase=15;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                wall(p.serverLevel(),245,Blocks.LIME_CONCRETE);
                // Delay just this outgoing column: cached geometry must fill even a compiled empty padding section.
                p.serverLevel().getChunk(15,1).setLightCorrect(false);
                p.connection.teleport(-252,82.5,8.5,-90,0);
            });
        } else if(phase==11) {
            if(ticks-started<40 || Math.abs(mc.player.getY()-100)>0.05 || Math.abs(mc.player.getZ()-56)>0.05
                    || Math.abs(mc.gameRenderer.getMainCamera().getPosition().z-56)>0.05)return;
            if(ticks%100==0)AroundPlanet.LOGGER.info("DH_SEAM_FAR waiting: {} passes={} opaque={}",
                    io.github.astelev.around_planet.compat.DhSeamViews.snapshot(),
                    io.github.astelev.around_planet.compat.DhSeamRenderer.passes,
                    io.github.astelev.around_planet.compat.DhSeamRenderer.opaqueDraws);
            if(!pixel(mc,0))return;
            require(SeamImages.get(mc.level,28,3)==null,"Far LOD fixture accidentally entered the image strip");
            require(!mc.level.getBlockState(new BlockPos(448,105,56)).is(Blocks.RED_CONCRETE),"Far preview leaked into gameplay");
            AroundPlanet.LOGGER.info("DH_SEAM_FAR_GPU PASS: opposite red coast visible beyond real image columns and vanilla view distance");
            io.github.astelev.around_planet.PlanetConfig.DH_SEAM.set(false);phase=111;started=ticks;
        } else if(phase==111 && ticks-started>20) {
            boolean hidden=!pixel(mc,0);
            io.github.astelev.around_planet.PlanetConfig.DH_SEAM.set(true);
            require(hidden,"Far red pixel did not depend on translated DH rendering");
            phase=112;started=ticks;
        } else if(phase==112) {
            if(ticks%100==0)AroundPlanet.LOGGER.info("DH_SEAM_REENABLE waiting: enabled={} {} passes={} opaque={}",
                    io.github.astelev.around_planet.PlanetConfig.DH_SEAM.getAsBoolean(),
                    io.github.astelev.around_planet.compat.DhSeamViews.snapshot(),
                    io.github.astelev.around_planet.compat.DhSeamRenderer.passes,
                    io.github.astelev.around_planet.compat.DhSeamRenderer.opaqueDraws);
            if(ticks-started<20 || !pixel(mc,0))return;
            dhFarVerified=true;DhSeamGpuRegression.verify();
            AroundPlanet.LOGGER.info("DH_SEAM_TOGGLE_GPU PASS: far image disappears when disabled and returns when enabled");
            phase=113;started=ticks;
            mc.getSingleplayerServer().execute(()->foreground(mc.getSingleplayerServer().overworld(),Blocks.LIME_CONCRETE));
        } else if(phase==113) {
            if(ticks-started<20 || !compiled(mc,new BlockPos(239,104,56)) || !pixel(mc,1))return;
            require(mc.level.getBlockState(new BlockPos(239,104,56)).is(Blocks.LIME_CONCRETE),"Depth control did not use a real native chunk");
            phase=114;started=ticks;
            mc.getSingleplayerServer().execute(()->foreground(mc.getSingleplayerServer().overworld(),Blocks.AIR));
        } else if(phase==114) {
            if(ticks-started<20 || !pixel(mc,0))return;
            AroundPlanet.LOGGER.info("DH_SEAM_DEPTH_GPU PASS: native foreground hides translated LOD; removing it reveals the same far coast");
            phase=1;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                    .connection.teleport(210.5,82.5,8.5,-90,0));
        } else if(phase==15 && ticks-started>40) {
            require(io.github.astelev.around_planet.client.SeamMeshCache.identity(-16,5,0)==retained,
                    "Canonical GPU mesh was recreated at crossing");
            require(io.github.astelev.around_planet.client.SeamMeshCache.draws>drawsBefore,
                    "Retained geometry never covered destination meshing");
            require(pixel(mc,0),"Destination red coast missing after crossing");
            AroundPlanet.LOGGER.info("SEAM_RETAINED_MESH PASS: same canonical GPU mesh survived crossing and drew during native meshing");
            if(dhOpposite!=null) {
                var dh=io.github.astelev.around_planet.compat.DhSeamViews.snapshot();
                require(dh!=null && dh.primary()==dhOpposite && dh.opposite()==dhPrimary,"DH trees were rebuilt at crossing");
                AroundPlanet.LOGGER.info("DH_SEAM_RETAIN PASS: opposite tree promoted and departure tree retained");
            }
            phase=16;started=ticks;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                    .connection.teleport(-247.5,82.5,24.5,90,0));
        } else if(phase==16 && ticks-started>240) {
            require(SeamImages.get(mc.level,-17,1)==null,"Delayed outgoing image arrived before cache check");
            require(io.github.astelev.around_planet.client.SeamMeshCache.identity(15,5,1)==departure,"Departure geometry was recreated");
            if(!pixel(mc,1))return;
            require(io.github.astelev.around_planet.client.SeamMeshCache.drawCount(15,5,1)>departureDraws,
                    "Departure mesh was not drawn into its periodic image");
            AroundPlanet.LOGGER.info("SEAM_DEPARTURE_GPU PASS: same outgoing mesh remains visible beyond the old 10-second expiry, with no native image data");
            phase=2;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.serverLevel().getChunk(15,1).setLightCorrect(true);
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
            verifyEmptyMeshCoverage();verifyOptionalDh(mc);
            verifyOptionalSable(mc);
            AroundPlanet.LOGGER.info("SEAM_VISUAL_SMOKE PASS: both directions, real GPU pixels, block updates, water, lighting, cache eviction and no gameplay aliases");
            if(!io.github.astelev.around_planet.client.SeamMeshCache.settled(15,5,0))return;
            beforeReload=io.github.astelev.around_planet.client.SeamMeshCache.identity(15,5,0);
            phase=35;started=ticks;reload=mc.reloadResourcePacks();
        } else if(phase==35 && reload.isDone()) {
            reload.join();
            if(!io.github.astelev.around_planet.client.SeamMeshCache.settled(15,5,0) || !pixel(mc,2))return;
            require(io.github.astelev.around_planet.client.SeamMeshCache.identity(15,5,0)!=beforeReload,
                    "Resource reload retained stale terrain buffers");
            AroundPlanet.LOGGER.info("SEAM_MESH_RELOAD PASS: old buffers disposed, images rebuilt with current resources");
            phase=4;started=ticks;awaySince=mc.level.getGameTime();
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.connection.teleport(0,200,0,0,0);
            });
        } else if(phase==4 && mc.level.getGameTime()-awaySince>240) {
            require(io.github.astelev.around_planet.client.SeamMeshCache.size()==0,"Old coast geometry did not expire");
            AroundPlanet.LOGGER.info("SEAM_MESH_EXPIRY PASS: leaving both coasts releases retained GPU geometry");
            phase=5;
            // Continue with the existing real-player/vehicle transition regression in the same warmed client.
            System.setProperty("around_planet.visualSmoke","false");
        }
    }
    private static void foreground(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.block.Block block) {
        for(int x=239;x<=240;x++)for(int y=96;y<=110;y++)for(int z=48;z<=64;z++)
            level.setBlockAndUpdate(new BlockPos(x,y,z),block.defaultBlockState());
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
    @SuppressWarnings("unchecked")
    private static void verifyEmptyMeshCoverage() {
        try {
            var type=io.github.astelev.around_planet.client.SeamMeshCache.class;
            var sf=type.getDeclaredField("state");sf.setAccessible(true);var state=sf.get(null);require(state!=null,"Missing mesh state");
            var mf=state.getClass().getDeclaredField("meshes");mf.setAccessible(true);var meshes=(java.util.Map<Long,Object>)mf.get(state);
            var meshClass=Class.forName(type.getName()+"$Mesh");var constructor=meshClass.getDeclaredConstructor();constructor.setAccessible(true);
            long key=SectionPos.asLong(15,5,0);var previous=meshes.put(key,constructor.newInstance());
            try{require(!io.github.astelev.around_planet.client.SeamMeshCache.hasMesh(15,5,0),"Empty retained mesh masked the LOD");}
            finally{if(previous==null)meshes.remove(key);else meshes.put(key,previous);}
            AroundPlanet.LOGGER.info("SEAM_EMPTY_MESH PASS: empty cache entry cannot claim DH coverage");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
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
