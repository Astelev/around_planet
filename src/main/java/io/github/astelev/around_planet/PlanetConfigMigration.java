package io.github.astelev.around_planet;
import com.electronwill.nightconfig.core.*;
import net.neoforged.fml.config.*;
/** Run migration before the normal spec corrector removes obsolete keys. Delegate all actual config storage. */
public final class PlanetConfigMigration implements IConfigSpec {
    public static void migrate(CommentedConfig config){
        // Upgrade the old shipped default once, while retaining custom ascent thresholds.
        if(!config.contains("space.atmosphereTop") && config.get("space.entryY") instanceof Number n && n.intValue()==768)
            config.set("space.entryY",2208);
        if(!config.contains("space.cloudCeilingRevision")){
            if(config.get("space.entryY") instanceof Number n && n.intValue()==3000)config.set("space.entryY",2208);
            config.set("space.cloudCeilingRevision",1);
        }
        if(!config.contains("space.atmosphereProfileRevision")){
            if(config.get("space.atmosphereTop") instanceof Number n && n.intValue()==6000)config.set("space.atmosphereTop",4000);
            if(config.get("space.atmosphereDrag") instanceof Number n && Math.abs(n.doubleValue()-.02)<1e-9)config.set("space.atmosphereDrag",.012);
            if(config.get("space.reentryMaxSpeed") instanceof Number n && n.doubleValue()==20)config.set("space.reentryMaxSpeed",50.0);
            config.set("space.atmosphereProfileRevision",1);
        }
        if(!config.contains("planet.circumferenceBlocks")){
            Object old=config.get("eastWest.boundaryX");
            if(old instanceof Number n && n.longValue()>=128 && n.longValue()<=29_500_000){long size=(n.longValue()*2+16)/32*32;config.set("planet.circumferenceBlocks",(int)size);}
        }
    }
    @Override public boolean isEmpty(){return PlanetConfig.SPEC.isEmpty();}
    @Override public void validateSpec(ModConfig config){PlanetConfig.SPEC.validateSpec(config);}
    @Override public boolean isCorrect(UnmodifiableCommentedConfig config){return PlanetConfig.SPEC.isCorrect(config);}
    @Override public void correct(CommentedConfig config){migrate(config);PlanetConfig.SPEC.correct(config);}
    @Override public void acceptConfig(ILoadedConfig config){if(config!=null){migrate(config.config());}PlanetConfig.SPEC.acceptConfig(config);}
}
