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
| `~onHealth<50%` | The mob's health crosses the threshold. Operators `<`, `<=`, `>`, `>=`, a number is absolute health, with `%` a share of max health. Fires once when it crosses and is armed again after the health has moved back across (healing). A missing threshold makes the line invalid |
| `~onTarget` | The mob acquires a target. `@trigger` is the target |
| `~onLoseTarget` | The mob loses its target |
| `~onEnterCombat` | The mob gets a target after having none (and was not in combat). `@trigger` is the target |
| `~onExitCombat` | The mob has had no target for 5 seconds after being in combat |
| `~onKill` | The mob killed a living entity (melee or projectile). `@trigger` is the victim |
| `~onSignal:<NAME>` | The mob received the signal `NAME` (case-insensitive) from the `signal` mechanic. `@trigger` is the entity that sent it, as long as it is in the same region as the receiver (on Folia a sender in another region is not passed, the skill then runs without a trigger) |
| `~onDespawn` | The mob was removed without dying: despawned, discarded or removed by a plugin or `/bettermob killall`. Not for deaths and not for chunk unloads. It runs before the mob's state is cleared, `@self` is the mob that is going away |
| `~onMove:<ticks>` | The mob moved more than 0.05 blocks since the last check, checked every `ticks` ticks (default 5). Only mobs with this trigger get a timer, nothing runs on mobs without it |
| `~onDeath` | The mob dies (the body still exists, so sounds and effects work) |
| `~onTimer:<ticks>` | Repeatedly, default every 20 ticks |
| `~onUse` | Items only: right-click with the item in the main hand |
| `~onSwing` (`~onLeftClick`) | Items only: the player swings the arm with the item in the main hand |
| `~onPressF` (`~onSwapHands`) | Items only: the player presses F (swap hands) with the item in the main hand. `cancelevent` stops the swap |
| `~onPickup` (`~onItemPickup`) | Items only: the player picks the item up. `cancelevent` stops it |
| `~onDrop` (`~onItemDrop`) | Items only: the player drops the item. `cancelevent` stops it |
| `~onEquip` | Items only: the player switches the hotbar to the item |

## Mechanics

