package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MoonSurfaceTest {
 @Test void lunarChartRoundTripsPositionAndPreservesVelocityWithoutAtmosphere(){var sys=new CelestialSystem(20860,4e9,0);var planet=MoonSurface.geometry(sys);assertEquals(sys.moonRadius(),planet.radius(),1e-8);for(double x:new double[]{-15000,0,12000})for(double z:new double[]{-2000,0,2000}){var p=planet.position(x,800,z);var surface=planet.surface(p);assertEquals(planet.wrapX(x),surface.x(),1e-7);assertEquals(800,surface.y(),1e-7);assertEquals(z,surface.z(),1e-7);var frame=planet.frame(surface.x(),surface.z());var velocity=new SpaceVector(20,-90,35);assertTrue(frame.toCosmic(frame.toLocal(velocity)).subtract(velocity).length()<1e-8);}}
 @Test void descendingShellCrossingTriggersButOutboundDoesNot(){var p=new PlanetGeometry(18000,9000);double r=p.radius()+800;var velocity=new SpaceVector(-100,0,0);var contact=MoonSurface.contact(p,new SpaceVector(r+10,0,0),new SpaceState(new SpaceVector(r-10,0,0),velocity),800);assertNotNull(contact);assertEquals(r,contact.length(),1e-8);assertNull(MoonSurface.contact(p,new SpaceVector(r-10,0,0),new SpaceState(new SpaceVector(r+10,0,0),velocity.scale(-1)),800));}
}
