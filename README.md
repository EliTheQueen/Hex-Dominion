# Hex Dominion

A turn-based hex-grid strategy game inspired by *Civilization VI*, built entirely with **Java Swing / Java2D**.

## How to run

### With Maven
```bash
mvn compile exec:java        # run straight from sources
mvn package                  # build target/hex-dominion-1.0.0.jar
java -jar target/hex-dominion-1.0.0.jar
```

### Without Maven (plain javac, Java 17)
```bash
BUILD_DIR=$(mktemp -d /tmp/hex-dominion-build.XXXXXX)
javac --release 17 -Xlint:unchecked -cp /path/to/gson-2.11.0.jar -d "$BUILD_DIR" $(rg --files src -g '*.java')
java -cp "$BUILD_DIR:/path/to/gson-2.11.0.jar" app.Main
```

Or simply open the project in IntelliJ IDEA and run `app.Main`.

> Requires Java 17 or newer.

## How to test

The tests are plain Java entry points and can run without Maven when Gson is on the classpath:

```bash
BUILD_DIR=$(mktemp -d /tmp/hex-dominion-tests.XXXXXX)
javac --release 17 -Xlint:unchecked -cp /path/to/gson-2.11.0.jar -d "$BUILD_DIR" $(rg --files src tests -g '*.java')
for test_source in tests/*Test.java; do
  test_class=$(basename "$test_source" .java)
  [ "$test_class" = "RealSwingRuntimeTest" ] || java -cp "$BUILD_DIR:/path/to/gson-2.11.0.jar" "$test_class" || exit 1
done
java -Djava.awt.headless=false -cp "$BUILD_DIR:/path/to/gson-2.11.0.jar" RealSwingRuntimeTest
```

`RealSwingRuntimeTest` launches the real `app.Main`, opens the gameplay dialogs, renders the map and combat UI, and fails on uncaught EDT/Timer errors. Run it in a graphical desktop session.

## Deterministic evaluation scenarios

Compile `src` and `tests` as above, then run every mandatory evaluation path:

```bash
java -cp "$BUILD_DIR" app.EvaluationScenarios
```

Or run one scenario by name: `revolt`, `mandatory-disasters`, `tribe-war`, `tribe-camp-defeat`, `mission-completion`, `combat`, or `save-load`.

```bash
java -cp "$BUILD_DIR" app.EvaluationScenarios combat
java -cp "$BUILD_DIR" PerformanceScenarioTest
java -cp "$BUILD_DIR" MapGenerationSafetyPropertyTest
```

The performance guard uses a deterministic 41×35 map for 250 turns. The map safety property checks 500 seeds.

The audit-to-implementation traceability record is in [`REQUIREMENT_MATRIX.md`](REQUIREMENT_MATRIX.md).

## Gameplay

You lead a young civilization on a fog-covered hex map. This is an open-ended **sandbox**:
there is no turn limit and no win condition — you explore, claim territory, build an economy
and research technologies for as long as you like. Your live score is always shown in the HUD.
The only way to lose is to let your whole empire (every unit and building) be wiped out.

You start with **1 Explorer, 2 Builders and 2 Workers**.

### Units
| Unit | Role |
|------|------|
| **Explorer** | Wide vision, fast scout; permanently reveals tiles. Can toggle *Auto-Explore* to head for the nearest fog frontier each turn. |
| **Builder** | Constructs buildings on adjacent territory. Has 3 charges; one is spent per build. |
| **Worker** | Stations inside a production building to make it yield resources; can transfer between buildings. |
| **Border Expander** | Claims its hex and all explored neighbours as territory, then is consumed. |

### Key mechanics
- **Town Hall command slot** — one recruitment, upgrade, or research command may run at a time; the HUD shows its remaining turns.
- **Town Hall base output** — +1 Food and +1 Wood every turn.
- **Finite resources** — each resource node has a remaining amount; working it depletes the node and the tile is redrawn as *empty*.
- **Starvation** — if Food goes negative the empire enters crisis: unit Action Points are halved until it recovers.
- **Building decay** — a building that misses upkeep for 3 turns falls into ruin and stops producing.
- **Unit cap** — starts at 6 and grows by 4 per Township (Townships require a resource-free hex).
- **Two-layer fog of war** — *explored* (permanent memory) plus *visible* (currently in sight); units and active buildings both grant vision.

### Buildings
Town Hall (start), Lumber Mill, Farm, Stable, Stone Mine, Iron Mine, Township.
Each production building yields resources per stationed worker, and has an upkeep cost.

### Resources
Food, Wood, Stone, Iron — capped by your storage capacity. Every unit eats 1 food per turn.

### Technologies
Legacy research contains Storage I/II, Stone Mining, Iron Mining, Professional Tools, and Township. Phase 2 research contains Sailing, Steel Tools, and Defensive Architecture. Both systems share the single Town Hall command slot and are labeled separately in the UI.

### Scoring
A running score = Territory ×5 + Buildings ×10 + Technologies ×15 + Explored hexes ×2 + Resources ÷10,
shown live in the HUD as a measure of how your empire is developing (the sandbox has no target to "win").

### Settings
A Settings dialog (from the main menu or the in-game gear button) offers a music-volume slider and a
mute toggle. Quitting the game always asks for confirmation first.

## Controls
- **Left-click** a unit to select it, then click a reachable hex to move (reachable hexes are tinted green; units walk there with a smooth animation, never teleporting).
- **Drag** to pan the map, **scroll** to zoom, **click the minimap** to jump the camera.
- **Hover** a hex for terrain, exact movement cost/prohibition, resource state, building/workers, and buildability.
- **Right-click** or **ESC** to deselect.
- Use the side panel for unit actions, and the top bar for *Recruit*, *Research* and *End Turn*.

## Architecture
- `model` — Swing-independent game logic (map, units, buildings, player, ordered turn phases, pathfinding, combat, tribes, disasters, and scoring).
- `controller` — `GameController` mediates between the views and the model.
- `view` — Swing/Java2D UI: animated menu, hex map renderer, HUD, side panel, dialogs and end screen.
- `app` — `Main` entry point.
