# Phase 2 Remediation Requirement Matrix

This matrix is the post-remediation counterpart to the independent gap analysis dated 2026-08-19. The audit's already-green requirements remain green; the rows below trace every P1/P2 remediation category from its old finding to its verified implementation path.

Status vocabulary:

- **Complete → Complete**: the audit already found the requirement complete and regression coverage still passes.
- **Missing/Incorrect/Partial → Complete**: the audited gap is implemented in real gameplay and covered by the named verification.
- **Unverified → Verified**: the implementation or repository property now has direct evidence.

| Block | Audit requirements and old status | New status | Authoritative implementation and verification |
| --- | --- | --- | --- |
| 1 — State/destruction | TH-09, TH-13 partial; TH-14 risky; destroyed-building cleanup across combat/disasters missing | **Complete** | `Player.destroyBuilding`, Town Hall domain projection, and live `ResourceStorage` capacity are authoritative. `AuthoritativeStateTest`, `BuildingDestructionLifecycleTest`, and `TownHallStorageIntegrationTest`. Commit `1667f02`. |
| 2 — Combat | MIL-02 incorrect; MIL-05/06 partial; COM-01/04/07/20/21 missing; COM-02/05/06/09/16/19/22/24/25 incorrect; COM-08/17 partial | **Complete** | `CombatService` is used by generic, structure, barbarian, bear, and tribe gameplay paths; participants, range, dice-by-type, wall values, damage ordering, AP, capture, cleanup, and aggregate structure damage use one ruleset. `CombatRulesTest`, `CombatIntegrationTest`, `ControllerGameFlowTest`. Commit `9667d28`. |
| 3 — Infrastructure | MAP-09 partial; wall resource/atomicity rules incorrect; demolition UI/lifecycle and Monument terrain incomplete | **Complete** | `InfrastructureService` owns wall/road/demolition validation and mutation; controller/UI use it with confirmation and exact reasons. `InfrastructureRulesTest`, `BuildingDestructionLifecycleTest`, `RealSwingRuntimeTest`. Commit `c2328f3`. |
| 4 — Happiness | HAP-05 and HAP-12 incorrect | **Complete** | Town Hall garrison is a current empire condition, not a farmable historical event; Revolt affects only military units and Workers. `HappinessIntegrationTest`. Commit `87aa939`. |
| 5 — Missions | Exact Farmer/Merchant/Warrior/Mountain/Coastal definitions, reward types, radius, deadline, and five-turn failure cooldown incorrect/partial (MIS family) | **Complete** | Typed objectives and rewards implement all five specification definitions and storage-aware turn-in. `TribeMissionSpecificationTest`, `TribeIntegrationTest`. Commit `5a16c4e`. |
| 6 — Diplomacy | Gift atomicity, pending Peace, attack/failure tracking, Alliance lifecycle, and permanent benefits missing/partial (TRI family) | **Complete** | Diplomacy transitions validate before spending, Peace remains pending until eligible, alliances auto-break below 70, and type-specific benefits attach/detach with UI descriptions. `TribeDiplomacyLifecycleTest`, `TribeIntegrationTest`, `TradeRulesTest`. Commit `1c3645c`. |
| 7 — Tribe defense/AI | COM-22 incorrect; TAI-01/03–06/09 missing or partial; TAI-08 incorrect; notifications partial | **Complete** | Tribes have positioned typed units with AP/range; terrain-aware defense and attacks use the common combat engine; warning/degradation/offers/spawn caps dispatch once per turn. `TribeAiBehaviorTest`, `CombatRulesTest`. Commit `d30a188`. |
| 8 — Tribe UI/visibility | Undiscovered/current-visibility masking, camp-click interaction, Gift/Trade controls, War confirmation, Peace progress, benefit/reward/progress details, mission HUD, and disabled reasons missing/partial | **Complete** | Map, HUD, and interaction panels project only permitted tribe state and route every action through controller availability. `TribeVisibilityTest`, `GlobalUiAvailabilityTest`, `RealSwingRuntimeTest`. Commit `e61c89f`. |
| 9 — Disasters | DIS-02 incorrect; DIS-07–11 cleanup/infrastructure effects partial | **Complete** | `TurnCoordinator` evaluates disaster occurrence at beginning-of-turn against the deterministic season state; implemented disaster types share destruction and infrastructure cleanup and preserve timed blocking. `DisasterPipelineTest`, `DisasterRulesTest`, `BuildingDestructionLifecycleTest`. Commit `4063bb6`. |
| 10 — Bear | BEAR-06 incorrect; BEAR-07/08 partial | **Complete** | Bear retains the conservative mandatory stats/targeting interpretation, uses common combat, tracks cooldown per area, returns to its den, cleans up, and emits animation state. `BearFinalizationTest`, `CombatPresentationTest`. Commit `91dc8d3`. |
| 11 — Save/load | SAVE-04/05/10/16 missing; SAVE-07/08/11–13/18 partial; SAVE-19 unverified | **Complete** | Versioned envelopes, stable-phase availability, atomic autosave preservation/reporting, Save/Continue/Cancel load flow, migration, deep validation, reference repair, derived-state recomputation, and serialized RNG continuation form the save contract. `SaveContractTest`, `SaveManagerTest`. Commit `bd074e6`. |
| 12 — Presentation | COM-14 partial; four military map visuals and detailed combat presentation incomplete | **Complete** | Distinct unit glyphs and a staged overlay show labeled dice pairs, wall modifier, casualties, structure damage, and capture. `CombatPresentationTest`, `VisualSmokeTest`, `RealSwingRuntimeTest`. Commit `cc906c6`. |
| 13 — UI availability | MAP-15, ARCH-02, UX-01/02 partial | **Complete** | `ActionAvailability` centralizes enabled/reason results for movement, construction, recruitment, research, Town Hall commands, infrastructure, trade, missions, and save. Tooltips expose movement cost/prohibition/buildability. `GlobalUiAvailabilityTest`, `RealSwingRuntimeTest`. Commit `3d5f95b`. |
| 14 — Verification | Required combat, state, mission, diplomacy, AI, visibility, disaster, save/RNG, map-generation, availability, Swing cleanup, and performance coverage missing; MAP-16/PERF-01 unverified | **Complete / Verified** | Deterministic Java entry-point tests cover every listed gap; map safety runs 500 seeds; performance runs a 41×35 map for 250 turns; Swing timers stop on disposal. Full suite includes `MapGenerationSafetyPropertyTest`, `PerformanceScenarioTest`, `VisualSmokeTest`, and `RealSwingRuntimeTest`. Commit `a51dc4c`. |
| 15 — Architecture | ARCH-03/04 partial; dead/duplicate combat, military, production queue, technology queue, and turn paths | **Complete** | Dead `MilitaryService` is removed; legacy serialized queue shells are non-gameplay compatibility types; `SingleCommandSlot` and `TurnCoordinator` are authoritative; model has no Swing/AWT dependency. `TurnCoordinatorTest` plus full-suite compilation. Commit `4c82237`. |
| 16 — Repository/evaluation | GIT-03, PERF-01, DEMO-01 unverified; GIT-04 incorrect | **Verified / Complete** | `.idea/`, `*.iml`, and `saves/` are ignored; tracked IDE metadata is removed without deleting local files. GitHub privacy was verified through the authenticated repository API as `private` on 2026-08-19. `EvaluationScenariosTest` covers revolt, mandatory disasters, tribe war, camp defeat, mission completion, combat, and save/load. Build/run/test instructions are in `README.md`. |

