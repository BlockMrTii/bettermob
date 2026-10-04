# Packs

A pack is a folder in `plugins/BetterMob/packs/`. It has its own `mobs/`, `skills/`, `items/` and `droptables/` folders (any capitalisation, any nesting), loaded on top of the plugin's own.

```
packs/
└── nogs_menagerie/
    ├── Mobs/
    │   └── companions/...
    ├── Skills/
    ├── Items/
    └── DropTables/
```

## Disabling a pack

`plugins/BetterMob/config.yml`:

```yaml
DisabledPacks:
  - nogs_menagerie
```

then `/bettermob reload`. `/bettermob packs` shows what was found.

## Importing a MythicMobs pack

1. Copy the pack folder into `packs/`.
2. Install the model plugin the pack uses (BetterModel or ModelEngine) **and the models**.
   Packs usually contain only the YAML, not the model files.
3. `/bettermob reload` and watch the console.

What to expect:

- Mob, skill and item files are found recursively, wherever they are in the pack.
- Mobs, skills and items with the same ID as an earlier one are skipped with a warning.
- Mechanics, conditions and targeters BetterMob doesn't have are logged once per use and
  skipped. The rest of the skill still runs.
- Things the pack gets from other plugins (for example a command like `/nstc givecard`)
  need those plugins on the server.

## Checking a pack

`/bettermob validate [pack]` reads every mob, skill and item file of the pack (or of all packs and the main folder) and reports what BetterMob cannot run, without spawning anything:

- lines that cannot be parsed, including malformed lines in `droptables/` files and in a mob's `Drops:` list
- unknown mechanics, targeters and conditions
- targeter conditions that are not supported (inside `Conditions=[ ... ]`)
- `skill`, `randomskill` and `sudoskill` lines that point to a skill that is not defined

It also looks into inline skills (`skill{s=[ ... ]}`, `totem{os=[ ... ]}`, `oh=[ ... ]` and so on). The result shows how many lines were checked, then per pack the problems with the file name and the line (the first 20 per pack). Mechanics that other plugins register are only known once those plugins are enabled, so run the command after the server has started. AI goals are not checked, they depend on the mob type.

Permission: `bettermob.reload`.

## Tips

- Run `/bettermob validate <pack>` first to see what is unsupported.
- Check a pack's skills one by one with `/bettermob skill <id>`.
- If a model doesn't show, first test it directly with `/bettermodel spawn <model id>`.
  See [Troubleshooting](Troubleshooting).
