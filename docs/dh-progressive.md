# Progressive DH generation experiment (0.10.0)

Implemented in around_planet only. DH remains optional; the integration targets
DH 3.3.3 on Minecraft 1.21.1. Cruising's settings and code are unchanged.

## Why this can help

In DH's built-in SURFACE_THEN_CHUNKS plan, data detail levels above zero use its
rough surface generator. Each native source has 64×64 columns, so an 8-block
column source covers 512×512 blocks instead of 64×64 at one-block detail. Rough
sampling interpolates many columns and covers more ground per generator query.
A detail-zero request can still run FEATURES chunk generation. Changing a task's
requested detail alone is insufficient: its encoded section position determines
the size and actual detail of the API source. We request real ancestor positions.

Near the generation target, a fine request now waits for native sources at data
details 3, 2, 1, then runs at detail 0 (8→4→2→1 block columns). Sibling requests
share pending ancestor requests. Existing available coarse stages are reused.
Only after the provider's native completion callback has handed a source to DH's
updater does the scheduler admit the next stage. It never retains pooled sources.

DH's existing data updater merges generation levels and its quadtree retains a
renderable parent until the children are renderable. We do not clear the database,
replace its renderer, change the player's requested quality, or create extra world
generation workers. Existing fine data is not deliberately downgraded. Later stages
still perform their own generator work; this is progressive coverage, not a shared
cache of Minecraft noise samples or reuse of full chunk-generation intermediates.
Additional surface work can increase total generation time; measure first coverage
and eventual refinement separately.

Native queue selection gives nearby coarser surface tiers priority over finer work.
Within a tier it preserves the native rank, including Cruising's forward bias.
The reducer captures its context, including when ConcurrentHashMap reduces in
parallel. Already running chunk tasks are not preempted. Preview ancestors crossing
the canonical world boundary are skipped. X-distance includes the opposite seam.
Custom/API generators and remote retrieval queues use their existing path.

## Settings

In `<world>/serverconfig/around_planet-server.toml`, restart after changes:
(`defaultconfigs/around_planet-server.toml` supplies defaults for new worlds.)

```toml
[dhProgressive]
    enabled = true
    initialDataDetail = 3
    previewRadiusBlocks = 1024
    maxDeferredRequests = 128
```

`initialDataDetail` is 1..4 (2..16-block columns). A larger value starts with a
wider source and adds refinement stages. The preview radius is 128..4096 blocks; selection uses distance to the source
footprint, so a large source covering the player keeps priority even when its
center is outside the radius.
The deferred request limit is 16..512 per provider, separate from native limits.
Shared previews and completed-stage bookkeeping are bounded. Cancellation of a
single fine request does not cancel a shared parent needed by siblings. Closing
the provider clears the scheduler. Set `enabled=false` for a baseline comparison.
The DH plan must have surface generation enabled. CHUNKS_ONLY is left unchanged.

## Diagnostics and testing

Existing `[AP-DIAG]` summaries include configuration and:

- `progressive.stage_submit.N`: native submissions at data detail N;
- `progressive.published.N`: completed native publication callbacks;
- `progressive.logical_request`, `progressive.failed`, `progressive.stage_split`;
- `progressive.queue_to_result.N`: queue + generation time, **not pure CPU time**;
- `progressive.queue_rank`: native queue selections with tier priority;
- `progressive[pending/stages]` in each queue snapshot: deferred fine requests and cached/in-flight stage barriers.

For comparison, use fresh areas at identical seed, fixed view distance and DH
thread settings, with the game unpaused. Record time to the first rough coverage,
time until refinement, and queue/time counters. Do not compare a cold database to
a previously generated one. User modpack performance still needs measurement;
passing correctness tests is not a claimed speedup.

Unit tests cover the publication barrier, order, sharing, cancellation, retry,
bounded backlog, cached stages, priorities and periodic distance. The optional
client smoke test `-PplanetClientSmoke -PplanetProgressiveSmoke` exercises real DH
provider callbacks, pooled-source ownership and queue reduction with deterministic
terrain work, alongside Sodium, Create, Sable and Cruising. It does not benchmark
terrain generation. All smoke classes are excluded from the production JAR.

`-PplanetProgressiveTerrainSmoke` additionally uses the real native generator in a
noise-based test world (`-PquickPlayWorld=around-ew-generation`): all 4096 columns
were populated at each of data details 3, 2 and 1. A cold local run took 6195, 822
and 532 ms respectively. These successive timings include JVM/cache warmup and
cover different areas; they are **not** a baseline-versus-experiment speedup.
