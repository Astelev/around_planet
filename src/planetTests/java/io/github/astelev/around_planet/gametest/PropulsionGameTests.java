package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.compat.PropulsionPhysics;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.gametest.framework.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import org.joml.Vector3d;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
@PrefixGameTestTemplate(false)
public final class PropulsionGameTests {
    private static PropulsionBlockEntity machine(ServerSubLevel ship,PropulsionRegistry.Kind kind){return ship.getPlot().getLoadedChunks().stream().flatMap(h->h.getChunk().getBlockEntities().values().stream()).filter(PropulsionBlockEntity.class::isInstance).map(PropulsionBlockEntity.class::cast).filter(be->be.kind()==kind).findFirst().orElseThrow(()->new AssertionError("No propulsion BE in native plot"));}
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void thrusterAppliesNativeImpulseAndStopsWithoutSignal(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,PropulsionRegistry.ENGINE.get().defaultBlockState());level.setBlockAndUpdate(p.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p,p.below()),new BoundingBox3i(p.getX(),p.getY()-1,p.getZ(),p.getX(),p.getY(),p.getZ()));
        try{var be=machine(ship,PropulsionRegistry.Kind.CREATIVE);var at=be.getBlockPos();if(be==null)throw new AssertionError("Engine BE missing after assembly");be.configure(10000);PropulsionBlockEntity.tick(level,at,be.getBlockState(),be);h.assertTrue(be.signal(0)==15,"Engine lost native redstone input");var pipe=c.physicsSystem().getPipeline();pipe.resetVelocity(ship);PropulsionPhysics.step(c.physicsSystem(),.05);var v=pipe.getLinearVelocity(ship,new Vector3d());h.assertTrue(Math.abs(v.y-500/ship.getMassTracker().getMass())<1e-6&&Math.abs(v.x)<1e-8,"Engine force/seconds/mass conversion wrong");
            be.configure(0);PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).distance(v)<1e-8,"Zero-thrust engine still accelerated");
            be.configure(10000);level.setBlockAndUpdate(at.below(),Blocks.IRON_BLOCK.defaultBlockState());PropulsionBlockEntity.tick(level,at,be.getBlockState(),be);h.assertTrue(be.signal(0)==0,"Engine did not switch off with redstone");PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).distance(v)<1e-8,"Unpowered engine still accelerated");
            level.setBlockAndUpdate(at.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());var sideways=be.getBlockState().setValue(OrbitalSensorBlock.FACING,Direction.EAST);level.setBlockAndUpdate(at,sideways);be.setBlockState(sideways);PropulsionBlockEntity.tick(level,at,sideways,be);pipe.resetVelocity(ship);PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).x>0&&pipe.getAngularVelocity(ship,new Vector3d()).z<0,"Off-center thrust failed to apply its torque");
            var saved=be.saveWithoutMetadata(level.registryAccess());be.configure(123);be.loadWithComponents(saved,level.registryAccess());h.assertTrue(be.thrust()==10000,"Thrust setting did not survive persistence");h.succeed();
        }finally{c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void damperConsumesFEAndPreservesLinearVelocity(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,PropulsionRegistry.DAMPER.get().defaultBlockState());level.setBlockAndUpdate(p.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p,p.below()),new BoundingBox3i(p.getX(),p.getY()-1,p.getZ(),p.getX(),p.getY(),p.getZ()));
        try{var be=machine(ship,PropulsionRegistry.Kind.DAMPER);var at=be.getBlockPos();PropulsionBlockEntity.tick(level,at,be.getBlockState(),be);var pipe=c.physicsSystem().getPipeline();pipe.resetVelocity(ship);pipe.addLinearAndAngularVelocity(ship,new Vector3d(10,3,2),new Vector3d(0,2,0));PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(Math.abs(pipe.getAngularVelocity(ship,new Vector3d()).y-2)<1e-8,"Empty damper damped without FE");
            var cap=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,at,Direction.UP);h.assertTrue(cap!=null&&cap.receiveEnergy(100000,false)>0&&!cap.canExtract(),"Damper FE capability missing or exports power");int before=be.energy.getEnergyStored();PropulsionPhysics.step(c.physicsSystem(),.05);var w=pipe.getAngularVelocity(ship,new Vector3d());h.assertTrue(w.y>0&&w.y<2&&be.energy.getEnergyStored()<before,"Powered damper failed to dissipate rotation using FE");h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).distance(new Vector3d(10,3,2))<1e-8,"Damper changed linear flight velocity");
            var saved=be.saveWithoutMetadata(level.registryAccess());int stored=be.energy.getEnergyStored();be.energy.stored(0);be.loadWithComponents(saved,level.registryAccess());h.assertTrue(be.energy.getEnergyStored()==stored,"Damper lost FE on save/load");h.succeed();
        }finally{c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void rsuReceivesIndependentCreateFrequencies(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}var level=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,PropulsionRegistry.RSU.get().defaultBlockState());var be=(PropulsionBlockEntity)level.getBlockEntity(p);be.frequency(0,new ItemStack(Items.DIAMOND));be.frequency(1,new ItemStack(Items.IRON_INGOT));be.frequency(2,new ItemStack(Items.EMERALD));be.frequency(3,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(be.direction(0).z()==1&&be.direction(1).x()==-1,"RSU thrust must point away from the selected exhaust nozzle");
        var key=net.createmod.catnip.data.Couple.create(com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency.of(new ItemStack(Items.DIAMOND)),com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency.of(new ItemStack(Items.IRON_INGOT)));
        var transmitter=new com.simibubi.create.content.redstone.link.IRedstoneLinkable(){public int getTransmittedStrength(){return 11;}public void setReceivedStrength(int n){}public boolean isListening(){return false;}public boolean isAlive(){return true;}public net.createmod.catnip.data.Couple<com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency> getNetworkKey(){return key;}public BlockPos getLocation(){return p;}};
        try{com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level,transmitter);h.assertTrue(be.signal(0)==11&&be.signal(1)==0,"RSU channels were crossed or did not receive native Create power");be.frequency(0,ItemStack.EMPTY);be.frequency(1,ItemStack.EMPTY);h.assertTrue(be.signal(0)==0,"Clearing frequency retained a stale signal");var box=be.getBlockState().getShape(level,p).bounds();h.assertTrue(box.getXsize()<=.5&&box.getYsize()<=.25&&box.getZsize()<=.5,"RSU hitbox is not compact");h.succeed();}
        finally{com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level,transmitter);level.removeBlock(p,false);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void thrusterUpdatesVirtualSpaceVelocity(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,PropulsionRegistry.ENGINE.get().defaultBlockState());level.setBlockAndUpdate(p.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p,p.below()),new BoundingBox3i(p.getX(),p.getY()-1,p.getZ(),p.getX(),p.getY(),p.getZ()));
        var cells=io.github.astelev.around_planet.space.SpaceCells.get(level);var data=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());io.github.astelev.around_planet.space.SpaceCells.Cell cell=null;
        try{var be=machine(ship,PropulsionRegistry.Kind.CREATIVE);var at=be.getBlockPos();PropulsionBlockEntity.tick(level,at,be.getBlockState(),be);var pipe=c.physicsSystem().getPipeline();pipe.teleport(ship,new Vector3d(1200,6144,1200),new org.joml.Quaterniond());ship.updateBoundingBox();ship.updateLastPose();pipe.resetVelocity(ship);
            cell=new io.github.astelev.around_planet.space.SpaceCells.Cell(UUID.randomUUID(),77,1200,6144,1200,0,2200,0,List.of(ship.getUniqueId()));cells.add(level,cell);var geometry=new io.github.astelev.around_planet.space.PlanetScale(131072).geometry();data.navigate(cell.id(),new io.github.astelev.around_planet.space.SpaceNavigation(geometry,io.github.astelev.around_planet.space.SpaceFrame.IDENTITY));data.put(cell.id(),new io.github.astelev.around_planet.space.SpaceTrajectory("planet",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(0,30000,0),new io.github.astelev.around_planet.space.SpaceVector(100,0,0)),0));
            PropulsionPhysics.step(c.physicsSystem(),.05);double expected=500/ship.getMassTracker().getMass();io.github.astelev.around_planet.compat.SpaceLocalPhysics.before(c.physicsSystem(),.05);var velocity=data.sample(cell.id(),level.getGameTime()).velocity();h.assertTrue(Math.abs(velocity.x()-100)<1e-7&&Math.abs(velocity.y()-expected)<1e-6,"Space frame did not capture engine impulse into virtual orbital velocity");h.succeed();
        }finally{if(cell!=null){data.remove(cell.id());cells.remove(level,cell);}c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void rsuOppositeNozzlesCancelAndSingleNozzlePushesAway(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,PropulsionRegistry.RSU.get().defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));
        try{var be=machine(ship,PropulsionRegistry.Kind.RSU);PropulsionBlockEntity.tick(level,be.getBlockPos(),be.getBlockState(),be);be.configure(1000);be.receiveSignal(0,15);be.receiveSignal(2,15);var pipe=c.physicsSystem().getPipeline();pipe.resetVelocity(ship);PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).length()<1e-8&&pipe.getAngularVelocity(ship,new Vector3d()).length()<1e-8,"Opposite RSU nozzles did not cancel");be.receiveSignal(2,0);PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(Math.abs(pipe.getLinearVelocity(ship,new Vector3d()).z-50/ship.getMassTracker().getMass())<1e-6,"Gold exhaust did not push away from its north side");h.succeed();}
        finally{c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }

}
