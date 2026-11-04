package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;

final class SableCollisionRegression {
    static void verify(ServerLevel level, Portal east) throws NoSuchMethodException {
        if (net.neoforged.fml.ModList.get().isLoaded("sable")) {
            var sheep=net.minecraft.world.entity.EntityType.SHEEP.create(level);
            var info=net.minecraft.world.entity.Entity.class.getMethod("sable$getCollisionInfo");
            level.setBlockAndUpdate(new net.minecraft.core.BlockPos(254,79,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            sheep.moveTo(254.5,80,0.5);
            sheep.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-0.08,0));
            try {
                var fresh=net.minecraft.world.entity.EntityType.SHEEP.create(level);
                fresh.moveTo(255.8,82,0.5);
                ((qouteall.imm_ptl.core.ducks.IEEntity)fresh).ip_notifyCollidingWithPortal(east);
                fresh.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0.1,-0.08,0));
                require(info.invoke(fresh)==null,"Fresh sheep did not exercise bypassed Sable collision");
                require(info.invoke(sheep)!=null && sheep.verticalCollision,"Ordinary Sable collision path was lost");
                sheep.moveTo(255.8,82,0.5);
                ((qouteall.imm_ptl.core.ducks.IEEntity)sheep).ip_notifyCollidingWithPortal(east);
                sheep.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0.1,-0.08,0));
                require(info.invoke(sheep)==null,"Portal short-circuit reused old Sable collision state");
                require(!sheep.verticalCollision,"Old grounded flags leaked into portal movement");
                AroundPlanet.LOGGER.info("SABLE_COLLISION_REGRESSION PASS: fresh sheep, stale state, ordinary ground collision");
            } catch (ReflectiveOperationException exception) {throw new AssertionError(exception);}
        }

    }
    private static void require(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
