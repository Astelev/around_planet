package io.github.astelev.around_planet.seam;

import java.util.*;

/** Oldest due snapshots first: a refresh period shorter than queue drain cannot starve the tail. */
public final class SnapshotSchedule {
    public record Candidate(long key,long lastChecked,double distance) {}
    public static List<Candidate> choose(List<Candidate> candidates,long now,int interval,int budget) {
        if(interval<1 || budget<1)throw new IllegalArgumentException("Positive cadence and budget required");
        return candidates.stream().filter(c->c.lastChecked==Long.MIN_VALUE || now-c.lastChecked>=interval)
                .sorted(Comparator.comparingLong(Candidate::lastChecked).thenComparingDouble(Candidate::distance)
                        .thenComparingLong(Candidate::key)).limit(budget).toList();
    }
    private SnapshotSchedule() {}
}
