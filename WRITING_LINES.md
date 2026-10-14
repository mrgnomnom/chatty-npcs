# Writing new lines

A practical guide to adding lines to Chatty NPCs. `TONE.md` is the agreed spec, and wins if the two
ever disagree. This file is the how-to: how a line is built, who says what, how to add it to the
JSON, how to check the lore, and how to review a batch before it goes in.

## North star: world immersion

The whole point of Chatty NPCs is to make Gielinor feel more alive: a world that notices you, talks
about what you've done, and gets on with its own business around you. Every rule below serves that.

When a choice isn't covered here, or two rules pull against each other, pick whichever keeps the
player in the world. A line that's funny but breaks the spell (it knows something it shouldn't,
sounds like our world, or gets the lore wrong) is worse than a quieter line that feels true.

## The thirty-second version

A line is something a Gielinor local says as you walk past. It's short (70 characters at most), dry,
and British the way the game is British: plain, slightly old-fashioned English, understatement, class
comedy, puns. It only mentions things the speaker could plausibly know: what they can see, what
they've heard, or what they lived through. It never names a quest, quotes a number about the player,
swears, or mentions anything from our world.

Before writing:

- **The immersion test:** does this make the world feel more real, or does it remind the player
  they're playing a game?
- **The goat test:** could this be said by someone who has never left Gielinor and never seen a
  television?
- **The stranger test:** would someone who's never met you know this, and say it like this?

## Anatomy of a line

A line is something a person says, not a joke with a setup and a punchline. Real people trail off,
ask you things, exclaim, interrupt themselves, and say one thing and stop. Read a voice's lines
back to back: if they all have the same rhythm, they sound like one writer in different hats.

**The trap: observation, exposition, quip.** "A fire cape. They say the caves are hot. Lovely."
One of these is fine. Two thousand of them is a script. The tests cap it: at most a quarter of any
voice's lines may end on a short tag sentence after a longer one ("Badly, but at length.",
"Keep at it.", "Bold.").

Mix the shapes. Across a voice's lines, aim for roughly:

| Shape | Example | Share |
| --- | --- | --- |
| One sentence, and stop | "You'll want a bigger axe than that for the trolls." | a third or more |
| A question at you | "Did you really go into the caves for that?" | a fifth |
| An exclamation or reaction | "Ooh, that's a fire cape, isn't it!" | some |
| Thinking aloud, trailing off | "A bald head like that, in this wind... rather you than me." | some |
| Self-interruption or correction | "Is that bronze? No, it's rust. Mostly rust." | a little |
| Two sentences | "Doric's still waiting on clay. Somebody fetch the man some clay." | the rest |
| Three sentences | | rarely |

Punctuation follows speech: commas, question marks, exclamation marks and "..." for trailing off
are all welcome. Not the long dash: the game's font may not draw it. Use a comma or "..." instead.

**Each voice has its own rhythm,** not just its own words:

| Voice | Rhythm |
| --- | --- |
| TzHaar, goblins, trolls, ogres | Clipped fragments, no articles, one idea: "Shiny hat. Me want." |
| Gnomes | Bouncy run-ons strung with "and", excitable questions |
| Dwarves | Blunt, one line, done. Judgement first |
| Fremennik, barbarians | Declarations and challenges. Saga talk. Questions are dares |
| Toffs, wizards, vicars | Long single sentences with a clause or two; they enjoy their own voice |
| Kids | Questions, exclamations, non sequiturs. Never a tidy two-beat joke |
| Nans | Fussing, trailing off, "dear", advice nobody asked for |
| Guards | Orders and flat reports. Short. Bored |
| Bankers, traders, barkeeps | Patter: talk aimed at you, often a question or an offer |
| Ordinary locals | Everything above, in moderation. Mostly one sentence |

**Name the thing.** "You earned that the hard way" could be about anything; "You earned that cape
the hard way, didn't you?" is about you. Say which cape, which boots, which dragon.

