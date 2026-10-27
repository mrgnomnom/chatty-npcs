"""
Turns your labels into what the next generation run reads. From the repository root:

    python3 linegen/eval/tune.py

1. Anchors. Every rewrite you gave becomes a pair (your version against the original), and every
   keep paired with a cut from the same cell, or failing that the same speaker and tone, becomes
   another. They go in linegen/judge/anchors.jsonl, which the judge reads before anything else.
2. Lessons. Your commonest reasons for cutting, with your own examples, and the lines you rated
   best, go in linegen/judge/lessons.md, which the writers and the judge read first.
3. A gate. If one eval judge predicts your verdicts well enough (results/report.json, from
   run_evals.py), its cut-off goes in gate.json, and pick.py drops lines it scored below that.
"""
import collections
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import items as catalogue  # noqa: E402

ANCHORS = os.path.join(catalogue.LINEGEN, "judge", "anchors.jsonl")
LESSONS = os.path.join(catalogue.LINEGEN, "judge", "lessons.md")
GATE = os.path.join(catalogue.EVAL, "gate.json")
REPORT = os.path.join(catalogue.RESULTS, "report.json")
# A judge has to be this good, on this many of your labels, before it may cut lines on its own
MIN_AUC = 0.75
MIN_LABELS = 40
EXAMPLES = 3


def fault(row):
	names = dict(catalogue.TAGS)
	parts = [names.get(t, t) for t in row.get("tags", [])]
	if row.get("note"):
		parts.append(row["note"])
	return "; ".join(parts) or "you cut it"


def anchors(rows, cat):
	have = catalogue.read_jsonl(ANCHORS)
	seen = {(a["good"], a["weak"]) for a in have}
	added = []

	def add(good, weak, why):
		voice = cat.items[good["id"]]["voice"] if good["id"] in cat.items else good.get("speaker", "")
		pair = (good["line"], weak["line"])
		if pair not in seen and good["line"] != weak["line"]:
			seen.add(pair)
			added.append({"voice": voice, "tone": good["tone"], "good": good["line"], "weak": weak["line"], "fault": why, "source": "labels"})

	for r in rows:
		if r.get("rewrite"):
			add(dict(r, line=r["rewrite"]), r, fault(r) if r["verdict"] == "cut" else (r.get("note") or "your rewrite"))
	kept, cut = collections.defaultdict(list), collections.defaultdict(list)
	for r in rows:
		(kept if r["verdict"] == "keep" else cut)[r["cell"]].append(r)
	spare_keeps, spare_cuts = [], []
	for cell in set(kept) | set(cut):
		k, c = kept.get(cell, []), cut.get(cell, [])
		for good, weak in zip(k, c):
			add(good, weak, fault(weak))
		spare_keeps += k[len(c):]
		spare_cuts += c[len(k):]
	# Then across cells, by speaker and tone
	by = collections.defaultdict(list)
	for r in spare_keeps:
		by[(r["speaker"], r["tone"])].append(r)
	for weak in spare_cuts:
		pool = by.get((weak["speaker"], weak["tone"]))
		if pool:
			add(pool.pop(), weak, fault(weak))
	with open(ANCHORS, "a", encoding="utf-8", newline="\n") as f:
		for a in added:
			f.write(json.dumps(a, ensure_ascii=False) + "\n")
	return len(added), len(have) + len(added)


