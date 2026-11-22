package io.github.astelev.around_planet.moon;
import com.mojang.serialization.*;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
/** Height-field Moon with no aquifers, caves, vegetation or vanilla climate router. */
public final class MoonChunkGenerator extends ChunkGenerator {
 public static final MapCodec<MoonChunkGenerator> CODEC=MoonBiomeSource.CODEC.codec().fieldOf("biome_source").xmap(MoonChunkGenerator::new,g->(MoonBiomeSource)g.biomeSource);
 public MoonChunkGenerator(MoonBiomeSource source){super(source);}
 @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
 public MoonTerrain terrain(){return ((MoonBiomeSource)biomeSource).terrain();}
 @Override public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures,RandomState random,long seed){((MoonBiomeSource)biomeSource).seed(MoonTerrain.worldSeed(seed));return super.createState(structures,random,seed);}
 private static BlockState block(MoonTerrain.Sample sample,int y){if(y<=-61)return Blocks.BEDROCK.defaultBlockState();if(y>sample.height())return Blocks.AIR.defaultBlockState();if(sample.ice()&&y>sample.height()-3)return Blocks.PACKED_ICE.defaultBlockState();return y>sample.height()-5?MoonRegistry.REGOLITH.get().defaultBlockState():Blocks.STONE.defaultBlockState();}
 /** Biomes are vertical columns: evaluate each of the 16 quart columns once, not at every Y. */
 @Override public CompletableFuture<ChunkAccess> createBiomes(RandomState r,Blender b,StructureManager m,ChunkAccess chunk){var palette=new ArrayList<Holder<Biome>>(16);int qx=chunk.getPos().getMinBlockX()>>2,qz=chunk.getPos().getMinBlockZ()>>2;for(int z=0;z<4;z++)for(int x=0;x<4;x++)palette.add(biomeSource.getNoiseBiome(qx+x,0,qz+z,r.sampler()));chunk.fillBiomesFromNoise((x,y,z,sampler)->palette.get((x&3)+(z&3)*4),r.sampler());return CompletableFuture.completedFuture(chunk);}
 @Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender b,RandomState r,StructureManager m,ChunkAccess chunk){var terrain=terrain();var pos=chunk.getPos();var cursor=new BlockPos.MutableBlockPos();for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=pos.getMinBlockX()+x,wz=pos.getMinBlockZ()+z;var s=terrain.sample(wx,wz);for(int y=Math.max(getMinY(),chunk.getMinBuildHeight());y<=s.height()&&y<chunk.getMaxBuildHeight();y++)chunk.setBlockState(cursor.set(wx,y,wz),block(s,y),false);}Heightmap.primeHeightmaps(chunk,Set.of(Heightmap.Types.OCEAN_FLOOR_WG,Heightmap.Types.WORLD_SURFACE_WG));return CompletableFuture.completedFuture(chunk);}
 @Override public int getBaseHeight(int x,int z,Heightmap.Types t,LevelHeightAccessor level,RandomState r){return Math.clamp(terrain().sample(x,z).height()+1,level.getMinBuildHeight(),level.getMaxBuildHeight());}
 @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor level,RandomState r){int min=Math.max(getMinY(),level.getMinBuildHeight()),max=Math.min(getMinY()+getGenDepth(),level.getMaxBuildHeight());var states=new BlockState[Math.max(0,max-min)];var s=terrain().sample(x,z);for(int y=min;y<max;y++)states[y-min]=block(s,y);return new NoiseColumn(min,states);}
 @Override public int getGenDepth(){return 384;}@Override public int getMinY(){return -64;}@Override public int getSeaLevel(){return 60;}
 @Override public void applyCarvers(WorldGenRegion l,long seed,RandomState r,BiomeManager b,StructureManager m,ChunkAccess c,GenerationStep.Carving step){}
 @Override public void buildSurface(WorldGenRegion l,StructureManager m,RandomState r,ChunkAccess c){}
 @Override public void spawnOriginalMobs(WorldGenRegion l){}
 @Override public void addDebugScreenInfo(List<String> text,RandomState random,BlockPos p){var s=terrain().sample(p.getX(),p.getZ());text.add("Moon: "+s.region()+" y="+s.height()+(s.ice()?" polar ice":""));}
}
