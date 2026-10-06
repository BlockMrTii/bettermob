# Skills

A skill line looks like this:

```
mechanic{param=value;param=value} @targeter{param=value} ?condition{...}
```

Only the mechanic name is required. The default targeter is `@self`.

## Where skills live

**Skill files** (`skills/**/*.yml`) hold named skills. A file may contain several:

```yaml
my_skill:
  Conditions:
    - onground{} true
  Skills:
    - sound{s=entity.pig.ambient;p=1.5;v=1} @self
    - delay 20
    - potion{type=SLOW;duration=40;level=5} @self
```

**A mob's or item's `Skills:` list** runs skills or mechanics on a trigger:

```yaml
Skills:
  - skill{s=my_skill} ~onInteract
  - sound{s=entity.pig.hurt} @self ~onDamaged
```

`Conditions`/`Condition`, `TargetConditions`/`TargetCondition` and `Skills`/`Skill` are all
accepted.

## Triggers

Written as `~onTrigger` at the end of a mob or item line.

| Trigger | Fires when |
|---|---|
| `~onSpawn` | The mob is spawned |
| `~onLoad` | The mob's chunk is loaded again (restart, chunk reload). The model is reattached |
| `~onInteract` | A player right-clicks the mob. `@trigger` is the player |
| `~onDamaged` | The mob is hit. `@trigger` is the attacker |
| `~onAttack` | The mob hits something in melee (projectiles don't count). `@trigger` is the victim |
| `~onShoot` | The mob shoots with a bow or crossbow. `@trigger` is its target. `CancelEvent` stops the vanilla arrow |
| `~onDeath` | The mob dies (the body still exists, so sounds and effects work) |
| `~onTimer:<ticks>` | Repeatedly, default every 20 ticks |
| `~onUse` | Items only: right-click with the item in the main hand |

## Mechanics

| Mechanic | Parameters | What it does |
|---|---|---|
| `skill` | `s` (also `skill`, `skills`) | Run another skill, or inline lines: `skill{s=[ - sound{...} - delay 5 ]}` |
| `randomskill` | `s` (also `skills`): comma separated list | Run one random skill of the list |
| `sudoskill` | `s` | Run a skill with the **target** as caster |
| `sound` | `s` sound key, `p` pitch, `v` volume | Play a sound at the target |
| `model` | `mid` | Attach a BetterModel model |
| `modelengine` | `mid` | Attach a ModelEngine model |
| `state` | `state` (`s`) | Play a model animation: once for BetterModel; for ModelEngine also `lerpIn`, `lerpOut` (default 0.1), `speed` (1) and `force` (true) |
| `mountmodel` | `seat` (default `mount`), `controller` | Seat the target on a model's seat. BetterModel handles steering; with ModelEngine `controller` is `walking` (default), `flying`, `walking_force` or `flying_force`, and the driver seat is used when `seat` is not a passenger seat |
| `potion` | `type`, `duration` (ticks), `level` | Apply an effect |
| `look` | - | Turn the caster toward the target |
| `summon` | `type` (also `t`, `mob`) | Spawn another registered mob at the target |
| `remove` | - | Remove the target |
| `command` | `c`: command | Run a console command. `<caster.name>` and `<target.name>` are replaced |
| `takeitem` | `i` item ID, `a` amount | Take items from the target player |
| `breakblock` | `usetool` | Break the target block |
| `setblock` | `m` material | Replace the block at the target |
| `damage` | `amount` | Damage the target with the caster as attacker. Does not fire `~onAttack` again |
| `throw` | `velocity`, `velocityY` | Fling the target away from the caster (`velocity` and `velocityY` are both divided by 10) |
| `lunge` | `velocity`, `velocityY` | The caster dashes toward the target |
| `gcd` | `ticks` | Start the global cooldown checked by the `offgcd` condition |
| `spin` | `duration` ticks, `velocity` degrees per tick | Spin the target |
| `effect:particles` (`e:p`) | `p`, `amount`, `hS`, `vS`, `speed`, `y`, `repeat`, `repeatInterval` | Spawn particles. `y` shifts them up; `repeat` spawns them that many more times every `repeatInterval` ticks, following a moving target (entity, `@ModelPart`, `@Forward`) |
| `effect:particlering` | `particle`, `radius`, `points`, `amount`, `hS`, `vS` | Ring of particles |
| `equip` | `item=<item>:<slot>` | Put a registered item or material into a slot: `HAND` (default), `OFFHAND`, `HEAD`, `CHEST`, `LEGS`, `FEET`. The slot's drop chance is set to 0 |
| `addtag` / `removetag` | `t` | Mark the target, check it with `hastag` |
| `onDamaged`, `onAttack`, `onDeath`, `onShoot`, `aura` | `auraName`, `time`, `cE`, `oS`, `oE`, `oT`, `i`, `oH` | Put a named aura on the target for `time` ticks (forever without it). `hasaura` sees it. `oS=[ ... ]` runs at the start, `oE` at the end, `oT` every `i` ticks (default 20), `oH` each time the matching event happens to that entity (`onDamaged` = it is hit, `onAttack` = it hits, ...). `cE=true` cancels that event while the aura is active |
| `bodyrotation` | `headUneven`, `bodyUneven`, `minHead`, `maxHead`, `minBody`, `maxBody`, `stable`, `duration`, `delay` | Set how far the model's head and body may turn apart (BetterModel and ModelEngine) |
| `actionbar` (`actionmessage`) | `m`, `d` | Show text above the hotbar of the target player. It stays about 2 seconds; with `d` (ticks) it is repeated until `d` has passed |
| `title` (`sendtitle`) | `t`, `st`, `fi`, `d`, `fo` | Show a title (`t`) and subtitle (`st`) with fade in `fi` (default 10 ticks), stay `d` (70) and fade out `fo` (20) |
| `bossbar` | `m`, `color`, `style`, `p`, `d`, `countdown` | Show a boss bar with the text `m` to the target player for `d` ticks (default 100), then remove it. `color` is `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE` (default) or `WHITE`; `style` is `SOLID` (default), `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12` or `SEGMENTED_20`; `p` is the start progress 0 to 1; `countdown=true` makes it shrink to empty over `d` |
| `message` | `m` | Send a message to the target player. The text rules below apply to `message`, `actionbar`, `title` and `bossbar`: `&` colors, `<caster.name>`, `<target.name>` and PlaceholderAPI placeholders work |
| `ignite` | `t` | Set the target on fire for `t` ticks |
| `setvariable` (`variableset`) | `var`, `value`, `type` | Store a value: `var=hits` for the current skill run, `var=caster.hits` for the caster. `type` is `INTEGER`, `FLOAT` or `STRING` (default), numbers are checked. See [Variables](#variables) |
| `addvariable` (`variableadd`) | `var`, `value` | Add a number to a variable (a missing one counts as 0) |
| `heal` | `a` | Heal the target by `a` health, capped at its max health |
| `teleport` | - | Move the caster to the targeted location or entity (for example `@Target`, `@Location{...}`, `@Forward{...}`); the caster keeps its facing |
| `explosion` | `yield`, `bd`, `fire` | Explosion at the target. `yield` is the power (default 2), `bd=true` breaks blocks, `fire=true` sets fires. Off by default |
| `lightning` | `damage` | Lightning at the target; only the visual effect unless `damage=true` |
| `setspeed` | `s` | Set the target's movement speed attribute (zombie default is about 0.23) |
| `setai` | `ai` | `ai=false` switches the target mob's AI off, `ai=true` back on |
| `stun` | `d`, `ai`, `g`, `f`, `state` | Stun the target mob for `d` ticks: `ai` (default true) turns its AI off, `g=true` also turns gravity off, `f=true` holds it still (velocity 0 every tick), `state=<animation>` plays that BetterModel animation once (same as the `state` mechanic) |
| `velocity` | `m`, `x`, `y`, `z`, `repeat`, `repeatInterval` | Change the target's velocity. `m` is `SET` (default), `ADD`, `MULTIPLY` or `DIVIDE`; `repeat` applies it that many more times, every `repeatInterval` ticks |
| `freeze` | `ticks` | Freeze the target (powder-snow overlay) for `ticks` ticks, default 140 |
| `setNoDamageTicks` | `ticks` | Set the target's invulnerability ticks (0 = can be hit again at once) |
| `shoot` | `type`, `velocity`, `speedscale`, `damage`, `spread`, `gravity`, `oh`, `oe`, `ot`, `i` | Fire a projectile at the target. `type` is `arrow` (default), `spectral_arrow`, `trident`, `snowball`, `egg`, `fireball` or `smallfireball` (fireballs don't explode or burn; they deal `damage`). `spread` is a random cone in degrees, `gravity=false` makes it fly straight. `speedscale` (`ss`, default 2) multiplies `velocity`; a value that is not a number falls back to 2. `oh=[ ... ]` runs on a hit with the hit entity as target, `oe=[ ... ]` when it ends anywhere (at its location), `ot=[ ... ]` every `i` ticks (default 5) while it flies |
| `totem` | `os`, `ot`, `oe`, `md`, `i`, `oh`, `yo` | Run `os=[ ... ]` once at the targeter's location (shifted up by `yo`); `@EntitiesNearOrigin` inside is centred there. With `md` (ticks) `ot=[ ... ]` repeats every `i` ticks (default 20) and `oe=[ ... ]` runs at the end. Stops early if the caster dies. With `oh=[ ... ]` an invisible, unbreakable body is placed at the totem for `md` ticks (default 100); the lines run whenever someone hits it, with the attacker as target |
| `cancelevent` | - | Cancel the event that triggered the skill. For `~onAttack`/`~onDamaged` that is the damage event, so the vanilla hit is suppressed |
| `cancelskill` | - | Stop the rest of the skill |

Every mechanic also accepts:

- `delay=<ticks>`: run this one line later, without holding up the lines after it.
- `cd=<seconds>`: per-caster cooldown for this line, fractions are fine.

A line of its own, `delay 20`, pauses the rest of the skill for 20 ticks.

## Targeters

| Targeter | Target |
|---|---|
| `@self`, `@Caster`, `@Mob` | The caster (default) |
| `@trigger`, `@target` | The entity that caused the trigger (falls back to the caster) |
| `@Forward{f=1.5;uel=true;yoffset=-1;rotate=-22}` | A point in front of the caster. `f` distance, `uel` start at eye height, `yoffset` vertical shift, `rotate` degrees sideways (positive = right) |
| `@SelfLocation{x=0;y=-1;z=0}` | The caster's position, optionally shifted |
| `@ObstructingBlock` | The block the caster is looking at (up to 2.5 blocks) |
| `@EntitiesNearOrigin{r=4;Conditions=[ - isPlayer{} true - isCaster{} false]}` (`@ENO`) | Every entity within `r` of a totem's location, closest first. Conditions: `isPlayer`, `isCaster`, `isMob`, `hasTag{t=...}`, `faction{faction=...}` and every skill condition (see below). Skill conditions test the candidate, not the caster: `health{h=<50%}` is the candidate's health, `distance{d=<3}` and `lineofsight` measure between the candidate and the caster |
| `@EntitiesInRadius{r=4}` (`@EIR`, `@LivingEntitiesInRadius`, `@LEIR`) | Same, around the caster |
| `@PlayersInRadius{r=10}` | Every player within `r` of the caster |
| `@ModelPart{p=tnt2}` | The position of that bone of the mob's BetterModel or ModelEngine model (falls back to chest height) |
| `@PIR{r=2}` | The nearest player within `r` blocks (`limit` defaults to 1). If nobody is there, the line is skipped |
| `@Origin` | A totem's location, or the caster's location outside a totem |
| `@Location{x=0;y=64;z=0;w=world}` | A fixed location; `w` (or `world`) is the world, default the caster's |
| `@TargetLocation` (`@TL`) | The location of the trigger/target entity |
| `@Owner`, `@Parent` | The entity whose `summon` created the caster. If there is none, the line is skipped |

All multi-target targeters (`@EntitiesNearOrigin`, `@EntitiesInRadius`, `@PlayersInRadius`, `@PIR`) also take `limit=<n>` and `sort=nearest|farthest|random`, and run the mechanic once per target. An unknown targeter is logged once and the line targets the default target.

## Variables

| | `var=name` | `var=caster.name` |
|---|---|---|
| Lives | for the current skill run, including the skills and inline skills it starts and across `delay` | as long as the mob, cleared when it is removed and on `/bettermob reload`, not saved over restarts |
| Read in a parameter | `<skill.name>` | `<var.name>` |

Names are letters, digits and `_` (up to 32 characters, case-insensitive), a scope holds at most 64 variables. Values are inserted as plain text; braces, brackets, `;`, `=`, quotes, `%` and control characters are removed so a variable can never add skill lines or trigger a command. An unset variable is inserted as `0`.

```yaml
Skills:
  - addvariable{var=caster.hits;value=1} @self ~onDamaged
  - skill{s=enrage} @self ~onDamaged ?variable{var=caster.hits;value=>=5}
```

## Conditions

| Condition | True when |
|---|---|
| `offgcd` | The global cooldown set with `gcd` has run out |
| `onground` | The caster stands on the ground |
| `sneaking` | The caster is a player who is sneaking |
| `health{h=<50%}` | The caster's health matches: a number, a comparison (`>10`, `<=5`), a range (`20-40`), each optionally with `%` for a share of max health |
| `lineofsight` (`los`) | The caster can see the trigger/target (or its own target) |
| `world{w=world,world_nether}` | The caster is in one of these worlds |
| `biome{b=DESERT,PLAINS}` | The caster stands in one of these biomes (names without `minecraft:` are fine) |
| `variable{var=hits;value=>=3}` | The variable matches: a number test (`>3`, `<=5`, `2-4`, `7`) when it holds a number, otherwise text equality. A missing variable counts as 0 for number tests |
| `sneaking` | The caster is a player and is sneaking |
| `time{t=day}` | `day` or `night` of the caster's world; also a tick or range, e.g. `time{t=0-6000}` |
| `chance{chance=0.75}` | A random roll succeeds (0 to 1) |
| `hastag{t=pet}` | The caster has the tag set by `addtag` |
| `faction{faction=Elite,Other}` | The caster (or, inside `@EntitiesNearOrigin`/`@EntitiesInRadius` conditions, each candidate) belongs to one of these factions. Set with `Faction:` in the mob file |
| `hasaura{n=spawn}` | The aura of that name is active on the caster |
| `onblock{b=GRASS_BLOCK}` | The block under the caster is one of these |
| `skillOnCooldown{skill=x}` | That skill's `Cooldown:` is still running for the caster |
| `distance{d=0-6}` | Distance to the trigger/target is in range (also `>3`, `<=5`) |
| `blocktype{type=OAK_LOG,SPRUCE_LOG}` | The obstructing block is one of these (used in `TargetConditions`) |

A skill can have `Cooldown: <seconds>` (per caster). A `Conditions`/`TargetConditions` entry ending in `castinstead <skill>` casts that skill instead when it holds. A line without a targeter uses the target of the line that called its skill. In parameters, `<caster.damage>` and `<caster.name>` are replaced.

Write `onground{} true` or `onground{} false` to require a value. Any mechanic line can end
with `?condition{...}`, or `?!condition{...}` to negate it.

Conditions BetterMob doesn't know are logged and treated as
**true**, so the line still runs.

## Not supported

Several MythicMobs mechanics, conditions and targeters. Mechanics inside aura
effects that aren't supported (for example `velocity`) produce a warning like
`Skill-Mechanic 'velocity' wird nicht unterstuetzt` and are skipped.
