package io.github.astelev.around_planet.moon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class MoonTerrainTest {
 private final MoonTerrain t=new MoonTerrain(131072*.2724,MoonTerrain.worldSeed(12345));
 @Test void allFourLandformsAndPolarIceExist(){var seen=EnumSet.noneOf(MoonTerrain.Region.class);int ice=0;for(int z=0;z<128;z++)for(int x=0;x<256;x++){var s=t.sample((x/256.0-.5)*t.circumference(),(z/128.0-.5)*t.circumference()/2);seen.add(s.region());if(s.ice())ice++;}assertEquals(4,seen.size());assertTrue(ice>0);}
 @Test void mareIsDryFlatNearSixtyWhilePlainsHaveRelief(){int mare=0,min=300,max=-64;for(int z=-8000;z<8000;z+=87)for(int x=-16000;x<16000;x+=91){var s=t.sample(x,z);if(s.region()==MoonTerrain.Region.MARE){mare++;assertTrue(s.height()>=50&&s.height()<=70,"Mare at "+s.height());}if(s.region()==MoonTerrain.Region.PLAINS){min=Math.min(min,s.height());max=Math.max(max,s.height());}}assertTrue(mare>100);assertTrue(max-min>40);}
 @Test void craterHasDepressedFloorRaisedRingAndConditionalCentralPeak(){assertTrue(MoonTerrain.relief(.4,400)<-20);assertTrue(MoonTerrain.relief(1,400)>50);assertTrue(MoonTerrain.relief(0,400)>MoonTerrain.relief(.4,400)+30);assertTrue(MoonTerrain.relief(0,200)<=MoonTerrain.relief(.4,200));assertTrue(Math.abs(MoonTerrain.relief(1.45,400))<.01);}
 @Test void eastWestSeamIsPeriodicIncludingIceAndBiomes(){for(double z:new double[]{-8000,-1000,0,4999,8500})for(double x:new double[]{-17853.1,-700,0,.1,17852.9}){assertEquals(t.sample(x,z),t.sample(x+t.circumference(),z));assertEquals(t.sample(x,z),t.sample(x-t.circumference(),z));}}
 @Test void polarIceIsOnlyInsideHighLatitudeBasins(){for(int z=-8900;z<=8900;z+=90)for(int x=-17000;x<17000;x+=110){var s=t.sample(x,z);if(s.ice()){assertEquals(MoonTerrain.Region.BASIN,s.region());assertTrue(Math.abs(z)>t.circumference()*.205);}}}
 @Test void deterministicWorldSeedsChangeSurface(){var copy=new MoonTerrain(t.circumference(),t.seed());var other=new MoonTerrain(t.circumference(),MoonTerrain.worldSeed(54321));int changed=0;for(int i=0;i<100;i++){assertEquals(t.sample(i*99,i*47),copy.sample(i*99,i*47));if(!t.sample(i*99,i*47).equals(other.sample(i*99,i*47)))changed++;}assertTrue(changed>70);}
 @Test void overlappingCratersDoNotCreateHeightDiscontinuities(){var random=new Random(72);for(int i=0;i<10000;i++){double x=(random.nextDouble()-.5)*t.circumference(),z=(random.nextDouble()-.5)*t.circumference()/2;assertTrue(Math.abs(t.sample(x,z).height()-t.sample(x+1,z).height())<=6,"Abrupt crater overlap at "+x+","+z);}}
 @Test void orbitalCoordinatesAgreeWithGroundChart(){for(double lon:new double[]{-2.1,-.4,0,1,2.9})for(double lat:new double[]{-1.4,-.1,.4,1.4}){var s=t.sample(lon/(2*Math.PI)*t.circumference(),lat/(2*Math.PI)*t.circumference());assertEquals(MoonTerrain.color(s),t.color(Math.sin(lon)*Math.cos(lat),Math.cos(lon)*Math.cos(lat),Math.sin(lat)));}}
}