**Tag questions are British, not American.** Not "huh?", "right?" or "yeah?", but "didn't you?",
"weren't you?", "isn't it?", "eh?", "I'll bet", "I expect", "I shouldn't wonder". The TzHaar have
their own: ", no?"

One-beat lines are the default, not the exception: "Not even a vest? Brave." A second sentence has
to earn its place.

**Shape rules**

- Usually one sentence, sometimes two, rarely three. Aim for under 60 characters; 70 is a hard limit (the tests enforce it).
- Plain words first. At most one dialect marker per line, and only for the personas that use one.
- British spelling: armour, colour, grey.
- Don't start a line with `{item}`. The plugin capitalises the first letter, but "Red partyhat,
  is it?" reads oddly. Put the item mid-sentence.
- Don't bolt a verdict on the end ("Pathetic.", "Tragic.", "Bold.", "Keep at it."). If the joke needs a tag to land, rewrite the joke.
- No capitals for emphasis ("Were you SCARED?"). Excitement is an exclamation mark; the game's
  NPCs never shout in capitals. Names keep their normal capital, and HAM is the only acronym.

## The kinds of line, with patterns

### Compliments (Kind and Balanced moods)

Warm, sincere and a little understated. Never gushing.

- Impressed understatement: "Fair play. Nobody gets a cape like that by accident."
- An exclamation, then a reason: "Blimey. A {item}. You don't see many of those."
- Recognition by deed: "You're the one who cured Slepe? The whole village owes you."

### Insults (Rude and Balanced moods)

Clever, cheeky and specific. The wit is the insult. Techniques the game itself uses:

| Technique | Example |
| --- | --- |
| Sarcastic frame | "Good job you're not the King." |
| Backhanded compliment | "Lovely cape. Shame about the rest of you." |
| Misattribution | "You're not the one who keeps leaving my gates open?" |
| Withering comparison | "Did the goblins dress you? They've better taste." |
| Deflating the hero | "Plenty of other adventurers about besides yourself." |
| Cheerful afterthought | "Thank you again for the ghost. Sorry, I only had the one quest." |
| Politeness as a weapon | "Good day. Clothing isn't required here. It is a courtesy." |
| Literal-mindedness | "It's clay. It just lies there." |

Never flat name-calling. "Pathetic" is reserved for villains (the `shady` persona).

### Remarks (Balanced mood only)

Neutral observations, small talk, or a quest giver muttering to themselves.

- Observation: "Must save you a fortune on combs."
- Muttering: "Eggs... milk... flour... the Duke's cake... I'm done for."
- Small talk: "More goblins about these days, they say."

### Gossip (every mood)

News overheard, not aimed at you. Because it's about the world rather than you, it comes up in Kind,
Rude and Balanced alike. Use the game's framings: "They say...", "Word is...", "I hear...", "Did you
hear?".

- After a deed: "Glough drank his own brew and brought the cave down on himself."
- Trouble brewing: "The Prince is missing, they say. Taken by the Emissaries."
- Glad or bitter both work, as long as it isn't said *to* you.

### Quest givers (`starter/` lines)

The NPC who gives out the quest talks in the first person, and is never wrapped with a persona
opener.

| Situation | How it sounds | Example |
| --- | --- | --- |
| Not done, Kind | Flagging you down for help | "Please, adventurer, I'm desperate. Eggs, milk, flour!" |
| Not done, Rude | Doubting you'll be any use | "You never know what shape you'll wake up in, dear. Rat's tail, please." |
| Not done, remark | Muttering about their trouble | "Red bead... yellow bead... black, white... wretched imps." |
| Done, Kind | A familiar face, glad to see you | "Hello again! I sleep with the window open now, thanks to you." |
| Done, Rude | A familiar face, unimpressed | "Wipe your feet, egg-fetcher. I've just mopped." |

Keep each giver in their real character. Read their dialogue on the Wiki first: the Cook panics,
Father Aereck is mild, Doric is courteous, King Narnode is gracious, King Roald is pompous.

