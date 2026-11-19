package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.lifesupport.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.CommonHooks;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class VacuumGameTests {
 private static final class TestPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
  TestPlayer(net.minecraft.server.level.ServerLevel l){super(l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VacuumTest"));try{var field=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(this,0);}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}}
  @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
 }
 private static TestPlayer player(net.minecraft.server.level.ServerLevel l){var p=new TestPlayer(l);p.setGameMode(GameType.SURVIVAL);p.setAirSupply(300);p.setHealth(20);return p;}
 @GameTest(template="empty",templateNamespace="around_planet",batch="vacuum",timeoutTicks=100)
 public static void outsideSpaceConsumesAirAndSuffocatesWithoutWater(GameTestHelper h){var l=h.getLevel();var p=player(l);p.setPos(0,PlanetConfig.SPACE_ENTRY.get()+100,0);boolean old=PlanetConfig.VACUUM.get();PlanetConfig.VACUUM.set(true);
  try{h.assertTrue(VacuumEnvironment.outside(p),"Upper atmosphere not vacuum");for(int i=0;i<320;i++)CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==0&&p.getHealth()==18,"Vacuum did not consume reserve and cause suffocation: air="+p.getAirSupply()+" hp="+p.getHealth());p.setPos(0,80,0);CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==4,"Air did not recover on ground");h.succeed();}finally{PlanetConfig.VACUUM.set(old);}
 }
 @GameTest(template="empty",templateNamespace="around_planet",batch="vacuum",timeoutTicks=100)
 public static void moonCreativeAndDisabledVacuumBehaveSeparately(GameTestHelper h){var l=h.getLevel().getServer().getLevel(MoonSurface.dimension());h.assertTrue(l!=null,"Moon dimension missing");var p=player(l);p.setPos(0,90,0);boolean old=PlanetConfig.VACUUM.get();PlanetConfig.VACUUM.set(true);
  try{h.assertTrue(VacuumEnvironment.outside(p),"Moon surface breathable");CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==299,"Moon did not consume air");p.setGameMode(GameType.CREATIVE);for(int i=0;i<50;i++)CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==300&&p.getHealth()==20,"Creative player harmed");p.setGameMode(GameType.SURVIVAL);PlanetConfig.VACUUM.set(false);p.setAirSupply(100);CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==104,"Disabled vacuum still active");h.succeed();}finally{PlanetConfig.VACUUM.set(old);}
 }
 @GameTest(template="empty",templateNamespace="around_planet",batch="vacuum",timeoutTicks=100)
 public static void nativeCreateHelmetAndBacktankSupplyAirInVacuum(GameTestHelper h){if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}var l=h.getLevel();var p=player(l);p.setPos(0,PlanetConfig.SPACE_ENTRY.get()+100,0);
  var helmet=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("create:copper_diving_helmet")));var tank=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("create:copper_backtank")));tank.set(com.simibubi.create.AllDataComponents.BACKTANK_AIR,100);p.setItemSlot(EquipmentSlot.HEAD,helmet);p.setItemSlot(EquipmentSlot.CHEST,tank);
  try{
   p.setAirSupply(100);CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==300,"Create helmet did not supply air");p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==299,"Tank without helmet protected player");p.setItemSlot(EquipmentSlot.HEAD,helmet);tank.set(com.simibubi.create.AllDataComponents.BACKTANK_AIR,0);CommonHooks.onLivingBreathe(p,1,4);h.assertTrue(p.getAirSupply()==298,"Empty backtank supplied free air");h.succeed();}finally{p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);}
 }
}
