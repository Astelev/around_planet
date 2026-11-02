package io.github.astelev.aerospace.compat.dh;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LoadedSurfaceTilesTest {
    @Test void allRealColumnsAreRequiredAndUnloadingRestoresFallback() {
        var tiles = new LoadedSurfaceTiles();
        for(int x=0;x<4;x++) for(int z=0;z<4;z++) if(x!=3||z!=3) tiles.load(x,z);
        assertFalse(tiles.covered(0,0));
        tiles.load(3,3); tiles.load(3,3);
        assertTrue(tiles.covered(0,0));
        tiles.unload(3,3); tiles.unload(3,3);
        assertFalse(tiles.covered(0,0));
    }
    @Test void negativeCoordinatesAndWorldResetRemainIndependent() {
        var tiles = new LoadedSurfaceTiles();
        for(int x=-4;x<0;x++) for(int z=-4;z<0;z++) tiles.load(x,z);
        assertTrue(tiles.covered(-1,-1));
        assertFalse(tiles.covered(0,0));
        tiles.clear();
        assertFalse(tiles.covered(-1,-1));
    }
}
