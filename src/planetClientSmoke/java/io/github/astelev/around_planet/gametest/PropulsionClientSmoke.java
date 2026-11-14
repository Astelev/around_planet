package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.client.PropulsionScreen;
import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class PropulsionClientSmoke {
    private static int ticks;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("around_planet.propulsionSmoke"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),2,202,5,java.util.Set.of(),180,34);var l=p.serverLevel();l.setBlockAndUpdate(new BlockPos(0,200,0),PropulsionRegistry.ENGINE.get().defaultBlockState());l.setBlockAndUpdate(new BlockPos(2,200,0),PropulsionRegistry.RSU.get().defaultBlockState());l.setBlockAndUpdate(new BlockPos(4,200,0),PropulsionRegistry.DAMPER.get().defaultBlockState());var be=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(4,200,0));be.energy.stored(0);be.energy.receiveEnergy(100000,false);var rsu=(PropulsionBlockEntity)l.getBlockEntity(new BlockPos(2,200,0));rsu.configure(1000);for(int i=0;i<8;i++)rsu.frequency(i,ItemStack.EMPTY);Item[] items={Items.DIAMOND,Items.IRON_INGOT,Items.EMERALD,Items.GOLD_INGOT,Items.REDSTONE,Items.COPPER_INGOT,Items.LAPIS_LAZULI,Items.AMETHYST_SHARD};for(int i=0;i<8;i++)p.getInventory().setItem(i,new ItemStack(items[i]));p.inventoryMenu.broadcastChanges();});
        if(ticks==80){open(mc,2);var buttons=mc.screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance).map(net.minecraft.client.gui.components.Button.class::cast).toList();for(int i=0;i<8;i++){buttons.get(i).onPress();buttons.get(9+i).onPress();}var slider=mc.screen.children().stream().filter(net.minecraft.client.gui.components.AbstractSliderButton.class::isInstance).map(net.minecraft.client.gui.components.AbstractSliderButton.class::cast).findFirst().orElseThrow();slider.mouseClicked(slider.getX()+slider.getWidth()*.7,slider.getY()+10,0);slider.mouseReleased(0,0,0);}
        if(ticks==120){var be=(PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(2,200,0));if(!be.frequency(0).is(Items.DIAMOND)||!be.frequency(7).is(Items.AMETHYST_SHARD)||be.thrust()==1000)throw new AssertionError("RSU ghost frequency or slider packet failed");snap(mc,"rsu-alpha18.png");AroundPlanet.LOGGER.info("PROPULSION_UI RSU PASS: eight server-authorized ghost slots and thrust slider");}
        if(ticks==140)open(mc,0);if(ticks==160)snap(mc,"creative-thruster-alpha18.png");if(ticks==180)open(mc,4);if(ticks==200){var be=(PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(4,200,0));if(be.energy.getEnergyStored()!=100000)throw new AssertionError("Client FE state missing");snap(mc,"inertia-damper-alpha18.png");AroundPlanet.LOGGER.info("PROPULSION_UI PASS: engine and FE damper screens");}if(ticks==220)mc.setScreen(null);if(ticks==230)snap(mc,"propulsion-models-alpha18.png");if(ticks==240)mc.stop();
    }
    private static void open(Minecraft mc,int x){PropulsionScreen.open((PropulsionBlockEntity)mc.level.getBlockEntity(new BlockPos(x,200,0)));}
    private static void snap(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
}
