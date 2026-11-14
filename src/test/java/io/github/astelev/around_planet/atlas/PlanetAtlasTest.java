package io.github.astelev.around_planet.atlas;
import io.github.astelev.around_planet.space.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlanetAtlasTest {
    @Test void biomePixelsMatchTheActualPlanetEntryChart(){
        var geometry=new PlanetScale(131072).geometry();int[] pixels=new int[PlanetAtlas.SIZE];for(int i=0;i<pixels.length;i++)pixels[i]=i;
        for(int y=0;y<PlanetAtlas.HEIGHT;y+=17)for(int x=0;x<PlanetAtlas.WIDTH;x+=23){var n=geometry.frame(PlanetAtlas.worldX(x,131072),PlanetAtlas.worldZ(y,131072)).up();assertEquals(y*PlanetAtlas.WIDTH+x,PlanetAtlas.sample(pixels,n.x(),n.y(),n.z()));}
    }
    @Test void wrapAndPolesAreBounded(){int[] pixels=new int[PlanetAtlas.SIZE];for(int i=0;i<pixels.length;i++)pixels[i]=i;assertEquals(PlanetAtlas.sample(pixels,0,-1,0),PlanetAtlas.sample(pixels,-0.0,-1,0));assertTrue(PlanetAtlas.sample(pixels,0,0,-1)<PlanetAtlas.WIDTH);assertTrue(PlanetAtlas.sample(pixels,0,0,1)>=(PlanetAtlas.HEIGHT-1)*PlanetAtlas.WIDTH);}
    @Test void textureChannelsAreConvertedToNativeImageOrder(){assertEquals(0xff563412,PlanetAtlas.abgr(0x123456));}
    @Test void spaceLightCannotBecomeBlackAndDoesNotClipBrightSources(){assertEquals(0xff7a6f69,SpaceIllumination.ambient(0xff000000));assertEquals(0xffffffff,SpaceIllumination.ambient(0xffffffff));assertEquals(0xff7a6fff,SpaceIllumination.ambient(0xff0000ff));}
    @Test void serverPaletteDoesNotDependOnClientGrassResources(){for(float t:new float[]{-.5f,0,.5f,1,2})for(float wet:new float[]{0,.5f,1}){int color=PlanetAtlas.landColor(t,wet);assertNotEquals(0,color);assertTrue((color&0xff000000)==0);}}

}
