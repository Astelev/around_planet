package io.github.astelev.around_planet.gametest;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.HashMap;

/** Tests the actual rigid body and Sable's public terrain section builder, not a Java collision approximation. */
@PrefixGameTestTemplate(false)
public final class ShipSeamGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=400)
    public static void nativeShipHitsOppositeWallBeforeCenterCrosses(GameTestHelper h) {
        var level=h.getLevel();var container=SubLevelContainer.getContainer(level);var system=container.physicsSystem();var pipeline=system.getPipeline();
        PlanetBorders.bind(level.getWorldBorder(),new EastWestTopology(512));
        var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        java.util.function.BiConsumer<BlockPos,net.minecraft.world.level.block.state.BlockState> put=(pos,state)->{
            saved.putIfAbsent(pos,level.getBlockState(pos));level.setBlockAndUpdate(pos,state);
        };
        var ships=new ArrayList<ServerSubLevel>();
        try {
            for(int cx:new int[]{-17,-16,15,16})for(int cz=4;cz<=5;cz++)level.getChunk(cx,cz);
            for(int direction:new int[]{1,-1}) {
                int wall=direction>0?-256:255,ghost=direction>0?256:-257;
                int z0=direction>0?70:82;
                for(int x:new int[]{-258,-257,-256,-255,-254,253,254,255,256,257})
                    for(int y=195;y<=205;y++)for(int z=z0-2;z<=z0+2;z++)put.accept(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                for(int y=195;y<=205;y++)for(int z=z0-2;z<=z0+2;z++)put.accept(new BlockPos(wall,y,z),Blocks.STONE.defaultBlockState());
                var anchor=h.absolutePos(new BlockPos(1,2,1));var blocks=new ArrayList<BlockPos>();
                for(int x=0;x<3;x++){var pos=anchor.offset(x,0,0);level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());blocks.add(pos);}
                var ship=SubLevelAssemblyHelper.assembleBlocks(level,anchor,blocks,new BoundingBox3i(anchor.getX(),anchor.getY(),anchor.getZ(),anchor.getX()+2,anchor.getY(),anchor.getZ()));
                ships.add(ship);container.addForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
                pipeline.teleport(ship,new Vector3d(direction*254,200,z0+0.5),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
                system.getTicketManager().update(level,container,system,pipeline,0.05);
                pipeline.resetVelocity(ship);pipeline.addLinearAndAngularVelocity(ship,new Vector3d(direction*2,0,0),new Vector3d());
                for(int step=0;step<200;step++) {
                    var velocity=pipeline.getLinearVelocity(ship,new Vector3d());
                    pipeline.addLinearAndAngularVelocity(ship,new Vector3d(0,-velocity.y,0),new Vector3d());
                    pipeline.prePhysicsTicks();pipeline.physicsTick(0.01);system.updatePose(ship);
                }
                double x=ship.logicalPose().position().x;
                h.assertTrue(direction*x<255.1,"Rigid body passed through the opposite wall before its center crossed: "+x);
                h.assertTrue(direction*x>253.5,"Collision teleported or rolled back the real ship: "+x);
                h.assertTrue(Math.abs(pipeline.getLinearVelocity(ship,new Vector3d()).x)<0.3,"Native contact did not stop the rigid body");
                // Remove the real wall; its already-uploaded image must update before the next physics step.
                for(int y=195;y<=205;y++)for(int z=z0-2;z<=z0+2;z++)put.accept(new BlockPos(wall,y,z),Blocks.AIR.defaultBlockState());
                // Contradictory guard terrain must never become the physics source.
                for(int y=195;y<=205;y++)for(int z=z0-2;z<=z0+2;z++)put.accept(new BlockPos(ghost,y,z),Blocks.STONE.defaultBlockState());
                pipeline.resetVelocity(ship);pipeline.addLinearAndAngularVelocity(ship,new Vector3d(direction*2,0,0),new Vector3d());
                for(int step=0;step<150;step++) {
                    var velocity=pipeline.getLinearVelocity(ship,new Vector3d());pipeline.addLinearAndAngularVelocity(ship,new Vector3d(0,-velocity.y,0),new Vector3d());
                    pipeline.prePhysicsTicks();pipeline.physicsTick(0.01);system.updatePose(ship);
                }
                h.assertTrue(direction*ship.logicalPose().position().x>255.3,"Removed canonical wall or invisible guard terrain still blocks ship");
                container.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
                container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            }
            h.succeed();
        } finally {
            for(var ship:ships)if(container.getSubLevel(ship.getUniqueId())!=null) {
                container.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            }
            saved.forEach(level::setBlockAndUpdate);PlanetBorders.unbind(level.getWorldBorder());
        }
    }
}
