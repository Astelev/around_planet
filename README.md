AeroSpace
========

A space exploration addon project for Create and Aeronautics, developed in stages.

- Mod ID: `aerospace`
- Minecraft: `1.21.1`
- NeoForge development version: `21.1.256`
- Java: `21`
- Java package: `io.github.astelev.aerospace`

Build with `./gradlew build`. The experimental high-altitude flight integration
targets Sable 2.0.6, Create 6.0.10 and Aeronautics 1.3.2.
See [high-altitude flight](docs/high-altitude-flight.md) for behavior, limitations
and integration tests. AeroWorks and AeroUtils are intended
to coexist with AeroSpace without being required dependencies. Compatibility
with those mods has not been tested yet.

Optional [Distant Horizons 3.3.3 flight support](docs/distant-horizons-flight.md)
uses coarse LODs while cruising and refines after slowing down, with F3 diagnostics
for DH's generation gates.

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
