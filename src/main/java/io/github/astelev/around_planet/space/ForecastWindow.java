package io.github.astelev.around_planet.space;
/** Forecast duration in revolutions of the current conic; open paths use its local circular timescale. */
public final class ForecastWindow {
    public static final int MAX_TURNS=32;
    public static double seconds(SpaceTrajectory path,long from,int turns){
        if(turns<1||turns>MAX_TURNS)throw new IllegalArgumentException("Forecast revolutions");
        var state=path.at(from);var ellipse=EllipticArc.of(state,path.mu());double period=ellipse!=null?ellipse.period():path.mu()>0?2*Math.PI*Math.sqrt(Math.pow(state.position().length(),3)/path.mu()):3600;
        if(!Double.isFinite(period)||period<=0)period=3600;return period*turns;
    }
    private ForecastWindow(){}
}
