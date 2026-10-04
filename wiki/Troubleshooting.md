# Troubleshooting

## The mob spawns, but I only see the vanilla mob (no model)

1. **Does the model exist?** Run `/bettermodel spawn <model id>`. If that shows nothing, the
   model isn't installed in BetterModel. BetterMob can't create it. Content packs usually
   ship only YAML, the model files are separate.
2. **Is the ID the same?** The ID in `model{mid=...}` (or `Model:`) must match the BetterModel
   model exactly.
3. **Read the console right after spawning.**
   - `BetterModel model '<id>' does not exist.` - the model isn't registered.
   - `BetterModel is not active while spawning '<id>' - no model attached.` - BetterModel isn't enabled.
   - `BetterModel '<id>' could not be attached: ...` - the message tells you why.
4. **Using ModelEngine?** Use `modelengine{mid=...}`, not `model{...}`. The two plugins
   have separate model lists.

## Wrong mob type (everything spawns as a zombie)

A mob without `Type` defaults to `ZOMBIE`. This usually means the file has an extra level of
indentation or the `Type:` line is missing. Check that `Type:` is at the root of the file
(or directly under the mob's ID in a multi-mob file).

## `Skill mechanic 'xyz' is not supported.`

BetterMob doesn't implement that mechanic, the line is skipped. See [Skills](Skills) for what
exists. The rest of the skill still runs.

## `Skill condition 'xyz' is not supported - ignored.`

The condition is unknown and counts as **true**. A skill that depends on it will run
more often than it should.

## `Mob '<id>' from ... overrides an already loaded definition - ignored.`

Two files define the same ID. The first one loaded wins, the other is ignored. Rename one or
disable a pack in `config.yml`. The same applies to skills and items.

## Some mobs or skills from a pack are missing

- Run `/bettermob packs` to see if the pack is enabled.
- Check the console for `could not be parsed` lines. They name the skill and the line.
- Sub-folders and `Mobs`/`mobs` casing don't matter, but a file must end in `.yml`.

## A mob never goes away (preview, display, effect)

Those rely on a `remove{delay=...} @self ~onSpawn` line. Make sure the line exists and that
you reloaded after adding it. Mobs that were already spawned keep their old definition.

## `Unknown command: /bettermob`

The plugin isn't enabled. Check `/plugins` and the console at startup for an error from
`[BetterMob]`. Also make sure you typed only the command into chat.

## A skill does nothing and the console is quiet

Turn on debug with `/bettermob debug verbose` (and `/bettermob debug filter <mob id>`), trigger the skill again and read why it stopped. See [Commands](Commands#debug-output).

## The console messages are in German

The message language comes from `Language:` in `config.yml` (`en` by default). If you see German lines, the setting is `de` or a custom file is selected, see [Installation](Installation#languages). The texts quoted on this page are the English ones.

## Still stuck?

Open an issue at https://github.com/HyperGaming99/bettermob/issues with your Paper version,
the BetterMob version, the mob file and the console output.
