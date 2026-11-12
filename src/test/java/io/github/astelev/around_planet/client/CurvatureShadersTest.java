package io.github.astelev.around_planet.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CurvatureShadersTest {
    @Test void worldPassSelectionExcludesPostprocessAndOurSky(){assertTrue(CurvatureShaders.worldProgram("rendertype_entity_solid"));assertTrue(CurvatureShaders.worldProgram("minecraft:rendertype_solid"));assertFalse(CurvatureShaders.worldProgram("simpleclouds:clouds"));assertFalse(CurvatureShaders.worldProgram("simpleclouds:clouds_shadow_map"));assertFalse(CurvatureShaders.worldProgram("position_tex"));assertFalse(CurvatureShaders.worldProgram("around_planet:surface_sky"));}
    @Test void cruisingFallbackBendsProjectionOnlyAndKeepsSamplingCoordinates(){
        assertTrue(CurvatureShaders.worldProgram("cruising:fallback_plane"));
        String source="#version 150\nin vec3 Position;uniform mat4 DhViewProjection;out vec3 surfacePosition;void main(){surfacePosition=Position;gl_Position = DhViewProjection * vec4(surfacePosition, 1.0);}";
        String p=CurvatureShaders.patch(source);assertTrue(p.contains("surfacePosition=Position;"));assertTrue(p.contains("vec3 apPosition=surfacePosition"));assertTrue(p.contains("vec4(apPosition, 1.0)"));assertEquals(p,CurvatureShaders.patch(p));assertTrue(CurvatureShaders.patch(source.replace("void main()","void main(void)").replace("gl_Position = DhViewProjection * vec4(surfacePosition, 1.0);","gl_Position=DhViewProjection*vec4(surfacePosition,1.0);")).contains("vec3 apPosition=surfacePosition"));
    }
    @Test void patchPreservesVersionAndEarlyReturnsWithoutDoubleWrapping(){String s="#version 150\nvoid main(void){if(false)return;gl_Position=ProjMat*ModelViewMat*vec4(1);}";String p=CurvatureShaders.patch(s);assertTrue(p.startsWith("#version 150"));assertTrue(p.contains("vec4 apCurveView(vec4 v)"));assertTrue(p.contains("gl_Position = ProjMat * apCurveView(ModelViewMat*vec4(1))"));assertEquals(p,CurvatureShaders.patch(p));assertEquals("#version 150\nvoid main(){}",CurvatureShaders.patch("#version 150\nvoid main(){}"));}
}
