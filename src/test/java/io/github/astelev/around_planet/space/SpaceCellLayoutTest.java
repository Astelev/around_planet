package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SpaceCellLayoutTest {
    @Test void smallCanonicalWorldHasOneCellNotAdditionalCellsInItsSafetyGuard() {
        var first=SpaceCellLayout.find(Set.of(),-256,256,-256,256).orElseThrow();
        assertEquals(0,first.x());assertEquals(0,first.z());
        assertTrue(SpaceCellLayout.find(Set.of(first.slot()),-256,256,-256,256).isEmpty());
    }
    @Test void addressesAreFiniteSeparatedAndReusedOnlyAfterRelease() {
        var ids=new HashSet<Integer>();var cells=new ArrayList<SpaceCellLayout.Address>();
        while(true){var found=SpaceCellLayout.find(ids,-65536,65536,-65536,65536);if(found.isEmpty())break;cells.add(found.get());assertTrue(ids.add(found.get().slot()));}
        assertEquals(81,cells.size());
        for(int i=0;i<cells.size();i++)for(int j=i+1;j<cells.size();j++)assertTrue(Math.max(Math.abs(cells.get(i).x()-cells.get(j).x()),Math.abs(cells.get(i).z()-cells.get(j).z()))>=1024);
        ids.remove(40);assertEquals(40,SpaceCellLayout.find(ids,-65536,65536,-65536,65536).orElseThrow().slot());
    }
    @Test void tighterOrDisplacedBoundsCannotProduceAClippedCell() {
        assertTrue(SpaceCellLayout.find(Set.of(),-100,100,-65536,65536).isEmpty());
        var c=SpaceCellLayout.find(Set.of(),500,1600,-256,256).orElseThrow();assertEquals(1024,c.x());assertEquals(0,c.z());
    }
}