## Final verification snapshot

On 2026-08-19, the complete source and test tree compiled with `javac --release 17 -Xlint:unchecked` and produced no compiler stderr. All 30 headless tests passed with zero stderr. `RealSwingRuntimeTest` launched the real application, advanced a turn, moved an Explorer, and opened the Recruit, Technology, Save/Load, Combat, and Tribe interfaces with `uncaught=0` and zero stderr.

The deterministic performance profile completed a 41×35 map and 250 turns in 83 ms inside the guard (0.14 seconds wall time, 46,613,320-byte peak memory footprint on the evaluation machine). `MapGenerationSafetyPropertyTest` passed all 500 seeds.

## Regression-preserved requirement families

| Audit family | Old status | New status and evidence |
| --- | --- | --- |
| Town Hall baseline TH-01–08, TH-10–12 and technology TECH-01–08 | **Complete** | **Complete → Complete** — `PhaseTwoTownHallTest`, `TownHallStorageIntegrationTest`, `MovementPolicyTest`. |
| Trade TR-01–15 | **Complete** | **Complete → Complete** — `TradeRulesTest` plus controller availability coverage. |
| Adjacency ADJ-01–03 and unaffected Happiness rules | **Complete** | **Complete → Complete** — `SeasonAndAdjacencyTest`, `HappinessIntegrationTest`. |
| Terrain/map baseline MAP-01–14 | **Complete/mostly complete** | **Complete** — `MovementPolicyTest`, `InfrastructureRulesTest`, and the 500-seed safety property. |
| Seasons SEA-01–09 | **Complete** | **Complete → Complete** — `SeasonAndAdjacencyTest`, `RealSwingRuntimeTest`. |
| Core disaster rules DIS-01 and DIS-03–06 | **Complete** | **Complete → Complete** — `DisasterPipelineTest`, `DisasterRulesTest`, visual runtime coverage. |
| Save baseline SAVE-01–03, SAVE-06, SAVE-09, SAVE-14/15/17 | **Complete** | **Complete → Complete** — `SaveManagerTest`, `SaveContractTest`. |
| Git history GIT-01/02 and Swing-independent model ARCH-01 | **Complete/mostly complete** | **Complete → Complete** — sixteen ordered remediation commits and source dependency scan/full compile. |

## Scope boundary

The completion result covers every P1 and P2 item in the audit. Audit items explicitly labeled P3/bonus—full naval units/pirates, map editor, Mountain Range line-of-sight, and optional design-pattern additions—are not part of this result. Cleanup for the already-present Tsunami, Volcano, Tornado, and Avalanche paths was nevertheless completed because Block 9 explicitly required it. The Sea Storm coastal subset remains bonus-only because the full effect depends on the optional naval system.
