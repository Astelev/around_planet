package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceEncounterTest {
    private SpaceState state(double x,double vx){return new SpaceState(new SpaceVector(x,40000,0),new SpaceVector(vx,0,0));}
    @Test void strictSpeedAndDistanceAndDisabledGate(){assertTrue(SpaceEncounter.eligible(state(0,500),state(96,549.99),96,50));assertFalse(SpaceEncounter.eligible(state(0,500),state(96,550),96,50));assertFalse(SpaceEncounter.eligible(state(0,500),state(96.01,500),96,50));assertFalse(SpaceEncounter.eligible(state(0,0),state(0,0),0,50));}
    @Test void changingFramePreservesCosmicPositionAndVelocity(){var from=new SpaceFrame(new SpaceVector(0,1,0),new SpaceVector(-1,0,0),new SpaceVector(0,0,1));var a=state(30,620);var b=state(0,600);var local=SpaceEncounter.relative(a,b,from);assertEquals(a,from.detach(b,local));assertEquals(new SpaceVector(0,-30,0),local.position());assertEquals(new SpaceVector(0,-20,0),local.velocity());}
}
