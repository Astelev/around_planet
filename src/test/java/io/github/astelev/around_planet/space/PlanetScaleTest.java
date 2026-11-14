package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlanetScaleTest {
    @Test void oneCircumferenceSetsBothBordersAndCosmicRadius(){var p=new PlanetScale(131072);assertEquals(65536,p.boundary());assertEquals(131072,2*Math.PI*p.radius(),1e-8);assertEquals(p.radius(),p.geometry().radius(),1e-9);assertEquals(p.geometry().halfX(),p.geometry().halfZ());}
    @Test void changingSizeKeepsChunkAlignedSeams(){for(int c:new int[]{256,512,131072,59000000}){var p=new PlanetScale(c);assertEquals(0,p.boundary()%16);assertEquals(c/2,p.boundary());}}
    @Test void invalidSizesCannotSilentlyDisableTerrainSeams(){for(int c:new int[]{0,255,257,131073,59000032})assertThrows(IllegalArgumentException.class,()->new PlanetScale(c));}
}
