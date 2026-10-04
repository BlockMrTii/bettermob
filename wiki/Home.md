# BetterMob

BetterMob is a Paper plugin for registering custom mobs in YAML and giving them 3D models
through [BetterModel](https://modrinth.com/plugin/bettermodel) or ModelEngine. The file
format follows MythicMobs, but BetterMob only implements a **subset** of it. Anything
unsupported is logged as a warning and skipped, it never breaks the rest of a skill.

## Start here

| Page | What's in it |
|---|---|
| [Installation](Installation) | Requirements, installing, folder layout, first mob |
| [Commands](Commands) | All `/bettermob` commands and their permissions |
| [Mobs](Mobs) | The mob file format and every option |
| [Spawners](Spawners) | Spawners and natural spawn rules |
| [Skills](Skills) | Triggers, mechanics, conditions and targeters |
| [Drop tables](Drop-Tables) | What mobs drop, `Drops:` and `droptables/` |
| [Items](Items) | Custom items, `~onUse` skills, `/bettermob give` |
| [Packs](Packs) | Content packs, importing MythicMobs packs |
| [Developer API](Developer-API) | Events, custom mechanics, Maven dependency |
| [Development](Development) | Building, tests, code layout, how to add a mechanic |
| [Troubleshooting](Troubleshooting) | "I only see a pig", warnings in the console, and more |

## Features

- One mob per file or many mobs per file, nested folders allowed
- BetterModel or ModelEngine models, including rideable seats
- Skill engine with triggers, `delay`, `cd` and inline `?condition`
- Custom items with model data and right-click skills
- Armor-stand style display mobs (invisible, floating, item on the head)
- Content packs with a one-line disable switch
- Factions, auras, totems, projectiles and more MythicMobs skill mechanics
- Message files in English and German, add your own
- PlaceholderAPI placeholders, debug output, per-command permissions
- Boss bars with live health, `/bettermob stats` with optional skill timing
- `/bettermob reload` updates living mobs, `/bettermob killall` cleans up
- Developer API for your own plugins, Folia supported

Source code and releases: https://github.com/HyperGaming99/bettermob
