package io.github.astelev.around_planet.atlas;
import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
/** Bounded incremental noise-biome queries, never chunk generation or loading. */
public final class PlanetAtlasData extends SavedData {
    public static final String ID="around_planet_biome_atlas";
    private int[] pixels=new int[PlanetAtlas.SIZE];private int progress,circumference;private long seed;
    public static PlanetAtlasData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new Factory<>(PlanetAtlasData::new,PlanetAtlasData::load),ID);}
    public void validate(ServerLevel level,int size){if(circumference!=size||seed!=level.getSeed()){circumference=size;seed=level.getSeed();progress=0;pixels=new int[PlanetAtlas.SIZE];setDirty();AroundPlanet.LOGGER.info("PLANET_ATLAS generating {}x{} circumference={}",PlanetAtlas.WIDTH,PlanetAtlas.HEIGHT,size);}}
    public boolean ready(){return progress==PlanetAtlas.SIZE;}
    public int progress(){return progress;}
    public int circumference(){return circumference;}
    public int[] pixels(){return pixels;}
    public void advance(ServerLevel level){
        if(ready())return;long until=System.nanoTime()+4_000_000;int start=progress;
        var generator=level.getChunkSource().getGenerator();var sampler=level.getChunkSource().randomState().sampler();
        for(int count=0;count<256&&progress<PlanetAtlas.SIZE;count++){
            int x=(int)Math.floor(PlanetAtlas.worldX(progress%PlanetAtlas.WIDTH,circumference)),z=(int)Math.floor(PlanetAtlas.worldZ(progress/PlanetAtlas.WIDTH,circumference));
            var biome=generator.getBiomeSource().getNoiseBiome(x>>2,generator.getSeaLevel()>>2,z>>2,sampler);
            pixels[progress++]=color(biome,x,z);
            if(System.nanoTime()>=until)break;
        }
        if(progress!=start)setDirty();
        if(progress/32768!=start/32768&&!ready())AroundPlanet.LOGGER.info("PLANET_ATLAS progress={}/{}",progress,PlanetAtlas.SIZE);
        if(ready())AroundPlanet.LOGGER.info("PLANET_ATLAS ready samples={} (cached in world data)",progress);
    }
    private static int color(Holder<Biome> holder,int x,int z){
        var biome=holder.value();String name=holder.unwrapKey().map(k->k.location().getPath()).orElse("");
        if(holder.is(BiomeTags.IS_OCEAN)||holder.is(BiomeTags.IS_RIVER)){int water=biome.getWaterColor();int r=(water>>>16)&255,g=(water>>>8)&255,b=water&255;return ((r/3)<<16)|((int)(g*.65)<<8)|(int)(b*.8);}
        if(biome.getBaseTemperature()<.15f)return 0xdde7e9;
        if(name.contains("badlands"))return 0xbc7449;
        if(name.contains("desert")||holder.is(BiomeTags.IS_BEACH))return 0xd8c38c;
        if(name.contains("stony")||name.contains("rocky"))return 0x8b9390;
        int grass=PlanetAtlas.landColor(biome.getBaseTemperature(),biome.getModifiedClimateSettings().downfall());
        if(holder.is(BiomeTags.IS_JUNGLE))grass=0x388449;
        else if(holder.is(BiomeTags.IS_TAIGA))grass=0x608579;
        else if(holder.is(BiomeTags.IS_FOREST))grass=0x528847;
        else if(holder.is(BiomeTags.IS_SAVANNA))grass=0xb1ac5d;
        var effects=biome.getSpecialEffects();return effects.getGrassColorModifier().modifyColor(x,z,effects.getGrassColorOverride().orElse(grass));
    }
    public static PlanetAtlasData load(CompoundTag tag,HolderLookup.Provider lookup){
        var data=new PlanetAtlasData();int[] pixels=tag.getIntArray("Pixels");
        if(tag.getInt("Version")==2&&pixels.length==PlanetAtlas.SIZE){data.pixels=pixels;data.progress=Math.clamp(tag.getInt("Progress"),0,PlanetAtlas.SIZE);data.seed=tag.getLong("Seed");data.circumference=tag.getInt("Circumference");}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.putInt("Version",2);tag.putInt("Circumference",circumference);tag.putLong("Seed",seed);tag.putInt("Progress",progress);tag.putIntArray("Pixels",pixels);return tag;}
}
