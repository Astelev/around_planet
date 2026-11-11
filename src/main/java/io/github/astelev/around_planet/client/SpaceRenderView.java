package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
/** Render-only policy: actual chunks, lighting and native ship meshes remain loaded. */
public final class SpaceRenderView {
    public static long terrainSkips,dhSkips,cloudSkips,simpleCloudSkips,skySkips,shipPasses;
    public static boolean active(){var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null)return false;var p=mc.gameRenderer.getMainCamera().getPosition();return SpaceCells.contains(mc.level,p.x,p.y,p.z) || SpaceCells.contains(mc.player);}
    private SpaceRenderView(){}
}
