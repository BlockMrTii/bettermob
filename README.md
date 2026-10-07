BILD

---

<center>
<a href="https://modrinth.com/plugin/bettermob"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>
<a href="https://hangar.papermc.io/HyperGaming99/BetterMob/"><img alt="hangar" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/hangar_vector.svg"></a>
<a href="https://github.com/HyperGaming99/bettermob/"><img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg"></a>
</center>

---

✨ What is BetterMob?
--

BetterMob lets you register your own mobs in a YAML file and spawn them with one command.
Give a mob a BetterModel model, wire it to skills, and it behaves the way you describe.

It was built so that content made for MythicMobs-style plugins can run on a lightweight
plugin built around BetterModel. If you know that file format, you already know this one.

- 🐷 **Mobs in YAML**: any vanilla type as the body, with health, damage, AI goals and options.
  One mob per file or many mobs per file.
- 🎭 **3D models**: attach BetterModel or ModelEngine models, including **rideable seats**
  with real WASD control.
- ⚡ **Skill engine**: triggers, mechanics, conditions and targeters, with `delay`, `cd` and
  inline `?condition` on every line.
- 🎴 **Items**: custom items with model data, lore and right-click skills.
- 🪧 **Display mobs**: invisible, floating armor stands with an item on their head.
- 📦 **Packs**: drop a whole content pack into `packs/` and disable it with one line.
- 🔌 **Developer API**: events and your own custom mechanics.

🧩 Comparison with MythicMobs
-

BetterMob is not a full MythicMobs replacement. It implements the parts that model-based
content packs actually rely on: custom mob registration, trigger handling, YAML packs,
and the MythicMobs-compatible skill system. The full feature list is in the [wiki](https://github.com/HyperGaming99/bettermob/wiki/Skills).

## 🚀 Quick example

```yaml
# plugins/BetterMob/mobs/wumpus.yml
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

```
/bettermob spawn wumpus
```


📚 Official wiki
-

<a href="https://github.com/HyperGaming99/bettermob/wiki/"><img alt="ghpages" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_vector.svg"></a>

🏗️ Supported environment
-
<a href="https://www.minecraft.net/en-us/download/server"><img src="https://img.shields.io/badge/minecraft-1.21.X%7E26.3.x-8FCA5C?style=for-the-badge" alt="mcversion"></a>
<a href="https://adoptium.net/"><img src="https://img.shields.io/badge/java-21--25-ED8B00?style=for-the-badge" alt="Java 21-25"></a>
<a href="https://papermc.io/downloads/folia"><img src="https://img.shields.io/badge/folia-%E2%9C%94-blue?style=for-the-badge" alt="folia"></a>

---

📊 Server thas Use BetterMob
-

![stats](https://bstats.org/signatures/bukkit/BetterMob.svg)
