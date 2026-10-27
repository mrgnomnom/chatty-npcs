# linegen

Writes new lines offline, checks them, and hands you the decision. Nothing here ships in the plugin
jar, and nothing reaches the game until you've read it and moved it into the JSON banks yourself.

The point is to get whole lines, written in each voice from the start, that beat what the plugin
says today. Whether they do is settled by you, blind, not by the model that wrote them.

## The steps

| Step | Who | In | Out |
| --- | --- | --- | --- |
| 1. Lore | a model, from the OSRS Wiki, then you | `prompts/lore.md` | `lore/*.json`, `voices/*.json` |
| 2. Anchors | a model, from the best lines, then your labels | `prompts/anchors.md` | `judge/anchors.jsonl` |
| 3. Write | a model | `prompts/write.md`, `pilot/cells.json` | `pilot/candidates.jsonl` |
| 4. Lint | Java, the same rules as the bank | `GeneratedLinesTest` | `build/linegen/lint.tsv` |
| 5. Thin | `tools/thin.py` | lint results | `pilot/thinned.jsonl` |
| 6. Check facts | a different model run | `prompts/verify.md` | `pilot/verified.jsonl` |
| 7. Judge | a different model run, blind | `prompts/judge.md`, anchors | `pilot/judged.jsonl` |
| 8. Pick | `tools/pick.py` | the above | `pilot/accepted.jsonl` |
| 9. You | you | `tools/sheets.py` | `pilot/sheets/*.tsv` |
| 10. Tune | `tools/calibrate.py` | your filled sheets | the verdict, and new anchors |

Run the Java steps with a forced rerun, since Gradle doesn't know the test reads these files:

    gradlew cleanTest test --tests com.chattynpcs.GeneratedLinesTest

That also writes `build/linegen/baseline.tsv`, what the plugin says in each cell today.

## How a line stays true to the lore

Models usually notice when something is outside what a character could know, and then say it
anyway. So the rules here are structural, not polite requests in a prompt:

- **Facts in, nothing else.** Every fact in `lore/` is in our own words, with the Wiki page it came
  from, where in the world people would know it (`knownIn`, region ids from `regions.json`) and a
  `gate`: the quest states in which it may be said. A gate keeps spoilers out, and keeps stale news
  out too: "something's eating the Grand Tree" stops being true once you've fixed it.
- **Lines cite their facts.** The writer gives the fact ids each line leans on. The test rejects a
  citation the speaker couldn't know or the player hasn't reached. A separate model run then lists
  every claim in the line and finds the fact behind it; a claim with none is cut.
- **A closed list of names.** `LoreTest` and `GeneratedLinesTest` reject any proper name that isn't
  in `wiki_names.txt` (every NPC, monster, item, place, shop and piece of scenery on the Wiki),
  `names.txt` (reviewed lore words with no infobox) or the voice's own attested words. Refresh the
  Wiki list with `python3 linegen/tools/fetch_names.py`.
- **Ordinary folk don't make lore claims.** A townsperson notices what's in front of them, grumbles,
  and passes on hearsay. They don't invent history, relatives in famous places, or secrets.

## How the judge learns your taste

A model asked cold whether a line is good does barely better than chance. So the judge here:

- answers yes-or-no questions, quoting the words that decide each one, rather than giving a score;
- reads anchor pairs first: a strong line next to a flatter version of the same idea, with what
  was lost named. They start from the best lines in the bank and grow from your labels;
- is checked against you. `tools/sheets.py` gives you lines to keep or cut, blind to what the judge
  said; `tools/calibrate.py` measures how often it agrees with you, and turns your keeps and cuts
  into new anchors for the next run.

Every line here was written by Claude, so don't train a model on these lines or on the judge's
verdicts: Anthropic's usage policy needs permission for that. Using them in prompts, as here, is
fine. If you ever want a trained judge, train it on your own labels with an open-weight model.

## Licences

Facts are paraphrased from the OSRS Wiki (CC BY-NC-SA 3.0) and cite their page. Don't paste Wiki
prose or Jagex's dialogue into `lore/`; the voice cards quote a few short attested lines as
examples for the writer, which stay here and never go in the jar.
