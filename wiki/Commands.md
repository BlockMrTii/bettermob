# Commands

Alias: `/bmob`. Every subcommand has its own permission node, all default to op:

| Permission | Allows |
|---|---|
| `bettermob.admin` | Everything below (it is the parent of all the others) |
| `bettermob.spawn` | `/bettermob spawn` |
| `bettermob.list` | `/bettermob list`, `/bettermob packs` and `/bettermob info` |
| `bettermob.reload` | `/bettermob reload` and `/bettermob validate` |
| `bettermob.skill` | `/bettermob skill` |
| `bettermob.give` | `/bettermob give` and `/bettermob egg` |
| `bettermob.killall` | `/bettermob killall` |
| `bettermob.spawner` | `/bettermob spawner` |
| `bettermob.debug` | `/bettermob debug` and `/bettermob stats` |

Without a node the subcommand is refused and left out of the help and out of tab completion. `bettermob.faction.<name>` is unrelated to commands, see [Mobs](Mobs#player-factions).

| Command | Description |
|---|---|
| `/bettermob spawn <id> [amount]` | Spawn a registered mob at your location |
| `/bettermob give <item> [player] [amount]` | Give a registered item. Without a player it goes to you |
| `/bettermob egg <mob> [player] [amount]` | Give a spawn egg item that spawns exactly that mob when used on a block, see [Spawn eggs](Mobs#spawn-eggs) |
| `/bettermob skill <id> [player]` | Run a skill by hand, with you (or the player) as caster |
| `/bettermob list` | List all registered mob IDs |
| `/bettermob packs` | List packs in `packs/` and whether they are enabled |
| `/bettermob info <mob>` | Show a mob's type, health, damage, faction, model, equipment, number of drop entries, skills by trigger and how many are alive |
| `/bettermob debug [off\|info\|verbose\|filter <id>\|filter clear\|chat]` | Show or change the debug output. Permission `bettermob.debug` (included in `bettermob.admin`) |
| `/bettermob stats [on\|off\|reset]` | Show living mobs per type, running timers, loaded skills and packs; `on`/`off` switch the skill timing, `reset` clears it. Permission `bettermob.debug` |
| `/bettermob killall [mob\|*] [world]` | Remove all living BetterMob mobs, or only one type and/or one world, and report how many |
| `/bettermob spawner create <id> <mob> [radius] [interval] [max]` | Create a spawner at your position; `remove <id>` and `list` manage them, see [Spawners](Spawners) |
| `/bettermob validate [pack]` | Check packs for unsupported lines without spawning anything, see [Packs](Packs#checking-a-pack) |
| `/bettermob reload` | Reload config, language files, mobs, skills, items and packs |

All IDs tab-complete. `/bettermob skill` is the quickest way to test a skill without
reproducing the trigger that normally fires it.

## Reload

`/bettermob reload` also updates mobs that are already alive. Each one is bound to the new definition of the same id:

- name, health cap, attack, speed and the options are applied again
- its timers are restarted (the old ones are cancelled, so nothing runs twice), its auras and global cooldown are cleared
- the model is attached again and the `~onLoad` skills run again
- AI goals are applied again only when `AIGoalSelectors` or `AITargetSelectors` changed and the new list starts with `clear`; goals an earlier `clear` removed cannot come back until the mob is respawned
- a mob whose definition was removed keeps the old one and a warning is logged
- totem bodies that are already in the world run out on their own

## Killall

`/bettermob killall [mob|*] [world]` removes every loaded living BetterMob mob. `<mob>` limits it to one id (`*` means all) and `<world>` to one world. Mobs in unloaded chunks are not touched. Without a mob id it also removes helper armor stands (the hit bodies of `totem` skills). Those carry the scoreboard tag `bettermob_helper`, and only entities with that tag are ever removed. Leftover helpers are also removed on startup and whenever a chunk loads.

## Stats

`/bettermob stats` shows the living BetterMob mobs per type, the running mob timers and the loaded skills and packs. With `Stats: on` in `config.yml` (or `/bettermob stats on`) it also measures every skill run and lists the 10 skills with the most total time (calls, average, maximum), plus the delayed steps that are still waiting.

- A run is measured up to its first `delay` and includes the skills it starts in the same tick.
- `StatsWarnMillis` (default 50, `0` turns it off) logs a console warning when a single run takes longer.
- While stats are off nothing is measured or counted.
- Delayed steps whose mob disappears before they run are not subtracted, so `reset` now and then.
- `reload` re-reads `Stats` and `StatsWarnMillis` from the config.

## Debug output

Set `Debug: off|info|verbose` in `config.yml` (default `off`), or change it while the server runs with `/bettermob debug`. `reload` resets it to the config value.

| Level | Logs |
|---|---|
| `info` | Every trigger that fires and whether its event was cancelled, every skill run and why it stopped (conditions, target conditions, cooldown, `castinstead`), AI goals that are missing together with the goals the mob type does have |
| `verbose` | Everything from `info`, plus every mechanic with its targeter, target count and parameters, `cancelskill`, failed conditions, BetterModel bone offsets, `shoot` and `totem` |

`/bettermob debug filter <id>` limits the output to a mob id, skill id or player name (mob ids, skill ids and online player names tab-complete; call it again to add more, `filter clear` removes them). `/bettermob debug chat` also sends the output to you in chat. While debug is off nothing is built or logged.
