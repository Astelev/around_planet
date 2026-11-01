package io.github.astelev.aerospace.mixin.sable;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicket;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager", remap = false)
public interface PhysicsChunkTicketsAccessor {
    @Accessor("physicsChunks")
    Map<SectionPos, PhysicsChunkTicket> aerospace$physicsChunks();

    @Invoker("addTicket")
    PhysicsChunkTicket aerospace$addSection(Level level, PhysicsPipeline pipeline, SectionPos section,
                                          int x, int y, int z, int index, long gameTime);
}
