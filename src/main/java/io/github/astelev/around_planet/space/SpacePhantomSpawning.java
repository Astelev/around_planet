package io.github.astelev.around_planet.space;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

/** Space cells share the Overworld, but do not inherit its insomnia spawner. */
@EventBusSubscriber(modid="around_planet")
public final class SpacePhantomSpawning {
    @SubscribeEvent public static void spawn(PlayerSpawnPhantomsEvent event) {
        if(SpaceCells.contains(event.getEntity()))event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
    }
    private SpacePhantomSpawning(){}
}
