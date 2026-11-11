package io.github.astelev.around_planet.space;

/** Universal-variable two-body propagation, including elliptic, parabolic, hyperbolic and radial arcs.
 * Solves a scalar Kepler equation at query time, never integrates elapsed ticks. Independent implementation
 * of the standard f/g equations; no Cosmonautics code or shaded library classes are used.
 */
final class KeplerMotion {
    static SpaceState at(SpaceState state,double mu,double elapsed) {
        var p=state.position();var v=state.velocity();
        double r0=p.length(),sqrtMu=Math.sqrt(mu),alpha=2/r0-v.dot(v)/mu;
        double dt=elapsed;
        if(alpha>0) {
            double period=2*Math.PI/(sqrtMu*Math.pow(alpha,1.5));
            if(Double.isFinite(period))dt=Math.IEEEremainder(dt,period);
        }
        if(dt==0)return state;
        double sign=Math.signum(dt),target=sqrtMu*Math.abs(dt),rv=p.dot(v)/sqrtMu;
        // Solve on a positive interval even for backward evaluation. Radial collision arcs can
        // have a zero derivative; bisection also handles those without dividing by a radius.
        double lo=0,hi=Math.max(1,Math.min(target/r0,Math.sqrt(r0)));
        for(int i=0;;i++) {
            double value=sign*equation(sign*hi,alpha,rv,r0);
            if(value>=target || value==Double.POSITIVE_INFINITY)break;
            if(i>=100 || !Double.isFinite(value))throw new ArithmeticException("Unable to bracket Kepler anomaly");
            hi*=2;
        }
        for(int i=0;i<120;i++) {
            double mid=lo+(hi-lo)/2;
            double value=sign*equation(sign*mid,alpha,rv,r0);
            if(Double.isNaN(value))throw new ArithmeticException("Invalid Kepler anomaly");
            if(value<target)lo=mid;else hi=mid;
            if(hi-lo<=2e-14*Math.max(1,hi))break;
        }
        double chi=sign*(lo+(hi-lo)/2),z=alpha*chi*chi,c=c(z),s=s(z);
        double f=1-chi*chi*c/r0,g=dt-chi*chi*chi*s/sqrtMu;
        var position=p.scale(f).add(v.scale(g));double r=position.length();
        if(!(r>0))throw new ArithmeticException("Trajectory reached attracting center");
        double fdot=sqrtMu/(r*r0)*(alpha*chi*chi*chi*s-chi),gdot=1-chi*chi*c/r;
        return new SpaceState(position,p.scale(fdot).add(v.scale(gdot)));
    }
    private static double equation(double x,double alpha,double rv,double r0) {
        double z=alpha*x*x;
        return rv*x*x*c(z)+(1-alpha*r0)*x*x*x*s(z)+r0*x;
    }
    private static double c(double z) {
        if(Math.abs(z)<1e-4)return 0.5-z/24+z*z/720-z*z*z/40320+z*z*z*z/3628800;
        if(z>0){double sin=Math.sin(Math.sqrt(z)/2);return 2*sin*sin/z;}
        double sin=Math.sinh(Math.sqrt(-z)/2);return 2*sin*sin/(-z);
    }
    private static double s(double z) {
        if(Math.abs(z)<1e-4)return 1.0/6-z/120+z*z/5040-z*z*z/362880+z*z*z*z/39916800;
        double root=Math.sqrt(Math.abs(z));
        return z>0?(root-Math.sin(root))/(root*root*root):(Math.sinh(root)-root)/(root*root*root);
    }
    private KeplerMotion(){}
}
