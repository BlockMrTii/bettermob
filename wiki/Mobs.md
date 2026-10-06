# Mobs

A mob file can hold **one mob** (fields at the file root, ID = file name) or **many mobs**
(every top-level key is a mob ID):

```yaml
# mobs/skeleton_knight.yml  ->  mob "skeleton_knight"
Type: SKELETON
Display: '&c&lSkeleton Knight'
Health: 60
```

```yaml
# mobs/horses.yml  ->  mobs "red_horse" and "blue_horse"
red_horse:
  Type: HORSE
  Health: 30
blue_horse:
  Type: HORSE
  Health: 30
```

A file with exactly one top-level key and no `Type` at the root is unwrapped
automatically, so a MythicMobs-style `my_mob:` block pasted into its own file works.

## Top-level fields

| Field | Default | Description |
|---|---|---|
| `Type` | `ZOMBIE` | Vanilla entity type (any `EntityType`, case-insensitive) |
| `Display` | `&f<id>` | Name, `&` color codes |
| `Model` | mob ID | BetterModel model attached automatically, see below |
| `Health` | 20 | Max health |
| `Damage` | 2 | Attack damage |
| `RemoveAi` | false | Disable the AI completely |
| `Faction` | none | Mobs with the same faction never target or damage each other (also through projectiles and skills). Names are case-insensitive. Players can join a faction too, see below |
| `AIGoalSelectors` | - | Movement/look/jump goals, see below |
| `AITargetSelectors` | - | Targeting goals, see below |
| `Options` | - | See the table below |
| `Modules.ThreatTable` | false | Re-target whoever dealt the most damage, every second |
| `Modules.BossBar` | - | A boss bar with the mob's health for nearby players, see [Boss bar](#boss-bar). `BossBar: true` uses the defaults |
| `DamageModifiers` | - | List like `- FIRE 1.2`, multiplies damage of that cause |
| `Skills` | - | Skill lines with triggers, see [Skills](Skills) |
| `Equipment` | - | List of `<item>:<slot>` lines, for example `- BOW:HAND` and `- my_helmet:HEAD`. The item is a registered [item](Items) id or a vanilla material, the slot is `HAND` (default), `OFFHAND`, `HEAD`, `CHEST`, `LEGS` or `FEET`. Applied on spawn and on `reload`; dropping chance is 0. Unknown items and slots are logged with the mob id |
| `Spawn` | - | Turns the mob into a natural spawn, see [Natural spawns](#natural-spawns) |
| `Egg` | - | Options of the spawn egg given by `/bettermob egg`, see [Spawn eggs](#spawn-eggs) |
| `Drops` | - | Drop table names and/or drop lines, see [Drop tables](Drop-Tables) |

Unknown fields are ignored.

## Options

| Option | Default | Description |
|---|---|---|
| `Collidable` | true | Entity collision |
| `MovementSpeed` | vanilla | Movement speed attribute |
| `KnockbackResistance` | vanilla | Knockback resistance attribute |
| `Silent` | false | No sounds |
| `Invincible` | false | Invulnerable |
| `PreventOtherDrops` | false | Clear the vanilla drops and experience on death. Happens automatically when the mob has `Drops:` |
| `PreventRenaming` | false | Name tags don't work |
| `PreventLeashing` | false | Can't be leashed |
| `AlwaysShowName` | false | Show the name tag permanently |
| `PreventSunburn` | true | Zombies and skeletons don't burn in daylight |
| `Invisible` | false | Hide the body, equipment stays visible |
| `CanMove` | true | `false` = no AI and no gravity, the mob floats in place |
| `Interactable` | true | `false` = right clicks and equipment swaps are cancelled |
| `Marker` | false | Armor stands only: no hitbox |
| `ItemHead` | - | ID of a registered [item](Items) worn on the head |
| `FollowRange` | vanilla | How far the mob notices targets |
| `Scale` | vanilla | Body size, `0.5` = half size |
| `PreventItemPickup` | false | The mob doesn't pick up items from the ground |

## Boss bar

```yaml
Modules:
  BossBar:
    Title: '&c<mob.name> &7<mob.hp>/<mob.maxhp>'
    Range: 64
    Color: RED
    Style: SOLID
```

The bar is shown to every player within `Range` blocks of the mob (same world) and updates twice a second.

| Option | Default | Description |
|---|---|---|
| `Title` | `<mob.name>` | Text of the bar. `&` colour codes, `<mob.name>` (the `Display`), `<mob.id>`, `<mob.hp>` and `<mob.maxhp>` (rounded up) and PlaceholderAPI placeholders without a player |
| `Range` | 64 | Blocks around the mob in which players see the bar |
| `Color` | `RED` | `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE` or `WHITE` |
| `Style` | `SOLID` | `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12` or `SEGMENTED_20` |
| `CreateFog`, `DarkenSky`, `PlayMusic` | false | Boss bar effects of the client |

The bar is removed when the mob dies, despawns or its chunk unloads, rebuilt from the new definition on `/bettermob reload` and removed when the plugin is disabled. An unknown `Color` or `Style` logs a warning and uses the default.

## AI goals

Paper can only reuse goals the mob already has, it can't invent new ones. `clear` removes
the whole category, every other entry re-adds a matching vanilla goal:

```yaml
AIGoalSelectors:
  - clear
  - randomstroll        # matches e.g. water_avoiding_random_stroll
AITargetSelectors:
  - clear
```

Names are matched loosely (case, `_` and `-` are ignored, partial names match). Parameters
in braces, like `meleeattack{attackReach=2}`, are ignored. An entry the mob doesn't have is
logged and skipped.

MythicMobs names that differ from Paper's are translated:

| You write | Paper goal |
|---|---|
| `meleeattack` | `melee_attack` |
| `attacker` | `hurt_by` (fight back against whoever hit it) |
| `players` / `nearestplayer` | `nearest_attackable` |
| `lookatplayers` | `look_at_player` |
| `fleeplayers` | `avoid_entity` |

Entries may start with a **priority number** (lower = more important), as MythicMobs packs write them:
`1 monsters`, `2 attacker`. Without a number the entries get 0, 1, 2 ... in order.

Two goals don't exist in Vanilla, BetterMob provides them itself:

| Goal | Behaviour |
|---|---|
| `lookAtTarget{r=15}` | Faces the target while it is within `r` blocks |
| `monsters` | Targets the nearest hostile monster. Mobs managed by BetterMob count as friends (there are no factions), so two of your own mobs never fight each other |

`rangedAttack` is `ranged_bow_attack` (or the crossbow one), `player` is `nearest_attackable`.

A mob that should fight back needs both a melee goal and a target goal after `clear`:

```yaml
AIGoalSelectors:
  - clear
  - meleeattack
  - randomstroll
AITargetSelectors:
  - clear
  - attacker
```

## Models

BetterMob attaches a model in three ways:

1. **Automatically** through `Model:` (default: the mob ID). BetterModel is asked first; if it has no model with that name, ModelEngine is asked. If neither has it, nothing happens and nothing is logged.
2. **Through a skill**: `model{mid=...} @self ~onSpawn` for BetterModel or `modelengine{mid=...} @self ~onSpawn` for ModelEngine (and `~onLoad`). If a mob has such a skill, the automatic way is skipped, otherwise the two would fight each other.

The vanilla body is hidden automatically once a model is attached. `state`, `mountmodel`, `@ModelPart` and `bodyrotation` work with both engines, see [Skills](Skills).

## Armor-stand display mobs

Floating, hands-off displays (for example a card hovering in front of a player):

```yaml
my_display:
  Type: ARMOR_STAND
  Display: '&7Label'
  Options:
    Invisible: true
    CanMove: false
    Interactable: false
    Marker: true
    Invincible: true
    AlwaysShowName: true
    ItemHead: my_item
  Skills:
    - remove{delay=60} @self ~onSpawn
```

Triggers work for every living entity, armor stands included.

## Spawn eggs

`/bettermob egg <mob> [player] [amount]` gives a spawn egg item for the mob (up to 64 at a time). Right-click a block with it to spawn the mob on the clicked face; the egg is used up unless you are in creative mode.

- The egg is the vanilla spawn egg of the mob's `Type` (a zombie egg for types without one), named after the mob's `Display:`, and carries the mob id so it keeps working after restarts and `/bettermob reload`.
- It never spawns the vanilla mob: using it on an entity or a spawner, and dispensers, do nothing.
- A removed mob makes its eggs tell the player that the mob is unknown.

```yaml
Egg:                        # optional
  Material: PIG_SPAWN_EGG   # any material, default is the egg of the mob's Type
  Name: '&6Goblin Egg'      # default is the mob's Display
```

An unknown `Material` is logged with the mob id and the default egg is used. Permission: `bettermob.give`.

## Natural spawns

A `Spawn` block makes the mob replace vanilla spawns:

```yaml
Type: SKELETON
Spawn:
  Worlds: [world]           # optional, empty = every world
  Biomes: [desert, plains]  # optional, empty = every biome
  Time: night               # day | night | any (default)
  Chance: 0.3               # 0-1, rolled per natural spawn, default 1
```

Whenever the server spawns a vanilla mob of the same `Type` **naturally** (not from spawners, eggs or commands) and the world, biome, time of day and `Chance` all fit, that spawn is replaced by this mob. `Worlds` and `Biomes` take lists (biome ids with or without `minecraft:`).

- Without a `Spawn` block nothing is replaced.
- If several mobs match, the first one that passes its chance wins.
- A `Worlds` or `Biomes` that is not a list, a `Chance` that is not a number or an unknown `Time` logs a warning and the whole `Spawn` block is ignored, so a typo never widens the spawn.

For spawns you place yourself see [Spawners](Spawners).

## Player factions

A player belongs to a faction when they have the permission `bettermob.faction.<name>` (name in lower case) or are listed under `factions:` in `config.yml`, by name or UUID:

```yaml
factions:
  elite:
    - PlayerName
    - 123e4567-e89b-12d3-a456-426614174000
```

Mobs of that faction then never target that player, and the player can't hurt them. Without the permission or an entry a player is in no faction, operators included. Two players are never "the same faction" to each other, only mobs trigger the rule.
