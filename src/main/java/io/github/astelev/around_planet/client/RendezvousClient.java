package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.RendezvousPayload;
import net.minecraft.client.Minecraft;
public final class RendezvousClient {
    private static RendezvousPayload sample;private static net.minecraft.client.multiplayer.ClientLevel level;private static long received;
    public static void accept(RendezvousPayload p){sample=p;level=Minecraft.getInstance().level;received=level==null?0:level.getGameTime();}
    public static RendezvousPayload current(){var mc=Minecraft.getInstance();return mc.level==level&&mc.player!=null&&SpaceCells.contains(mc.player)?sample:null;}
    public static boolean active(){var p=current();return p!=null&&p.active();}
    public static long now(){var p=current();return p==null?0:p.now()+Math.max(0,level.getGameTime()-received);}
    public static SpaceState other(){var p=current();var status=SpaceHud.current();if(CelestialSystem.body(p.target())&&status!=null&&status.system()!=null)return status.system().target(p.target(),status.primary(),now());return new SpaceTrajectory("target",p.now(),p.other(),p.mu()).at(now());}
    public static SpaceState own(){var p=current();var t=SpaceHud.current();return new SpaceTrajectory("own",p.now(),p.own(),t==null?0:t.mu()).at(now());}
    public static java.util.Optional<SpaceVector> direction(OrbitalAttitude.Mode mode){return active()?RendezvousMath.direction(mode,own(),other()):java.util.Optional.empty();}
    private RendezvousClient(){}
}
