package io.github.astelev.around_planet.space;
/** Apparent Moon in the rotating observer's tangent frame; shares the inertial orbital ephemeris. */
public record MoonAppearance(SpaceVector center,double radius) {
 public static MoonAppearance at(CelestialSystem system,PlanetGeometry planet,PlanetSpin spin,SpaceVector world,double gameTicks){var observer=spin.position(planet,world.x(),world.y(),world.z());var moon=system.moonSeconds(gameTicks/20).position();return new MoonAppearance(spin.frame(planet,world.x(),world.z()).toLocal(moon.subtract(observer)),system.moonRadius());}
 public double angularRadius(){return Math.asin(Math.min(1,radius/center.length()));}
}