### Quest news by who's speaking

| Speaker | Quest finished | Quest not done |
| --- | --- | --- |
| Someone else | News spreading: "Word is you staked Count Draynor." | Gossip about the trouble: "Cook's still short of an egg." |
| The quest giver | A familiar face (see above) | Muttering, or flagging you down |

### Pets

React to what a passer-by can see the pet doing. Examine text is inspiration, not a script: a
stranger has never read it, so they shouldn't quote it back at you.

- Good: "Is that thing smouldering? Keep it off the thatch." (TzRek-Jad)
- Bad: "Loves to nibble, does it?" (quotes the examine text, which a stranger wouldn't know)

Match the reaction to the pet: fearsome pets make people nervous, cute or silly ones get fondness or
bafflement. See the pet reference table in `TONE.md`.

### Gear

- Only name a piece if it's obvious at a glance ("a red partyhat", "a rubber chicken", "a twisted
  bow"). Otherwise describe what they see: "You dug that gear out of a grave, didn't you?" rather
  than "...that Dharok's platebody...".
- React to what the piece says about you: wealth and flash ((t), (g), heraldic, third age), physical
  impressions (obsidian looks heavy), and delight at cosmetics and oddities.
- Breadth beats depth: many pieces with a few lines each.

## Personas

Lines without a persona prefix are shared by everyone, so they must suit the politest speaker (a
banker or a vicar). Sharper lines get a temperament prefix (`plain/`, `sharp/`) or a persona prefix
(`kid/`, `shady/`).

| Persona id | Who | Voice | Temperament | Rude ceiling |
| --- | --- | --- | --- | --- |
| `local` | Anyone not matched below | Plain, slightly peevish townsfolk: goblins, weather, the king | plain | medium, curt |
| `toff` | Dukes, kings, lords, sirs, squires | Pompous, distracted, "citizen", "the peasantry" | sharp | medium-high, snobbish |
| `trader` | Shopkeepers, merchants | Salesman first: pitch, pun, "my friend", "an absolute bargain" | polite | low-medium, salesy |
| `barkeep` | Bartenders, innkeepers, cooks | Casual gossip hub: "What can I do yer for?", "love" sparingly | plain | medium, dry |
| `banker` | Bankers, clerks | Courteous news-teller: a welcome once per visit, then local quest news (unfinished first) or news from afar, half and half | polite | very low |
| `nan` | Old women, witches | Warm with a witchy glint: "dear", biscuits, toads | polite | low, fond |
| `vicar` | Priests, monks, brothers | "Greetings, traveller", "Saradomin smile on you" | polite | low |
| `kid` | Children, urchins, gnome children | Blunt, mercenary, surreal, excitable. **Own lines only** | plain | high, innocent |
| `guard` | City guards | Bored, officious, on shift: "Move along", "Stay out of trouble". **Own lines only, plus gossip** | sharp | medium, orders not insults |
| `wizard` | Wizards, archmages | Condescending or dotty: "fetching and carrying for cleverer people" | sharp | medium, superior |
| `rough` | Farmers, miners, fishermen, sailors, smiths | Gruff, suspicious: "Be off wi' you", "Blimey" | plain | medium-high, earthy |
| `shady` | Villains, gang members, Zamorakians | Cold contempt: "your sort", "meddler". The only home for "pathetic" | sharp | high, cold |
| `dwarf` | Dwarves | Craft pride, pie, beer, "By Guthix!" | polite | low-medium |
| `gnome` | Gnomes | "human", formal or whimsical | polite | low |
| `fremennik` | Rellekka folk | "outerlander", boasts, honour | plain | medium |
| `pirate` | Pirates | "Arr", "ye", rum. The one persona that may lean on dialect | plain | medium, bawdy |
| `burgher` | Anyone standing in Burgh de Rott | Poor, plain-spoken, can't read, news by word of mouth. **Own lines only, plus gossip** | plain | low-medium, wry |

**Openers and closers** go round any of a persona's lines, whatever they're about, so each must
make sense on its own: "Mind how you go." works after anything; "Don't ask me to read it." only
works after talk of a notice, and "I already knew that." only as a reply. Never use gossip framing
("Or so they say") as a closer: gossip is never wrapped, so it would only land on the wrong lines.

**Temperament ladder:** `polite` < `plain` < `sharp`. A persona gets unprefixed lines, plus
`plain/` lines if it's plain or sharp, plus `sharp/` lines if it's sharp.

| Key | Who can say it |
| --- | --- |
| `insult` | Everyone, so keep it gentle |
| `plain/insult` | local, barkeep, kid, rough, fremennik, pirate, and all sharp personas |
| `sharp/insult` | toff, wizard, shady, guard |
| `banker/remark` | Bankers only |
| `starter/insult` | The quest giver only |

**Address words:** adventurer, traveller and friend by default; "citizen" from royals, "human" from
gnomes, "outerlander" from the Fremennik, "guv" from Varrock street folk, "dear" from nans.

## Words

**Keep:** blimey, cor, chuffed, knackered, skint, proper, faff, dog's dinner, in the wars, face like a
wet weekend, Good heavens, Ye gods, Gadzooks, By Guthix, Saradomin smile on you.

**Banned** (the tests check most of these):

- Swearing of any kind: bloody, arse, can't be arsed.
- Modern slang and catchphrases: muppet, just saying, no offence, lovely jubbly, sweet as a nut, on
  my life, oi oi, Gordon Bennett, minted.
- Northern endearments: pet, duck, petal, ta-ra. ("love" occasionally, barkeeps only.)
- Anything from our world: telly, radio, PE, lycra, raves, insurance, CVs, fivers, tenners, cash,
  charity shops, stag dos, bins, film references, calendar years, weekday names, "guides", "last decade".
- Player language: quest titles, "free experience", "the big one", "maxed", "total level", levels.
- "cute" as an insult, "awesome", and Americanisms in general.
- Numbers about the player: no combat level, total level, skill levels, XP or gold value.

## Adding lines to the JSON

Everything lives in `src/main/resources/com/chattynpcs/`.

| File | What's in it |
| --- | --- |
| `chatter.json` | Personas, plus gear and appearance topics |
| `quests.json` | Quest topics |
| `regions.json` | Named map areas used by quest topics |
| `speech.json` | How races and places talk, and their own sayings |

**Line keys** are a tone (`compliment`, `insult`, `remark`, `gossip`), optionally prefixed by a
persona id, a temperament (`plain`, `sharp`) or `starter`. The tests reject any other key.

### A gear topic

```json
{
	"id": "obsidian",
	"priority": 55,
	"lean": "remark",
	"item": {"pattern": "^Obsidian (platebody|platelegs|helmet)", "minCount": 1},
	"lines": {
		"compliment": ["That armour looks like it'd stop a cannonball."],
		"insult": ["Can you lift your arms in that? Go on. Try."],
		"remark": ["Looks heavy, that. Mind the bridges."],
		"dwarf/compliment": ["Now that's proper work. Volcanic, is it?"]
	}
}
```

- `pattern` is a case-insensitive regex searched in the item's name. Check exact names on the Wiki.
- Add `slots` (e.g. `["HEAD"]`) to limit which slots count, `minCount` for "head to toe in..."
  topics, and `label` for what `{item}` reads as in lines.
- `priority` (1-100): how notable it is. It's weighted by its square, so 90 comes up far more than
  30.
- `lean`: the tone people naturally take about it, which gets a boost.

### A pet topic

```json
{
	"id": "pet_heron",
	"priority": 65,
	"lean": "compliment",
	"pet": {"pattern": "^(Heron|Great blue heron)$"},
	"lines": {
		"compliment": ["Lovely bird. Very elegant, the way it stands there."],
		"remark": ["That bird's eyeing the fish stall, you know."],
		"kid/compliment": ["Is that YOUR bird? Can it fly? Make it fly!"]
	}
}
```

- `pattern` is a case-insensitive regex matched against the follower's name as shown in game.
  Recolours and metamorphs often have different names, so cover them all, and check each on the
  Wiki.
- Give every pet at least one `kid/` line: kids only say their own lines.

### A quest topic

```json
{
	"id": "vampyre_slayer_done",
	"priority": 45,
	"quest": {
		"name": "VAMPYRE_SLAYER",
		"states": ["FINISHED"],
		"near": {"x": 3098, "y": 3268, "radius": 25},
		"starter": "^Morgan$"
	},
	"lines": {
		"compliment": ["Word is you staked Count Draynor. Draynor sleeps easy now."],
		"starter/compliment": ["Hello again! I sleep with the window open now, thanks to you."]
	}
}
```

Make a `_todo` topic (`["NOT_STARTED", "IN_PROGRESS"]`) and a `_done` topic (`["FINISHED"]`) for
each quest.

**Where a quest is talked about depends on its tier:**

| Tier | Set | Effect |
| --- | --- | --- |
| Novice, intermediate, experienced | `near` | Only NPCs near the quest start mention it |
| Master | `region` | Any NPC in that `regions.json` area |
| Grandmaster (and Recipe for Disaster) | `anywhere: true` plus `region` | Anyone anywhere, three times as often in its region |

For grandmasters, also add a `_local` topic: a `region` without `anywhere`, holding the gossip and
bitter lines that only make sense on the spot. An Iorwerth loyalist's grumble belongs in Prifddinas,
not Varrock.

**Storylines:**

- `supersededBy`: later chapters that make this old news. Sins of the Father's done topic goes quiet
  once The Blood Moon Rises is finished.
- `requires`: earlier chapters that have to be done before anyone gossips about the trouble. There's
  no Blood Moon gossip before Sins of the Father.

### A region

```json
{"id": "canifis", "name": "Canifis", "city": true, "boxes": [[3470, 3465, 3520, 3515]], "estimated": true}
```

Boxes are `[x1, y1, x2, y2]` in world tiles, corners inclusive. `city: true` lets guards speak
there. Mark anything you didn't take from the map as `estimated`.

Regions also decide what people know:

- `hearsEverything: true` (the Grand Exchange): NPCs there have heard all quest news, wherever it
  happened.
- `onlyHears: ["morytania"]` (Burgh de Rott): NPCs there only hear news from those regions, not even
  grandmaster deeds from elsewhere.

A region's `place` is what the locals call it, in their own tongue where the game has a name
for it ("Ta Quir Priw" for the Grand Tree), and it's used in greetings. When regions overlap, the
smallest one with a `place` wins.

A persona's `greetings` are said once per visit, before anything else ("Welcome, {you}, to {place}
bank!"): `{you}` is what the local speech calls you and `{place}` the region's `place`. A visit ends
after a quarter of an hour without that persona speaking there. With `newsOnly`, the persona sticks
to quest news while there is any, and knows every quest that starts in their own region (the
smallest one they stand in), not just those within earshot. Their local news is that region's;
anything else they've heard, like the Grand Exchange hearing the whole world, counts as from afar.

A persona with `regions` only applies to NPCs standing there. With no `npcNames`, it's everyone in
that place, which gives a place a voice of its own.

## Languages and accents

`speech.json` decides how someone talks, in this order:

1. **Race**, by `npcNames`: gnomes, TzHaar, dwarves, the Fremennik, ghosts, monkeys, skavids. They
   talk this way wherever they stand, and it reaches persona lines as well as shared ones.
2. **Place**, by `regions`: anyone standing there who isn't a race above. Only shared lines are
   bent. The first match wins, so list specific places before broad ones. A race entry's `regions`
   also count here, for unnamed locals in its homeland.
3. Otherwise plain English.

Two ways to keep the wrong people out of a voice:

- `notIn` on a race: regions where its names don't count, because someone there shares a name
  but belongs to the place (Jatizso has its own Freygerd).
- `outsiders` on any entry with regions: people who stand there without being of the place, so
  they keep their own voice (Nieve, a human slayer master, in the Gnome Stronghold). If an
  outsider belongs to another race we have, add them to that race's `npcNames` instead.

Quest givers' `starter/` lines are never translated. Each entry is built from named features rather
than raw regex wherever one fits:

| Feature | What it does | Example |
| --- | --- | --- |
| `address` | What they call you: swaps "traveller", "my friend" and so on (`swap`), adds it to lines that don't name you (`append`), and with `asNoun` also "an adventurer" → "a human" | `{"word": "JalYt", "asNoun": true}` |
| `exclaim` | Replaces Blimey, Cor, Ye gods, Good heavens. An empty word drops it | `{"words": ["By Seren"], "chance": 0.5}` |
| `praise` | Replaces Lovely, Splendid, Fair play | `{"words": ["Kuani"]}` |
| `greet` / `farewell` | Hello and goodbye round lines addressed to you | `{"words": ["Nilsal."], "chance": 0.2}` |
| `formal` | Chance of spelling out contractions ("do not", "cannot") | `0.3` |
| `swaps` | Anything else, as regex | `{"pattern": "\\btoday\\b", "replace": "this day", "chance": 0.5}` |
| `babble` | Everything comes out as these phrases unless you're wearing `understoodWearing` or have finished one of `understoodAfter` | monkeys, ghosts, skavids |
| `sayings` | Lines of their own, keyed like topic lines, said only by them and never translated again | `{"remark": ["JalYt always cold. Is in the name."]}` |
| `sayingsIn` | Sayings only said in one region, for races found elsewhere too: ghosts talk about the Ectofuntus in Port Phasmatys, not Lumbridge | `{"port_phasmatys": {"remark": [...]}}` |

Sayings are where a place's character mostly comes from, since swaps can only change words a
generic line happens to use. They skip the topic cooldown (they're many different remarks, kept
from repeating by the line history), and must be true whatever the player's quest progress: no
spoilers, and nothing that stops being true once a quest is done. Run `./gradlew test` and check
`build/language-share.txt` to see how often each place actually sounds like itself.

A persona can name a race with `"speech": "gnome"` instead of listing NPC names, so the names live
in one place.

Every entry needs `evidence`: quotes from in-game dialogue or the Wiki showing a real pattern, not
one character's quirk and not a stereotype. Native words must be attested (see `LANGUAGES.md`) and
readable from context: "Sarko!" after a gnome loses patience, not a sentence of Gnomish. Keep chances
subtle (around 0.2-0.5). A translated line over 70 characters falls back to the original.

## Lore due diligence

Lines that state lore wrongly break the magic faster than anything. Do all of this before a line
about a quest, character or place goes in.

### 1. Read the source, not your memory

- Use the OSRS Wiki (`oldschool.runescape.wiki`). Read the quest page, its **transcript**
  (`Transcript:<Quest name>`), and the pages of every character you mention.
- For anything after the quest, read the **"Aftermath"** or **"Post-quest"** sections and the
  characters' current pages. These change as later quests come out.
- Don't rely on memory, RuneScape 3, or fan theories. RS3 lore often differs from OSRS.

### 2. Confirm every fact a line states or implies

For each line, list the facts it depends on and find each one on the Wiki:

- **Who did what.** Did the player personally do it? In Sins of the Father the player does *not*
  defeat Lord Drakan; that's The Blood Moon Rises. Lucien *escapes* in While Guthix Sleeps.
- **Who's alive and who's dead.** Dallas Jones dies in Dragon Slayer II; Idria and Akrisae survive
  While Guthix Sleeps.
- **What changed afterwards.** Who rules now, what opened up, who moved where.
- **Who'd be glad or bitter**, based on factions the Wiki actually describes.

If the Wiki can't confirm something, **don't write the line**, or write it as rumour that doesn't
commit ("They say...", "Some say..."). Things we've already found unconfirmed:

- who assassinated King Hosidius
- whether the Pharaoh is truly free of Amascut
- any organised faction of Glough's supporters
- whether Canifis's werewolves sided with Drakan

### 3. Check the tier and the start location

- Look up the quest's official difficulty on the Wiki. It decides `near`, `region` or `anywhere`.
- For `near` quests, take the start NPC's coordinates from their Wiki page (the map link's x and y)
  and use a 20-30 tile radius.
