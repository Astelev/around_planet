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
            pipeline.teleport(ship,new Vector3d(0,1700,0),new Quaterniond());ship.updateBoundingBox();ship.updateLastPose();
            cell=new SpaceCells.Cell(UUID.randomUUID(),40,0,1700,0,100,2200,100,List.of(ship.getUniqueId()));cells.add(level,cell);data.navigate(cell.id(),new SpaceNavigation(new PlanetScale(131072).geometry(),SpaceFrame.IDENTITY));data.put(cell.id(),new SpaceTrajectory("planet",level.getGameTime(),new SpaceState(new SpaceVector(0,30000,0),new SpaceVector(100,0,0)),0));
            var plot=ship.getPlot().getBoundingBox();var sensorPos=new BlockPos(plot.minX(),plot.minY(),plot.minZ());var be=(OrbitalSensorBlockEntity)level.getBlockEntity(sensorPos);h.assertTrue(be!=null,"Sensor did not survive native assembly");
            var target=io.github.astelev.around_planet.compat.SpaceSensorNavigation.direction(level,sensorPos,OrbitalAttitude.Mode.PROGRADE).orElseThrow();h.assertTrue(target.subtract(new SpaceVector(1,0,0)).length()<1e-6,"Sensor read stationary local speed instead of virtual orbital motion");
            OrbitalSensorBlockEntity.tick(level,sensorPos,be.getBlockState(),be);h.assertTrue(be.active() && be.power(Direction.EAST)==15 && be.power(Direction.WEST)==0,"Prograde yaw output is wrong");
            be.configure(OrbitalAttitude.Mode.RETROGRADE,30);OrbitalSensorBlockEntity.tick(level,sensorPos,be.getBlockState(),be);h.assertTrue(be.power(Direction.WEST)==15 && be.power(Direction.EAST)==0,"Retrograde did not reverse control output");
            pipeline.teleport(ship,new Vector3d(ship.logicalPose().position()),new Quaterniond().rotateY(-Math.PI/2));ship.updateBoundingBox();be.configure(OrbitalAttitude.Mode.PROGRADE,45);OrbitalSensorBlockEntity.tick(level,sensorPos,be.getBlockState(),be);h.assertTrue(Math.abs(be.yaw())<1e-5 && be.power(Direction.EAST)==0,"Sensor ignored native body orientation");
            var saved=be.saveWithoutMetadata(level.registryAccess());var restored=new OrbitalSensorBlockEntity(sensorPos,be.getBlockState());restored.loadWithComponents(saved,level.registryAccess());h.assertTrue(restored.mode()==be.mode() && restored.scale()==45,"Sensor settings did not persist");
            if(net.neoforged.fml.ModList.get().isLoaded("aeroengineering"))checkHelmet(h,level,ship,sensorPos);
            var orbit=data.get(cell.id()).orElseThrow();var velocity=pipeline.getLinearVelocity(ship,new Vector3d());PlanetConfig.SPACE_Y.set(9000);io.github.astelev.around_planet.compat.SpaceCellHeight.relocate(level,c);
            var shifted=cells.forShip(ship.getUniqueId());h.assertTrue(shifted.y()==9000 && Math.abs(ship.logicalPose().position().y-9000)<.01,"Configured height did not move an existing loaded cell");h.assertTrue(data.get(cell.id()).orElseThrow().equals(orbit) && pipeline.getLinearVelocity(ship,new Vector3d()).equals(velocity),"Physical height rebase changed orbit or native velocity");
            h.assertTrue(SpaceCells.load(cells.save(new CompoundTag(),level.registryAccess()),level.registryAccess()).forShip(ship.getUniqueId()).y()==9000,"High cell failed save validation");h.succeed();
        }finally{PlanetConfig.SPACE_Y.set(oldHeight);if(cell!=null){data.remove(cell.id());cells.remove(level,cell);}if(ship!=null)c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }
    private static void checkHelmet(GameTestHelper h,net.minecraft.server.level.ServerLevel level,ServerSubLevel ship,BlockPos pos){
        var helmet=new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("aeroengineering:engine_monitor_helmet")));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,helmet);player.setShiftKeyDown(true);
        try{var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));helmet.getItem().useOn(context);
            h.assertTrue(io.github.astelev.around_planet.compat.AeroHelmetBridge.targets(helmet).contains(pos),"Native helmet rejected orbital sensor binding");h.assertTrue(ship.getUniqueId().equals(io.github.astelev.around_planet.compat.AeroHelmetBridge.structure(helmet)),"Helmet stored wrong native structure");
            var regular=pos.east();level.setBlockAndUpdate(regular,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("simulated:gimbal_sensor")).defaultBlockState());helmet.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(regular),Direction.UP,regular,false)));h.assertTrue(io.github.astelev.around_planet.compat.AeroHelmetBridge.targets(helmet).containsAll(List.of(pos,regular)),"Native and orbital gimbals could not share the helmet");level.setBlockAndUpdate(regular,Blocks.AIR.defaultBlockState());
            try{var cls=Class.forName("com.cxw.aeroengineering.content.item.EngineMonitorHelmetItem");cls.getMethod("pruneDestroyedTargets",net.minecraft.world.item.ItemStack.class,net.minecraft.world.level.Level.class).invoke(null,helmet,level);var type=cls.getDeclaredMethod("getTargetType",net.minecraft.world.level.Level.class,BlockPos.class);type.setAccessible(true);h.assertTrue(((Enum<?>)type.invoke(null,level,pos)).name().equals("OTHER"),"Orbital sensor incorrectly occupies the ordinary gimbal attitude slot");}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            h.assertTrue(io.github.astelev.around_planet.compat.AeroHelmetBridge.targets(helmet).contains(pos),"Native prune removed a valid orbital binding");
            var restored=net.minecraft.world.item.ItemStack.parseOptional(level.registryAccess(),(CompoundTag)helmet.save(level.registryAccess()));h.assertTrue(io.github.astelev.around_planet.compat.AeroHelmetBridge.targets(restored).contains(pos),"Helmet binding did not survive save");
            var settings=helmet.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();for(var raw:settings.getList("MonitorTargets",10))((CompoundTag)raw).putBoolean("Enabled",false);helmet.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(settings));h.assertTrue(!io.github.astelev.around_planet.compat.AeroHelmetBridge.enabled(helmet,pos),"Disabled native target still enables the orbital instrument");
        }finally{player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);player.setShiftKeyDown(false);}
    }
}
