package io.github.astelev.around_planet.diagnostics;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

/** Five-second CPU deltas, including process CPU outside Java-managed threads. */
public final class CpuLoadDiagnostics {
    private static final java.lang.management.ThreadMXBean THREADS=ManagementFactory.getThreadMXBean();
    private static Map<Long,Long> previous=Map.of();
    private static long lastWall,lastProcess,next;
    private record Busy(long id,long nanos){}
    public static void poll(){
        long now=System.nanoTime();if(now<next)return;next=now+5_000_000_000L;
        if(!SeamDiagnostics.enabled() || !THREADS.isThreadCpuTimeSupported() || !THREADS.isThreadCpuTimeEnabled()){previous=Map.of();lastWall=0;return;}
        long process=ProcessHandle.current().info().totalCpuDuration().map(java.time.Duration::toNanos).orElse(0L);
        var current=new HashMap<Long,Long>();var busy=new ArrayList<Busy>();long javaCpu=0;
        for(long id:THREADS.getAllThreadIds()){
            long cpu=THREADS.getThreadCpuTime(id);if(cpu<0)continue;current.put(id,cpu);var old=previous.get(id);
            if(old!=null){long delta=Math.max(0,cpu-old);javaCpu+=delta;if(delta>0)busy.add(new Busy(id,delta));}
        }
        if(lastWall!=0){
            long elapsed=now-lastWall;double processCores=Math.max(0,process-lastProcess)/(double)elapsed,javaCores=javaCpu/(double)elapsed;
            busy.sort(Comparator.comparingLong(Busy::nanos).reversed());var text=new StringBuilder();
            for(var b:busy.stream().limit(8).toList()){
                var info=THREADS.getThreadInfo(b.id,8);if(info==null)continue;
                text.append('[').append(info.getThreadName()).append(" cores=").append(String.format(Locale.ROOT,"%.3f",b.nanos/(double)elapsed)).append(" state=").append(info.getThreadState()).append(" stack=").append(Arrays.stream(info.getStackTrace()).map(f->f.getClassName()+"#"+f.getMethodName()+":"+f.getLineNumber()).collect(Collectors.joining(" <- "))).append(']');
            }
            String detail="windowMs="+elapsed/1_000_000+" processors="+Runtime.getRuntime().availableProcessors()+String.format(Locale.ROOT," processCores=%.3f javaCores=%.3f ",processCores,javaCores)+text;
            SeamDiagnostics.event("client","cpu.load",()->detail);
        }
        previous=current;lastWall=now;lastProcess=process;
    }
    private CpuLoadDiagnostics(){}
}
