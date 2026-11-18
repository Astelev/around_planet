package io.github.astelev.around_planet.plasma;

import io.github.astelev.around_planet.space.SpaceVector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlasmaTest {
    @Test void thresholdAndVacuumAreStrictlyDark() {
        for (double v : new double[]{0, 100, 199.999, 200}) assertEquals(0, PlasmaMath.intensity(v, 1));
        assertEquals(0, PlasmaMath.intensity(2000, 0));
        assertEquals(0, PlasmaMath.intensity(Double.NaN, 1));
        assertTrue(PlasmaMath.intensity(200.001, 1) > 0);
    }
    @Test void pressureIsQuadraticAndDensityScalesIt() {
        double a = -Math.log1p(-PlasmaMath.intensity(300, .05));
        double b = -Math.log1p(-PlasmaMath.intensity(400, .05));
        assertEquals((400.0*400-200*200)/(300.0*300-200*200), b/a, 1e-12);
        assertEquals(a*2, -Math.log1p(-PlasmaMath.intensity(300, .1)), 1e-12);
        assertTrue(PlasmaMath.intensity(600, .16) > .98);
    }
    @Test void cubeHasSixFacesAndPadding() {
        var builder = new ProjectedHull.Builder(); builder.block(0, 0, 0);
        var hull = builder.build(.2); assertEquals(6, hull.faces().size());
        for (var face : hull.faces()) { assertEquals(4, face.vertices().size()); assertEquals(1, face.normal().length(), 1e-12); }
        assertEquals(.5, hull.center().x(), 1e-12);
        assertTrue(hull.faces().stream().flatMap(f->f.vertices().stream()).anyMatch(v->Math.abs(v.x()+.2)<1e-9));
    }
    @Test void taperedShipRetainsSilhouetteRatherThanBoundingBox() {
        var builder = new ProjectedHull.Builder();
        for (int y=0;y<6;y++) for (int x=-(5-y)/2;x<=(5-y)/2;x++) for(int z=-1;z<=1;z++) builder.block(x,y,z);
        var hull = builder.build(0); assertTrue(hull.faces().size()>6);
        assertTrue(hull.faces().stream().anyMatch(f->Math.abs(f.normal().x())>.1 && Math.abs(f.normal().y())>.1));
        assertTrue(hull.faces().stream().flatMap(f->f.vertices().stream()).filter(v->v.y()>5.99).allMatch(v->v.x()>=0&&v.x()<=1));
    }
    @Test void allOccupiedCornersStayInsideIrregularHull() {
        var builder = new ProjectedHull.Builder(); var points = new java.util.ArrayList<SpaceVector>(); var random = new java.util.Random(42);
        for(int i=0;i<150;i++) { int x=random.nextInt(20)-10,y=random.nextInt(20)-10,z=random.nextInt(20)-10;builder.block(x,y,z);
            for(int dx=0;dx<=1;dx++)for(int dy=0;dy<=1;dy++)for(int dz=0;dz<=1;dz++)points.add(new SpaceVector(x+dx,y+dy,z+dz)); }
        var hull=builder.build(.2);assertFalse(hull.faces().isEmpty());assertTrue(hull.faces().size()<=54);
        for(var face:hull.faces())for(var p:points)assertTrue(face.normal().dot(p)<=face.normal().dot(face.vertices().getFirst())+1e-6);
    }
    @Test void emptyAndTranslatedShapes() {
        assertTrue(new ProjectedHull.Builder().build(.2).faces().isEmpty());
        var a=new ProjectedHull.Builder();var b=new ProjectedHull.Builder();
        for(int y=0;y<10;y++){a.block(0,y,0);b.block(-17,y+23,31);}
        var one=a.build(.2);var two=b.build(.2);assertEquals(one.faces().size(),two.faces().size());
        assertEquals(new SpaceVector(-17,23,31),two.center().subtract(one.center()));
    }
}
