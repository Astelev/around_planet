# Space height migration and CPU diagnostics (0.11.0-alpha.16)

## Height and existing saves

Old layouts at Y=1400–1700, including a custom `space.cellY=1700` with
`heightLayoutRevision=1`, migrate to Y=6144 and revision 2. Previous migration
recognized only Y=1536. New configuration values must be in 3072–30000;
valid custom high values remain unchanged. Persisted low cells are still readable
and relocate using the existing rigid assembly/rider transfer. Unsafe or incomplete
assemblies retry at most once per second and emit bounded `space.height_deferred`
reasons. No partial assembly is moved to force a migration through.

## Rendering and diagnosis

The helmet overlay batches all ring/marker quads instead of flushing a draw call
for each pixel. Its sphere texture uploads at most once per game tick. This fixes
a real HUD inefficiency but cannot explain freezes when no helmet/sensor is used.
The client now reports `space.hud`, `space.sensor_model` and `space.frame`; the
server reports `space.control`, actual cell centers and configured `spaceCellY`.

With existing `diagnostics.enabled=true`, `event=cpu.load` reports five-second CPU
deltas for the entire process and up to eight busiest Java threads with stacks.
`processCores=1` means one logical CPU fully occupied; divide by `processors` to
compare with a whole-machine percentage. `javaCores` excludes JVM/native workers
and threads that disappear during the interval; the difference is not a measurement
of physics. Stacks are instantaneous samples, whereas CPU deltas cover the window.
`client.slow_tick` and `space.slow_frame` capture stalled client work over 500ms,
limited to one stack per five seconds. Disabling diagnostics stops these samples.

User testing found that pausing DH background generation strongly reduces CPU in
an empty world. This identifies a major contributor, but does not establish why
usage differs from the older pack or whether completed LOD data is regenerated.
No forced global reduction of DH settings or changes to physics are included.
Collect `latest.log` after roughly a minute stationary with generation enabled,
then another minute paused. Keep the same world, location, view distance and frame
limit. The CPU events can distinguish DH workers from render/server work.

## Validation and limits

109 unit tests, 13 standalone server tests and 25 native server tests pass. The
native tests include legacy Y=1700 relocation with occupied riders. A real client
without a helmet or sensor completed entry at 6144, occupied relocation to 9000,
stable physics, orbital map/detachment and seated reentry. Its save began with
cellY=1700/revision=1 and ended at 6144/revision=2.

A separate helmet run confirmed the batched widget renders correctly, but later
stalled in Sable's client entity-collision block-volume loop during reentry.
This intermittent issue is not presented as fixed; the new client-tick sampler
also covers that path. The local software-rendered test pack is not a substitute
for profiling the user's full pack.

`-PplanetClientSmoke -PplanetEmptyPerfSmoke` runs an isolated empty-world comparison
of the enabled flag and DH generation state with a JFR capture. All four phases
completed in the local client; the empty-world `space.control` averaged about
0.01 ms/call. Software rendering dominates this host, so its overall CPU figures
are not used to predict hardware-rendered performance. Toggling the enabled
flag does not unload mixins; this is not equivalent to removing the mod. The test
harness and recordings are excluded from the production JAR.
