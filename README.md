# Hex Dominion

A turn-based hex-grid strategy game inspired by *Civilization VI*, built entirely with **Java Swing / Java2D**.

## How to run

### With Maven
```bash
mvn compile exec:java        # run straight from sources
mvn package                  # build target/hex-dominion-1.0.0.jar
java -jar target/hex-dominion-1.0.0.jar
```

### Without Maven (plain javac)
```bash
javac -d out/production/"Hex Dominion" $(find src -name "*.java")
java -cp out/production/"Hex Dominion" app.Main
```

Or simply open the project in IntelliJ IDEA and run `app.Main`.

> Requires Java 11 or newer (developed and tested on Temurin 17 and 21).

## Gameplay

You lead a young civilization on a fog-covered hex map. Over **50 turns** you explore,
claim territory, build an economy and research technologies to maximise your final score.

You start with **1 Explorer, 2 Builders and 2 Workers**.

### Units
| Unit | Role |
|------|------|
| **Explorer** | Wide vision, fast scout; permanently reveals tiles. Can toggle *Auto-Explore* to head for the nearest fog frontier each turn. |
| **Builder** | Constructs buildings on adjacent territory. Has 3 charges; one is spent per build. |
| **Worker** | Stations inside a production building to make it yield resources; can transfer between buildings. |
| **Border Expander** | Claims its hex and all explored neighbours as territory, then is consumed. |

### Key mechanics
- **Production queue** — recruiting units and choosing research adds them to the Town Hall queue; they complete over several turns (the HUD shows the time remaining).
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
Storage I/II (capacity), Stone Mining, Iron Mining, Professional Tools (+50% production), Township.

### Scoring
Final score = Territory ×5 + Buildings ×10 + Technologies ×15 + Explored hexes ×2 + Resources ÷10.
Reach 300+ for a Victory.

## Controls
- **Left-click** a unit to select it, then click a reachable hex to move (reachable hexes are tinted green; units walk there with a smooth animation, never teleporting).
- **Drag** to pan the map, **scroll** to zoom, **click the minimap** to jump the camera.
- **Hover** a hex for a tooltip (terrain, resource & remaining amount, building, workers).
- **Right-click** or **ESC** to deselect.
- Use the side panel for unit actions, and the top bar for *Recruit*, *Research* and *End Turn*.

## Architecture
- `model` — pure game logic (map, units, buildings, player, turn engine, pathfinding, scoring).
- `controller` — `GameController` mediates between the views and the model.
- `view` — Swing/Java2D UI: animated menu, hex map renderer, HUD, side panel, dialogs and end screen.
- `app` — `Main` entry point.
