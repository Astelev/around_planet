package io.github.astelev.around_planet.lifesupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import io.github.astelev.around_planet.network.VacuumPayload;
import java.util.*;
/** Supplies environment input to NeoForge breathing; Create's normal helmet/backtank handler follows. */
@EventBusSubscriber(modid="around_planet")
public final class VacuumBreathing {
    private static final Map<ServerPlayer,VacuumPayload.Status> STATE=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void environment(LivingBreatheEvent event){
        if(!(event.getEntity() instanceof Player player))return;
        boolean outside,room;
        if(player instanceof ServerPlayer server){
            outside=VacuumEnvironment.outside(player);room=outside&&net.neoforged.fml.ModList.get().isLoaded("create_submarine")&&io.github.astelev.around_planet.compat.DeepSeasAir.breathable(player);
            STATE.put(server,new VacuumPayload.Status(outside,room,event.canBreathe()));
        }else{
            var sample=io.github.astelev.around_planet.client.VacuumHud.forPlayer(player);if(sample==null)return;outside=sample.status().outside();room=sample.status().room();
        }
        if(!outside||player.getAbilities().invulnerable||player.isSpectator()||!player.getEyeInFluidType().isAir())return;
        event.setCanBreathe(room);event.setConsumeAirAmount(1);event.setRefillAirAmount(room?4:0);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void result(LivingBreatheEvent event){
        if(event.getEntity() instanceof ServerPlayer player){var status=STATE.get(player);if(status!=null)STATE.put(player,new VacuumPayload.Status(status.outside(),status.room(),event.canBreathe()));}
    }
    @SubscribeEvent public static void suffocation(LivingDrownEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;var status=STATE.get(player);
        if(status==null||!status.outside()||status.breathable()||!player.getEyeInFluidType().isAir())return;
        event.setBubbleCount(0);
        if(event.isDrowning()&&!event.isCanceled()){
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("around_planet","vacuum"));
            player.hurt(new net.minecraft.world.damagesource.DamageSource(player.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key)),event.getDamageAmount());event.setDamageAmount(0);
        }
    }
    public static VacuumPayload.Status status(ServerPlayer player){return STATE.getOrDefault(player,new VacuumPayload.Status(false,false,true));}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)STATE.remove(player);}
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event){if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level){STATE.keySet().removeIf(p->p.level()==level);if(net.neoforged.fml.ModList.get().isLoaded("create_submarine"))io.github.astelev.around_planet.compat.DeepSeasAir.clear(level);}}
    private VacuumBreathing(){}
}
