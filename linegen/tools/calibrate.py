"""
Step 10: reads your filled sheets and says two things.

1. Do the new lines beat today's? From pairs.tsv: how often the new line won, with a 95% interval.
   The pilot's bar is 65% of decided pairs. Below it, don't scale up; fix what lost first.
2. Does the judge agree with you? From lines.tsv: how often it kept what you kept and cut what you
   cut, and kappa, which discounts agreement by chance. Under about 0.4, its verdicts are weak, and
   your keeps and cuts become its new anchors before the next run.

Your labels are also written to linegen/judge/labels.jsonl, and every keep paired with a cut from
the same cell becomes an anchor pair in linegen/judge/anchors.jsonl, so the judge learns from you.
"""
import collections
import math
import os

from common import PILOT, read_jsonl, read_tsv, write_jsonl

SHEETS = PILOT + "sheets/"
BAR = 0.65


def wilson(wins, n):
	if n == 0:
		return 0.0, 0.0
	p, z = wins / n, 1.96
	centre = (p + z * z / (2 * n)) / (1 + z * z / n)
	half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n)
	return centre - half, centre + half


def pairs():
	sheet = {row["pair"]: row for row in read_tsv(SHEETS + "pairs.tsv")}
	keys = read_tsv(SHEETS + "pairs_key.tsv")
	tally = collections.defaultdict(collections.Counter)
	for key in keys:
		better = sheet[key["pair"]]["better"].strip().upper()
		if better not in ("A", "B", "SAME"):
			continue
		voice = key["cell"].split("/")[0]
		outcome = "same" if better == "SAME" else ("new" if better == key["new_is"] else "today")
		for v in (voice, "all"):
			tally[v][outcome] += 1
	if not tally:
		print("pairs.tsv: nothing filled in yet")
		return
	print("New lines against today's (ties left out of the share):")
	for voice in sorted(tally, key=lambda v: v == "all"):
		t = tally[voice]
		decided = t["new"] + t["today"]
		low, high = wilson(t["new"], decided)
		share = t["new"] / decided if decided else 0
		print("  %-8s new %3d, today %3d, same %3d: new wins %.0f%% (%.0f-%.0f%%)" % (
			voice, t["new"], t["today"], t["same"], 100 * share, 100 * low, 100 * high))
	t = tally["all"]
	decided = t["new"] + t["today"]
	low, _ = wilson(t["new"], decided)
	if decided and t["new"] / decided >= BAR:
		print("  Clears the %.0f%% bar%s." % (100 * BAR, "" if low >= 0.5 else ", though the interval still reaches below half: label more pairs"))
	else:
		print("  Below the %.0f%% bar: look at what lost before scaling up." % (100 * BAR))


def lines():
	sheet = {row["row"]: row for row in read_tsv(SHEETS + "lines.tsv")}
	keys = {row["row"]: row for row in read_tsv(SHEETS + "lines_key.tsv")}
	labelled = []
	for row, s in sheet.items():
		verdict = s["verdict"].strip().lower()
		if verdict in ("keep", "cut"):
			labelled.append((s, keys[row], verdict == "keep"))
	if not labelled:
		print("lines.tsv: nothing filled in yet")
		return
	both = sum(1 for _, k, keep in labelled if keep and k["judge_pass"] == "True")
	neither = sum(1 for _, k, keep in labelled if not keep and k["judge_pass"] == "False")
	judge_only = sum(1 for _, k, keep in labelled if not keep and k["judge_pass"] == "True")
	you_only = sum(1 for _, k, keep in labelled if keep and k["judge_pass"] == "False")
	n = len(labelled)
	agree = (both + neither) / n
	p_you, p_judge = (both + you_only) / n, (both + judge_only) / n
	chance = p_you * p_judge + (1 - p_you) * (1 - p_judge)
	kappa = (agree - chance) / (1 - chance) if chance < 1 else 0
	print("The judge against you, over %d lines:" % n)
	print("  both keep %d, both cut %d, judge keeps what you cut %d, judge cuts what you keep %d" % (both, neither, judge_only, you_only))
	print("  agreement %.0f%%, kappa %.2f; of the lines it passes, you keep %.0f%%" % (
		100 * agree, kappa, 100 * both / max(1, both + judge_only)))

	write_jsonl("linegen/judge/labels.jsonl", [
		{"id": k["id"], "cell": k["cell"], "voice": s["voice"], "tone": s["tone"], "line": s["line"],
			"keep": keep, "note": s["note"].strip()} for s, k, keep in labelled])

	anchors = read_jsonl("linegen/judge/anchors.jsonl")
	have = {(a["good"], a["weak"]) for a in anchors}
	kept, cut = collections.defaultdict(list), collections.defaultdict(list)
	for s, k, keep in labelled:
		(kept if keep else cut)[k["cell"]].append(s)
	added = 0
	for cell in kept:
		for good, weak in zip(kept[cell], cut.get(cell, [])):
			if (good["line"], weak["line"]) not in have:
				anchors.append({"voice": good["voice"], "tone": good["tone"], "good": good["line"], "weak": weak["line"],
					"fault": weak["note"].strip() or "you cut it", "source": "labels"})
				added += 1
	write_jsonl("linegen/judge/anchors.jsonl", anchors)
	print("  %d of your keep-and-cut pairs added to the judge's anchors" % added)


def main():
	if not os.path.exists(SHEETS + "pairs.tsv"):
		raise SystemExit("no sheets yet: run tools/sheets.py")
	pairs()
	print()
	lines()


if __name__ == "__main__":
	main()
