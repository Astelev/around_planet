package io.github.astelev.around_planet.gametest;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.compat.SablePassengers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import java.util.*;

/** Real player, native ray picking/packets, a driven physical deck, and both directions of carrier transfer. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class ShipTransferSmoke {
    private static int ticks,phase,started,crossTicks;private static volatile Throwable failure;
    private static volatile ServerSubLevel ship;private static volatile UUID id;private static Object mesh,clientIdentity;
    private static volatile int drive;private static volatile boolean sourcePowered,sourceAir,sourcePlaced,serverMoved;
    private static final BlockPos SOURCE=new BlockPos(-177,202,0),IMAGE=SOURCE.offset(512,0,0),CHEST=SOURCE.offset(0,0,2);
    private static double localX;
    @SubscribeEvent public static void drive(LevelTickEvent.Pre event) {
        if(!Boolean.getBoolean("around_planet.shipTransferSmoke") || drive==0 || ship==null || event.getLevel()!=ship.getLevel())return;
        try {
            var system=SubLevelContainer.getContainer(ship.getLevel()).physicsSystem();var pipeline=system.getPipeline();
            double centerX=ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass())).x;
            if(drive>0 && centerX<0 || drive<0 && centerX>0) {
                if(++crossTicks>=4){pipeline.resetVelocity(ship);system.setPaused(true);drive=0;serverMoved=true;
                    var p=ship.getLevel().players().getFirst();AroundPlanet.LOGGER.info("SHIP_TRANSFER_STOP server shipX={} playerX={} local={}",ship.logicalPose().position().x,p.getX(),p.getX()-ship.logicalPose().position().x);return;}
            }
            var v=pipeline.getLinearVelocity(ship,new Vector3d());var w=pipeline.getAngularVelocity(ship,new Vector3d());
            pipeline.addLinearAndAngularVelocity(ship,new Vector3d(drive*4-v.x,-v.y,-v.z),w.negate());
        }catch(Throwable e){failure=e;}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.shipTransferSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SHIP_TRANSFER server failure",failure);
        if(ticks>2600)throw new AssertionError("SHIP_TRANSFER timeout phase="+phase+" x="+(mc.player==null?null:mc.player.getX()));
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        if(ticks%100==0)AroundPlanet.LOGGER.info("SHIP_TRANSFER state phase={} x={} y={} hit={} actorShip={}",phase,mc.player.getX(),mc.player.getY(),mc.hitResult instanceof BlockHitResult h?h.getBlockPos():mc.hitResult,
                SablePassengers.ship(mc.player)==null?null:SablePassengers.ship(mc.player).getUniqueId());
        if(phase==1) {
            var eye=mc.player.getEyePosition();double dx=IMAGE.getX()+0.5-eye.x,dz=0.5-eye.z;
            mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));
            mc.player.setXRot((float)Math.toDegrees(Math.atan2(eye.y-(SOURCE.getY()+0.18),Math.hypot(dx,dz))));
        }
        if(phase==0) {
            phase=10;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    PlanetConfig.SHIP_TRANSFER.set(true);
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();var container=SubLevelContainer.getContainer(level);
                    for(var old:new ArrayList<>(container.getAllSubLevels()))if(old.getName()!=null && old.getName().startsWith("around_planet_transfer_test"))container.removeSubLevel(old,SubLevelRemovalReason.REMOVED);
                    p.stopRiding();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
                    for(int x:new int[]{-17,-16,-15,15,16,17})for(int z=-1;z<=1;z++)level.getChunk(x,z);
                    var anchor=new BlockPos(20,200,20);var blocks=new ArrayList<BlockPos>();
                    for(int x=0;x<177;x++)for(int z=0;z<5;z++){var pos=anchor.offset(x,0,z);level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());blocks.add(pos);}
                    ship=SubLevelAssemblyHelper.assembleBlocks(level,anchor,blocks,new BoundingBox3i(20,200,20,196,200,24));id=ship.getUniqueId();ship.setName("around_planet_transfer_test");
                    container.addForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
                    var center=ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass()));
                    var delta=new Vector3d(254,200,0.5).sub(center);
                    container.physicsSystem().getPipeline().teleport(ship,new Vector3d(ship.logicalPose().position()).add(delta),ship.logicalPose().orientation());
                    AroundPlanet.LOGGER.info("SHIP_FIXTURE pose={} center={} rp={}",ship.logicalPose().position(),ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass())),ship.logicalPose().rotationPoint());ship.updateBoundingBox();ship.updateLastPose();container.physicsSystem().setPaused(true);
                    for(int x:new int[]{SOURCE.getX(),IMAGE.getX()})for(int z:new int[]{0,2})for(int y=195;y<=205;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(SOURCE.below(),Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(SOURCE,Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
                    level.setBlockAndUpdate(IMAGE,Blocks.BLUE_CONCRETE.defaultBlockState());
                    level.setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.connection.teleport(250,205,0.5,-90,0);

                }catch(Throwable e){failure=e;}
            });
        } else if(phase==10 && ticks-started>40 && client(mc)!=null) {
            var local=client(mc).logicalPose().transformPositionInverse(new Vector3d(332,200.49,0.5));
            if(!mc.level.getBlockState(BlockPos.containing(local.x,local.y,local.z)).is(Blocks.IRON_BLOCK))return;
            phase=1;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.getAbilities().flying=false;p.onUpdateAbilities();p.setNoGravity(false);
                p.connection.teleport(332,ship.boundingBox().maxY()+0.01,0.5,-90,0);
                ((dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension)p).sable$setTrackingSubLevel(ship);
            });
        } else if(phase==1 && ticks-started>60 && client(mc)!=null && mc.player.getX()>256
                && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(IMAGE) && SablePassengers.ship(mc.player)==client(mc)) {
            require(mc.player.getX()>288,"Outboard fixture did not clear the original safety border and first preview strip");
            require(client(mc).logicalPose().position().x<256,"Passenger caused premature carrier teleport");
            mesh=client(mc).getRenderData();clientIdentity=client(mc);localX=client(mc).logicalPose().transformPositionInverse(new Vector3d(mc.player.getX(),mc.player.getY(),mc.player.getZ())).x;
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit);phase=2;started=ticks;
        } else if(phase==2) {
            probe(mc);
            if(!sourcePowered || image(mc,IMAGE)==null || !image(mc,IMAGE).getValue(LeverBlock.POWERED))return;
            AroundPlanet.LOGGER.info("SHIP_INTERACTION_USE PASS: native ray picked and used a real opposite lever while passenger remained outboard");
            mc.gameMode.startDestroyBlock(IMAGE,Direction.WEST);phase=3;started=ticks;
        } else if(phase==3) {
            probe(mc);if(!sourceAir || image(mc,IMAGE)==null || !image(mc,IMAGE).isAir())return;
            AroundPlanet.LOGGER.info("SHIP_INTERACTION_MINE PASS: actual client mining changed canonical block and refreshed its read-only image");
            phase=4;started=ticks;
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,8));p.inventoryMenu.broadcastChanges();});
        } else if(phase==4 && ticks-started>10 && mc.player.getMainHandItem().is(Items.DIRT)) {
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(IMAGE.getX()+0.5,SOURCE.getY(),0.5),Direction.UP,IMAGE.below(),false));phase=5;started=ticks;
        } else if(phase==5) {
            probe(mc);if(!sourcePlaced || image(mc,IMAGE)==null || !image(mc,IMAGE).is(Blocks.DIRT))return;
            AroundPlanet.LOGGER.info("SHIP_INTERACTION_PLACE PASS: actual client placed on the opposite shore without changing player coordinates");
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(IMAGE.getX(),SOURCE.getY()+0.5,2.5),Direction.WEST,CHEST.offset(512,0,0),false));phase=6;started=ticks;
        } else if(phase==6 && ticks-started>30 && mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen) {
            require(mc.player.getX()>256,"Opening a menu independently wrapped the passenger");
            AroundPlanet.LOGGER.info("SHIP_INTERACTION_MENU PASS: opposite chest stays valid while player is on the outboard deck");
            mc.player.closeContainer();phase=7;started=ticks;
            mc.getSingleplayerServer().execute(()->{for(int x:new int[]{SOURCE.getX(),IMAGE.getX()})for(int z:new int[]{0,2})for(int y=195;y<=205;y++)ship.getLevel().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                serverMoved=false;crossTicks=0;drive=1;SubLevelContainer.getContainer(ship.getLevel()).physicsSystem().setPaused(false);});
        } else if(phase==7 && serverMoved && ticks-started>40 && mc.player.getX()<0 && client(mc)!=null) {
            AroundPlanet.LOGGER.info("SHIP_TRANSFER_OFFSET client shipX={} playerX={} beforeLocal={} afterLocal={} delta={}",client(mc).logicalPose().position().x,mc.player.getX(),localX,client(mc).logicalPose().transformPositionInverse(new Vector3d(mc.player.getX(),mc.player.getY(),mc.player.getZ())).x,client(mc).logicalPose().transformPositionInverse(new Vector3d(mc.player.getX(),mc.player.getY(),mc.player.getZ())).x-localX);
            require(client(mc)==clientIdentity && client(mc).getRenderData()==mesh,"Transfer respawned the ship or rebuilt its mesh identity");
            require(SablePassengers.ship(mc.player)==client(mc),"Player lost native carrier attachment after east transfer");
            require(Math.abs(client(mc).logicalPose().transformPositionInverse(new Vector3d(mc.player.getX(),mc.player.getY(),mc.player.getZ())).x-localX)<0.6,"Passenger local position changed at east transfer: before="+localX+" after="+client(mc).logicalPose().transformPositionInverse(new Vector3d(mc.player.getX(),mc.player.getY(),mc.player.getZ())).x);
            AroundPlanet.LOGGER.info("SHIP_TRANSFER_EAST PASS: real standing player and native ship move together, same plot/GPU buffers and attachment");
            phase=8;started=ticks;
            mc.getSingleplayerServer().execute(()->{serverMoved=false;crossTicks=0;drive=-1;SubLevelContainer.getContainer(ship.getLevel()).physicsSystem().setPaused(false);});
        } else if(phase==8 && serverMoved && ticks-started>60 && mc.player.getX()>256 && client(mc)!=null) {
            require(SablePassengers.ship(mc.player)==client(mc),"Standing player lost carrier attachment after west transfer");
            require(Math.abs(client(mc).logicalPose().transformPositionInverse(new Vector3d(mc.player.getX(),mc.player.getY(),mc.player.getZ())).x-localX)<0.6,"Passenger local position changed at reverse transfer");
            require(client(mc)==clientIdentity && client(mc).getRenderData()==mesh,"Reverse transfer replaced the plot mesh");
            testLateSnapshot();AroundPlanet.LOGGER.info("SHIP_TRANSFER_WEST PASS: reverse carrier transfer leaves outboard passenger on the same ship without independent bounce");
            phase=9;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var container=SubLevelContainer.getContainer(ship.getLevel());
                    p.connection.teleport(0,205,0,0,0);p.getAbilities().flying=true;p.onUpdateAbilities();
                    container.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);container.physicsSystem().setPaused(false);
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==9 && ticks-started>40)mc.stop();
    }
    private static void testLateSnapshot() {
        var pose=new dev.ryanhcode.sable.companion.math.Pose3d();pose.position().set(256.2,200,0);
        var interpolator=new dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator(pose);
        ((io.github.astelev.around_planet.compat.ShipInterpolation)interpolator).around_planet$rebase(256.2,-512,512);
        interpolator.setFirstPoses(pose,pose);
        interpolator.receiveSnapshot(1,pose);var other=new dev.ryanhcode.sable.companion.math.Pose3d().set(pose);other.position().x=-255.7;interpolator.receiveSnapshot(2,other);
        var sample=new dev.ryanhcode.sable.companion.math.Pose3d();interpolator.getSampleAt(1.5,sample);
        require(sample.position().x< -255 && sample.position().x> -256,"Late old-coordinate UDP snapshot caused interpolation across the world");
        AroundPlanet.LOGGER.info("SHIP_TRANSFER_INTERPOLATION PASS: late old-side snapshot normalizes into the new coordinate history");
    }
    private static void probe(Minecraft mc) {
        mc.getSingleplayerServer().execute(()->{var state=ship.getLevel().getBlockState(SOURCE);sourcePowered=state.is(Blocks.LEVER)&&state.getValue(LeverBlock.POWERED);sourceAir=state.isAir();sourcePlaced=state.is(Blocks.DIRT);});
    }
    private static net.minecraft.world.level.block.state.BlockState image(Minecraft mc,BlockPos pos){var c=SeamImages.get(mc.level,pos.getX()>>4,pos.getZ()>>4);return c==null?null:c.getBlockState(pos);}
    private static ClientSubLevel client(Minecraft mc){return id==null?null:(ClientSubLevel)SubLevelContainer.getContainer(mc.level).getSubLevel(id);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
