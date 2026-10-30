<h1 align="center">BetterMob</h1>

<p align="center">
  <b>Custom mobs in YAML, with 3D models, skills and boss fights.</b><br>
  A lightweight mob plugin for Paper and Folia that runs MythicMobs-style content.
</p>

<p align="center">
  <a href="https://modrinth.com/plugin/bettermob"><img alt="Modrinth" height="48" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>
  <a href="https://hangar.papermc.io/HyperGaming99/BetterMob/"><img alt="Hangar" height="48" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/hangar_vector.svg"></a>
  <a href="https://github.com/HyperGaming99/bettermob/releases/latest"><img alt="GitHub" height="48" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg"></a>
  <a href="https://github.com/HyperGaming99/bettermob/wiki"><img alt="Documentation" height="48" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_vector.svg"></a>
</p>

<p align="center">
  <a href="#-features">Features</a> ·
  <a href="#-quick-start">Quick start</a> ·
  <a href="#-mythicmobs-compatibility">MythicMobs</a> ·
  <a href="#-for-developers">Developers</a> ·
  <a href="#-addons">Addons</a> ·
  <a href="https://github.com/HyperGaming99/bettermob/wiki">Wiki</a>
</p>

---

## 🎯 Goals & Milestones

<p align="center">
  <img alt="20 Servers Reached" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/servers-20_unlocked_vector.svg">
  <img alt="40 Servers Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/servers-40_locked_vector.svg">
  <img alt="100 Servers Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/servers-100_locked_vector.svg">
  <img alt="500 Servers Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/servers-500_locked_vector.svg">
  <img alt="1k Servers Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/servers-1k_locked_vector.svg">
</p>

<p align="center">
  <img alt="100 Downloads Reached" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/downloads-100_unlocked_vector.svg">
  <img alt="200 Downloads Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/downloads-200_locked_vector.svg">
  <img alt="500 Downloads Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/downloads-500_locked_vector.svg">
  <img alt="1k Downloads Goal" height="48" src="https://raw.githubusercontent.com/HyperGaming99/bettermob/docs/goals-badges/assets/badges/downloads-1k_locked_vector.svg">
</p>

---

## ✨ Features

