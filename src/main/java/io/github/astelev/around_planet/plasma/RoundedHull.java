package io.github.astelev.around_planet.plasma;

import io.github.astelev.around_planet.space.SpaceVector;
import java.util.*;

/** Rounded outward offset: flat patches, cylindrical edge strips and spherical corner patches.
 * The entire mesh, including interpolated normals, is cached with the block-derived hull. */
public record RoundedHull(List<Triangle> triangles, SpaceVector center) {
    public record Vertex(SpaceVector position, SpaceVector normal) {}
    public record Triangle(Vertex a, Vertex b, Vertex c) {}
    private record Edge(int a, int b) { static Edge of(int a, int b) { return new Edge(Math.min(a,b), Math.max(a,b)); } }
    private static final int STEPS = 4;
    public static RoundedHull of(ProjectedHull hull) {
        if (hull.faces().isEmpty()) return new RoundedHull(List.of(), hull.center());
        var points = new ArrayList<SpaceVector>(); var edges = new LinkedHashMap<Edge, List<SpaceVector>>();
        for (var face : hull.faces()) for (var p : face.vertices()) index(points, p);
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX;
        for (var p : points) { minX=Math.min(minX,p.x());maxX=Math.max(maxX,p.x());minY=Math.min(minY,p.y());maxY=Math.max(maxY,p.y());minZ=Math.min(minZ,p.z());maxZ=Math.max(maxZ,p.z()); }
        double radius = Math.clamp(Math.min(maxX-minX,Math.min(maxY-minY,maxZ-minZ))*.12,.25,1.25);
        var normals = new ArrayList<Set<SpaceVector>>(); for (var p : points) normals.add(new LinkedHashSet<>());
        var triangles = new ArrayList<Triangle>();
        for (var face : hull.faces()) {
            var vertices=face.vertices();var n=face.normal();
            for(int i=1;i+1<vertices.size();i++) triangles.add(new Triangle(offset(vertices.getFirst(),n,radius),offset(vertices.get(i),n,radius),offset(vertices.get(i+1),n,radius)));
            for(int i=0;i<vertices.size();i++) {
                int a=index(points,vertices.get(i)),b=index(points,vertices.get((i+1)%vertices.size()));
                normals.get(a).add(n);edges.computeIfAbsent(Edge.of(a,b),k->new ArrayList<>()).add(n);
            }
        }
        for(var entry:edges.entrySet()) {
            if(entry.getValue().size()!=2)throw new IllegalArgumentException("Nonmanifold projected hull");
            var edge=entry.getKey();var a=points.get(edge.a);var b=points.get(edge.b);
            var na=entry.getValue().get(0);var nb=entry.getValue().get(1);
            for(int i=0;i<STEPS;i++) {
                var n0=blend(na,nb,i/(double)STEPS);var n1=blend(na,nb,(i+1)/(double)STEPS);
                var a0=offset(a,n0,radius);var a1=offset(a,n1,radius);var b0=offset(b,n0,radius);var b1=offset(b,n1,radius);
                triangles.add(new Triangle(a0,b0,b1));triangles.add(new Triangle(a0,b1,a1));
            }
            // Each incident edge bounds one spherical wedge at both endpoints. Using the exact
            // same arc samples closes the corner/strip seam without cracks or frame-time work.
            for(int endpoint:new int[]{edge.a,edge.b}) {
                var sum=SpaceVector.ZERO;for(var n:normals.get(endpoint))sum=sum.add(n);
                var middle=unit(sum);var p=points.get(endpoint);
                for(int i=0;i<STEPS;i++)triangles.add(new Triangle(offset(p,middle,radius),offset(p,blend(na,nb,i/(double)STEPS),radius),offset(p,blend(na,nb,(i+1)/(double)STEPS),radius)));
            }
        }
        return new RoundedHull(List.copyOf(triangles),hull.center());
    }
    private static int index(List<SpaceVector> points, SpaceVector p) {
        for(int i=0;i<points.size();i++)if(points.get(i).subtract(p).length()<1e-5)return i;
        points.add(p);return points.size()-1;
    }
    private static SpaceVector unit(SpaceVector n) { return n.scale(1/n.length()); }
    private static SpaceVector blend(SpaceVector a,SpaceVector b,double t) { return unit(a.scale(1-t).add(b.scale(t))); }
    private static Vertex offset(SpaceVector p,SpaceVector n,double r) { return new Vertex(p.add(n.scale(r)),n); }
}
