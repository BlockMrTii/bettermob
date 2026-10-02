# BetterMob

A Paper plugin for registering custom mobs in YAML — MysticMobs/MythicMobs-style —
and rendering them through [BetterModel](https://modrinth.com/plugin/bettermodel) or
[ModelEngine](https://www.spigotmc.org/resources/model-engine-4.108821/). Includes a
small skill engine for AI behavior, triggers, and MythicMobs-compatible skill syntax.

## Requirements

- Paper (or a fork) **1.21+** — [Folia](https://papermc.io/software/folia) is supported too
- Java 21
- [BetterModel](https://modrinth.com/plugin/bettermodel) and/or
  [ModelEngine](https://www.spigotmc.org/resources/model-engine-4.108821/) installed
  and enabled if you want mobs to actually render a custom model — without either,
  mobs still spawn and run their AI/skills, just with their vanilla appearance. A mob
  can use either engine (or both, for different mobs), chosen per `Skills:` line via
  `model{}` (BetterModel) or `modelengine{}` (ModelEngine).

## Building

```bash
mvn clean package
```

Produces `target/bettermob-<version>.jar`. Drop it into your server's `plugins/`
folder.

## Releases

Every push to `main` and every PR triggers a GitHub Actions build (`.github/workflows/release.yml`)
that compiles the jar and uploads it as a build artifact. A release is cut automatically
when the version in `pom.xml` changes (see "Getting the API").

## Commands

| Command | Description |
|---|---|
| `/bettermob spawn <id> [amount]` | Spawn a registered mob at your location |
| `/bettermob list` | List all registered mob IDs |
| `/bettermob packs` | List discovered packs and whether they're enabled |
| `/bettermob reload` | Reload config, mobs, skills, and packs |
| `/bettermob skill <id> [player]` | Manually run a registered skill, bypassing its normal triggers |
| `/bettermob give <item> [player] [amount]` | Give a registered item (see [Items](#items)) |

Alias: `/bmob`. Permission: `bettermob.admin` (default: op).

## Folder layout

```
plugins/BetterMob/
├── config.yml          # DisabledPacks: [...]
├── mobs/                # default mob files
│   └── my_mob.yml
├── skills/               # default skill files
│   └── my_skill.yml
├── items/                # default item files
│   └── my_item.yml
└── packs/
    └── some_pack/
        ├── mobs/
        ├── skills/
        └── items/
```

The flat `mobs/`/`skills/` folders are always loaded. Each subfolder under `packs/`
is its own bundle with its own `mobs/`/`skills/` (case-insensitive — `Mobs`/`Skills`
work too, matching how most MythicMobs packs are shipped). Disable a pack by adding
its folder name to `DisabledPacks` in `config.yml`.

If two mobs or skills share an ID, the one loaded first wins; the rest are skipped
with a console warning.

## Defining a mob

A mob file can define **one mob** (fields directly at the file root, ID = filename)
or **multiple mobs** (each top-level key is its own mob, like a classic MysticMobs
`mobs.yml`):

```yaml
# mobs/skeleton_knight.yml — one mob, ID = "skeleton_knight"
Type: SKELETON
Display: '&c&lSkeleton Knight'
Model: skeleton_knight      # BetterModel model ID; defaults to the mob ID
Health: 60
Damage: 8
RemoveAi: false

AIGoalSelectors:
  - clear
  - randomstroll            # matched against the mob's actual vanilla goals
AITargetSelectors:
  - clear

Options:
  Collidable: true
  MovementSpeed: 0.25
  PreventOtherDrops: true
  Silent: true
  PreventRenaming: false
  PreventLeashing: false
  AlwaysShowName: false
  PreventSunburn: true       # zombies/skeletons won't burn in daylight
  Invincible: false
  # Armor-stand style mobs (e.g. floating, hands-off display cards):
  Invisible: false           # hide the body, equipment stays visible
  CanMove: true              # false = no AI and no gravity (floats in place)
  Interactable: true         # false = right clicks / equipment swaps are cancelled
  Marker: false              # armor stands only: no hitbox
  ItemHead: my_item          # a registered item (see Items) worn on the head
  KnockbackResistance: 1

Modules:
  ThreatTable: true           # retarget to whoever dealt the most damage

DamageModifiers:
  - FIRE 1.2                  # multiply fire damage taken by 1.2

Skills:
  - skill{s=my_skill_id} ~onInteract
  - sound{s=entity.skeleton.ambient;p=1.0;v=1} @self ~onTimer:200
```

A file with multiple mobs looks like:

```yaml
nm_mustang_red:
  Type: IRON_GOLEM
  ...
nm_mustang_cyan:
  Type: IRON_GOLEM
  ...
```

`AIGoalSelectors`/`AITargetSelectors` only work with goals the mob's vanilla AI
actually has — Paper can reuse existing goals, not invent new ones. `clear` removes
the category first; named goals (matched loosely, e.g. `randomstroll` also matches
`water_avoiding_random_stroll`) are re-added from what the mob had before clearing.

## Items

`items/*.yml` (and each pack's `Items/`) define items the same way MythicMobs does -
every top-level key with an `Id:` is one item:

```yaml
nm_pack_starter_pack:
  Id: PAPER                  # the Vanilla material
  Model: 635000              # CustomModelData the resource pack switches on
  Display: '&9Starter &fBooster Pack'
  Lore:
  - '&7Contains &f&l2 &r&7Trading Cards!'
  Skills:
  - skill{s=nm_pack_open_starter_pack;cd=1} @self ~onUse
  - takeitem{i=nm_pack_starter_pack;a=1} @self ~onUse
```

`/bettermob give <item> [player] [amount]` hands them out. Right-clicking one runs its
`~onUse` skills with the player as caster. The same item ids can be used as a mob's
`Options.ItemHead`. Furniture settings on an item (`Type: FURNITURE`) are ignored.

## Skills

Skill files under `skills/` hold named, reusable skills (a file may contain several,
e.g. a `_parse`/`_activate` pair):

```yaml
my_skill_parse:
  TargetConditions:
    - blocktype{type=OAK_LOG,SPRUCE_LOG}
  Skills:
    - CancelEvent
    - skill{s=my_skill_activate}

my_skill_activate:
  Skills:
    - sound{s=entity.rabbit.ambient;p=1;v=1} @self
    - delay 8
    - potion{type=SLOW;duration=20;level=5} @self
```

A mob's own `Skills:` list can call a named skill (`skill{s=<id>}`) or run a mechanic
directly, with a trigger suffix:

```yaml
Skills:
  - skill{s=my_skill_parse} ~onInteract
  - model{mid=my_model} @self ~onSpawn
  - sound{s=entity.pig.hurt} @self ~onDamaged
```

**Triggers:** `~onSpawn`, `~onLoad` (chunk/restart rehydration), `~onInteract`,
`~onDamaged`, `~onAttack`, `~onDeath`, `~onTimer:<ticks>` (repeats). Triggers fire for any
living entity, armor stands included. On items: `~onUse` (right click, see [Items](#items)).

**Mechanics:** `sound`, `model` (attach via BetterModel), `modelengine` (attach the
same way via ModelEngine instead — pick whichever engine that mob's model is
registered in), `mountmodel` (BetterModel seats — the rider gets actual WASD
control), `potion`, `look`, `breakblock`, `state` (plays a BetterModel animation),
`summon` (spawns another registered mob), `remove`, `command`, `gcd`, `randomskill`
(`s=a,b,c`), `skill`, `sudoskill` (run a skill with the target as caster), `cancelevent`,
`cancelskill`, `effect:particles` (`p`, `amount`, `hS`, `vS`, `speed`; alias `e:p`),
`effect:particlering` (`particle`, `radius`, `points`, ...), `spin` (`duration` ticks,
`velocity` degrees/tick), `takeitem` (`i=<item>;a=<amount>`, removes a registered item
from the target player).

Every mechanic accepts `delay=<ticks>` (run this line later without holding up the rest)
and `cd=<seconds>` (cooldown per caster). `skill`/`randomskill` read their skill ids from
`s=`, `skill=` or `skills=`; `summon` from `type=` (or `t=`/`mob=`).

`modelengine{mid=<id>}` needs the [ModelEngine](https://www.spigotmc.org/resources/model-engine-4.108821/)
plugin and uses its own model IDs — these are separate registries from BetterModel's,
so a model has to exist in whichever engine you point at it.

**Conditions:** `offgcd`, `onground`, `blocktype{type=...}`. Any mechanic line can
end with `?condition{...}` (or `?!condition{...}` to negate) to run only when that
check passes; unsupported conditions (this plugin has no variable/faction system)
are logged and treated as passing, so the line still runs.

**Targeters:** `@self`, `@trigger`/`@target`, `@ObstructingBlock`, `@Forward{f=1.5;
uel=true;yoffset=-1;rotate=-22}` (point in front of the caster, `rotate` swings it sideways,
positive = right), `@SelfLocation`, `@PIR{r=2}` (nearest player within `r`).

Unknown mechanics/conditions/targeters are logged with a clear warning and skipped
rather than crashing the skill or the server.

## Developer API

BetterMob registers a `BetterMobAPI` Bukkit service you can use from your own plugins.
Add `depend: [BetterMob]` (or `softdepend`) to your `plugin.yml` and compile against the
BetterMob jar (`provided` scope). Everything public lives in `eu.northsoft.bettermob.api`.

### Getting the API

Releases are automatic: whenever a push to `main` carries a `<version>` in `pom.xml`
that has no `v<version>` tag yet, the workflow creates the tag and a GitHub Release with
the jar, and publishes it to GitHub Packages (Maven). To release, just bump the version
in `pom.xml` and push.

**JitPack** (no login needed):

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.HyperGaming99</groupId>
    <artifactId>bettermob</artifactId>
    <version>v1.1.1</version> <!-- a tag -->
    <scope>provided</scope>
</dependency>
```

**GitHub Packages** (needs a `read:packages` token in your `settings.xml`, server id `github`):

```xml
<repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/HyperGaming99/bettermob</url>
</repository>

<dependency>
    <groupId>eu.northsoft</groupId>
    <artifactId>bettermob</artifactId>
    <version>1.1.1</version>
    <scope>provided</scope>
</dependency>
```

```java
BetterMobAPI api = BetterMobAPI.get();

api.getMobs();                                   // all registered mob definitions (MobInfo)
api.spawn("zombie_brute", player.getLocation()); // Optional<LivingEntity>
api.isBetterMob(entity);                         // is this one of ours?
api.getMobInfo(entity);                          // its definition
api.runSkill("wumpus_wave", player);             // run a skill with any LivingEntity as caster
api.reload();                                    // same as /bettermob reload
```

**Events** (`eu.northsoft.bettermob.api.event`): `BetterMobSpawnEvent` and
`BetterMobDeathEvent`, both exposing the entity and its `MobInfo`.

**Custom mechanics:** register your own mechanic and use it in skill lines like any
built-in one (`heal{amount=4} @self ~onDamaged`). Built-ins can't be overridden, and
your mechanics are removed automatically when your plugin is disabled.

```java
api.registerMechanic(this, "heal", ctx -> {
    double amount = Double.parseDouble(ctx.params().getOrDefault("amount", "2"));
    if (ctx.target() instanceof LivingEntity living) living.heal(amount);
});
```

## Persistence

Mob state (model tracker, threat table, timers) lives in memory and is tied to the
mob's UUID, tagged via a small PDC marker so it survives a server restart or chunk
unload/reload — `~onLoad` fires again and the model gets reattached automatically.

## License

No license file yet — private repository.
