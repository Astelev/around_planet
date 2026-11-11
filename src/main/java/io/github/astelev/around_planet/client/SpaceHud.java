package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.network.SpaceStatusPayload;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import java.util.*;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SpaceHud {
    private static SpaceStatusPayload sample;private static net.minecraft.client.multiplayer.ClientLevel sampledLevel;
    public static void accept(SpaceStatusPayload p){sample=p;sampledLevel=Minecraft.getInstance().level;}
    public static SpaceStatusPayload current(){return Minecraft.getInstance().level==sampledLevel?sample:null;}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();var p=sample;
        if(mc.options.hideGui || mc.player==null || mc.level!=sampledLevel || p==null || !p.active() || !SpaceCells.contains(mc.player))return;
        var lines=new ArrayList<Component>();lines.add(Component.translatable("around_planet.space.coordinates"));
        lines.add(Component.literal(String.format(Locale.ROOT,"X %.1f  Y %.1f  Z %.1f",p.x(),p.y(),p.z())));
        double speed=Math.hypot(Math.hypot(p.vx(),p.vy()),p.vz());
        lines.add(Component.translatable("around_planet.space.speed",String.format(Locale.ROOT,"%.2f",speed),String.format(Locale.ROOT,"%.1f",speed*3.6)));
        lines.add(Component.literal(String.format(Locale.ROOT,"Vx %.2f  Vy %.2f  Vz %.2f",p.vx(),p.vy(),p.vz())));
        lines.add(Component.translatable("around_planet.space.altitude",String.format(Locale.ROOT,"%.1f",p.altitude())));
        if(p.returning())lines.add(Component.translatable("around_planet.space.returning"));
        int width=lines.stream().mapToInt(mc.font::width).max().orElse(0);var g=event.getGuiGraphics();g.fill(5,5,13+width,11+lines.size()*11,0x99000000);
        for(int i=0;i<lines.size();i++)g.drawString(mc.font,lines.get(i),9,9+i*11,0xFFFFFF,true);
    }
    private SpaceHud(){}
}
