# Hex Dominion

A turn-based hex-grid strategy game inspired by *Civilization VI*, built entirely with **Java Swing / Java2D**.

## How to run

From the project root:

```bash
# Compile
javac -d out/production/"Hex Dominion" $(find src -name "*.java")

# Run
java -cp out/production/"Hex Dominion" app.Main
```

Or simply open the project in IntelliJ IDEA and run `app.Main`.

> Requires Java 11 or newer (developed and tested on Java 21).

## Gameplay

You lead a young civilization on a fog-covered hex map. Over **50 turns** you explore,
claim territory, build an economy and research technologies to maximise your final score.

### Units
| Unit | Role |
|------|------|
| **Explorer** | Wide vision (3), fast scout. Can toggle *Auto-Explore* to reveal the map automatically each turn. |
| **Builder** | Constructs buildings. Has 3 charges; spent when it builds. |
| **Worker** | Stations inside a production building to make it yield resources. |
| **Border Expander** | Claims the surrounding hexes as your territory. |

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
- **Left-click** a unit to select it, then click a reachable hex to move (reachable hexes are tinted green).
- **Drag** to pan the map, **scroll** to zoom.
- **Right-click** or **ESC** to deselect.
- Use the side panel for unit actions, and the top bar for *Recruit*, *Research* and *End Turn*.

## Architecture
- `model` — pure game logic (map, units, buildings, player, turn engine, pathfinding, scoring).
- `controller` — `GameController` mediates between the views and the model.
- `view` — Swing/Java2D UI: animated menu, hex map renderer, HUD, side panel, dialogs and end screen.
- `app` — `Main` entry point.
