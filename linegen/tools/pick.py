"""
Step 8: picks the lines worth your time. A line goes through only if the fact check found nothing
unsupported and nothing invented, the judge passed it, and it's among the best three of those in
its cell. Your own verdicts from the rating site (linegen/eval/labels.jsonl) outrank all of that:
a line you cut never ships, and a line you kept ships whatever the judge thought, as long as the
fact check passed it. A batch's cuts.jsonl holds lines cut on review, each with its reason, and its
added.jsonl lines added on review, which ship beside their cell's best three as long as the fact
check and the judge both passed them. If
linegen/eval/gate.json names an eval judge that matches your own verdicts (see
linegen/eval/README.md), a line that judge scored below its cut-off is dropped too.

In:  linegen/pilot/thinned.jsonl, verified.jsonl, judged.jsonl
Out: linegen/pilot/accepted.jsonl
"""
import collections
import hashlib
import json
import os

from common import PILOT, read_jsonl, write_jsonl

TOP = 3
GATE = "linegen/eval/gate.json"
SCORES = "linegen/eval/results/scores.jsonl"
LABELS = "linegen/eval/labels.jsonl"


def gated():
	"""The eval gate, and {id: hash of the line it scored} for each line it scored below its cut-off"""
	if not os.path.exists(GATE):
		return None, {}
	with open(GATE, encoding="utf-8") as f:
		gate = json.load(f)
	latest = {}
	for row in read_jsonl(SCORES):
		if row["metric"] == gate["metric"] and row.get("model") == gate["model"] and row["score"] is not None:
			latest[row["id"]] = row
	return gate, {i: r["lineHash"] for i, r in latest.items() if r["score"] < gate["threshold"]}


def verdicts():
	"""{id: (keep or cut, the line as rated)}, the last rating of each line winning"""
	out = {}
	for row in read_jsonl(LABELS) if os.path.exists(LABELS) else []:
		out[row["id"]] = (row["verdict"], row["line"])
	return out


def line_hash(line):
	return hashlib.sha1(line.encode("utf-8")).hexdigest()[:10]


def main():
	thinned = read_jsonl(PILOT + "thinned.jsonl")
	verified = {v["id"]: v for v in read_jsonl(PILOT + "verified.jsonl")}
	judged = {j["id"]: j for j in read_jsonl(PILOT + "judged.jsonl")}
	missing = [c["id"] for c in thinned if c["id"] not in verified or c["id"] not in judged]
	if missing:
		raise SystemExit("%d lines not yet checked or judged, e.g. %s" % (len(missing), missing[0]))

	gate, below = gated()
	yours = verdicts()
	# Cut on review, as long as the line hasn't changed since
	cuts = {c["id"]: c for c in read_jsonl(PILOT + "cuts.jsonl")} if os.path.exists(PILOT + "cuts.jsonl") else {}
	added = {c["id"]: c for c in read_jsonl(PILOT + "added.jsonl")} if os.path.exists(PILOT + "added.jsonl") else {}
	why = collections.Counter()
	ranked = collections.defaultdict(list)
	kept = []
	extra = []
	for c in thinned:
		v, j = verified[c["id"]], judged[c["id"]]
		# Only while the verdict is for the line as it now stands
		verdict = yours.get(c["id"], (None, None))
		verdict = verdict[0] if verdict[1] == c["line"] else None
		if verdict == "cut":
			why["you cut it"] += 1
		elif c["id"] in cuts and cuts[c["id"]]["line"] == c["line"]:
			why["cut on review"] += 1
		elif c["id"] in added and added[c["id"]]["line"] == c["line"] and v["grounded"] and j["pass"]:
			extra.append(dict(c, rank=j.get("rank") or 0))
		elif verdict == "keep" and v["grounded"]:
			kept.append(dict(c, rank=0))
		# Only while the score is for the line as it now stands
		elif below.get(c["id"]) == line_hash(c["line"]):
			why["below the eval gate (%s)" % gate["metric"]] += 1
		elif not v["grounded"]:
			why["not grounded"] += 1
		elif not j["pass"]:
			why["judge failed it"] += 1
		elif not j.get("rank"):
			why["outranked in its cell"] += 1
		else:
			ranked[c["cell"]].append(dict(c, rank=j["rank"]))
	# A top line that turned out ungrounded leaves room for the next one down
	accepted = list(kept)
	yours_in = collections.Counter(c["cell"] for c in kept)
	for cell, lines in ranked.items():
		lines.sort(key=lambda c: c["rank"])
		# The lines you kept take their cell's places first
		room = max(0, TOP - yours_in[cell])
		accepted += lines[:room]
		why["outranked in its cell"] += len(lines[room:])
	accepted += extra
	if kept:
		print("%d you kept, shipped whatever the judge thought" % len(kept))
	if extra:
		print("%d added on review" % len(extra))
	accepted.sort(key=lambda c: (c["cell"], c["rank"]))
	write_jsonl(PILOT + "accepted.jsonl", accepted)

	print("%d checked, %d accepted" % (len(thinned), len(accepted)))
	for reason, n in why.most_common():
		print("  %d: %s" % (n, reason))
	ungrounded = collections.Counter()
	for v in verified.values():
		for claim in v["claims"]:
			if claim["support"] == "none":
				ungrounded[v["id"].split("-")[0]] += 1
	print("unsupported claims by voice: %s" % dict(ungrounded))
	print("invented something: %d" % sum(1 for v in verified.values() if v.get("invents")))


if __name__ == "__main__":
	main()
