# Judging lines

You decide which lines a real passer-by in this place would say. You don't know who wrote them,
and it doesn't matter. Judge the line, not the effort.

## Read first

- `linegen/judge/anchors.jsonl`: pairs of a strong line and a weaker version, with the fault
  named. Read all of them before judging anything. They are the standard.
- `linegen/voices/<voice>.json`: who is speaking, and what they notice.
- `TONE.md`: the plugin's tone.
- `linegen/judge/lessons.md`, if it exists: what the person curating these lines keeps and cuts,
  from their own ratings. Judge as they would.

## For each line, answer each question YES or NO

Quote the words that decide it. N/A only where a question can't apply.

1. **Occasion.** Does it hang on something the speaker can see now, has heard (marked: "word is",
   "they say"), or their own business, and would a player hearing it cold, with only what's on
   screen and what they've just done, know what it's about? A line that leaves them asking "what
   is this about?" is NO, however good it would be with context. So is one that contradicts what
   the player did (they made the cake, so they don't come back wanting one).
2. **One job.** Does it do one thing: notice, compliment, tease, complain, gossip, mutter?
3. **Specific.** Does it name the particular thing, in words a passer-by would use?
4. **Stranger-safe.** Does it avoid knowing what a stranger couldn't, marking guesses?
5. **Walk-on-safe.** If it asks something, could the player walk on and it still feel finished?
6. **Implied.** Is the feeling or joke left implied, with no clause explaining it?
7. **Mood.** Compliment: specific and understated, not gushing. Insult: aimed at a choice the
   player made, plainly not meant to wound. Remark and gossip: neither fawning nor sneering.
8. **Footing.** Is it clear who it's said to, and do address words and tags fit that?
9. **Voice.** Is what the speaker noticed first what this voice would notice?
10. **Spoken.** Could someone say it aloud in one breath? No prose constructions?
11. **Period.** Free of modern, American or real-world words and ideas?
12. **Fresh.** Is it unlike the other lines in this cell, and unlike a stock joke? A speaker's
   catchphrase or tag tacked on the end by reflex is NO.

A line **passes** if it gets YES on 1, 4, 6, 7, 10 and 11, and at most one NO among the rest.

## Then rank each cell

Among the lines that pass in a cell, rank the best five. Read them aloud in your head as the
player walks past. Prefer the line you'd remember tomorrow, then the one that sounds most like
this voice and nobody else. Never prefer a line for being longer or cleverer-sounding.

## Out

Append one JSON object per line to the output file you're given:

```json
{"id": "gnome-dragon_armour-compliment-07", "answers": {"1": "YES 'Red armour'", "2": "YES", "3": "YES 'Red armour'", "4": "YES", "5": "N/A", "6": "YES", "7": "YES", "8": "YES 'human'", "9": "YES 'from a glider'", "10": "YES", "11": "YES", "12": "YES"}, "pass": true, "rank": 2}
```

`rank` is 1 to 5 for the best five passing lines in a cell, and null otherwise.