- Check the start city. Children of the Sun starts in Varrock, not Varlamore.

### 4. Check the quest exists in RuneLite

Every `quest.name`, `supersededBy` and `requires` entry must be a constant in RuneLite's
`net.runelite.api.Quest` enum. Grep it in the RuneLite source (e.g. `grep -n "BLOOD_MOON"
runelite-api/src/main/java/net/runelite/api/Quest.java`). The tests fail on a misspelt name, but
checking first saves a round trip.

### 5. Check names and spellings

Character, place and item names are spelt exactly as in game: "vampyre" not "vampire", "Prifddinas",
"Ape Atoll", "the tenth squad". Item regex patterns must match the item's in-game name. Check it on
the item's Wiki page.

### 6. Keep the storyline consistent

- Within a storyline, only the latest finished chapter should talk. Set `supersededBy` on earlier
  chapters' done topics.
- Trouble gossip should only start once the story has reached it. Set `requires`.
- Don't let a line spoil a later quest the player hasn't done. "Trouble brewing" lines should only
  hint at what the player would already know by that point.

### 7. Record your sources

When you add a batch of storyline lines, note in the pull request or commit which Wiki pages you
used, and any facts you deliberately avoided because you couldn't confirm them.

## Checklist before a batch goes in

For every line:

- [ ] Makes the world feel more alive, not less (the north star).
- [ ] Passes the goat test (nothing from our world).
- [ ] Passes the stranger test (they could plausibly know this).
- [ ] 70 characters or fewer.
- [ ] No quest names, numbers about the player, swearing or banned words.
- [ ] In the right tone, and under the right key for who would say it.
- [ ] Shared (unprefixed) insults are gentle enough for a banker or vicar.
- [ ] Rudeness is clever, not a flat verdict.
- [ ] Grandmaster deeds get awe or sober bitterness, never cheap teasing.
- [ ] Every lore fact checked on the Wiki (see above).
- [ ] Sounds like the persona saying it, if it has a persona prefix.

