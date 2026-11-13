# Gimbal model and AeroEngine helmet — 0.11.0-alpha.15

The existing `around_planet:orbital_sensor` becomes an open gimbal instrument:
a low mount, side pivots, copper outer ring, cyan inner ring and a directional
needle. White/yellow is the target end and red the tail. The gold base tab is the
block's forward reference; the red rear tab and differently colored side ports
retain the previous output convention. The item model includes its own static
rings and needle. Existing blocks/settings keep the same registry ID.

The animated pointer uses the selected orbital target. Inactive sensors park at
the forward reference. Geometry is original, built from vanilla material textures;
no Simulated models or textures are copied. The supplied Simulated gimbal was
inspected as a visual/function reference. Standard block-entity rendering keeps
native Sable lighting and existing through-seam/space rendering paths.

## Connect the AeroEngine helmet

1. Hold the AeroEngine engine monitor helmet.
2. **Shift + right-click** the orbital sensor on the physical construction.
3. Equip the helmet. Choose the desired target in the sensor's normal right-click
   settings screen.

Use the helmet's normal target settings to disable a linked instrument; its
existing dynamic-HUD switch also applies. The attitude layout's visibility,
scale and offsets are honored by the circular widget. Like other custom HUD
content, it is hidden while screens are open; editor changes take effect when
returning to the flight view.

The integration uses the native AeroEngine `MonitorTargets` and structure ID,
including the original toggle/unlink and destroyed-target pruning behavior.
A conditional Create goggles interface makes the sensor eligible for AeroEngine's
normal binding system, and also supplies ordinary goggles information. The new
sensor deliberately remains a generic native monitor rather than occupying the
single ordinary gimbal attitude slot. Therefore both kinds of sensor can share
one helmet. Native same-sublevel/target-count restrictions still apply.
If several orbital sensors are enabled, the first enabled loaded one is shown;
use the helmet settings to choose which remains enabled.

Only this sensor's generic left-hand text is replaced by the new circular
instrument, avoiding overlap with cosmic telemetry. Other AeroEngine instruments
and the ordinary gimbal HUD retain their native behavior. No changes to the
AeroEngine JAR are needed.

## Circular orbital horizon

The bottom-right circle is relative to the sensor's forward axis, not head look.
Blue/brown hemispheres show the planet's radial-up reference; roll and pitch tilt
the horizon. Latitude bands and spherical shading provide depth. The center
boresight is fixed to the construction. The selected mode's glyph and label
identify the requested direction:

- Green indicates the selected target in the forward hemisphere.
- Orange at the rim and a rear-target label indicate a target behind the sensor.
- Crossed glyphs distinguish the opposite directions; triangles mark orbital
  normals and diamonds radial/planet directions.
- Missing orbital data shows a short no-signal label and no false target marker.

The server sends target/up unit vectors in physical cell axes, using the same
inertial trajectory and point-velocity calculation as the redstone controller.
The client transforms them through the interpolated Sable body pose every frame.
Thus fast native rotations move the needle and horizon smoothly rather than
stepping only at the two-Hz telemetry interval. Modes, redstone outputs and saved
settings retain their earlier behavior. No terrain, generation or extra chunk
loading is requested.

The round texture is bounded to 96×96 and cached by quantized attitude. The mesh
is batched through a standard entity render type and uses fixed topology without
per-face allocations. This remains an instrument, not automatic steering or SAS.
The current circular widget is a helmet overlay, not an AeroEngine world-display
block renderer.

## Optional dependencies and checks

AeroEngine is optional; the sensor/model work without it. Create's goggles API is
compile-only and conditionally mixed in, so standalone Around Planet does not
require Create. Nothing from the supplied JARs is bundled. Tested integration:
user-supplied `AeroEngine-1.3.0.jar`, Create 6.0.10, Sable 2.0.6 and the supplied
Aeronautics/Simulated 1.3.2 bundle.

106 unit tests include instrument axes, front/rear projection, transparent sphere
bounds and rolled horizon shading. Native server tests bind a real helmet using
its item action, verify structure ID, save/read the ItemStack, preserve an orbital
sensor alongside an ordinary gimbal, prune the destroyed gimbal and honor disabled
target state. All 25 native tests pass with AeroEngine; 25 pass in the basic
Sable/Create/Cruising set and 13 pass without optional mods.

The actual Sodium/DH/Simple Clouds/Aeronautics/AeroEngine client exercises the
settings UI, native helmet binding, rendered gimbal and circular target overlay,
occupied height rebase, stable physics, map/detachment and seated planet return.
Captures were inspected, including removal of duplicated left-side text.
Production artifacts exclude all test classes and optional dependency JARs.
