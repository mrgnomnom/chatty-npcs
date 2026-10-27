# Building a named character

A named character isn't a culture with a name on it. Hans has walked round Lumbridge Castle for
longer than anyone remembers; the Cook has a party to cater and no eggs. Their lines should be things
only they would say, about their own life, in their own way, and true to the game.

Read `prompts/lore.md` first: everything there about sources, paraphrasing, `knownIn` and `gate`
applies here too.

## Sources

- The character's Wiki page, and `Transcript:<their name>` if it exists.
- The transcript of every quest they appear in (`Transcript:<quest name>`). This is where their
  voice is: read what they actually say, how they say it, and what they care about.
- The plugin's existing lines for them: grep `src/main/resources/com/chattynpcs/quests.json` for
  their name, especially `starter` lines, which are theirs already.

## Facts: `linegen/lore/char_<batch>.json`

Facts prefixed `char.<id>.`, 10 to 20 per character. Facts about their own life and work: who
they are, what they do all day, who they know and what they think of them, what happened to them,
what they want. `knownIn` is where they live and work, since they know their own business.

Gate their story carefully. Before a quest, they have a problem; during it, the player is helping;
after it, it's solved and they may be grateful, or changed, or gone. Twists are only sayable once
the quest that reveals them is `FINISHED`. If a quest kills or transforms them, nothing they say
before it may hint at it.

## Card: `linegen/characters/cards/<id>.json`

```json
{
	"id": "hans",
	"npc": "Hans",
	"x": 3221, "y": 3218, "plane": 0,
	"region": "lumbridge",
	"speech": null,
	"who": "Two or three sentences: who they are, what they do, what they want.",
	"notices": ["what they notice first about a passer-by"],
	"values": ["what they care about"],
	"measuresBy": ["what they compare things to"],
	"talksAbout": ["what they bring up unprompted, each with the fact ids behind it"],
	"address": "how they address strangers, with an example",
	"grammar": ["their sentence habits"],
	"rhythm": "how they talk: brisk, rambling, pompous, nervous",
	"canon": ["up to five short lines they really say, each with its source"],
	"neverSay": ["things that would be out of character or false for them"],
	"states": [
		{"id": "always", "quests": {}},
		{"id": "before_cooks_assistant", "quests": {"COOKS_ASSISTANT": "NOT_STARTED"}},
		{"id": "after_cooks_assistant", "quests": {"COOKS_ASSISTANT": "FINISHED"}}
	]
}
```

- `npc` is their name exactly as the game shows it over their head.
- `x`, `y`, `plane`: where they usually stand. Take it from the Wiki's map coordinates for them
  (the `locline` bucket, or the infobox map) and say in your reply if you had to estimate.
- `region`: the smallest region in `regions.json` containing that spot, or null if none does. If
  two NPCs share the name (there are eight Cooks), the region is how the plugin tells them apart.
  They always know their own `char.<id>.` facts, wherever they stand.
- `speech`: the `speech.json` id their people talk in (a dwarf, a gnome, a goblin), or null.
- `states`: `always` first, for lines true whatever the player has done, then one state per
  quest stage that changes what they'd say, most important first. Only the first three are
  written for, so put the moments players most often meet them in. Use RuneLite's `Quest` enum names as they appear in
  `quests.json`, and one state each: the writer will write for exactly that moment.

Leave out villains: anyone whose role is to deceive or attack the player. A model plays them
badly, and a hint in the wrong place spoils their quest.
