# Building lore and voice cards

You are building the material a writer will be allowed to use, and nothing else. Anything the
writer says about the world must trace back to a fact you write here, so be right, be specific,
and leave out what you can't source.

## Before you start

Read, in the repository:
- `TONE.md`, `WRITING_LINES.md` and `LANGUAGES.md`: how the plugin's people talk.
- `src/main/resources/com/chattynpcs/speech.json`: the voice's attested words and `evidence`.
- `src/main/resources/com/chattynpcs/regions.json`: the region ids `knownIn` must use.
- `src/main/resources/com/chattynpcs/chatter.json` and `quests.json`: the lines this voice already
  has. Grep for the persona or the place.
- `linegen/pilot/cells.json`: the cells the writer will fill for this voice.

Use the OSRS Wiki through its API, one request at a time, with a descriptive User-Agent, for
example:

    curl -s -A "chatty-npcs lore (RuneLite plugin)" "https://oldschool.runescape.wiki/api.php?action=parse&page=Grand_Tree&prop=wikitext&format=json"

Old School lore only. RuneScape 3 lore is not canon here, and the Wiki labels it.

## Facts: `linegen/lore/<name>.json`

```json
{"facts": [
	{
		"id": "gnome.gliders",
		"says": "Gnomes fly gliders between the Grand Tree and a few other landing sites.",
		"source": "https://oldschool.runescape.wiki/w/Gnome_glider",
		"canon": "in-game",
		"knownIn": ["gnome_stronghold"],
		"gate": null
	}
]}
```

- `id`: prefixed with the file's name, lower case, dots and underscores.
- `says`: one plain sentence in your own words. Never paste Wiki prose or game dialogue.
- `source`: the Wiki page, as an `https://oldschool.runescape.wiki/w/...` URL.
- `canon`: `in-game` (said or shown in the game), `developer` (a Jagex statement), or `wiki` (Wiki
  prose without a direct game source). Prefer `in-game`.
- `knownIn`: region ids from `regions.json` where ordinary people would know this, or
  `["everywhere"]` for common knowledge across Gielinor. Be stingy: a Varrock baker doesn't know
  the gnomes' glider routes.
- `gate`: `null` if it's true and sayable whatever the player has done. Otherwise
  `{"quest": "THE_GRAND_TREE", "states": ["FINISHED"]}`, using RuneLite's `Quest` enum names as
  they appear in `quests.json`, and listing the states in which it may be said. A gate does two
  jobs: no spoilers (a twist is only said once the player has finished the quest) and no stale
  news (a quest's trouble is only said while it's still trouble: `["NOT_STARTED", "IN_PROGRESS"]`).
  If unsure whether something is a spoiler, gate it. When a fact also gives away an earlier
  quest's ending, add `"gates": [{"quest": "THE_GRAND_TREE", "states": ["FINISHED"]}]` as well:
  every gate must hold.

Write facts a passer-by could plausibly know and might bring up: what the place is like, its
trades, worries, food, rivals, rulers, famous residents, what happened there, what's visible.
Aim for 40 to 70 facts, specific over general.

## Voice card: `linegen/voices/<voice>.json`

```json
{
	"id": "gnome",
	"who": "Who these speakers are, in two sentences.",
	"notices": ["what they notice first about a stranger, most telling first"],
	"values": ["what they care about"],
	"measuresBy": ["what they compare things to: 'a glider', 'a good gnomeball season'"],
	"address": "how they call the player, and when; null if they don't",
	"nativeWords": [{"word": "kar", "use": "how it's used", "evidence": "the speech.json evidence or Wiki page"}],
	"grammar": ["sentence habits: what they drop, add or reorder"],
	"rhythm": "how long, how clipped, how a line usually lands",
	"markersPerLine": 1,
	"taboos": ["what they'd never say, or never say like that"],
	"canon": ["up to five short attested lines showing the voice, each with its source"],
	"best": ["eight to twelve of the best existing bank lines in this voice, verbatim"],
	"avoid": ["caricatures and real-world stereotypes to keep clear of"]
}
```

The most important fields are `notices`, `values` and `measuresBy`. A voice is a way of noticing,
not a vocabulary: a dwarf sees the rivets before the shine. Native words are seasoning, at most
`markersPerLine` in a line, and only words with evidence. Pick `best` lines that sound like a real
person reacting to something, not lines that merely carry the native word.
