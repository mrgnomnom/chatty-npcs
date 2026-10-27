"""
Shared reading and writing for the linegen tools. Run the tools from the repository root, naming
the batch: python3 linegen/tools/thin.py characters. Without one, it's the pilot.
"""
import csv
import json
import os
import re
import sys

RUN = sys.argv[1] if len(sys.argv) > 1 else "pilot"
PILOT = "linegen/%s/" % RUN
BUILD = "build/linegen/%s/" % RUN
BANK = "src/main/resources/com/chattynpcs/"

# Words that carry a sentence's shape rather than its content. Everything else becomes "w", so
# "Red armour! You'd be easy to spot from a glider." and "Blue hat! You'd be hard to miss from a
# tower." share a skeleton: the same joke wearing different clothes.
FUNCTION = set("""
a an the this that these those it its it's i i'm i'd i'll i've me my mine you you're you'd you'll
you've your yours he him his she her we us our they them their there here what who whom whose which
when where why how is are was were be been being am do does did done have has had having will would
shall should can could may might must not no nor never ever and or but so if then than as at by for
from in into of off on onto out over to up with without about after before again all any some such
too very just only still even well oh ooh ah eh here's that's what's who's there's isn't aren't
wasn't weren't don't doesn't didn't haven't hasn't hadn't won't wouldn't can't couldn't shouldn't
mustn't ain't one ones bit lot little much more most less least own same other another else
""".split())


def read_jsonl(path):
	if not os.path.exists(path):
		return []
	with open(path, encoding="utf-8") as f:
		return [json.loads(line) for line in f if line.strip()]


def write_jsonl(path, rows):
	with open(path, "w", encoding="utf-8", newline="\n") as f:
		for row in rows:
			f.write(json.dumps(row, ensure_ascii=False) + "\n")


def read_tsv(path):
	if not os.path.exists(path):
		return []
	with open(path, encoding="utf-8", newline="") as f:
		return list(csv.DictReader(f, delimiter="\t"))


def write_tsv(path, header, rows):
	with open(path, "w", encoding="utf-8", newline="") as f:
		out = csv.writer(f, delimiter="\t", lineterminator="\n")
		out.writerow(header)
		out.writerows(rows)


def cells():
	with open(PILOT + "cells.json", encoding="utf-8") as f:
		data = json.load(f)
	return data["voices"], {c["id"]: c for c in data["cells"]}


def words(line):
	return re.findall(r"[a-z']+", line.lower())


def skeleton(line):
	shape = re.sub(r"[a-z']+", lambda m: m.group() if m.group() in FUNCTION else "w", line.lower())
	return re.sub(r"(w )+w", "w", shape)


def trigrams(line):
	w = words(line)
	return {tuple(w[i:i + 3]) for i in range(len(w) - 2)} or {tuple(w)}


def similar(a, b):
	"""How much of two lines' word sequences they share, 0 to 1"""
	ta, tb = trigrams(a), trigrams(b)
	return len(ta & tb) / max(1, min(len(ta), len(tb)))


def cue(cell):
	"""What the player has going on in a cell, as a reader of the sheets needs it"""
	parts = []
	if cell.get("worn"):
		parts.append("wearing " + ", ".join(cell["worn"].values()))
	if cell.get("pet"):
		parts.append("followed by a " + cell["pet"])
	if cell.get("facts"):
		parts.append("looks: " + ", ".join(cell["facts"]))
	for quest, state in cell.get("quests", {}).items():
		parts.append(quest.replace("__", " & ").replace("_", " ").title() + ": " + state.replace("_", " ").lower())
	return "; ".join(parts) or "nothing in particular"


def bank_lines(topic, npc=None):
	"""
	Every line the bank already has for a topic. For a topic it doesn't have yet, like a named
	character's own, every line that character already says about their quests.
	"""
	found = []
	for file in ("chatter.json", "quests.json", "characters.json"):
		with open(BANK + file, encoding="utf-8") as f:
			for t in json.load(f)["topics"]:
				starter = (t.get("quest") or {}).get("starter")
				own = (t.get("character") or {}).get("name")
				if t["id"] == topic:
					for lines in t["lines"].values():
						found.extend(lines)
				elif npc and (own and own.lower() == npc.lower() or starter and re.search(starter, npc, re.I)):
					for key, lines in t["lines"].items():
						if key.startswith("starter/"):
							found.extend(lines)
	return found
