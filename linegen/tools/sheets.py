"""
Step 9: two sheets for you to fill in, blind. Neither says which lines are new or what the judge
thought; the keys sit in separate files for calibrate.py.

pairs.tsv: in each cell the plugin can already speak in, one of today's lines against one of the
new picks, in random order. Put A, B or same in "better".

lines.tsv: new candidates the judge passed and failed, mixed. Put keep or cut in "verdict", and,
if you like, why in "note". These tune the judge.

In:  linegen/pilot/accepted.jsonl, thinned.jsonl, judged.jsonl, verified.jsonl,
     build/linegen/baseline.tsv
Out: linegen/pilot/sheets/pairs.tsv, pairs_key.tsv, lines.tsv, lines_key.tsv
"""
import collections
import os
import random

from common import BUILD, PILOT, cells, cue, read_jsonl, read_tsv, write_tsv

SHEETS = PILOT + "sheets/"
PAIRS_PER_CELL = 3
LINES = 200


def main():
	rng = random.Random(169)
	_, by_id = cells()
	os.makedirs(SHEETS, exist_ok=True)
	if os.path.exists(SHEETS + "pairs.tsv") or os.path.exists(SHEETS + "lines.tsv"):
		raise SystemExit("sheets already exist; move them aside first so nothing you've filled in is lost")

	today = collections.defaultdict(list)
	for row in read_tsv(BUILD + "baseline.tsv"):
		today[row["cell"]].append(row["said"])
	new = collections.defaultdict(list)
	for c in read_jsonl(PILOT + "accepted.jsonl"):
		new[c["cell"]].append(c)

	pairs, pair_keys = [], []
	for cell_id in sorted(new):
		olds = today.get(cell_id, [])
		rng.shuffle(olds)
		for k, c in enumerate(sorted(new[cell_id], key=lambda c: c["rank"])[:PAIRS_PER_CELL]):
			if not olds:
				break
			old = olds[k % len(olds)]
			new_first = rng.random() < 0.5
			a, b = (c["line"], old) if new_first else (old, c["line"])
			cell = by_id[cell_id]
			pairs.append([cell["voice"], cue(cell), cell["tone"], a, b, ""])
			pair_keys.append([cell_id, c["id"], "A" if new_first else "B", c["line"], old])
	order = list(range(len(pairs)))
	rng.shuffle(order)
	write_tsv(SHEETS + "pairs.tsv", ["pair", "voice", "the player", "tone", "A", "B", "better"],
		[[n + 1] + pairs[i] for n, i in enumerate(order)])
	write_tsv(SHEETS + "pairs_key.tsv", ["pair", "cell", "id", "new_is", "new", "today"],
		[[n + 1] + pair_keys[i] for n, i in enumerate(order)])

	judged = {j["id"]: j for j in read_jsonl(PILOT + "judged.jsonl")}
	verified = {v["id"]: v for v in read_jsonl(PILOT + "verified.jsonl")}
	thinned = [c for c in read_jsonl(PILOT + "thinned.jsonl") if c["id"] in judged]
	passed = [c for c in thinned if judged[c["id"]]["pass"]]
	failed = [c for c in thinned if not judged[c["id"]]["pass"]]
	rng.shuffle(passed)
	rng.shuffle(failed)
	sample = passed[:LINES // 2] + failed[:LINES // 2]
	# Too few failures to make up half: top up with more passes
	sample += passed[LINES // 2:LINES // 2 + LINES - len(sample)]
	rng.shuffle(sample)
	write_tsv(SHEETS + "lines.tsv", ["row", "voice", "the player", "tone", "line", "verdict", "note"],
		[[n + 1, by_id[c["cell"]]["voice"], cue(by_id[c["cell"]]), by_id[c["cell"]]["tone"], c["line"], "", ""] for n, c in enumerate(sample)])
	write_tsv(SHEETS + "lines_key.tsv", ["row", "id", "cell", "judge_pass", "judge_rank", "grounded"],
		[[n + 1, c["id"], c["cell"], judged[c["id"]]["pass"], judged[c["id"]].get("rank") or "",
			verified.get(c["id"], {}).get("grounded", "")] for n, c in enumerate(sample)])
	print("%d pairs over %d cells, %d lines to label (%d the judge passed)" % (
		len(pairs), len({k[0] for k in pair_keys}), len(sample), sum(1 for c in sample if judged[c["id"]]["pass"])))


if __name__ == "__main__":
	main()
