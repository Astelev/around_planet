package io.github.astelev.around_planet.gametest;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.ShipRenderScope;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.UUID;

/** Exercises native plot packets, actual ship GPU buffers, both shores, block edits and tracked removal. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class ShipSeamSmoke {
    private static int ticks,phase,started;
    private static volatile Throwable failure;
    private static volatile UUID id;
    private static ServerSubLevel serverShip;
    private static Object clientData;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.shipSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SHIP_SEAM fixture failure",failure);
        if(ticks>2400)throw new AssertionError("SHIP_SEAM timeout phase="+phase+" ships="+(mc.level==null?0:SubLevelContainer.getContainer(mc.level).getAllSubLevels().size()));
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);mc.player.setNoGravity(true);
        if(phase==0) {
            phase=1;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();var container=SubLevelContainer.getContainer(level);
                    io.github.astelev.around_planet.PlanetConfig.SHIP_TRANSFER.set(false);
                    p.stopRiding();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
                    level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                    for(int x:new int[]{-16,-15,15,16})for(int z=-1;z<=1;z++)level.getChunk(x,z);
                    for(var old:new ArrayList<>(container.getAllSubLevels()))if(old.getName()!=null && old.getName().startsWith("around_planet_render_test"))container.removeSubLevel(old,SubLevelRemovalReason.REMOVED);
                    var positions=new ArrayList<BlockPos>();var anchor=new BlockPos(-253,219,-1);
                    for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++) {
                        var pos=anchor.offset(x,y,z);level.setBlockAndUpdate(pos,Blocks.LIME_CONCRETE.defaultBlockState());positions.add(pos);
                    }
                    serverShip=SubLevelAssemblyHelper.assembleBlocks(level,anchor,positions,new BoundingBox3i(-253,219,-1,-251,221,1));
                    serverShip.setName("around_planet_render_test");id=serverShip.getUniqueId();
                    container.addForceLoadTicket(serverShip,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
                    container.physicsSystem().getPipeline().teleport(serverShip,new Vector3d(-252,220,0),serverShip.logicalPose().orientation());
                    serverShip.updateBoundingBox();serverShip.updateLastPose();container.physicsSystem().setPaused(true);
                    p.connection.teleport(246,219,0,-90,0);
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==1 && ticks-started>60 && image(mc)!=null) {
            var ship=image(mc);
            require(ship.logicalPose().position().x<0,"Image changed native logical pose");
            require(ship.renderPose().position().x()<0,"Translated render pose escaped rendering scope");
            ShipRenderScope.enter();
            try {
                require(ship.renderPose().position().x()>256,"Periodic render pose was not placed behind east seam");
                require(ship.boundingBox().minX()>256,"Culling bounds did not follow the image");
            }finally{ShipRenderScope.exit();}
            if(!pixel(mc,false))return;
            clientData=ship.getRenderData();phase=2;started=ticks;
            AroundPlanet.LOGGER.info("SHIP_SEAM_EAST_GPU PASS: native tracked plot and GPU mesh visible across east seam, logical and unscoped poses unchanged");
            mc.getSingleplayerServer().execute(()->{
                try {
                    var bounds=serverShip.getPlot().getBoundingBox();var level=serverShip.getLevel();
                    for(int x=bounds.minX();x<=bounds.maxX();x++)for(int y=bounds.minY();y<=bounds.maxY();y++)for(int z=bounds.minZ();z<=bounds.maxZ();z++)
                        level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.RED_CONCRETE.defaultBlockState());
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==2 && ticks-started>30 && image(mc)!=null && pixel(mc,true)) {
            require(image(mc).getRenderData()==clientData,"Block update replaced the ship render identity");
            phase=3;started=ticks;
            AroundPlanet.LOGGER.info("SHIP_SEAM_UPDATE_GPU PASS: native plot block updates rebuild the same ship's GPU geometry");
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var system=SubLevelContainer.getContainer(p.serverLevel()).physicsSystem();
                    system.getPipeline().teleport(serverShip,new Vector3d(256.5,220,0),serverShip.logicalPose().orientation());serverShip.updateBoundingBox();serverShip.updateLastPose();
                    p.connection.teleport(-246,219,0,90,0);
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==3 && ticks-started>60 && mc.player.getX()<0 && image(mc)!=null && image(mc).logicalPose().position().x>0 && pixel(mc,true)) {
            require(SubLevelContainer.getContainer(mc.level).getAllSubLevels().stream().filter(s->s.getUniqueId().equals(id)).count()==1,"Duplicate physics image object");
            phase=4;started=ticks;
            AroundPlanet.LOGGER.info("SHIP_SEAM_WEST_GPU PASS: same tracked ship, including an outboard center, visible across west seam without duplicate sublevel/rigid body");
            mc.getSingleplayerServer().execute(()->{
                try {
                    var container=SubLevelContainer.getContainer(serverShip.getLevel());
                    container.removeForceLoadTicket(serverShip,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);container.removeSubLevel(serverShip,SubLevelRemovalReason.REMOVED);container.physicsSystem().setPaused(false);
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==4 && ticks-started>30 && image(mc)==null) {
            phase=5;started=ticks;AroundPlanet.LOGGER.info("SHIP_SEAM_REMOVE PASS: native removal discards image and GPU resources");
        } else if(phase==5 && ticks-started>40)mc.stop();
    }
    private static ClientSubLevel image(Minecraft mc) {
        return id==null?null:(ClientSubLevel)SubLevelContainer.getContainer(mc.level).getSubLevel(id);
    }
    private static boolean pixel(Minecraft mc,boolean red) {
        mc.getMainRenderTarget().bindWrite(false);var bytes=BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(mc.getWindow().getWidth()/2+10,mc.getWindow().getHeight()/2-10,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
        int r=bytes.get(0)&255,g=bytes.get(1)&255,b=bytes.get(2)&255;
        if(ticks%100==0)AroundPlanet.LOGGER.info("SHIP_SEAM_PIXEL phase={} rgb={},{},{} visible={}",phase,r,g,b,image(mc).getRenderData().getVisibleSectionCount());
        return red?r>40 && r>g*1.4 && r>b*1.4:g>40 && g>r*1.3 && g>b*1.2;
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
