package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.moon.MoonTerrain;
import io.github.astelev.around_planet.network.MoonTerrainPayload;
import io.github.astelev.around_planet.atlas.PlanetAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
/** One async, descriptor-keyed lunar atlas; no terrain chunks or biome queries. */
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class MoonAtlasTexture {
 public static final int WIDTH=1024,HEIGHT=512;
 private static MoonTerrainPayload descriptor;private static int[] pixels;private static DynamicTexture texture;private static ResourceLocation id;private static long generation;public static long revision;
 public static void accept(MoonTerrainPayload p){if(p.equals(descriptor))return;clear();descriptor=p;long ticket=generation;
  java.util.concurrent.CompletableFuture.supplyAsync(()->{var terrain=new MoonTerrain(p.circumference(),p.seed());int[] result=new int[WIDTH*HEIGHT];for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){double wx=((x+.5)/WIDTH-.5)*p.circumference(),z=((y+.5)/HEIGHT-.5)*p.circumference()/2;var sample=terrain.sample(wx,z);int rgb=MoonTerrain.color(sample);double shade=.86+.14*Math.clamp((terrain.sample(wx-8,z-8).height()-terrain.sample(wx+8,z+8).height())/32+.5,0,1);result[y*WIDTH+x]=((int)((rgb>>16&255)*shade)<<16)|((int)((rgb>>8&255)*shade)<<8)|(int)((rgb&255)*shade);}return result;},net.minecraft.Util.backgroundExecutor()).thenAccept(data->Minecraft.getInstance().execute(()->{if(ticket!=generation)return;pixels=data;var image=new NativeImage(WIDTH,HEIGHT,false);for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++)image.setPixelRGBA(x,y,PlanetAtlas.abgr(data[y*WIDTH+x]));texture=new DynamicTexture(image);texture.setFilter(true,false);texture.bind();org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S,org.lwjgl.opengl.GL11.GL_REPEAT);org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T,org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);id=Minecraft.getInstance().getTextureManager().register("around_planet_moon_atlas",texture);revision++;io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_ATLAS ready {}x{}",WIDTH,HEIGHT);}));
 }
 public static boolean ready(){return pixels!=null;}
 public static int sample(double x,double y,double z){return PlanetAtlas.sample(pixels,x,y,z);}
 public static void bind(net.minecraft.client.renderer.ShaderInstance shader){shader.safeGetUniform("HasAtlas").set(ready()?1:0);if(texture!=null)shader.setSampler("PlanetAtlas",texture.getId());}
 public static void bindSurface(net.minecraft.client.renderer.ShaderInstance shader){shader.safeGetUniform("HasMoonAtlas").set(ready()?1:0);if(texture!=null)shader.setSampler("MoonAtlas",texture.getId());}
 @net.neoforged.bus.api.SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){clear();}
 private static void clear(){generation++;revision++;descriptor=null;pixels=null;if(id!=null)Minecraft.getInstance().getTextureManager().release(id);texture=null;id=null;}
 private MoonAtlasTexture(){}
}
