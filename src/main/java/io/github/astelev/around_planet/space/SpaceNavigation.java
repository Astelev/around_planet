package io.github.astelev.around_planet.space;

/** Persisted entry basis. Cell axes do not rotate as the orbit moves around the planet. */
public record SpaceNavigation(PlanetGeometry planet,SpaceFrame frame) {}
