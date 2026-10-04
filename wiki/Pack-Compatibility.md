# Pack compatibility

BetterMob reads MythicMobs-style packs, but only a subset of MythicMobs. This page lists the packs the
maintainers have tried, how far they run, and how to check your own.

## Check your pack first

1. Copy the pack folder into `plugins/BetterMob/packs/`.
2. Run `/bettermob validate <pack>` (see [Packs](Packs#checking-a-pack)). It lists unsupported mechanics, conditions, targeters, undefined skills and unparsable lines, with file and line, before you spawn anything.
3. Read the console after `/bettermob reload` for warnings about missing models and AI goals (those depend on the mob type and are not part of `validate`).
4. Test the skills one by one with `/bettermob skill <id>`; `/bettermob debug verbose` shows why a skill stopped.

## Packs we have tried

| Pack | Status | Notes |
|---|---|---|
| **1Bit Elite Pillager** (MythicMobs pack, BetterModel) | Played in-game | Crossbow shots with on-hit effects, the bomb totem at the model's `tnt2` bone, the spawn aura (stun and velocity), drops, baby and pet variants. Arrow speed and damage are estimates of MythicMobs' values (`speedscale` in `shoot` adjusts them). |
| **Wumpus** (`nm_wumpus`, shipped as an example) | Played in-game | BetterModel model, `model`, `sound`, `randomskill`, `~onSpawn`, `~onLoad`, `~onDamaged`, `~onDeath`, `~onInteract`. |
| **Bison and baby alligator** | Played in-game | AI goals with priorities and aliases (`rangedAttack`, `players`, `attacker`), threat, drop tables with `1to2` amounts. |
| **Trading cards** (NorthSoft trading cards pack) | Lines read, reported fixed in 1.1.1 | `_parse` / `_activate` skills with nested braces, `takeitem`, `spin`, `~onUse` item skills. Commands such as `/nstc givecard` need the plugin that owns them. A cut-down copy is in `src/test/resources/packs/trading_cards`. |
| **Cubees** (ModelEngine) | Reported working | Attach the model with `modelengine{mid=...}`; ModelEngine mobs get no BetterModel features (`state`, seats, `@ModelPart`, `bodyrotation`). |
| **Mustang and kart packs** | `mountmodel` supported, lines read | Rideable seats work through BetterModel only (WASD control on the seat bone). Nested braces in the kart skills parse since 1.1.1. |

"Played in-game" means a maintainer ran the pack on a server. "Lines read" means the pack's lines parse and
validate, but it was not re-tested in-game for the current version. If you find a difference, please open a
[pack compatibility issue](https://github.com/HyperGaming99/bettermob/issues/new/choose).

## What is checked automatically

Every pack under `src/test/resources/packs/` is parsed on every pull request and then validated against the registered mechanics, conditions and targeters; a pack that uses something BetterMob does not have fails the build. The folder holds a cut-down trading cards pack and `feature_showcase`, a small pack of our own that uses the newer mechanics (`totem`, `shoot` with `oh`, auras, inline skills, `@ModelPart`, targeter `Conditions`, drop tables). Add a pack there (use the usual `Mobs/`, `Skills/`, `Items/` and `DropTables/` folder names) when you fix something for it, and only content you may share.

## Known gaps

- Variables, factions as a MythicMobs system and several MythicMobs mechanics and conditions are not implemented, they are reported by `/bettermob validate`. See [Skills](Skills) for what exists.
- AI goals are matched against the goals the mob type really has; names without a match are skipped with a warning.
- Things a pack gets from other plugins (commands, items, models) need those plugins.
