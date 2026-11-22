package io.github.astelev.around_planet.propulsion;
import io.github.astelev.around_planet.fuel.FuelMassBridge;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
/** Whole mB are paid before thrust. Unspent prepaid impulse survives substeps and reloads. */
public final class RsuReservoir {
 public static final int CAPACITY=8000;
 public static final double EXHAUST_SPEED=800;
 private final PropulsionBlockEntity owner;
 private double credit;private int lastAmount;
 private net.minecraft.world.level.material.Fluid lastFluid=net.minecraft.world.level.material.Fluids.EMPTY;
 public final FluidTank tank=new FluidTank(CAPACITY){@Override protected void onContentsChanged(){changed();}};
 public RsuReservoir(PropulsionBlockEntity owner){this.owner=owner;}
 private void changed(){int now=tank.getFluidAmount();if(owner.getLevel()!=null&&!owner.getLevel().isClientSide){if(!tank.isEmpty())lastFluid=tank.getFluid().getFluid();if(lastFluid!=net.minecraft.world.level.material.Fluids.EMPTY)lastDensity=FuelMassBridge.rsuKgPerMb(owner,lastFluid);if(net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.astelev.around_planet.compat.RocketMassChange.changed(owner,(now-lastAmount)*lastDensity);}lastAmount=now;owner.setChanged();}
 private double lastDensity;
 public double consume(double requestedImpulse){
  if(!Double.isFinite(requestedImpulse)||requestedImpulse<=0)return 0;
  double paid=Math.min(requestedImpulse,credit);credit-=paid;double needed=requestedImpulse-paid;
  if(needed>0&&!tank.isEmpty()){
   double perMb=FuelMassBridge.rsuKgPerMb(owner,tank.getFluid().getFluid())*EXHAUST_SPEED;
   if(perMb>0){lastDensity=perMb/EXHAUST_SPEED;int amount=(int)Math.min(tank.getFluidAmount(),Math.ceil(needed/perMb));int drained=tank.drain(amount,FluidAction.EXECUTE).getAmount();double bought=drained*perMb;double used=Math.min(needed,bought);paid+=used;credit+=Math.max(0,bought-used);}
  }
  if(paid>0)owner.setChanged();return paid;
 }
 public double credit(){return credit;}
 public void save(CompoundTag n,HolderLookup.Provider r){n.put("RsuFluid",tank.writeToNBT(r,new CompoundTag()));n.putDouble("RsuCredit",credit);}
 public void load(CompoundTag n,HolderLookup.Provider r){tank.readFromNBT(r,n.getCompound("RsuFluid"));lastAmount=tank.getFluidAmount();lastFluid=tank.getFluid().getFluid();credit=Double.isFinite(n.getDouble("RsuCredit"))?Math.max(0,n.getDouble("RsuCredit")):0;}
}
