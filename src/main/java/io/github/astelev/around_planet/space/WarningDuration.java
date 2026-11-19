package io.github.astelev.around_planet.space;

/** Seconds are a UI unit; stored/networked durations are integral 20 Hz ticks. */
public final class WarningDuration {
    public static final long MAX_TICKS=Integer.MAX_VALUE*20L;
    public static long ticks(double seconds){
        if(!Double.isFinite(seconds)||seconds<0||seconds>Integer.MAX_VALUE)throw new IllegalArgumentException("Warning duration");
        return Math.round(seconds*20);
    }
    public static double seconds(long ticks){if(ticks < -1 || ticks > MAX_TICKS)throw new IllegalArgumentException("Warning ticks");return ticks<0?-1:ticks/20.0;}
    private WarningDuration(){}
}
