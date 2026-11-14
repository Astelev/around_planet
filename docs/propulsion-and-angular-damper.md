# Vacuum propulsion and angular damping (0.11.0-alpha.18)

Three new blocks appear in the Redstone creative tab and inventory search.
They work with Sable in ordinary terrain and Around Planet space cells. Their
physics does not query atmospheric density, engine RPM or generated terrain.

## Creative thruster

Right-click with an empty hand and set thrust using the logarithmic slider
(0–10,000,000 N, default 10,000 N). Neighboring redstone provides a proportional
throttle: 0 is off, 15 is full thrust. The white end points along thrust; the dark
orange nozzle is the exhaust. Placement uses the clicked face and supports all
six axes. It needs neither fuel, FE nor rotation. This creative block has no
survival recipe.

## Four-way RSU

The RSU occupies an 8×4×8 model-space hitbox, with a quarter-block height and a
quarter-block footprint. It mounts on floors, ceilings or walls. Four colored
nozzles have independent built-in **native Create redstone-link receivers**.
A nozzle pushes the ship away from its colored exhaust side. Gold/copper/red/green
labels identify those physical nozzle sides; they are not global compass axes.

In the screen, select either frequency slot for a nozzle, then click an item in
the inventory grid below. This creates a ghost copy without consuming/moving the
item. Select a slot and press Clear to erase it. Two blank slots disable the
channel. Matching Create transmitters and controllers work normally, including
Create's range rules. The location follows the moving ship; receiver range is
refreshed twice per second and transmitter changes propagate through Create.
All four channels have a common adjustable maximum thrust (default 1,000 N per
nozzle); each channel's received signal independently scales it by signal/15.
No fuel, FE or shaft rotation is needed in this first implementation. Create is
optional for the mod, but is required for RSU wireless input.

## Angular inertia damper

An FE-only input buffer holds 1,000,000 FE and accepts power on every face.
Redstone enables damping; its strength and signal scale an exponential angular
velocity decay. Strength is adjustable from 0.1 to 20 per second (default 2).
The damper never directly changes linear velocity or overrides joint constraints.
It acts on the Sable body containing it; linked bodies respond through the native
constraint solver, rather than having all bearing velocities reset together.

FE cost is proportional to removed rotational kinetic energy (0.1 FE per joule,
rounded up per active substep). When the buffer cannot cover the requested work,
the reduction in rotation is limited to what the available FE can pay for. No
power is consumed at rest, with redstone off or with invalid native mass data.
An empty buffer provides no damping. FE and settings survive chunk/world reload;
the current ordinary block drop does not preserve the buffer or configuration.
RSU and damper have simple iron/copper/redstone crafting recipes; the damper also
uses an ender pearl.

## Physics and lifecycle

Only loaded propulsion block entities are visited, without scanning world chunks.
Forces are integrated per native substep in seconds. A point impulse contributes
both linear momentum and torque about the body's center of mass using its inertia
tensor. The body pose transforms local engine directions into world vectors.
This happens before the space controller captures common velocity, so owner thrust
updates the saved virtual trajectory while native collisions/joints remain active.
The blocks also work on detached objects inside a space cell.

Wireless receivers disconnect when reconfigured, removed or unloaded. Runtime
registries clear on level unload. Idle blocks do not broadcast unchanged state;
changed FE/signals are synchronized at most twice per second, and configuration
changes synchronize immediately. Configuration packets require build permission,
a loaded block and actual-world proximity, including Sable plot transforms.
Frequency changes identify a server inventory slot rather than accepting arbitrary
client item data. Optional mod JARs and test classes are never bundled.

## Validation

Unit tests cover throttle/seconds, substep independence, exponential damping,
finite inputs and FE energy budgets. Native Sable/Create tests cover thrust and
off-center torque, power-off, FE capability/persistence, preserved translation,
independent wireless channels, stale-signal removal, compact collision geometry
and capture of thrust into virtual orbital velocity. The real client exercises
all eight frequency ghost slots, the thrust slider and the FE display. Existing
seam, orbit, sensor and AeroEngine tests are retained.

Release validation: 117 unit tests, 30 native Sable/Create/AeroEngine server
tests and 13 standalone server tests pass. The final client smoke passes with
Sodium, DH, Simple Clouds and AeroEngine; all three screens and world models
were captured and visually inspected.
