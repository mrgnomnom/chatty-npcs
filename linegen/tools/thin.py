"""
Step 5: keeps the candidates that passed the Java lint, then thins out sameness: near-copies of each
other or of the bank, and sentence shapes a voice keeps reaching for.

In:  linegen/pilot/candidates.jsonl, build/linegen/lint.tsv
Out: linegen/pilot/thinned.jsonl, and a summary of what was dropped and why
"""
import collections
import json
import os
import re

from common import BUILD, PILOT, bank_lines, cells, read_jsonl, read_tsv, similar, skeleton, words, write_jsonl

# A voice can use one sentence shape this many times across the pilot, and once per cell
SHAPE_PER_VOICE = 3
TOO_SIMILAR = 0.5
# Across everything a voice says, heard over hours: what they call you, each word of their own and
# each way of starting a line can only come up so often before it's a tic
ADDRESS_SHARE = 0.2
NATIVE_SHARE = 0.03
OPENING_SHARE = 0.025


def markers(voice):
	"""What this voice calls you, its own words, and the lines it really says, from its card"""
	for path in ("linegen/voices/%s.json" % voice, PILOT + "cards/%s.json" % voice):
		if os.path.exists(path):
			with open(path, encoding="utf-8") as f:
				card = json.load(f)
			address = re.findall(r"[a-z']+", (card.get("address") or "").split(",")[0].split(" ")[0].lower())
			native = [w["word"].lower() for w in card.get("nativeWords", [])]
			return (address[0] if address else None), native, card.get("canon", [])
	return None, [], []


def main():
	voices, by_id = cells()
	candidates = read_jsonl(PILOT + "candidates.jsonl")
	lint = {row["id"]: row for row in read_tsv(BUILD + "lint.tsv")}
	if len(lint) < len(candidates):
		raise SystemExit("lint.tsv is behind candidates.jsonl: rerun GeneratedLinesTest first")

	dropped = collections.Counter()
	problems = collections.Counter()
	kept = []
	shapes_by_voice = collections.Counter()
	shapes_by_cell = collections.defaultdict(set)
	tics = collections.Counter()
	passing = collections.Counter(by_id[c["cell"]]["voice"] for c in candidates if lint[c["id"]]["ok"] == "true")
	bank = {}
	# Unusual lines first, so when two collide the less obvious one survives
	for c in sorted(candidates, key=lambda c: c.get("p", 0.5)):
		row = lint[c["id"]]
		if row["ok"] != "true":
			dropped["lint"] += 1
			for problem in row["problems"].split("; "):
				problems[problem.split(",")[0].split(" which")[0]] += 1
			continue
		cell = by_id[c["cell"]]
		voice = cell["voice"]
		topic = cell["topic"]
		if topic not in bank:
			bank[topic] = bank_lines(topic, voices[voice]["npc"])
		if any(similar(c["line"], b) >= TOO_SIMILAR for b in bank[topic]):
			dropped["copies the bank"] += 1
			continue
		if any(by_id[k["cell"]]["voice"] == voice and similar(c["line"], k["line"]) >= TOO_SIMILAR for k in kept):
			dropped["near-copy"] += 1
			continue
		shape = skeleton(c["line"])
		if shape in shapes_by_cell[c["cell"]] or shapes_by_voice[(voice, shape)] >= SHAPE_PER_VOICE:
			dropped["overused shape"] += 1
			continue
		address, native, canon = markers(voice)
		# Their real lines show the voice; a line that reworks one is Jagex's, not ours
		if any(similar(c["line"], q) >= TOO_SIMILAR for q in canon):
			dropped["copies the game"] += 1
			continue
		w = words(c["line"])
		used = [("opening", " ".join(w[:2]), OPENING_SHARE)]
		if address and address in w:
			used.append(("address", address, ADDRESS_SHARE))
		used += [("native", n, NATIVE_SHARE) for n in native if n in c["line"].lower()]
		over = [kind + " " + word for kind, word, share in used if tics[(voice, word)] + 1 > max(2, share * passing[voice])]
		if over:
			dropped["voice tic: " + over[0].split(" ")[0]] += 1
			continue
		for _, word, _ in used:
			tics[(voice, word)] += 1
		shapes_by_cell[c["cell"]].add(shape)
		shapes_by_voice[(voice, shape)] += 1
		kept.append(c)

	kept.sort(key=lambda c: c["id"])
	write_jsonl(PILOT + "thinned.jsonl", kept)
	print("%d candidates, %d kept" % (len(candidates), len(kept)))
	for reason, n in dropped.most_common():
		print("  dropped %d: %s" % (n, reason))
	for problem, n in problems.most_common(12):
		print("    lint %d: %s" % (n, problem))
	per_cell = collections.Counter(c["cell"] for c in kept)
	thin = sorted(cell for cell, n in per_cell.items() if n < 5)
	if thin:
		print("cells with under five left: " + ", ".join(thin))


if __name__ == "__main__":
	main()
