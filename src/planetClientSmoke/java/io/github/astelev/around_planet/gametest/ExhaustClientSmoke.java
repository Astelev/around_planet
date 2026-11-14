package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.client.PropulsionExhaustRenderer;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class ExhaustClientSmoke {
    private static int ticks;private static long offFrames;private static float flowTime;private static int cloudPlane;
    private static final java.util.Map<net.neoforged.neoforge.common.ModConfigSpec.ConfigValue,Object> cloudConfig=new java.util.HashMap<>();
    private static final java.util.Map<net.neoforged.neoforge.common.ModConfigSpec.DoubleValue,Double> fogConfig=new java.util.HashMap<>();
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("around_planet.exhaustSmoke"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),2.5,200,5,java.util.Set.of(),180,20);var l=p.serverLevel();l.setDayTime(6000);
            l.setBlockAndUpdate(new BlockPos(0,200,0),PropulsionRegistry.ENGINE.get().defaultBlockState());l.setBlockAndUpdate(new BlockPos(-1,200,0),Blocks.REDSTONE_BLOCK.defaultBlockState());l.setBlockAndUpdate(new BlockPos(2,200,0),PropulsionRegistry.RSU.get().defaultBlockState());l.setBlockAndUpdate(new BlockPos(4,200,0),PropulsionRegistry.ENGINE.get().defaultBlockState().setValue(OrbitalSensorBlock.FACING,Direction.EAST));l.setBlockAndUpdate(new BlockPos(4,199,0),Blocks.REDSTONE_BLOCK.defaultBlockState());for(int x:new int[]{0,4})((PropulsionBlockEntity)l.getBlockEntity(new BlockPos(x,200,0))).configure(10000);
            var rsu=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0));rsu.configure(1000);for(int i=0;i<8;i++)rsu.frequency(i,net.minecraft.world.item.ItemStack.EMPTY);rsu.receiveSignal(0,15);rsu.receiveSignal(1,8);rsu.receiveSignal(3,3);
        });
        if(ticks==70)mc.options.hideGui=true;
        if(ticks==100){if(io.github.astelev.around_planet.client.DeferredExhaust.draws==0)throw new AssertionError("Deferred exhaust pass never drew");if(PropulsionExhaustRenderer.shader==null)throw new AssertionError("Gas-flow shader did not load");flowTime=PropulsionExhaustRenderer.lastFlowTime;if(PropulsionExhaustRenderer.frames==0||PropulsionExhaustRenderer.quads<10)throw new AssertionError("Powered core/cone exhaust did not render");var rsu=(PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(2,200,0));if(rsu.signal(0)!=15||rsu.signal(1)!=8||rsu.signal(2)!=0||rsu.signal(3)!=3)throw new AssertionError("Client nozzle signals are incorrect");snap(mc,"exhaust-powered-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_ON PASS: two-section engine/RSU gas plumes, multiple mounting axes, independently synchronized nozzles");}
        if(ticks==135){if(PropulsionExhaustRenderer.lastFlowTime<=flowTime)throw new AssertionError("Gas-flow animation time did not advance");snap(mc,"exhaust-flow-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_FLOW PASS: shared gas shader loaded and outward flow time advances");}
        if(ticks==140)mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().overworld();for(int x:new int[]{0,4})((PropulsionBlockEntity)l.getBlockEntity(new BlockPos(x,200,0))).configure(100);((PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0))).configure(10);});
        if(ticks==175){var engine=(PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(0,200,0));if(engine.thrust()!=100)throw new AssertionError("Low-thrust state did not arrive");snap(mc,"exhaust-low-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_LOW PASS: shortened active cores with minimum-length expanding gas cones");}
        if(ticks==190)mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().overworld();l.setBlockAndUpdate(new BlockPos(-1,200,0),Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(new BlockPos(4,199,0),Blocks.AIR.defaultBlockState());var rsu=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0));for(int i=0;i<4;i++)rsu.receiveSignal(i,0);});
        if(ticks==215)offFrames=PropulsionExhaustRenderer.frames;
        if(ticks==235){if(PropulsionExhaustRenderer.frames!=offFrames)throw new AssertionError("Unpowered exhaust kept rendering");snap(mc,"exhaust-off-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_OFF PASS: inactive engines allocate no jet geometry");}
        if(ticks==250)mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().overworld();var p=new BlockPos(2,200,0);l.setBlockAndUpdate(p,l.getBlockState(p).setValue(OrbitalSensorBlock.FACING,Direction.WEST));var be=(PropulsionBlockEntity)l.getBlockEntity(p);be.configure(1000);be.receiveSignal(2,15);});
        if(ticks==280){if(PropulsionExhaustRenderer.frames<=offFrames)throw new AssertionError("Wall-mounted exhaust did not resume");snap(mc,"exhaust-wall-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_WALL PASS: rotated gas plume resumes without particles");}
        if(ticks==300&&!depthSmoke()){mc.options.hideGui=false;mc.stop();}
        if(depthSmoke())depthTests(mc);

    }

    private static boolean depthSmoke(){return Boolean.getBoolean("around_planet.exhaustDepthSmoke");}
    private static void depthTests(Minecraft mc){
        if(ticks==320)mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().overworld();l.setBlockAndUpdate(new BlockPos(4,199,0),Blocks.REDSTONE_BLOCK.defaultBlockState());((PropulsionBlockEntity)l.getBlockEntity(new BlockPos(4,200,0))).configure(10000);var rsu=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0));for(int i=0;i<4;i++)rsu.receiveSignal(i,0);});
        if(ticks==340){var be=(PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(4,200,0));AroundPlanet.LOGGER.info("CLOUD_FIXTURE engine thrust={} signal={} draws={}",be.thrust(),be.signal(0),io.github.astelev.around_planet.client.DeferredExhaust.draws);if(be.signal(0)==0)throw new AssertionError("Cloud fixture engine not powered");snap(mc,"exhaust-cloud-baseline-alpha23.png");}
        if(ticks==350)cloudPlane=1;
        if(ticks==380){checkCloudPixels(mc,true);snap(mc,"exhaust-far-cloud-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_FAR_CLOUD: synthetic opaque cloud behind the nozzle");}
        if(ticks==390)cloudPlane=2;
        if(ticks==420){if(io.github.astelev.around_planet.client.ExhaustCloudDepth.boundFrames==0)throw new AssertionError("Cloud-depth uniforms were not bound");checkCloudPixels(mc,false);snap(mc,"exhaust-near-cloud-alpha23.png");AroundPlanet.LOGGER.info("EXHAUST_NEAR_CLOUD: synthetic opaque cloud in front of the nozzle");cloudPlane=0;}
        if(ticks==440){try{var c=Class.forName("net.aste.aeroworks.client.CloudFogConfig");String[] keys={"DENSITY_OFFSET","EDGE_DENSITY_MARGIN","TRANSITION_TICKS"};double[] values={1,1,0};for(int i=0;i<keys.length;i++){var entry=(net.neoforged.neoforge.common.ModConfigSpec.DoubleValue)c.getField(keys[i]).get(null);fogConfig.put(entry,entry.get());entry.set(values[i]);}var config=Class.forName("dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig");var single=Class.forName("dev.nonamecrackers2.simpleclouds.api.common.cloud.CloudMode").getField("SINGLE").get(null);for(String side:new String[]{"CLIENT","SERVER"}){var cfg=config.getField(side).get(null);for(String name:new String[]{"cloudMode","singleModeCloudType"}){var entry=(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue)cfg.getClass().getField(name).get(cfg);cloudConfig.put(entry,entry.get());entry.set(name.equals("cloudMode")?single:"simpleclouds:nimbostratus");}}}catch(ReflectiveOperationException e){throw new AssertionError(e);}mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.serverLevel().setDayTime(18000);p.teleportTo(p.serverLevel(),2.5,638.38,5,java.util.Set.of(),180,-60);});}
        if(ticks==600){try{float amount=((Number)Class.forName("net.aste.aeroworks.client.CloudFogEvents").getMethod("strength").invoke(null)).floatValue();if(amount<.99)throw new AssertionError("Fog fixture did not become dense: "+amount);if(!io.github.astelev.around_planet.client.AeroFogBridge.hasWorldFog())throw new AssertionError("AeroWorks world-fog queue not detected");try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){int p=image.getPixelRGBA(image.getWidth()/2,image.getHeight()/4);int r=p&255,g=(p>>8)&255,b=(p>>16)&255;if(Math.abs(r-g)>3||Math.abs(g-b)>3)throw new AssertionError("Sky leaked through dense fog: "+r+","+g+","+b);}snap(mc,"sky-dense-fog-alpha23.png");AroundPlanet.LOGGER.info("SKY_FOG PASS: actual AeroWorks dense fog covers custom sky and stars");}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
        if(ticks==620){fogConfig.forEach((k,v)->k.set(v));cloudConfig.forEach((k,v)->k.set(v));mc.options.hideGui=false;mc.stop();}
    }
    @SubscribeEvent public static void cloudPlane(net.neoforged.neoforge.client.event.RenderLevelStageEvent event){
        if(!depthSmoke()||cloudPlane==0||event.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER)return;
        var mc=Minecraft.getInstance();try{var type=Class.forName("dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer");var renderer=type.getMethod("getInstance").invoke(null);var target=(com.mojang.blaze3d.pipeline.RenderTarget)type.getMethod("getCloudTarget").invoke(renderer);var gl=com.mojang.blaze3d.systems.RenderSystem.getShaderColor();boolean mask=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);float[] clear=new float[4];org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_COLOR_CLEAR_VALUE,clear);target.bindWrite(true);com.mojang.blaze3d.systems.RenderSystem.depthMask(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,0);org.lwjgl.opengl.GL11.glClearDepth(1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glScissor((int)(target.width*.35),(int)(target.height*.35),(int)(target.width*.3),(int)(target.height*.3));org.lwjgl.opengl.GL11.glClearColor(.3f,.3f,.3f,1);org.lwjgl.opengl.GL11.glClearDepth(cloudPlane==2?.1:1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);var main=mc.getMainRenderTarget();main.bindWrite(true);org.lwjgl.opengl.GL11.glScissor((int)(main.width*.35),(int)(main.height*.35),(int)(main.width*.3),(int)(main.height*.3));org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearDepth(1);org.lwjgl.opengl.GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);com.mojang.blaze3d.systems.RenderSystem.depthMask(mask);io.github.astelev.around_planet.client.ExhaustCloudDepth.capture(event.getProjectionMatrix());}catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void checkCloudPixels(Minecraft mc,boolean far){
        try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int bright=0;for(int y=(int)(image.getHeight()*.39);y<image.getHeight()*.48;y++)for(int x=(int)(image.getWidth()*.38);x<image.getWidth()*.60;x++){
                int p=image.getPixelRGBA(x,y);if((p&255)+((p>>8)&255)+((p>>16)&255)>500)bright++;
            }
            AroundPlanet.LOGGER.info("CLOUD_PIXELS far={} bright={}",far,bright);
            if(far?bright<10:bright>0)throw new AssertionError("Incorrect exhaust/cloud ordering: far="+far+" bright="+bright);
        }
    }
    private static void snap(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
}
