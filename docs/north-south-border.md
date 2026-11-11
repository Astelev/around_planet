# Solid north/south borders (0.10.3)

North and south use an ordinary world border instead of reflected transitions.
West and east retain their existing periodic seam.

In `<world>/serverconfig/around_planet-server.toml`:

```toml
[eastWest]
enabled = true
boundaryX = 65536
boundaryZ = 65536
```

`boundaryZ` sets the walls at `Z=-boundaryZ` and `Z=+boundaryZ`, independently of
X, centered on Z=0. Range: 128–29,500,000 blocks. Restart the world after changing
it. Existing configurations acquire the default 65,536-block boundary.

This uses native border rendering, entity collision and proximity warnings on
both client and server. There is no Z teleport, rotation or generation blending.
The invisible X safety guard and suppression of its danger vignette remain as
before; the solid Z walls retain their warnings. Saved vanilla border center and
size are not rewritten. A tighter overlapping vanilla border wins. A vanilla
border entirely disjoint from the planet retains its vanilla behavior, avoiding
inverted bounds. Disabling the planet restores vanilla bounds.

Validation: production build, 27 unit tests and 7 native GameTests with
Sable/Create/Cruising. The added test checks both Z collision shapes, warning
distance, an open X crossing corridor, tighter/displaced vanilla borders and
restored bounds on unbind. The dedicated integrated-client harness
`-PplanetClientSmoke -PplanetNorthSouthBorderSmoke` drives the player against each
Z wall, checks client config synchronization, then crosses the east/west seam.

The new limit is a vanilla world border, not a custom rigid-body wall for Sable.
