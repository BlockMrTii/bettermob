# Spawners

A spawner spawns one of your mobs at a random ground spot around a position, again and again, while players are close.

## Commands

| Command | Description |
|---|---|
| `/bettermob spawner create <id> <mob> [radius] [interval] [max]` | Create a spawner at your position |
| `/bettermob spawner remove <id>` | Remove a spawner |
| `/bettermob spawner list` | List all spawners |

Permission: `bettermob.spawner` (default: op, part of `bettermob.admin`). All ids tab-complete.

## Example

```
/bettermob spawner create camp goblin 6 20 4
```

Makes a spawner named `camp` at your position. Every 20 seconds it spawns `goblin` at a random ground spot within 6 blocks, as long as a player is within 32 blocks, and it never lets more than 4 of its loaded mobs live at once.

Defaults when you leave the numbers out: radius 5, interval 30 seconds, max 3.

## spawners.yml

Spawners are saved in `plugins/BetterMob/spawners.yml` and reloaded with `/bettermob reload`. Each entry has `mob`, `world`, `x`, `y`, `z`, `radius`, `interval` (seconds), `max` and `player-range` (blocks, default 32). You can edit the file and reload instead of using the command. An entry without a mob or a world is skipped with a warning.

## How mobs are counted

Spawned mobs carry the scoreboard tag `bettermob_spawner:<id>`. That is how a spawner counts its mobs again after a restart or when their chunk loads, without scanning the area around it. On Folia the counting works through events, not area scans.

## Natural spawns instead

If you want a mob to replace vanilla spawns everywhere (by biome, world, time and chance) use the `Spawn` block of the mob file, see [Natural spawns](Mobs#natural-spawns).
