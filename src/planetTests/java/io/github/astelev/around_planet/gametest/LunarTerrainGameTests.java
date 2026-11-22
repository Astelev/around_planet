package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.moon.*;
import io.github.astelev.around_planet.space.MoonSurface;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class LunarTerrainGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="moonTerrain",timeoutTicks=400)
 public static void generatedColumnsBiomesAndPolarIceMatchSharedModel(GameTestHelper h){
  var moon=h.getLevel().getServer().getLevel(MoonSurface.dimension());h.assertTrue(moon!=null&&moon.getChunkSource().getGenerator() instanceof MoonChunkGenerator,"Moon did not load new generator");var gen=(MoonChunkGenerator)moon.getChunkSource().getGenerator();var terrain=gen.terrain();h.assertTrue(gen.getBiomeSource().possibleBiomes().size()==4,"Expected exactly four lunar biomes");
  var chosen=new EnumMap<MoonTerrain.Region,BlockPos>(MoonTerrain.Region.class);BlockPos ice=null;
  for(int z=0;z<80&&(chosen.size()<4||ice==null);z++)for(int x=0;x<100;x++){int wx=((int)((x/100.0-.5)*terrain.circumference())>>2)<<2,wz=((int)((z/80.0-.5)*terrain.circumference()/2)>>2)<<2;var s=terrain.sample(wx,wz);chosen.putIfAbsent(s.region(),new BlockPos(wx,s.height(),wz));if(s.ice()&&ice==null)ice=new BlockPos(wx,s.height(),wz);}
  h.assertTrue(chosen.size()==4&&ice!=null,"Missing terrain classes or polar ice");var points=new ArrayList<>(chosen.values());points.add(ice);
  for(var p:points){var s=terrain.sample(p.getX(),p.getZ());moon.getChunkAt(p);h.assertTrue(moon.getHeight(Heightmap.Types.WORLD_SURFACE,p.getX(),p.getZ())==s.height()+1,"Actual column height differs from atlas model at "+p);h.assertTrue(moon.getBlockState(p).is(s.ice()?Blocks.PACKED_ICE:MoonRegistry.REGOLITH.get()),"Wrong lunar surface block");h.assertTrue(moon.getBlockState(p.above()).isAir(),"Unexpected fluid or vegetation");String expected=switch(s.region()){case PLAINS->"moon_plains";case MARE->"moon_mare";case RIM->"moon_crater_mountains";case BASIN->"moon_crater_basin";};var biome=moon.getNoiseBiome(p.getX()>>2,p.getY()>>2,p.getZ()>>2);h.assertTrue(biome.unwrapKey().orElseThrow().location().getPath().equals(expected),"Biome differs from crater geometry");}
  var packet=new io.github.astelev.around_planet.network.MoonTerrainPayload(terrain.circumference(),terrain.seed());var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),moon.registryAccess());try{var codec=io.github.astelev.around_planet.network.MoonTerrainPayload.CODEC;codec.encode(buf,packet);h.assertTrue(codec.decode(buf).equals(packet)&&buf.readableBytes()==0,"Atlas descriptor lost world seed/scale");}finally{buf.release();}h.succeed();
 }
}
