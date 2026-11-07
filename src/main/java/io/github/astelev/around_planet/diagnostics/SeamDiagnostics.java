package io.github.astelev.around_planet.diagnostics;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/** Read-only, bounded telemetry. Keys are fixed code labels, never coordinates or world objects. */
public final class SeamDiagnostics {
    private static final long WINDOW=5_000_000_000L;
    private static final String SESSION=Long.toHexString(System.nanoTime());
    private static volatile boolean enabled=true;
    private static final Bucket CLIENT=new Bucket(),SERVER=new Bucket();
    private static final List<java.lang.ref.WeakReference<SeamQueueProbe>> QUEUES=new ArrayList<>();
    private static final class Bucket {
        final Map<String,LongAdder> counts=new ConcurrentHashMap<>();
        final Map<String,Timing> timings=new ConcurrentHashMap<>();
        long next,eventsUntil,lastReport=System.nanoTime();int events,importantEvents;long seq;
    }
    private static final class Timing {final LongAdder count=new LongAdder(),nanos=new LongAdder();final AtomicLong max=new AtomicLong();}
    public static void registerQueue(SeamQueueProbe queue) {
        synchronized(QUEUES){QUEUES.removeIf(ref->ref.get()==null);if(QUEUES.size()>=32)QUEUES.remove(0);QUEUES.add(new java.lang.ref.WeakReference<>(queue));}
    }
    public static String queues() {
        var text=new StringBuilder();
        List<java.lang.ref.WeakReference<SeamQueueProbe>> copy;
        synchronized(QUEUES){QUEUES.removeIf(ref->ref.get()==null);copy=List.copyOf(QUEUES);}
        for(var ref:copy) {
            var queue=ref.get();if(queue==null)continue;
            try {text.append('[').append(queue.around_planet$queueDiagnostic()).append(']');}
            catch(RuntimeException | LinkageError e){text.append("[queue_probe_error:").append(e.getClass().getSimpleName()).append(']');}
        }
        return text.toString();
    }
    public static String caller() {
        return StackWalker.getInstance().walk(frames->frames.filter(f->!f.getClassName().startsWith("io.github.astelev.around_planet") && !f.getMethodName().contains("around_planet"))
                .limit(6).map(f->f.getClassName()+"#"+f.getMethodName()+":"+f.getLineNumber()).collect(java.util.stream.Collectors.joining(" <- ")));
    }
    public static boolean enabled(){return enabled;}
    private static Bucket bucket(String side){return side.equals("client")?CLIENT:SERVER;}
    public static void count(String side,String key){add(side,key,1);}
    public static void add(String side,String key,long n){if(enabled)bucket(side).counts.computeIfAbsent(key,k->new LongAdder()).add(n);}
    public static long start(){return enabled?System.nanoTime():0;}
    public static void time(String side,String key,long start) {
        if(!enabled || start==0)return;long elapsed=System.nanoTime()-start;
        var t=bucket(side).timings.computeIfAbsent(key,k->new Timing());t.count.increment();t.nanos.add(elapsed);t.max.accumulateAndGet(elapsed,Math::max);
    }
    public static boolean observed(String side,String key){return bucket(side).counts.containsKey(key) || bucket(side).timings.containsKey(key);}
    public static void event(String side,String key,Supplier<String> detail) {
        if(!enabled)return;count(side,key);var b=bucket(side);long now=System.nanoTime();
        synchronized(b){if(now>=b.eventsUntil){b.eventsUntil=now+WINDOW;b.events=0;b.importantEvents=0;}boolean important=key.equals("crossing") || key.equals("session.start") || key.equals("level.unload") || key.startsWith("transfer.") || key.equals("diagnostic.mark") || key.equals("server.slow_tick") || key.contains("renderer_reset") || key.startsWith("dh.reset") || key.equals("mesh.clear");if(important?b.importantEvents++>=4:b.events++>=12){count(side,"events.suppressed");return;}}
        AroundPlanet.LOGGER.info("[AP-DIAG] session={} side={} event={} {}",SESSION,side,key,detail.get());
    }
    public static boolean due(String side) {
        if(PlanetConfig.SPEC.isLoaded())enabled=PlanetConfig.DIAGNOSTICS.getAsBoolean();
        if(!enabled)return false;var b=bucket(side);long now=System.nanoTime();
        synchronized(b){if(now<b.next)return false;b.next=now+WINDOW;return true;}
    }
    public static void report(String side,String state) {
        if(!enabled)return;var b=bucket(side);long now=System.nanoTime(),windowMs=(now-b.lastReport)/1_000_000;b.lastReport=now;var counts=new TreeMap<String,Long>();
        b.counts.forEach((k,v)->{long n=v.sumThenReset();if(n!=0)counts.put(k,n);});
        var timings=new TreeMap<String,String>();
        b.timings.forEach((k,v)->{long n=v.count.sumThenReset(),ns=v.nanos.sumThenReset(),max=v.max.getAndSet(0);if(n!=0)timings.put(k,String.format(Locale.ROOT,"n=%d avgMs=%.3f maxMs=%.3f",n,ns/1e6/n,max/1e6));});
        AroundPlanet.LOGGER.info("[AP-DIAG] session={} side={} summary={} epochMs={} windowMs={} {} counts={} timings={}",SESSION,side,++b.seq,System.currentTimeMillis(),windowMs,state,counts,timings);
    }
    public static String config() {
        return "config{enabled="+PlanetConfig.ENABLED.getAsBoolean()+",boundary="+PlanetConfig.BOUNDARY.getAsInt()+",radius="+PlanetConfig.RADIUS.getAsInt()+",activation="+PlanetConfig.ACTIVATION.getAsInt()
                +",snapTicks="+PlanetConfig.SNAPSHOT_INTERVAL.getAsInt()+",snapBudget="+PlanetConfig.SNAPSHOT_BUDGET.getAsInt()+",retain="+PlanetConfig.RETAIN_MESHES.getAsBoolean()+",meshMiB="+PlanetConfig.MESH_MIB.getAsInt()+",meshSections="+PlanetConfig.MESH_SECTIONS.getAsInt()
                +",dhSeam="+PlanetConfig.DH_SEAM.getAsBoolean()+",dhPreview="+PlanetConfig.DH_PREVIEW.getAsInt()+",terrainBlend="+PlanetConfig.TERRAIN_BLEND.getAsBoolean()+"}";
    }
    public static String id(Object object){return object==null?"none":Integer.toHexString(System.identityHashCode(object));}
    private SeamDiagnostics(){}
}
