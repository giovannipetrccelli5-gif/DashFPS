# DashFPS

DashFPS is a client-side Fabric optimization mod for Minecraft 26.3.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Fabric API 0.161.0+26.3
- Java 25
- Fabric Loom 1.17 / Gradle 9.6.0 for building

## Features

- Raises the client render-distance option cap to 500.
- F8 toggles FPS Mode.
- FPS Mode disables clouds, uses minimal particles, disables entity shadows, and lowers render distance.
- Aggressive block-entity culling beyond 16 blocks while FPS Mode is enabled.
- Item-frame maps, beacon rendering, and selected mob-spawn particles are skipped while FPS Mode is enabled.
- Smart entity FOV culling for entities farther than 32 blocks.
- Optional chunk FOV filtering.
- Dynamic render distance:
  - checks FPS every 5 seconds
  - below the configured minimum, lowers render distance by 2 to a minimum of 4
  - above the configured maximum for about 10 seconds, raises render distance by 2 up to the configured maximum
  - shows changes in the action bar
- Persistent JSON configuration at `.minecraft/config/dashfps.json`.

## Commands

- `/dashfps renderdistance <2-500>`
- `/dashfps config reload`
- `/dashfps config reset`

## Config

Default `config/dashfps.json` values:

```json
{
  "fpsModeOn": false,
  "dynamicRenderDistance": true,
  "maxViewDistance": 32,
  "minFps": 45,
  "maxFps": 90,
  "fovCullAngle": 90.0
}
```

## Minecraft 26.3 rendering notes

Minecraft 26.3 already compiles render pipelines during resource reload and keeps a persistent light-map texture. DashFPS does not duplicate those engine systems. For additional light-map update suppression, BadOptimizations can be used alongside DashFPS.

All DashFPS mixin injections use optional injection requirements so a changed optional render hook does not hard-fail the mod.

## Build

```text
gradle build
```

The release JAR is `build/libs/DashFPS-2.0.0.jar`.

GitHub Actions builds on every push to `main` and publishes the successful JAR to the v2.0.0 GitHub Release.
