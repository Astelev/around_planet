package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.space.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import com.maxenonyme.createsubmarine.submarine.block.entity.OxygeneDiffuserBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import java.util.*;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class VacuumClientSmoke {
 private static int ticks,phase,start;private static volatile Throwable failure;private static volatile ServerSubLevel ship;private static volatile OxygeneDiffuserBlockEntity diffuser;private static volatile boolean damaged;private static boolean refilled;private static int initialGas;
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
  if(mc.screen!=null&&mc.screen.getClass().getName().contains("pondus")&&mc.screen.getClass().getSimpleName().contains("Import")){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Skip")){b.onPress();return;}}
  if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
  if(++ticks>5000)throw new AssertionError("Vacuum client timeout phase="+phase);if(failure!=null)throw new AssertionError("Vacuum client",failure);if(mc.player==null||mc.getSingleplayerServer()==null||ticks<100)return;
  if(phase==0){phase=1;start=ticks;mc.options.renderDistance().set(3);mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{try{setup(mc);}catch(Throwable t){failure=t;}});return;}
  var air=VacuumHud.current();if(ship==null||diffuser==null||air==null)return;
  if(phase==1&&ticks-start>180&&air.status().room()&&air.air()>=air.maximum()){
   mc.gui.getChat().clearMessages(false);shot(mc,"vacuum-sealed-room-alpha65.png");phase=2;start=ticks;mc.getSingleplayerServer().execute(()->{var player=ship.getLevel().players().getFirst();player.teleportTo(12010,6144,0);player.setYRot(90);player.setXRot(0);});return;
  }
  if(phase==2&&ticks-start>100&&!air.status().breathable()&&air.air()<air.maximum()){shot(mc,"vacuum-exposure-alpha65.png");mc.getSingleplayerServer().execute(()->ship.getLevel().players().getFirst().setAirSupply(-19));phase=3;start=ticks;return;}
  if(phase==3){if(ticks%10==0)mc.getSingleplayerServer().execute(()->damaged=ship.getLevel().players().getFirst().getHealth()<20);
   if(damaged){shot(mc,"vacuum-suffocation-alpha65.png");phase=4;start=ticks;mc.getSingleplayerServer().execute(()->inside());}return;
  }
  if(phase==4&&ticks%5==0)mc.getSingleplayerServer().execute(VacuumClientSmoke::inside);
  if(phase==4&&ticks-start>100&&air.status().room()&&air.air()>=air.maximum()){
   shot(mc,"vacuum-recovered-alpha65.png");mc.getSingleplayerServer().execute(()->{if(diffuser.oxygenTank.getFluidAmount()>=initialGas)failure=new AssertionError("Deep Seas consumed no oxygen");});phase=5;start=ticks;return;
  }
  if(phase==5&&ticks-start>10){AroundPlanet.LOGGER.info("VACUUM_CLIENT_PASS realDeepSeasRoom=true outsideAirLoss=true realSuffocationDamage=true airRecovered=true tankRemaining={}",diffuser.oxygenTank.getFluidAmount());phase=6;mc.stop();}
 }
 private static void inside(){if(phase==4&&!refilled){refilled=true;diffuser.oxygenTank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("create_submarine:oxygen")),1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);}var p=diffuser.getBlockPos();var at=ship.logicalPose().transformPosition(new org.joml.Vector3d(p.getX()+1.5,p.getY()+1.62,p.getZ()+1.5));var player=ship.getLevel().players().getFirst();player.teleportTo(at.x,at.y-player.getEyeHeight(),at.z);player.setYRot(0);player.setXRot(0);player.setNoGravity(true);player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.hurtMarked=true;

 }
 private static void setup(Minecraft mc){var l=mc.getSingleplayerServer().overworld();PlanetConfig.ENABLED.set(true);var p=new BlockPos(30,300,30);var blocks=new ArrayList<BlockPos>();
  for(int x=0;x<=6;x++)for(int y=0;y<=5;y++)for(int z=0;z<=6;z++)if(x==0||x==6||y==0||y==5||z==0||z==6){var at=p.offset(x,y,z);l.setBlockAndUpdate(at,(y==0?Blocks.IRON_BLOCK:Blocks.GLASS).defaultBlockState());blocks.add(at);}
  var machine=p.offset(2,1,2);l.setBlockAndUpdate(machine.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());blocks.add(machine.west());l.setBlockAndUpdate(machine,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create_submarine:oxygene_diffuser")).defaultBlockState());blocks.add(machine);
  ((OxygeneDiffuserBlockEntity)l.getBlockEntity(machine)).oxygenTank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("create_submarine:oxygen")),10000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  var c=SubLevelContainer.getContainer(l);ship=SubLevelAssemblyHelper.assembleBlocks(l,p,blocks,new BoundingBox3i(30,300,30,36,305,36));c.physicsSystem().getPipeline().teleport(ship,new org.joml.Vector3d(12000,6144,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();c.physicsSystem().getPipeline().resetVelocity(ship);c.addForceLoadTicket(ship,dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);
  diffuser=ship.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).filter(OxygeneDiffuserBlockEntity.class::isInstance).map(OxygeneDiffuserBlockEntity.class::cast).findFirst().orElseThrow();
  initialGas=diffuser.oxygenTank.getFluidAmount();
  var cell=new SpaceCells.Cell(UUID.randomUUID(),0,12000,6144,0,30,2200,30,List.of(ship.getUniqueId()));SpaceCells.get(l).add(l,cell);var data=SpaceOrbitData.get(l.getServer());var planet=new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry();double r=planet.radius()+10000,mu=9.81*planet.radius()*planet.radius();data.put(cell.id(),new SpaceTrajectory(CelestialSystem.EARTH,l.getGameTime(),new SpaceState(new SpaceVector(0,r,0),new SpaceVector(Math.sqrt(mu/r),0,0)),mu));data.navigate(cell.id(),new SpaceNavigation(planet,SpaceFrame.IDENTITY));
  var player=l.players().getFirst();player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setHealth(20);player.getFoodData().setFoodLevel(20);player.setAirSupply(300);inside();
 }
 private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