| | |
|---|---|
| 🐷 **Mobs in YAML** | Any vanilla type as the body, with health, damage, AI goals and options. One mob per file or many per file. |
| 🎭 **3D models** | Attach [BetterModel](https://modrinth.com/plugin/bettermodel) or [ModelEngine](https://www.spigotmc.org/resources/model-engine-4.108821/) models, including rideable seats with real WASD control. |
| ⚡ **Skill engine** | Triggers, mechanics, conditions and targeters with `delay`, `cd` and inline `?condition` on every line. Variables, skill arguments, inline skills. |
| 👹 **Boss fights** | Health phases, boss bars, patrol and guard behaviour, groups that call for help, leaders and followers, threat tables. |
| 🎴 **Items and loot** | Custom items with right-click, swing and pickup skills, drop tables, vanilla loot tables, spawn eggs, kill counters and top lists. |
| 🗺️ **World** | Natural spawn rules, spawners, [WorldGuard](https://enginehub.org/worldguard) regions and a spawn flag. |
| 📦 **Packs** | Drop a content pack into `packs/`, validate it with `/bettermob validate` and switch it off with one line. |
| 🧰 **Tools** | `/bettermob test`, aggro lines, a live reload report, skill profiling, JSON schemas for editors. |
| 🔌 **Developer API** | Custom mechanics, conditions, targeters, placeholders and a long list of events. |

BetterMob runs on **Folia** as well as Paper.

## 🚀 Quick start

1. Put `BetterMob.jar` into `plugins/` (BetterModel or ModelEngine is optional) and start the server.
2. Create `plugins/BetterMob/mobs/wumpus.yml`:

```yaml
Type: PIG
Display: '&9Wumpus'
Health: 12

AIGoalSelectors:
  - clear
  - randomstroll

Options:
  MovementSpeed: 0.25
  Invincible: true

Skills:
  - model{mid=wumpus} @self ~onSpawn
  - model{mid=wumpus} @self ~onLoad
  - sound{s=entity.pig.hurt;p=1.5} @self ~onDamaged
  - skill{s=wumpus_wave} ~onInteract
```

3. Run `/bettermob reload`, then `/bettermob spawn wumpus`.

The wiki walks through [installation](https://github.com/HyperGaming99/bettermob/wiki/Installation), [mobs](https://github.com/HyperGaming99/bettermob/wiki/Mobs), [skills](https://github.com/HyperGaming99/bettermob/wiki/Skills) and [packs](https://github.com/HyperGaming99/bettermob/wiki/Packs).

## 🧩 MythicMobs compatibility

BetterMob is not a full MythicMobs replacement. It implements the parts that model-based content packs rely on: custom mob registration, triggers, YAML packs and the MythicMobs-style skill system. If you know that file format, you already know this one. Check a pack before you use it:

```
/bettermob validate <pack>
```

It lists every line BetterMob does not understand. The [pack compatibility](https://github.com/HyperGaming99/bettermob/wiki/Pack-Compatibility) page explains what is supported and what is not.

## Wild spawns

Add a `Spawn` block to a mob file (including a mob inside `packs/<pack>/Mobs/`).
`REPLACE` is the default and converts natural spawns of the mob's vanilla `Type`.
`ADD` makes independent land spawn attempts every 20 seconds around players in
Survival or Adventure mode. It only uses already loaded chunks, at a distance
of 24–48 blocks from the player.

```yaml
# In nm_raccoon.yml, below Type: PIG
Spawn:
  Action: ADD
  Worlds: [world]
  Biomes: [TAIGA, OLD_GROWTH_PINE_TAIGA]
  Time: ANY
  Chance: 0.05
  Cooldown: 60
  MaxNearby: 3
```

`Chance` is a probability from 0 to 1 **per attempt**, not a number of mobs
per day. `Cooldown` is in seconds per player and mob after a successful spawn;
`MaxNearby` limits that mob to the given count within 48 blocks of the spawn
point. `Worlds` must use the server's exact world name. Reload with
`/bettermob reload` after changing the file.

To diagnose `ADD` attempts, use `/bettermob debug chat`. This also works when
global `Debug` is off and immediately lists the active `ADD` rules. Players
with the `bettermob.debug` permission see their own attempt results in chat,
including the location and reason a spawn was skipped. `/bettermob debug info`
also enables the global debug log. Use
`/bettermob debug filter nm_raccoon` to limit output to one mob.


## 🔌 For developers

BetterMob registers a service with `BetterMobAPI`, custom mechanics, conditions, targeters and placeholders, and fires events for spawns, damage, targets, phases, drops, skills, mechanics, spawners and reloads. See the [Developer API](https://github.com/HyperGaming99/bettermob/wiki/Developer-API) and the [API events](https://github.com/HyperGaming99/bettermob/wiki/API-Events), or start from the example plugin in [`examples/api-example`](examples/api-example). Releases and the newest `dev` state (`dev-SNAPSHOT`) are on JitPack.

## 🧱 Addons

Addons are separate plugins that build on the Developer API and need BetterMob installed.

- **[BetterMobCrucible](https://github.com/HyperGaming99/BetterMobCrucible)**: extra item triggers, item data and lore mechanics for content made for MythicCrucible-style plugins. [Latest release](https://github.com/HyperGaming99/BetterMobCrucible/releases/latest).

Made an addon? Open an issue or a pull request to get it listed here.

## 🏗️ Supported environment

<a href="https://www.minecraft.net/en-us/download/server"><img src="https://img.shields.io/badge/minecraft-1.21.X%7E26.4.x-8FCA5C?style=for-the-badge" alt="Minecraft 1.21.X to 26.3.x"></a>
<a href="https://adoptium.net/"><img src="https://img.shields.io/badge/java-21--25-ED8B00?style=for-the-badge" alt="Java 21-25"></a>
<a href="https://papermc.io/downloads/folia"><img src="https://img.shields.io/badge/folia-%E2%9C%94-blue?style=for-the-badge" alt="Folia"></a>

## 🤝 Contributing

Bug reports, pack compatibility reports and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md) and please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## 👥 Contributors

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://0x79.one"><img src="https://avatars.githubusercontent.com/u/130169800?v=4?s=100" width="100px;" alt="TheNull"/><br /><sub><b>TheNull</b></sub></a><br /><a href="https://github.com/HyperGaming99/bettermob/commits?author=HyperGaming99" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/BlockMrTii"><img src="https://avatars.githubusercontent.com/u/134159440?v=4?s=100" width="100px;" alt="BlockMrTii"/><br /><sub><b>BlockMrTii</b></sub></a><br /><a href="https://github.com/HyperGaming99/bettermob/commits?author=BlockMrTii" title="Code">💻</a></td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td align="center" size="13px" colspan="7">
        <img src="https://raw.githubusercontent.com/all-contributors/all-contributors-cli/1b8533af435da9854653492b1327a23a4dbd0a10/assets/logo-small.svg">
          <a href="https://all-contributors.js.org/docs/en/bot/usage">Add your contributions</a>
        </img>
      </td>
    </tr>
  </tfoot>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

## 📊 Servers that use BetterMob

![bStats](https://bstats.org/signatures/bukkit/BetterMob.svg)

## 📄 License

[MIT](LICENSE)
