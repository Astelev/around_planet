package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamEntityImages;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.util.UUID;

/** Actual custom channel and GPU check; excluded from the production JAR. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamEntitySmoke {
    private static int ticks,phase,started;
    private static volatile UUID slime,item,stand,boat,pig,west;
    private static volatile Throwable failure;
    private static volatile java.util.Map<UUID,Integer> sourceLight=java.util.Map.of();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.entitySmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("Entity fixture failed",failure);
        if(ticks>2200)throw new AssertionError("SEAM_ENTITY_SMOKE timeout phase="+phase+" images="+SeamEntityImages.size());
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || ticks<100)return;
        mc.player.setNoGravity(true);mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(ticks%5==0 && slime!=null)mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();var values=new java.util.HashMap<UUID,Integer>();
            for(var id:new UUID[]{slime,item,stand,boat,pig,west})if(id!=null) {
                var entity=level.getEntity(id);if(entity==null)continue;
                var pos=net.minecraft.core.BlockPos.containing(entity.getLightProbePosition(1));
                values.put(id,net.minecraft.client.renderer.LightTexture.pack(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos),level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos)));
            }
            sourceLight=java.util.Map.copyOf(values);
        });
        if(phase==0) {
            phase=1;started=ticks;mc.options.renderDistance().set(12);mc.options.setServerRenderDistance(12);
            mc.getSingleplayerServer().execute(()->{
                try {
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=player.serverLevel();
                    player.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
                    player.stopRiding();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);
                    player.getAbilities().flying=true;player.onUpdateAbilities();level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                    for(int x:new int[]{-17,-16,-15,14,15,16})for(int z=-2;z<=2;z++)level.getChunk(x,z);
                    for(var e:level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(-256,70,-20,256,100,30),e->!(e instanceof net.minecraft.world.entity.player.Player)))e.discard();
                    for(int x=-252;x<=-242;x++)for(int y=79;y<=88;y++)for(int z=3;z<=15;z++)
                        level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,z),(y==79?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
                    var s=EntityType.SLIME.create(level);s.setSize(4,true);s.setNoAi(true);s.setNoGravity(true);s.moveTo(-248.5,80,8.5,90,0);level.addFreshEntity(s);slime=s.getUUID();
                    var it=new ItemEntity(level,-247,84,12,new ItemStack(Items.DIAMOND));it.setNoGravity(true);level.addFreshEntity(it);item=it.getUUID();
                    var a=new ArmorStand(level,-246,80,4);a.setNoGravity(true);a.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.RED_CONCRETE));level.addFreshEntity(a);stand=a.getUUID();
                    var b=new Boat(level,-244,84,12);b.setNoGravity(true);level.addFreshEntity(b);boat=b.getUUID();
                    var p=EntityType.PIG.create(level);p.moveTo(-244,84,12);p.setNoAi(true);p.setNoGravity(true);level.addFreshEntity(p);p.startRiding(b,true);pig=p.getUUID();
                    player.connection.teleport(247.5,80,8.5,-90,0);
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==1 && ticks-started>100 && slime!=null) {
            var s=SeamEntityImages.get(slime);var i=SeamEntityImages.get(item);var a=SeamEntityImages.get(stand);var p=SeamEntityImages.get(pig);
            if(ticks%100==0)AroundPlanet.LOGGER.info("SEAM entity models: slime={} item={} stand={} pig={} boat={} mount={}",s!=null,i!=null,a!=null,p!=null,SeamEntityImages.get(boat)!=null,p==null?null:p.getVehicle());
            if(s==null || i==null || a==null || p==null || p.getVehicle()!=SeamEntityImages.get(boat) || !green(mc))return;
            if(!lightsReady(mc,slime,item,stand,boat,pig))return;
            require(net.minecraft.client.renderer.LightTexture.sky(sourceLight.get(slime))==15,"Daylight fixture is not exposed to the sky");
            s.setSharedFlagOnFire(true);
            require(net.minecraft.client.renderer.LightTexture.block(mc.getEntityRenderDispatcher().getPackedLightCoords(s,1))==15,"Image light suppressed burning emission");
            s.setSharedFlagOnFire(false);
            AroundPlanet.LOGGER.info("SEAM_ENTITY_LIGHT daylight PASS: source sky/block light, burning emission and isolated gameplay light");
            require(s.getX()>256 && s.getX()<270,"Wrong image translation");
            for(var id:new UUID[]{slime,item,stand,boat,pig}) {var e=SeamEntityImages.get(id);require(mc.level.getEntity(e.getId())!=e,"Visual entity entered gameplay cache");}
            require(((ItemEntity)i).getItem().is(Items.DIAMOND),"Item metadata missing");
            require(((ArmorStand)a).getItemBySlot(EquipmentSlot.HEAD).is(Items.RED_CONCRETE),"Equipment missing");
            AroundPlanet.LOGGER.info("SEAM_ENTITY_SMOKE east GPU PASS: slime, item, equipment, boat/passenger, isolated gameplay entities");
            phase=2;started=ticks;mc.getSingleplayerServer().execute(()->{
                var l=mc.getSingleplayerServer().overworld();l.getEntity(slime).setInvisible(true);
                ((ItemEntity)l.getEntity(item)).setItem(new ItemStack(Items.EMERALD));
                ((ArmorStand)l.getEntity(stand)).setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                for(int x=-249;x<=-243;x++)for(int z=1;z<=7;z++)l.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,84,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                l.setBlockAndUpdate(new net.minecraft.core.BlockPos(-246,82,4),net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState());
            });
        } else if(phase==2 && ticks-started>20) {
            var s=SeamEntityImages.get(slime);var i=(ItemEntity)SeamEntityImages.get(item);var a=(ArmorStand)SeamEntityImages.get(stand);
            if(s==null || !s.isInvisible() || i==null || !i.getItem().is(Items.EMERALD) || a==null || !a.getItemBySlot(EquipmentSlot.HEAD).isEmpty())return;
            if(!sourceLight.containsKey(stand) || net.minecraft.client.renderer.LightTexture.block(sourceLight.get(stand))==0
                    || net.minecraft.client.renderer.LightTexture.sky(sourceLight.get(stand))==15 || !lightsReady(mc,stand))return;
            AroundPlanet.LOGGER.info("SEAM_ENTITY_LIGHT roof/lamp PASS: updated source light reaches image renderer without changing real-world light engine");
            phase=3;started=ticks;mc.getSingleplayerServer().execute(()->{
                var s1=mc.getSingleplayerServer().overworld().getEntity(slime);s1.setInvisible(false);s1.setPos(-248.5,80,12.5);
            });
        } else if(phase==3 && ticks-started>20) {
            var s=SeamEntityImages.get(slime);if(s==null || s.isInvisible() || Math.abs(s.getZ()-12.5)>0.01)return;
            AroundPlanet.LOGGER.info("SEAM_ENTITY_SMOKE updates PASS: motion, item change, equipment removal, metadata reverted to default");
            phase=4;started=ticks;mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();
                var w=EntityType.SLIME.create(level);w.setSize(4,true);w.setNoAi(true);w.setNoGravity(true);w.moveTo(248.5,640,8.5,90,0);level.addFreshEntity(w);west=w.getUUID();
                mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(-247.5,640,8.5,90,0);
            });
        } else if(phase==4 && ticks-started>80) {
            var w=SeamEntityImages.get(west);if(w==null || w.getX()>-256 || !green(mc))return;
            if(!lightsReady(mc,west))return;
            require(SeamEntityImages.get(slime)==null,"Previous seam models survived transition");
            phase=5;started=ticks;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().getEntity(west).discard());
        } else if(phase==5 && ticks-started>20) {
            if(SeamEntityImages.get(west)!=null)return;
            phase=6;started=ticks;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(0,200,0,0,0));
        } else if(phase==6 && ticks-started>120) {
            require(SeamEntityImages.size()==0,"Images retained outside seam band");
            AroundPlanet.LOGGER.info("SEAM_ENTITY_SMOKE PASS: both GPU views including Y=640, metadata/equipment/motion, mounts, despawn, side change, cleanup");
            phase=7;mc.stop();
        }
    }
    private static boolean lightsReady(Minecraft mc,UUID... ids) {
        for(var id:ids) {
            var image=SeamEntityImages.get(id);var expected=sourceLight.get(id);if(image==null || expected==null)return false;
            var pos=net.minecraft.core.BlockPos.containing(image.getLightProbePosition(1));
            var light=io.github.astelev.around_planet.client.SeamImages.renderLight(mc.level);
            int snapshot=net.minecraft.client.renderer.LightTexture.pack(light.getLayerListener(net.minecraft.world.level.LightLayer.BLOCK).getLightValue(pos),light.getLayerListener(net.minecraft.world.level.LightLayer.SKY).getLightValue(pos));
            if(snapshot!=expected)return false;
            require(mc.getEntityRenderDispatcher().getPackedLightCoords(image,1)==expected,"Entity renderer used gameplay light instead of source snapshot: "+id);
            var control=EntityType.PIG.create(mc.level);control.setPos(image.position());
            var probe=net.minecraft.core.BlockPos.containing(control.getLightProbePosition(1));
            int ordinary=net.minecraft.client.renderer.LightTexture.pack(mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,probe),mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,probe));
            require(mc.getEntityRenderDispatcher().getPackedLightCoords(control,1)==ordinary,"Image lighting changed an ordinary entity");
        }
        return true;
    }
    private static boolean green(Minecraft mc) {
        mc.getMainRenderTarget().bindWrite(false);var bytes=BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(mc.getWindow().getWidth()/2+10,mc.getWindow().getHeight()/2-10,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
        int r=bytes.get(0)&255,g=bytes.get(1)&255,b=bytes.get(2)&255;
        if(ticks%100==0)AroundPlanet.LOGGER.info("SEAM entity pixel: phase={} rgb={},{},{}",phase,r,g,b);
        return g>40 && g>r*1.25 && g>b*1.1;
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
