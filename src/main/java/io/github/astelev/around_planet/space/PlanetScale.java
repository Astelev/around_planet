package io.github.astelev.around_planet.space;
/** One equatorial circumference controls the square world chart and physical radius. */
public record PlanetScale(int circumference) {
    public PlanetScale {if(circumference<256 || circumference>59_000_000 || circumference%32!=0)throw new IllegalArgumentException("Planet circumference must be a multiple of 32 in [256,59000000]");}
    public int boundary(){return circumference/2;}
    public double radius(){return circumference/(2*Math.PI);}
    public PlanetGeometry geometry(){return new PlanetGeometry(boundary(),boundary());}
}
