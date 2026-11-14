# Late atmospheric braking and blue haze — 0.11.0-alpha.10

The previous atmosphere began at 6000, used substantial drag high above the
handoff shell and allowed a 20-block/s settling phase. The new shipped profile is:

```toml
[space]
entryY = 2208
atmosphereTop = 4000
atmosphereBottom = 2000
atmosphereDrag = 0.012
reentryMaxSpeed = 50.0
```

The ascent threshold stays above the stock Simple Clouds ceiling. The return
shell is now `min(atmosphereBottom, entryY-32)`: 2000 by default, so the final
braking band is traversed in virtual flight. Lower custom ascent thresholds keep
a safe hysteresis. Cosmic altitude remains distance from planet center minus
planet radius, unrelated to the physical cell's storage Y.

Both layers grow quadratically. One percent of full density comes from the
4000–2000 ramp; 99 percent comes from a second quadratic ramp over the final
500 blocks. With `u=clamp((top-h)/(top-bottom),0,1)` and
`w=clamp((bottom+500-h)/500,0,1)`, density is `0.01*u² + 0.99*w²`.
Shorter custom bands shrink the lower ramp to fit. The weak upper layer starts
at 4000 without taking most orbital speed away near 3500. A 500-block/s radial
inertial descent retains over 450 blocks/s at 2500; most loss occurs below it.

Quadratic speed drag retains the existing stable per-step solution. Drag no
longer reduces a faster body below the handoff speed (50 blocks/s = 180 km/h).
Already slower objects are neither accelerated nor braked to that threshold.
The handoff cap is also 50, including skipped-shell steps and readiness waits.
Native gravity can still accelerate a falling body after return; this is not a
constant-speed landing controller. Relative/angular native motion remains native.

A retained `space.atmosphereProfileRevision` migrates only the old shipped top
6000, drag 0.02 and handoff cap 20 once; defaults add the new bottom 2000. Other
custom values are kept, and subsequent explicit edits are not overwritten.

## Atmosphere rendering

The sky adds one fullscreen GPU pass with eight samples per covered pixel.
Analytic ray/sphere intersections delimit the 4000-block shell and clip the path
at the opaque planet surface. Exponential radial density and optical depth make
the limb blue, fade the upper edge and fill more of the view when the observer
enters the shell. The fixed global Sun lights the day side; the night side is
fainter. No terrain, generator queries, cloud meshes or extra chunk requests are
introduced. Native ships still render in front of the sky.

The server sends the effective atmosphere top in the existing four-Hz telemetry,
so clients show the configured shell rather than their local server defaults.
The status-payload protocol is now version 4: install the same alpha.10 on client
and server. Shader registration supports normal resource reloads. The existing
planet/star buffers and one static fullscreen quad are reused.

The haze makes atmospheric approach more legible and softens the sky change;
it does not yet blend native terrain into the procedural globe at reentry or
replace normal-world weather/fog after return.

## Validation

79 unit tests include upper-layer speed retention, majority loss below 2500,
monotonic drag, no viscous speed undershoot, no artificial acceleration of slow
objects, gravity/drag descent, and final 50-block/s handoff. 21 native and 10 standalone server tests cover
one-time migration and native whole-cell rotary/seat/relative-motion return at
the new shell. Combined Sodium/DH/Simple Clouds client captures the shaded haze
outside and inside the shell, then runs map/catalog and seated return checks.
