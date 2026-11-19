package io.github.astelev.around_planet.fuel;
/** SI mass/energy; volumes are mB. One bucket is 1 m³ for the default density model. */
public record Bipropellant(double fuelKgPerMb,double oxidizerKgPerMb,double oxidizerToFuel,double energyPerKg,double efficiency) {
 public static final Bipropellant HYDROLOX=new Bipropellant(.07085,1.141,8,120_000_000.0/9*.25,.1625);
 public Bipropellant {for(double n:new double[]{fuelKgPerMb,oxidizerKgPerMb,oxidizerToFuel,energyPerKg,efficiency})if(!Double.isFinite(n)||n<=0)throw new IllegalArgumentException("Positive mixture parameters required");if(efficiency>1)throw new IllegalArgumentException("Efficiency > 1");}
 public double exhaustSpeed(){return Math.sqrt(energyPerKg)*Math.sqrt(2*efficiency);}
 public record Burn(double fuel,double oxidizer,double mass,double impulse){}
 public Burn burn(double fuel,double oxidizer,double requestedMass,double effectiveFuelKgPerMb,double effectiveOxidizerKgPerMb){
  for(double n:new double[]{fuel,oxidizer,requestedMass,effectiveFuelKgPerMb,effectiveOxidizerKgPerMb})if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid burn parameters");
  double ratio=oxidizerToFuel*fuelKgPerMb/oxidizerKgPerMb;
  double unitMass=effectiveFuelKgPerMb+ratio*effectiveOxidizerKgPerMb;
  if(!Double.isFinite(unitMass)||unitMass<=0||effectiveFuelKgPerMb<=0||effectiveOxidizerKgPerMb<=0)return new Burn(0,0,0,0);
  double amount=Math.min(Math.min(fuel,oxidizer/ratio),requestedMass/unitMass),ox=amount*ratio,mass=amount*unitMass;
  double impulse=mass*exhaustSpeed();return Double.isFinite(impulse)?new Burn(amount,ox,mass,impulse):new Burn(0,0,0,0);
 }
}
