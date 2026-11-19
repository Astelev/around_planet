package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.loader.*;
import io.github.astelev.around_planet.compat.*;
import io.github.astelev.around_planet.space.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import java.util.*;
import org.joml.Vector3d;
/** Two actual game processes: save an unattended orbital machine, then cold-load it while far away. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class ShipLoaderClientSmoke {
 private static int ticks;private static long began;private static boolean initialized,busy;private static volatile boolean passed;private static volatile Throwable failure;private static UUID id;private static int initialOutput;
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}if(++ticks>2400)throw new AssertionError("Ship loader client timeout");if(failure!=null)throw new AssertionError("Ship loader client",failure);if(passed){AroundPlanet.LOGGER.info("SHIP_LOADER_CLIENT_PASS reload={} unattended furnace ticking, orbital owner, native persistent ticket",Boolean.getBoolean("around_planet.shipLoaderReload"));mc.stop();return;}if(mc.player==null||mc.getSingleplayerServer()==null||ticks<120||ticks%10!=0||busy)return;busy=true;mc.getSingleplayerServer().execute(()->{try{check(mc);}catch(Throwable t){failure=t;}finally{busy=false;}});}
 private static void check(Minecraft mc){var level=mc.getSingleplayerServer().overworld();var c=SubLevelContainer.getContainer(level);if(!initialized){var player=level.players().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setNoGravity(true);player.teleportTo(1000,200,1000);if(Boolean.getBoolean("around_planet.shipLoaderReload")){var ship=c.getAllSubLevels().stream().filter(s->"ship_loader_cold_restart".equals(s.getName())).findFirst().orElseThrow(()->new AssertionError("Persistent ship absent before any player visit"));id=ship.getUniqueId();require(SpaceCells.get(level).forShip(id)!=null,"Orbital cell missing after restart");initialOutput=furnace(ship).getItem(2).getCount();}else{var p=new BlockPos(30,280,30);level.setBlockAndUpdate(p,ShipLoaderRegistry.BLOCK.get().defaultBlockState());level.setBlockAndUpdate(p.east(),Blocks.FURNACE.defaultBlockState());var f=(FurnaceBlockEntity)level.getBlockEntity(p.east());f.setItem(0,new ItemStack(Items.RAW_IRON,64));f.setItem(1,new ItemStack(Items.COAL,64));var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p,p.east()),new BoundingBox3i(30,280,30,31,280,30));ship.setName("ship_loader_cold_restart");id=ship.getUniqueId();c.physicsSystem().getPipeline().teleport(ship,new Vector3d(12000,9000,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();c.physicsSystem().getPipeline().resetVelocity(ship);var cells=SpaceCells.get(level);var cell=new SpaceCells.Cell(UUID.randomUUID(),0,12000,9000,0,30,2208,30,List.of(id));cells.add(level,cell);var data=SpaceOrbitData.get(level.getServer());var planet=new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry();double r=planet.radius()+10000,mu=9.81*planet.radius()*planet.radius();data.put(cell.id(),new SpaceTrajectory(CelestialSystem.EARTH,level.getGameTime(),new SpaceState(new SpaceVector(0,r,0),new SpaceVector(Math.sqrt(mu/r),0,0)),mu));data.navigate(cell.id(),new SpaceNavigation(planet,SpaceFrame.IDENTITY));ShipLoaderBridge.before(c);}began=level.getGameTime();initialized=true;return;}
 var ship=(ServerSubLevel)c.getSubLevel(id);require(ship!=null&&!ship.isRemoved(),"Unattended ship unloaded");require(c.collectForceLoadTickets().getOrDefault(ship,Set.of()).stream().anyMatch(t->t.type().equals(ShipLoaderBridge.TICKET)),"Persistent loader ticket absent");require(level.players().stream().allMatch(p->p.position().distanceToSqr(ship.logicalPose().position().x(),ship.logicalPose().position().y(),ship.logicalPose().position().z())>1000000),"Player accidentally loaded fixture");if(level.getGameTime()-began<260)return;require(furnace(ship).getItem(2).getCount()>initialOutput,"Unattended furnace stopped ticking");passed=true;
 }
 private static FurnaceBlockEntity furnace(ServerSubLevel ship){for(var holder:ship.getPlot().getLoadedChunks())if(holder.getChunk()!=null)for(var be:holder.getChunk().getBlockEntities().values())if(be instanceof FurnaceBlockEntity f)return f;throw new AssertionError("Native furnace missing");}
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
