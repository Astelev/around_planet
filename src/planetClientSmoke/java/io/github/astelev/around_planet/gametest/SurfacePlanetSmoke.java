package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SurfacePlanetSmoke {
    private static int ticks,phase,start;private static volatile Throwable failure;private static boolean closing;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.screen!=null&&mc.screen.getClass().getName().contains("pondus")&&mc.screen.getClass().getSimpleName().contains("Import")){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Skip")){b.onPress();return;}}
        if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
        if(++ticks>3000)throw new AssertionError("Surface planet timeout phase="+phase);
        if(failure!=null)throw new AssertionError("Surface planet setup",failure);
        if(mc.player==null||mc.getSingleplayerServer()==null||ticks<120||!PlanetAtlasTexture.ready())return;
        mc.player.setYRot(0);mc.player.setXRot(18);
        if(phase==0){phase=1;start=ticks;mc.options.hideGui=true;mc.options.renderDistance().set(3);move(mc,1500,6000);return;}
        if(ticks-start<70)return;
        if(phase==1){shot(mc,"surface-planet-low-alpha57.png");phase=2;start=ticks;move(mc,1904,6000);}
        else if(phase==2){if(SurfaceSkyRenderer.planetBackdropFrames==0)throw new AssertionError("Surface planet pass never ran");shot(mc,"surface-planet-fade-alpha57.png");phase=3;start=ticks;move(mc,2190,6000);}
        else if(phase==3){shot(mc,"surface-planet-before-entry-alpha57.png");phase=4;start=ticks;move(mc,2190,18000);}
        else if(phase==4&&!closing){shot(mc,"surface-planet-night-alpha57.png");closing=true;AroundPlanet.LOGGER.info("SURFACE_PLANET_PASS atlas={} backdropFrames={} skyFrames={} activeSpace={}",PlanetAtlasTexture.ready(),SurfaceSkyRenderer.planetBackdropFrames,SurfaceRenderState.skyFrames,SpaceRenderView.active());if(SpaceRenderView.active())throw new AssertionError("Fixture teleported into space instead of rendering surface sky");mc.stop();}
    }
    private static void move(Minecraft mc,int y,long day){mc.getSingleplayerServer().execute(()->{try{
        PlanetConfig.ENABLED.set(true);PlanetConfig.SURFACE_SKY.set(true);PlanetConfig.SURFACE_CURVE.set(true);PlanetConfig.DAY_LENGTH_MINUTES.set(0.0);
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(0,y,0);p.serverLevel().setDayTime(day);p.serverLevel().setWeatherParameters(60000,0,false,false);
    }catch(Throwable t){failure=t;}});}
    private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
