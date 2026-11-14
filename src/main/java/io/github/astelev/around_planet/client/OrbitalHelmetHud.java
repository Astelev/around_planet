package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.compat.AeroHelmetBridge;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class OrbitalHelmetHud {
    private static long uploadTick=Long.MIN_VALUE;public static long geometryBatches,textureUploads;
    private static DynamicTexture texture;private static ResourceLocation id;private static SpaceVector cachedUp;private static net.minecraft.client.multiplayer.ClientLevel level;public static long frames,plannerFrames,statsFrames;
    @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null || mc.options.hideGui || mc.screen!=null || !net.neoforged.fml.ModList.get().isLoaded("aeroengineering"))return;
        var helmet=mc.player.getItemBySlot(EquipmentSlot.HEAD);if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(helmet.getItem()).equals(ResourceLocation.fromNamespaceAndPath("aeroengineering","engine_monitor_helmet")))return;
        OrbitalSensorBlockEntity sensor=null;io.github.astelev.around_planet.maneuver.ManeuverPlannerBlockEntity planner=null;
        for(var pos:AeroHelmetBridge.targets(helmet)){if(!mc.level.hasChunkAt(pos)||!AeroHelmetBridge.enabled(helmet,pos))continue;var be=mc.level.getBlockEntity(pos);if(be==null||!java.util.Objects.equals(AeroHelmetBridge.structure(helmet),io.github.astelev.around_planet.compat.SableSensorVisual.structure(be)))continue;if(be instanceof OrbitalSensorBlockEntity found&&sensor==null)sensor=found;else if(be instanceof io.github.astelev.around_planet.maneuver.ManeuverPlannerBlockEntity found&&planner==null)planner=found;}
        var telemetry=SpaceHud.current();boolean stats=sensor!=null&&telemetry!=null&&telemetry.active();int rows=(stats?2:0)+(planner!=null?2:0);
        if(sensor!=null){long start=io.github.astelev.around_planet.diagnostics.SeamDiagnostics.start();var reading=OrbitalSensorVisual.read(sensor,event.getPartialTick().getGameTimeDeltaPartialTick(false));draw(event.getGuiGraphics(),sensor,reading,AeroHelmetBridge.scale(helmet),AeroHelmetBridge.offset(helmet,true),AeroHelmetBridge.offset(helmet,false),rows);frames++;io.github.astelev.around_planet.diagnostics.SeamDiagnostics.time("client","space.hud",start);}
        if(rows>0){var g=event.getGuiGraphics();int x=Math.clamp(g.guiWidth()-8+AeroHelmetBridge.offset(helmet,true),130,g.guiWidth()-4),y=g.guiHeight()-16-(rows-1)*11;
            if(planner!=null){plannerFrames++;long now=mc.level.getGameTime();boolean next=planner.nextTick()>=now;var title=next?net.minecraft.network.chat.Component.translatable("around_planet.maneuver.countdown",String.format(java.util.Locale.ROOT,"%.1f",(planner.nextTick()-now)/20.0)):net.minecraft.network.chat.Component.translatable("around_planet.maneuver.none");hudLine(g,title,x,y,ManeuverGuidance.warning(now,planner.nextTick(),planner.warningSeconds())?0xffffb24f:0xffa7dbe7);y+=11;hudLine(g,net.minecraft.network.chat.Component.literal(next?String.format(java.util.Locale.ROOT,"Δv %.2f m/s",planner.deltaV()):"Δv —"),x,y,0xffffd180);y+=11;}
            if(stats){statsFrames++;var state=new SpaceState(new SpaceVector(telemetry.x(),telemetry.y(),telemetry.z()),new SpaceVector(telemetry.vx(),telemetry.vy(),telemetry.vz()));if(!telemetry.returning())state=new SpaceTrajectory("hud",0,state,telemetry.mu()).afterSeconds(SpaceHud.age(event.getPartialTick().getGameTimeDeltaPartialTick(false)));hudLine(g,net.minecraft.network.chat.Component.translatable("around_planet.helmet.orbit_speed",String.format(java.util.Locale.ROOT,"%.2f",state.velocity().length())),x,y,0xff9ee9df);hudLine(g,net.minecraft.network.chat.Component.translatable("around_planet.helmet.altitude",String.format(java.util.Locale.ROOT,"%.0f",state.position().length()-telemetry.radius())),x,y+11,0xffc5e2f0);}
        }
    }
    private static void hudLine(GuiGraphics g,net.minecraft.network.chat.Component text,int right,int y,int color){var font=Minecraft.getInstance().font;int left=right-font.width(text);g.fill(left-3,y-1,right+3,y+10,0x9908121d);g.drawString(font,text,left,y,color,true);}

    private static void update(SpaceVector up){var mc=Minecraft.getInstance();if(level!=mc.level){if(id!=null)mc.getTextureManager().release(id);texture=null;id=null;cachedUp=null;uploadTick=Long.MIN_VALUE;level=mc.level;}
        if(texture==null){texture=new DynamicTexture(new NativeImage(96,96,true));id=mc.getTextureManager().register("around_planet_orbital_navball",texture);}
        if(cachedUp!=null && uploadTick==mc.level.getGameTime())return;
        var quantized=new SpaceVector(Math.rint(up.x()*256)/256,Math.rint(up.y()*256)/256,Math.rint(up.z()*256)/256);if(quantized.equals(cachedUp))return;
        for(int y=0;y<96;y++)for(int x=0;x<96;x++){int c=NavballMath.color((x+.5-48)/47.5,-(y+.5-48)/47.5,quantized);texture.getPixels().setPixelRGBA(x,y,(c&0xFF00FF00)|((c>>>16)&255)|((c&255)<<16));}texture.upload();cachedUp=quantized;uploadTick=mc.level.getGameTime();textureUploads++;
    }
    private static void draw(GuiGraphics g,OrbitalSensorBlockEntity sensor,OrbitalSensorVisual.Reading reading,float scale,int ox,int oy,int rows){var mc=Minecraft.getInstance();int r=Math.clamp(Math.round(38*scale),26,Math.max(26,Math.min(g.guiWidth(),g.guiHeight())/4));int x=Math.clamp(g.guiWidth()-r-10+ox,r+8,g.guiWidth()-r-8),y=Math.clamp(g.guiHeight()-r-21-rows*11+oy,r+8,g.guiHeight()-r-19-rows*11);
        update(sensor.active()?reading.up():new SpaceVector(0,1,0));g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();g.blit(id,x-r,y-r,2*r,2*r,0,0,96,96,96,96);com.mojang.blaze3d.systems.RenderSystem.disableBlend();var canvas=new Canvas(g.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui()),g.pose().last().pose());circle(canvas,x,y,r,0xFF86B9C4);
        canvas.fill(x-10,y,x-2,y+1,0xFFFFDD60);canvas.fill(x+3,y,x+11,y+1,0xFFFFDD60);canvas.fill(x,y-2,x+1,y+3,0xFFFFDD60);
        boolean rear=false;
        if(sensor.active()){var t=reading.target();boolean behind=t.z()<0;double px=t.x(),py=t.y();if(behind){double length=Math.hypot(px,py);if(length<1e-6){px=0;py=-1;}else{px/=length;py/=length;}}int mx=x+(int)Math.round(px*(r-5)),my=y-(int)Math.round(py*(r-5));int color=behind?0xFFFFA94F:0xFF8DFF66;glyph(canvas,mx,my,sensor.mode(),color);rear=behind;}
        g.flush();geometryBatches++;if(rear)g.drawCenteredString(mc.font,net.minecraft.network.chat.Component.translatable("around_planet.sensor.behind"),x,y-r-10,0xFFFFA94F);
        var label=sensor.active()?OrbitalSensorScreen.name(sensor.mode()):net.minecraft.network.chat.Component.translatable("around_planet.sensor.hud_inactive");int half=mc.font.width(label)/2;g.drawCenteredString(mc.font,label,Math.clamp(x,half+4,g.guiWidth()-half-4),y+r+5,sensor.active()?0xFF99FF88:0xFFBBBBBB);
    }
    private static void glyph(Canvas g,int x,int y,OrbitalAttitude.Mode mode,int color){switch(mode){case NORMAL,ANTINORMAL->{line(g,x,y-5,x-5,y+4,color);line(g,x-5,y+4,x+5,y+4,color);line(g,x+5,y+4,x,y-5,color);}case RADIAL_OUT,RADIAL_IN,PLANET_OUT,PLANET_IN->{line(g,x,y-5,x+5,y,color);line(g,x+5,y,x,y+5,color);line(g,x,y+5,x-5,y,color);line(g,x-5,y,x,y-5,color);}default->circle(g,x,y,5,color);}
        if(mode==OrbitalAttitude.Mode.RETROGRADE || mode==OrbitalAttitude.Mode.ANTINORMAL || mode==OrbitalAttitude.Mode.RADIAL_IN || mode==OrbitalAttitude.Mode.PLANET_IN){line(g,x-3,y-3,x+3,y+3,color);line(g,x-3,y+3,x+3,y-3,color);}else{g.fill(x-8,y,x-4,y+1,color);g.fill(x+5,y,x+9,y+1,color);g.fill(x,y-8,x+1,y-4,color);}}
    private static void circle(Canvas g,int x,int y,int r,int color){for(int i=0;i<64;i++){double a=i*2*Math.PI/64,b=(i+1)*2*Math.PI/64;line(g,x+(int)Math.round(r*Math.cos(a)),y+(int)Math.round(r*Math.sin(a)),x+(int)Math.round(r*Math.cos(b)),y+(int)Math.round(r*Math.sin(b)),color);}}
    private static void line(Canvas g,int a,int b,int c,int d,int color){int n=Math.max(Math.abs(c-a),Math.abs(d-b));for(int i=0;i<=n;i++){int x=n==0?a:a+(c-a)*i/n,y=n==0?b:b+(d-b)*i/n;g.fill(x,y,x+1,y+1,color);}}
    private record Canvas(com.mojang.blaze3d.vertex.VertexConsumer vertices,org.joml.Matrix4f matrix){
        void fill(int a,int b,int c,int d,int color){vertices.addVertex(matrix,a,b,0).setColor(color);vertices.addVertex(matrix,a,d,0).setColor(color);vertices.addVertex(matrix,c,d,0).setColor(color);vertices.addVertex(matrix,c,b,0).setColor(color);}
    }
    private OrbitalHelmetHud(){}
}
