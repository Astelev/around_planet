package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class RendezvousOverlay {
    public static long frames;
    @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||!RendezvousClient.active())return;var telemetry=SpaceHud.current();if(telemetry==null||!telemetry.active())return;frames++;var g=e.getGuiGraphics();var own=RendezvousClient.own();var target=RendezvousClient.other();double distance=target.position().subtract(own.position()).length(),speed=own.velocity().subtract(target.velocity()).length();var a=Component.translatable("around_planet.rendezvous.distance",String.format(java.util.Locale.ROOT,"%.1f",distance));var b=Component.translatable("around_planet.rendezvous.speed",String.format(java.util.Locale.ROOT,"%.2f",speed));if(!OrbitalHelmetHud.handlesTelemetry())for(var row:java.util.List.of(a,b)){int y=row==a?g.guiHeight()-27:g.guiHeight()-16,x=g.guiWidth()-8-mc.font.width(row);g.fill(x-3,y-1,g.guiWidth()-5,y+10,0x9908121d);g.drawString(mc.font,row,x,y,0xff70f5f1,true);}
        var direction=RendezvousClient.direction(OrbitalAttitude.Mode.TARGET);if(direction.isEmpty())return;var d=telemetry.frame().toLocal(direction.get());var v=new org.joml.Vector3f((float)d.x(),(float)d.y(),(float)d.z());new org.joml.Quaternionf(mc.gameRenderer.getMainCamera().rotation()).conjugate().transform(v);boolean behind=v.z>=0;double focal=g.guiHeight()/(2*Math.tan(Math.toRadians(mc.options.fov().get())/2)),z=Math.max(.001,-v.z),dx=v.x/z*focal,dy=-v.y/z*focal;if(behind){dx=v.x*10000;dy=-v.y*10000;if(Math.hypot(dx,dy)<1){dx=0;dy=-10000;}}double sx=g.guiWidth()/2.0,sy=g.guiHeight()/2.0,limit=Math.max(Math.abs(dx)/Math.max(1,sx-24),Math.abs(dy)/Math.max(1,sy-40));if(limit>1){dx/=limit;dy/=limit;}int x=(int)(sx+dx),y=(int)(sy+dy);g.hLine(x-5,x+5,y,0xff70f5f1);g.vLine(x,y-5,y+5,0xff70f5f1);g.drawCenteredString(mc.font,Component.literal((behind?"↶ ":"◎ ")+RendezvousClient.current().name()),Math.clamp(x,65,g.guiWidth()-65),Math.clamp(y+8,8,g.guiHeight()-42),0xff70f5f1);
    }
}
