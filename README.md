# Spire

**Slay the Spire in MITE** — a roguelike dungeon-crawling mod for Minecraft 1.6.4 MITE.

Climb the Spire, collect relics, forge weapon affixes, and conquer three floors of combat, events, shops, and treasures.

## Features

- **Floor Map System** — procedurally generated multi-branch floor maps with combat, event, shop, rest, treasure, and forge nodes
- **Relic System** — 10+ relics with unique effects (ON_HIT, ON_KILL, HEAL_ON_KILL, STAT_MODIFIER) and rarity tiers
- **Weapon Affix System** — forge weapon affixes via the Forge room; affixes persist on items across runs
- **Combat System** — spawn enemies from templates with random affix modifiers; combat state tracked per player
- **Custom Dimension** — void + stone arena platform for spire runs; seamless teleport in/out
- **GUI Screens** — FloorMap, Combat, Event, Shop, Rest, Treasure, Forge, Reward, Victory
- **Server-Authoritative** — all state managed server-side; clients sync via packets

## Requirements

- Minecraft 1.6.4 MITE
- FishModLoader >= 3.4.0
- RustedIronCore >= 1.5.0
- Java >= 17

## Setup

For MITE mod development setup, see the [FishModLoader documentation](https://minecraftistooeasy.github.io/pages/docs-navigation.html).

```bash
git clone https://github.com/limingzxc/spire.git
cd spire
./gradlew build
```

The compiled jar will be at `build/libs/spire-1.0.0.jar`.

## License

GPL-3.0 — see [LICENSE](LICENSE).