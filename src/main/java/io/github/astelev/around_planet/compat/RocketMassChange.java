package io.github.astelev.around_planet.compat;
/** Pondus updates its own cache. Without it, change only this block's native mass contribution. */
public final class RocketMassChange {
 public static void changed(io.github.astelev.around_planet.propulsion.PropulsionBlockEntity be,double delta){if(delta==0||be.getLevel()==null||be.getLevel().isClientSide||io.github.astelev.around_planet.fuel.FuelMassBridge.pondus())return;var body=dev.ryanhcode.sable.Sable.HELPER.getContaining(be.getLevel(),be.getBlockPos());if(body instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel ship&&!ship.isRemoved())ship.getSelfMassTracker().addBlockMass(be.getLevel(),be.getBlockState(),be.getBlockPos(),delta,dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getInertia(be.getLevel(),be.getBlockPos(),be.getBlockState()));}
 private RocketMassChange(){}
}
