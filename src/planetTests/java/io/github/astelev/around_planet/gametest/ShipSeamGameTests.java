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
            var observed=rider;boolean listed=java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).anyMatch(e->e==observed);var storage=dev.ryanhcode.sable.Sable.HELPER.getContaining(rider);
            io.github.astelev.around_planet.compat.ShipTransferService.check(level,container);
            h.assertTrue(seat.position().equals(seatPos),"Seat was translated out of its storage plot");
            h.assertTrue(rider.getVehicle()==seat && rider.position().distanceTo(before.add(-512,0,0))<1e-6,"Seated rider mismatch: before="+before+" after="+rider.position()+" ship="+ship.logicalPose().position()+" mounted="+(rider.getVehicle()==seat)+" listed="+listed+" storage="+storage+" seat="+seat.position());
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
            double returnY=io.github.astelev.around_planet.compat.SpaceAtmosphere.returnAltitude();
            var velocity=planet.frame(90,50).toCosmic(new io.github.astelev.around_planet.space.SpaceVector(30,-1000000,2));
            // A full-shell step followed by a maneuver has already rebased the trajectory epoch.
            // Its latest point is outside and outbound; only the previous observed position detects entry.
            io.github.astelev.around_planet.compat.SpaceReentry.entered(level,cell.id(),planet.position(90,returnY+1,50));
            orbits.put(cell.id(),new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(planet.position(90,returnY+1,50).scale(-2),velocity),0));
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
            h.assertTrue(Math.abs(actualCenter.y-returnY)<.01,"Reentry altitude is wrong");
            h.assertTrue(pipeline.getLinearVelocity(a,new Vector3d()).length()<=20.001,"Unsafe owner velocity passed into native reentry");
            var destination=planet.frame(actualCenter.x,actualCenter.z);var q=io.github.astelev.around_planet.compat.SpaceRotation.between(frame,destination);var boost=destination.toLocal(velocity);
            var expectedV=q.transform(new Vector3d(3,1,2)).add(boost.x(),boost.y(),boost.z());// Rapier stores velocity with float precision: one ULP at this deliberately extreme speed.
            h.assertTrue(pipeline.getLinearVelocity(free,new Vector3d()).distance(expectedV)<=Math.max(1e-6,Math.ulp((float)expectedV.length())),"Free body velocity: actual="+pipeline.getLinearVelocity(free,new Vector3d())+" expected="+expectedV+" error="+pipeline.getLinearVelocity(free,new Vector3d()).distance(expectedV));
            h.assertTrue(pipeline.getAngularVelocity(free,new Vector3d()).distance(q.transform(oldOmega))<1e-6,"Reentry lost angular momentum direction");
            var expectedP=io.github.astelev.around_planet.compat.SpaceRotation.point(before,new Vector3d(101,1536,80),actualCenter,q);h.assertTrue(free.logicalPose().position().distance(expectedP)<.01,"Free body did not preserve its relative layout");
            h.assertTrue(joint.isValid() && cooldown.keySet().containsAll(ships.stream().map(ServerSubLevel::getUniqueId).toList()),"Reentry lost a rotary joint or a free body");h.succeed();
        } finally {if(cell!=null){orbits.remove(cell.id());cells.remove(level,cell);}if(joint!=null && joint.isValid())joint.remove();for(var ship:ships)c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void distantFreeRotaryGroupGetsIndependentOrbitWithoutLosingNativeMomentum(GameTestHelper h) {
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var pipeline=c.physicsSystem().getPipeline();var ships=new ArrayList<ServerSubLevel>();
        var cells=io.github.astelev.around_planet.space.SpaceCells.get(level);var orbits=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());
        var oldBorder=PlanetBorders.get(level.getWorldBorder());double oldZ=PlanetBorders.boundaryZ(level.getWorldBorder());
        PlanetBorders.bind(level.getWorldBorder(),new EastWestTopology(131072),65536);
        io.github.astelev.around_planet.space.SpaceCells.Cell parent=null,child=null;dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle joint=null;
        try {
            for(int n=0;n<3;n++){var p=h.absolutePos(new BlockPos(1+3*n,2,1));level.setBlockAndUpdate(p,Blocks.IRON_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,java.util.List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));ships.add(ship);pipeline.teleport(ship,new Vector3d(n==0?0:191+2*n,1536,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();}
            var owner=ships.get(0);var a=ships.get(1);var b=ships.get(2);
            joint=pipeline.addConstraint(a,b,new dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration(new Vector3d(a.getSelfMassTracker().getCenterOfMass()).add(1,0,0),new Vector3d(b.getSelfMassTracker().getCenterOfMass()).add(-1,0,0),new Vector3d(0,1,0),new Vector3d(0,1,0)));
            parent=new io.github.astelev.around_planet.space.SpaceCells.Cell(java.util.UUID.randomUUID(),40,0,1536,0,0,736,0,java.util.List.of(owner.getUniqueId()));cells.add(level,parent);
            var planet=new io.github.astelev.around_planet.space.PlanetGeometry(65536,65536);var frame=io.github.astelev.around_planet.space.SpaceFrame.IDENTITY;
            var trajectory=new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(0,planet.radius()+1000,0),new io.github.astelev.around_planet.space.SpaceVector(20,30,40)),0);orbits.put(parent.id(),trajectory);orbits.navigate(parent.id(),new io.github.astelev.around_planet.space.SpaceNavigation(planet,frame));
            for(var ship:java.util.List.of(a,b))pipeline.addLinearAndAngularVelocity(ship,new Vector3d(2,3,4),new Vector3d(.1,.2,.3));var angular=pipeline.getAngularVelocity(a,new Vector3d());
            io.github.astelev.around_planet.compat.SpaceDetachment.check(level,c);
            if(cells.forShip(a.getUniqueId())==null){h.assertTrue(a.logicalPose().position().x==193 && b.logicalPose().position().x==195 && orbits.ids().contains(parent.id()),"Partial group moved before destination readiness");for(int x=-66;x<=-62;x++)for(int z=-66;z<=-62;z++)level.getChunk(x,z);io.github.astelev.around_planet.compat.SpaceDetachment.check(level,c);}
            child=cells.forShip(a.getUniqueId());h.assertTrue(child!=null && !child.id().equals(parent.id()) && cells.forShip(b.getUniqueId()).id().equals(child.id()),"Distant free group was not promoted together");
            var state=orbits.sample(child.id(),level.getGameTime());h.assertTrue(state.position().subtract(trajectory.initial().position().add(new io.github.astelev.around_planet.space.SpaceVector(194,0,0))).length()<1e-6,"Detached cosmic position lost the local offset");
            h.assertTrue(state.velocity().subtract(new io.github.astelev.around_planet.space.SpaceVector(22,33,44)).length()<1e-6,"Detached object lost inherited/relative velocity");
            h.assertTrue(pipeline.getLinearVelocity(a,new Vector3d()).length()<1e-6 && pipeline.getAngularVelocity(a,new Vector3d()).distance(angular)<1e-6 && joint.isValid(),"New fixed frame lost native angular motion or joint identity");
            h.assertTrue(Math.abs(a.logicalPose().position().distance(b.logicalPose().position())-2)<1e-6,"Detachment changed connected body layout");
            orbits.impulse(parent.id(),level.getGameTime(),new io.github.astelev.around_planet.space.SpaceVector(100,0,0));h.assertTrue(orbits.sample(child.id(),level.getGameTime()).equals(state),"Parent maneuver changed the detached orbit");
            var restored=io.github.astelev.around_planet.space.SpaceOrbitData.load(orbits.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());h.assertTrue(restored.sample(child.id(),level.getGameTime()+200).equals(orbits.sample(child.id(),level.getGameTime()+200)),"Independent coasting state did not survive persistence");h.succeed();
        }finally {if(parent!=null){orbits.remove(parent.id());cells.remove(level,parent);}if(child!=null){orbits.remove(child.id());cells.remove(level,child);}if(joint!=null && joint.isValid())joint.remove();for(var ship:ships)c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);io.github.astelev.around_planet.compat.SpaceDetachment.clear(level);if(oldBorder==null)PlanetBorders.unbind(level.getWorldBorder());else PlanetBorders.bind(level.getWorldBorder(),oldBorder,oldZ);}
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void disconnectedOwnerChildBecomesFreeWithoutJumpingCosmicPosition(GameTestHelper h) {
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var pipeline=c.physicsSystem().getPipeline();var ships=new ArrayList<ServerSubLevel>();
        var cells=io.github.astelev.around_planet.space.SpaceCells.get(level);var data=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());io.github.astelev.around_planet.space.SpaceCells.Cell cell=null;
        try {
            for(int n=0;n<2;n++){var p=h.absolutePos(new BlockPos(1+3*n,2,1));level.setBlockAndUpdate(p,Blocks.IRON_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,java.util.List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));ships.add(ship);pipeline.teleport(ship,new Vector3d(-1+2*n,1536,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();}
            cell=new io.github.astelev.around_planet.space.SpaceCells.Cell(java.util.UUID.randomUUID(),40,0,1536,0,0,736,0,ships.stream().map(ServerSubLevel::getUniqueId).toList());cells.add(level,cell);
            var position=new io.github.astelev.around_planet.space.SpaceVector(0,22000,0);data.put(cell.id(),new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(position,io.github.astelev.around_planet.space.SpaceVector.ZERO),0));data.navigate(cell.id(),new io.github.astelev.around_planet.space.SpaceNavigation(new io.github.astelev.around_planet.space.PlanetGeometry(65536,65536),io.github.astelev.around_planet.space.SpaceFrame.IDENTITY));
            // Original connected-group membership remains saved after its joint has disappeared.
            io.github.astelev.around_planet.compat.SpaceDetachment.check(level,c);
            var current=cells.forShip(ships.getFirst().getUniqueId());h.assertTrue(current.ships().size()==1 && cells.forShip(ships.getLast().getUniqueId())==null,"Disconnected child remained part of the fixed owner");
            var origin=data.sample(cell.id(),level.getGameTime()).position();var child=ships.getLast().logicalPose().position();
            h.assertTrue(origin.add(new io.github.astelev.around_planet.space.SpaceVector(child.x,child.y-1536,child.z)).subtract(position.add(new io.github.astelev.around_planet.space.SpaceVector(1,0,0))).length()<1e-6,"Owner rebasing changed the free child's absolute cosmic location");
            h.assertTrue(ships.getFirst().logicalPose().position().distance(new Vector3d(0,1536,0))<1e-6 && io.github.astelev.around_planet.compat.SpaceDetachment.associated(level,ships.getLast().getUniqueId()),"Split owner did not retain a local free-body association");h.succeed();
        } finally {if(cell!=null){data.remove(cell.id());cells.remove(level,cell);}for(var ship:ships)c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);io.github.astelev.around_planet.compat.SpaceDetachment.clear(level);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void planetResizePreservesAltitudeVelocityAndPendingContact(GameTestHelper h){
        var level=h.getLevel();var data=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());var a=java.util.UUID.randomUUID();var b=java.util.UUID.randomUUID();int old=io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get();
        try {var planet=new io.github.astelev.around_planet.space.PlanetGeometry(65536,65536);var nav=new io.github.astelev.around_planet.space.SpaceNavigation(planet,planet.frame(50,60));var v=new io.github.astelev.around_planet.space.SpaceVector(10,20,30);double mu=9.81*planet.radius()*planet.radius();
            for(var id:java.util.List.of(a,b)){data.put(id,new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(planet.position(50,1000,60),v),mu));data.navigate(id,nav);}data.reenter(b,planet.position(50,736,60),level.getGameTime());
            io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.set(262144);io.github.astelev.around_planet.compat.SpacePlanetScale.clear(level);io.github.astelev.around_planet.compat.SpacePlanetScale.check(level);var next=new io.github.astelev.around_planet.space.PlanetScale(262144).geometry();
            h.assertTrue(Math.abs(data.sample(a,level.getGameTime()).position().length()-next.radius()-1000)<1e-6 && data.sample(a,level.getGameTime()).velocity().equals(v),"Planet resize changed altitude or inertial velocity");
            h.assertTrue(Math.abs(data.get(a).orElseThrow().mu()/mu-4)<1e-9 && data.navigation(a).orElseThrow().planet().equals(next),"Radius and gravitational parameter did not share the planet size");
            h.assertTrue(Math.abs(data.sample(b,level.getGameTime()).position().length()-next.radius()-736)<1e-6 && data.reentry(b).isPresent(),"Resize lost a pending crossing");h.succeed();
        }finally{data.remove(a);data.remove(b);io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.set(old);io.github.astelev.around_planet.compat.SpacePlanetScale.clear(level);}
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void unloadedReentryHoldsContactWithoutLoadingDestinationOrMovingStorage(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var cells=io.github.astelev.around_planet.space.SpaceCells.get(level);var data=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());
        var id=java.util.UUID.randomUUID();var missingBody=java.util.UUID.randomUUID();var planet=new io.github.astelev.around_planet.space.PlanetGeometry(65536,65536);
        var cell=new io.github.astelev.around_planet.space.SpaceCells.Cell(id,40,0,1536,0,13000,736,24000,java.util.List.of(missingBody));var column=new net.minecraft.world.level.ChunkPos(13000>>4,24000>>4);
        h.assertTrue(level.getChunkSource().getChunkNow(column.x,column.z)==null,"Dormant destination was already loaded");
        double returnY=io.github.astelev.around_planet.compat.SpaceAtmosphere.returnAltitude();
        try {cells.add(level,cell);var velocity=planet.frame(13000,24000).up().scale(-20);
            data.navigate(id,new io.github.astelev.around_planet.space.SpaceNavigation(planet,planet.frame(13000,24000)));
            data.put(id,new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(planet.position(13000,returnY-1,24000),velocity),0));
            io.github.astelev.around_planet.compat.SpaceReentry.check(level,c,new java.util.HashSet<>(),new java.util.HashMap<>());
            h.assertTrue(data.reentry(id).isPresent() && Math.abs(data.sample(id,level.getGameTime()).position().length()-planet.radius()-returnY)<1e-6,"Unloaded inbound record did not latch the surface contact");
            h.assertTrue(level.getChunkSource().getChunkNow(column.x,column.z)==null && c.getSubLevel(missingBody)==null,"Dormant return forced destination chunks or a native body to load");
            h.assertTrue(cells.forShip(missingBody).equals(cell) && data.sample(id,level.getGameTime()+200).velocity().equals(velocity),"Current dormant return unexpectedly relocated/removed its storage frame or lost velocity");h.succeed();
        }finally{data.remove(id);cells.remove(level,cell);io.github.astelev.around_planet.compat.SpaceReentry.clear(level);}
    }

    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void nativeUnloadPromotesFastFreeBodyButNotSlowBodyOrOwner(GameTestHelper h){
        var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var pipeline=c.physicsSystem().getPipeline();var ships=new ArrayList<ServerSubLevel>();
        var cells=io.github.astelev.around_planet.space.SpaceCells.get(level);var orbits=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());var registry=io.github.astelev.around_planet.space.SpaceDormantData.get(level.getServer());
        var previousBorder=PlanetBorders.get(level.getWorldBorder());double previousZ=PlanetBorders.boundaryZ(level.getWorldBorder());PlanetBorders.bind(level.getWorldBorder(),new EastWestTopology(131072),65536);
        io.github.astelev.around_planet.space.SpaceCells.Cell parent=null,child=null;java.util.UUID dormantId=null;double oldThreshold=io.github.astelev.around_planet.PlanetConfig.SPACE_UNLOAD_SPEED.get();
        try {io.github.astelev.around_planet.PlanetConfig.SPACE_UNLOAD_SPEED.set(1.0);
            for(int i=0;i<3;i++){var pos=h.absolutePos(new BlockPos(1+3*i,2,1));level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,pos,java.util.List.of(pos),new BoundingBox3i(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ()));ships.add(ship);pipeline.teleport(ship,new Vector3d(i*40,1536,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();}
            var owner=ships.get(0);var fast=ships.get(1);var slow=ships.get(2);
            parent=new io.github.astelev.around_planet.space.SpaceCells.Cell(java.util.UUID.randomUUID(),40,0,1536,0,0,736,0,java.util.List.of(owner.getUniqueId()));cells.add(level,parent);
            var planet=new io.github.astelev.around_planet.space.PlanetGeometry(65536,65536);var frame=io.github.astelev.around_planet.space.SpaceFrame.IDENTITY;
            var initial=new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(0,planet.radius()+1000,0),new io.github.astelev.around_planet.space.SpaceVector(20,30,40));
            orbits.put(parent.id(),new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),initial,0));orbits.navigate(parent.id(),new io.github.astelev.around_planet.space.SpaceNavigation(planet,frame));
            pipeline.addLinearAndAngularVelocity(fast,new Vector3d(2,0,0),new Vector3d(.1,.2,.3));pipeline.addLinearAndAngularVelocity(slow,new Vector3d(.1,0,0),new Vector3d());
            var target=new net.minecraft.world.level.ChunkPos(-64,-64);boolean wasLoaded=level.getChunkSource().getChunkNow(target.x,target.z)!=null;
            c.getHoldingChunkMap().moveToUnloaded(fast,new net.minecraft.world.level.ChunkPos(2,0));
            var binding=registry.forShip(fast.getUniqueId());h.assertTrue(binding!=null,"Real native unload did not create an independent record below distance threshold");dormantId=binding.orbit();
            h.assertTrue(c.getSubLevel(fast.getUniqueId())==null && c.getHoldingChunkMap().getHoldingSubLevel(fast.getUniqueId())!=null,"Fixture did not enter native holding storage");
            h.assertTrue(cells.all().size()==1 && (level.getChunkSource().getChunkNow(target.x,target.z)!=null)==wasLoaded,"Dormant capture allocated/loaded an active destination");
            var state=orbits.sample(dormantId,level.getGameTime());h.assertTrue(state.position().subtract(initial.position().add(new io.github.astelev.around_planet.space.SpaceVector(40,0,0))).length()<1e-6 && state.velocity().subtract(new io.github.astelev.around_planet.space.SpaceVector(22,30,40)).length()<1e-6,"Unload lost relative or inherited motion");
            c.getHoldingChunkMap().moveToUnloaded(slow,new net.minecraft.world.level.ChunkPos(5,0));h.assertTrue(registry.forShip(slow.getUniqueId())==null,"Slow free body was unnecessarily promoted");
            io.github.astelev.around_planet.compat.SpaceDormant.capture(owner,java.util.List.of(owner.getUniqueId()));h.assertTrue(registry.forShip(owner.getUniqueId())==null,"Owner was detached by unload speed policy");
            var restoredData=io.github.astelev.around_planet.space.SpaceDormantData.load(registry.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());h.assertTrue(restoredData.forShip(fast.getUniqueId()).equals(binding),"Dormant binding did not survive save/load");
            var held=c.getHoldingChunkMap().getHoldingSubLevel(fast.getUniqueId());c.getHoldingChunkMap().loadHoldingSubLevel(held);var restored=(ServerSubLevel)c.getSubLevel(fast.getUniqueId());h.assertTrue(restored!=null,"Native holding record did not reload");
            io.github.astelev.around_planet.compat.SpaceDormant.prepare(c);io.github.astelev.around_planet.compat.SpaceDormant.check(level,c);
            if(cells.forShip(restored.getUniqueId())==null){for(int x=-66;x<=-62;x++)for(int z=-66;z<=-62;z++)level.getChunk(x,z);io.github.astelev.around_planet.compat.SpaceDormant.check(level,c);}
            child=cells.forShip(restored.getUniqueId());h.assertTrue(child!=null && child.id().equals(dormantId) && registry.forShip(restored.getUniqueId())==null,"Observed dormant object did not acquire its own active frame");
            h.assertTrue(pipeline.getLinearVelocity(restored,new Vector3d()).length()<1e-6 && orbits.sample(dormantId,level.getGameTime()).velocity().subtract(state.velocity()).length()<1e-6,"Activation counted stored motion twice");
            h.assertTrue(pipeline.getAngularVelocity(restored,new Vector3d()).distance(new Vector3d(.1,.2,.3))<1e-6,"Native angular motion was lost on reactivation");h.succeed();
        } finally {if(dormantId!=null){registry.remove(dormantId);orbits.remove(dormantId);}if(parent!=null){orbits.remove(parent.id());cells.remove(level,parent);}if(child!=null)cells.remove(level,child);
            for(var old:ships){var current=c.getSubLevel(old.getUniqueId());if(current!=null)c.removeSubLevel(current,SubLevelRemovalReason.REMOVED);else{var holding=c.getHoldingChunkMap().getHoldingSubLevel(old.getUniqueId());if(holding!=null){c.getHoldingChunkMap().loadHoldingSubLevel(holding);var reloaded=c.getSubLevel(old.getUniqueId());if(reloaded!=null)c.removeSubLevel(reloaded,SubLevelRemovalReason.REMOVED);}}}
            io.github.astelev.around_planet.compat.SpaceDormant.clear(level);io.github.astelev.around_planet.PlanetConfig.SPACE_UNLOAD_SPEED.set(oldThreshold);if(previousBorder==null)PlanetBorders.unbind(level.getWorldBorder());else PlanetBorders.bind(level.getWorldBorder(),previousBorder,previousZ);
        }
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
