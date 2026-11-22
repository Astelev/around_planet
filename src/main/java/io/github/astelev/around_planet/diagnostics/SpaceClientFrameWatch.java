package io.github.astelev.around_planet.diagnostics;
import io.github.astelev.around_planet.client.SpaceRenderView;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.stream.Collectors;
/** Bounded samples of a stalled space frame, including its actual render-thread stack. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT)
public final class SpaceClientFrameWatch {
    private static volatile String phase="frame";private static volatile Thread renderThread;private static volatile long start;private static long lastSample;private static ScheduledExecutorService watcher;
    @SubscribeEvent public static void pre(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e){
        if(!SeamDiagnostics.enabled() || !SpaceRenderView.active()){start=0;return;}begin("frame");
    }
    @SubscribeEvent public static void tickPre(net.neoforged.neoforge.client.event.ClientTickEvent.Pre e){if(SeamDiagnostics.enabled() && net.minecraft.client.Minecraft.getInstance().level!=null)begin("tick");}
    @SubscribeEvent public static void tickPost(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(phase.equals("tick"))start=0;}
    private static void begin(String kind){phase=kind;renderThread=Thread.currentThread();start=System.nanoTime();
        if(watcher==null){watcher=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"around-planet-space-frame-watch");t.setDaemon(true);return t;});watcher.scheduleAtFixedRate(SpaceClientFrameWatch::sample,250,250,TimeUnit.MILLISECONDS);}
    }
    @SubscribeEvent public static void post(net.neoforged.neoforge.client.event.RenderFrameEvent.Post e){long before=start;start=0;SeamDiagnostics.time("client","space.frame",before);}
    private static void sample(){long before=start,now=System.nanoTime();var thread=renderThread;if(before==0 || thread==null || !SeamDiagnostics.enabled() || now-before<500_000_000L || now-lastSample<5_000_000_000L)return;lastSample=now;var stack=thread.getStackTrace();if(before!=start)return;SeamDiagnostics.event("client",phase.equals("tick")?"client.slow_tick":"space.slow_frame",()->"elapsedMs="+(now-before)/1_000_000+" state="+thread.getState()+" stack="+Arrays.stream(stack).limit(18).map(f->f.getClassName()+"#"+f.getMethodName()+":"+f.getLineNumber()).collect(Collectors.joining(" <- ")));}
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload e){if(!e.getLevel().isClientSide())return;start=0;renderThread=null;if(watcher!=null)watcher.shutdownNow();watcher=null;lastSample=0;}
    private SpaceClientFrameWatch(){}
}
