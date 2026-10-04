# Development

## Building

```bash
mvn clean package
```

produces `target/bettermob-<version>.jar`. Java 21 and Maven are required.

## Tests

```bash
mvn test
```

JUnit 5 tests cover the line parsers (skill steps, conditions, mob triggers, drop lines, inline skills), that every former built-in mechanic name stays registered, that the language files match and every message key exists, that the permission nodes in the code match `plugin.yml`, the boss bar title placeholders and the skill statistics. `PackFilesTest` also parses every line of the YAML files shipped in `src/main/resources` and of any pack placed under `src/test/resources/packs/` (use the usual `Mobs/`, `Items/`, `Skills/` and `DropTables/` folder names). The workflow runs `mvn test` on every push and pull request, and a failing test stops the build and the release.

## Branches and releases

- Work happens on `dev`. Every push builds the jar and replaces the rolling pre-release `dev-build`.
- Merging `dev` into `main` with a new `<version>` in `pom.xml` (and `plugin.yml`) creates the release `v<version>` and publishes the Maven package.
- Dev builds and releases list every commit since the last release in their notes.
- Every change is its own commit.

## Editing the wiki

The wiki pages live in the repository under `wiki/`. Change them in a pull request like code: when it lands on `dev`, the workflow `wiki-sync.yml` publishes `wiki/` to the GitHub wiki (files that are not in `wiki/` are removed from the wiki). Never edit the wiki on GitHub directly, the next sync would overwrite it. A pull request that changes behaviour updates `wiki/` in the same pull request.

## Code layout

Everything lives under `eu.northsoft.bettermob`:

| Package | Contents |
|---|---|
| (root) | `BetterMobPlugin`, the plugin class that wires everything |
| `api`, `api.event` | The public API (see [Developer API](Developer-API)) |
| `command` | `/bettermob` and its permissions |
| `mob` | Mob definitions, registry, manager (spawn, reload, killall), boss bars and listeners |
| `skill` | The skill engine, `SkillStep` parser, contexts, auras, factions |
| `skill.mechanic` | One class per mechanic behind the `Mechanic` interface |
| `skill.condition` | One class per condition behind `SkillCondition` |
| `skill.target` | One class per targeter behind `Targeter` |
| `ai` | AI goal handling and custom goals |
| `model` | BetterModel and ModelEngine hooks |
| `drop`, `item` | Drop tables and custom items |
| `pack` | Pack scanning and YAML file collection |
| `lang` | Message files and `Language:` handling |
| `debug` | The debug output |
| `stats` | Skill timing for `/bettermob stats` |
| `integration` | PlaceholderAPI |
| `service` | The API implementation |
| `util` | Scheduling helpers (Folia aware) |

## Adding a mechanic

1. Create a class in `skill.mechanic` that implements `Mechanic`. `execute(MechanicCall call)` gets the line (`call.step()`), the `SkillContext`, the resolved `Target` and the parameters (keys lower case, placeholders already replaced). Multi-target lines call it once per target.
2. Register it with its name and aliases in `BuiltinMechanics.registerAll`, for example `registry.register(new IgniteMechanic(), "ignite")`.
3. Add a test line to a pack or parser test, then document the mechanic in README and in [Skills](Skills).

Conditions and targeters work the same way: implement `SkillCondition` or `Targeter` and register it in `ConditionRegistry` or `TargeterRegistry`. Plugins that don't want to touch BetterMob can register their own mechanics through the [Developer API](Developer-API).

## Example plugin

`examples/api-example` is a separate Maven module that uses the public API, see [Developer API](Developer-API#example-plugin). The workflow installs the build and compiles it with `-Plocal` on every push, so an API change that breaks it fails the build.

## Messages

User-facing text goes into `lang/en.yml` and `lang/de.yml`, never into the code. `LangFilesTest` fails when the two files differ or when a key used in the code is missing.
