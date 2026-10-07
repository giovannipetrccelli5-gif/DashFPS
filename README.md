# DashFPS / FPS Plus

A Fabric client mod for Minecraft 26.3.

## Features

- Raises the client render-distance option cap from 32 to 500.
- F8 toggles FPS Mode:
  - clouds off
  - particles minimal
  - entity shadows off
  - render distance 8
  - restores your previous settings when toggled off
- Client command: `/fpsplus renderdistance <2-500>`

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Fabric API 0.161.0+26.3
- Java 25

## Build

```text
gradle build
```

The mod jar is written to `build/libs/fpsplus-1.0.0.jar`.
