package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.atlas.*;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** Real map texture/upload checks in a copied world. The deterministic atlas is fixture data only. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class MapSurfacesClientSmoke {
 private static int ticks,phase,started,configured=-1;private static volatile Throwable failure;private static volatile boolean ready;private static com.mojang.blaze3d.platform.NativeImage image;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){if(!Boolean.getBoolean("around_planet.mapSurfacesSmoke"))return;var mc=Minecraft.getInstance();ticks++;if(failure!=null)throw new AssertionError("Map surface fixture",failure);if(ticks>2000)throw new AssertionError("Map surface timeout phase="+phase);
  if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
  if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||ticks<100)return;
  if(phase==0){phase=1;mc.getSingleplayerServer().execute(()->{try{SolarClientSmoke.setup(mc);var l=mc.getSingleplayerServer().overworld();int[] pixels=new int[PlanetAtlas.SIZE];java.util.Arrays.fill(pixels,0x20a040);var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Version",3);tag.putLong("Seed",l.getSeed());tag.putInt("Circumference",PlanetConfig.PLANET_SIZE.get());tag.putInt("Progress",PlanetAtlas.SIZE);tag.putIntArray("Pixels",pixels);l.getDataStorage().set(PlanetAtlasData.ID,PlanetAtlasData.load(tag,l.registryAccess()));ready=true;}catch(Throwable t){failure=t;}});return;}
  var status=SpaceHud.current();var sys=CelestialSky.current();if(!ready||status==null||!status.active()||sys==null||ManeuverClient.current()==null||!PlanetAtlasTexture.ready())return;
  if(!(mc.screen instanceof OrbitMapScreen)){mc.setScreen(new OrbitMapScreen());started=ticks;return;}
  var map=(OrbitMapScreen)mc.screen;String wanted=phase==2?CelestialSystem.SUN:phase==5?CelestialSystem.MOON:CelestialSystem.EARTH;
  if(!chart(map).equals(wanted)){viewButton(map).onPress();configured=-1;started=ticks;return;}
  var camera=camera(map);var moon=sys.moon(ManeuverClient.now()).position();
  if(configured!=phase){configured=phase;started=ticks;
   if(phase==3||phase==4){var z=new SpaceVector(0,0,1);var right=phase==3?z.cross(moon):moon.cross(z);camera.fit(new SpaceState(right,SpaceVector.ZERO),sys.earthRadius()*3);camera.pitch=0;}
   else {camera.fit(new SpaceState(new SpaceVector(1,0,0),SpaceVector.ZERO),(phase==2?sys.earthRadius():sys.moonRadius())*3);camera.pitch=.6;camera.yaw=.4;}
  }
  camera.focus=phase==1?moon:phase==2?sys.earth(ManeuverClient.now()).position():SpaceVector.ZERO;
  if(ticks-started<35)return;var pixels=pixels(map);if(image==null)image=pixels;else require(image==pixels,"Map allocates a new surface image per body/chart");int color=pixels.getPixelRGBA(pixels.getWidth()/2,pixels.getHeight()/2);
  if(phase==1||phase==3||phase==5)require(gray(color),"Moon surface absent or Earth texture used at center: "+Integer.toHexString(color));else require(green(color),"Earth atlas absent or hidden by rear Moon: "+Integer.toHexString(color));
  if(phase==3){double size=viewport(map),scale=camera.scale(size);int dx=(int)(sys.earthRadius()*.65*scale*pixels.getWidth()/(map.width-16));require(green(pixels.getPixelRGBA(pixels.getWidth()/2+dx,pixels.getHeight()/2)),"Foreground Moon replaced the whole Earth rather than just its silhouette");}
  net.minecraft.client.Screenshot.grab(mc.gameDirectory,"map69-"+phase+".png",mc.getMainRenderTarget(),m->{});
  AroundPlanet.LOGGER.info("MAP_SURFACE_PHASE phase={} primary={} center={}",phase,wanted,Integer.toHexString(color));phase++;configured=-1;started=ticks;
  if(phase==6){AroundPlanet.LOGGER.info("MAP_SURFACES_CLIENT_PASS Moon in Earth chart; Earth in Sun chart with shared atlas; front/rear Moon depth; standalone Moon; one reused bounded image");mc.setScreen(null);mc.stop();}
 }
 private static boolean gray(int c){int r=c&255,g=(c>>>8)&255,b=(c>>>16)&255;return (c>>>24)>200&&r>15&&Math.abs(r-g)<2&&Math.abs(g-b)<2;}
 private static boolean green(int c){int r=c&255,g=(c>>>8)&255,b=(c>>>16)&255;return (c>>>24)>200&&g>r*2&&g>b;}
 private static Object field(Object target,String name){try{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static OrbitCamera camera(OrbitMapScreen map){return (OrbitCamera)field(map,"camera");}
 private static com.mojang.blaze3d.platform.NativeImage pixels(OrbitMapScreen map){return ((net.minecraft.client.renderer.texture.DynamicTexture)field(field(map,"sphere"),"texture")).getPixels();}
 private static double viewport(OrbitMapScreen map){return Math.min((Integer)field(map,"right")-(Integer)field(map,"left"),(Integer)field(map,"bottom")-(Integer)field(map,"top"));}
 private static String chart(OrbitMapScreen map){try{var m=OrbitMapScreen.class.getDeclaredMethod("viewPrimary");m.setAccessible(true);return (String)m.invoke(map);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static net.minecraft.client.gui.components.Button viewButton(OrbitMapScreen map){return map.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getX()==map.width-300).map(net.minecraft.client.gui.components.Button.class::cast).findFirst().orElseThrow();}
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
