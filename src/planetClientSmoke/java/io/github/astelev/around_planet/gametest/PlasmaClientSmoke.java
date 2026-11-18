package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.space.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import java.util.*;

/** Real client/server/network/render/cache regression; never included in the production artifact. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class PlasmaClientSmoke {
    private static int ticks,phase,start; private static volatile Throwable failure;
    private static volatile ServerSubLevel ship; private static volatile SpaceCells.Cell cell;
    private static volatile double speed=600; private static boolean busy;
    private static long builds,draws; private static volatile BlockPos changed;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.screen!=null&&mc.screen.getClass().getName().contains("pondus")&&mc.screen.getClass().getSimpleName().contains("Import")){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Skip")){b.onPress();return;}}
        if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
        if(++ticks>2400)throw new AssertionError("Plasma timeout phase="+phase);
        if(failure!=null)throw new AssertionError("Plasma smoke",failure);
        if(mc.player==null||mc.getSingleplayerServer()==null||ticks<100)return;
        if(phase==0){phase=1;start=ticks;mc.options.renderDistance().set(3);mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{try{setup(mc);}catch(Throwable t){failure=t;}});return;}
        if(ship==null||cell==null)return;
        mc.player.setYRot(145);mc.player.setXRot(3);
        if(!busy){busy=true;mc.getSingleplayerServer().execute(()->{try{hold(mc);}catch(Throwable t){failure=t;}finally{busy=false;}});}
        if(phase==1&&PlasmaRenderer.draws>30&&ticks-start>120){builds=PlasmaHullCache.builds;draws=PlasmaRenderer.draws;phase=2;start=ticks;return;}
        if(phase==2&&ticks-start>110){
            if(PlasmaHullCache.builds!=builds)throw new AssertionError("Hull rebuilt without block edits: "+builds+" -> "+PlasmaHullCache.builds);
            if(PlasmaRenderer.draws<=draws)throw new AssertionError("Plasma stopped drawing");
            shot(mc,"plasma-heated-alpha60.png");phase=3;start=ticks;
            mc.getSingleplayerServer().execute(()->{try{var p=ship.getSelfMassTracker().getCenterOfMass();changed=BlockPos.containing(p.x()+3,p.y(),p.z());ship.getLevel().setBlockAndUpdate(changed,Blocks.GOLD_BLOCK.defaultBlockState());}catch(Throwable t){failure=t;}});return;
        }
        if(phase==3&&changed!=null&&mc.level.getBlockState(changed).is(Blocks.GOLD_BLOCK)&&ticks-start>40){
            if(PlasmaHullCache.builds<=builds)throw new AssertionError("Edited hull was not invalidated by block packet");
            shot(mc,"plasma-updated-hull-alpha60.png");builds=PlasmaHullCache.builds;speed=150;phase=4;start=ticks;return;
        }
        if(phase==4&&ticks-start>40){draws=PlasmaRenderer.draws;phase=5;start=ticks;return;}
        if(phase==5&&ticks-start>60){
            if(PlasmaRenderer.draws!=draws)throw new AssertionError("Plasma below 200 m/s");
            if(PlasmaHullCache.builds!=builds)throw new AssertionError("Cold flight rebuilt the hull");
            shot(mc,"plasma-below-threshold-alpha60.png");
            AroundPlanet.LOGGER.info("PLASMA_CLIENT_PASS hotRender=true cold150mps=false eventInvalidation=true idleCacheStable=true hullBuilds={} triangles={}",PlasmaHullCache.builds,PlasmaRenderer.triangles);phase=6;mc.stop();
        }
    }
    private static void hold(Minecraft mc){
        var level=ship.getLevel();var data=SpaceOrbitData.get(level.getServer());var planet=new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry();
        var r=new SpaceVector(0,planet.radius()+2320,0);var v=PlanetDaylight.spin(level).velocity(r).add(new SpaceVector(0,-speed,0));
        data.put(cell.id(),new SpaceTrajectory(CelestialSystem.EARTH,level.getGameTime(),new SpaceState(r,v),9.81*planet.radius()*planet.radius()));
        var player=level.players().getFirst();player.teleportTo(12010,6147,14);player.setYRot(145);player.setXRot(3);
    }
    private static void setup(Minecraft mc){
        var level=mc.getSingleplayerServer().overworld();level.setDayTime(6000);level.setWeatherParameters(60000,0,false,false);PlanetConfig.ENABLED.set(true);
        var base=new BlockPos(30,300,30);var blocks=new ArrayList<BlockPos>();
        for(int y=0;y<7;y++) {int half=y<3?2:y<5?1:0;for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){
            var p=base.offset(x,y,z);blocks.add(p);level.setBlockAndUpdate(p,(y%2==0?Blocks.IRON_BLOCK:Blocks.COPPER_BLOCK).defaultBlockState());}}
        var c=SubLevelContainer.getContainer(level);var body=SubLevelAssemblyHelper.assembleBlocks(level,base,blocks,new BoundingBox3i(28,300,28,32,306,32));
        c.physicsSystem().getPipeline().teleport(body,new org.joml.Vector3d(12000,6144,0),new org.joml.Quaterniond().rotationZ(Math.PI));body.updateBoundingBox();body.updateLastPose();c.physicsSystem().getPipeline().resetVelocity(body);
        c.addForceLoadTicket(body,dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);
        cell=new SpaceCells.Cell(UUID.randomUUID(),0,12000,6144,0,30,2200,30,List.of(body.getUniqueId()));SpaceCells.get(level).add(level,cell);
        SpaceOrbitData.get(level.getServer()).navigate(cell.id(),new SpaceNavigation(new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry(),SpaceFrame.IDENTITY));
        ship=body;hold(mc);var player=level.players().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
