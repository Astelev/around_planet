package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.HashMap;
import java.util.UUID;

/** Real vanilla packet handlers and game mode; the source player is never teleported for an interaction. */
@PrefixGameTestTemplate(false)
public final class SeamInteractionGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=200)
    public static void nativePacketsUsePlaceMineAndRespectReach(GameTestHelper h) {
        var level=h.getLevel();PlanetBorders.bind(level.getWorldBorder(),new EastWestTopology(512));
        var p=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PeriodicActor"));
        var oldConnection=p.connection;
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false));
        var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        java.util.function.BiConsumer<BlockPos,net.minecraft.world.level.block.state.BlockState> put=(pos,state)->{saved.putIfAbsent(pos,level.getBlockState(pos));level.setBlockAndUpdate(pos,state);};
        var source=new BlockPos(-255,201,40);var image=source.offset(512,0,0);var base=source.below();var imageBase=image.below();
        var dig=source.offset(0,0,2);var imageDig=dig.offset(512,0,0);
        try {
            level.getChunk(-16,2);level.getChunk(16,2);p.setPos(257.5,201,43);p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            put.accept(base,Blocks.STONE.defaultBlockState());put.accept(source,Blocks.LEVER.defaultBlockState());put.accept(image,Blocks.BLUE_CONCRETE.defaultBlockState());
            var click=new BlockHitResult(new Vec3(257.5,201.5,41),Direction.SOUTH,image,false);
            p.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,click,1));
            h.assertTrue(level.getBlockState(source).getValue(LeverBlock.POWERED),"Use packet did not activate the canonical lever");
            h.assertTrue(level.getBlockState(image).is(Blocks.BLUE_CONCRETE),"Use wrote into the invisible guard world");
            put.accept(source,Blocks.AIR.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,8));
            var place=new BlockHitResult(new Vec3(257.5,201,40.5),Direction.UP,imageBase,false);
            p.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,place,2));
            h.assertTrue(level.getBlockState(source).is(Blocks.DIRT),"Placement did not use canonical block coordinates");
            put.accept(dig,Blocks.STONE.defaultBlockState());put.accept(imageDig,Blocks.BLUE_CONCRETE.defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            p.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,imageDig,Direction.SOUTH,3));
            h.assertTrue(level.getBlockState(dig).isAir(),"Creative mining did not remove the real opposite block");
            h.assertTrue(level.getBlockState(imageDig).is(Blocks.BLUE_CONCRETE),"Mining changed the raw guard block");
            put.accept(dig,Blocks.STONE.defaultBlockState());p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_PICKAXE));
            p.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,imageDig,Direction.SOUTH,4));
            for(int i=0;i<30;i++)p.gameMode.tick();
            p.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,imageDig,Direction.SOUTH,5));
            h.assertTrue(level.getBlockState(dig).isAir(),"Survival start/stop mining did not keep a canonical target");
            put.accept(dig,Blocks.STONE.defaultBlockState());p.setPos(257.5,201,70);p.setGameMode(GameType.CREATIVE);
            p.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,imageDig,Direction.SOUTH,6));
            h.assertTrue(level.getBlockState(dig).is(Blocks.STONE),"Far-away image packet bypassed reach validation");
            p.setPos(257.5,201,43);p.setGameMode(GameType.ADVENTURE);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            p.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,imageDig,Direction.SOUTH,7));
            h.assertTrue(level.getBlockState(dig).is(Blocks.STONE),"Periodic mining bypassed adventure restrictions");
            p.setGameMode(GameType.CREATIVE);p.setYRot(-90);p.setXRot(20);
            var water= new BlockPos(-253,201,43);put.accept(water,Blocks.AIR.defaultBlockState());
            put.accept(water.east(),Blocks.STONE.defaultBlockState());put.accept(water.offset(512,0,0),Blocks.BLUE_CONCRETE.defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
            p.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND,8,-90,20));
            h.assertTrue(level.getFluidState(water).is(net.minecraft.tags.FluidTags.WATER),"Native bucket ray placed fluid on the wrong side");
            h.assertTrue(level.getBlockState(water.offset(512,0,0)).is(Blocks.BLUE_CONCRETE),"Bucket changed raw guard terrain");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            p.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND,9,-90,20));
            h.assertTrue(level.getFluidState(water).isEmpty(),"Native bucket did not pick up canonical water");
            var clicked=new BlockPos(-256,201,40);var placed=new BlockPos(255,201,40);var rawNeighbor=new BlockPos(-257,201,40);
            put.accept(clicked,Blocks.STONE.defaultBlockState());put.accept(placed,Blocks.AIR.defaultBlockState());put.accept(rawNeighbor,Blocks.BLUE_CONCRETE.defaultBlockState());
            p.setPos(254.5,201,43);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
            p.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(256,201.5,40.5),Direction.WEST,clicked.offset(512,0,0),false),10));
            h.assertTrue(level.getBlockState(placed).is(Blocks.DIRT),"Derived placement neighbor was not canonicalized at the seam face");
            h.assertTrue(level.getBlockState(rawNeighbor).is(Blocks.BLUE_CONCRETE),"Placement wrote into the opposite guard neighbor");
            put.accept(source,Blocks.AIR.defaultBlockState());p.setPos(257.5,201,40.5);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
            p.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(257.5,201,40.5),Direction.UP,imageBase,false),11));
            h.assertTrue(level.getBlockState(source).isAir(),"Periodic placement bypassed the acting player's body collision");
            h.assertTrue(p.getX()==257.5,"Interaction moved the player independently of its carrier");h.succeed();
        } finally {p.connection=oldConnection;saved.forEach(level::setBlockAndUpdate);PlanetBorders.unbind(level.getWorldBorder());}
    }
}
