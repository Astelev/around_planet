package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.solid.*;
public final class SolidNativeMass {
    public static void changed(SolidColumnBlockEntity be,double delta){if(delta==0)return;var body=dev.ryanhcode.sable.Sable.HELPER.getContaining(be.getLevel(),be.getBlockPos());if(body instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel ship&&!ship.isRemoved())ship.getSelfMassTracker().addBlockMass(be.getLevel(),be.getBlockState(),be.getBlockPos(),delta,dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getInertia(be.getLevel(),be.getBlockPos(),be.getBlockState()));}
    public static void assembled(dev.ryanhcode.sable.sublevel.ServerSubLevel ship,SolidColumnBlockEntity be){if(io.github.astelev.around_planet.fuel.FuelMassBridge.pondus()){try{Class.forName("org.fractalinteractive.ci.pondus_inventory_fi.listener.InventoryChangeListener").getMethod("onInventoryChanged",dev.ryanhcode.sable.sublevel.ServerSubLevel.class,net.minecraft.core.BlockPos.class,net.minecraft.world.level.block.entity.BlockEntity.class).invoke(null,ship,be.getBlockPos(),be);}catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot synchronize solid inventory",e);}}else changed(be,be.mass());ship.updateMergedMassData(0);}
    public static boolean attached(net.minecraft.world.level.block.entity.BlockEntity be){return dev.ryanhcode.sable.Sable.HELPER.getContaining(be.getLevel(),be.getBlockPos())!=null;}
    private SolidNativeMass(){}
}
