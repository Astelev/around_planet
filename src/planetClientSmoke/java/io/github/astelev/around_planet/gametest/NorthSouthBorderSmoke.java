package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Actual client movement must stop at each Z wall, while the X seam still transfers. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class NorthSouthBorderSmoke {
    private static int ticks,phase,started;private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.northSouthBorderSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("NS_BORDER server failure",failure);
        if(ticks>1800)throw new AssertionError("NS_BORDER timeout phase="+phase);
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || ticks<100)return;
        int bound=PlanetConfig.BOUNDARY_Z.getAsInt();
        if(phase==0) {
            require(mc.level.getWorldBorder().getMinZ()==-bound && mc.level.getWorldBorder().getMaxZ()==bound,"Client Z bounds did not synchronize");
            phase=1;started=ticks;teleport(mc,0,bound-2);
        } else if(phase==1 && ticks-started>30 && mc.player.getZ()>bound-4) {
            phase=2;started=ticks;
        } else if(phase==2) {
            mc.player.setDeltaMovement(new Vec3(0,0,0.4));
            if(ticks-started<40)return;
            require(mc.player.getZ()>bound-0.5 && mc.player.getBoundingBox().maxZ<=bound+1e-6,"South wall did not stop player: "+mc.player.position());
            require(PlanetBorders.warningDistance(mc.level.getWorldBorder(),mc.player)<1,"South wall has no warning");
            AroundPlanet.LOGGER.info("NS_BORDER_SOUTH PASS: actual creative movement stops at Z={} with warning",bound);
            phase=3;started=ticks;mc.player.setDeltaMovement(Vec3.ZERO);teleport(mc,0,-bound+2);
        } else if(phase==3 && ticks-started>30 && mc.player.getZ()< -bound+4) {
            phase=4;started=ticks;
        } else if(phase==4) {
            mc.player.setDeltaMovement(new Vec3(0,0,-0.4));
            if(ticks-started<40)return;
            require(mc.player.getZ()< -bound+0.5 && mc.player.getBoundingBox().minZ>=-bound-1e-6,"North wall did not stop player: "+mc.player.position());
            require(PlanetBorders.warningDistance(mc.level.getWorldBorder(),mc.player)<1,"North wall has no warning");
            AroundPlanet.LOGGER.info("NS_BORDER_NORTH PASS: actual creative movement stops at Z={}",-bound);
            phase=5;started=ticks;mc.player.setDeltaMovement(Vec3.ZERO);teleport(mc,PlanetConfig.BOUNDARY.getAsInt()+0.75,0);
        } else if(phase==5 && ticks-started>40 && mc.player.getX()<0) {
            require(Math.abs(mc.player.getX()+PlanetConfig.BOUNDARY.getAsInt()-0.75)<0.05,"X seam destination changed");
            AroundPlanet.LOGGER.info("NS_BORDER_EAST_WEST PASS: X seam still transfers without a Z displacement");
            phase=6;started=ticks;teleport(mc,0,0);
        } else if(phase==6 && ticks-started>30)mc.stop();
    }
    private static void teleport(Minecraft mc,double x,double z) {
        mc.getSingleplayerServer().execute(()->{try {
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.stopRiding();
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            for(int cx=(int)Math.floor(x/16)-2;cx<=(int)Math.floor(x/16)+2;cx++)for(int cz=(int)Math.floor(z/16)-2;cz<=(int)Math.floor(z/16)+2;cz++)p.serverLevel().getChunk(cx,cz);
            var topology=PlanetBorders.get(p.serverLevel().getWorldBorder());
            if(topology.outside(x)) {
                int target=(int)Math.floor(topology.canonicalX(x)/16);
                for(int cx=target-2;cx<=target+2;cx++)for(int cz=-2;cz<=2;cz++)p.serverLevel().getChunk(cx,cz);
            }
            p.connection.teleport(x,280,z,0,0);
        }catch(Throwable e){failure=e;}});
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
