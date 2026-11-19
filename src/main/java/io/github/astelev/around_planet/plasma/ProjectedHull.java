package io.github.astelev.around_planet.plasma;

import io.github.astelev.around_planet.space.SpaceVector;
import java.util.*;

/** Intersection of three convex orthographic silhouettes. Built only when occupied blocks change. */
public record ProjectedHull(List<Face> faces, SpaceVector center) {
    public record Face(SpaceVector normal, List<SpaceVector> vertices) {}
    private record Point(double x, double y) {}
    private record Plane(SpaceVector n, double d) {}
    private static final double EPS = 1e-6;
    public static final ProjectedHull EMPTY = new ProjectedHull(List.of(), SpaceVector.ZERO);

    public static final class Builder {
        // Four extrema per coordinate, independent of the number of blocks in a slice.
        private final Map<Integer, double[]> xSlices = new HashMap<>(), ySlices = new HashMap<>();
        public void block(int x, int y, int z) {
            for (int end = 0; end <= 1; end++) {
                extend(xSlices, x + end, y, y + 1, z, z + 1);
                extend(ySlices, y + end, z, z + 1, 0, 0);
            }
        }
        private static void extend(Map<Integer, double[]> slices, int i, double lo, double hi, double lo2, double hi2) {
            var a = slices.computeIfAbsent(i, k -> new double[]{lo, hi, lo2, hi2});
            a[0] = Math.min(a[0], lo); a[1] = Math.max(a[1], hi);
            a[2] = Math.min(a[2], lo2); a[3] = Math.max(a[3], hi2);
        }
        public ProjectedHull build(double padding) {
            if (xSlices.isEmpty()) return EMPTY;
            var xy = new ArrayList<Point>(); var xz = new ArrayList<Point>(); var yz = new ArrayList<Point>();
            xSlices.forEach((x, a) -> { xy.add(new Point(x, a[0])); xy.add(new Point(x, a[1])); xz.add(new Point(x, a[2])); xz.add(new Point(x, a[3])); });
            ySlices.forEach((y, a) -> { yz.add(new Point(y, a[0])); yz.add(new Point(y, a[1])); });
            var planes = new ArrayList<Plane>();
            silhouette(planes, xy, 0, padding); silhouette(planes, xz, 1, padding); silhouette(planes, yz, 2, padding);
            // Always retain the six bounds, even if an unusually complex silhouette is reduced.
            bounds(planes, xy, xz, padding);
            var vertices = new ArrayList<SpaceVector>();
            for (int i = 0; i < planes.size(); i++) for (int j = i + 1; j < planes.size(); j++) for (int k = j + 1; k < planes.size(); k++) {
                var a = planes.get(i); var b = planes.get(j); var c = planes.get(k);
                var bc = b.n.cross(c.n); double det = a.n.dot(bc);
                if (Math.abs(det) < EPS) continue;
                var v = bc.scale(a.d).add(c.n.cross(a.n).scale(b.d)).add(a.n.cross(b.n).scale(c.d)).scale(1 / det);
                boolean inside = true;
                for (var p : planes) if (p.n.dot(v) > p.d + EPS) { inside = false; break; }
                if (inside && vertices.stream().noneMatch(old -> old.subtract(v).length() < EPS * 10)) vertices.add(v);
            }
            if (vertices.isEmpty()) return EMPTY;
            var center = average(vertices); var faces = new ArrayList<Face>();
            for (var p : planes) {
                var points = new ArrayList<SpaceVector>();
                for (var v : vertices) if (Math.abs(p.n.dot(v) - p.d) < EPS * 10) points.add(v);
                if (points.size() < 3) continue;
                var origin = average(points);
                var u = p.n.cross(Math.abs(p.n.z()) < .8 ? new SpaceVector(0, 0, 1) : new SpaceVector(0, 1, 0));
                u = u.scale(1 / u.length()); var tangent = u; var v = p.n.cross(u);
                points.sort(Comparator.comparingDouble(a -> Math.atan2(a.subtract(origin).dot(v), a.subtract(origin).dot(tangent))));
                faces.add(new Face(p.n, List.copyOf(points)));
            }
            return new ProjectedHull(List.copyOf(faces), center);
        }
        private static void bounds(List<Plane> p, List<Point> xy, List<Point> xz, double pad) {
            double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
            double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
            for (var v : xy) { minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x); minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y); }
            for (var v : xz) { minZ = Math.min(minZ, v.y); maxZ = Math.max(maxZ, v.y); }
            add(p, new SpaceVector(1, 0, 0), maxX + pad); add(p, new SpaceVector(-1, 0, 0), -minX + pad);
            add(p, new SpaceVector(0, 1, 0), maxY + pad); add(p, new SpaceVector(0, -1, 0), -minY + pad);
            add(p, new SpaceVector(0, 0, 1), maxZ + pad); add(p, new SpaceVector(0, 0, -1), -minZ + pad);
        }
        private static void silhouette(List<Plane> planes, List<Point> input, int axes, double pad) {
            var points = new ArrayList<>(new HashSet<>(input));
            points.sort(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y));
            var hull = new ArrayList<Point>();
            for (var p : points) { while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.getLast(), p) <= 0) hull.removeLast(); hull.add(p); }
            int lower = hull.size();
            for (int i = points.size() - 2; i >= 0; i--) { var p = points.get(i); while (hull.size() > lower && cross(hull.get(hull.size() - 2), hull.getLast(), p) <= 0) hull.removeLast(); hull.add(p); }
            hull.removeLast();
            // Discarding constraints is conservative: no occupied corner can be cut off.
            int count = Math.min(16, hull.size());
            for (int i = 0; i < count; i++) {
                int j = i * hull.size() / count; var a = hull.get(j); var b = hull.get((j + 1) % hull.size());
                double nx = b.y - a.y, ny = a.x - b.x, length = Math.hypot(nx, ny); nx /= length; ny /= length;
                var normal = axes == 0 ? new SpaceVector(nx, ny, 0) : axes == 1 ? new SpaceVector(nx, 0, ny) : new SpaceVector(0, nx, ny);
                add(planes, normal, nx * a.x + ny * a.y + pad);
            }
        }
        private static void add(List<Plane> planes, SpaceVector n, double d) {
            for (int i = 0; i < planes.size(); i++) if (planes.get(i).n.subtract(n).length() < EPS) {
                if (d < planes.get(i).d) planes.set(i, new Plane(n, d)); return;
            }
            planes.add(new Plane(n, d));
        }
        private static double cross(Point a, Point b, Point c) { return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x); }
    }
    private static SpaceVector average(List<SpaceVector> points) {
        var sum = SpaceVector.ZERO; for (var p : points) sum = sum.add(p); return sum.scale(1.0 / points.size());
    }
}
