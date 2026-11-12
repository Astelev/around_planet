package io.github.astelev.around_planet.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CurvatureShadersTest {
    @Test void worldPassSelectionExcludesPostprocessAndOurSky(){assertTrue(CurvatureShaders.worldProgram("rendertype_entity_solid"));assertTrue(CurvatureShaders.worldProgram("minecraft:rendertype_solid"));assertFalse(CurvatureShaders.worldProgram("simpleclouds:clouds"));assertFalse(CurvatureShaders.worldProgram("simpleclouds:clouds_shadow_map"));assertFalse(CurvatureShaders.worldProgram("position_tex"));assertFalse(CurvatureShaders.worldProgram("around_planet:surface_sky"));}
    @Test void patchPreservesVersionAndEarlyReturnsWithoutDoubleWrapping(){String s="#version 150\nvoid main(void){if(false)return;gl_Position=ProjMat*ModelViewMat*vec4(1);}";String p=CurvatureShaders.patch(s);assertTrue(p.startsWith("#version 150"));assertTrue(p.contains("vec4 apCurveView(vec4 v)"));assertTrue(p.contains("gl_Position = ProjMat * apCurveView(ModelViewMat*vec4(1))"));assertEquals(p,CurvatureShaders.patch(p));assertEquals("#version 150\nvoid main(){}",CurvatureShaders.patch("#version 150\nvoid main(){}"));}
}
