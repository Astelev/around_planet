package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.network.PlasmaPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.*;

/** Shared translucent pressure envelope, drawn in the exhaust batch after clouds/fog. No particles. */
@EventBusSubscriber(modid = "around_planet", value = Dist.CLIENT)
public final class PlasmaRenderer {
    private static PlasmaPayload latest = new PlasmaPayload(List.of());
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static long received;
    public static long draws, triangles;
    public static void accept(PlasmaPayload payload) {
        level = Minecraft.getInstance().level; latest = payload; received = level == null ? 0 : level.getGameTime();
    }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() == level) { level = null; latest = new PlasmaPayload(List.of()); }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PropulsionExhaustRenderer.shader == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != level || mc.level.getGameTime() - received > 20 || latest.samples().isEmpty()) return;
        if (net.neoforged.fml.ModList.get().isLoaded("sable")) PlasmaMeshRenderer.render(event, latest);
    }
    private PlasmaRenderer() {}
}
