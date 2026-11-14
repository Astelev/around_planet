package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.network.PropulsionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
/** Ghost frequency slots copy real inventory items without consuming or moving them. */
public final class PropulsionScreen extends Screen {
    private final PropulsionBlockEntity be;private int left,top,w,selected=-1;private double value;
    public PropulsionScreen(PropulsionBlockEntity be){super(be.getBlockState().getBlock().getName());this.be=be;value=be.kind()==PropulsionRegistry.Kind.DAMPER?be.strength():be.thrust();}
    public static void open(PropulsionBlockEntity be){Minecraft.getInstance().setScreen(new PropulsionScreen(be));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override protected void init(){w=Math.min(360,width-16);left=(width-w)/2;top=Math.max(4,(height-(be.kind()==PropulsionRegistry.Kind.RSU?236:160))/2);
        boolean damping=be.kind()==PropulsionRegistry.Kind.DAMPER;double initial=damping?(value-.1)/19.9:Math.log10(value+1)/7;
        addRenderableWidget(new AbstractSliderButton(left+12,top+28,w-24,20,Component.empty(),initial){
            {updateMessage();}
            @Override protected void updateMessage(){double n=damping?.1+value*19.9:Math.pow(10,value*7)-1;setMessage(Component.translatable(damping?"around_planet.propulsion.damping":"around_planet.propulsion.thrust",String.format(java.util.Locale.ROOT,damping?"%.2f":"%.0f",n)));}
            @Override protected void applyValue(){PropulsionScreen.this.value=damping?.1+value*19.9:Math.pow(10,value*7)-1;PacketDistributor.sendToServer(new PropulsionPayload(be.getBlockPos(),0,0,(int)Math.round(PropulsionScreen.this.value*(damping?100:1))));}
        });
        if(be.kind()==PropulsionRegistry.Kind.RSU){for(int i=0;i<8;i++){final int index=i;addRenderableWidget(Button.builder(Component.literal(""),b->selected=index).bounds(left+142+(i%2)*24,top+49+(i/2)*18,22,18).build());}
            addRenderableWidget(Button.builder(Component.translatable("around_planet.propulsion.clear"),b->{if(selected>=0)PacketDistributor.sendToServer(new PropulsionPayload(be.getBlockPos(),1,selected,-1));}).bounds(left+208,top+50,w-220,20).build());
            for(int i=0;i<36;i++){final int slot=i;addRenderableWidget(Button.builder(Component.empty(),b->{if(selected>=0)PacketDistributor.sendToServer(new PropulsionPayload(be.getBlockPos(),1,selected,slot));}).bounds(left+12+(i%9)*18,top+138+(i/9)*18,18,18).build());}
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(left+12,top+(be.kind()==PropulsionRegistry.Kind.RSU?214:130),w-24,18).build());
    }
    @Override public void tick(){if(minecraft.level==null||be.isRemoved()||minecraft.level.getBlockEntity(be.getBlockPos())!=be)onClose();}
    @Override public void render(GuiGraphics g,int mx,int my,float dt){int h=be.kind()==PropulsionRegistry.Kind.RSU?236:155;g.fill(left,top,left+w,top+h,0xF0111D2D);g.drawCenteredString(font,title,left+w/2,top+9,0xFFFFFF);super.render(g,mx,my,dt);
        if(be.kind()==PropulsionRegistry.Kind.RSU){for(int i=0;i<4;i++){String[] sides={"gold","copper","red","green"};g.drawString(font,Component.translatable("around_planet.propulsion.side."+sides[i]),left+12,top+54+i*18,0xFFFFFF);g.drawString(font,""+be.signal(i)+"/15",left+98,top+54+i*18,0x72D5FF);for(int j=0;j<2;j++){int index=i*2+j,x=left+145+j*24,y=top+50+i*18;g.renderItem(be.frequency(index),x,y);if(index==selected)g.renderOutline(x-3,y-3,22,22,0xFFFFDC54);}}
            g.drawString(font,Component.translatable("around_planet.propulsion.frequency_help"),left+12,top+125,0xCCCCCC);for(int i=0;i<36;i++)g.renderItem(minecraft.player.getInventory().getItem(i),left+13+(i%9)*18,top+139+(i/9)*18);
            if(!net.neoforged.fml.ModList.get().isLoaded("create"))g.drawString(font,Component.translatable("around_planet.propulsion.requires_create"),left+12,top+130,0xFF8888);
        }else{g.drawString(font,Component.translatable("around_planet.propulsion.signal",be.signal(0)),left+12,top+63,0x72D5FF);if(be.kind()==PropulsionRegistry.Kind.DAMPER){g.drawString(font,Component.literal(be.energy.getEnergyStored()+" / "+PropulsionMath.CAPACITY+" FE"),left+12,top+83,0x72D5FF);g.drawString(font,Component.translatable("around_planet.propulsion.rotation_only"),left+12,top+103,0xCCCCCC);}else g.drawString(font,Component.translatable("around_planet.propulsion.vacuum"),left+12,top+85,0xCCCCCC);}
    }
}
