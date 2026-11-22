package io.github.astelev.around_planet.plasma;

import io.github.astelev.around_planet.space.SpaceVector;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoundedHullTest {
    @Test void roundedBoxHasCurvedSidesAndUnitNormals() {
        var b=new ProjectedHull.Builder();for(int y=0;y<10;y++)b.block(0,y,0);
        var mesh=RoundedHull.of(b.build(.08));assertEquals(204,mesh.triangles().size());
        var points=vertices(mesh);
        assertTrue(points.stream().anyMatch(v->v.normal().x()>.1&&v.normal().z()>.1&&Math.abs(v.normal().y())<1e-9),"Edge parallel to velocity Y must be a curved strip");
        assertTrue(points.stream().anyMatch(v->Math.abs(v.normal().x())>.1&&Math.abs(v.normal().y())>.1&&Math.abs(v.normal().z())>.1),"Corner must be spherical");
        for(var v:points)assertEquals(1,v.normal().length(),1e-9);
        closed(mesh);
    }
    @Test void taperedHullIsClosedAndRoundingDoesNotCutIntoShip() {
        var b=new ProjectedHull.Builder();for(int y=0;y<6;y++)for(int x=-(5-y)/2;x<=(5-y)/2;x++)for(int z=-1;z<=1;z++)b.block(x,y,z);
        var hull=b.build(.08);var mesh=RoundedHull.of(hull);closed(mesh);
        for(var v:vertices(mesh))assertTrue(hull.faces().stream().anyMatch(f->f.normal().dot(v.position())>f.normal().dot(f.vertices().getFirst())+1e-6));
    }
    @Test void irregularHullRemainsClosedAndBounded() {
        var b=new ProjectedHull.Builder();var random=new Random(17);
        for(int i=0;i<500;i++)b.block(random.nextInt(31),random.nextInt(47),random.nextInt(19));
        var mesh=RoundedHull.of(b.build(.08));closed(mesh);assertTrue(mesh.triangles().size()<6000);
    }
    @Test void emptyHullAndTranslationAreStable() {
        assertTrue(RoundedHull.of(ProjectedHull.EMPTY).triangles().isEmpty());
        var a=new ProjectedHull.Builder();var b=new ProjectedHull.Builder();a.block(0,0,0);b.block(10,-20,30);
        var one=RoundedHull.of(a.build(.08));var two=RoundedHull.of(b.build(.08));
        assertEquals(one.triangles().size(),two.triangles().size());
        assertEquals(10,two.center().x()-one.center().x(),1e-9);
        assertEquals(-20,two.center().y()-one.center().y(),1e-9);
        assertEquals(30,two.center().z()-one.center().z(),1e-9);
    }
    private static List<RoundedHull.Vertex> vertices(RoundedHull mesh) {return mesh.triangles().stream().flatMap(t->java.util.stream.Stream.of(t.a(),t.b(),t.c())).toList();}
    private static String key(SpaceVector p){return Math.round(p.x()*1e6)+","+Math.round(p.y()*1e6)+","+Math.round(p.z()*1e6);}
    private static void closed(RoundedHull mesh){
        var edges=new HashMap<String,Integer>();
        for(var t:mesh.triangles()){
            var a=t.a().position();var b=t.b().position();var c=t.c().position();
            assertTrue(b.subtract(a).cross(c.subtract(a)).length()>1e-10,"Degenerate triangle");
            var ps=List.of(a,b,c);for(int i=0;i<3;i++){var u=key(ps.get(i));var v=key(ps.get((i+1)%3));edges.merge(u.compareTo(v)<0?u+":"+v:v+":"+u,1,Integer::sum);}
        }
        assertFalse(edges.isEmpty());for(var edge:edges.entrySet())assertEquals(2,edge.getValue(),"Mesh seam: "+edge.getKey());
    }
}
