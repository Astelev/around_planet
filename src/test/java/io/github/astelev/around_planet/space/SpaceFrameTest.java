package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaceFrameTest {
    @Test void ownerAccelerationGivesFreeBodyOppositeDeltaVWithoutChangingItsCosmicVelocity() {
        var frame=new SpaceFrame(new SpaceVector(0,0,-1),new SpaceVector(0,1,0),new SpaceVector(1,0,0));
        var origin=new SpaceState(new SpaceVector(100,200,300),new SpaceVector(1000,2000,3000));
        var local=new SpaceState(new SpaceVector(10,20,30),new SpaceVector(1,2,3));
        var before=frame.detach(origin,local);
        var delta=new SpaceVector(50,-20,10);
        var after=frame.detach(new SpaceState(origin.position(),origin.velocity().add(delta)),new SpaceState(local.position(),local.velocity().add(frame.counterImpulse(delta))));
        assertEquals(before,after);assertEquals(local.position(),frame.toLocal(frame.toCosmic(local.position())));
    }
    @Test void detachIncludesFrameVelocityExactlyOnce() {
        var origin=new SpaceState(new SpaceVector(100,200,300),new SpaceVector(1000,0,0));
        var local=new SpaceState(new SpaceVector(10,0,0),new SpaceVector(-20,30,0));
        var detached=SpaceFrame.IDENTITY.detach(origin,local);
        assertEquals(new SpaceVector(110,200,300),detached.position());assertEquals(new SpaceVector(980,30,0),detached.velocity());
    }
    @Test void aReflectionIsNotAcceptedAsFrameRotation() {
        assertThrows(IllegalArgumentException.class,()->new SpaceFrame(new SpaceVector(1,0,0),new SpaceVector(0,1,0),new SpaceVector(0,0,-1)));
    }
}
