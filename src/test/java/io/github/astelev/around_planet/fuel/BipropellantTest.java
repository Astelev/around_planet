package io.github.astelev.around_planet.fuel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BipropellantTest {
 private final Bipropellant mix=Bipropellant.HYDROLOX;
 @Test void balancedMixtureTradesFourTimesLegacyFuelForTheSameMaximumThrust(){
  var old=new Bipropellant(.07085,1.141,8,120_000_000.0/9,.65);
  assertEquals(old.exhaustSpeed()/4,mix.exhaustSpeed(),1e-9);
  var oldBurn=old.burn(1000,1000,5,.07085,1.141);
  var newBurn=mix.burn(1000,1000,20,.07085,1.141);
  assertEquals(oldBurn.impulse(),newBurn.impulse(),1e-9);
  assertEquals(oldBurn.fuel()*4,newBurn.fuel(),1e-9);
  assertEquals(oldBurn.oxidizer()*4,newBurn.oxidizer(),1e-9);
  assertEquals(.25*old.exhaustSpeed()*Math.log(3),mix.exhaustSpeed()*Math.log(3),1e-9);
 }
 @Test void momentumAndEnergyFollowMass(){var b=mix.burn(1000,1000,20,.07085,1.141);assertEquals(20,b.mass(),1e-10);assertEquals(8,b.oxidizer()*1.141/(b.fuel()*.07085),1e-12);assertEquals(b.mass()*mix.exhaustSpeed(),b.impulse(),1e-9);assertEquals(mix.energyPerKg()*mix.efficiency(),.5*Math.pow(mix.exhaustSpeed(),2),1e-8);}
 @Test void pondusScaleChangesImpulseForSameVolume(){var a=mix.burn(10,100,1e6,.07085,1.141);var b=mix.burn(10,100,1e6,.0007085,.01141);assertEquals(a.fuel(),b.fuel(),1e-12);assertEquals(a.mass()*.01,b.mass(),1e-12);assertEquals(a.impulse()*.01,b.impulse(),1e-9);}
 @Test void shortageScalesBothComponents(){var b=mix.burn(100,1,1e6,.07085,1.141);assertEquals(1,b.oxidizer(),1e-12);assertEquals(8,b.oxidizer()*1.141/(b.fuel()*.07085),1e-12);}
 @Test void missingComponentCannotProduceThrust(){assertEquals(0,mix.burn(100,0,10,.07085,1.141).impulse());assertEquals(0,mix.burn(0,100,10,.07085,1.141).impulse());assertEquals(0,mix.burn(100,100,10,0,1.141).impulse());}
 @Test void tinyStepsConserveFuelAndImpulse(){double f=100,o=100,impulse=0;for(int i=0;i<10000;i++){var b=mix.burn(f,o,.00001,.07085,1.141);f-=b.fuel();o-=b.oxidizer();impulse+=b.impulse();}var b=mix.burn(100,100,.1,.07085,1.141);assertEquals(b.fuel(),100-f,1e-9);assertEquals(b.oxidizer(),100-o,1e-9);assertEquals(b.impulse(),impulse,1e-7);}
 @Test void invalidInputsRejected(){assertThrows(IllegalArgumentException.class,()->mix.burn(1,1,Double.NaN,1,1));assertThrows(IllegalArgumentException.class,()->new Bipropellant(1,1,8,1,2));}
}
