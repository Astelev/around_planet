package io.github.astelev.around_planet.diagnostics;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/** Sample only an already slow active tick; never wait for or suspend the server thread. */
public final class SeamServerStallWatch {
    private static volatile Thread serverThread;
    private static volatile long tickStart;
    private static ScheduledExecutorService watcher;
    private static long lastSample;
    public static synchronized void begin(long start) {
        tickStart=start;serverThread=Thread.currentThread();
        if(watcher!=null || !SeamDiagnostics.enabled())return;
        watcher=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"around-planet-stall-watch");t.setDaemon(true);return t;});
        watcher.scheduleAtFixedRate(SeamServerStallWatch::sample,250,250,TimeUnit.MILLISECONDS);
    }
    public static void end(){tickStart=0;}
    private static void sample() {
        long start=tickStart,now=System.nanoTime();var thread=serverThread;
        if(!SeamDiagnostics.enabled() || start==0 || thread==null || now-start<500_000_000L || now-lastSample<5_000_000_000L)return;
        lastSample=now;var stack=thread.getStackTrace();if(start!=tickStart)return;
        SeamDiagnostics.event("server","server.slow_tick",()->"elapsedMs="+(now-start)/1_000_000+" state="+thread.getState()+" stack="
                +Arrays.stream(stack).limit(18).map(f->f.getClassName()+"#"+f.getMethodName()+":"+f.getLineNumber()).collect(Collectors.joining(" <- ")));
    }
    public static synchronized void close(){tickStart=0;serverThread=null;if(watcher!=null)watcher.shutdownNow();watcher=null;lastSample=0;}
    private SeamServerStallWatch(){}
}
