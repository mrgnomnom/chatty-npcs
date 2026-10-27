"""
Scores lines with real eval frameworks on a local model, then measures each judge against your
labels. Run with the eval environment (see README.md), from the repository root:

    ~/.venvs/chatty-eval/bin/python linegen/eval/run_evals.py --labelled
    ~/.venvs/chatty-eval/bin/python linegen/eval/run_evals.py --source characters --n 200

The judges:
- DeepEval G-Eval, four criteria taken from judge.md: in character, implied, passing-remark craft,
  and knowing only what the speaker could
- DeepEval Faithfulness: is every claim in the line backed by the facts it was written from?
- OpenEvals LLM-as-judge, few-shot on your own keeps and cuts: "would you keep this?". When it
  scores a line you've rated, that line is left out of its examples, so its agreement is honest.

Scores go to results/scores.jsonl, and are reused while the line, the model and (for the few-shot
judge) your labels are unchanged. The agreement report goes to results/report.json.
"""
import argparse
import asyncio
import hashlib
import json
import math
import os
import random
import sys
import time

os.environ.setdefault("DEEPEVAL_TELEMETRY_OPT_OUT", "YES")
os.environ.setdefault("DEEPEVAL_DISABLE_DOTENV", "1")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import items as catalogue  # noqa: E402

from deepeval.metrics import FaithfulnessMetric, GEval  # noqa: E402
from deepeval.models import OllamaModel  # noqa: E402
from deepeval.test_case import LLMTestCase, SingleTurnParams  # noqa: E402
from langchain_ollama import ChatOllama  # noqa: E402
from openevals.llm import create_async_llm_as_judge  # noqa: E402

MODEL = os.environ.get("CHATTY_EVAL_MODEL", "qwen3:14b")
OLLAMA = os.environ.get("OLLAMA_HOST", "http://localhost:11434")
JOB = os.path.join(catalogue.RESULTS, "job.json")
REPORT = os.path.join(catalogue.RESULTS, "report.json")
FEW_SHOT = 16


class LocalJudge(OllamaModel):
	"""DeepEval's Ollama model, with Qwen3's thinking off: a one-line verdict doesn't need it, and
	it's several times faster"""

	def _options(self):
		return {"temperature": 0, **(self.generation_kwargs or {})}

	def generate(self, prompt, schema=None, audio=None):
		r = self.load_model().chat(model=self.name, messages=self._prompt_messages(prompt, audio),
			format=schema.model_json_schema() if schema else None, think=False, options=self._options())
		return (schema.model_validate_json(r.message.content) if schema else r.message.content), 0

	async def a_generate(self, prompt, schema=None, audio=None):
		r = await self.load_model(async_mode=True).chat(model=self.name, messages=self._prompt_messages(prompt, audio),
			format=schema.model_json_schema() if schema else None, think=False, options=self._options())
		return (schema.model_validate_json(r.message.content) if schema else r.message.content), 0


PARAMS = [SingleTurnParams.INPUT, SingleTurnParams.ACTUAL_OUTPUT]

GEVAL = {
	"voice": ("In character", [
		"Read who the speaker is in the input: who they are, what they notice and value, how they address people, their grammar and rhythm.",
		"Decide whether the first thing the line notices is what this particular speaker would notice, said in their grammar and rhythm.",
		"Score low if any other NPC could have said the line unchanged; score high only if it could only be this speaker.",
		"Score very low if it does anything listed under neverSay or taboos for this speaker.",
	]),
	"implied": ("Implied", [
		"Find the joke, jab or feeling in the line.",
		"Score low if any clause explains the joke or states the feeling outright (for example 'I like that', 'which is funny', 'I'm proud', or a tag that spells out the point).",
		"For a compliment, score low if it gushes; for an insult, score low unless it is aimed at a choice the player made and plainly not meant to wound.",
		"Score high when the point lands without being explained and the mood fits the tone wanted.",
	]),
	"craft": ("Passing-remark craft", [
		"Check the line hangs on something the speaker can see now, has heard (marked as hearsay), or their own business.",
		"Check it does exactly one job (notice, compliment, tease, complain, gossip or mutter) and names the particular thing in plain words.",
		"Check it still feels finished if the player walks on without answering, and that it can be said aloud in one breath, with no written-prose constructions.",
		"Score low for modern, American or real-world words and ideas, and for stock jokes or stale phrasing.",
	]),
	"boundary": ("Knows only what they could", [
		"List every claim the line makes about the player: their looks, clothes, gear, what they carry, what they did, where they've been, their habits.",
		"Each such claim must follow from the situation in the input; score low for any that doesn't (cells about named characters show no gear or appearance at all).",
		"List every claim about the world, lore or other people; each must come from the facts given, be the speaker's own everyday business, or be marked as hearsay or a guess.",
		"Score very low if the line gives away anything beyond the player's stated quest progress.",
	]),
}

