# Motion Cues

Motion Cues is a client-side Minecraft mod that adds camera-relative 3D motion cues for player movement and camera rotation. It is inspired by the visual vehicle motion cues found on mobile devices, extended for walking, sprinting, falling, flying, riding vehicles, and freely rotating the camera.

The dots move from acceleration projected onto the plane perpendicular to the camera. Acceleration along the view ray changes their apparent size, providing a forward/backward depth cue.

## Features

- Camera-relative cues for all three movement dimensions
- Forward/backward depth response through dot scaling
- Optional cues while standing still and rotating the camera
- Continuous staggered dot columns that enter and leave the screen smoothly
- Adjustable density, size, spacing, edge margin, coverage, opacity, sensitivity, smoothing, and idle fade delay
- Adaptive light/dark contrast based on the local scene, mathematical difference blending, or a fixed color
- Live preview in the Cloth Config screen with a standalone preview checkbox
- In-game hot reload when the configuration file changes
- Key bindings for toggling cues and opening the settings screen
- English, Simplified Chinese, Traditional Chinese, and Japanese translations
- Fabric and NeoForge support

## Requirements

- [Cloth Config](https://modrinth.com/mod/cloth-config) is required.
- Fabric builds also require [Fabric API](https://modrinth.com/mod/fabric-api).
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional on Fabric and provides a convenient settings button.

The mod is client-side only. It does not need to be installed on a server.

## Supported versions

Each JAR targets one exact Minecraft version and one loader:

| Minecraft | Java | Fabric | NeoForge |
| --- | ---: | :---: | :---: |
| 1.21.1 | 21 | Yes | Yes |
| 1.21.3 | 21 | Yes | Yes |
| 1.21.4 | 21 | Yes | Yes |
| 1.21.5 | 21 | Yes | Yes |
| 1.21.8 | 21 | Yes | Yes |
| 1.21.10 | 21 | Yes | Yes |
| 1.21.11 | 21 | Yes | Yes |
| 26.1.2 | 25 | Yes | Yes |
| 26.2 | 25 | Yes | Yes |

Do not use a JAR for a different Minecraft version, even when the version numbers are close.

## Configuration

Open the settings screen through Mod Menu, NeoForge's Mods screen, or the configurable “Open Motion Cues Settings” key binding. Both key bindings are unassigned by default and can be configured in Minecraft's Controls screen.

The configuration is stored at `config/motion_cues.json`. External edits are reloaded automatically while the game is running. Saving the Cloth Config screen applies changes immediately.

The three visibility modes are:

- **Motion Only:** fade after movement stops; the idle delay is configurable from 0 to 60 seconds.
- **Always in Gameplay:** keep dots visible during gameplay, but not over other screens.
- **Always:** keep dots visible during gameplay and supported screens.

## Building

This project uses [mise](https://mise.jdx.dev/) to select Java and Gradle. Install the declared tools first:

```bash
mise install
```

Build the primary Minecraft 1.21.1 Fabric and NeoForge artifacts:

```bash
mise exec -- ./gradlew buildAll
```

Build an individual 1.21.x target:

```bash
mise exec java@zulu-21 vfox:mise-plugins/vfox-gradle@9.5.1 -- \
  gradle -p ports/legacy-fabric exportJar -PmcVersion=1.21.11

mise exec java@zulu-21 vfox:mise-plugins/vfox-gradle@9.5.1 -- \
  gradle -p ports/legacy-neoforge exportJar -PmcVersion=1.21.11
```

Build a 26.x target with Java 25:

```bash
mise exec java@zulu-25 vfox:mise-plugins/vfox-gradle@9.5.1 -- \
  gradle -p ports/modern-fabric exportJar -PmcVersion=26.2

mise exec java@zulu-25 vfox:mise-plugins/vfox-gradle@9.5.1 -- \
  gradle -p ports/modern-neoforge exportJar -PmcVersion=26.2
```

Exported JARs are written to `dist/`.

## Project layout

- `common/` contains loader-independent behavior, configuration, UI, translations, the icon, and the 1.21.1 shader implementation.
- `ports/shared/` contains renderer and key-binding variants shared by groups of Minecraft versions.
- `ports/legacy-fabric/` and `ports/legacy-neoforge/` build Minecraft 1.21.1 through 1.21.11.
- `ports/modern-fabric/` and `ports/modern-neoforge/` build Minecraft 26.1.2 and 26.2.
- `ports/targets.gradle` is the central version/dependency matrix.

The build scripts select the appropriate renderer source set and apply small API-name transformations for each target. This keeps the motion model, configuration, and dot-field behavior in one common implementation while isolating Minecraft's rendering API changes.

## License

This project is available under the [MIT License](LICENSE).
