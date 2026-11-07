package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.seam.SeamTicketBounds;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Filter only ordinary view-distance tickets, before vanilla queues any work. Generation dependencies remain untouched. */
@Mixin(targets="net.minecraft.server.level.DistanceManager$PlayerTicketTracker")
public abstract class SeamPlayerTicketsMixin {
    @Shadow @Final private DistanceManager this$0;
    @ModifyVariable(method="onLevelChange(JIZZ)V",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private boolean around_planet$canonicalTickets(boolean value,long pos,int level,boolean hadTicket,boolean hasTicket) {
        boolean allowed=value && SeamTicketBounds.allow(this$0,ChunkPos.getX(pos),ChunkPos.getZ(pos));
        if(value && !allowed)io.github.astelev.around_planet.diagnostics.SeamDiagnostics.count("server","ticket.filtered_outside");
        return allowed;
    }
}
