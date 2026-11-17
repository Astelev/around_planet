package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.*;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.compat.TelescopeAccess;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class FlightToolsClientSmoke {
    private static int ticks,phase,start;private static volatile Throwable failure;private static final BlockPos BLOCK=new BlockPos(0,300,0),ENGINE=new BlockPos(2,300,0);private static volatile UUID object;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.screen!=null&&mc.screen.getClass().getName().contains("pondus")&&mc.screen.getClass().getSimpleName().contains("Import")){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Skip")){b.onPress();return;}}
        if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){for(var w:mc.screen.children())if(w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains("I know")){b.onPress();return;}}
        if(++ticks>5000)throw new AssertionError("Flight tools timeout phase="+phase);
        if(failure!=null)throw new AssertionError("Flight tools",failure);
        if(mc.player==null||mc.getSingleplayerServer()==null||ticks<120)return;
        if(phase==0){phase=1;start=ticks;mc.options.renderDistance().set(3);mc.getSingleplayerServer().execute(()->{try{
            var level=mc.getSingleplayerServer().overworld();PlanetConfig.ENABLED.set(true);level.setDayTime(6000);level.setWeatherParameters(60000,0,false,false);
            level.setBlockAndUpdate(BLOCK,io.github.astelev.around_planet.maneuver.ManeuverRegistry.TELESCOPE.get().defaultBlockState());level.setBlockAndUpdate(ENGINE,io.github.astelev.around_planet.propulsion.PropulsionRegistry.LIQUID.get().defaultBlockState());
            ((io.github.astelev.around_planet.propulsion.PropulsionBlockEntity)level.getBlockEntity(ENGINE)).configure(20);
            var player=level.players().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(.5,301,2.5);
            object=UUID.randomUUID();SpaceOrbitData.get(level.getServer()).put(object,new SpaceTrajectory(CelestialSystem.EARTH,level.getGameTime(),new SpaceState(new SpaceVector(35000,0,0),new SpaceVector(0,340,0)),4e9));
        }catch(Throwable t){failure=t;}});return;}
        if(phase==1&&ticks-start>70&&mc.level.getBlockEntity(ENGINE) instanceof io.github.astelev.around_planet.propulsion.PropulsionBlockEntity be){
            if(be.actualThrust()!=0||be.configuredThrust()<20000)throw new AssertionError("Idle engine lost configured thrust");
            PropulsionScreen.open(be);phase=2;start=ticks;return;
        }
        if(phase==2&&ticks-start>30){shot(mc,"idle-engine-thrust-alpha58.png");mc.setScreen(null);phase=3;start=ticks;mc.getSingleplayerServer().execute(()->{try{TelescopeAccess.open(mc.getSingleplayerServer().overworld().players().getFirst(),BLOCK);}catch(Throwable t){failure=t;}});return;}
        if(phase==3&&ticks-start>70&&mc.screen instanceof OrbitMapScreen){var catalog=SpaceMapCatalog.current();if(catalog==null||catalog.bodies().stream().noneMatch(b->b.id().equals(object)))return;
            if(SpaceHud.current()==null||SpaceHud.current().active())throw new AssertionError("Telescope requires an orbital cell");
            if(!PlanetAtlasTexture.ready())return;
            shot(mc,"telescope-earth-map-alpha58.png");mc.screen.mouseClicked(mc.screen.width-246,18,0);phase=4;start=ticks;return;
        }
        if(phase==4&&ticks-start>50){if(!(mc.screen instanceof OrbitMapScreen))throw new AssertionError("Ground map closed unexpectedly");shot(mc,"telescope-moon-map-alpha58.png");AroundPlanet.LOGGER.info("FLIGHT_TOOLS_CLIENT_PASS atlas={}x{} groundRadius={} catalogObjects={} idleThrust={} telescopeMap=true",io.github.astelev.around_planet.atlas.PlanetAtlas.WIDTH,io.github.astelev.around_planet.atlas.PlanetAtlas.HEIGHT,SpaceHud.current().radius(),SpaceMapCatalog.current().bodies().size(),((io.github.astelev.around_planet.propulsion.PropulsionBlockEntity)mc.level.getBlockEntity(ENGINE)).configuredThrust());phase=5;mc.stop();}
    }
    private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
