# Drop tables

Drop tables decide what a mob drops when it dies. They live in `droptables/` (and each pack's
`DropTables/`), nested folders are fine.

```yaml
nm_bison_drops:
  MinItems: 2
  MaxItems: 3
  Drops:
  - EXP 5-11 100%
  - LEATHER 4-5 80%
  - nm_bison_fur 2-3 40%
```

## Fields

| Field | Default | Description |
|---|---|---|
| `Drops` | - | The drop lines, see below. Required |
| `MinItems` | 0 | At least this many **item entries** drop |
| `MaxItems` | no limit | At most this many item entries drop |

A file can hold several tables, every top-level key with a `Drops:` list is one table.

## Drop lines

```
<what> <amount> <chance>
```

| Part | Examples | Notes |
|---|---|---|
| what | `BONE`, `nm_bison_fur`, `nm_other_table`, `EXP` | See the order below |
| amount | `3`, `2-4` | Optional, default 1. A range is rolled between both numbers |
| chance | `80%`, `0.8`, `1` | Optional, default 100% |

A line like `BONE 25%` is read as a chance, `BONE 3` as an amount.

**What a name means**, checked in this order:

1. `EXP`, `experience` or `xp`: experience points
2. The name of another drop table (it is rolled as many times as the amount says)
3. A registered [item](Items)
4. A Vanilla material

Anything else is logged once and skipped.

## How a roll works

1. Every line is rolled against its chance.
2. `EXP` lines are added up separately. They do **not** count towards `MinItems`/`MaxItems`.
3. If more item entries hit than `MaxItems`, random ones are dropped. If fewer than `MinItems`
   hit, random missed ones are added (without rolling their chance again).
4. Item amounts above the stack size are split into several stacks.

## Using a table on a mob

In the mob file, `Drops:` lists tables and/or direct drops:

```yaml
nm_bison:
  Type: IRON_GOLEM
  Options:
    PreventOtherDrops: true
  Drops:
  - nm_bison_drops
  - DIAMOND 1 5%
```

As soon as a mob has a `Drops:` list, its vanilla drops **and** vanilla experience are removed, so
only what you define drops. `PreventOtherDrops: true` does the same for mobs that have no `Drops:`.

`/bettermob reload` reloads the tables. Tables referencing each other in a loop stop after 5 levels.
