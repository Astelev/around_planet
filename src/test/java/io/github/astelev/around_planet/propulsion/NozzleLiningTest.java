package io.github.astelev.around_planet.propulsion;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class NozzleLiningTest {
    @Test void solidBellHasSlopingConvergingAndDivergingWalls()throws Exception {
        var model=NozzleLining.read(Files.newBufferedReader(Path.of("src/main/resources/assets/around_planet/models/block/solid_rocket_nozzle.obj")));
        assertEquals(24,model.faces().size());
        assertEquals(-.605f,model.outlet(),1e-6);assertEquals(.37f,model.mouthRadius(),1e-6);
        assertTrue(model.faces().stream().anyMatch(f->f.normal().y()>.01));
        assertTrue(model.faces().stream().anyMatch(f->f.normal().y()<-.01));
        for(var f:model.faces()){
            assertTrue(Math.abs(f.normal().y())>.01,"Bell wall must slope, not form steps");
            assertEquals(1,Math.sqrt(f.normal().x()*f.normal().x()+f.normal().y()*f.normal().y()+f.normal().z()*f.normal().z()),1e-6);
            for(var p:f.points())assertTrue(Math.abs(p.x())<=.5&&Math.abs(p.z())<=.5);
        }
    }
    @Test void editedBlenderModelIncludesAllInnerSegmentsAndCap()throws Exception {
        var model=NozzleLining.read(Files.newBufferedReader(Path.of("src/main/resources/assets/around_planet/models/block/liquid_rocket_engine.obj")));
        assertEquals(33,model.faces().size()); // 32 inner panels plus the chamber cap, no body/pipe faces.
        assertTrue(model.faces().stream().flatMap(f->f.points().stream()).anyMatch(p->Math.abs(p.y()+.043349f)<1e-6));
        assertTrue(model.faces().stream().flatMap(f->f.points().stream()).anyMatch(p->Math.abs(p.y()+1)<1e-6));
        assertEquals(-1.13f,model.outlet(),1e-6);
        assertEquals(.36f,model.mouthRadius(),1e-6);
        for(var f:model.faces())assertEquals(1,Math.sqrt(f.normal().x()*f.normal().x()+f.normal().y()*f.normal().y()+f.normal().z()*f.normal().z()),1e-6);
    }
    @Test void supportsExporterFaceTokensNegativeIndicesAndTriangles()throws Exception {
        var model=NozzleLining.read(new StringReader("v 0 0 0\nv 1 0 0\nv 0 1 0\nusemtl body\nf 1 2 3\nusemtl inner\nf -3/1/2 -2/2/2 -1/3/2 # exported triangle\n"));
        assertEquals(1,model.faces().size());
        assertEquals(model.faces().getFirst().points().get(2),model.faces().getFirst().points().get(3));
        assertEquals(1,model.faces().getFirst().normal().z());
    }
    @Test void movedOutletAndWidthFollowGeometryRatherThanOldProfile()throws Exception {
        var model=NozzleLining.read(new StringReader("v .2 -2 .2\nv .8 -2 .2\nv .8 -1 .2\nv .2 -1 .2\nusemtl inner\nf 1 2 3 4\n"));
        assertEquals(-2.505f,model.outlet(),1e-6);
        assertEquals(.3f/.24f,model.plumeScale(),1e-6);
    }
    @Test void rejectsMissingOrDegenerateLining(){
        assertThrows(IOException.class,()->NozzleLining.read(new StringReader("v 0 0 0\nusemtl body\n")));
        assertThrows(IOException.class,()->NozzleLining.read(new StringReader("v 0 0 0\nusemtl inner\nf 1 1 1\n")));
    }
}