CURATOR_SYSTEM = """You help curate one-line remarks that NPCs in Old School RuneScape say overhead as a
player walks past, for a RuneLite plugin. The curator keeps lines that sound like that particular
speaker, leave the joke or feeling implied, name something specific, claim nothing the speaker
couldn't know, and could be said aloud in one breath. They cut generic, explained, stale, modern or
invented lines. Learn their taste from the examples, which are their real verdicts."""

CURATOR_PROMPT = """Would the curator keep this line?

<context>
{inputs}
</context>
<line>
{outputs}
</line>

Score how likely they are to keep it, from 0 (certainly cut) to 1 (certainly keep). Judge it as
they did in the examples below."""


def tag_text(tags):
	names = dict(catalogue.TAGS)
	return ", ".join(names.get(t, t) for t in tags)


def few_shot(item, labels, cat):
	"""Up to FEW_SHOT of your verdicts, half keeps and half cuts, nearest first: same speaker, then
	same tone. Never the line being scored."""
	rows = [r for r in labels.values() if r["id"] != item["id"] and r["id"] in cat.items]

	def near(r):
		return (r.get("speaker") != item["speaker"], r.get("tone") != item["tone"], r["id"])

	keeps = sorted((r for r in rows if r["verdict"] == "keep"), key=near)
	cuts = sorted((r for r in rows if r["verdict"] == "cut"), key=near)
	half = FEW_SHOT // 2
	chosen = keeps[:half] + cuts[:half]
	chosen += (keeps[half:] + cuts[half:])[:FEW_SHOT - len(chosen)]
	random.Random(item["id"]).shuffle(chosen)
	examples = []
	for r in chosen:
		why = []
		if r.get("tags"):
			why.append("Faults: " + tag_text(r["tags"]))
		if r.get("note"):
			why.append(r["note"])
		examples.append({
			"inputs": catalogue.judge_input(cat.items[r["id"]]),
			"outputs": r["line"],
			"reasoning": " ".join(why) or ("Kept." if r["verdict"] == "keep" else "Cut."),
			"score": 1.0 if r["verdict"] == "keep" else 0.0,
		})
	return examples


def labels_version(labels):
	return hashlib.sha1(json.dumps(sorted((r["id"], r["verdict"]) for r in labels.values())).encode()).hexdigest()[:8]


def write_job(state):
	os.makedirs(catalogue.RESULTS, exist_ok=True)
	with open(JOB, "w", encoding="utf-8") as f:
		json.dump(state, f)


