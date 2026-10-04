# Items

Items live in `items/` (and each pack's `Items/`). Every top-level key with an `Id:` is an
item, the same way MythicMobs does it:

```yaml
nm_pack_starter_pack:
  Id: PAPER
  Model: 635000
  Display: '&9Starter &fBooster Pack'
  Lore:
  - ' '
  - '&7Contains &f&l2 &r&7Trading Cards!'
  Skills:
  - skill{s=nm_pack_open_starter_pack;cd=1} @self ~onUse
  - takeitem{i=nm_pack_starter_pack;a=1} @self ~onUse
```

| Field | Description |
|---|---|
| `Id` | The Vanilla material (`PAPER`, `STICK`, ...) |
| `Model` | CustomModelData number. Your resource pack switches the look on it |
| `Display` | Name, `&` color codes, not italic |
| `Lore` | List of lines |
| `Skills` | Skill lines. `~onUse` runs on right-click with the main hand |

Other fields, such as the MythicCrucible `Type: FURNITURE` block, are ignored.

## Using items

- `/bettermob give <item> [player] [amount]` hands one out.
- Right-clicking runs the `~onUse` lines with the player as caster and trigger.
- `takeitem{i=<item>;a=<amount>}` removes them again, for example to consume the item.
- `Options.ItemHead: <item>` puts an item on a mob's head, see [Mobs](Mobs).

Built items remember their ID, so `~onUse` and `takeitem` recognise them. An item created
some other way (for example by another plugin) isn't recognised.
