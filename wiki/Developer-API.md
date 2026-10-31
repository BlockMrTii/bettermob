# Developer API

BetterMob registers a `BetterMobAPI` Bukkit service. Add `depend: [BetterMob]` (or
`softdepend`) to your `plugin.yml` and compile against the jar with `provided` scope.
Everything public is in `eu.northsoft.bettermob.api`.

## Dependency

**JitPack** (no login):

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.HyperGaming99</groupId>
    <artifactId>bettermob</artifactId>
    <version>v1.2.0</version> <!-- a release tag -->
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
    <version>1.2.0</version>
    <scope>provided</scope>
</dependency>
```

### Dev version

The newest state of the `dev` branch has a Maven artifact too, so you can build against API changes before they are released:

```xml
<dependency>
    <groupId>com.github.HyperGaming99</groupId>
    <artifactId>bettermob</artifactId>
    <version>dev-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

- `dev-SNAPSHOT` is the head of `dev`. JitPack builds it on the first request, which can take a few minutes. Maven checks snapshots about once a day; `mvn -U` forces a fresh download. For a build that never changes, use the hash of a `dev` commit as the version instead.
- The matching plugin jar for the test server is the rolling pre-release [`dev-build`](https://github.com/HyperGaming99/bettermob/releases/tag/dev-build): every push to `dev` replaces it. Run your plugin against that jar when you compile against `dev-SNAPSHOT`.
- The dev API can change between commits. Do not release a plugin that needs `dev-SNAPSHOT`; wait for the tagged release and depend on that.

## Using the API

```java
BetterMobAPI api = BetterMobAPI.get();

api.getMobs();                                   // all registered mobs (MobInfo)
api.getMob("zombie_brute");                      // Optional<MobInfo>
api.spawn("zombie_brute", player.getLocation()); // Optional<LivingEntity>
api.spawnAsync("zombie_brute", location);        // CompletableFuture<Optional<LivingEntity>>
api.isBetterMob(entity);                         // is this one of ours?
api.getMobInfo(entity);                          // Optional<MobInfo>
api.getSkillIds();                               // all skill IDs
api.runSkill("wumpus_wave", player);             // any LivingEntity as caster
api.runSkill("wumpus_wave", caster, trigger);    // with a trigger (target of @trigger)
api.reload();                                    // same as /bettermob reload, living mobs included
```

`spawn` must run on the thread that owns the location (on Folia the region, on Paper the main thread); otherwise it logs a warning and returns an empty `Optional`. From other threads, use `spawnAsync` (returns a `CompletableFuture`).

`MobInfo` is a record: `id`, `type`, `displayName`, `modelId`, `health`, `damage`.

## Events

Every event lives in a package that says what it is about: `eu.northsoft.bettermob.api.event.mob`, `.skill`, `.item`, `.spawner` and `.system`. All of them, with their getters and the moment they fire, are on the [API events](API-Events) page. No event object is created while nobody listens to it.

```java
@EventHandler
public void onDamage(BetterMobDamageEvent event) {
    if (event.isMobVictim() && event.getOther() instanceof Player) event.setDamage(event.getDamage() * 0.5);
}
```

## Package layout

| Package | Contents |
|---|---|
| `eu.northsoft.bettermob.api` | `BetterMobAPI` (the service) and `MobInfo` |
| `eu.northsoft.bettermob.api.custom` | `CustomMechanic`, `CustomCondition`, `CustomTargeter`, `CustomPlaceholder` and their context records `MechanicContext`, `ConditionContext`, `TargeterContext`, `PlaceholderContext` |
| `eu.northsoft.bettermob.api.event.mob` | Events about a mob: spawn, load, despawn, death, damage, target, kill, combat, health, phase, signal, group alert, drop |
| `eu.northsoft.bettermob.api.event.skill` | Skill, trigger and mechanic events |
| `eu.northsoft.bettermob.api.event.item` | Item events |
| `eu.northsoft.bettermob.api.event.spawner` | Spawner events |
| `eu.northsoft.bettermob.api.event.system` | The reload event |

**Moved in this version:** before 1.2.0 the custom types and the contexts sat directly in `eu.northsoft.bettermob.api` and the four old events in `eu.northsoft.bettermob.api.event`. Change your imports (`api.CustomMechanic` -> `api.custom.CustomMechanic`, `api.event.BetterMobDamageEvent` -> `api.event.mob.BetterMobDamageEvent`, `api.event.BetterMobSkillEvent` -> `api.event.skill.BetterMobSkillEvent`, and so on) and rebuild; a plugin compiled against the old packages does not load the new classes.

## Custom mechanics

Register a mechanic and use it in skill lines like any built-in one:

```java
api.registerMechanic(this, "heal", ctx -> {
    double amount = Double.parseDouble(ctx.params().getOrDefault("amount", "2"));
    if (ctx.target() instanceof LivingEntity living) living.heal(amount);
});
```

```yaml
Skills:
  - heal{amount=4} @self ~onDamaged
```

- Built-in mechanics can't be overridden: `registerMechanic` returns `false`.
- `unregisterMechanic(name)` removes one. Your mechanics are removed automatically when your
  plugin is disabled.
- The `MechanicContext` gives you `caster`, `trigger`, the cancellable `event`, the resolved
  `target` entity and `location`, and the line's `params` (keys are lower case).
- An exception thrown by your mechanic is caught and logged, it doesn't stop the skill.

## Custom conditions, targeters and placeholders

Plugins that bring their own mobs or pets can register the other building blocks of a skill line too. This needs **BetterMob 1.1.8 or newer**: the published 1.1.8.x builds (and the Maven/JitPack coordinates pinned to them above) do not have these methods, so use the `dev-build` jar until 
is released.

```java
api.registerCondition(this, "petlevel", ctx -> level(ctx.caster()) >= Integer.parseInt(ctx.params().getOrDefault("min", "1")));
api.registerTargeter(this, "petowner", ctx -> List.of(ownerOf(ctx.caster())));
api.registerPlaceholder(this, "pet", (key, ctx) -> key.equals("health") ? String.valueOf(ctx.caster().getHealth()) : null);
```

```yaml
Skills:
  - heal{amount=<pet.health>} @PetOwner ~onTimer:100 ?petlevel{min=3}
```

| Registration | Interface and context | Notes |
|---|---|---|
| `registerCondition(owner, name, condition)` | `CustomCondition#test(ConditionContext)` with `caster`, `trigger`, the optional `target` entity and `location` (set when the condition is tested against a target, e.g. a radius targeter candidate or `TargetConditions`) and the line's `params` (lower-case keys) | Usable in `Conditions`, `TargetConditions`, after `?` on a line and inside targeter `Conditions=[ ... ]` (there the candidate is the `caster`) |
| `registerTargeter(owner, name, targeter)` | `CustomTargeter#resolve(TargeterContext)` returns the entities, with `caster`, `trigger`, `origin` and `params` | The mechanic runs once per returned entity; return an empty list to skip the line |
| `registerPlaceholder(owner, namespace, placeholder)` | `CustomPlaceholder#resolve(key, PlaceholderContext)` returns the text, or `null` to leave `<namespace.key>` as it is | `caster` and `target` are reserved. Values are inserted as plain text: `{ } [ ] ; = " ' \ %` and control characters are removed, so a value can't add skill lines or trigger a command |

- Each `register...` returns `false` when the name is a built-in or already taken; `unregisterCondition`, `unregisterTargeter` and `unregisterPlaceholder` remove one. Everything you registered is removed when your plugin is disabled.
- An exception thrown by your code is caught and logged. A failing condition counts as not met, a failing targeter as no target, a failing placeholder keeps its text and is logged with the namespace and your plugin.
- `/bettermob validate` knows registered names once your plugin is enabled.

## Custom triggers

An addon that runs `~onWave`-style lines itself can tell BetterMob about the trigger so it stops warning "could not be parsed" for those lines. This needs **BetterMob 1.1.9 or newer**.

```java
api.registerTrigger(this, "onWave");
```

- `registerTrigger(owner, name)` takes the name with or without `on` and returns `false` for a built-in trigger or a name that is already taken. `unregisterTrigger(name)` removes one; an addon's triggers are removed when it is disabled.
- BetterMob only accepts the lines when loading mobs and items and in `/bettermob validate`; it never runs them. Running them is up to the addon.
- Warnings for unparsed trigger lines are held until the first server tick, so an addon that registers in its own `onEnable` is already known by then.

## Example plugin

[`examples/api-example`](https://github.com/HyperGaming99/bettermob/tree/main/examples/api-example) is a small plugin built on this API: it registers a `heal{amount=4}` mechanic, a `?daytime` condition, a `@NearestPlayer` targeter and an `<example.world>` placeholder, listens to `BetterMobSpawnEvent` and `BetterMobDeathEvent` and has a `/apiexample <mob>` command that spawns a mob. `ModernEvents` listens to `BetterMobDamageEvent` and `BetterMobSkillEvent`.

```bash
mvn -f examples/api-example/pom.xml package
```

This resolves `com.github.HyperGaming99:bettermob:v1.2.0` from JitPack, so it needs the released 1.2.0 (the API packages changed in that version). Before that release exists, or to compile it against your own checkout, use the local profile:

```bash
./gradlew publishToMavenLocal -x test
mvn -f examples/api-example/pom.xml -Plocal -Dbettermob.version=<version in build.gradle.kts> package
```

The workflow builds it that way on every push and pull request, so it keeps compiling with the API.
