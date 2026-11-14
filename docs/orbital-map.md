# Orbital map — 0.11.0-alpha.6

Press **M** in a registered space cell to open the live navigation map. M and Esc
close it. The binding can be changed under Controls → Around Planet. In space,
the key is intercepted before ordinary Minecraft key bindings and NeoForge key
events, so a competing world-map binding does not receive the same press. Outside
space the ordinary map key/event path remains active. Chat, inventories and other
already-open screens retain their own keyboard handling.

- Left drag rotates the orthographic 3D view.
- Right or middle drag pans it.
- Wheel or +/- zooms; **R** or the Reset button fits the trajectory again.
- The initial/reset view faces the orbital plane, making circular/elliptical
  paths readable without first finding their plane manually.

The map also shows planet-centered **global inertial XYZ axes**: X red, Y green,
Z blue. Positive rays have arrowheads and + labels; negative rays are dim/dashed.
The globe occludes their hidden parts. A corner XYZ compass stays visible when
pan/zoom takes the origin offscreen; it follows camera orientation but ignores
pan and zoom. These are the cosmic axes shared by all orbit records, not F3's
Minecraft storage coordinates or the ship's rotating local axes.

The **checker-shaded sphere** is the planet, **yellow marker/arrow** is the current cell and
velocity direction, and **cyan line** is the future coast trajectory. Distant
parts behind the planet are occluded by its disk. Pe/Ap mark periapsis/apoapsis
when they are distinct; heights are relative to the planet radius. An orange
marker identifies the first inward crossing of the return shell. Future path
ends there instead of continuing through the planet. The instantaneous
osculating periapsis may still lie below the surface: it describes why the craft
will reenter, not a promise to pass through solid ground.

Elliptical, hyperbolic, radial and zero-gravity linear motion are supported.
Open escape paths have a bounded display horizon (50 planet radii or ten current
radii, whichever is larger), rather than sampling infinity. A waiting reentry
shows the held point rather than a fictitious continuing orbit.

The server sends r, v, mu, planet radius and return altitude in the existing
four-times-per-second telemetry. The client constructs the spatial conic from
those same orbital invariants; it does not call world generation, request chunks
or change the saved trajectory. Curves are bounded to 384 segments and rebuilt
only on a new sample while the map is open. Camera motion only projects cached
vertices. Predictions assume **no further thrust/collisions**; subsequent
impulses update the displayed path with the next telemetry sample.

The map **does not pause simulation**. Thrust, collisions and automatic reentry
continue; the screen closes when its cell membership ends. Keep matching client
and server JARs: telemetry protocol version changed to include orbital parameters.
The view also receives a bounded one-Hz catalog of independent saved orbits,
including unloaded records. See [planet size and objects](planet-scale-and-objects.md).

Validation: 66 unit tests (including conics, shell termination, near-parabolic
cases, camera rotation, clipping and zoom limits); 8 GameTests without optional
mods. Actual client with Sable/Create/Cruising/Sodium/DH/Simple Clouds checks M
priority against another M binding and key events, zoom/rotate/pan/reset, live
orbit updates and reentry with the map open; ordinary M binding/events resume on
the surface. Fit/rotated screenshots are visually inspected.
