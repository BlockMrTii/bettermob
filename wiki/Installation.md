# Installation

## Requirements

- Paper (or a fork) **1.21+**, Java 21. Folia is supported.
- [BetterModel](https://modrinth.com/plugin/bettermodel) and/or ModelEngine if you want
  custom models. Without them mobs still spawn and run their AI and skills, they just keep
  their vanilla look.

## Install

1. Download `bettermob-<version>.jar` from the
   [Releases](https://github.com/HyperGaming99/bettermob/releases) page.
2. Put it into `plugins/` together with BetterModel (and/or ModelEngine).
3. Start the server. BetterMob creates `plugins/BetterMob/` with a few example files.

## Folder layout

```
plugins/BetterMob/
├── config.yml        # DisabledPacks, Language, Debug, Stats, factions
├── lang/             # message files (en.yml, de.yml, your own)
├── mobs/             # mob files
├── skills/           # skill files
├── items/            # item files
├── droptables/       # drop tables
└── packs/
    └── some_pack/
        ├── mobs/
        ├── skills/
        ├── items/
        └── droptables/
```

- Files can be nested in any depth of sub-folders.
- Folder names inside a pack are matched case-insensitively (`Mobs`, `mobs`).
- If two mobs, skills or items share an ID, the one loaded first wins and the console
  logs a warning for the other.

## Languages

Console and command messages live in `plugins/BetterMob/lang/<code>.yml`. `en` and `de` are written there on first start. Pick one with `Language:` in `config.yml` (default `en`) and apply it with `/bettermob reload`.

To add a language, copy `lang/en.yml` to `lang/<code>.yml`, translate the values and set `Language: <code>`. Keys you leave out fall back to the bundled English text. Values use `&` colour codes and placeholders such as `{mob}`, `{skill}`, `{folder}` or `{error}`; keep the placeholders of the English original. Debug output (`/bettermob debug`) stays English.

## Your first mob

Create `plugins/BetterMob/mobs/my_pig.yml`:

```yaml
Type: PIG
Display: '&dFancy Pig'
Health: 20
```

Then run `/bettermob reload` and `/bettermob spawn my_pig`.

To give it a model, add a model skill (see [Skills](Skills)):

```yaml
Skills:
  - model{mid=my_model_id} @self ~onSpawn
  - model{mid=my_model_id} @self ~onLoad
```

`my_model_id` must exist in BetterModel. Test it first with `/bettermodel spawn my_model_id`.

## PlaceholderAPI (optional)

With [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed BetterMob registers these placeholders:

| Placeholder | Value |
|---|---|
| `%bettermob_mobs_alive%` | Number of loaded BetterMob mobs |
| `%bettermob_mobs_alive_<id>%` | Number of loaded mobs with that id |
| `%bettermob_loaded_mobs%` | Registered mob definitions |
| `%bettermob_loaded_skills%` | Registered skills |
| `%bettermob_loaded_items%` | Registered items |

Placeholders from any expansion are also resolved in a mob's `Display:` name and in the text of the `command{c=...}` and `message{m=...}` mechanics. The player is the trigger if it is a player, otherwise the caster. Without PlaceholderAPI nothing changes.
