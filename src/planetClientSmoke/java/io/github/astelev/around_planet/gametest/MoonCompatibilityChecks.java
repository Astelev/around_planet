package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.moon.*;
import io.github.astelev.around_planet.space.MoonSurface;
public final class MoonCompatibilityChecks {
 public static void run(net.minecraft.server.MinecraftServer server) throws Exception{
  com.seibel.distanthorizons.core.config.Config.Common.WorldGenerator.generatorPlan.setWithoutSaving(com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiGeneratorPlan.SURFACE_ONLY);
  var l=server.getLevel(MoonSurface.dimension());var wrapper=com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper_neoforge.getWrapper(l);
  var selected=com.seibel.distanthorizons.coreapi.DependencyInjection.WorldGeneratorInjector.INSTANCE.get(wrapper);require(selected instanceof io.github.astelev.around_planet.compat.MoonDhWorldgen,"DH did not select lunar generator override");
  int before=l.getChunkSource().getLoadedChunksCount();var terrain=((MoonChunkGenerator)l.getChunkSource().getGenerator()).terrain();var chunk=((io.github.astelev.around_planet.compat.MoonDhWorldgen)selected).generateApiChunk(411,-731,com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode.FEATURES);
  var full=com.seibel.distanthorizons.core.dataObjects.transformers.LodDataBuilder.createFromApiChunkData(chunk,true);
  for(int z=0;z<16;z++)for(int x=0;x<16;x++){var expected=terrain.sample(411*16+x,-731*16+z);var points=chunk.getDataPoints(x,z);require(points.stream().mapToInt(p->p.topYBlockPos).max().orElseThrow()==expected.height()+1,"DH height differs from actual lunar terrain");require(!full.getApiDataPointColumn((411&3)*16+x,(-731&3)*16+z).isEmpty(),"DH rejected generated column");}
  require(l.getChunkSource().getLoadedChunksCount()==before,"LOD generator loaded Minecraft chunks");
  try{var moon=server.getLevel(MoonSurface.dimension());var worlds=Class.forName("net.aste.aeroworks.wind.WindWorlds");worlds.getMethod("tick",net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class).invoke(null,new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,moon));var field=worlds.getDeclaredField("WORLDS");field.setAccessible(true);require(!((java.util.Map<?,?>)field.get(null)).containsKey(moon),"AeroWorks built a weather/flow graph on the Moon");
   var fields=Class.forName("net.aste.aeroworks.wind.WindFields");var providerType=Class.forName("net.aste.aeroworks.wind.WindField");var sampleType=Class.forName("net.aste.aeroworks.wind.WindSample");var count=new java.util.concurrent.atomic.AtomicInteger();var provider=java.lang.reflect.Proxy.newProxyInstance(providerType.getClassLoader(),new Class[]{providerType},(proxy,method,args)->{if(method.getName().equals("sample")){count.incrementAndGet();((org.joml.Vector3d)args[1]).set(3,4,5);}return null;});fields.getMethod("setProvider",providerType).invoke(null,provider);
   try{var ctor=sampleType.getConstructor(net.minecraft.server.level.ServerLevel.class,double.class,double.class,double.class,double.class);var sample=fields.getMethod("sampleForStep",sampleType,org.joml.Vector3d.class,org.joml.Vector3d.class);var wind=new org.joml.Vector3d(1);var torque=new org.joml.Vector3d(1);sample.invoke(null,ctor.newInstance(moon,0d,100d,0d,0d),wind,torque);require(wind.lengthSquared()==0&&torque.lengthSquared()==0&&count.get()==0,"Lunar wind queried a provider or retained force");sample.invoke(null,ctor.newInstance(server.overworld(),0d,100d,0d,0d),wind,torque);require(count.get()==1&&wind.equals(new org.joml.Vector3d(3,4,5)),"Earth wind was disabled too");}finally{fields.getMethod("resetProvider").invoke(null);}

  }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
 io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_COMPAT_SERVER_PASS selected DH override; exact columns accepted by real DH converter; no native chunk loads; AeroWorks graph/provider absent on Moon and retained on Earth");
 }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
