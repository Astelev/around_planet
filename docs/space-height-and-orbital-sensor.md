# High space cells and orbital attitude sensor — 0.11.0-alpha.14

## Physical space height

The default physical cell center is now **Y=6144**, up from 1536. This is independent
of the 2208 ascent threshold, orbital altitude and the atmosphere's 4000–2000 band.

Edit the existing world's `serverconfig/around_planet-server.toml`:

```toml
[space]
cellY = 6144
```

For singleplayer this is normally `.minecraft/saves/<world>/serverconfig/`; on a
server it is `<world>/serverconfig/`. `defaultconfigs/` templates affect newly
created worlds, not an existing world's saved server config. Restart after editing.

The previous spec silently corrected values outside 1400–1700; saved-cell
validation repeated the same restriction, and ship-fit checks also rejected any
hull above 2000. All three constraints are corrected. `cellY` accepts 1400–30000;
the narrower legacy range remains loadable to migrate old saves safely. The old
shipped value 1536 upgrades once to 6144 via `heightLayoutRevision`. Explicit
custom heights are retained. The 256-block half-height safety envelope still
bounds each cell; changing height does not expand its horizontal layout.

A loaded complete cell is moved vertically as one transaction, including free
loaded bodies and entities, native seat/passenger trees, histories and clients.
Its inertial trajectory, orientation and velocities do not change. Missing owner
members, external constraints/leashes and incompatible clients defer relocation.
Unloaded owners are not force-loaded: their old address stays valid until they
activate and can move safely. New/detached cells use the configured height.

Vanilla terrain/build height is not expanded. Actual ship blocks and block
entities still live in Sable's native plots; high coordinates are physical poses
and entity addresses. Cruising remains disabled inside registered space cells.
This is not a new high-altitude block-building layer for ordinary terrain.

## Orbital attitude sensor

Item/block ID: `around_planet:orbital_sensor`. Find it in the vanilla Redstone tab
or use `/give @s around_planet:orbital_sensor`. The survival recipe uses four iron
ingots, two redstone dust, two amethyst shards and a compass. No extra Simulated
or Aeronautics dependency is added; the new sensor is an independent two-axis
redstone instrument, not a copied implementation or model.

Place it on a Sable construction in an active space cell. Its observer-face side
is the instrument's forward axis. Right-click opens the non-pausing settings
screen with live yaw/pitch error, target buttons and the angle producing full
redstone power. The original Simulated gimbal sensor in the supplied Aeronautics
bundle was inspected for its four directional redstone-port convention.

| Mode | Target |
| --- | --- |
| Prograde / Retrograde | Along / against inertial orbital velocity |
| Radial out / in | Orbit-plane radial direction perpendicular to velocity |
| Normal / Anti-normal | Along / against `position × velocity` |
| Away from / Toward planet | Directly away from / toward the planet center |
| Toward Sun | Fixed inertial solar direction |

Radial-out and planet-out differ on an eccentric/radial flight path. At zero
speed the velocity modes are unavailable; an undefined orbital plane gives no
normal/radial target. Invalid/out-of-space sensors emit zero rather than a false
control signal. No automatic torque or ship stabilization is applied by the block.

| Physical port | Positive error signal |
| --- | --- |
| Front / back | Pitch up / down |
| Right / left, looking along the front axis | Yaw right / left |

Power is 0–15, with a one-degree dead zone. The default full-scale angle is 45°;
the UI offers 15°, 30°, 45°, 90° and 180°. At exact vertical pointing yaw is
undefined and is suppressed. Roll around the selected direction is unconstrained.
Wire the four outputs to steering/thrust logic as with a two-axis gimbal sensor.
Placement determines the forward axis; the block's rotation/mirror states also
follow structure transforms.

The sensor combines the saved inertial state with its native relative point
velocity, including local angular motion, and transforms the target through the
saved cell frame and actual native body orientation. It does not mistake the
locally centered owner for a stationary orbital object. It can also query a
loaded independently bound dormant object without allocating a new space cell.
No terrain queries or remote chunk loading are needed.

Configuration is server-authoritative and persisted. Requests validate enum/range,
build permission, already-loaded block entity and physical reach, resolving Sable
storage addresses for both instrument and player. Redstone updates at ordinary
server tick frequency; visual telemetry is throttled to two Hz and only sent
when it changes. The screen and recipe have English and Russian translations.

## Validation

103 unit tests cover orbit directions, degeneracy, perpendicular radial targets,
control errors, dead zones and saturation alongside earlier flight math. Server
checks accept and round-trip `cellY=9000`, migrate only the old default, read true
virtual velocity from a real native sensor, verify opposite ports, rotate the
body, persist configuration and rebase a live cell without changing its orbit.

25 server tests pass with Sable/Create/Cruising; 13 pass without optional mods.
The real Sodium/DH/Simple Clouds/Sable client assembles a chest, sensor and Create
seat, configures mode/scale through the UI/network, ascends to 6144, rebases the
occupied cell to 9000, continues native physics and returns to the planet with
the original seat, meshes and passengers. Captures include the working sensor
screen. Production artifacts exclude every test/smoke class.
