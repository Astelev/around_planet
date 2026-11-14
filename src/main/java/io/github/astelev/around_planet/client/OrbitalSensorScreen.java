package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity;
import io.github.astelev.around_planet.space.OrbitalAttitude;
import io.github.astelev.around_planet.network.OrbitalSensorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
public final class OrbitalSensorScreen extends Screen {
    private final OrbitalSensorBlockEntity sensor;private OrbitalAttitude.Mode selected;private int scale,left,top,w;
    public OrbitalSensorScreen(OrbitalSensorBlockEntity sensor){super(Component.translatable("block.around_planet.orbital_sensor"));this.sensor=sensor;selected=sensor.mode();scale=sensor.scale();}
    public static void open(OrbitalSensorBlockEntity sensor){Minecraft.getInstance().setScreen(new OrbitalSensorScreen(sensor));}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){w=Math.min(420,width-20);left=(width-w)/2;top=Math.max(8,(height-210)/2);int cell=(w-16)/3;
        for(var mode:OrbitalAttitude.Mode.values())addRenderableWidget(Button.builder(name(mode),b->{selected=mode;send();}).bounds(left+8+(mode.ordinal()%3)*cell,top+28+(mode.ordinal()/3)*22,cell-2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("around_planet.sensor.scale",scale),b->{int[] values={15,30,45,90,180};int next=values[0];for(int v:values)if(v>scale){next=v;break;}scale=next;b.setMessage(Component.translatable("around_planet.sensor.scale",scale));send();}).bounds(left+8,top+168,w/2-12,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(left+w/2+4,top+168,w/2-12,20).build());
    }
    private void send(){PacketDistributor.sendToServer(new OrbitalSensorPayload(sensor.getBlockPos(),selected.ordinal(),scale));}
    public static Component name(OrbitalAttitude.Mode mode){return Component.translatable("around_planet.sensor.mode."+mode.name().toLowerCase(java.util.Locale.ROOT));}
    @Override public void tick(){if(minecraft.level==null || sensor.isRemoved() || minecraft.level.getBlockEntity(sensor.getBlockPos())!=sensor)onClose();}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(left,top,left+w,top+200,0xEE111D2D);g.drawCenteredString(font,title,left+w/2,top+9,0xFFFFFF);super.render(g,x,y,partial);
        g.drawCenteredString(font,name(sensor.mode()),left+w/2,top+100,0x72D5FF);g.drawCenteredString(font,Component.translatable(sensor.active()?"around_planet.sensor.error":"around_planet.sensor.inactive",String.format(java.util.Locale.ROOT,"%.1f",sensor.yaw()),String.format(java.util.Locale.ROOT,"%.1f",sensor.pitch())),left+w/2,top+116,sensor.active()?0xAAFFAA:0xFFCC66);
        g.drawCenteredString(font,Component.translatable("around_planet.sensor.ports.pitch"),left+w/2,top+136,0xCCCCCC);g.drawCenteredString(font,Component.translatable("around_planet.sensor.ports.yaw"),left+w/2,top+149,0xCCCCCC);
    }
}
