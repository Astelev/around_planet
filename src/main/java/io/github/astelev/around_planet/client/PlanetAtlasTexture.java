package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.atlas.PlanetAtlas;
import io.github.astelev.around_planet.network.PlanetAtlasPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
/** One world-scoped CPU/GPU copy shared by space and the map. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class PlanetAtlasTexture {
    private static int[] pending;private static int pendingSize;private static final java.util.BitSet parts=new java.util.BitSet();
    public static void acceptPart(PlanetAtlasPayload.Part p){if(p.offset()==0||pending==null||pendingSize!=p.circumference()){pending=new int[PlanetAtlas.SIZE];pendingSize=p.circumference();parts.clear();}System.arraycopy(p.pixels(),0,pending,p.offset(),p.pixels().length);parts.set(p.offset()/PlanetAtlasPayload.Part.CHUNK);if(parts.cardinality()==(PlanetAtlas.SIZE+PlanetAtlasPayload.Part.CHUNK-1)/PlanetAtlasPayload.Part.CHUNK){var complete=pending;pending=null;accept(new PlanetAtlasPayload(pendingSize,complete));}}
    private static int[] pixels;private static DynamicTexture texture;private static ResourceLocation id;public static long revision;
    public static void accept(PlanetAtlasPayload payload){clear();pixels=payload.pixels().clone();var image=new NativeImage(PlanetAtlas.WIDTH,PlanetAtlas.HEIGHT,false);for(int y=0;y<PlanetAtlas.HEIGHT;y++)for(int x=0;x<PlanetAtlas.WIDTH;x++)image.setPixelRGBA(x,y,PlanetAtlas.abgr(pixels[y*PlanetAtlas.WIDTH+x]));texture=new DynamicTexture(image);texture.setFilter(true,false);texture.bind();org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S,org.lwjgl.opengl.GL11.GL_REPEAT);org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T,org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);id=Minecraft.getInstance().getTextureManager().register("around_planet_biome_atlas",texture);revision++;io.github.astelev.around_planet.AroundPlanet.LOGGER.info("PLANET_ATLAS client ready {}x{}",PlanetAtlas.WIDTH,PlanetAtlas.HEIGHT);}
    public static boolean ready(){return pixels!=null;}
    public static int sample(double x,double y,double z){return PlanetAtlas.sample(pixels,x,y,z);}
    public static void bind(net.minecraft.client.renderer.ShaderInstance shader){shader.safeGetUniform("HasAtlas").set(ready()?1:0);if(texture!=null)shader.setSampler("PlanetAtlas",texture.getId());}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){clear();}
    private static void clear(){pending=null;parts.clear();if(id!=null)Minecraft.getInstance().getTextureManager().release(id);pixels=null;texture=null;id=null;revision++;}
    private PlanetAtlasTexture(){}
}
