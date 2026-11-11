package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SphereGridTest {
    private SpaceVector normal(double lon,double lat){return new SpaceVector(Math.sin(lon)*Math.cos(lat),Math.cos(lon)*Math.cos(lat),Math.sin(lat));}
    @Test void AdjacentPlanetTilesAlternateAndStayOpaque(){int a=SphereGrid.color(normal(.04,.04)),b=SphereGrid.color(normal(.24,.04));assertTrue(Math.abs((a&255)-(b&255))>50);assertEquals(255,a>>>24);assertEquals(255,b>>>24);}
    @Test void cameraInverseProjectsTheSameSphereNormal(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(100,20,10),new SpaceVector(0,5,3)),200);c.rotate(71,24);var v=c.fromView(.3,.4,Math.sqrt(.75));var q=c.project(v,0,0,400);assertEquals(.3,q.x()/c.scale(400),1e-9);assertEquals(.4,-q.y()/c.scale(400),1e-9);assertEquals(1,v.length(),1e-9);}
    @Test void textureIsPlanetFixedNotPaintedOnCamera(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(100,0,0),new SpaceVector(0,0,1)),200);var points=new java.util.ArrayList<SpaceVector>();for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++){double a=x*.16,b=y*.16;if(a*a+b*b<.9)points.add(new SpaceVector(a,b,Math.sqrt(1-a*a-b*b)));}var before=points.stream().map(p->SphereGrid.color(c.fromView(p.x(),p.y(),p.z()))).toList();c.rotate(90,50);boolean changed=false;for(int i=0;i<points.size();i++){var p=points.get(i);changed|=before.get(i)!=SphereGrid.color(c.fromView(p.x(),p.y(),p.z()));}assertTrue(changed);}
}