async def score_all(chosen, metrics, cat, labels, parallel, force):
	judge = LocalJudge(model=MODEL, base_url=OLLAMA)
	chat = ChatOllama(model=MODEL, base_url=OLLAMA, temperature=0, reasoning=False)
	version = labels_version(labels)
	done = {}
	for row in catalogue.read_jsonl(catalogue.SCORES):
		done[(row["id"], row["metric"])] = row
	todo = []
	for item in chosen:
		for metric in metrics:
			if metric == "curator" and len([r for r in labels.values() if r["id"] != item["id"]]) < 4:
				continue
			old = done.get((item["id"], metric))
			key = (catalogue.line_hash(item["line"]), MODEL, version if metric == "curator" else None)
			if not force and old and (old.get("lineHash"), old.get("model"), old.get("labelsVersion")) == key:
				continue
			todo.append((item, metric))
	total = len(todo)
	print("%d scores to compute (%d lines, %d judges) on %s" % (total, len(chosen), len(metrics), MODEL), flush=True)
	write_job({"done": 0, "total": total, "started": time.strftime("%H:%M:%S")})
	sem = asyncio.Semaphore(parallel)
	count = {"n": 0, "failed": 0}
	out = open(catalogue.SCORES, "a", encoding="utf-8", newline="\n")
	started = time.time()

	async def one(item, metric):
		async with sem:
			case = LLMTestCase(input=catalogue.judge_input(item), actual_output=item["line"],
				retrieval_context=(item["facts"] or []) + [item["situation"]] + ([item["cardWho"]] if item["cardWho"] else []))
			score, reason = None, ""
			try:
				if metric in GEVAL:
					name, steps = GEVAL[metric]
					m = GEval(name=name, evaluation_steps=steps, evaluation_params=PARAMS, model=judge, async_mode=True)
					await m.a_measure(case, _show_indicator=False)
					score, reason = m.score, m.reason
				elif metric == "faithfulness":
					m = FaithfulnessMetric(model=judge, async_mode=True, include_reason=True)
					await m.a_measure(case, _show_indicator=False)
					score, reason = m.score, m.reason
				elif metric == "curator":
					evaluator = create_async_llm_as_judge(prompt=CURATOR_PROMPT, system=CURATOR_SYSTEM, judge=chat,
						continuous=True, feedback_key="keep", few_shot_examples=few_shot(item, labels, cat))
					r = await evaluator(inputs=catalogue.judge_input(item), outputs=item["line"])
					# Asked for 0 to 1, but a small model sometimes strays
					score, reason = min(max(float(r["score"]), 0.0), 1.0), r.get("comment") or ""
			except Exception as e:  # one bad judgement shouldn't sink the run
				count["failed"] += 1
				reason = "failed: %s" % e
			row = {"id": item["id"], "metric": metric, "score": score, "reason": reason, "lineHash": catalogue.line_hash(item["line"]),
				"model": MODEL, "labelsVersion": version if metric == "curator" else None, "at": time.strftime("%Y-%m-%dT%H:%M:%S")}
			out.write(json.dumps(row, ensure_ascii=False) + "\n")
			out.flush()
			count["n"] += 1
			if count["n"] % 10 == 0 or count["n"] == total:
				rate = (time.time() - started) / count["n"]
				print("  %d/%d scored, %.1fs each, about %d min left" % (count["n"], total, rate, rate * (total - count["n"]) / 60), flush=True)
				write_job({"done": count["n"], "total": total, "failed": count["failed"]})

	await asyncio.gather(*(one(i, m) for i, m in todo))
	out.close()
	if count["failed"]:
		print("%d judgements failed; their reasons say why" % count["failed"])


def auc(pos, neg):
	"""Chance a random keep scores above a random cut (Mann-Whitney), ties half"""
	if not pos or not neg:
		return None
	wins = 0.0
	for p in pos:
		for n in neg:
			wins += 1 if p > n else 0.5 if p == n else 0
	return wins / (len(pos) * len(neg))


def kappa(pairs, threshold):
	"""pairs of (score, kept); predicts keep when score >= threshold"""
	n = len(pairs)
	tp = sum(1 for s, k in pairs if s >= threshold and k)
	tn = sum(1 for s, k in pairs if s < threshold and not k)
	agree = (tp + tn) / n
	p_you = sum(1 for _, k in pairs if k) / n
	p_it = sum(1 for s, _ in pairs if s >= threshold) / n
	chance = p_you * p_it + (1 - p_you) * (1 - p_it)
	return agree, (agree - chance) / (1 - chance) if chance < 1 else 0.0


def measure(name, pairs):
	pairs = [(s, k) for s, k in pairs if s is not None]
	if len(pairs) < 2:
		return {"metric": name, "n": len(pairs), "auc": None, "threshold": None, "accuracy": None, "kappa": None}
	best = (None, -2.0, None)
	for t in sorted({s for s, _ in pairs}):
		acc, kap = kappa(pairs, t)
		if kap > best[1]:
			best = (t, kap, acc)
	a = auc([s for s, k in pairs if k], [s for s, k in pairs if not k])
	return {"metric": name, "n": len(pairs), "auc": None if a is None else round(a, 3), "threshold": best[0],
		"accuracy": None if best[2] is None else round(best[2], 3), "kappa": round(best[1], 3) if best[0] is not None else None}


