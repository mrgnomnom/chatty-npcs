"""
Writes a batch's accepted gear lines into the plugin: one topic per new item or pet in
src/main/resources/com/chattynpcs/chatter.json, from the batch's gear.json. A voice with a `key`
(the alliance in the Fractured Archive) says its lines under "<key>/<tone>"; one without says the
topic's shared lines, for anyone. A topic already in the bank is replaced whole.

Only run this on lines you've read. Run from the repository root:
python3 linegen/tools/export_items.py archive
"""
import collections
import json
import sys

from common import read_jsonl

BANK = "src/main/resources/com/chattynpcs/chatter.json"


def main():
	batch = "linegen/%s/" % (sys.argv[1] if len(sys.argv) > 1 else "archive")
	with open(batch + "gear.json", encoding="utf-8") as f:
		gear = json.load(f)
	with open(batch + "cells.json", encoding="utf-8") as f:
		cells = {c["id"]: c for c in json.load(f)["cells"]}

	lines = collections.defaultdict(lambda: collections.defaultdict(list))
	for c in read_jsonl(batch + "accepted.jsonl"):
		cell = cells[c["cell"]]
		voice = gear["voices"].get(cell["voice"])
		if voice is None:
			continue
		key = cell["tone"] if not voice.get("key") else "%s/%s" % (voice["key"], cell["tone"])
		lines[cell["topic"]][key].append(c["line"])

	with open(BANK, encoding="utf-8") as f:
		bank = json.load(f)
	topics = bank["topics"]
	written = 0
	for t in gear["topics"]:
		if not lines[t["id"]]:
			continue
		topic = {"id": t["id"], "priority": t["priority"], "lean": t["lean"]}
		for kind in ("item", "pet"):
			if kind in t:
				topic[kind] = t[kind]
		topic["lines"] = {k: sorted(v) for k, v in sorted(lines[t["id"]].items())}
		ids = [x["id"] for x in topics]
		if t["id"] in ids:
			topics[ids.index(t["id"])] = topic
		else:
			# Pets go with the pets, gear with the gear
			same = [i for i, x in enumerate(topics) if ("pet" in x) == ("pet" in topic)]
			topics.insert(same[-1] + 1 if same else len(topics), topic)
		written += sum(len(v) for v in topic["lines"].values())
	with open(BANK, "w", encoding="utf-8", newline="\n") as f:
		f.write(json.dumps(bank, indent="\t", ensure_ascii=False) + "\n")
	print("%d topics, %d lines" % (sum(1 for t in gear["topics"] if lines[t["id"]]), written))


if __name__ == "__main__":
	main()
