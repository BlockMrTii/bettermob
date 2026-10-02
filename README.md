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

## Model engine support

A mob's model comes from [BetterModel](https://modrinth.com/plugin/bettermodel) or
[ModelEngine](https://www.spigotmc.org/resources/model-engine-4.108821/). Both are optional and
independent: each uses its own model IDs, and a model must exist in the engine you point at it.

| Feature | BetterModel | ModelEngine |
|---|---|---|
| Attach a model with `model{mid=...}` | Yes | - |
| Attach a model with `modelengine{mid=...}` | - | Yes |
| `Model:` field in the mob file (automatic attach on spawn/load) | Yes | No, use a `modelengine{}` line |
| Hide the vanilla body while a model is attached | Yes | Yes |
| Model reattached after restart/chunk load (`~onLoad`) | Yes | Yes, with a `modelengine{}` line on `~onLoad` |
| `state{s=...}` plays a model animation | Yes | No |
| `mountmodel{seat=...}` (rideable seats with WASD control) | Yes | No |
| `@ModelPart{p=<bone>}` targeter (bone position) | Yes | No, falls back to chest height |
| `bodyrotation{...}` head/body turn limits | Yes | No, ignored |

Mechanics that need BetterModel simply do nothing on a ModelEngine mob (`mountmodel` logs a
warning) instead of failing. Using both engines for different mobs on one server works.

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

### Dev builds

Pushing to the `dev` branch builds the jar and publishes it as the **Dev build**
pre-release (tag `dev-build`). There is only ever one: each push replaces it with the newest jar
(`bettermob-<version>-dev-<commit>.jar`). It is unstable and meant for testing, the
tagged releases above are the stable ones.

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
├── droptables/           # default drop tables
│   └── my_drops.yml
└── packs/
    └── some_pack/
        ├── mobs/
        ├── skills/
        ├── items/
        └── droptables/
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
Faction: Elite              # mobs of one faction never target or hurt each other

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
  FollowRange: 15
  Scale: 0.5
  PreventItemPickup: true

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

## Drop tables

`droptables/*.yml` (and each pack's `DropTables/`) define what a mob drops on death:

```yaml
nm_bison_drops:
  MinItems: 2           # at least / at most this many item entries drop
  MaxItems: 3
  Drops:
  - EXP 5-11 100%       # experience, not counted towards MinItems/MaxItems
  - LEATHER 4-5 80%     # <item> <amount or range> <chance>
  - nm_bison_fur 2-3 40%
```

A line is `<what> <amount> <chance>`. The amount is a number or a range (`2-4`) and the chance is
`80%` or `0.8`; both are optional (1 piece, 100%). `<what>` is `EXP`, another drop table, a
registered [item](#items) or a Vanilla material. Point a mob at a table (or write drops directly)
with `Drops:`:

```yaml
Drops:
- nm_bison_drops
- DIAMOND 1 5%
```

A mob with a `Drops:` list loses its vanilla drops **and** vanilla experience automatically, only
its own drops remain. `PreventOtherDrops: true` does the same for mobs without any `Drops:`.

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
`~onDamaged`, `~onAttack` (melee hits only, projectiles don't count), `~onShoot` (bow/crossbow
shot, `CancelEvent` stops the vanilla arrow), `~onDeath`, `~onTimer:<ticks>` (repeats). Triggers fire for any
living entity, armor stands included. On items: `~onUse` (right click, see [Items](#items)).

**Mechanics:** `sound`, `model` (attach via BetterModel), `modelengine` (attach the
same way via ModelEngine instead — pick whichever engine that mob's model is
registered in), `mountmodel` (BetterModel seats — the rider gets actual WASD
control), `potion`, `look`, `breakblock`, `state` (plays a BetterModel animation),
`summon` (spawns another registered mob), `remove`, `command`, `gcd`, `randomskill`
(`s=a,b,c`), `skill`, `sudoskill` (run a skill with the target as caster), `cancelevent`,
`cancelskill`, `equip` (`item=BOW:HAND`), `addtag`/`removetag`, `damage` (`amount`), `throw` (`velocity`, `velocityY`, both scaled by 1/10), `lunge` (`velocity`),
`setblock` (`m`), `effect:particles` (`p`, `amount`, `hS`, `vS`, `speed`; alias `e:p`),
`effect:particlering` (`particle`, `radius`, `points`, ...), `spin` (`duration` ticks,
`velocity` degrees/tick), `takeitem` (`i=<item>;a=<amount>`, removes a registered item
from the target player), `ignite` (`t` ticks), `stun` (`d` ticks; `ai` default true disables the AI, `g=true` also turns gravity off, `f=true` holds the mob still, `state=<animation>` plays that BetterModel animation), `velocity` (`m=SET|ADD|MULTIPLY|DIVIDE`, `x`, `y`, `z`, `repeat`, `repeatInterval`), `freeze` (`ticks`, powder-snow effect),
`setNoDamageTicks` (`ticks`), `onDamaged`/`onAttack`/`onDeath`/`onShoot`/`aura` (`auraName`, `time`, `cE`, `oS`, `oE`, `oT`, `i`, `oH`: a timed aura that runs `oS` at start, `oE` at end, `oT` every `i` ticks and `oH` on its event, `cE=true` cancels that event meanwhile), `bodyrotation` (`headUneven`, `bodyUneven`, `minHead`, `maxHead`, `minBody`, `maxBody`, `delay`; BetterModel only), `shoot` (`type=arrow;velocity;damage;oh=[ ...]` fires an arrow at
the target, the `oh` lines run on a hit with the hit entity as target), `totem` (`os=[ ... ]`
runs once at the targeter's location, `yo` shifts it up; with `md` ticks, `ot=[ ... ]` repeats every `i` ticks
(default 20) and `oe=[ ... ]` runs at the end; stops early if the caster dies; with `oh=[ ... ]` an invisible, unbreakable body is placed at the totem for `md` ticks (default 100) and the lines run whenever someone hits it, with the attacker as target).

`<caster.damage>` and `<caster.name>` inside mechanic parameters are replaced with the caster's
attack damage and name. A skill may set `Cooldown: <seconds>` (per caster).

Every mechanic accepts `delay=<ticks>` (run this line later without holding up the rest)
and `cd=<seconds>` (cooldown per caster). `skill`/`randomskill` read their skill ids from
`s=`, `skill=` or `skills=`; `summon` from `type=` (or `t=`/`mob=`).

`modelengine{mid=<id>}` needs the [ModelEngine](https://www.spigotmc.org/resources/model-engine-4.108821/)
plugin and uses its own model IDs — these are separate registries from BetterModel's,
so a model has to exist in whichever engine you point at it.

**Conditions:** `offgcd`, `onground`, `chance{chance=0.75}`, `hastag{t=...}`, `hasaura{n=...}`, `faction{faction=Elite,Other}` (the caster's, or each candidate's inside a multi-target targeter), `onblock{b=...}` (block under the caster), `blocktype{type=...}`, `skillOnCooldown{skill=...}`, `distance{d=0-6}` (also `>3`, `<=5`) to the trigger/target. A skill's `Conditions`/`TargetConditions` entry may end in `castinstead <skill>` to cast that skill instead when it holds. Any mechanic line can
end with `?condition{...}` (or `?!condition{...}` to negate) to run only when that
check passes; unsupported conditions (this plugin has no variable/faction system)
are logged and treated as passing, so the line still runs.

**Targeters:** `@self`, `@trigger`/`@target`, `@ObstructingBlock`, `@Forward{f=1.5;
uel=true;yoffset=-1;rotate=-22}` (point in front of the caster, `rotate` swings it sideways,
positive = right), `@SelfLocation{x;y;z}` (caster position, optionally shifted), `@PIR{r=2}` (nearest player within `r`), `@EntitiesNearOrigin{r=4;Conditions=[ - isPlayer{} true - isCaster{} false]}` (alias `@ENO`, around a totem's location) and `@EntitiesInRadius` (`@EIR`, around the caster) hit every matching entity, `@ModelPart{p=<bone>}` (position of a BetterModel bone, falls back to chest height). A skill line without a targeter inherits the target of the line that called it.

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
    <version>v1.1.4</version> <!-- a tag -->
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
    <version>1.1.4</version>
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
