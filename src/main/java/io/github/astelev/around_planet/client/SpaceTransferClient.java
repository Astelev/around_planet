package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.network.SpaceTransferPayload;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
/** Region synchronization is valid even when the optional Sable classes are absent. */
public final class SpaceTransferClient {
    public static void accept(SpaceTransferPayload p) {
        var level=Minecraft.getInstance().level;if(level==null || !level.dimension().location().equals(p.dimension()))return;
        SpaceCells.view(level,p.cells());
        if(!p.moves().isEmpty())SpaceShipTransferClient.accept(p);
    }
    private SpaceTransferClient(){}
}
