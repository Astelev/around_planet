package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.network.VacuumPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class VacuumHud {
    private static VacuumPayload sample;private static net.minecraft.client.multiplayer.ClientLevel level;private static long received;
    public static void accept(VacuumPayload p){sample=p;level=Minecraft.getInstance().level;received=level==null?0:level.getGameTime();}
    public static VacuumPayload current(){var mc=Minecraft.getInstance();return mc.level==level&&level!=null&&level.getGameTime()-received<40?sample:null;}
    public static VacuumPayload forPlayer(net.minecraft.world.entity.player.Player player){return Minecraft.getInstance().player==player?current():null;}
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event){if(event.getLevel()==level){level=null;sample=null;}}
    @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();var p=current();if(p==null||!p.status().outside()||mc.player==null||mc.player.getAbilities().invulnerable||mc.options.hideGui||mc.screen!=null)return;
        var g=event.getGuiGraphics();int x=g.guiWidth()/2,y=g.guiHeight()-74;boolean safe=p.status().breathable();int color=safe?0x89E5C0:0xFF957E;
        var label=Component.translatable(p.status().room()?"around_planet.vacuum.room":safe?"around_planet.vacuum.supplied":"around_planet.vacuum.exposed");
        int w=mc.font.width(label);g.fill(x-w/2-4,y-3,x+w/2+4,y+22,0xB008121D);g.drawCenteredString(mc.font,label,x,y,color);
        g.fill(x-45,y+13,x+45,y+18,0xFF26384A);g.fill(x-45,y+13,x-45+(int)(90*Math.clamp(p.air()/(double)Math.max(1,p.maximum()),0,1)),y+18,0xFF000000|color);
    }
    private VacuumHud(){}
}
