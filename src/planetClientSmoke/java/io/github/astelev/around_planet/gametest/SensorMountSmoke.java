package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.sensor.*;
import io.github.astelev.around_planet.client.OrbitalSensorRenderer;
import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SensorMountSmoke {
    private static int ticks;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("around_planet.sensorMountSmoke"))return;var mc=Minecraft.getInstance();if(mc.player==null || mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20)mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=player.serverLevel();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,5,203,9,java.util.Set.of(),180,20);
            for(var d:Direction.values()){var p=new BlockPos(d.ordinal()*2,200,0);var state=OrbitalSensorRegistry.BLOCK.get().defaultBlockState().setValue(OrbitalSensorBlock.FACING,d);level.setBlockAndUpdate(p,state);if(state.getShape(level,p).isEmpty())throw new AssertionError("Missing mounted collision shape "+d);}
        });
        if(ticks==220){for(var d:Direction.values()){var p=new BlockPos(d.ordinal()*2,200,0);var s=mc.level.getBlockState(p);if(!s.is(OrbitalSensorRegistry.BLOCK.get()) || s.getValue(OrbitalSensorBlock.FACING)!=d)throw new AssertionError("Client lost mounting direction "+d);}if(OrbitalSensorRenderer.frames<6)throw new AssertionError("Mounted gimbals did not render");net.minecraft.client.Screenshot.grab(mc.gameDirectory,"orbital-sensor-six-mounts-alpha17.png",mc.getMainRenderTarget(),message->{});AroundPlanet.LOGGER.info("SENSOR_MOUNTS PASS: six client poses, collision shapes and rendered models");}
        if(ticks==240)mc.stop();
    }
}
