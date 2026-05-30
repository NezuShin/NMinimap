# AGENTS — How to work on NMinimap (for AI coding agents)

Checklist
- Read plugin entrypoint `src/main/java/su/nezushin/nminimap/NMinimap.java` to understand lifecycle (onEnable/load/unload).
- Use `SchedulerUtil` for scheduling (Folia vs Spigot abstraction) and `NMinimap.async/sync` helpers for thread boundaries.
- Verify external dependencies (AnvilORM, Packetevents, PassengerAPI, PlaceholderAPI) before running locally.
- Build with the included Gradle wrapper and run a dev server via the run-paper plugin.

Quick commands
- Build jar: `./gradlew build` (Windows PowerShell: `./gradlew.bat build`).
- Run local server for iterative dev: `./gradlew runServer` (configured in `build.gradle` — uses run-paper plugin, minecraftVersion 1.21).

Big-picture architecture (short)
- Single Bukkit/JavaPlugin entrypoint: `NMinimap` wires managers and registers listeners/commands.
  - Managers: `PacketManager`, `ChunkManager`, `MarkerImageManager`, `DatabaseManager`, `ModCompatibilityManager`, `UpdateCheckerManager`.
- Responsibilities:
  - `ChunkManager` — tile caching, render queue and eviction (in-memory + on-disk cache under `chunks/cache`).
  - `PacketManager` — per-player entity & map handling, interfaces with PacketEvents/PassengerAPI to spawn/update invisible frames/entities.
  - `MarkerImageManager` — builds resourcepack textures and font images from `/resources/defaults/markers` and config; used by `PacketManager` and markers.
  - `DatabaseManager` — AnvilORM-backed player table (MySQL/SQLite depending on `config.yml`).

Key integration points and gotchas
- Mandatory soft-dependencies declared in `src/main/resources/plugin.yml`: packetevents, PassengerAPI, AnvilORM. Development build currently references a local AnvilORM jar in `build.gradle` (line with absolute path). Provide AnvilORM (or adjust dependency) when building.
- Use `SchedulerUtil.getScheduler()` (see `src/main/java/.../util/SchedulerUtil.java`) — it handles Folia vs Spigot differences. Do not call Bukkit scheduler directly in long-running work.
- Threading conventions:
  - Short main-thread work via `NMinimap.sync(Runnable)` or `SchedulerUtil.sync(...)`.
  - Heavy/IO work via `NMinimap.async(...)` (creates `NMinimapThread`) or `SchedulerUtil.async(...)` for scheduled periodic tasks.
- Resourcepack and assets: `MarkerImageManager` builds and copies files under `src/main/resources/defaults/*` — changes here may require rebuilding and restarting the server (resourcepack generation is runtime).
- Reload semantics: admin reload is supported by `/minimap admin reload` (see `MinimapCommand`) which calls `NMinimap.unload()` and `NMinimap.load()` — expect full reinitialization of managers and scheduled tasks.

Patterns and examples (copyable)
- Registering listeners and commands (entrypoint):
  - `getCommand("minimap").setExecutor(new MinimapCommand());`
  - `Bukkit.getPluginManager().registerEvents(new MarkerListener(), getInstance());`
- Scheduling periodic map pushes to players (from `NMinimap.load`):
  - `SchedulerUtil.getScheduler().async(() -> playersWithMap.forEach(NMapPlayer::sendMap), 1, Config.mapRenderInterval);`
- Async DB-backed player load (from `NMinimap.loadPlayer`):
  - `NMinimap.async(() -> { var player = getDatabaseManager().getPlayersTable().query().where("id", uuid).completeAsOne(); ... });`
- Command-run async pattern (from `MinimapCommand.onCommand`):
  - `NMinimap.async(() -> { /* permission checks, heavy commands (reload/stats) */ });`

Files you should read first
- `src/main/java/su/nezushin/nminimap/NMinimap.java` — lifecycle and wiring
- `src/main/java/su/nezushin/nminimap/packets/PacketManager.java` — entity/map update flow
- `src/main/java/su/nezushin/nminimap/chunks/ChunkManager.java` — caching & render queue
- `src/main/java/su/nezushin/nminimap/resourcepack/MarkerImageManager.java` — resourcepack and marker image generation
- `src/main/resources/config.yml` and `plugin.yml` — configuration and runtime flags
- `build.gradle` — Java target (21), run-server config, and a local AnvilORM jar reference

Developer notes for agents
- Prefer small, focused code changes and ensure you run `./gradlew build` and load the plugin into a local test server via `runServer` to validate runtime behavior.
- When changing scheduling or threading code, verify both Spigot (Paper) and Folia flows — `SchedulerUtil` has distinct implementations.
- External dependency handling: if you modify `build.gradle`, replace the absolute local path reference to AnvilORM with a publicly resolvable dependency or add instructions to put AnvilORM.jar into a known location.
- Look at `readme.md` for API usage examples (marker creation and runtime API) to craft tests or example plugins that exercise cross-component behavior.

Where to make quick, safe changes
- UI strings and messages: `src/main/resources/defaults/lang/*.yml` and `util/config/Message` classes.
- Configuration defaults: `src/main/resources/config.yml` + `util/config/Config` class for runtime validation.

If you need to run or debug locally
- Build the plugin: `./gradlew.bat build` (Windows Powershell). Resulting jar: `build/libs/`.
- Start a dev server: `./gradlew.bat runServer` (uses run-paper plugin and configured minecraftVersion 1.21). Drop the built jar into the server's `plugins/` for manual tests.

Contact points in code (quick map)
- Commands: `src/main/java/su/nezushin/nminimap/command/MinimapCommand.java`
- Events: `listeners/*` (PlayerListener, MarkerListener, ChunkListener, BlockListener)
- Scheduling abstraction: `util/SchedulerUtil.java`
- Database: `database/DatabaseManager.java`

Limitations of this doc
- This file documents discoverable, implemented patterns only (runtime behavior and conventions seen in code). It does not prescribe style or add missing tests.

---
Generated by scanning repository files: main plugin class, managers, listeners, build configuration and resource files.

