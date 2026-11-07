package io.github.astelev.around_planet.seam;

import com.mojang.datafixers.util.Pair;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.mixin.EntityDataAccessor;
import io.github.astelev.around_planet.network.SeamEntitiesPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Queries existing entity sections only; no extra chunks, trackers, or physics instances. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class SeamEntityStreamer {
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level) || level.getGameTime()%SeamEntitiesPayload.INTERVAL!=0)return;
        var topology=PlanetBorders.get(level.getWorldBorder());if(topology==null)return;
        int radius=PlanetConfig.RADIUS.getAsInt();
        for(var player:level.players()) {
            if(!player.connection.hasChannel(SeamEntitiesPayload.TYPE)
                    || topology.distanceToSeam(player.getX())>Math.max(PlanetConfig.ACTIVATION.getAsInt(),(radius+3)*16))continue;
            // The client clears immediately outside this band, so distant players need no frame at all.
            List<byte[]> entries=new ArrayList<>();int bytes=5;
            int shift=player.getX()>=0?topology.width():-topology.width();
            if(topology.distanceToSeam(player.getX())<=Math.max(PlanetConfig.ACTIVATION.getAsInt(),(radius+3)*16)) {
                boolean east=shift>0;double minX=east?topology.west():topology.east()-(radius+2)*16;
                double maxX=east?topology.west()+(radius+2)*16:topology.east();
                int z=player.chunkPosition().z;
                double verticalRange=Math.max(128,Math.min(player.requestedViewDistance(),level.getServer().getPlayerList().getViewDistance())*16.0);
                var bounds=new AABB(minX,player.getY()-verticalRange,(z-radius-1)*16,
                        maxX,player.getY()+verticalRange,(z+radius+2)*16);
                List<Entity> entities=new ArrayList<>();
                level.getEntities(EntityTypeTest.forClass(Entity.class),bounds,
                        e->e!=player && !e.isRemoved() && !e.isSpectator() && !topology.outside(e.getX())
                                && level.getChunkSource().getChunkNow(e.chunkPosition().x,e.chunkPosition().z)!=null,
                        entities,SeamEntitiesPayload.MAX_ENTITIES);
                entities.sort(Comparator.comparingDouble(e->e.position().add(shift,0,0).distanceToSqr(player.position())));
                for(var entity:entities) {
                    var entry=encode(level,entity);
                    if(entry!=null && entry.length<=SeamEntitiesPayload.MAX_ENTRY_BYTES && bytes+entry.length+5<=SeamEntitiesPayload.MAX_BYTES) {
                        entries.add(entry);bytes+=entry.length+5;
                    }
                }
            }
            var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
            try {
                buf.writeVarInt(entries.size());entries.forEach(buf::writeByteArray);
                byte[] data=new byte[buf.readableBytes()];buf.readBytes(data);
                PacketDistributor.sendToPlayer(player,new SeamEntitiesPayload(level.dimension().location(),shift,data));
            } finally {buf.release();}
        }
    }
    private static byte[] encode(ServerLevel level,Entity entity) {
        var tracker=new ServerEntity(level,entity,SeamEntitiesPayload.INTERVAL,false,p->{});
        var packet=entity.getAddEntityPacket(tracker);
        ClientboundAddEntityPacket spawn;
        if(packet instanceof ClientboundAddEntityPacket add)spawn=add;
        else if(entity instanceof ExperienceOrb orb)spawn=new ClientboundAddEntityPacket(entity.getId(),entity.getUUID(),entity.getX(),entity.getY(),entity.getZ(),
                entity.getXRot(),entity.getYRot(),entity.getType(),orb.getValue(),entity.getDeltaMovement(),entity.getYHeadRot());
        else return null; // Custom spawning protocols require their own renderer adapter.
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try {
            ClientboundAddEntityPacket.STREAM_CODEC.encode(buf,spawn);
            List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values=new ArrayList<>();
            for(var item:((EntityDataAccessor)entity.getEntityData()).around_planet$items())values.add(item.value());
            ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buf,new ClientboundSetEntityDataPacket(entity.getId(),values));
            buf.writeUtf(entity instanceof Player p?p.getGameProfile().getName():"",16);
            buf.writeInt(entity.tickCount);buf.writeFloat(entity instanceof LivingEntity living?living.yBodyRot:entity.getYRot());
            buf.writeBoolean(entity.getVehicle()!=null);
            if(entity.getVehicle()!=null)buf.writeUUID(entity.getVehicle().getUUID());
            buf.writeBoolean(entity instanceof LivingEntity);
            if(entity instanceof LivingEntity living) {
                List<Pair<EquipmentSlot,ItemStack>> equipment=new ArrayList<>();
                for(var slot:EquipmentSlot.values())equipment.add(Pair.of(slot,living.getItemBySlot(slot).copy()));
                ClientboundSetEquipmentPacket.STREAM_CODEC.encode(buf,new ClientboundSetEquipmentPacket(entity.getId(),equipment));
                ClientboundUpdateAttributesPacket.STREAM_CODEC.encode(buf,new ClientboundUpdateAttributesPacket(entity.getId(),living.getAttributes().getSyncableAttributes()));
                buf.writeVarInt(living.hurtTime);buf.writeVarInt(living.deathTime);
            }
            if(buf.readableBytes()>SeamEntitiesPayload.MAX_ENTRY_BYTES)return null;
            byte[] data=new byte[buf.readableBytes()];buf.readBytes(data);return data;
        } finally {buf.release();}
    }
    private SeamEntityStreamer() {}
}
