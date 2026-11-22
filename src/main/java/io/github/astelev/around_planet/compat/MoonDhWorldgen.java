package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.moon.*;
import io.github.astelev.around_planet.space.MoonSurface;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.AbstractDhApiChunkWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.interfaces.block.*;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.data.*;
import com.seibel.distanthorizons.api.enums.worldGeneration.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Direct, exact height-field LOD columns. No temporary Minecraft chunks or worldgen pipeline. */
public final class MoonDhWorldgen extends AbstractDhApiChunkWorldGenerator {
 private final MoonTerrain terrain;private final Map<MoonTerrain.Region,IDhApiBiomeWrapper> biomes=new EnumMap<>(MoonTerrain.Region.class);
 private final IDhApiBlockStateWrapper regolith,ice,stone,bedrock;private volatile boolean closed;
 public static final java.util.concurrent.atomic.AtomicLong generated=new java.util.concurrent.atomic.AtomicLong();
 public static void init(){DhApi.events.bind(DhApiLevelLoadEvent.class,new DhApiLevelLoadEvent(){@Override public void onLevelLoad(DhApiEventParam<DhApiLevelLoadEvent.EventParam> e){var wrapper=e.value.levelWrapper;if(wrapper.getWrappedMcObject() instanceof ServerLevel level&&level.dimension().equals(MoonSurface.dimension())&&level.getChunkSource().getGenerator() instanceof MoonChunkGenerator){var result=DhApi.worldGenOverrides.registerWorldGeneratorOverride(wrapper,new MoonDhWorldgen(level,wrapper));io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_DH_GENERATOR registered={} message={}",result.success,result.message);}}});}
 public MoonDhWorldgen(ServerLevel level,IDhApiLevelWrapper wrapper){terrain=((MoonChunkGenerator)level.getChunkSource().getGenerator()).terrain();var f=DhApi.Delayed.wrapperFactory;regolith=f.getBlockStateWrapper(new Object[]{MoonRegistry.REGOLITH.get().defaultBlockState()},wrapper);ice=f.getBlockStateWrapper(new Object[]{Blocks.PACKED_ICE.defaultBlockState()},wrapper);stone=f.getBlockStateWrapper(new Object[]{Blocks.STONE.defaultBlockState()},wrapper);bedrock=f.getBlockStateWrapper(new Object[]{Blocks.BEDROCK.defaultBlockState()},wrapper);for(var region:MoonTerrain.Region.values()){String path=switch(region){case PLAINS->"moon_plains";case MARE->"moon_mare";case RIM->"moon_crater_mountains";case BASIN->"moon_crater_basin";};var holder=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("around_planet",path)));biomes.put(region,f.getBiomeWrapper(new Object[]{holder},wrapper));}}
 @Override public EDhApiWorldGeneratorReturnType getReturnType(){return EDhApiWorldGeneratorReturnType.API_CHUNKS;}
 @Override public int getPriority(){return 100;}
 @Override public Object[] generateChunk(int x,int z,EDhApiDistantGeneratorMode mode){throw new UnsupportedOperationException("Moon uses API_CHUNKS");}
 @Override public DhApiChunk generateApiChunk(int chunkX,int chunkZ,EDhApiDistantGeneratorMode mode){if(closed)throw new java.util.concurrent.CancellationException("Lunar generator closed");var chunk=DhApiChunk.create(chunkX,chunkZ,-64,320);for(int z=0;z<16;z++)for(int x=0;x<16;x++){var sample=terrain.sample(chunkX*16+x,chunkZ*16+z);var biome=biomes.get(sample.region());int top=sample.height()+1;var points=new ArrayList<DhApiTerrainDataPoint>(4);points.add(point(-64,-60,bedrock,biome));points.add(point(-60,top-5,stone,biome));if(sample.ice()){points.add(point(top-5,top-3,regolith,biome));points.add(point(top-3,top,ice,biome));}else points.add(point(top-5,top,regolith,biome));chunk.setDataPoints(x,z,points);}generated.incrementAndGet();return chunk;}
 private static DhApiTerrainDataPoint point(int bottom,int top,IDhApiBlockStateWrapper block,IDhApiBiomeWrapper biome){return DhApiTerrainDataPoint.create((byte)0,0,15,bottom,top,block,biome);}
 @Override public void preGeneratorTaskStart(){}@Override public void close(){closed=true;}
}
