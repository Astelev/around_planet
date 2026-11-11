# Planet scale and independent map objects — 0.11.0-alpha.5

## One planet size

In `<world>/serverconfig/around_planet-server.toml`:

```toml
[planet]
circumferenceBlocks = 131072
```

This is the equatorial **circumference**, in blocks/meters. It is the single
source of size: radius = circumference / (2*pi), east/west seams = +/- half the
circumference, and solid north/south borders use the same half extent. The
square/equirectangular chart and its polar distortion remain; there is no new
north/south wrapping. At the default size, all four bounds remain +/-65536 and
cosmic radius is about 20860.76 m. A genuinely tighter vanilla border still
restricts playable space.

Use multiples of 32 between 256 and 59000000 so seam columns remain chunk aligned.
Restart after changing size. Old `eastWest.boundaryX` is migrated to twice its
value before ordinary config correction removes legacy fields. An explicit new
circumference wins. The independently adjustable old `boundaryZ` is removed;
Z now always follows the chosen circumference.

On load, saved planet-bound orbits are rebased to the new radius once. Radial
altitude, inertial direction, velocity and local entry basis are retained; mu is
scaled with radius squared to retain the captured surface gravity. Pending entry
contacts retain their altitude too. A size edit is an external world edit, not a
physical maneuver that conserves the old planet's orbital energy. Existing
terrain is never regenerated merely by resizing the chart.

## Checker sphere

The map now shades a sphere with a planet-fixed latitude/longitude checker.
Camera rotation changes the visible checks and lighting, showing curvature.
It uses a cached bounded texture (maximum 384 pixels on the longest viewport
side), rebuilt only for a changed camera/viewport/radius. Orbit samples do not
invalidate it. The per-pixel loop allocates no temporary vectors. Map lines are
batched, and texture resources are released when the screen closes.

## Independent bodies

The preceding alpha had `SpaceOrbitData.detach` as a mathematical/persistent
primitive; **automatic native detachment was not connected yet**. This alpha
connects it for loaded free groups.

When an unanchored free connected assembly gets at least 192 local blocks from
its owner's anchor on any axis, it receives an independent saved trajectory:

```
r_child = r_parent + entry_basis * local_center
v_child = v_parent + entry_basis * local_mean_velocity
```

The destination is prepared asynchronously. Until it is ready, no partial group
is moved. The whole connected group then moves to its own fixed cell, keeping
native ship IDs, plots, bearings/joints, angular motion and passengers. Its common
linear velocity becomes virtual rather than driving it across the new cell.
Later parent maneuvers do not modify this record. External constraints/leashes,
incomplete loading, an oversized hull or unavailable addresses defer detachment.
Native plot-local seat roots are recognized even before their world rider is
visible in the general entity tracker.

If a previously owned child loses its native connection, membership is reconciled:
the original first body retains the owner frame; disconnected parts become free
bodies and can subsequently depart. Rebasing the remaining owner compensates
both physical offsets and cosmic origin so the released object's cosmic position
does not jump. Connected rotors remain one assembly.

No persistent force-load ticket is added. Native Sable unload/observation rules
still apply; the independent orbit record survives unloading and save/load.
A reserved cell address is retained as metadata even when its body is unloaded;
it does not by itself force terrain or native physics to remain loaded. The
existing allocator is bounded to 81 addresses. Alpha.7 also [captures fast objects that unload before distance promotion](space-sky-and-unload.md), without reserving a new address while dormant. Releasing older dormant cell addresses and arbitrary orbital rendezvous / reactivation scheduling remain subsequent lifecycle work.

## Other trajectories on the map

While M is open, a server-authoritative catalog is sent once per second. It
contains saved records around the same planet, including unloaded records. Reading
them does not load chunks or native objects. The occupied cell keeps the existing
four-Hz HUD/path update. Closing the map unsubscribes; a bounded lease also expires
if the close packet is lost.

Other objects have stable ID-based colors, independent paths and position markers.
Hover a marker for its name or short ID; unloaded records are identified. R fits
all received paths. Up to 128 other records are sent; the count displays a fraction
if the catalog is truncated. Camera motion does not request new server data.
Native scene isolation and selecting/rendezvousing with another ship are not added
by this diagnostic view.

## Checks

- 63 unit tests: scale/alignment, checker sampling and inverse projection, orbital
  conics, clipping and previous mathematical regressions.
- 17 native GameTests with Sable/Create/Cruising: promotion of a real rotary group,
  absolute position/velocity inheritance, independent later maneuvers, disconnected
  owner rebasing, save/load, resizing pending contacts, native seats and prior seam /
  reentry behavior.
- 8 server GameTests without optional mods, including old/new config migration.
- Integrated client with Sodium/DH/Simple Clouds: actual free-ship promotion,
  catalog delivery of that ship plus a saved unloaded record, checker/rotated
  screenshots, key priority, live simulation and return with a real Create seat.

## Current unloaded reentry behavior (alpha.6 audit)

The desired dormant return is to move the saved body's world pose and holding
address to the planet destination without creating physics bodies or requesting
terrain tickets. It should then activate through native observation when that
world chunk loads. That storage/index relocation is **not implemented yet**.

Currently `SpaceReentry` evaluates an inward crossing even for an unloaded cell,
persists/holds the contact and velocity, then checks that all owner bodies are
available. A missing native body stops the transaction before destination
readiness/ticket requests. The original cell/storage address remains. When the
native group becomes available again, normal loaded reentry prepares destination
chunks and moves the group. It does not yet arrive in a dormant destination
chunk on its own. A new native regression test explicitly covers the held record,
unchanged cell address, retained velocity, and absence of destination chunk loads.