Then run the tests: `./gradlew test`. They check line keys, length, digits, banned words, that every
quest and region resolves, and that polite personas never get sharp lines.

Then try it in game with **Debug mode** on:

- Use **Pretend quests** (None / Quest cape) to hear both the not-done and done lines.
- Tick **Describe my character** to see which topics apply where you're standing.
- Tick **Make someone talk** next to the right kind of NPC.

## A prompt for generating lines

Use this when asking an assistant to draft a batch. Fill in the brackets.

> Read `TONE.md` and `WRITING_LINES.md` in the Chatty NPCs repo; they're the spec. Draft lines for
> [topic: a gear piece, pet, quest or storyline]. Before writing anything about lore, research it on
> the OSRS Wiki (quest page, transcript, aftermath and character pages) and list the facts each line
> relies on, with the page each came from. Flag anything you couldn't confirm, and don't write lines
> that depend on it.
>
> For each [topic], write:
> - 3 compliments
> - 3 insults: clever, not blunt, gentle enough for a banker unless prefixed
> - 2 remarks
> - for quests and storylines, 3 gossip lines, plus quest giver `starter/` lines if there's a giver
> - persona-specific lines where a persona would say it differently: [personas]
>
> Every line must be 70 characters or fewer, pass the goat test, never name a quest, and never quote
> a number about the player. Return the lines as JSON in the `quests.json` / `chatter.json` format,
> plus the tier, region or `near` coordinates, and any `supersededBy` / `requires` it needs.
