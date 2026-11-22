package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.compat.SpaceMapSnapshot;
import io.github.astelev.around_planet.network.SpaceMapPayload;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
@PrefixGameTestTemplate(false)
public final class MapChartGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="mapCharts",timeoutTicks=100)
 public static void lunarObserverReceivesUnloadedEarthObjectsAndTheirPrimaryThroughNetworkCodec(GameTestHelper h){var level=h.getLevel();var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);var old=player.position();var focus=UUID.randomUUID();var other=UUID.randomUUID();var cell=new SpaceCells.Cell(focus,79,24000,9000,0,0,2200,0,List.of(UUID.randomUUID()));try{var planet=new PlanetScale(131072).geometry();long now=level.getGameTime();data.put(focus,new SpaceTrajectory(CelestialSystem.MOON,now,new SpaceState(new SpaceVector(12000,0,0),new SpaceVector(0,50,0)),5e7));data.put(other,new SpaceTrajectory(CelestialSystem.EARTH,now,new SpaceState(new SpaceVector(90000,0,0),new SpaceVector(0,200,0)),4e9));data.navigate(focus,new SpaceNavigation(planet,SpaceFrame.IDENTITY));data.navigate(other,new SpaceNavigation(planet,SpaceFrame.IDENTITY));cells.add(level,cell);player.setPos(24000,9000,0);var snapshot=SpaceMapSnapshot.sample(player);h.assertTrue(snapshot!=null,"Lunar observer has no map catalog");var earth=snapshot.bodies().stream().filter(b->b.id().equals(other)).findFirst().orElseThrow(()->new AssertionError("Earth object hidden from lunar observer"));h.assertTrue(earth.primary().equals(CelestialSystem.EARTH)&&earth.x()==90000&&!earth.loaded(),"Earth object lost its native chart or was force loaded");var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());try{SpaceMapPayload.CODEC.encode(buf,snapshot);var copy=SpaceMapPayload.CODEC.decode(buf);h.assertTrue(copy.equals(snapshot),"Catalog primary lost during network encoding");}finally{buf.release();}h.succeed();}finally{player.setPos(old);cells.remove(level,cell);data.remove(focus);data.remove(other);}}
}
