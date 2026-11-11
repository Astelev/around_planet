# Atmospheric return — 0.11.0-alpha.8

The default ascent threshold is now **Y=3000**, above the AeroWorks atmosphere.
Physical space cells remain at Y=1536; their coordinates are storage/frame
addresses, not cosmic altitude. Existing cells also return through the current
configured threshold, rather than retaining the old 736-block return height.
The old shipped `entryY=768` migrates to 3000 when atmosphere settings are first
added. Custom thresholds remain intact.

In `<world>/serverconfig/around_planet-server.toml`:

```toml
[space]
entryY = 3000
atmosphereTop = 6000
atmosphereDrag = 0.02
reentryMaxSpeed = 20.0
```

Altitude is the length of the planet-centered cosmic position minus planet
radius. The atmosphere therefore starts at **radius + 6000**, independently of
the cell's physical Y. The lower shell is `entryY-32` (2968 by default). The
effective top is at least 512 blocks above that lower shell.

Below the top, during inward flight, a dimensionless density fades from zero
to one as `((top-altitude)/(top-bottom))²`. Each short observation step applies
quadratic drag opposing the full virtual velocity:

```
v_new = v / (1 + atmosphereDrag * density * |v| * dt)
```

The state is sampled using the existing Kepler solver, and this drag impulse
rebases the trajectory. No native owner's linear velocity is used as an orbital
speed. Rotation and joints are retained. Free bodies receive the opposite frame
impulse, keeping their independent inertia instead of silently inheriting an
external force on the owner. Above the atmosphere and on outbound flight, normal
Kepler coasting is unchanged. The map displays the current osculating trajectory;
it does not yet predict future atmospheric drag or show heating effects.

Before contact is held and again immediately before native transfer, the owner's
common virtual speed is capped to `reentryMaxSpeed` (20 blocks/s = 72 km/h). This
also covers a very fast step that skips the atmospheric band, manual returns,
and engines applied while destination chunks are still being prepared. The
handoff keeps the velocity direction and preserves native internal relative and
angular motion; independent fast debris is not rigidly frozen to the owner.
Destination chunk and lighting readiness still gates the whole transaction.
The ordinary-world handoff accepts the new high altitude without enlarging
physical cell bounds or changing native plot addresses.

Use **Cruising 1.1.5-diagnostic** alongside this version. Its terrain exemptions
are disabled as soon as a connected assembly's mass-weighted mean vertical
velocity is negative (apart from a 0.0001 blocks/s solver tolerance). This applies
both to player temporary chunks and native ship terrain tickets. A rotor's local
motion and player motion inside a cabin do not replace assembly velocity.
There is no sticky altitude latch; ascent/hover becomes eligible again normally.

## Checks

- Around Planet: 77 unit tests, including density/decay stability, an inward
  1200-block/s gravity/drag descent and skipped-shell speed limits; 20 native
  server tests, including a high-speed rotary/free-body return at Y=2968,
  bounded owner speed, preserved relative/angular motion and config migration;
  9 server tests without optional mods.
- Cruising: 55 unit tests; 9 native tests with Sable/Create/Aeronautics. A real
  high ship stops both player and native terrain exemptions when given -20
  blocks/s vertically, then regains cruise eligibility after stopping descent.
- Combined client with Sodium/DH/Simple Clouds: ascent at 3000, native seat and
  mesh identity, space sky/map/independent objects, return above 2000 and real
  player chunks retained during descent.

Dormant surface holding-record relocation remains a separate unfinished step.
Atmospheric observations do not create missing native bodies or force their
holding chunks to load. Contact still waits for native observation as before.