def report(cat, labels):
	scores = catalogue.latest_scores()
	rated = [r for r in labels.values() if r["id"] in cat.items]
	metrics = []
	names = ["voice", "implied", "craft", "boundary", "faithfulness", "curator"]
	for name in names:
		pairs = []
		for r in rated:
			s = scores.get(r["id"], {}).get(name)
			if s and s.get("lineHash") == catalogue.line_hash(r["line"]) and s.get("model") == MODEL:
				pairs.append((s["score"], r["verdict"] == "keep"))
		metrics.append(measure(name, pairs))
	# All four G-Eval criteria together
	pairs = []
	for r in rated:
		s = scores.get(r["id"], {})
		vals = [s[m]["score"] for m in GEVAL if m in s and s[m]["score"] is not None]
		if len(vals) == len(GEVAL):
			pairs.append((sum(vals) / len(vals), r["verdict"] == "keep"))
	metrics.append(measure("geval_mean", pairs))
	# The pipeline's own verdicts, as yes/no judges, for comparison
	for name, field in (("pipeline_shipped", "shipped"), ("pipeline_judge", "judgePass"), ("pipeline_facts", "grounded")):
		pairs = []
		for r in rated:
			p = cat.pipeline.get(r["id"])
			if p and p.get(field) is not None and r.get("source") in ("characters", "pilot"):
				pairs.append((1.0 if p[field] else 0.0, r["verdict"] == "keep"))
		metrics.append(measure(name, pairs))
	eval_metrics = [m for m in metrics if m["auc"] is not None and not m["metric"].startswith("pipeline") and m["n"] >= 20]
	best = max(eval_metrics, key=lambda m: m["auc"])["metric"] if eval_metrics else None
	out = {"at": time.strftime("%Y-%m-%d %H:%M"), "model": MODEL, "labelled": len(rated),
		"keeps": sum(1 for r in rated if r["verdict"] == "keep"), "cuts": sum(1 for r in rated if r["verdict"] == "cut"),
		"metrics": metrics, "best": best}
	os.makedirs(catalogue.RESULTS, exist_ok=True)
	with open(REPORT, "w", encoding="utf-8") as f:
		json.dump(out, f, indent="\t")
	print("\nAgainst your %d labels (%d keeps, %d cuts):" % (out["labelled"], out["keeps"], out["cuts"]))
	print("  %-18s %5s %6s %8s %7s %6s" % ("judge", "lines", "AUC", "cut-off", "agrees", "kappa"))
	for m in metrics:
		if m["n"]:
			print("  %-18s %5d %6s %8s %7s %6s" % (m["metric"], m["n"], m["auc"], m["threshold"],
				"%d%%" % round(100 * m["accuracy"]) if m["accuracy"] is not None else "-", m["kappa"]))
	if best:
		print("Best match to your taste: %s" % best)
	return out


def main():
	ap = argparse.ArgumentParser()
	ap.add_argument("--labelled", action="store_true", help="score every line you've rated")
	ap.add_argument("--ids", nargs="*", default=[], help="score these items")
	ap.add_argument("--source", help="score unrated lines from a source: characters, pilot, bank, custom")
	ap.add_argument("--n", type=int, default=50, help="how many, with --source")
	ap.add_argument("--metrics", default="voice,implied,craft,boundary,faithfulness,curator")
	ap.add_argument("--parallel", type=int, default=int(os.environ.get("CHATTY_EVAL_PARALLEL", "2")))
	ap.add_argument("--force", action="store_true", help="rescore even if a fresh score exists")
	ap.add_argument("--report-only", action="store_true")
	args = ap.parse_args()

	cat = catalogue.Catalogue()
	labels = catalogue.latest_labels()
	if not args.report_only:
		chosen = []
		if args.labelled:
			chosen += [cat.items[i] for i in labels if i in cat.items]
		chosen += [cat.items[i] for i in args.ids if i in cat.items]
		if args.source:
			pool = [i for i in cat.items.values() if i["source"] == args.source and i["id"] not in labels]
			random.Random(0).shuffle(pool)
			chosen += pool[:args.n]
		if not chosen:
			raise SystemExit("nothing to score: rate some lines first, or pass --source")
		metrics = [m.strip() for m in args.metrics.split(",") if m.strip()]
		asyncio.run(score_all(chosen, metrics, cat, labels, args.parallel, args.force))
	report(cat, labels)
	write_job({"finished": time.strftime("%H:%M:%S")})


if __name__ == "__main__":
	main()
