package io.github.astelev.around_planet.topology;
import io.github.astelev.around_planet.seam.SnapshotSchedule;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SnapshotScheduleTest {
    @Test void shortRefreshCannotStarveTail() {
        var checked=new HashMap<Long,Long>();
        for(long now=0;now<100;now++) {
            var due=new ArrayList<SnapshotSchedule.Candidate>();
            for(long key=0;key<28;key++)due.add(new SnapshotSchedule.Candidate(key,checked.getOrDefault(key,Long.MIN_VALUE),key));
            var selected=SnapshotSchedule.choose(due,now,2,2);
            assertTrue(selected.size()<=2);
            for(var candidate:selected)checked.put(candidate.key(),now);
        }
        assertEquals(28,checked.size());
        assertTrue(checked.values().stream().allMatch(t->t>=86),"Every column must receive service despite continuous early work");
    }
    @Test void cadenceAndNearestInitialPriorityArePreserved() {
        var candidates=List.of(new SnapshotSchedule.Candidate(1,Long.MIN_VALUE,100),
                new SnapshotSchedule.Candidate(2,Long.MIN_VALUE,1),new SnapshotSchedule.Candidate(3,9,0));
        assertEquals(List.of(2L,1L),SnapshotSchedule.choose(candidates,10,10,2).stream().map(SnapshotSchedule.Candidate::key).toList());
        assertTrue(SnapshotSchedule.choose(List.of(new SnapshotSchedule.Candidate(1,9,0)),10,10,2).isEmpty());
    }
}
