# Building judge anchors

A judge learns what "good" means from contrasts, not from good lines alone. An anchor is a strong
line next to a weaker version of the same idea, with what was lost named, so the judge can see the
difference instead of being told about it.

## Where the strong lines come from

1. Lines you have labelled "keep" in a filled sheet (`linegen/pilot/sheets/`), once there are any.
   These outrank everything else.
2. Until then, the strongest lines in the bank (`src/main/resources/com/chattynpcs/*.json`),
   judged against `TONE.md`, `WRITING_LINES.md` and these marks: a concrete thing the listener can
   see, an observation in the speaker's own terms, a point that's implied, spoken rhythm. Take them
   from many voices and personas, all four tones, and at least ten different topics.

## How to weaken a line

Make one change, so the pair isolates one fault. Name it in `fault`:

- `explains`: adds a clause explaining the joke or the point.
- `states_feeling`: says the feeling outright instead of implying it.
- `generic`: swaps the specific thing for a vague one ("your gear", "that").
- `written`: turns it into prose: a trailing "-ing" clause, a list of three, "not just X, it's Y".
- `gushes`: makes a compliment effusive, or a tease cruel.
- `knows_too_much`: claims something a stranger couldn't know, without "must be" or "they say".
- `wrong_footing`: speaks to the player in an aside, or calls them "human" in gossip.
- `stuck_on`: tacks a greeting or sign-off onto a finished line.
- `wrong_voice`: keeps the words but loses the voice's way of noticing.

The weak version must still be a plausible line, inside 70 characters, the kind a model would
produce. A weak line nobody would write teaches nothing.

## Output: `linegen/judge/anchors.jsonl`

One JSON object per line:

```json
{"voice": "kid", "tone": "insult", "good": "You run on roofs? Mum says that's not allowed.", "weak": "You run on roofs? That's dangerous, and Mum says it's not allowed, so stop.", "fault": "explains", "source": "bank"}
```

`source` is `bank`, or `labels` for lines from your sheets. Write 40 pairs, at least four of each
fault.
