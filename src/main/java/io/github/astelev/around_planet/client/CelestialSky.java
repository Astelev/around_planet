package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.CelestialSystem;
import io.github.astelev.around_planet.network.CelestialSkyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
/** Never carry an old world's clock into a new client level. */
public final class CelestialSky {
 private static java.lang.ref.WeakReference<ClientLevel> level=new java.lang.ref.WeakReference<>(null);private static CelestialSystem system;
 public static void accept(CelestialSkyPayload packet){level=new java.lang.ref.WeakReference<>(Minecraft.getInstance().level);system=packet.system();}
 public static CelestialSystem current(){var current=Minecraft.getInstance().level;return current!=null&&current==level.get()?system:null;}
 private CelestialSky(){}
}