| Mechanic | Parameters | What it does |
|---|---|---|
| `skill` | `s` (also `skill`, `skills`) | Run another skill, or inline lines: `skill{s=[ - sound{...} - delay 5 ]}` |
| `randomskill` | `s` (also `skills`): comma separated list | Run one random skill of the list |
| `sudoskill` | `s` | Run a skill with the **target** as caster |
| `sound` | `s` sound key, `p` pitch, `v` volume | Play a sound at the target |
| `model` | `mid` | Attach a BetterModel model |
| `modelengine` | `mid` | Attach a ModelEngine model |
| `state` (`animation`) | `state` (`s`, `a`) | Play a model animation: once for BetterModel; for ModelEngine also `lerpIn`, `lerpOut` (default 0.1), `speed` (1) and `force` (true) |
| `modifystate` | `s`, `sp` (speed, may be a calculation), `l` (`LOOP`, `ONCE`, `HOLD`) | Change speed and loop mode of an animation that is playing on the target's model (BetterModel replaces it with the new settings) |
| `defaultstate` | `t` (state: `idle`, `walk`, `death`, ...), `s` (animation), `li`, `lo`, `sp` | ModelEngine only: play animation `s` instead of the default one for state `t`. BetterModel has no default states, the line is logged and skipped |
| `tint` | `c` (hex `ff7c00` or `r,g,b`, default white), `p` (part), `em` | Tint the target's model, or only the parts named `p`. `em=false` matches every part whose name contains `p` |
| `glowbone` | `c`, `g` (default true), `p`, `em` | Let the model (or parts `p`) glow in color `c`; `g=false` turns it off |
| `partvis` (`partvisibility`) | `p`, `v` (default false), `em` | Hide (`v=false`) or show (`v=true`) the parts `p` of the model |
| `meg:setitemmodel` (`setitemmodel`) | `bone` (`p`), `i` (material or BetterMob item id, `air` clears) | Show an item on that bone (ModelEngine; BetterModel item bones) |
| `lockmodel` | `l` (default true) | Stop the model from turning with the entity; `l=false` unlocks it |
| `mountmodel` | `seat` (`p`, default `mount`), `controller` | Seat the target on a model's seat. BetterModel handles steering; with ModelEngine `controller` is `walking` (default), `flying`, `walking_force` or `flying_force`, and the driver seat is used when `seat` is not a passenger seat |
| `dismountallmodel` | | Take everyone BetterMob seated on the target's model (with `mountmodel`) off it. A rider is forgotten only after it really left (a plugin that cancels the dismount keeps the record) |
| `dismount` | | The target leaves the vehicle it rides |
| `dismountall` | | Take every passenger off the target, seated by a model or not: plain passengers are ejected and the riders BetterMob seated on its model are taken off too |
| `mounttarget` | `repeat`, `repeati` | Seat the target on the caster (plain vehicle passenger). `repeat` tries again that many more times (up to 40) every `repeati` ticks, and stops as soon as one try worked |
| `setrotation` | `yaw`, `pitch`, `relative` | Turn the target to that yaw/pitch (pitch is limited to -90..90). With `relative=true` the numbers are added to the current ones. `<caster.l.yaw>` / `<caster.l.pitch>` insert the caster's own, `<caster.l.x>` `<caster.l.y>` `<caster.l.z>` its position |
| `setpitch` | `p` | Set only the pitch (the yaw stays) |
| `setgravity` | `g` | `false` switches gravity off for the target, anything else on |
| `scale` | `s` | Scale a display entity (its transformation) or a living entity (the scale attribute), from 0.0625 to 16 |
| `brightness` | `b`, `s` | Block and sky light level 0-15 of a display entity |
| `setdisplayentityitem` | `i` | Put a BetterMob item id or a material on an item display |
| `potion` | `type`, `duration` (ticks), `level` | Apply an effect |
| `potionclear` (`removepotion`) | `type` | Remove the effects in `type` (a list with `,`), or all effects when `type` is missing |
| `look` | - | Turn the caster toward the target |
| `summon` | `type` (also `t`, `mob`) | Spawn another registered mob at the target |
| `remove` | - | Remove the target |
| `command` | `c`: command | Run a console command. `<caster.name>` and `<target.name>` are replaced |
| `takeitem` | `i` item ID, `a` amount | Take items from the target player |
| `setname` | `n` | Set the target's custom name (`&` colors); an empty name removes it |
| `giveitem` | `i`, `a` | Give the target player a BetterMob item id or material (`a` up to 64); what doesn't fit drops at their feet |
| `fakeexplosion` | | An explosion particle and sound at the target without any damage |
| `recoil` | `f` | Push the target backwards, away from where it looks (force -5 to 5, default 1) |
| `loot` | `table`, `vanilla`, `lootingmodifier`, `mode` | Roll a BetterMob drop table (`table=<id>`) or a vanilla loot table (`vanilla=minecraft:entities/zombie`, with the caster as the looted entity and `lootingmodifier` as looting level, default 0) and give the result to the target player; what doesn't fit is dropped at their feet. Drop-table exp goes to the player. With `mode=drop` (also `target=drop`) the items and exp drop at the target's location instead, and the target doesn't have to be a player. An unknown table is logged once and reported by `/bettermob validate`. Use it from an item's `~onUse` after `takeitem` |
| `killcount` | `var`, `mob` | Store a player's kill count (the target if it is a player, otherwise the trigger, otherwise the caster) (of `mob`, or all mobs without it) in a skill variable, e.g. `killcount{var=caster.kills;mob=goblin} @trigger`, to use it as `<var.kills>` |
| `signal` | `s` | Send the signal `s` to the target mob: its `~onSignal:<s>` skills run, with the caster as `@trigger`. A target that is not a BetterMob mob or has no matching line does nothing. Example: `signal{s=HIT} @target` |
| `breakblock` | `usetool` | Break the target block |
| `setblock` | `m` material | Replace the block at the target |
| `damage` | `amount` | Damage the target with the caster as attacker. Does not fire `~onAttack` again |
| `throw` | `velocity`, `velocityY` | Fling the target away from the caster (`velocity` and `velocityY` are both divided by 10) |
| `lunge` | `velocity`, `velocityY` | The caster dashes toward the target |
| `gcd` | `ticks` | Start the global cooldown checked by the `offgcd` condition |
| `spin` | `duration` ticks, `velocity` degrees per tick | Spin the target |
| `effect:particles` (`e:p`) | `p`, `amount`, `hS`, `vS`, `speed`, `y`, `repeat`, `repeatInterval` | Spawn particles. `y` shifts them up; `repeat` spawns them that many more times every `repeatInterval` ticks, following a moving target (entity, `@ModelPart`, `@Forward`) |
| `effect:particlering` | `particle`, `radius`, `points`, `amount`, `hS`, `vS` | Ring of particles |
| `effect:particleline` (`particleline`) | `particle`, `points` or `density`, `fy`, `y` | Particles on a line from the caster (chest height, `fy` shifts it) to the target (`y` shifts its end). `density` is points per block (default 2), `points` fixes the count; at most 500 points. Particle options are the same as for `effect:particles`, except `repeat` and `repeatInterval`, which the line, beam, sphere and helix ignore |
| `effect:particlebeam` (`particlebeam`) | as `effect:particleline`, plus `d` | The same line, drawn from the caster towards the target over `d` ticks (default 10, at most 100). The end points are taken when the mechanic starts |
| `effect:particlesphere` (`particlesphere`) | `particle`, `radius`, `points`, `y` | Evenly spread particles on a sphere around the target (radius 1.5, 40 points by default) |
| `effect:particlehelix` (`particlehelix`) | `particle`, `radius`, `height`, `turns`, `points`, `y` | A spiral rising from the target's location (radius 1, height 2, 3 turns, 60 points by default) |
| `glow` | `d` | Make the target glow for `d` ticks (default 40), then restore its previous glow state |
| `equip` | `item=<item>:<slot>` | Put a registered item or material into a slot: `HAND` (default), `OFFHAND`, `HEAD`, `CHEST`, `LEGS`, `FEET`. The slot's drop chance is set to 0 |
| `addtag` / `removetag` | `t` | Mark the target, check it with `hastag` |
| `onDamaged`, `onAttack`, `onDeath`, `onShoot`, `aura` | `auraName`, `time`, `cE`, `oS`, `oE`, `oT`, `i`, `oH` | Put a named aura on the target for `time` ticks (forever without it). `hasaura` sees it. `oS=[ ... ]` runs at the start, `oE` at the end, `oT` every `i` ticks (default 20), `oH` each time the matching event happens to that entity (`onDamaged` = it is hit, `onAttack` = it hits, ...). `cE=true` cancels that event while the aura is active |
| `bodyrotation` | `headUneven`, `bodyUneven`, `minHead`, `maxHead`, `minBody`, `maxBody`, `stable`, `duration`, `delay` | Set how far the model's head and body may turn apart (BetterModel and ModelEngine) |
| `actionbar` (`actionmessage`) | `m`, `d` | Show text above the hotbar of the target player. It stays about 2 seconds; with `d` (ticks) it is repeated until `d` has passed |
| `title` (`sendtitle`) | `t`, `st`, `fi`, `d`, `fo` | Show a title (`t`) and subtitle (`st`) with fade in `fi` (default 10 ticks), stay `d` (70) and fade out `fo` (20) |
| `bossbar` | `m`, `color`, `style`, `p`, `d`, `countdown` | Show a boss bar with the text `m` to the target player for `d` ticks (default 100), then remove it. `color` is `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE` (default) or `WHITE`; `style` is `SOLID` (default), `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12` or `SEGMENTED_20`; `p` is the start progress 0 to 1; `countdown=true` makes it shrink to empty over `d` |
| `message` | `m` | Send a message to the target player. The text rules below apply to `message`, `actionbar`, `title` and `bossbar`: `&` colors, `<caster.name>`, `<target.name>` and PlaceholderAPI placeholders work |
| `jsonmessage` (`jsonmsg`) | `m` (JSON text component) | Send a JSON chat component (click and hover events work) to the target player. `<target.name>` and `<caster.name>` are inserted escaped; invalid JSON is sent as a normal message |
| `speak` | `m`, `yo` (height above the head, default 0.5), `d` (ticks, default 60), `chat` (default true) | Show `m` as a speech bubble (text display) above the caster and, unless `chat=false`, send `Name: m` to the target players. A new bubble replaces the old one |
| `onchat` | `onchat` (skill id), `d` (ticks to wait, default 200), `cancel` (default true) | Wait for the target player's next chat message, then run that skill with the text as `<skill.var.input>` (plain text, at most 256 characters, skill syntax removed). The message is hidden from chat unless `cancel=false` |
| `blackscreen` | `d` (ticks, default 20) | Blind the target player (blindness and darkness) for `d` ticks |
| `setitemnbt` | `key`, `value` | Store a text value on the item in the target's main hand; read it back with `<caster.itemnbt.key>` |
| `setlore` | `l` (`"line 1","line 2"` or `line 1\|line 2`) | Replace the lore of the item in the target's main hand (`&` colors, placeholders work) |
| `consumeslot` (`consumehelditem`) | `amount` (`a`, default 1) | Take items from the stack in the target's main hand |
| `settargetscore` | `objective`, `value` (calculation allowed) | Set the target's score in that main-scoreboard objective (players by name, other entities by UUID). Does nothing on Folia |
| `ignite` | `t` | Set the target on fire for `t` ticks |
| `setvariable` (`variableset`, `setvar`) | `var`, `value` (`val`), `type`, `duration` | Store a value: `var=hits` for the current skill run, `var=caster.hits` for the caster. `type` is `INTEGER`, `FLOAT` or `STRING` (default), numbers are checked. See [Variables](#variables) |
| `variablemath` | `var`, `equation` | Store the result of plain arithmetic (`+ - * / %` and parentheses, numbers only, no functions): `variablemath{var=caster.scale;equation="<var.scale> + (5 - <var.scale>) * 0.1"}`. An invalid equation or a division by zero changes nothing and logs a warning |
| `variablesubtract` (`subtractvariable`) | `var`, `value` (`amount`) | Subtract from a number, the opposite of `addvariable` |
| `variableunset` (`unsetvar`, `unsetvariable`) | `var` | Remove the variable |
| `addvariable` (`variableadd`) | `var`, `value` | Add a number to a variable (a missing one counts as 0) |
| `heal` | `a` | Heal the target by `a` health, capped at its max health |
| `teleportto` (`tpt`) | `location` (`x,y,z[,world]`, calculations allowed, or a targeter like `@TriggerLocation{fo=6;yo=0.25}`) | Move the target there; it keeps its facing |
| `teleporty` (`tpy`) | `y` (calculation allowed, e.g. `<caster.l.y>+5`) | Move the target to that height, same x and z |
| `pull` | `velocity` (`v`, default 1), `origin` (a targeter like `@Forward{...}`) | Pull the target towards the caster, or towards `origin`. The speed grows with the distance (`v/10` per block) and is capped at 4 blocks per tick |
| `propel` | `velocity` (`v`, default 10) | Push the caster towards the target at `v/10` blocks per tick (at most 4); when the target is the caster itself, along its facing |
| `teleport` | - | Move the caster to the targeted location or entity (for example `@Target`, `@Location{...}`, `@Forward{...}`); the caster keeps its facing |
| `explosion` | `yield`, `bd`, `fire` | Explosion at the target. `yield` is the power (default 2), `bd=true` breaks blocks, `fire=true` sets fires. Off by default |
| `effect:firework` (`firework`) | `type` (`t`: `BALL`, `BALL_LARGE`, `STAR`, `BURST`, `CREEPER`), `color` (`c`, hex list), `fadecolor` (`fc`), `flicker` (`f`), `trail` (`tr`), `duration` (`d`, ticks before it bursts, default 0) | A firework burst at the target. It never deals damage |
| `effect:blockmask` (`blockmask`) | `material` (`m`), `radius` (`r`, at most 8), `duration` (`d` ticks), `shape` (`s=sphere`, default cube), `onlyair` (`oa`), `noair` (`na`) | Show the blocks around the target as `m` to players within 64 blocks, only for them and only for `d` ticks (for example `m=light` for a moving light) |
| `setchunkforceloaded` | `l` (default true) | Keep the target's chunk loaded (`l=false` releases it) |
| `displaytransformation` | `action` (`SET`, `ADD`, `MULTIPLY`), `transformation` (`scale` or `translation`), `value` (`x,y,z`, calculations allowed) | Change a display entity's scale or translation |
| `raytraceto` | `es` (skill id or inline skills), `syo`, `tyo` (height offsets at caster and target), `rw` (ray width) | Trace a line from the caster to the target location and run `es` for every living entity the line passes through (that entity is the trigger) |
| `lightning` (`fakelightning`) | `damage` | Lightning at the target; only the visual effect unless `damage=true` (`fakelightning` never damages) |
| `setspeed` | `s` | Set the target's movement speed attribute (zombie default is about 0.23) |
| `setai` | `ai` | `ai=false` switches the target mob's AI off, `ai=true` back on |
| `stun` | `d`, `ai`, `g`, `f`, `state` | Stun the target mob for `d` ticks: `ai` (default true) turns its AI off, `g=true` also turns gravity off, `f=true` holds it still (velocity 0 every tick), `state=<animation>` plays that BetterModel animation once (same as the `state` mechanic) |
| `velocity` | `m`, `x`, `y`, `z`, `repeat`, `repeatInterval` | Change the target's velocity. `m` is `SET` (default), `ADD`, `MULTIPLY` or `DIVIDE`; `repeat` applies it that many more times, every `repeatInterval` ticks |
| `freeze` | `ticks` | Freeze the target (powder-snow overlay) for `ticks` ticks, default 140 |
| `setNoDamageTicks` | `ticks` | Set the target's invulnerability ticks (0 = can be hit again at once) |
| `shoot` | `type`, `velocity`, `speedscale`, `damage`, `spread`, `gravity`, `oh`, `oe`, `ot`, `i` | Fire a projectile at the target. `type` is `arrow` (default), `spectral_arrow`, `trident`, `snowball`, `egg`, `fireball` or `smallfireball` (fireballs don't explode or burn; they deal `damage`). `spread` is a random cone in degrees, `gravity=false` makes it fly straight. `speedscale` (`ss`, default 2) multiplies `velocity`; a value that is not a number falls back to 2. `oh=[ ... ]` runs on a hit with the hit entity as target, `oe=[ ... ]` when it ends anywhere (at its location), `ot=[ ... ]` every `i` ticks (default 5) while it flies |
| `projectile` | `model`, `speed`, `range`, `radius`, `damage`, `homing`, `bounce`, `oh`, `oe`, `ot`, `i` | Fly an invisible body carrying a BetterModel or ModelEngine model (`model=<id>`, optional) toward the target, or where the caster looks. `speed` is blocks per second (default 20), `range` the distance in blocks (default 30, max 200), `radius` the hit size (default 1). The first living entity inside `radius` (not the caster) takes `damage` and ends the flight; `oh=[ ... ]` runs with it as target. `homing=true` steers towards the target (8° per tick). `bounce=2` reflects off that many blocks, the next block ends it. `oe=[ ... ]` runs where it ends (hit, block or range), `ot=[ ... ]` every `i` ticks (default 5). It is removed when the caster dies, without `oe` |
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
| `@Forward{f=1.5;uel=true;yoffset=-1;rotate=-22}` | A point in front of the caster. `f` distance, `uel` start at eye height, `yoffset` (`y`) vertical shift, `rotate` degrees sideways (positive = right), `sideoffset` (`so`) blocks sideways |
| `@SelfLocation{x=0;y=-1;z=0}` | The caster's position, optionally shifted |
| `@ObstructingBlock` | The block the caster is looking at (up to 2.5 blocks) |
| `@EntitiesNearOrigin{r=4;Conditions=[ - isPlayer{} true - isCaster{} false]}` (`@ENO`) | Every entity within `r` of a totem's location, closest first. Conditions: `isPlayer`, `isCaster`, `isMob`, `hasTag{t=...}`, `faction{faction=...}` and every skill condition (see below). Skill conditions test the candidate, not the caster: `health{h=<50%}` is the candidate's health, `distance{d=<3}` and `lineofsight` measure between the candidate and the caster |
| `@EntitiesInRadius{r=4}` (`@EIR`, `@LivingEntitiesInRadius`, `@LEIR`) | Same, around the caster |
| `@PlayersInRadius{r=10}` | Every player within `r` of the caster |
| `@ModelPart{p=tnt2}` | The position of that bone of the mob's BetterModel or ModelEngine model (falls back to chest height) |
| `@LivingInCone{a=90;r=16;rot=0;targetplayers=true}` (`@LIC`) | Every living entity except the caster within `r` blocks (default 16) and within a horizontal cone of `a` degrees (default 90) around the caster's facing, turned by `rot` degrees. `targetplayers=false` leaves players out. `y` is accepted and ignored, since the cone is horizontal |
| `@PIR{r=2}` | The nearest player within `r` blocks (`limit` defaults to 1). If nobody is there, the line is skipped |
| `@Origin` | A totem's location, or the caster's location outside a totem |
| `@Location{x=0;y=64;z=0;w=world}` | A fixed location; `w` (or `world`) is the world, default the caster's |
| `@TargetLocation` (`@TL`, `@TriggerLocation`) | The location of the trigger/target entity, optionally moved by `fo` (forward), `so` (sideways, positive = right) and `yo` (up) along its facing |
| `@Owner`, `@Parent` | The entity whose `summon` created the caster. If there is none, the line is skipped |
| `@ModelPassengers{p=driver;mid=model;conditions=[...]}` | The entities BetterMob seated on the caster's model with `mountmodel`, optionally only one seat `p` and only when the caster's mob uses model `mid`. Riders the model plugin seated itself are found too: the mount manager's seats with ModelEngine, the mounted hit boxes with BetterModel (seat name = the bone group name) |
| `@mountedmodel` | The model owner the caster rides: seated with `mountmodel`, or by the model plugin itself (ModelEngine's mount pairs, BetterModel's mounted hit boxes; BetterModel is searched once per second at most per rider) |
| `@children{target=MARKER;r=32}` | Entities summoned by the caster (`summon`) within `r` blocks (default 32), optionally only of that entity type |

All multi-target targeters (`@EntitiesNearOrigin`, `@EntitiesInRadius`, `@PlayersInRadius`, `@LivingInCone`, `@PIR`) also take `limit=<n>` and `sort=nearest|farthest|random`, and run the mechanic once per target. An unknown targeter is logged once and the line targets the default target.

## Arguments

A skill can take values. Every parameter of `skill{s=name;...}`, `sudoskill{...}` and `randomskill{...}` other than `s`, `skill`, `skills`, `sync`, `delay` and `cd` is passed to the called skill; `<arg.name>` in its mechanic parameters (also in its inline skills) is replaced by the value. Defaults go into an `Arguments:` section of the skill.

```yaml
heal_pulse:
  Arguments:
    amount: 2
  Skills:
    - heal{amount=<arg.amount>} @self
```

`skill{s=heal_pulse;amount=6} @self` heals 6, without `amount` it heals 2.

- Arguments are not handed on to the skills that skill calls; repeat them explicitly, for example `skill{s=other;amount=<arg.amount>}`.
- An argument that was not passed and has no default stays as `<arg.name>` and is logged once.
- Names are case-insensitive. Values are inserted as plain text: `{ } [ ] ; = " ' \ %` and control characters are removed, so an argument can never add skill lines or trigger a command.
- The variables of the calling skill run are shared with the called skill, see [Variables](#variables).

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
| `varequals{var;val}` (`variableequals`) | Same test as `variable` |
| `variableisset{var}` | The variable exists |
| `variableinrange{var;range=0.5to2}` | The number is in the range (`0.5to2`, `>3`, `<=5`, `2-4`); a text value never matches |
| `stringeq{val1;val2}` (`stringequals`) | Both texts are equal after the placeholders are inserted (see "Placeholders in condition parameters" below) |
| `sneaking` | The caster is a player and is sneaking |
| `region{r=spawn,arena}` | WorldGuard: the caster, or the target of the line, stands in one of these regions (ids, case-insensitive). Never true without WorldGuard |
| `modelhaspassengers{p=driver}` | BetterMob seated someone on the entity's model (on seat `p`, if given) |
| `ridingmodel{mid=model}` | The entity rides a model, seated by `mountmodel` or by the model plugin (of mob model `mid`, if given) |
| `mounted` | The entity rides a model or any other vehicle |
| `crouching` | Same as `sneaking` (also usable in targeter `conditions=[...]`) |
| `day` | It is day in the caster's world |
| `gamemode{gm=SURVIVAL,ADVENTURE}` | The caster is a player in one of these game modes |
| `outside` | Nothing is above the caster up to the sky |
| `holding{m=DIAMOND_SWORD}` | The caster holds this material or BetterMob item id in the main hand (a list with `,` works) |
| `moving` (`ismoving`) | The caster's velocity is above a small limit; players often report no velocity, so use it for mobs |
| `playerwithin{d=10;count=2}` (`playersinradius`) | At least `count` players (default 1) are within `d` blocks; `count` also takes `<3` or `1-2` |
| `fieldofview{a=90}` | The target (or trigger) is within a cone of `a` degrees in front of the caster |
| `score{o=objective;v=3}` | The target's (or trigger's) score in that main-scoreboard objective matches `v` (also `>3`, `<=5`, `1-3`). Players are looked up by name, other entities by UUID. `o` and `v` take variable placeholders. Always false on Folia, which has no main scoreboard |
| `killedmob{mob=goblin;count=10}` | The player has killed at least 10 of that mob (the kill counters, see `/bettermob top`). `count` is a number (at least), a comparison (`<5`) or a range (`3-6`); default 1. The player is the checked target, otherwise the trigger, otherwise the caster |
| `killedmobs{count=50}` | The same for the kills of all BetterMob mobs together |
| `time{t=day}` | `day` or `night` of the caster's world; also a tick or range, e.g. `time{t=0-6000}` |
| `chance{chance=0.75}` | A random roll succeeds (0 to 1) |
| `hastag{t=pet}` | The caster has the tag set by `addtag` |
| `faction{faction=Elite,Other}` | The caster (or, inside `@EntitiesNearOrigin`/`@EntitiesInRadius` conditions, each candidate) belongs to one of these factions. Set with `Faction:` in the mob file |
| `hasaura{n=spawn}` | The aura of that name is active on the caster |
| `onblock{b=GRASS_BLOCK}` | The block under the caster is one of these |
| `skillOnCooldown{skill=x}` | That skill's `Cooldown:` is still running for the caster |
| `distance{d=0-6}` | Distance to the trigger/target is in range (also `>3`, `<=5`) |
| `owner` | The target (or trigger) is the caster's owner: the entity whose `summon` created it, or the owner of a tamed caster. Usually written as `?~owner` |
| `mythicmobtype{t=goblin,orc}` (`mobtype`) | The target (or the caster) is a BetterMob mob with one of these ids; inside targeter `conditions=[...]` it tests the candidate |
| `boundingboxesoverlap` | The hitboxes of the caster and the target (or trigger) overlap; inside targeter `conditions=[...]` it tests the candidate against the caster |
| `blocktype{type=OAK_LOG,SPRUCE_LOG}` | The obstructing block is one of these (used in `TargetConditions`) |

A skill can have `Cooldown: <seconds>` (per caster). A `Conditions`/`TargetConditions` entry ending in `castinstead <skill>` casts that skill instead when it holds. A line without a targeter uses the target of the line that called its skill. In parameters, `<caster.damage>`, `<caster.name>`, `<caster.mythic_type>` (the caster's BetterMob id, or its entity type) and `<caster.itemnbt.key>` (a value set with `setitemnbt` on the caster's main-hand item, empty when missing) are replaced.

Write `onground{} true` or `onground{} false` to require a value. Any mechanic line can end
with `?condition{...}`, or `?!condition{...}` to negate it. `?~condition{...}` (and `?!~condition{...}`) tests the trigger instead
of the caster; without a trigger the line is skipped.

Conditions can be combined in brackets with `||` (or) and `&&` (and, binds stronger), also nested, and the whole group can take
`true`/`false` and `castinstead`:

```yaml
Conditions:
  - ( varequals{var=caster.item;val=banana} || varequals{var=caster.item;val=shell} ) true
  - ( variableisset{var=caster.speed} false || variableisset{var=caster.top} false ) false
```

Conditions BetterMob doesn't know are logged and treated as
**true**, so the line still runs.

## Not supported

Several MythicMobs mechanics, conditions and targeters. Mechanics inside aura
effects that aren't supported (for example `velocity`) produce a warning like
`Skill-Mechanic 'velocity' wird nicht unterstuetzt` and are skipped.

`effect:particle` is an alias of `effect:particles`.

Particles that need data take it from the same line: `material` (`m`) for `block`, `falling_dust`, `block_marker` and `item`; `color` (`#rrggbb` or `r,g,b`) and `size` for `dust`, plus `toColor` for `dust_color_transition`; `color` for `entity_effect`; `color` and `power` for `effect` and `instant_effect`. A `material` that is not a block falls back to stone for block particles. `spread` sets `hS` and `vS` at once, `hSpread` and `vSpread` are aliases. Legacy names like `block_crack`, `reddust` and `spell_mob` are mapped to the current particles.

BetterMob does not support `segment{m;p;extend}`. In ModelEngine R4, the `segment` mechanic configures existing segment or tail bones and their properties, such as bounds, align rate and elasticity; the model defines the bone chain. `animation{a=walk}` is supported as an alias of `state`.

## Calling a skill by its name

A line whose first word is not a mechanic but the id of a skill calls that skill, like MythicMobs: `my_skill @target ~onTimer:100` does the same as `skill{s=my_skill} @target`. Parameters on the line become [skill arguments](#arguments). A line that is neither a mechanic nor a skill logs one warning per name, not one per run, so a broken line can no longer fill the console.
