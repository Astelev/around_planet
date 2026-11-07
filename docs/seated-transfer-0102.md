# Native Sable seats and seam transfer (0.10.2)

The native Create seat entity lives in Sable's reserved plot coordinates. Its
passenger lives in world coordinates. A seat's containing sublevel, rather than
its terrain tracking sublevel, identifies the carrier. Addon seats using this
same native mechanism follow the same path without a dependency on Create.

Ship group transfer now snapshots world-space riders, rebases the rigid bodies,
and moves only those riders. Plot-local entities remain at their storage
positions. Mounted `ServerPlayer.teleportTo` deliberately invokes Sable's native
server/client coordinate conversion; no world-space vehicle correction packet is
sent for a plot seat. Player chunk tracking is updated after the world position
changes. Ordinary boats and standing passengers retain their existing path.

## User log, 7 November 2026

At 22:40:44 both diagnostics show the player at approximately
`X=20,612,105.5, Z=20,505,607.5`, inside Sable's reserved storage space. After
`/tp` at 22:40:52, a vanilla chunk-forget packet hits Sable's
`UnsupportedOperationException: Cannot drop chunks in plot`, disconnecting the
client. Preventing the original coordinate-space mixup avoids that path; this
change does not suppress Sable's storage protection.

A separate integrated-server failure at 22:41:21 originates in
`gadgetsngizmos@2.0.0`,
`TimedItemTransformations.onServerTick(TimedItemTransformations.java:199)`:
`ConcurrentModificationException` while copying a `WeakHashMap` key set. That
later exception has no around_planet frame; its cause is not established by this
log and is not claimed fixed here.

## Regression checks

- Production build and unit tests.
- Native server tests with Sable 2.0.6, Create 6.0.10 and Cruising 1.1.4-diagnostic:
  real Create seat with a pig rider, both crossings, unchanged storage position,
  preserved mount and velocity, subsequent native rider repositioning; existing
  boat/standing passenger, rotary constraint, collision and generation tests.
- Real integrated client with those mods plus Sodium 0.8.13 and DH 3.3.3:
  `-PplanetClientSmoke -PplanetSeatTransferSmoke`, a native Create seat and the
  actual player; east/west crossing and dismount. Assertions cover both server
  and client coordinate spaces and mount identity.

The exact Create and Deco / Aeronautics addon seat was not included in the local
fixture; it exercises Create's native seat and Sable's actual packet hooks.
Test harnesses are excluded from the published JAR.

The supplementary standing-player client scenario initially lost attachment at
an east crossing under the software renderer. A control run using the unmodified
production sources and a repeat with the final patch both passed east/west,
interaction, unchanged render identity and interpolation checks. The initial
failure is retained in `build/seat-standing-regression.log`; its cause is not
established and it is not claimed fixed by the seat change. Native server
standing/boat/joint regressions passed as well.
