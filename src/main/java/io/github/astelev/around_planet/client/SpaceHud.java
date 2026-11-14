package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.network.SpaceStatusPayload;
import net.minecraft.client.Minecraft;
public final class SpaceHud {
    private static long receivedTick;private static SpaceStatusPayload sample;private static net.minecraft.client.multiplayer.ClientLevel sampledLevel;
    public static void accept(SpaceStatusPayload p){sample=p;sampledLevel=Minecraft.getInstance().level;receivedTick=sampledLevel==null?0:sampledLevel.getGameTime();}
    public static SpaceStatusPayload current(){return Minecraft.getInstance().level==sampledLevel?sample:null;}
    public static double age(float partial){return sampledLevel==null?0:Math.max(0,Math.min(1,(sampledLevel.getGameTime()-receivedTick+partial)/20.0));}
    private SpaceHud(){}
}
