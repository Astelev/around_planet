package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.client.PropulsionExhaustRenderer;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class ExhaustClientSmoke {
    private static int ticks;private static long offFrames;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("around_planet.exhaustSmoke"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),2.5,200,5,java.util.Set.of(),180,20);var l=p.serverLevel();l.setDayTime(6000);
            l.setBlockAndUpdate(new BlockPos(0,200,0),PropulsionRegistry.ENGINE.get().defaultBlockState());l.setBlockAndUpdate(new BlockPos(-1,200,0),Blocks.REDSTONE_BLOCK.defaultBlockState());l.setBlockAndUpdate(new BlockPos(2,200,0),PropulsionRegistry.RSU.get().defaultBlockState());l.setBlockAndUpdate(new BlockPos(4,200,0),PropulsionRegistry.ENGINE.get().defaultBlockState().setValue(OrbitalSensorBlock.FACING,Direction.EAST));l.setBlockAndUpdate(new BlockPos(4,199,0),Blocks.REDSTONE_BLOCK.defaultBlockState());for(int x:new int[]{0,4})((PropulsionBlockEntity)l.getBlockEntity(new BlockPos(x,200,0))).configure(10000);
            var rsu=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0));rsu.configure(1000);for(int i=0;i<8;i++)rsu.frequency(i,net.minecraft.world.item.ItemStack.EMPTY);rsu.receiveSignal(0,15);rsu.receiveSignal(1,8);rsu.receiveSignal(3,3);
        });
        if(ticks==100){if(PropulsionExhaustRenderer.frames==0||PropulsionExhaustRenderer.quads<10)throw new AssertionError("Powered exhaust sprites did not render");var rsu=(PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(2,200,0));if(rsu.signal(0)!=15||rsu.signal(1)!=8||rsu.signal(2)!=0||rsu.signal(3)!=3)throw new AssertionError("Client nozzle signals are incorrect");snap(mc,"exhaust-powered-alpha20.png");AroundPlanet.LOGGER.info("EXHAUST_ON PASS: animated engine/RSU atlas sprites, multiple mounting axes, independently synchronized nozzles");}
        if(ticks==120)mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().overworld();l.setBlockAndUpdate(new BlockPos(-1,200,0),Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(new BlockPos(4,199,0),Blocks.AIR.defaultBlockState());var rsu=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0));for(int i=0;i<4;i++)rsu.receiveSignal(i,0);});
        if(ticks==145)offFrames=PropulsionExhaustRenderer.frames;
        if(ticks==165){if(PropulsionExhaustRenderer.frames!=offFrames)throw new AssertionError("Unpowered exhaust kept rendering");snap(mc,"exhaust-off-alpha20.png");AroundPlanet.LOGGER.info("EXHAUST_OFF PASS: inactive engines allocate no jet geometry");}
        if(ticks==180)mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().overworld();var p=new BlockPos(2,200,0);l.setBlockAndUpdate(p,l.getBlockState(p).setValue(OrbitalSensorBlock.FACING,Direction.WEST));var be=(PropulsionBlockEntity)l.getBlockEntity(p);be.receiveSignal(2,15);});
        if(ticks==210){if(PropulsionExhaustRenderer.frames<=offFrames)throw new AssertionError("Wall-mounted exhaust did not resume");snap(mc,"exhaust-wall-alpha20.png");AroundPlanet.LOGGER.info("EXHAUST_WALL PASS: rotated nozzle sprite resumes without particles");}
        if(ticks==230)mc.stop();
    }
    private static void snap(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
}
