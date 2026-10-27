"""
The rating site: rate any number of lines, blind, then see what the pipeline and the eval judges
made of each. Standard library only. From the repository root:

    ~/.venvs/chatty-eval/bin/python linegen/eval/server.py

then open http://localhost:8765. Run it with the eval environment's Python so the Score and Tune
buttons can start run_evals.py and tune.py with it.
"""
import json
import os
import random
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import items as catalogue  # noqa: E402

PORT = int(os.environ.get("CHATTY_EVAL_PORT", "8765"))
STATIC = os.path.join(catalogue.EVAL, "static")
JOB = os.path.join(catalogue.RESULTS, "job.json")
REPORT = os.path.join(catalogue.RESULTS, "report.json")

lock = threading.Lock()
cat = catalogue.Catalogue()
labels = catalogue.latest_labels()
jobs = {}


def pipeline_matches(item_id, want):
	p = cat.pipeline.get(item_id)
	if want == "any" or p is None:
		return want == "any"
	if want == "shipped":
		return p.get("shipped", False)
	if want == "rejected":
		return not p.get("shipped", False)
	return True


def select(q):
	source = q.get("source", "characters")
	n = max(1, min(int(q.get("n", 20)), 5000))
	show = q.get("show", "unlabelled")
	pipe = q.get("pipeline", "any")
	pool = []
	for item in cat.items.values():
		if source != "all" and item["source"] != source:
			continue
		if q.get("speaker") and item["voice"] != q["speaker"]:
			continue
		if q.get("tone") and item["tone"] != q["tone"]:
			continue
		if q.get("stateKind") and item["stateKind"] != q["stateKind"]:
			continue
		if q.get("search") and q["search"].lower() not in item["line"].lower():
			continue
		labelled = item["id"] in labels
		if (show == "unlabelled" and labelled) or (show == "labelled" and not labelled):
			continue
		if pipe != "mixed" and not pipeline_matches(item["id"], pipe):
			continue
		pool.append(item)
	rng = random.Random(q.get("seed") or None)
	matching = len(pool)
	if pipe == "mixed":
		# Half the pipeline shipped, half it didn't, shuffled: a blind test of its verdicts
		shipped = [i for i in pool if cat.pipeline.get(i["id"], {}).get("shipped")]
		other = [i for i in pool if not cat.pipeline.get(i["id"], {}).get("shipped")]
		rng.shuffle(shipped)
		rng.shuffle(other)
		pool = shipped[:n // 2 + n % 2] + other[:n // 2]
		pool += (shipped[n // 2 + n % 2:] + other[n // 2:])[:n - len(pool)]
		rng.shuffle(pool)
	elif q.get("order") == "cell":
		pool.sort(key=lambda i: (i["cell"], i["id"]))
	else:
		rng.shuffle(pool)
	return [blind(i) for i in pool[:n]], matching


def blind(item):
	out = dict(item)
	if item["id"] in labels:
		out["label"] = labels[item["id"]]
	return out


def reveal(item_id):
	scores = catalogue.latest_scores().get(item_id, {})
	item = cat.items.get(item_id, {})
	fresh = {m: s for m, s in scores.items() if s.get("lineHash") == catalogue.line_hash(item.get("line", ""))}
	return {"pipeline": cat.pipeline.get(item_id), "scores": fresh, "label": labels.get(item_id)}


def meta():
	speakers = {}
	for item in cat.items.values():
		speakers.setdefault(item["source"], set()).add(item["voice"])
	counts = {}
	for item in cat.items.values():
		counts[item["source"]] = counts.get(item["source"], 0) + 1
	return {
		"sources": counts,
		"speakers": {s: sorted(v) for s, v in speakers.items()},
		"tags": [{"id": t, "label": l} for t, l in catalogue.TAGS],
		"labelled": len(labels),
		"job": job_state(),
	}


def stats():
	rows = list(labels.values())
	by_source, tags = {}, {}
	agree = {"both keep": 0, "both cut": 0, "pipeline shipped, you cut": 0, "pipeline dropped, you kept": 0}
	for r in rows:
		s = by_source.setdefault(r.get("source", "?"), {"keep": 0, "cut": 0})
		s[r["verdict"]] = s.get(r["verdict"], 0) + 1
		for t in r.get("tags", []):
			tags[t] = tags.get(t, 0) + 1
		p = cat.pipeline.get(r["id"])
		if p and r.get("source") in ("characters", "pilot"):
			keep, shipped = r["verdict"] == "keep", p.get("shipped")
			key = ("both keep" if shipped else "pipeline dropped, you kept") if keep else ("pipeline shipped, you cut" if shipped else "both cut")
			agree[key] += 1
	report = None
	if os.path.exists(REPORT):
		with open(REPORT, encoding="utf-8") as f:
			report = json.load(f)
	return {"labelled": len(rows), "bySource": by_source, "tags": sorted(tags.items(), key=lambda kv: -kv[1]),
		"pipelineAgreement": agree, "report": report, "job": job_state()}


def save_label(body):
	item = cat.items.get(body["id"])
	if item is None or body.get("verdict") not in ("keep", "cut"):
		raise ValueError("unknown item or verdict")
	row = {
		"id": item["id"], "source": item["source"], "cell": item["cell"], "speaker": item["speaker"],
		"tone": item["tone"], "line": item["line"], "verdict": body["verdict"],
		"score": body.get("score"), "tags": [t for t in body.get("tags", []) if t in dict(catalogue.TAGS)],
		"note": (body.get("note") or "").strip(), "rewrite": (body.get("rewrite") or "").strip(),
		"at": time.strftime("%Y-%m-%dT%H:%M:%S"),
	}
	with lock:
		with open(catalogue.LABELS, "a", encoding="utf-8", newline="\n") as f:
			f.write(json.dumps(row, ensure_ascii=False) + "\n")
		labels[row["id"]] = row
	return row


def job_state():
	state = {}
	if os.path.exists(JOB):
		try:
			with open(JOB, encoding="utf-8") as f:
				state = json.load(f)
		except ValueError:
			pass
	for name, proc in list(jobs.items()):
		if proc.poll() is None:
			state["running"] = name
	return state


def start(name, args):
	with lock:
		if any(p.poll() is None for p in jobs.values()):
			raise ValueError("a job is already running")
		os.makedirs(catalogue.RESULTS, exist_ok=True)
		log = open(os.path.join(catalogue.RESULTS, name + ".log"), "w", encoding="utf-8")
		jobs[name] = subprocess.Popen([sys.executable, os.path.join(catalogue.EVAL, name + ".py")] + args,
			cwd=catalogue.ROOT, stdout=log, stderr=subprocess.STDOUT)


def tail(name, lines=40):
	path = os.path.join(catalogue.RESULTS, name + ".log")
	if not os.path.exists(path):
		return ""
	with open(path, encoding="utf-8", errors="replace") as f:
		return "".join(f.readlines()[-lines:])


class Handler(BaseHTTPRequestHandler):
	def log_message(self, fmt, *args):
		pass

	def send(self, status, body, kind="application/json"):
		data = body if isinstance(body, bytes) else json.dumps(body, ensure_ascii=False).encode("utf-8")
		self.send_response(status)
		self.send_header("Content-Type", kind + "; charset=utf-8")
		self.send_header("Content-Length", str(len(data)))
		self.send_header("Cache-Control", "no-store")
		self.end_headers()
		self.wfile.write(data)

	def do_GET(self):
		url = urlparse(self.path)
		q = {k: v[0] for k, v in parse_qs(url.query).items()}
		try:
			if url.path in ("/", "/index.html"):
				with open(os.path.join(STATIC, "index.html"), "rb") as f:
					return self.send(200, f.read(), "text/html")
			if url.path == "/api/meta":
				return self.send(200, meta())
			if url.path == "/api/items":
				chosen, total = select(q)
				return self.send(200, {"items": chosen, "matching": total})
			if url.path == "/api/reveal":
				return self.send(200, reveal(q["id"]))
			if url.path == "/api/stats":
				return self.send(200, stats())
			if url.path == "/api/log":
				return self.send(200, {"log": tail(q.get("name", "run_evals")), "job": job_state()})
			return self.send(404, {"error": "not found"})
		except Exception as e:  # a local tool: say what broke
			return self.send(500, {"error": str(e)})

	def do_POST(self):
		url = urlparse(self.path)
		length = int(self.headers.get("Content-Length", 0))
		body = json.loads(self.rfile.read(length) or b"{}")
		try:
			if url.path == "/api/label":
				return self.send(200, save_label(body))
			if url.path == "/api/custom":
				lines = body.get("lines", [])
				with lock:
					ids = cat.add_custom(body.get("speaker", "").strip(), body.get("situation", "").strip(), body.get("tone", "remark"), lines)
				return self.send(200, {"ids": ids})
			if url.path == "/api/run":
				args = ["--labelled"] if body.get("what", "labelled") == "labelled" else ["--ids"] + body.get("ids", [])
				if body.get("metrics"):
					args += ["--metrics", ",".join(body["metrics"])]
				start("run_evals", args)
				return self.send(200, {"started": True})
			if url.path == "/api/tune":
				start("tune", [])
				return self.send(200, {"started": True})
			return self.send(404, {"error": "not found"})
		except Exception as e:
			return self.send(400, {"error": str(e)})


def main():
	server = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
	print("Chatty NPCs line eval: http://localhost:%d  (%d lines, %d labelled)" % (PORT, len(cat.items), len(labels)), flush=True)
	try:
		server.serve_forever()
	except KeyboardInterrupt:
		pass


if __name__ == "__main__":
	main()
