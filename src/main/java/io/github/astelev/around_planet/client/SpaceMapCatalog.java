package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.network.SpaceMapPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
public final class SpaceMapCatalog {
    private static SpaceMapPayload sample;private static net.minecraft.client.multiplayer.ClientLevel level;
    public static void accept(SpaceMapPayload p){sample=p;level=Minecraft.getInstance().level;}
    public static SpaceMapPayload current(){return level==Minecraft.getInstance().level?sample:null;}
    public static void subscribe(boolean open){var mc=Minecraft.getInstance();if(mc.getConnection()!=null && mc.getConnection().hasChannel(SpaceMapPayload.Request.TYPE))PacketDistributor.sendToServer(new SpaceMapPayload.Request(open));}
    private SpaceMapCatalog(){}
}
