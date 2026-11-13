package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.sensor.*;
import io.github.astelev.around_planet.PlanetConfig;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import java.util.*;
@PrefixGameTestTemplate(false)
public final class SpaceSensorGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void orbitalSensorReadsVirtualVelocityAndHeightRebasePreservesNavigation(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var pipeline=c.physicsSystem().getPipeline();var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);ServerSubLevel ship=null;SpaceCells.Cell cell=null;int oldHeight=PlanetConfig.SPACE_Y.getAsInt();
        try{
            var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,OrbitalSensorRegistry.BLOCK.get().defaultBlockState());ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));
            pipeline.teleport(ship,new Vector3d(0,1536,0),new Quaterniond());ship.updateBoundingBox();ship.updateLastPose();
            cell=new SpaceCells.Cell(UUID.randomUUID(),40,0,1536,0,100,2200,100,List.of(ship.getUniqueId()));cells.add(level,cell);data.navigate(cell.id(),new SpaceNavigation(new PlanetScale(131072).geometry(),SpaceFrame.IDENTITY));data.put(cell.id(),new SpaceTrajectory("planet",level.getGameTime(),new SpaceState(new SpaceVector(0,30000,0),new SpaceVector(100,0,0)),0));
            var plot=ship.getPlot().getBoundingBox();var sensorPos=new BlockPos(plot.minX(),plot.minY(),plot.minZ());var be=(OrbitalSensorBlockEntity)level.getBlockEntity(sensorPos);h.assertTrue(be!=null,"Sensor did not survive native assembly");
            var target=io.github.astelev.around_planet.compat.SpaceSensorNavigation.direction(level,sensorPos,OrbitalAttitude.Mode.PROGRADE).orElseThrow();h.assertTrue(target.subtract(new SpaceVector(1,0,0)).length()<1e-6,"Sensor read stationary local speed instead of virtual orbital motion");
            OrbitalSensorBlockEntity.tick(level,sensorPos,be.getBlockState(),be);h.assertTrue(be.active() && be.power(Direction.EAST)==15 && be.power(Direction.WEST)==0,"Prograde yaw output is wrong");
            be.configure(OrbitalAttitude.Mode.RETROGRADE,30);OrbitalSensorBlockEntity.tick(level,sensorPos,be.getBlockState(),be);h.assertTrue(be.power(Direction.WEST)==15 && be.power(Direction.EAST)==0,"Retrograde did not reverse control output");
            pipeline.teleport(ship,new Vector3d(ship.logicalPose().position()),new Quaterniond().rotateY(-Math.PI/2));ship.updateBoundingBox();be.configure(OrbitalAttitude.Mode.PROGRADE,45);OrbitalSensorBlockEntity.tick(level,sensorPos,be.getBlockState(),be);h.assertTrue(Math.abs(be.yaw())<1e-5 && be.power(Direction.EAST)==0,"Sensor ignored native body orientation");
            var saved=be.saveWithoutMetadata(level.registryAccess());var restored=new OrbitalSensorBlockEntity(sensorPos,be.getBlockState());restored.loadWithComponents(saved,level.registryAccess());h.assertTrue(restored.mode()==be.mode() && restored.scale()==45,"Sensor settings did not persist");
            var orbit=data.get(cell.id()).orElseThrow();var velocity=pipeline.getLinearVelocity(ship,new Vector3d());PlanetConfig.SPACE_Y.set(9000);io.github.astelev.around_planet.compat.SpaceCellHeight.relocate(level,c);
            var shifted=cells.forShip(ship.getUniqueId());h.assertTrue(shifted.y()==9000 && Math.abs(ship.logicalPose().position().y-9000)<.01,"Configured height did not move an existing loaded cell");h.assertTrue(data.get(cell.id()).orElseThrow().equals(orbit) && pipeline.getLinearVelocity(ship,new Vector3d()).equals(velocity),"Physical height rebase changed orbit or native velocity");
            h.assertTrue(SpaceCells.load(cells.save(new CompoundTag(),level.registryAccess()),level.registryAccess()).forShip(ship.getUniqueId()).y()==9000,"High cell failed save validation");h.succeed();
        }finally{PlanetConfig.SPACE_Y.set(oldHeight);if(cell!=null){data.remove(cell.id());cells.remove(level,cell);}if(ship!=null)c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }
}
