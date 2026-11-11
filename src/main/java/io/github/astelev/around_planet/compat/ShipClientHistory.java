package io.github.astelev.around_planet.compat;
public interface ShipClientHistory { default void around_planet$shiftRenderCache(double dx){around_planet$shiftRenderCache(dx,0,0);}
    void around_planet$shiftRenderCache(double dx,double dy,double dz); }
