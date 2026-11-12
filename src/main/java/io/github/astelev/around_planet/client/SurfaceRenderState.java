package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
/** One world render scope; HUD and hand rendering never inherit the curvature uniforms. */
public final class SurfaceRenderState {
    public static final Matrix4f view=new Matrix4f(),inverseView=new Matrix4f(),viewProjection=new Matrix4f(),inverseViewProjection=new Matrix4f();
    private static boolean frame,sky,frameEnabled;private static double frameRadius;public static long curvedDraws,dhDraws,skyFrames;
    public static void enter(Matrix4f view,Matrix4f projection){frameEnabled=baseEnabled();frameRadius=frameEnabled && PlanetConfig.SURFACE_CURVE.getAsBoolean()?new PlanetScale(PlanetConfig.PLANET_SIZE.get()).radius():0;frame=true;sky=false;SurfaceRenderState.view.set(view);inverseView.set(view).invert();viewProjection.set(projection).mul(view);inverseViewProjection.set(viewProjection).invert();}
    public static void exit(){frame=false;sky=false;}
    public static void sky(boolean value){sky=value;}
    public static boolean enabled(){return frame?frameEnabled:baseEnabled();}
    private static boolean baseEnabled(){var mc=Minecraft.getInstance();return mc.level!=null && mc.player!=null && mc.level.dimension().equals(Level.OVERWORLD) && PlanetConfig.SPEC.isLoaded() && PlanetConfig.ENABLED.getAsBoolean() && !SpaceRenderView.active() && !shaderPack();}
    private static boolean shaderPack(){try{if(!net.neoforged.fml.ModList.get().isLoaded("iris"))return false;var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");return (Boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));}catch(ReflectiveOperationException e){return true;}}
    public static double radius(){return frame && !sky?frameRadius:0;}
    public static double darkness(double y){return SurfaceVisualMath.darkness(y,PlanetConfig.SKY_DARKEN.getAsInt(),Math.max(PlanetConfig.SKY_DARKEN.getAsInt()+1,PlanetConfig.SPACE_ENTRY.getAsInt()));}
    private SurfaceRenderState(){}
}
