package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.sensor.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
@PrefixGameTestTemplate(false)
public final class InstrumentInteractionGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void configurationRequiresEmptyHands(GameTestHelper h){
        var level=h.getLevel();var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InstrumentHands"));var p=h.absolutePos(new BlockPos(1,2,1));var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.EAST,p,false);
        try{for(var block:List.of(OrbitalSensorRegistry.BLOCK.get(),PropulsionRegistry.ENGINE.get(),PropulsionRegistry.RSU.get(),PropulsionRegistry.DAMPER.get(),io.github.astelev.around_planet.maneuver.ManeuverRegistry.PLANNER.get())){level.setBlockAndUpdate(p,block.defaultBlockState());var state=level.getBlockState(p);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_INGOT));player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(state.useWithoutItem(level,player,hit)==InteractionResult.PASS,"Held item was consumed by configuration on "+block);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK));h.assertTrue(state.useWithoutItem(level,player,hit)==InteractionResult.PASS,"Offhand item was consumed by configuration");player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(state.useWithoutItem(level,player,hit).consumesAction(),"Empty hands no longer open configuration");}h.succeed();}
        finally{player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);level.removeBlock(p,false);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void createWrenchRotatesInstrumentsWithoutLosingSettings(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}var level=h.getLevel();var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InstrumentWrench"));var p=h.absolutePos(new BlockPos(1,2,1));var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.EAST,p,false);var tool=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("create:wrench")));
        try{player.setItemInHand(InteractionHand.MAIN_HAND,tool);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);for(var block:List.of(OrbitalSensorRegistry.BLOCK.get(),PropulsionRegistry.ENGINE.get(),PropulsionRegistry.RSU.get(),PropulsionRegistry.DAMPER.get(),io.github.astelev.around_planet.maneuver.ManeuverRegistry.PLANNER.get())){level.setBlockAndUpdate(p,block.defaultBlockState());var before=level.getBlockEntity(p);if(before instanceof PropulsionBlockEntity machine){machine.configure(machine.kind()==PropulsionRegistry.Kind.DAMPER?3.5:337);if(machine.kind()==PropulsionRegistry.Kind.DAMPER)machine.energy.stored(12345);if(machine.kind()==PropulsionRegistry.Kind.RSU)machine.frequency(0,new ItemStack(Items.DIAMOND));}else if(before instanceof OrbitalSensorBlockEntity sensor)sensor.configure(io.github.astelev.around_planet.space.OrbitalAttitude.Mode.RETROGRADE,30);
            var state=level.getBlockState(p);h.assertTrue(state.useWithoutItem(level,player,hit)==InteractionResult.PASS,"Configuration intercepted the wrench");var result=tool.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));h.assertTrue(result.consumesAction()&&level.getBlockState(p).getValue(OrbitalSensorBlock.FACING)!=Direction.UP,"Native Create wrench did not rotate "+block);h.assertTrue(level.getBlockEntity(p)==before,"Wrench replaced the block entity");
            if(before instanceof PropulsionBlockEntity machine){h.assertTrue(machine.kind()==PropulsionRegistry.Kind.DAMPER?machine.strength()==3.5&&machine.energy.getEnergyStored()==12345:machine.thrust()==337,"Wrench lost machine configuration");if(machine.kind()==PropulsionRegistry.Kind.RSU)h.assertTrue(machine.frequency(0).is(Items.DIAMOND),"Wrench lost RSU frequency");}else if(before instanceof OrbitalSensorBlockEntity sensor)h.assertTrue(sensor.scale()==30&&sensor.mode()==io.github.astelev.around_planet.space.OrbitalAttitude.Mode.RETROGRADE,"Wrench reset sensor mode");
        }h.succeed();}finally{player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);level.removeBlock(p,false);}
    }
}
