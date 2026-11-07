package io.github.astelev.around_planet.client;

import com.mojang.authlib.GameProfile;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.network.SeamEntitiesPayload;
import io.github.astelev.around_planet.seam.SeamGeometry;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.*;

/** Detached render models. Never add these to ClientLevel or run their gameplay tick. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamEntityImages {
    private static final Map<UUID,Image> IMAGES=new LinkedHashMap<>();
    private static ClientLevel world;
    private static int shift;
    private static long ticks,lastFrame;
    private static final class Image {
        final Entity entity;
        Vec3 target;
        float yaw,pitch,head,body;
        int steps;
        UUID vehicle;
        Image(Entity entity){this.entity=entity;}
    }
    public static Entity get(UUID uuid){var image=IMAGES.get(uuid);return image==null?null:image.entity;}
    public static int size(){return IMAGES.size();}
    public static void accept(SeamEntitiesPayload payload) {
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null || mc.player==null || !level.dimension().location().equals(payload.dimension()))return;
        var topology=PlanetBorders.get(level.getWorldBorder());
        if(topology==null || payload.shift()!=(mc.player.getX()>=0?topology.width():-topology.width()))return;
        if(world!=level || shift!=payload.shift()){clear();world=level;shift=payload.shift();}
        var buf=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()),level.registryAccess());
        Set<UUID> seen=new HashSet<>();
        try {
            int count=buf.readVarInt();
            if(count<0 || count>SeamEntitiesPayload.MAX_ENTITIES)throw new IllegalArgumentException("Entity image count");
            for(int i=0;i<count;i++) {
                var entry=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray(SeamEntitiesPayload.MAX_ENTRY_BYTES)),level.registryAccess());
                try {
                    var spawn=ClientboundAddEntityPacket.STREAM_CODEC.decode(entry);
                    var data=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(entry);
                    String name=entry.readUtf(16);int age=entry.readInt();float body=entry.readFloat();
                    UUID vehicle=entry.readBoolean()?entry.readUUID():null;
                    boolean living=entry.readBoolean();
                    var equipment=living?ClientboundSetEquipmentPacket.STREAM_CODEC.decode(entry):null;
                    var attributes=living?ClientboundUpdateAttributesPacket.STREAM_CODEC.decode(entry):null;
                    int hurt=living?entry.readVarInt():0,death=living?entry.readVarInt():0;
                    double x=spawn.getX()+shift,y=spawn.getY(),z=spawn.getZ();
                    int cx=Mth.floor(x/16),cz=Mth.floor(z/16);
                    int range=Math.max(mc.options.getEffectiveRenderDistance()+2,PlanetConfig.RADIUS.getAsInt()+3);
                    if(!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                            || !SeamGeometry.canonical(topology,Mth.floor(spawn.getX()/16)) || !SeamGeometry.clipped(level,cx,cz)
                            || Math.abs((long)cx-mc.player.chunkPosition().x)>range
                            || Math.abs((long)cz-mc.player.chunkPosition().z)>PlanetConfig.RADIUS.getAsInt()+2)continue;
                    var image=IMAGES.get(spawn.getUUID());
                    boolean fresh=image==null || image.entity.getType()!=spawn.getType() || image.entity.getId()!=spawn.getId();
                    if(fresh) {
                        Entity entity;
                        if(spawn.getType()==EntityType.PLAYER) {
                            var info=mc.getConnection().getPlayerInfo(spawn.getUUID());
                            entity=new RemotePlayer(level,info!=null?info.getProfile():new GameProfile(spawn.getUUID(),name.isEmpty()?"Player":name));
                        } else if(spawn.getType()==EntityType.EXPERIENCE_ORB)entity=new ExperienceOrb(level,x,y,z,spawn.getData());
                        else entity=spawn.getType().create(level);
                        if(entity==null)continue;
                        entity.recreateFromPacket(spawn);
                        entity.absMoveTo(x,y,z,spawn.getYRot(),spawn.getXRot());entity.setOldPosAndRot();entity.tickCount=age;
                        image=new Image(entity);IMAGES.put(spawn.getUUID(),image);
                    }
                    Entity entity=image.entity;
                    entity.getEntityData().assignValues(data.packedItems());
                    if(entity instanceof LivingEntity model && living) {
                        for(var pair:equipment.getSlots())model.setItemSlot(pair.getFirst(),pair.getSecond());
                        for(var value:attributes.getValues()) {
                            var attribute=model.getAttributes().getInstance(value.attribute());
                            if(attribute!=null){attribute.setBaseValue(value.base());attribute.removeModifiers();value.modifiers().forEach(attribute::addTransientModifier);}
                        }
                        model.hurtTime=hurt;model.deathTime=death;
                    }
                    image.target=new Vec3(x,y,z);image.yaw=spawn.getYRot();image.pitch=spawn.getXRot();
                    image.head=spawn.getYHeadRot();image.body=body;image.vehicle=vehicle;
                    image.steps=fresh?0:SeamEntitiesPayload.INTERVAL;
                    if(fresh && entity instanceof LivingEntity model){model.yBodyRot=model.yBodyRotO=body;model.yHeadRot=model.yHeadRotO=image.head;}
                    seen.add(spawn.getUUID());
                } finally {entry.release();}
            }
            IMAGES.entrySet().removeIf(e->!seen.contains(e.getKey()));lastFrame=ticks;
            for(var image:IMAGES.values()) {
                var vehicle=image.vehicle==null?null:get(image.vehicle);
                if(image.entity.getVehicle()!=vehicle) {
                    image.entity.stopRiding();
                    if(vehicle!=null && vehicle!=image.entity)image.entity.startRiding(vehicle,true);
                }
            }
        } finally {buf.release();}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();if(mc.isPaused())return;ticks++;
        var topology=mc.level==null?null:PlanetBorders.get(mc.level.getWorldBorder());
        if(mc.level!=world || mc.player==null || topology==null || ticks-lastFrame>80
                || shift!=(mc.player.getX()>=0?topology.width():-topology.width())
                || topology.distanceToSeam(mc.player.getX())>Math.max(PlanetConfig.ACTIVATION.getAsInt(),(PlanetConfig.RADIUS.getAsInt()+3)*16)) {clear();return;}
        for(var image:IMAGES.values()) {
            var entity=image.entity;entity.setOldPosAndRot();entity.tickCount++;
            double previousX=entity.getX(),previousZ=entity.getZ();
            if(image.steps>0) {
                double a=1.0/image.steps--;
                var p=entity.position().lerp(image.target,a);
                entity.absMoveTo(p.x,p.y,p.z,Mth.rotLerp((float)a,entity.getYRot(),image.yaw),Mth.lerp((float)a,entity.getXRot(),image.pitch));
            }
            if(entity instanceof LivingEntity model) {
                model.yBodyRotO=model.yBodyRot;model.yHeadRotO=model.yHeadRot;
                model.yBodyRot=Mth.rotLerp(0.5f,model.yBodyRot,image.body);model.yHeadRot=Mth.rotLerp(0.5f,model.yHeadRot,image.head);
                model.walkAnimation.update(Math.min(1,(float)Math.hypot(entity.getX()-previousX,entity.getZ()-previousZ)*4),0.4f);
                if(model.hurtTime>0)model.hurtTime--;
            }
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES || IMAGES.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level!=world || mc.player==null)return;
        var camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var dispatcher=mc.getEntityRenderDispatcher();var buffers=mc.renderBuffers().bufferSource();
        for(var image:IMAGES.values()) {
            var entity=image.entity;
            if(SeamImages.get(world,entity.chunkPosition().x,entity.chunkPosition().z)==null
                    || !dispatcher.shouldRender(entity,event.getFrustum(),camera.x,camera.y,camera.z))continue;
            dispatcher.render(entity,Mth.lerp(partial,entity.xOld,entity.getX())-camera.x,
                    Mth.lerp(partial,entity.yOld,entity.getY())-camera.y,Mth.lerp(partial,entity.zOld,entity.getZ())-camera.z,
                    Mth.rotLerp(partial,entity.yRotO,entity.getYRot()),partial,event.getPoseStack(),buffers,dispatcher.getPackedLightCoords(entity,partial));
        }
        buffers.endBatch();
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel()==world)clear();}
    private static void clear(){IMAGES.values().forEach(i->i.entity.stopRiding());IMAGES.clear();world=null;shift=0;}
    private SeamEntityImages() {}
}
