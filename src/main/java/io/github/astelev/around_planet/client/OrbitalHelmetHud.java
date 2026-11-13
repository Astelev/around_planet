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
    private static DynamicTexture texture;private static ResourceLocation id;private static SpaceVector cachedUp;private static net.minecraft.client.multiplayer.ClientLevel level;public static long frames;
    @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null || mc.options.hideGui || mc.screen!=null || !net.neoforged.fml.ModList.get().isLoaded("aeroengineering"))return;
        var helmet=mc.player.getItemBySlot(EquipmentSlot.HEAD);if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(helmet.getItem()).equals(ResourceLocation.fromNamespaceAndPath("aeroengineering","engine_monitor_helmet")))return;
        for(var pos:AeroHelmetBridge.targets(helmet)){if(!mc.level.hasChunkAt(pos) || !(mc.level.getBlockEntity(pos) instanceof OrbitalSensorBlockEntity sensor) || !AeroHelmetBridge.enabled(helmet,pos))continue;
            if(!java.util.Objects.equals(AeroHelmetBridge.structure(helmet),io.github.astelev.around_planet.compat.SableSensorVisual.structure(sensor)))continue;
            var reading=OrbitalSensorVisual.read(sensor,event.getPartialTick().getGameTimeDeltaPartialTick(false));draw(event.getGuiGraphics(),sensor,reading,AeroHelmetBridge.scale(helmet),AeroHelmetBridge.offset(helmet,true),AeroHelmetBridge.offset(helmet,false));frames++;break;
        }
    }
    private static void update(SpaceVector up){var mc=Minecraft.getInstance();if(level!=mc.level){if(id!=null)mc.getTextureManager().release(id);texture=null;id=null;cachedUp=null;level=mc.level;}
        if(texture==null){texture=new DynamicTexture(new NativeImage(96,96,true));id=mc.getTextureManager().register("around_planet_orbital_navball",texture);}
        var quantized=new SpaceVector(Math.rint(up.x()*256)/256,Math.rint(up.y()*256)/256,Math.rint(up.z()*256)/256);if(quantized.equals(cachedUp))return;
        for(int y=0;y<96;y++)for(int x=0;x<96;x++){int c=NavballMath.color((x+.5-48)/47.5,-(y+.5-48)/47.5,quantized);texture.getPixels().setPixelRGBA(x,y,(c&0xFF00FF00)|((c>>>16)&255)|((c&255)<<16));}texture.upload();cachedUp=quantized;
    }
    private static void draw(GuiGraphics g,OrbitalSensorBlockEntity sensor,OrbitalSensorVisual.Reading reading,float scale,int ox,int oy){var mc=Minecraft.getInstance();int r=Math.clamp(Math.round(38*scale),26,Math.max(26,Math.min(g.guiWidth(),g.guiHeight())/4));int x=Math.clamp(g.guiWidth()-r-10+ox,r+8,g.guiWidth()-r-8),y=Math.clamp(g.guiHeight()-r-21+oy,r+8,g.guiHeight()-r-19);
        update(sensor.active()?reading.up():new SpaceVector(0,1,0));g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();g.blit(id,x-r,y-r,2*r,2*r,0,0,96,96,96,96);com.mojang.blaze3d.systems.RenderSystem.disableBlend();circle(g,x,y,r,0xFF86B9C4);
        g.hLine(x-10,x-3,y,0xFFFFDD60);g.hLine(x+3,x+10,y,0xFFFFDD60);g.vLine(x,y-2,y+2,0xFFFFDD60);
        if(sensor.active()){var t=reading.target();boolean behind=t.z()<0;double px=t.x(),py=t.y();if(behind){double length=Math.hypot(px,py);if(length<1e-6){px=0;py=-1;}else{px/=length;py/=length;}}int mx=x+(int)Math.round(px*(r-5)),my=y-(int)Math.round(py*(r-5));int color=behind?0xFFFFA94F:0xFF8DFF66;glyph(g,mx,my,sensor.mode(),color);if(behind)g.drawCenteredString(mc.font,net.minecraft.network.chat.Component.translatable("around_planet.sensor.behind"),x,y-r-10,color);}
        var label=sensor.active()?OrbitalSensorScreen.name(sensor.mode()):net.minecraft.network.chat.Component.translatable("around_planet.sensor.hud_inactive");int half=mc.font.width(label)/2;g.drawCenteredString(mc.font,label,Math.clamp(x,half+4,g.guiWidth()-half-4),y+r+5,sensor.active()?0xFF99FF88:0xFFBBBBBB);
    }
    private static void glyph(GuiGraphics g,int x,int y,OrbitalAttitude.Mode mode,int color){switch(mode){case NORMAL,ANTINORMAL->{line(g,x,y-5,x-5,y+4,color);line(g,x-5,y+4,x+5,y+4,color);line(g,x+5,y+4,x,y-5,color);}case RADIAL_OUT,RADIAL_IN,PLANET_OUT,PLANET_IN->{line(g,x,y-5,x+5,y,color);line(g,x+5,y,x,y+5,color);line(g,x,y+5,x-5,y,color);line(g,x-5,y,x,y-5,color);}default->circle(g,x,y,5,color);}
        if(mode==OrbitalAttitude.Mode.RETROGRADE || mode==OrbitalAttitude.Mode.ANTINORMAL || mode==OrbitalAttitude.Mode.RADIAL_IN || mode==OrbitalAttitude.Mode.PLANET_IN){line(g,x-3,y-3,x+3,y+3,color);line(g,x-3,y+3,x+3,y-3,color);}else{g.hLine(x-8,x-5,y,color);g.hLine(x+5,x+8,y,color);g.vLine(x,y-8,y-5,color);}}
    private static void circle(GuiGraphics g,int x,int y,int r,int color){for(int i=0;i<64;i++){double a=i*2*Math.PI/64,b=(i+1)*2*Math.PI/64;line(g,x+(int)Math.round(r*Math.cos(a)),y+(int)Math.round(r*Math.sin(a)),x+(int)Math.round(r*Math.cos(b)),y+(int)Math.round(r*Math.sin(b)),color);}}
    private static void line(GuiGraphics g,int a,int b,int c,int d,int color){int n=Math.max(Math.abs(c-a),Math.abs(d-b));for(int i=0;i<=n;i++){int x=n==0?a:a+(c-a)*i/n,y=n==0?b:b+(d-b)*i/n;g.fill(x,y,x+1,y+1,color);}}
    private OrbitalHelmetHud(){}
}
