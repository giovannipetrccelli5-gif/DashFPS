# DashFPS

DashFPS is a client-side Fabric performance orchestrator for Minecraft Java 26.3. It is designed to run beside Sodium, Lithium, FerriteCore, Entity Culling, MoreCulling, ImmediatelyFast, Krypton, C2ME, ScalableLux, ModernFix, and BadOptimizations instead of reimplementing their optimization domains.

## Version baseline

- Minecraft Java: 26.3 (Wilderness Bound)
- Fabric Loader: 0.19.5
- Fabric API: 0.162.0+26.3
- Fabric Loom: 1.17.21
- Gradle: 9.6.0
- Java: 25

Minecraft 26.3 requires Java 25. Minecraft 26.1+ is unobfuscated, so DashFPS intentionally has no Yarn mappings dependency.

## Conflict policy

DashFPS does not implement chunk rendering, chunk build threading, entity rendering culling, occlusion/frustum culling, lighting-engine changes, networking optimization, texture-atlas caching, general game-logic optimization, or memory optimization.

The only DashFPS mixin is SoundEngineMixin. Its injection uses require = 0. Sodium, Lithium, Entity Culling, MoreCulling, and ImmediatelyFast were checked for SoundEngine use before adding it.

Client-side block-entity/entity tick reduction is intentionally not injected. Current Lithium touches Level.tickBlockEntities and ClientLevel, and Sodium also mixes into ClientLevel, so adding those hooks would violate DashFPS's conflict policy and risk multiplayer desync.

## Features

- Startup optimization-mod detection and /dashfps mods delegation report.
- F8 universal FPS Mode with in-session exact vanilla restore and best-effort reflection adapters.
- Adaptive 8-step quality scaler: clouds, particles, shadows, render distance, simulation distance, smooth lighting, render distance again, biome blend.
- Quality steps down below 30 FPS and restores upward after staying above 75 FPS for 10 seconds.
- F9 HUD: current FPS, 1% low, 0.1% low over the last 60 seconds, 60-sample frametime graph, view distance, active optimization count, detected mods.
- F10 cycles potato, performance, balanced, quality, max.
- PojavLauncher detection and first-launch mobile defaults.
- F11 cycles frame pacing off, smooth (60 FPS with VSync off), and max (vanilla unlimited cap with VSync off).
- Distant positional sound suppression beyond 24 blocks while FPS Mode is active.
- CrashGuard disables only the failing DashFPS feature and writes .minecraft/logs/dashfps.log.
- Config file: .minecraft/config/dashfps.json.

## Commands

- /dashfps
- /dashfps mods
- /dashfps preset <potato|performance|balanced|quality|max>
- /dashfps hud position <top_left|top_right|bottom_left|bottom_right>
- /dashfps hud toggle
- /dashfps pacing <off|smooth|max>
- /dashfps mobile optimize
- /dashfps config reload
- /dashfps config reset
- /dashfps config save

## Keys

- F8 — FPS Mode
- F9 — HUD
- F10 — cycle preset
- F11 — cycle frame pacing

All keys appear under the DashFPS controls category.

## Build

Run: gradle --no-daemon clean build

Output: build/libs/DashFPS-3.0.0.jar
