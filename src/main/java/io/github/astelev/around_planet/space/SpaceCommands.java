package io.github.astelev.around_planet.space;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
@EventBusSubscriber(modid="around_planet")
public final class SpaceCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("around_planet_space").then(Commands.literal("return").executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(net.neoforged.fml.ModList.get().isLoaded("sable") && io.github.astelev.around_planet.compat.SpaceShipTransfers.requestReturn(p)) {
                c.getSource().sendSuccess(()->Component.literal("Return requested; waiting for destination chunks."),false);return 1;
            }c.getSource().sendFailure(Component.literal("Stand on or ride a ship in a space cell."));return 0;
        })));
    }
}
