package io.github.astelev.around_planet.space;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
@EventBusSubscriber(modid="around_planet")
public final class SpaceCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("time").requires(s->s.hasPermission(2)).then(Commands.literal("warp").then(Commands.argument("seconds",com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(.05)).executes(c->{double seconds=com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,"seconds");if(!Double.isFinite(seconds)||seconds*20>Long.MAX_VALUE){c.getSource().sendFailure(Component.translatable("around_planet.warp.invalid"));return 0;}try{var result=SpaceTimeWarp.advance(c.getSource().getServer(),Math.max(1,Math.round(seconds*20)));var message=Component.translatable("around_planet.warp.result",String.format(java.util.Locale.ROOT,"%.2f",result.ticks()/20.0),result.objects());if(!result.limit().isEmpty())message.append(" · ").append(Component.translatable(result.limit()));c.getSource().sendSuccess(()->message,true);return result.ticks()>0?1:0;}catch(ArithmeticException|IllegalArgumentException failure){c.getSource().sendFailure(Component.translatable("around_planet.warp.invalid"));return 0;}}))));
        e.getDispatcher().register(Commands.literal("around_planet_space").then(Commands.literal("return").executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(net.neoforged.fml.ModList.get().isLoaded("sable") && io.github.astelev.around_planet.compat.SpaceShipTransfers.requestReturn(p)) {
                c.getSource().sendSuccess(()->Component.literal("Return requested; waiting for destination chunks."),false);return 1;
            }c.getSource().sendFailure(Component.literal("Stand on or ride a ship in a space cell."));return 0;
        })));
    }
}
