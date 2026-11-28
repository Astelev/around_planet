package io.github.astelev.around_planet.moon;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import java.util.stream.Stream;
public final class MoonBiomeSource extends BiomeSource {
 public static final MapCodec<MoonBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Biome.CODEC.fieldOf("plains").forGetter(s->s.plains),Biome.CODEC.fieldOf("mare").forGetter(s->s.mare),Biome.CODEC.fieldOf("rim").forGetter(s->s.rim),Biome.CODEC.fieldOf("basin").forGetter(s->s.basin)).apply(i,MoonBiomeSource::new));
 private final Holder<Biome> plains,mare,rim,basin;private volatile long seed;
 public MoonBiomeSource(Holder<Biome> plains,Holder<Biome> mare,Holder<Biome> rim,Holder<Biome> basin){this.plains=plains;this.mare=mare;this.rim=rim;this.basin=basin;}
 public void seed(long value){seed=value;}
 public MoonTerrain terrain(){double size=io.github.astelev.around_planet.PlanetConfig.SPEC.isLoaded()?io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get():131072;return new MoonTerrain(io.github.astelev.around_planet.space.MoonScale.fromEarthCircumference(size).circumference(),seed);}
 @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
 @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.of(plains,mare,rim,basin);}
 @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){return switch(terrain().sample(x*4.0,z*4.0).region()){case PLAINS->plains;case MARE->mare;case RIM->rim;case BASIN->basin;};}
}
