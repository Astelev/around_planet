package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.network.ManeuverPayload;
import net.minecraft.client.Minecraft;
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class ManeuverClient {
    private static ManeuverPayload sample;private static net.minecraft.client.multiplayer.ClientLevel level;private static long received;
    public static void accept(ManeuverPayload p){var mc=Minecraft.getInstance();sample=p;level=mc.level;received=level==null?0:level.getGameTime();if(p.open()&&!p.session().equals(ManeuverPayload.NONE))mc.setScreen(new OrbitMapScreen(p.session()));}
    public static ManeuverPayload current(){return Minecraft.getInstance().level==level?sample:null;}
    public static long now(){return sample==null?0:sample.now()+Math.max(0,level.getGameTime()-received);}
    @net.neoforged.bus.api.SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){sample=null;level=null;received=0;}
    private ManeuverClient(){}
}