def lessons(rows):
	names = dict(catalogue.TAGS)
	cuts = [r for r in rows if r["verdict"] == "cut"]
	keeps = [r for r in rows if r["verdict"] == "keep"]
	tags = collections.Counter(t for r in cuts for t in r.get("tags", []))
	out = [
		"# What the curator keeps and cuts",
		"",
		"Written by `linegen/eval/tune.py` from %d lines the person who curates this plugin rated by hand"
		" (%d kept, %d cut), %s. Their verdicts outrank any rule of thumb: when this and a prompt"
		" disagree, this wins. Don't copy these lines; learn what makes them work or fail." % (
			len(rows), len(keeps), len(cuts), time.strftime("%Y-%m-%d")),
		"",
	]
	if tags:
		out += ["## Why they cut, commonest first", ""]
		for tag, n in tags.most_common():
			share = 100 * n / max(1, len(cuts))
			out.append("### %s (%d of %d cuts, %.0f%%)" % (names.get(tag, tag), n, len(cuts), share))
			out.append("")
			for r in [r for r in cuts if tag in r.get("tags", [])][:EXAMPLES]:
				note = " (%s)" % r["note"] if r.get("note") else ""
				out.append("- %s, %s: \"%s\"%s" % (r["speaker"], r["tone"], r["line"], note))
			out.append("")
	untagged = [r for r in cuts if not r.get("tags") and r.get("note")]
	if untagged:
		out += ["## Other cuts, in their words", ""]
		out += ["- \"%s\": %s" % (r["line"], r["note"]) for r in untagged[:10]]
		out.append("")
	rewrites = [r for r in rows if r.get("rewrite")]
	if rewrites:
		out += ["## How they'd have written it", ""]
		out += ["- \"%s\" became \"%s\"" % (r["line"], r["rewrite"]) for r in rewrites[:15]]
		out.append("")
	best = sorted((r for r in keeps if r.get("score")), key=lambda r: -r["score"])[:15] or keeps[:15]
	if best:
		out += ["## Lines they rated best", ""]
		out += ["- %s, %s: \"%s\"%s" % (r["speaker"], r["tone"], r["line"], " (%s)" % r["note"] if r.get("note") else "") for r in best]
		out.append("")
	with open(LESSONS, "w", encoding="utf-8", newline="\n") as f:
		f.write("\n".join(out))
	return tags


def gate(n_labels):
	if not os.path.exists(REPORT):
		return "no eval run yet, so no gate: score your rated lines first"
	with open(REPORT, encoding="utf-8") as f:
		report = json.load(f)
	usable = [m for m in report["metrics"] if not m["metric"].startswith("pipeline") and m["metric"] != "geval_mean"
		and m["auc"] is not None and m["n"] >= MIN_LABELS]
	if not usable:
		if os.path.exists(GATE):
			os.remove(GATE)
		return "no gate: a judge needs at least %d of your rated lines scored (you have %d)" % (MIN_LABELS, n_labels)
	best = max(usable, key=lambda m: m["auc"])
	if best["auc"] < MIN_AUC:
		if os.path.exists(GATE):
			os.remove(GATE)
		return "no gate: the best judge, %s, only reaches AUC %.2f against you (needs %.2f). Label more, or keep it advisory" % (
			best["metric"], best["auc"], MIN_AUC)
	g = {"metric": best["metric"], "threshold": best["threshold"], "auc": best["auc"], "kappa": best["kappa"],
		"n": best["n"], "model": report["model"], "at": report["at"]}
	with open(GATE, "w", encoding="utf-8") as f:
		json.dump(g, f, indent="\t")
	return "gate set: %s at %.2f (AUC %.2f, kappa %.2f over %d lines). pick.py drops lines it scored lower" % (
		g["metric"], g["threshold"], g["auc"], g["kappa"], g["n"])


def main():
	cat = catalogue.Catalogue()
	rows = [r for r in catalogue.latest_labels().values()]
	if not rows:
		raise SystemExit("nothing rated yet")
	added, total = anchors(rows, cat)
	print("Anchors: %d new pairs from your labels, %d in all (%s)" % (added, total, os.path.relpath(ANCHORS, catalogue.ROOT)))
	tags = lessons(rows)
	top = ", ".join("%s %d" % (dict(catalogue.TAGS).get(t, t), n) for t, n in tags.most_common(4)) or "no reasons tagged yet"
	print("Lessons: %s (%s)" % (os.path.relpath(LESSONS, catalogue.ROOT), top))
	print("Gate: " + gate(len(rows)))


if __name__ == "__main__":
	main()
