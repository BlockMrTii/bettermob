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
    <version>v1.1.7</version> <!-- a release tag -->
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
    <version>1.1.7</version>
    <scope>provided</scope>
</dependency>
```

## Using the API

```java
BetterMobAPI api = BetterMobAPI.get();

api.getMobs();                                   // all registered mobs (MobInfo)
api.getMob("zombie_brute");                      // Optional<MobInfo>
api.spawn("zombie_brute", player.getLocation()); // Optional<LivingEntity>
api.isBetterMob(entity);                         // is this one of ours?
api.getMobInfo(entity);                          // Optional<MobInfo>
api.getSkillIds();                               // all skill IDs
api.runSkill("wumpus_wave", player);             // any LivingEntity as caster
api.runSkill("wumpus_wave", caster, trigger);    // with a trigger (target of @trigger)
api.reload();                                    // same as /bettermob reload, living mobs included
```

`MobInfo` is a record: `id`, `type`, `displayName`, `modelId`, `health`, `damage`.

## Events

`BetterMobSpawnEvent` and `BetterMobDeathEvent` (package `eu.northsoft.bettermob.api.event`).
Both expose the entity and its `MobInfo`.

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

Plugins that bring their own mobs or pets can register the other building blocks of a skill line too. This needs **BetterMob 1.1.7 or newer**: the published 1.1.7.x builds (and the Maven/JitPack coordinates pinned to them above) do not have these methods, so use the `dev-build` jar until 1.1.7 is released.

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

## Example plugin

[`examples/api-example`](https://github.com/HyperGaming99/bettermob/tree/main/examples/api-example) is a small plugin built on this API: it registers a `heal{amount=4}` mechanic, listens to `BetterMobSpawnEvent` and `BetterMobDeathEvent` and has a `/apiexample <mob>` command that spawns a mob.

```bash
mvn -f examples/api-example/pom.xml package
```

This resolves `com.github.HyperGaming99:bettermob:v1.1.7` from JitPack. To compile it against your own checkout instead:

```bash
mvn install -DskipTests
mvn -f examples/api-example/pom.xml -Plocal -Dbettermob.version=<version in pom.xml> package
```

The workflow builds it that way on every push and pull request, so it keeps compiling with the API.
