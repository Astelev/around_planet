package io.github.astelev.around_planet.space;
import dev.devce.rocketnautics.shaded.orekit.orbits.CartesianOrbit;
import dev.devce.rocketnautics.shaded.orekit.frames.Frame;
import dev.devce.rocketnautics.shaded.orekit.time.AbsoluteDate;
import dev.devce.rocketnautics.shaded.orekit.utils.PVCoordinates;
import dev.devce.rocketnautics.shaded.hipparchus.geometry.euclidean.threed.Vector3D;
import java.util.Random;
public class OrbitOracle {
 public static void main(String[] args) {
  var random=new Random(531);double maxPosition=0,maxVelocity=0;int count=0;
  double mu=3.986004418e14;
  for(int i=0;i<600;i++) {
   var p=new SpaceVector(7e6+random.nextDouble()*5e7,random.nextDouble()*1e6,random.nextDouble()*1e6);
   var v=new SpaceVector((random.nextDouble()-.5)*2000,Math.sqrt(mu/p.length())*(.3+random.nextDouble()*2.5),random.nextDouble()*3000);
   var initial=new SpaceState(p,v);
   var reference=new CartesianOrbit(new PVCoordinates(new Vector3D(p.x(),p.y(),p.z()),new Vector3D(v.x(),v.y(),v.z())),Frame.getRoot(),AbsoluteDate.ARBITRARY_EPOCH,mu);
   for(double dt:new double[]{-100000,-30,30,100000}) {
    var expected=reference.shiftedBy(dt).getPVCoordinates();var got=KeplerMotion.at(initial,mu,dt);
    double dp=got.position().subtract(new SpaceVector(expected.getPosition().getX(),expected.getPosition().getY(),expected.getPosition().getZ())).length();
    double dv=got.velocity().subtract(new SpaceVector(expected.getVelocity().getX(),expected.getVelocity().getY(),expected.getVelocity().getZ())).length();
    if(dp>.1 || dv>1e-4)throw new AssertionError("Oracle mismatch sample="+i+" dt="+dt+" dp="+dp+" dv="+dv);
    maxPosition=Math.max(maxPosition,dp);maxVelocity=Math.max(maxVelocity,dv);count++;
   }
  }
  System.out.println("OREKIT_ORACLE PASS samples="+count+" maxPositionError="+maxPosition+" maxVelocityError="+maxVelocity);
 }
}
