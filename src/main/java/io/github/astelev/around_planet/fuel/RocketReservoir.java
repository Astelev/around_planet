package io.github.astelev.around_planet.fuel;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
/** Two persistent precise chambers. External integer-mB transfers never round stored fuel upward. */
public final class RocketReservoir implements IFluidHandler {
 public static final int CAPACITY=4000;
 private double fuel,oxidizer;private final java.util.function.BiConsumer<Double,Double> changed;
 public RocketReservoir(java.util.function.BiConsumer<Double,Double> changed){this.changed=changed;}
 public double fuel(){return fuel;}public double oxidizer(){return oxidizer;}
 public void restore(double f,double o){fuel=valid(f);oxidizer=valid(o);}
 private static double valid(double n){return Double.isFinite(n)?Math.clamp(n,0,CAPACITY):0;}
 public Bipropellant.Burn burn(double requested,double fuelMass,double oxMass){var burn=Bipropellant.HYDROLOX.burn(fuel,oxidizer,requested,fuelMass,oxMass);if(burn.mass()>0){fuel=Math.max(0,fuel-burn.fuel());oxidizer=Math.max(0,oxidizer-burn.oxidizer());changed.accept(-burn.fuel(),-burn.oxidizer());}return burn;}
 @Override public int getTanks(){return 2;}
 @Override public int getTankCapacity(int tank){return tank>=0&&tank<2?CAPACITY:0;}
 @Override public FluidStack getFluidInTank(int tank){return tank==0?new FluidStack(CryogenicFluids.HYDROGEN.get(),(int)Math.floor(fuel)):tank==1?new FluidStack(CryogenicFluids.OXYGEN.get(),(int)Math.floor(oxidizer)):FluidStack.EMPTY;}
 @Override public boolean isFluidValid(int tank,FluidStack stack){return !stack.isEmpty()&&(tank==0&&stack.getFluid().isSame(CryogenicFluids.HYDROGEN.get())||tank==1&&stack.getFluid().isSame(CryogenicFluids.OXYGEN.get()));}
 @Override public int fill(FluidStack stack,FluidAction action){int tank=isFluidValid(0,stack)?0:isFluidValid(1,stack)?1:-1;if(tank<0)return 0;double stored=tank==0?fuel:oxidizer;int amount=Math.min(stack.getAmount(),(int)Math.floor(CAPACITY-stored));if(amount>0&&action.execute()){if(tank==0)fuel+=amount;else oxidizer+=amount;changed.accept(tank==0?(double)amount:0.0,tank==1?(double)amount:0.0);}return amount;}
 @Override public FluidStack drain(FluidStack stack,FluidAction action){if(stack.isEmpty())return FluidStack.EMPTY;int tank=isFluidValid(0,stack)?0:isFluidValid(1,stack)?1:-1;return tank<0?FluidStack.EMPTY:drainTank(tank,stack.getAmount(),action);}
 @Override public FluidStack drain(int amount,FluidAction action){return drainTank(fuel>=1?0:1,amount,action);}
 private FluidStack drainTank(int tank,int request,FluidAction action){int n=Math.min(Math.max(0,request),(int)Math.floor(tank==0?fuel:oxidizer));if(n==0)return FluidStack.EMPTY;var result=getFluidInTank(tank).copyWithAmount(n);if(action.execute()){if(tank==0)fuel-=n;else oxidizer-=n;changed.accept(tank==0?-(double)n:0.0,tank==1?-(double)n:0.0);}return result;}
}
