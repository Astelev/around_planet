package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.space.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
@PrefixGameTestTemplate(false)
public final class LunarOrbitGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="lunarPersistence",timeoutTicks=100)
 public static void lunarWarpAndTargetSurviveSaveWithoutLoadingAnyShip(GameTestHelper h){var data=new SpaceOrbitData();var sys=new CelestialSystem(20000,4e9,0);data.system(sys);var id=UUID.randomUUID();long now=1000;double r=sys.moonRadius()*2;var path=new SpaceTrajectory(CelestialSystem.MOON,now,new SpaceState(new SpaceVector(r,0,0),new SpaceVector(0,Math.sqrt(sys.moonMu()/r),0)),sys.moonMu());data.put(id,path);data.target(id,CelestialSystem.MOON_ID);boolean rejected=false;try{data.put(CelestialSystem.MOON_ID,path);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Moon was accepted as an editable vessel orbit");var node=new ManeuverNode(UUID.randomUUID(),now+10000,5,0,0,.15);data.maneuver(id,node);data.warp(now,2400);var copy=SpaceOrbitData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.system().clockTicks()==2400&&copy.target(id).orElseThrow().equals(CelestialSystem.MOON_ID),"Lunar ephemeris/target did not persist");h.assertTrue(copy.get(id).orElseThrow().primary().equals(CelestialSystem.MOON)&&copy.sample(id,now).position().subtract(path.at(now+2400).position()).length()<1e-7,"Dormant lunar coast lost during warp");h.assertTrue(copy.system().moon(now).position().subtract(sys.moon(now+2400).position()).length()<1e-7&&copy.maneuvers(id).getFirst().warningTicks(-1)==3,"Moon and ship clocks diverged");h.succeed();}
}
