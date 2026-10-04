# Daladez FPS 1.1
Fabric client mod for Minecraft 1.21.11. Watches your FPS while you play and lowers/raises
video settings automatically to hold your target (default 60). Tuned for old CPUs + integrated graphics.

- Press **K** in game to open the menu (change the key in Controls).
- **AI Brain**: learns which quality level hits your target in each situation (overworld/nether/end,
  outside/cave, rain) and jumps straight to it next time. Saved in config/daladez_fps_brain.json.
- No mixins: only changes vanilla video options, so it does not conflict with other mods.

Levels: 0 Balanced, 1 Low, 2 Potato, 3 Boom.
Commands: /daladezfps, /daladezfps on|off, /daladezfps level 0-3, /daladezfps target <fps>

Build locally: JDK 21 + Gradle 9.2+, then `gradle build` -> jar in build/libs/.
Build on GitHub: push this folder to a repo, Actions tab -> "build" -> download the artifact.
