package io.github.astelev.around_planet;
import com.electronwill.nightconfig.core.*;
import net.neoforged.fml.config.*;
/** Run migration before the normal spec corrector removes obsolete keys. Delegate all actual config storage. */
public final class PlanetConfigMigration implements IConfigSpec {
    public static void migrate(CommentedConfig config){
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
