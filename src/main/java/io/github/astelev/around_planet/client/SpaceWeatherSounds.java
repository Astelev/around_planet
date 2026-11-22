package io.github.astelev.around_planet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import java.util.List;

/** Listener-local weather isolation; surface weather and other players stay unaffected. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SpaceWeatherSounds {
    private static final List<ResourceLocation> THUNDER=List.of(
        ResourceLocation.parse("minecraft:entity.lightning_bolt.thunder"),
        ResourceLocation.parse("simpleclouds:distant_thunder"),
        ResourceLocation.parse("simpleclouds:close_thunder"));
    private static boolean wasInSpace;
    public static boolean weather(SoundInstance sound){return sound.getSource()==SoundSource.WEATHER||THUNDER.contains(sound.getLocation());}
    @SubscribeEvent public static void play(PlaySoundEvent event){
        if(SpaceRenderView.active()&&event.getSound()!=null&&weather(event.getSound()))event.setSound(null);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        boolean inSpace=SpaceRenderView.active();
        if(inSpace&&!wasInSpace){var sounds=Minecraft.getInstance().getSoundManager();sounds.stop(null,SoundSource.WEATHER);for(var id:THUNDER)sounds.stop(id,null);}
        wasInSpace=inSpace;
    }
    private SpaceWeatherSounds(){}
}
