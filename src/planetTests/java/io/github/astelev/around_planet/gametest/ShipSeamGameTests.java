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
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void centerOfMassMovesJointAndPassengersTogether(GameTestHelper h) {
        var level=h.getLevel();var container=SubLevelContainer.getContainer(level);var system=container.physicsSystem();var pipeline=system.getPipeline();
        PlanetBorders.bind(level.getWorldBorder(),new EastWestTopology(512));
        var bodies=new ArrayList<ServerSubLevel>();var entities=new ArrayList<net.minecraft.world.entity.Entity>();
        dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle joint=null;
        try {
            for(int x:new int[]{-17,-16,15,16})for(int z=5;z<=7;z++)level.getChunk(x,z);
            for(int i=0;i<2;i++) {
                var pos=h.absolutePos(new BlockPos(1+i*4,2,1));level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());
                var ship=SubLevelAssemblyHelper.assembleBlocks(level,pos,java.util.List.of(pos),new BoundingBox3i(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ()));
                bodies.add(ship);pipeline.teleport(ship,new Vector3d(254.5+i*2,200,100),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
            }
            var a=bodies.get(0);var b=bodies.get(1);
            var anchorA=a.getSelfMassTracker().getCenterOfMass();var anchorB=b.getSelfMassTracker().getCenterOfMass();
            joint=pipeline.addConstraint(a,b,new dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration(
                    new Vector3d(anchorA).add(1,0,0),new Vector3d(anchorB).add(-1,0,0),new Vector3d(0,1,0),new Vector3d(0,1,0)));
            h.assertTrue(joint!=null && joint.isValid(),"Rotary fixture was not created");
            var actor=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);actor.setPos(256.7,201,100);actor.setNoGravity(true);level.addFreshEntity(actor);entities.add(actor);
            var movement=(dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension)actor;movement.sable$setTrackingSubLevel(b);
            io.github.astelev.around_planet.transfer.TransferService.check(actor);
            h.assertTrue(actor.getX()==256.7,"Outboard passenger wrapped independently of its carrier");
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(a.logicalPose().position().x==254.5 && b.logicalPose().position().x==256.5,"Child center triggered a premature group transfer");
            var boat=net.minecraft.world.entity.EntityType.BOAT.create(level);boat.setPos(256.5,201,103);level.addFreshEntity(boat);entities.add(boat);
            ((dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension)boat).sable$setTrackingSubLevel(b);
            var pig=net.minecraft.world.entity.EntityType.PIG.create(level);pig.setPos(256.5,201,103);level.addFreshEntity(pig);entities.add(pig);pig.startRiding(boat,true);
            actor.setDeltaMovement(0.3,0.2,0.1);boat.setDeltaMovement(0.6,0.1,0.2);
            for(var ship:bodies) {
                pipeline.teleport(ship,new Vector3d(ship.logicalPose().position()).add(1,0,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
                pipeline.addLinearAndAngularVelocity(ship,new Vector3d(2,0.3,0.1),new Vector3d(0.01,0.02,0.03));
            }
            actor.setPos(actor.getX()+1,actor.getY(),actor.getZ());boat.setPos(boat.getX()+1,boat.getY(),boat.getZ());boat.positionRider(pig);
            var linear=pipeline.getLinearVelocity(a,new Vector3d());var angular=pipeline.getAngularVelocity(a,new Vector3d());
            double actorX=actor.getX(),boatX=boat.getX();
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(Math.abs(a.logicalPose().position().x+256.5)<1e-6 && Math.abs(b.logicalPose().position().x+254.5)<1e-6,"Group did not wrap by one common circumference");
            h.assertTrue(joint.isValid(),"Transfer broke the rotary constraint");
            h.assertTrue(pipeline.getLinearVelocity(a,new Vector3d()).distance(linear)<1e-8 && pipeline.getAngularVelocity(a,new Vector3d()).distance(angular)<1e-8,"Transfer changed rigid body velocity");
            h.assertTrue(Math.abs(actor.getX()-actorX+512)<1e-6 && SableTracking(actor)==b,"Standing passenger did not follow its original sublevel");
            h.assertTrue(Math.abs(boat.getX()-boatX+512)<1e-6 && pig.getVehicle()==boat,"Riding group separated during carrier transfer");
            h.assertTrue(actor.getDeltaMovement().equals(new net.minecraft.world.phys.Vec3(0.3,0.2,0.1)) && boat.getDeltaMovement().x==0.6,"Passenger velocity changed");
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(Math.abs(a.logicalPose().position().x+256.5)<1e-6,"Outboard member caused an immediate reverse bounce");
            for(var ship:bodies){pipeline.teleport(ship,new Vector3d(ship.logicalPose().position()).add(-1,0,0),ship.logicalPose().orientation());ship.updateBoundingBox();}
            actor.setPos(actor.getX()-1,actor.getY(),actor.getZ());boat.setPos(boat.getX()-1,boat.getY(),boat.getZ());
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(Math.abs(a.logicalPose().position().x-254.5)<1e-6 && Math.abs(b.logicalPose().position().x-256.5)<1e-6,"West group crossing did not preserve the relative layout");
            for(int step=0;step<20;step++) {
                pipeline.prePhysicsTicks();pipeline.physicsTick(0.01);system.updatePose(a);system.updatePose(b);
            }
            var worldA=a.logicalPose().transformPosition(new Vector3d(anchorA).add(1,0,0));
            var worldB=b.logicalPose().transformPosition(new Vector3d(anchorB).add(-1,0,0));
            h.assertTrue(joint.isValid() && worldA.distance(worldB)<0.1,"Native solver lost the rotary connection after transfer");
            h.assertTrue(a.logicalPose().position().x>254.6,"Body did not continue moving after transfer");
            try {Class.forName("dev.ryanhcode.sable.physics.impl.rapier.rope.RapierRopeHandle");Class.forName("dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject");}
            catch(ClassNotFoundException missing){throw new AssertionError(missing);}
            joint.remove();joint=pipeline.addConstraint(a,null,new dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration(
                    new Vector3d(anchorA),new Vector3d(257,200,100),new org.joml.Quaterniond()));
            pipeline.teleport(a,new Vector3d(257,200,100),a.logicalPose().orientation());a.updateBoundingBox();
            pipeline.teleport(b,new Vector3d(255,200,100),b.logicalPose().orientation());b.updateBoundingBox();
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(a.logicalPose().position().x==257 && joint.isValid(),"World-anchored body was partially transferred");
            joint.remove();joint=null;
            pipeline.teleport(a,new Vector3d(257,200,1234567),a.logicalPose().orientation());a.updateBoundingBox();
            h.assertTrue(level.getChunkSource().getChunkNow(-16,1234567>>4)==null,"Cold destination fixture was already loaded");
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(a.logicalPose().position().x==257 && level.getChunkSource().getChunkNow(-16,1234567>>4)==null,"Transfer entered or synchronously generated a missing destination");
            h.succeed();
        } finally {
            entities.forEach(net.minecraft.world.entity.Entity::discard);if(joint!=null && joint.isValid())joint.remove();
            for(var ship:bodies)if(container.getSubLevel(ship.getUniqueId())!=null)container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            PlanetBorders.unbind(level.getWorldBorder());
        }
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void plotSeatStaysLocalWhileRiderWraps(GameTestHelper h) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var level=h.getLevel();var container=SubLevelContainer.getContainer(level);
        PlanetBorders.bind(level.getWorldBorder(),new EastWestTopology(512));
        ServerSubLevel ship=null;net.minecraft.world.entity.Entity seat=null,rider=null;
        try {
            for(int x:new int[]{-17,-16,15,16})for(int z=5;z<=7;z++)level.getChunk(x,z);
            var pos=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());
            ship=SubLevelAssemblyHelper.assembleBlocks(level,pos,java.util.List.of(pos),new BoundingBox3i(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ()));
            var local=ship.getPlot().getBoundingBox();
            seat=(net.minecraft.world.entity.Entity)Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatEntity")
                    .getConstructor(net.minecraft.world.level.Level.class).newInstance(level);
            seat.setPos(local.minX()+0.5,local.minY()+1,local.minZ()+0.5);level.addFreshEntity(seat);
            var seatPos=seat.position();
            var pipeline=container.physicsSystem().getPipeline();
            pipeline.teleport(ship,new Vector3d(256.5,200,100),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
            rider=net.minecraft.world.entity.EntityType.PIG.create(level);rider.setPos(256.5,202,100);level.addFreshEntity(rider);
            rider.startRiding(seat,true);seat.positionRider(rider);
            h.assertTrue(dev.ryanhcode.sable.Sable.HELPER.getContaining(seat)==ship,"Seat fixture is not in its plot");
            h.assertTrue(io.github.astelev.around_planet.compat.SablePassengers.ship(rider)==ship,"Native seat did not identify its carrier");
            var before=rider.position();rider.setDeltaMovement(0.2,0.1,0.3);
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(seat.position().equals(seatPos),"Seat was translated out of its storage plot");
            h.assertTrue(rider.getVehicle()==seat && rider.position().distanceTo(before.add(-512,0,0))<1e-6,"Seated rider did not wrap with its carrier");
            h.assertTrue(rider.getDeltaMovement().equals(new net.minecraft.world.phys.Vec3(0.2,0.1,0.3)),"Seated rider lost velocity");
            seat.positionRider(rider);
            h.assertTrue(rider.position().distanceTo(before.add(-512,0,0))<1e-6,"Native riding update returned rider to old coordinates");
            pipeline.teleport(ship,new Vector3d(-256.5,200,100),ship.logicalPose().orientation());ship.updateBoundingBox();seat.positionRider(rider);
            before=rider.position();io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(seat.position().equals(seatPos) && rider.getVehicle()==seat && rider.position().distanceTo(before.add(512,0,0))<1e-6,"Reverse seat transfer changed coordinate space or detached rider");
            h.succeed();
        } finally {
            if(rider!=null)rider.discard();if(seat!=null)seat.discard();
            if(ship!=null)container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            PlanetBorders.unbind(level.getWorldBorder());
        }
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void threeAxisSpaceTranslationPreservesJointMomentumAndNativeSeat(GameTestHelper h) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var level=h.getLevel();var container=SubLevelContainer.getContainer(level);var pipeline=container.physicsSystem().getPipeline();
        var ships=new ArrayList<ServerSubLevel>();net.minecraft.world.entity.Entity seat=null,pig=null;
        dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle joint=null;
        io.github.astelev.around_planet.space.SpaceCells.Cell cell=null;
        try {
            for(int i=0;i<2;i++) {
                var block=h.absolutePos(new BlockPos(1+4*i,2,1));level.setBlockAndUpdate(block,Blocks.IRON_BLOCK.defaultBlockState());
                var ship=SubLevelAssemblyHelper.assembleBlocks(level,block,java.util.List.of(block),new BoundingBox3i(block.getX(),block.getY(),block.getZ(),block.getX(),block.getY(),block.getZ()));ships.add(ship);
                pipeline.teleport(ship,new Vector3d(20+2*i,200,20),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
                pipeline.addLinearAndAngularVelocity(ship,new Vector3d(2,.5,-1),new Vector3d(.01,.02,.03));
            }
            var a=ships.getFirst();var b=ships.getLast();
            joint=pipeline.addConstraint(a,b,new dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration(new Vector3d(a.getSelfMassTracker().getCenterOfMass()),new Vector3d(b.getSelfMassTracker().getCenterOfMass()),new Vector3d(0,1,0),new Vector3d(0,1,0)));
            var box=a.getPlot().getBoundingBox();seat=(net.minecraft.world.entity.Entity)Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatEntity").getConstructor(net.minecraft.world.level.Level.class).newInstance(level);
            seat.setPos(box.minX()+.5,box.minY()+1,box.minZ()+.5);level.addFreshEntity(seat);
            pig=net.minecraft.world.entity.EntityType.PIG.create(level);pig.setPos(20,201,20);level.addFreshEntity(pig);pig.startRiding(seat,true);seat.positionRider(pig);
            var seatPos=seat.position();var before=pig.position();var linear=pipeline.getLinearVelocity(a,new Vector3d());var angular=pipeline.getAngularVelocity(a,new Vector3d());
            var delta=new Vector3d(80,1336,60);cell=new io.github.astelev.around_planet.space.SpaceCells.Cell(java.util.UUID.randomUUID(),40,100,1536,80,20,736,20,ships.stream().map(ServerSubLevel::getUniqueId).toList());
            io.github.astelev.around_planet.space.SpaceCells.get(level).add(level,cell);
            io.github.astelev.around_planet.compat.SpaceShipTransfers.move(level,container,ships,java.util.Set.of(seat),delta,java.util.List.of(cell));
            h.assertTrue(a.logicalPose().position().distance(new Vector3d(100,1536,80))<1e-8 && b.logicalPose().position().distance(new Vector3d(102,1536,80))<1e-8,"Space translation broke joint layout");
            h.assertTrue(joint.isValid() && pipeline.getLinearVelocity(a,new Vector3d()).distance(linear)<1e-8 && pipeline.getAngularVelocity(a,new Vector3d()).distance(angular)<1e-8,"Space transfer changed momentum or removed joint");
            h.assertTrue(seat.position().equals(seatPos) && pig.getVehicle()==seat && pig.position().distanceTo(before.add(80,1336,60))<1e-6,"Space transfer corrupted native seat coordinates");
            if(net.neoforged.fml.ModList.get().isLoaded("cruising")) {
                var policy=Class.forName("io.github.astelev.cruising.flight.ConnectedFlightAssembly").getMethod("currentMinY",dev.ryanhcode.sable.sublevel.SubLevel.class);
                h.assertTrue((Double)policy.invoke(null,a)==Double.NEGATIVE_INFINITY,"Space assembly still qualifies for Cruising height optimization");
            }
            var saved=io.github.astelev.around_planet.space.SpaceCells.get(level).save(new net.minecraft.nbt.CompoundTag(),level.registryAccess());
            var restored=io.github.astelev.around_planet.space.SpaceCells.load(saved,level.registryAccess());
            h.assertTrue(restored.forShip(a.getUniqueId()).id().equals(cell.id()),"Cell membership lost across save/reload");
            io.github.astelev.around_planet.compat.SpaceShipTransfers.move(level,container,ships,java.util.Set.of(seat),new Vector3d(delta).negate(),java.util.List.of());
            h.assertTrue(pig.position().distanceTo(before)<1e-6 && joint.isValid() && seat.position().equals(seatPos),"Reverse space translation did not preserve seat/joint");
            h.succeed();
        } finally {
            if(pig!=null)pig.discard();if(seat!=null)seat.discard();if(joint!=null && joint.isValid())joint.remove();
            if(cell!=null)io.github.astelev.around_planet.space.SpaceCells.get(level).remove(level,cell);
            for(var ship:ships)container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
        }
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void spaceOwnerDoesNotFallAndEngineImpulseMovesOnlyVirtualFrame(GameTestHelper h) {
        var level=h.getLevel();var container=SubLevelContainer.getContainer(level);var system=container.physicsSystem();var pipeline=system.getPipeline();
        var ships=new ArrayList<ServerSubLevel>();
        io.github.astelev.around_planet.space.SpaceCells.Cell cell=null;
        dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle joint=null;
        try {
            for(int i=0;i<4;i++) {
                var pos=h.absolutePos(new BlockPos(1+3*i,2,1));level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());
                var ship=SubLevelAssemblyHelper.assembleBlocks(level,pos,java.util.List.of(pos),new BoundingBox3i(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ()));ships.add(ship);
                pipeline.teleport(ship,new Vector3d(i<2?100+2*i:130,i==3?500:1536,80),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
            }
            var a=ships.get(0);var b=ships.get(1);var free=ships.get(2);var surface=ships.get(3);
            var anchorA=new Vector3d(a.getSelfMassTracker().getCenterOfMass()).add(1,0,0);var anchorB=new Vector3d(b.getSelfMassTracker().getCenterOfMass()).add(-1,0,0);
            joint=pipeline.addConstraint(a,b,new dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration(anchorA,anchorB,new Vector3d(0,1,0),new Vector3d(0,1,0)));
            for(var ship:java.util.List.of(a,b))pipeline.addLinearAndAngularVelocity(ship,new Vector3d(4,-8,3),new Vector3d(0,.05,0));
            cell=new io.github.astelev.around_planet.space.SpaceCells.Cell(java.util.UUID.randomUUID(),40,101,1536,80,20,736,20,java.util.List.of(a.getUniqueId(),b.getUniqueId()));
            io.github.astelev.around_planet.space.SpaceCells.get(level).add(level,cell);
            io.github.astelev.around_planet.compat.SpaceLocalPhysics.enter(level,container,cell);
            var orbit=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());
            h.assertTrue(Math.abs(orbit.navigation(cell.id()).orElseThrow().frame().toLocal(orbit.sample(cell.id(),level.getGameTime()).velocity()).y()+8)<1e-6,"Entry velocity was discarded instead of retained virtually");
            pipeline.prePhysicsTicks();
            for(int step=0;step<500;step++) {
                if(step==50)for(var ship:java.util.List.of(a,b))pipeline.addLinearAndAngularVelocity(ship,new Vector3d(1,0,0),new Vector3d());
                var freeBefore=pipeline.getLinearVelocity(free,new Vector3d());
                var beforeAngular=pipeline.getAngularVelocity(a,new Vector3d());
                io.github.astelev.around_planet.compat.SpaceLocalPhysics.before(system,.02);
                if(step==50)h.assertTrue(Math.abs(pipeline.getLinearVelocity(free,new Vector3d()).x-freeBefore.x+1)<.01,"Free craft missed the opposite instantaneous engine delta-v");
                h.assertTrue(pipeline.getAngularVelocity(a,new Vector3d()).distance(beforeAngular)<1e-9,"Frame velocity removal changed native angular velocity");
                pipeline.physicsTick(.02);for(var ship:ships)system.updatePose(ship);
                var integratedAngular=pipeline.getAngularVelocity(a,new Vector3d());
                io.github.astelev.around_planet.compat.SpaceLocalPhysics.after(system);
                h.assertTrue(pipeline.getAngularVelocity(a,new Vector3d()).distance(integratedAngular)<1e-9,"Recentering changed solver angular velocity");
                if(step==9)h.assertTrue(pipeline.getLinearVelocity(surface,new Vector3d()).y<-.1,"Surface gravity was incorrectly disabled");
            }
            pipeline.postPhysicsTicks();
            var center=new Vector3d();double mass=0;for(var ship:java.util.List.of(a,b)){double m=ship.getSelfMassTracker().getMass();center.fma(m,ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass())));mass+=m;}center.div(mass);
            h.assertTrue(center.distance(new Vector3d(101,1536,80))<.01,"Owner drifted/fell out of its cell: "+center);
            double vx=orbit.navigation(cell.id()).orElseThrow().frame().toLocal(orbit.sample(cell.id(),level.getGameTime()).velocity()).x();h.assertTrue(Math.abs(vx-5)<.02,"Owner thrust was not added exactly once to virtual motion: "+vx);
            h.assertTrue(joint.isValid(),"Frame controller broke the rotary group");
            h.assertTrue(free.logicalPose().position().x<129.9,"Free craft did not move opposite to owner thrust: "+free.logicalPose().position());
            h.assertTrue(Math.abs(pipeline.getLinearVelocity(free,new Vector3d()).y)<.02,"Free craft still experiences surface gravity");
            h.succeed();
        } finally {
            if(cell!=null){io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer()).remove(cell.id());io.github.astelev.around_planet.space.SpaceCells.get(level).remove(level,cell);}
            if(joint!=null && joint.isValid())joint.remove();for(var ship:ships)container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
        }
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void sphericalReentryDetectsFastCrossingAfterManeuverAndRetainsAllBodyMomentum(GameTestHelper h) {
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var pipeline=c.physicsSystem().getPipeline();
        var ships=new ArrayList<ServerSubLevel>();var cells=io.github.astelev.around_planet.space.SpaceCells.get(level);var orbits=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());
        io.github.astelev.around_planet.space.SpaceCells.Cell cell=null;dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle joint=null;
        try {
            for(int i=0;i<3;i++){var p=h.absolutePos(new BlockPos(1+3*i,2,1));level.setBlockAndUpdate(p,Blocks.IRON_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,java.util.List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));ships.add(ship);pipeline.teleport(ship,new Vector3d(i<2?100+2*i:130,1536,80),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();}
            var a=ships.get(0);var b=ships.get(1);var free=ships.get(2);
            joint=pipeline.addConstraint(a,b,new dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration(new Vector3d(a.getSelfMassTracker().getCenterOfMass()).add(1,0,0),new Vector3d(b.getSelfMassTracker().getCenterOfMass()).add(-1,0,0),new Vector3d(0,1,0),new Vector3d(0,1,0)));
            cell=new io.github.astelev.around_planet.space.SpaceCells.Cell(java.util.UUID.randomUUID(),40,101,1536,80,0,736,0,java.util.List.of(a.getUniqueId(),b.getUniqueId()));cells.add(level,cell);
            var planet=new io.github.astelev.around_planet.space.PlanetGeometry(256,256);var frame=io.github.astelev.around_planet.space.SpaceFrame.IDENTITY;
            orbits.navigate(cell.id(),new io.github.astelev.around_planet.space.SpaceNavigation(planet,frame));
            var velocity=planet.frame(90,50).toCosmic(new io.github.astelev.around_planet.space.SpaceVector(30,-1000000,2));
            // A full-shell step followed by a maneuver has already rebased the trajectory epoch.
            // Its latest point is outside and outbound; only the previous observed position detects entry.
            io.github.astelev.around_planet.compat.SpaceReentry.entered(level,cell.id(),planet.position(90,737,50));
            orbits.put(cell.id(),new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(planet.position(90,737,50).scale(-2),velocity),0));
            pipeline.addLinearAndAngularVelocity(free,new Vector3d(3,1,2),new Vector3d(.1,.2,.3));
            var before=new Vector3d(free.logicalPose().position());var oldOmega=pipeline.getAngularVelocity(free,new Vector3d());
            var request=new java.util.HashSet<java.util.UUID>();var cooldown=new java.util.HashMap<java.util.UUID,Long>();
            io.github.astelev.around_planet.compat.SpaceReentry.check(level,c,request,cooldown);
            if(cells.forShip(a.getUniqueId())!=null){
                h.assertTrue(cells.forShip(a.getUniqueId())!=null && a.logicalPose().position().y>1400 && free.logicalPose().position().y>1400,"Reentry moved only part of the region before chunks were ready");
                var held=orbits.reentry(cell.id()).orElseThrow();var saved=orbits.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess());var restored=io.github.astelev.around_planet.space.SpaceOrbitData.load(saved,level.registryAccess());h.assertTrue(restored.navigation(cell.id()).orElseThrow().equals(orbits.navigation(cell.id()).orElseThrow()) && restored.reentry(cell.id()).orElseThrow().equals(held),"Pending reentry/basis did not persist");
                for(int x=-3;x<=12;x++)for(int z=-3;z<=12;z++)level.getChunk(x,z);
                io.github.astelev.around_planet.compat.SpaceReentry.check(level,c,request,cooldown);
            }
            h.assertTrue(cells.forShip(a.getUniqueId())==null && orbits.get(cell.id()).isEmpty(),"Automatic radial reentry did not complete");
            var actualCenter=a.logicalPose().transformPosition(new Vector3d(a.getSelfMassTracker().getCenterOfMass())).add(b.logicalPose().transformPosition(new Vector3d(b.getSelfMassTracker().getCenterOfMass()))).mul(.5);
            h.assertTrue(Math.abs(actualCenter.y-736)<.01,"Reentry altitude is wrong");
            var destination=planet.frame(actualCenter.x,actualCenter.z);var q=io.github.astelev.around_planet.compat.SpaceRotation.between(frame,destination);var boost=destination.toLocal(velocity);
            var expectedV=q.transform(new Vector3d(3,1,2)).add(boost.x(),boost.y(),boost.z());// Rapier stores velocity with float precision: one ULP at this deliberately extreme speed.
            h.assertTrue(pipeline.getLinearVelocity(free,new Vector3d()).distance(expectedV)<=Math.max(1e-6,Math.ulp((float)expectedV.length())),"Free body velocity: actual="+pipeline.getLinearVelocity(free,new Vector3d())+" expected="+expectedV+" error="+pipeline.getLinearVelocity(free,new Vector3d()).distance(expectedV));
            h.assertTrue(pipeline.getAngularVelocity(free,new Vector3d()).distance(q.transform(oldOmega))<1e-6,"Reentry lost angular momentum direction");
            var expectedP=io.github.astelev.around_planet.compat.SpaceRotation.point(before,new Vector3d(101,1536,80),actualCenter,q);h.assertTrue(free.logicalPose().position().distance(expectedP)<.01,"Free body did not preserve its relative layout");
            h.assertTrue(joint.isValid() && cooldown.keySet().containsAll(ships.stream().map(ServerSubLevel::getUniqueId).toList()),"Reentry lost a rotary joint or a free body");h.succeed();
        } finally {if(cell!=null){orbits.remove(cell.id());cells.remove(level,cell);}if(joint!=null && joint.isValid())joint.remove();for(var ship:ships)c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }

    private static dev.ryanhcode.sable.sublevel.SubLevel SableTracking(net.minecraft.world.entity.Entity entity) {
        return dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(entity);
    }

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
