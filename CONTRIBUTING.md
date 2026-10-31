# Contributing to BetterMob

Thanks for helping! BetterMob is a Paper and Folia plugin for custom mobs, models and skills. This page tells you how to report problems, how to build the plugin and what a good pull request looks like.

By taking part you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## 🐛 Reporting problems and ideas

| You want to | Open |
|---|---|
| Report a bug | a [bug report](https://github.com/HyperGaming99/bettermob/issues/new?template=bug_report.yml) |
| Suggest a feature | a [feature request](https://github.com/HyperGaming99/bettermob/issues/new?template=feature_request.yml) |
| Tell us that a MythicMobs-style pack does not load | a [pack compatibility report](https://github.com/HyperGaming99/bettermob/issues/new?template=pack_compatibility.yml) |
| Ask how something works | the [wiki](https://github.com/HyperGaming99/bettermob/wiki) first, then an issue |

Search the open and closed issues before you write a new one. For a bug, include the BetterMob version, your server software and version (Paper or Folia), the relevant part of the console log and the mob, skill or item file that goes with it. `/bettermob validate <pack>` and `/bettermob debug info` show a lot.

## 🧰 Setting up

You need Java 21 or newer. Gradle comes with the wrapper (`./gradlew`).

```bash
git clone https://github.com/HyperGaming99/bettermob.git
cd bettermob
./gradlew build
```

The jar ends up in `bettermob-paper/build/libs/bettermob-<version>.jar`. `./gradlew test` runs the unit tests. Two optional tests check the reflection calls against the real model plugins; give them the jars to run them:

```bash
./gradlew test -Dmodelengine.jar=<ModelEngine jar> -Dbettermodel.jars=<bettermodel-api jar>:<bettermodel-bukkit-api jar>
```

The Folia smoke test starts a real Folia server with your build. How to run it by hand is described on the [Development](wiki/Development.md) page; on GitHub it runs for every pull request.

## 🔀 How we work

1. **Open or pick an issue.** Changes start as an issue with a short checklist. Say in the issue that you are working on it.
2. **Branch from `dev`.** All work lands on `dev`, `main` only gets releases. Name the branch after the change: `feature/...`, `fix/...`, `docs/...`, `refactor/...`.
3. **One change, one commit.** Do not mix a fix and a feature in a commit. The message says what the change does, in the imperative: `Add the region condition`, `Fix the drop list reload`.
4. **Open a pull request into `dev`** that says `Closes #<issue>` in its description. Describe what you tested and, just as important, what you did not test.
5. **Keep it green.** The build, the unit tests and the Folia smoke test have to pass. CodeRabbit reviews every pull request automatically; answer its findings or explain why you disagree.

A maintainer merges after review. Releases are cut by merging `dev` into `main` with a new version.

## ✅ What a good pull request contains

- **Tests** for new logic (parsers, calculations, selection rules). Most of BetterMob's logic is pure on purpose, so it can be tested without a server.
- **Documentation in the wiki.** The wiki lives in the [`wiki/`](wiki) folder and is published automatically when your pull request lands on `dev`. Change it in the same pull request as the feature. The README is only the landing page.
- **Language keys** in both `lang/en.yml` and `lang/de.yml` for every new message. A test checks that the files match and that every key is used.
- **JSON schema and validator updates** when you add a mob, skill or item option (`schemas/` and `/bettermob validate`).

## 🧼 Code style

- **No code comments.** The code explains itself through names; the explanation belongs in the wiki. A workflow strips comments from pull requests.
- Follow the package layout described on the Development page and keep new classes small and focused.
- **Folia first.** Entity state may only be read or changed on the thread of the region that owns the entity. Use the helpers in `util/Tasks` (`runOwned`, `runLater`, `runTimer`) and check `Bukkit.isOwnedByCurrentRegion(...)` before you touch an entity that may belong to another region. Never block a region thread.
- Treat every text that ends up in a skill line, command or message as untrusted: insert values as plain text and strip skill syntax from names and placeholders.
- Do not add a dependency for something a few lines of Java can do. Talk to the model plugins (BetterModel, ModelEngine) through reflection, never compile against them.
- Public API goes in `eu.northsoft.bettermob.api` and its sub packages. Changing it needs a note in the [Developer API](wiki/Developer-API.md) page.

## 👥 Recognition

Contributors are listed in the README with the [all-contributors](https://allcontributors.org) bot. If you helped with code, docs, bug reports or ideas, ask for an entry in your pull request or issue.

## 📄 License

By contributing you agree that your work is licensed under the [MIT License](LICENSE).
