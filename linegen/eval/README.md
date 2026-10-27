# eval

A local site for rating lines by hand, and real eval frameworks (DeepEval and OpenEvals) that learn
to agree with you. Your ratings then tune the next generation run. Nothing here ships in the jar.

## Set up once

The judges run on a local model through Ollama, so nothing leaves your machine and nothing is paid
for. The model is from a different family than the one that wrote the lines, so it doesn't favour
its own writing.

    ollama pull qwen3:14b
    python3 -m venv --without-pip ~/.venvs/chatty-eval
    curl -sS https://bootstrap.pypa.io/get-pip.py | ~/.venvs/chatty-eval/bin/python
    ~/.venvs/chatty-eval/bin/pip install deepeval openevals langchain-ollama

`CHATTY_EVAL_MODEL` picks another Ollama model, and `CHATTY_EVAL_PARALLEL` sets how many judgements
run at once (2 by default).

## The loop

1. **Rate.** From the repository root, `~/.venvs/chatty-eval/bin/python linegen/eval/server.py`,
   then open http://localhost:8765. Load as many lines as you like, from:
   - the named characters (every line that was checked, shipped or not)
   - the gnome and Varrock pilot
   - the bank as it ships today
   - lines you paste in yourself

   "Blind 50/50" mixes what the pipeline shipped with what it dropped, so your ratings also test
   the pipeline. Keep or cut each line (`K`/`X`), say how good (`1`-`5`), tag why, and write a better
   version if you have one. What the pipeline and the judges thought only shows once you've saved.
2. **Score.** On *Results & tuning*, *Score my rated lines* runs `run_evals.py`. It scores each line
   with:
   - DeepEval G-Eval, on four criteria taken from `prompts/judge.md`: in character, implied,
     passing-remark craft, and knowing only what the speaker could
   - DeepEval Faithfulness, against the facts the line was written from
   - an OpenEvals judge, few-shot on your own keeps and cuts, with the line being scored left out
     of its examples

   Then it reports how well each judge predicts your verdict (AUC, best cut-off, agreement, kappa),
   next to the pipeline's own fact check and judge.
3. **Tune.** *Tune from my labels* runs `tune.py`:
   - Your rewrites, and keep-and-cut pairs from the same cell, become judge anchors in
     `linegen/judge/anchors.jsonl`.
   - Your commonest reasons for cutting, with examples, become `linegen/judge/lessons.md`, which
     `prompts/write.md` and `prompts/judge.md` tell the writers and the judge to read first.
   - If one judge reaches AUC 0.75 over at least 40 of your lines, its cut-off goes in `gate.json`,
     and `tools/pick.py` drops lines it scored below that.
4. **Generate again**, then rate a fresh blind batch to see whether the new lines do better.

Everything also runs from the command line: `run_evals.py --labelled`, or `--source characters
--n 200` to score lines you haven't rated, and `tune.py`.

## Files

- `labels.jsonl`: your ratings, one row per save. The last row for a line wins. Keep it in git:
  it's the most valuable thing here.
- `custom.jsonl`: lines you pasted in.
- `results/`: scores, the report and job logs. These are rebuilt from the labels, so they're
  ignored by git.
- `gate.json`: written by `tune.py` when a judge is good enough to filter with.

As with the rest of `linegen/`, the lines were written by Claude, so use these labels to steer
prompts and judges, not to train a model on the lines themselves.
