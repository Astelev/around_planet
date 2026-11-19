package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SurfaceVisualSmoke {
    private static int ticks,phase,start;private static Boolean clouds;private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("around_planet.surfaceSmoke"))return;var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SURFACE_SMOKE server",failure);
        if(ticks>1800)throw new AssertionError("SURFACE_SMOKE timeout phase="+phase);
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||ticks<100)return;
        if(phase==0){phase=1;start=ticks;mc.options.hideGui=true;mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);clouds(false);server(mc,()->{PlanetConfig.ENABLED.set(true);PlanetConfig.SURFACE_CURVE.set(true);PlanetConfig.SURFACE_SKY.set(true);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(0,300,0);p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(60000,0,false,false);});}
        else if(phase==1){look(mc,SurfaceVisualMath.sun(mc.level.getSunAngle(0)));if(ticks-start>80){require(SurfaceRenderState.skyFrames>0 && SurfaceRenderState.curvedDraws>0,"Surface sky/geometry hooks did not run");shot(mc,"surface-round-sun-alpha11.png");phase=2;start=ticks;server(mc,()->mc.getSingleplayerServer().overworld().setDayTime(18000));}}
        else if(phase==2){look(mc,SurfaceVisualMath.sun(mc.level.getSunAngle(0)).scale(-1));if(ticks-start>60){shot(mc,"surface-round-moon-alpha11.png");phase=3;start=ticks;server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.serverLevel().setDayTime(6000);p.teleportTo(0,1700,0);});mc.player.setYRot(0);mc.player.setXRot(20);}}
        else if(phase==3 && ticks-start>100){fallbackDiagnostic(true);require(SurfaceRenderState.darkness(1700)>0 && SurfaceRenderState.darkness(1700)<1,"Altitude sky fade not partial");require(!net.neoforged.fml.ModList.get().isLoaded("cruising") || !net.neoforged.fml.ModList.get().isLoaded("distanthorizons") || SurfaceRenderState.fallbackDraws>0,"Cruising fallback shader never received active curvature");shot(mc,"surface-curved-horizon-alpha12.png");phase=4;start=ticks;server(mc,()->PlanetConfig.SURFACE_CURVE.set(false));}
        else if(phase==4 && ticks-start>40){fallbackDiagnostic(false);shot(mc,"surface-flat-comparison-alpha12.png");phase=5;start=ticks;server(mc,()->{PlanetConfig.SURFACE_CURVE.set(true);mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().teleportTo(0,2100,0);});mc.player.setXRot(-20);}
        else if(phase==5 && ticks-start>60){shot(mc,"surface-high-dark-sky-alpha11.png");clouds(clouds==null?true:clouds);require(SurfaceRenderState.darkness(2100)>.8,"High sky did not darken toward space");AroundPlanet.LOGGER.info("SURFACE_VISUAL PASS: shared round Sun/Moon, day/night, 1600+ altitude fade, curvature draws={} DH draws={} Cruising fallback draws={}",SurfaceRenderState.curvedDraws,SurfaceRenderState.dhDraws,SurfaceRenderState.fallbackDraws);phase=6;start=ticks;server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.serverLevel().setDayTime(6000);p.teleportTo(PlanetConfig.PLANET_SIZE.get()/2.0-32,300,0);});}
        else if(phase==6 && ticks-start>80){require(Math.abs(mc.level.getSunAngle(0)-Math.PI)<.05,"Opposite longitude did not receive local midnight");require(mc.level.getSkyDarken()>=10 && mc.level.getSkyDarken(0)<.3,"Client lightmap still uses global noon at opposite longitude");look(mc,SurfaceVisualMath.sun(mc.level.getSunAngle(0)).scale(-1));shot(mc,"surface-local-midnight-alpha13.png");AroundPlanet.LOGGER.info("LOCAL_DAYLIGHT PASS: global noon gives local midnight and dark lightmap at opposite longitude");mc.options.hideGui=false;mc.stop();phase=7;}
    }
    private static void fallbackDiagnostic(boolean curved){
        if(!net.neoforged.fml.ModList.get().isLoaded("cruising") || !net.neoforged.fml.ModList.get().isLoaded("distanthorizons"))return;
        try{var type=Class.forName("io.github.astelev.cruising.client.FallbackPlaneRenderer");var f=type.getDeclaredField("shader");f.setAccessible(true);var shader=(net.minecraft.client.renderer.ShaderInstance)f.get(null);
            int location=org.lwjgl.opengl.GL20.glGetUniformLocation(shader.getId(),"apCurveRadius");require(location>=0,"Cruising fallback lost curvature after shader recompilation");
            float radius=org.lwjgl.opengl.GL20.glGetUniformf(shader.getId(),location);
            require(curved?Math.abs(radius-SurfaceRenderState.terrainRadius())<.01:radius==0,"Cruising fallback curvature toggle/radius incorrect: "+radius);
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @SuppressWarnings("unchecked") private static void clouds(boolean value){if(!net.neoforged.fml.ModList.get().isLoaded("simpleclouds"))return;try{var c=Class.forName("dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig").getField("CLIENT").get(null);var config=(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Boolean>)c.getClass().getField("renderClouds").get(c);if(clouds==null)clouds=config.get();config.set(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static void look(Minecraft mc,SpaceVector v){mc.player.setYRot((float)Math.toDegrees(Math.atan2(-v.x(),v.z())));mc.player.setXRot((float)Math.toDegrees(Math.atan2(-v.y(),Math.hypot(v.x(),v.z()))));}
    private static void server(Minecraft mc,Runnable r){mc.getSingleplayerServer().execute(()->{try{r.run();}catch(Throwable t){failure=t;}});}
    private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,net.neoforged.fml.ModList.get().isLoaded("sodium")?name:name.replace("surface-","surface-vanilla-"),mc.getMainRenderTarget(),message->{});}
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
}
