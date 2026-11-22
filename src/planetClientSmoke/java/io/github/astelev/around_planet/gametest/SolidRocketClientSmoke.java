package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.solid.*;
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
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SolidRocketClientSmoke {
 private static int ticks,phase,start;private static volatile Throwable failure;private static volatile ServerSubLevel ship;private static volatile PropulsionBlockEntity nozzle;private static volatile BlockPos top;private static volatile double before;private static boolean busy;
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
  if(mc.screen!=null&&mc.screen.getClass().getName().contains("pondus")&&mc.screen.getClass().getSimpleName().contains("Import")){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Skip")){b.onPress();return;}}
  if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
  if(++ticks>2400)throw new AssertionError("Solid client timeout phase="+phase);if(failure!=null)throw new AssertionError("Solid rocket client",failure);if(mc.player==null||mc.getSingleplayerServer()==null||ticks<100)return;
  if(phase==0){phase=1;start=ticks;mc.options.hideGui=true;mc.options.renderDistance().set(3);mc.getSingleplayerServer().execute(()->{try{setup(mc);}catch(Throwable t){failure=t;}});return;}
  if(ship==null||nozzle==null)return;mc.player.setYRot(153);mc.player.setXRot(18);
  if(!busy&&ticks%1==0){busy=true;mc.getSingleplayerServer().execute(()->{try{var container=SubLevelContainer.getContainer(ship.getLevel());var pipe=container.physicsSystem().getPipeline();pipe.teleport(ship,new org.joml.Vector3d(ship.logicalPose().position()),new org.joml.Quaterniond());pipe.addLinearAndAngularVelocity(ship,new org.joml.Vector3d(),pipe.getAngularVelocity(ship,new org.joml.Vector3d()).negate());var p=ship.logicalPose().position();var player=ship.getLevel().players().getFirst();player.teleportTo(p.x()+4,p.y()+4.5,p.z()+8);player.setYRot(153);player.setXRot(18);}catch(Throwable t){failure=t;}finally{busy=false;}});}
  if(phase==1&&ticks-start>100&&partial(mc) instanceof SolidColumnBlockEntity column&&column.fuel()<62&&column.fuel()>0&&SolidColumnRenderer.frames>0&&PropulsionExhaustRenderer.frames>0){
   top=column.getBlockPos();shot(mc,"solid-rocket-loaded-alpha63.png");phase=2;start=ticks;mc.getSingleplayerServer().execute(()->{try{before=((SolidColumnBlockEntity)ship.getLevel().getBlockEntity(top)).fuel();ship.getLevel().removeBlock(nozzle.getBlockPos().east(),false);}catch(Throwable t){failure=t;}});return;
  }
  if(phase==2&&ticks-start>80){phase=3;mc.getSingleplayerServer().execute(()->{try{var column=(SolidColumnBlockEntity)ship.getLevel().getBlockEntity(top);if(!nozzle.solid.active()||nozzle.actualThrust()<=0||column.fuel()>=before)throw new AssertionError("Redstone removal stopped solid burn");var orbit=SpaceOrbitData.get(ship.getLevel().getServer());var c=SpaceCells.get(ship.getLevel()).forShip(ship.getUniqueId());if(orbit.sample(c.id(),ship.getLevel().getGameTime()).velocity().length()<1)throw new AssertionError("Native motor produced no orbital motion");AroundPlanet.LOGGER.info("SOLID_ROCKET_CLIENT_PASS liveWindow=true liveExhaust=true burnAfterSignalRemoved=true remaining={} thrust={} coreFrames={}",column.fuel(),nozzle.actualThrust(),SolidColumnRenderer.frames);}catch(Throwable t){failure=t;}});start=ticks;return;}
  if(phase==3&&ticks-start>20){shot(mc,"solid-rocket-burning-alpha63.png");phase=4;mc.stop();}
 }
 private static void setup(Minecraft mc){var level=mc.getSingleplayerServer().overworld();var p=new BlockPos(30,300,30);var blocks=new ArrayList<BlockPos>();level.setBlockAndUpdate(p,PropulsionRegistry.SOLID.get().defaultBlockState());blocks.add(p);level.setBlockAndUpdate(p.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());blocks.add(p.east());for(int i=1;i<=6;i++){var at=p.above(i);level.setBlockAndUpdate(at,SolidRegistry.COLUMN.get().defaultBlockState());((SolidColumnBlockEntity)level.getBlockEntity(at)).loadFuel();blocks.add(at);}
 var c=SubLevelContainer.getContainer(level);var body=SubLevelAssemblyHelper.assembleBlocks(level,p,blocks,new BoundingBox3i(30,300,30,31,306,30));c.physicsSystem().getPipeline().teleport(body,new org.joml.Vector3d(12000,6144,0),body.logicalPose().orientation());body.updateBoundingBox();body.updateLastPose();c.physicsSystem().getPipeline().resetVelocity(body);c.addForceLoadTicket(body,dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);
 var cell=new SpaceCells.Cell(UUID.randomUUID(),0,12000,6144,0,30,2200,30,List.of(body.getUniqueId()));SpaceCells.get(level).add(level,cell);var data=SpaceOrbitData.get(level.getServer());var planet=new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry();double r=planet.radius()+10000,mu=9.81*planet.radius()*planet.radius();data.put(cell.id(),new SpaceTrajectory(CelestialSystem.EARTH,level.getGameTime(),new SpaceState(new SpaceVector(0,r,0),new SpaceVector(Math.sqrt(mu/r),0,0)),mu));data.navigate(cell.id(),new SpaceNavigation(planet,SpaceFrame.IDENTITY));
 ship=body;var bes=body.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).toList();nozzle=bes.stream().filter(PropulsionBlockEntity.class::isInstance).map(PropulsionBlockEntity.class::cast).findFirst().orElseThrow();top=bes.stream().filter(SolidColumnBlockEntity.class::isInstance).map(SolidColumnBlockEntity.class::cast).max(Comparator.comparingInt(b->b.getBlockPos().getY())).orElseThrow().getBlockPos();
 var player=level.players().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(12004,6147,6);
 }
 private static net.minecraft.world.level.block.entity.BlockEntity partial(Minecraft mc){var body=SubLevelContainer.getContainer(mc.level).getSubLevel(ship.getUniqueId());if(body==null)return null;return body.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).filter(SolidColumnBlockEntity.class::isInstance).map(SolidColumnBlockEntity.class::cast).filter(c->c.fuel()>0&&c.fuel()<62).findFirst().orElse(null);}
 private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
