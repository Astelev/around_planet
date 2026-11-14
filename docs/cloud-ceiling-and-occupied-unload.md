# Cloud ceiling and occupied holding unload — alpha.9 / Cruising 1.1.6

## Evidence and missing unload path

The supplied client/server log keeps the same native UUID across high flight,
with `skyMode` changing and later `tracked=none` / `selected=none`. Five-second
samples do not prove the exact removal instant or distinguish a native server
unload from a client tracking loss. The log does not contain a fatal rendering
exception explaining the reported seat interruptions.

Inspection of Sable 2.0.6 found a separate native path:
`SubLevelHoldingChunkMap.processUnload` retires ships intersecting a terrain
column and consults the force-loaded collection, independently of the physics
ticket manager's player safeguard. Cruising's temporary terrain mode permits
those ordinary columns to unload. Prior crew protection covered physics ticket
updates, not this holding-column retirement.

Cruising now treats an occupied high connected group as protected for this one
native retirement decision. Crew can stand inside the current/previous hull or
ride a plot-local seat; connected rotary children share protection. Descending
assemblies keep this protection while their ordinary terrain requests resume.
No terrain generation, persistent force-load ticket, serialized body copy or
seat replacement is added. Protection expires as soon as real occupancy ends.
Normal unoccupied unload and save behavior remain unchanged. Space cells retain
their existing full-chunk behavior/exclusions.

The existing five-second `CRUISING-FLIGHT` summaries gain `holdingProtected`,
`mounted` and `vehicle`. `CRUISING-NATIVE-REMOVE` records actual native removal
with side, UUID, altitude and reason, bounded to eight messages per side/level
per 100 game ticks. These distinguish future client tracking loss from server
native unloading. Diagnostics do not cancel removals.

## Lower space threshold

Simple Clouds 0.7.3 has default cloud base **128**. Its renderer scales noise
geometry by **8**. The tallest bundled type (cumulonimbus) ends at noise height
**256**, giving an upper bound `128 + 256 * 8 = 2176` in world blocks.
The new default ascent threshold is **2208**, with a 32-block margin. Normal
reentry uses **2176** (`entryY-32`). Atmosphere top and drag settings are unchanged.

This is the ceiling of the **stock** Simple Clouds presets, not a query of the
locally visible cloud or a live client-dependent server border. If a pack changes
cloud base height or noise types, adjust server `space.entryY` to the desired
ceiling plus clearance. That keeps one authoritative multiplayer threshold.
The shipped previous default 3000 migrates once to 2208, marked by
`space.cloudCeilingRevision`. Other custom values are kept; later explicit edits
to 3000 are also kept. Legacy 768 migration now uses the new default.

## Validation

- Cruising: 55 unit tests, 10 native GameTests with Sable/Create/Aeronautics, and
  2 standalone server tests. The new native holding-retirement regression uses a
  real Create seat/pig rider, standing player crew and a rotary child at Y=1500.
  It verifies native identity, seat/joint retention, normal nearby unoccupied
  unload, no terrain/ticket allocation, and release after crew leaves.
- Around Planet: 77 unit tests, 20 native GameTests and 9 standalone server tests,
  including one-time cloud-default migration and retained custom settings.
- Combined Sodium/DH/Simple Clouds client: actual seated **network player** and
  original ship survive an explicit holding-column retirement immediately
  before ascent; the existing sky/map/free-object/seat return checks then run
  at the new cloud-ceiling threshold.
