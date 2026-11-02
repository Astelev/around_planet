package io.github.astelev.aerospace.compat.dh;

/** Horizontal biome tiles with closed risers, plus a neutral outer sea plane. */
public final class FallbackSurfaceMesh {
    private FallbackSurfaceMesh() {}
    @FunctionalInterface public interface QuadSink {
        void quad(float x0, float y0, float z0, float x1, float y1, float z1,
                  float x2, float y2, float z2, float x3, float y3, float z3);
    }
    public static void build(int[][] heights, int cellSize, float extent, QuadSink out) {
        int width = heights.length;
        float span = width * cellSize;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                float x0 = x * cellSize, z0 = z * cellSize, x1 = x0 + cellSize, z1 = z0 + cellSize;
                int h = heights[x][z];
                top(out, x0, z0, x1, z1, h);
                wallX(out, x0, z0, z1, h, x == 0 ? 0 : heights[x - 1][z]);
                wallZ(out, z0, x0, x1, h, z == 0 ? 0 : heights[x][z - 1]);
                if (x == width - 1) wallX(out, x1, z0, z1, 0, h);
                if (z == width - 1) wallZ(out, z1, x0, x1, 0, h);
            }
        }
        float low = -extent, high = span + extent;
        top(out, low, low, high, 0, 0);
        top(out, low, span, high, high, 0);
        top(out, low, 0, 0, span, 0);
        top(out, span, 0, high, span, 0);
    }
    private static void top(QuadSink out, float x0, float z0, float x1, float z1, float h) {
        out.quad(x0,h,z0, x0,h,z1, x1,h,z1, x1,h,z0);
    }
    private static void wallX(QuadSink out, float x, float z0, float z1, int a, int b) {
        if (a == b) return;
        float low = Math.min(a,b), high = Math.max(a,b);
        // a is the tile on +X, b on -X. Point away from the higher solid tile.
        if (a > b) out.quad(x,low,z1, x,high,z1, x,high,z0, x,low,z0);
        else out.quad(x,low,z0, x,high,z0, x,high,z1, x,low,z1);
    }
    private static void wallZ(QuadSink out, float z, float x0, float x1, int a, int b) {
        if (a == b) return;
        float low = Math.min(a,b), high = Math.max(a,b);
        // a is the tile on +Z, b on -Z.
        if (a < b) out.quad(x1,low,z, x1,high,z, x0,high,z, x0,low,z);
        else out.quad(x0,low,z, x0,high,z, x1,high,z, x1,low,z);
    }
}
