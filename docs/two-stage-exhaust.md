# Active core and expanding gas cone (0.11.0-alpha.21)

[Alpha.22](large-flowing-exhaust.md) enlarges both sections and animates outward
gas flow on their surfaces. Dimensions and material details below refer to alpha.21.

Both creative thrusters and RSU nozzles now render an untextured gas plume.
The previous fire/soul-fire atlas animation is removed. No particles or animated
texture uploads are used.

Each plume has two sections:

1. A bright, slightly tapering active core. Its length decreases with effective
   thrust (configured force × redstone throttle), approaching zero at low thrust.
2. An expanding, less opaque gas cone after the core. Vertex alpha decreases
   continuously toward zero at its far end. Its length has a floor while the
   nozzle is active: **0.8 blocks for the creative engine, 0.24 blocks for RSU**.
   Above that floor it grows with thrust. Its opacity also falls with low thrust.

Zero configured thrust or zero signal hides both sections. A small smooth pulse
animates the active section and gas brightness; it cannot pull the cone below
its minimum length. The engine is white/blue and RSU white/cyan.

Each section has eight sides, for 16 batched quads per active nozzle. Circular
coordinates are precomputed once; all plumes share the built-in emissive lightning
render layer without textures or per-nozzle draw calls. Expanded finite render
bounds and the 96-block visibility distance are retained. No shader pack or new
optional dependency is required. Physics, sensor display, nozzle axes and power
synchronization are unchanged.

Unit coverage checks decreasing active length, the gas floor at tiny positive
thrust, power-off, bounds, opacity, and pulse behavior. The client smoke captures
full and low thrust, verifies the low-thrust setting arrives, checks that disabled
engines emit no geometry, and tests a wall-mounted nozzle.

Release checks: 122 unit tests, 32 native Sable/Create/AeroEngine server tests
and 15 standalone server tests pass. Full/low-thrust, disabled and wall-mounted
plumes pass the real-client smoke; the full/low screenshots were inspected after
allowing render frames between state changes.
