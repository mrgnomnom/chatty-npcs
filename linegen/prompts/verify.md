# Checking lines against the lore

You check whether each line says anything about the world that its cited facts don't support. You
are not judging whether the line is good, only whether it's true and the speaker could know it.

## In

- `linegen/lore/*.json`: the facts.
- `linegen/pilot/cells.json`: what the player is wearing, has following them, looks like or has
  done, in each cell.
- The lines to check, from the file you're given. Each has a `cell` and the `facts` it cites.

## For each line

1. List every claim the line makes, as short plain statements. Include what's implied: "back from
   the caves, then?" claims the item comes from caves.
2. Give each claim a `support`:
   - `visible`: true of what the cell says the player is wearing, carrying, followed by or looks
     like, as a passer-by would see it. Check the appearance against the facts if it's specific
     ("red", "glowing").
   - `opinion`: the speaker's own feeling or judgement, which nobody could contradict.
   - `own`: the speaker's ordinary day: their knees, their stall, the weather. Not lore.
   - `fact:<id>`: one of the line's cited facts says it. Read the fact; don't trust the id.
   - `none`: nothing supports it.
3. Mark `invents` if the line makes up a person, place, relative, event or history, even a small
   one ("my cousin in the Fremennik lands").
4. `grounded` is true only if no claim is `none` and nothing is invented.

## Out

Append one JSON object per line to the output file you're given:

```json
{"id": "gnome-fire_cape-insult-03", "claims": [{"claim": "the cape came from a hot place", "support": "fact:item.fire_cape_fight_cave"}, {"claim": "the cape is showy", "support": "opinion"}], "invents": false, "grounded": true, "note": ""}
```

Be strict. A line that's charming but wrong is still wrong; a writer can fix it, a player can't
unhear it.
