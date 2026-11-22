package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.MoonBlockColors;
import io.github.astelev.around_planet.moon.*;
import io.github.astelev.around_planet.space.MoonSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.world.level.Level;
/** Replays terrain-particle tinting against real ClientLevels before and after dimension transfer. */
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class RegolithClientSmoke {
 private static int ticks,phase,start;private static volatile Throwable failure;private static volatile int ground;
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("around_planet.regolithSmoke"))return;var mc=Minecraft.getInstance();ticks++;if(failure!=null)throw new AssertionError("Regolith server fixture",failure);if(ticks>2000)throw new AssertionError("Regolith timeout phase="+phase);
  if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
  if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||ticks<80)return;
  if(phase==0){phase=1;start=ticks;server(mc,()->{var l=mc.getSingleplayerServer().overworld();var p=l.players().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();PlanetConfig.VACUUM.set(false);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.setBlockAndUpdate(new BlockPos(x,199,z),MoonRegistry.REGOLITH.get().defaultBlockState());p.teleportTo(l,.5,201,3,java.util.Set.of(),180,35);});return;}
  if(phase==1&&mc.level.dimension().equals(Level.OVERWORLD)&&ticks-start>60&&mc.level.getBlockState(new BlockPos(0,199,0)).is(MoonRegistry.REGOLITH.get())){check(mc,new BlockPos(0,199,0),false);shot(mc,"regolith73-overworld.png");phase=2;start=ticks;server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var l=mc.getSingleplayerServer().getLevel(MoonSurface.dimension());var g=(MoonChunkGenerator)l.getChunkSource().getGenerator();int x=0,z=0;var t=g.terrain();outer:for(int zz=-2000;zz<=2000;zz+=64)for(int xx=-2000;xx<=2000;xx+=64)if(t.sample(xx,zz).region()==MoonTerrain.Region.MARE){x=xx;z=zz;break outer;}l.getChunk(x>>4,z>>4);ground=t.sample(x,z).height();p.teleportTo(l,x+.5,ground+3,z+3,java.util.Set.of(),180,35);});return;}
  if(phase==2&&mc.level.dimension().equals(MoonSurface.dimension())&&ticks-start>80){var p=mc.player.blockPosition();var at=new BlockPos(p.getX(),ground,p.getZ()-3);if(!mc.level.hasChunkAt(at)||!mc.level.getBlockState(at).is(MoonRegistry.REGOLITH.get()))return;check(mc,at,true);shot(mc,"regolith73-moon.png");phase=3;start=ticks;}
  if(phase==3&&ticks-start>50){AroundPlanet.LOGGER.info("REGOLITH_CLIENT_PASS registered resolver; ClientLevel tint; terrain particle provider; destroy/crack particles; Overworld gray and lunar mare palette after dimension transfer");mc.stop();phase=4;}
 }
 private static void check(Minecraft mc,BlockPos p,boolean moon){if(!net.neoforged.neoforge.client.ColorResolverManager.getRegisteredResolvers().contains(MoonBlockColors.REGOLITH))throw new AssertionError("Regolith resolver not registered");var state=mc.level.getBlockState(p);for(int i=0;i<16;i++){int color=mc.getBlockColors().getColor(state,mc.level,p,0);if(color<0||!moon&&color!=0xaaa8a3)throw new AssertionError("Invalid regolith color "+Integer.toHexString(color));mc.particleEngine.createParticle(new BlockParticleOption(ParticleTypes.BLOCK,state),p.getX()+.5,p.getY()+1,p.getZ()+.5,.1,.1,.1);}mc.particleEngine.destroy(p,state);mc.particleEngine.crack(p,Direction.UP);AroundPlanet.LOGGER.info("REGOLITH_PARTICLES_PASS dimension={} color={}",mc.level.dimension().location(),Integer.toHexString(mc.getBlockColors().getColor(state,mc.level,p,0)));}
 private static void server(Minecraft mc,Runnable r){mc.getSingleplayerServer().execute(()->{try{r.run();}catch(Throwable t){failure=t;}});}
 private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
