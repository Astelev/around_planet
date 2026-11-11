package io.github.astelev.around_planet.compat;
public interface ShipPoseHistory { default void around_planet$shiftHistory(double dx){around_planet$shiftHistory(dx,0,0);}
    void around_planet$shiftHistory(double dx,double dy,double dz);
    void around_planet$rotateHistory(org.joml.Vector3dc old,org.joml.Vector3dc target,org.joml.Quaterniondc q); }
