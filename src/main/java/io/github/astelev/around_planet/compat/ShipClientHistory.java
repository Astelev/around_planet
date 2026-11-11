package io.github.astelev.around_planet.compat;
public interface ShipClientHistory { default void around_planet$shiftRenderCache(double dx){around_planet$shiftRenderCache(dx,0,0);}
    void around_planet$shiftRenderCache(double dx,double dy,double dz);
    void around_planet$rotateRenderCache(org.joml.Vector3dc old,org.joml.Vector3dc target,org.joml.Quaterniondc q); }
