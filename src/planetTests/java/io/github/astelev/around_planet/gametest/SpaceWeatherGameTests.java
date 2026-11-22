package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class SpaceWeatherGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",batch="spaceWeather",timeoutTicks=100)
    public static void phantomSpawnerIsDeniedOnlyForPlayerInsideSpaceCell(GameTestHelper h){
        var level=h.getLevel();var cells=SpaceCells.get(level);var player=FakePlayerFactory.getMinecraft(level);var old=player.position();
        var cell=new SpaceCells.Cell(UUID.randomUUID(),79,24000,9000,0,0,2200,0,List.of(UUID.randomUUID()));
        try {
            cells.add(level,cell);player.setPos(24000,9000,0);
            var space=new PlayerSpawnPhantomsEvent(player,4);NeoForge.EVENT_BUS.post(space);
            h.assertTrue(space.getResult()==PlayerSpawnPhantomsEvent.Result.DENY&&!space.shouldSpawnPhantoms(level,player.blockPosition()),"Space inherited phantom spawning");
            player.setPos(24000,100,0);var surface=new PlayerSpawnPhantomsEvent(player,4);NeoForge.EVENT_BUS.post(surface);
            h.assertTrue(surface.getResult()==PlayerSpawnPhantomsEvent.Result.DEFAULT&&surface.getPhantomsToSpawn()==4,"Space changed surface phantom rules");h.succeed();
        } finally {player.setPos(old);cells.remove(level,cell);}
    }
}
