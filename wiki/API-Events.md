# API events

All events are in `eu.northsoft.bettermob.api.event`. They are normal Bukkit events: register a `Listener` and use `@EventHandler`. **Cancellable** events stop the thing they describe when you cancel them. BetterMob only creates an event object when a plugin listens to it, so unused events cost nothing. On Folia a mob event runs on the thread of the region that owns the mob; entities from other regions are not handed to listeners (see the notes below), because their state may only be read by their own region. Item, spawner and reload events run on the thread that fired them.

Most mob events have `getEntity()` (the `LivingEntity`) and `getMob()` (a `MobInfo` with `id`, `type`, `displayName`, `modelId`, `health`, `damage`).

## Mob events (`api.event.mob`)

| Event | Fired when | Extra getters |
|---|---|---|
| `BetterMobSpawnEvent` | A BetterMob mob was spawned (command, spawner, egg, API) | |
| `BetterMobLoadEvent` | A saved BetterMob mob was loaded again with its chunk | |
| `BetterMobDespawnEvent` | A mob is removed without dying (despawn, discard, plugin removal), before its state is released | `getCause()` |
| `BetterMobDeathEvent` | A BetterMob mob died | |
| `BetterMobDropEvent` | A mob died and its drops are known, before the death event | `getKiller()` (may be null), `getDrops()` (a list you can change), `getExp()` / `setExp(int)` |
| `BetterMobDamageEvent` *(cancellable)* | A mob is hit or hits something; one event per mob involved. `getDamage()` is the damage after the mob's `DamageModifiers` | `getOther()`, `isMobVictim()`, `getCause()`, `getDamage()` / `setDamage(double)` |
| `BetterMobTargetEvent` *(cancellable)* | A mob picks, changes or drops its target | `getOldTarget()`, `getNewTarget()` (null = target lost) |
| `BetterMobKillEvent` | A mob killed a living entity. On Folia it is only fired when killer and victim are in the same region | `getVictim()` |
| `BetterMobCombatEvent` | A mob enters combat (first target) or leaves it (no target for 5 seconds) | `isEntering()`, `getTarget()` |
| `BetterMobHealthEvent` | Damage or healing moved a mob across a health threshold of one of its `~onHealth` lines | `getBefore()`, `getAfter()`, `getThreshold()` (like `<50%`) |
| `BetterMobPhaseEvent` | A mob changes its phase (see [Mobs](Mobs#phases)) | `getFrom()`, `getTo()` (-1 = no phase) |
| `BetterMobSignalEvent` *(cancellable)* | A mob receives a signal that one of its `~onSignal` lines listens to | `getSignal()`, `getSender()` |
| `BetterMobGroupAlertEvent` *(cancellable)* | A damaged group member alerts its group | `getAttacker()`, `getMembers()` (a list you can change) |

## Skill events (`api.event.skill`)

| Event | Fired when | Getters |
|---|---|---|
| `BetterMobSkillEvent` *(cancellable)* | A named skill is about to run: after its `Conditions` and `TargetConditions` passed, before its `Cooldown` starts. Inline skills and single mechanic lines are not skills and fire nothing | `getSkillId()`, `getCaster()`, `getTrigger()` (may be null), `getMob()` (null when the caster is not a BetterMob mob) |
| `BetterMobTriggerEvent` *(cancellable)* | A mob trigger (`~onDamaged`, `~onTimer`, ...) is about to run its lines | `getEntity()`, `getMob()`, `getTrigger()` (name like `DAMAGED`), `getSource()` (the entity that caused it, may be null) |
| `BetterMobMechanicEvent` *(cancellable)* | One mechanic is about to run on one target, after its parameters were filled in | `getCaster()`, `getMechanic()` (lower case name), `getParams()` (read only), `getTarget()` (null when there is none or, on Folia, when it is in another region), `getMob()` (may be null) |

## Item events (`api.event.item`)

| Event | Fired when | Getters |
|---|---|---|
| `BetterMobItemUseEvent` *(cancellable)* | A player right-clicks with a BetterMob item, before its `~onUse` lines run | `getPlayer()`, `getItemId()`, `getItem()` |

## Spawner events (`api.event.spawner`)

| Event | Fired when | Getters |
|---|---|---|
| `BetterMobSpawnerSpawnEvent` *(cancellable)* | A spawner found a spot and is about to spawn its mob | `getSpawnerId()`, `getMobId()`, `getLocation()` |

## System events (`api.event.system`)

| Event | Fired when | Getters |
|---|---|---|
| `BetterMobReloadEvent` | `/bettermob reload` (or `api.reload()`) finished | `getChanges()` (per kind `mobs`, `skills`, `items`, `droptables`: lists `added()`, `removed()`, `changed()`), `getUpdatedMobs()` |

## Example

```java
@EventHandler
public void onDrop(BetterMobDropEvent event) {
    if (event.getKiller() != null && event.getMob().id().equals("boss")) {
        event.getDrops().add(new ItemStack(Material.DIAMOND));
        event.setExp(event.getExp() * 2);
    }
}

@EventHandler
public void onPhase(BetterMobPhaseEvent event) {
    if (event.getTo() == 1) Bukkit.broadcast(Component.text(event.getMob().displayName() + " is enraged!"));
}
```
