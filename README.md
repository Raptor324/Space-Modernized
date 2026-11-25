<div align="center">
  <img src="docs/images/Space-enl.png" alt="Space-Modernized" width="160" />
  <h1>Space-Modernized</h1>
</div>

***

## ENG Version 🇺🇸 | [RU Версия 🇷🇺](/README.ru.md)

**Status:** Beta (`0.1.0-beta`) \
**Minecraft Versions:** 1.20.1 (Forge) / 1.21.1 (NeoForge) \
**Mod ID:** `space_m` \
**Required Dependencies:** HBM-Modernized (`hbm_m`), Architectury API \
**License:** GPL-3.0-only (see [License](#-license))

***

## 📥 Official Platforms

<div align="center">

# <img src="https://cdn-icons-png.flaticon.com/128/5968/5968756.png" height=28 /> <a href="https://discord.gg/f2BhvzG6CS">Discord</a> | <a href="../../releases">GitHub Releases</a> | <a href="../../issues">GitHub Issues</a>
</div>

> [!WARNING]
> **This mod is in active beta development.**
> **Back up your worlds before installing or updating!**
> Bugs, crashes, and balance/API changes are possible.
> Report issues at [GitHub Issues](../../issues)

***

## About

**Space-Modernized** is a modernized port of the well-known **NTM Space** addon for HBM's Nuclear Tech Mod, built exclusively for **[HBM-Modernized](https://github.com/Raptor324/HBM-Modernized)** on Minecraft 1.20.1 (Forge) and 1.21.1 (NeoForge).

Explore celestial bodies, build rockets and orbital stations, manage atmospheres and life support, launch satellites, and harness space-tier industrial machinery on a modern codebase and rendering pipeline.

This project is an independent port and is not an official release of HBM/NTM or original NTM Space.

***

## Source And Attribution

This project is a modernized port derived from **NTM Space** for Minecraft 1.7.10 and built on top of **[HBM-Modernized](https://github.com/Raptor324/HBM-Modernized)** (derived from **[Hbm's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT)**). The original NTM Space and HBM/NTM authors and contributors retain credit and copyright in the original code, assets, and gameplay design.

All modernization work — the 1.20.1/1.21.1 codebase, multi-loader platform layer, and rendering integration — is authored by **FuchsDev**, **RaptorDev**, and contributors (see [gradle.properties](gradle.properties)).

***

## 📄 License

This project is licensed under **[GPL-3.0-only](LICENSE)**.

- The `license` field in mod metadata states `GPL-3.0-only`.
- `LICENSE` is shipped inside release JARs under `META-INF/`.
- Distributing a compiled JAR requires making the complete corresponding source — including build scripts — available by a GPLv3-compliant method. The canonical source repository is this one.

***

## 🏗️ Building

The project uses a single shared source set with [Stonecutter](https://stonecutter.kikoz.dev/) preprocessing for multi-version support. Active targets: `1.20.1-forge` (JDK 17) and `1.21.1-neoforge` (JDK 21).

```bash
./gradlew :1.20.1-forge:reobfJar     # build the 1.20.1 Forge JAR (versions/1.20.1-forge/build/libs/)
./gradlew :1.21.1-neoforge:jar       # build the 1.21.1 NeoForge JAR (versions/1.21.1-neoforge/build/libs/)
./gradlew :1.20.1-forge:runClient    # launch a 1.20.1 Forge dev client
./gradlew :1.21.1-neoforge:runClient # launch a 1.21.1 NeoForge dev client
./gradlew :1.20.1-forge:runData      # run data generation (Forge 1.20.1 only)
```

Before committing, make sure the active Stonecutter project is set to `1.20.1-forge` (default in `stonecutter.gradle.kts`).

***

## 📦 Installation

1. Install **Forge 1.20.1** or **NeoForge 1.21.1**
2. Install **HBM-Modernized** (`hbm_m`) and **Architectury API** for your Minecraft version
3. Download the latest **Space-Modernized** `.jar` from [Releases](../../releases)
4. Place all `.jar` files in your `mods` folder

***

## 🤝 Contributing

Pull requests, suggestions, and bug reports are welcome. Fork the repository and propose improvements, or report issues at [Issues](../../issues) with detailed descriptions and logs.

***

## 💝 Acknowledgments

**Original NTM Space authors & contributors** — for creating the 1.7.10 space addon

**The Bobcat** — author of the original HBM's Nuclear Tech Mod

**FuchsDev**, **RaptorDev / Raptor324**, and contributors — modernization and port

The Forge, NeoForge, Architectury, Stonecutter, and Mojang teams for development tools
