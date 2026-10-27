# Writing lines

You are writing what a passer-by says, out loud, in one breath, as a player walks past. The player
can't answer. The line floats over the speaker's head for a few seconds. Most lines in games read
as written; yours have to read as said.

## Before you start

Read, in the repository:
- `TONE.md` and `WRITING_LINES.md`, all of them. They are binding.
- `linegen/judge/lessons.md`, if it exists: what the person curating these lines keeps and cuts,
  from their own ratings. Where it and anything else here disagree, it wins.
- `linegen/voices/<voice>.json`: who is speaking. `notices`, `values` and `measuresBy` matter
  more than `nativeWords`.
- `linegen/lore/*.json`: the only facts about the world you may use.
- `linegen/pilot/cells.json`: your cells. Each is a voice, a topic (look it up in
  `src/main/resources/com/chattynpcs/chatter.json` or `quests.json` to see what it covers: an
  item topic's `pattern` is every item it matches), a tone, and what the player is wearing, has
  following them, looks like or has done.
- The topic's existing lines, so you don't repeat them.

## For each cell, write 15 candidates

Each one does a single **move**, from a single **footing**, about a **cue** the speaker can see or
has heard:

| Move | What it does | Example |
| --- | --- | --- |
| M1 Noticing | names the thing just seen, reacts | "Ooh! A little dragon! Mind it doesn't breathe on the washing." |
| M2 Assessment | judges it; guesses about what can't be seen are marked ("must be", "looks") | "Must be heavy, all that black glass." |
| M3 Compliment | understated, specific, often deflected sideways | "I do like a proper hat. You don't see many about." |
| M4 Small talk | about the place, or the speaker's own lot | "My knees aren't what they were. Mind that step." |
| M5 Complaint | a gripe about someone or something else, closed by an idiom or tail | "Price of bread round here. Daylight robbery, that is." |
| M6 Tease | at something the player chose or overdid, plainly exaggerated | "All that armour to buy a fish?" |
| M7 Self-talk | muttering about their own trouble | "Now where did I put that bucket?" |
| M8 Hearsay | news, marked as heard ("word is", "they say") | "Word is someone sorted out the count in Draynor." |
| M9 To the pet | addresses the pet; the owner is the real audience | "Hello, little bandit. Don't you eye my purse." |
| M10 Aside | about the player, to someone nearby, in earshot | "Here, look at that hat. No, don't stare." |

Footings: `to` (said to the player), `aside` (about them, not to them: no "you"), `gossip`
(overheard news; gossip-tone cells only, and only they), `self` (muttered: no "you"), `pet` (to the
pet). The voice's address word ("human") only goes in lines said `to` the player.

Spread the 15 across at least four moves and three sentence shapes. No two may open with the same
two words. Include a few you think are unusual or unlikely to be written by anyone else, as well as
the obvious ones: for each candidate, estimate `p`, how likely a typical writer would be to write
something like it (0 to 1). Don't write fifteen versions of one joke.

## The rules

- At most 70 characters. No digits. No placeholders: name the thing as a passer-by would, in a way
  that's true for every item the topic covers ("that red armour", not "that dragon platebody" when
  the topic also covers dragon boots).
- **The player hears it cold.** They get one glance, no context, and can't ask. From the line
  itself, plus what they can see (their own gear, the place, the speaker) and what they've just
  done, they must be able to tell what it's about. Name the subject: "that bow", not "Rare as
  anything out of there"; "your friend's cake", not "Bless the man" about someone off-screen. No
  "it" or "that" that points at nothing on screen, and no hint at an in-joke the player wasn't
  there for.
- **It squares with what the player did.** The player made the cake, so they don't come back
  wanting one; eggs and milk aren't "odd things". Check each line against the quest from the
  player's side.
- **Passing remarks, not trivia.** Nobody volunteers a hyper-specific fact to a stranger walking
  by ("Mahogany creaks"). A detail is there to make the moment land, not to show the lore.
- **A speaker's tic is seasoning.** A catchphrase or tag ("what?", "old bean") in at most one line
  in four, and never tacked on the end by reflex.
- One action. At most one lead-in ("Ooh", "Here", a tag) and one add-on (a tag question, a tail like
  "...that is."). Three sentences is too many.
- If it asks something, the player must be able to walk on without answering: a tag, a yes-or-no a
  nod could answer, or a question that answers itself.
- **Every claim about the world cites a fact.** Put the fact ids in `facts`. What the speaker can
  see on the player, their own opinion, and their own day-to-day business need no fact. Where
  something comes from, what it does, who made it, what happened somewhere, who rules: those need a
  fact, and the fact's `gate` must allow the cell's quest states and its `knownIn` must include
  where the speaker stands. Cells without quests can only use ungated facts. If no fact supports
  what you want to say, don't say it.
- **No names except these:** names in the facts you cite, names in the voice card, and names the
  existing lines for this topic already use. Never invent a person, place, relative or event.
  Never name a quest.
- Ordinary folk don't make lore claims of their own. A gnome on a platform doesn't know the King's
  secrets; a Varrock local has heard rumours, marked as rumours.
- The tone: `compliment` is warm and understated, never gushing; `insult` is banter, aimed at a
  choice the player made and obviously not meant to wound; `remark` is neither; `gossip` is news.
- Never: explain the joke, state a feeling outright ("I'm so impressed"), flatter, list three
  things, "not just X, it's Y", trailing "-ing" clauses, long dashes, modern or American words.
- The voice is a way of noticing. Let what the speaker notices first, and what they compare it to,
  carry the voice. One native word at most, and none is fine.

## Output

Append one JSON object per line to the output file you're given:

```json
{"id": "gnome-dragon_armour-compliment-07", "cell": "gnome/dragon_armour/compliment", "thought": "noticed the red; gnomes think of gliders and visibility", "line": "Red armour! You'd be easy to spot from a glider, human.", "move": "M1", "footing": "to", "facts": [], "p": 0.4}
```

`thought` is what the speaker noticed and why they speak. Write it before the line, then write the
line from it.
