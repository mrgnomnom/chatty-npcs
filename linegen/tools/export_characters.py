"""
Writes the named characters' accepted lines into the plugin: one topic per character and state in
src/main/resources/com/chattynpcs/characters.json, said only by that character, only in that
state, and before anything else they could say. Every batch with cards of its own goes in: the
named characters, and the people of the Fractured Archive.

Only run this on lines you've read. Run from the repository root:
python3 linegen/tools/export_characters.py
"""
import collections
import json
import os

from common import read_jsonl

PRIORITY = 40
# Where the player is in their story matters more than what they'd say any day: as much as a quest
# giver's own quest, so before it the Cook frets about his cake as often as he asks for help
STATE_PRIORITY = 55
BATCHES = ["characters", "archive"]


def main():
	cells, cards = {}, {}
	lines = collections.defaultdict(lambda: collections.defaultdict(list))
	for batch in BATCHES:
		folder = "linegen/%s/" % batch
		if not os.path.exists(folder + "accepted.jsonl"):
			continue
		with open(folder + "cells.json", encoding="utf-8") as f:
			cells.update({c["id"]: c for c in json.load(f)["cells"]})
		for c in read_jsonl(folder + "accepted.jsonl"):
			cell = cells[c["cell"]]
			card = folder + "cards/%s.json" % cell["voice"]
			# A batch's gear lines go in with export_items.py
			if not os.path.exists(card):
				continue
			if cell["voice"] not in cards:
				with open(card, encoding="utf-8") as f:
					cards[cell["voice"]] = json.load(f)
			# Someone the game already gives lines of their own is left to them
			if cards[cell["voice"]].get("ownChat"):
				continue
			lines[cell["topic"]][cell["tone"]].append(c["line"])

	topics = []
	for topic_id in sorted(lines):
		cell = next(c for c in cells.values() if c["topic"] == topic_id)
		card = cards[cell["voice"]]
		topics.append({
			"id": topic_id,
			"priority": STATE_PRIORITY if cell["quests"] else PRIORITY,
			"wrap": False,
			# Only to tell apart people who share a name
			"regions": [card["region"]] if card.get("region") else [],
			"character": {"name": card["npc"], "quests": {q: [s] for q, s in cell["quests"].items()}},
			"lines": {"starter/" + tone: sorted(ls) for tone, ls in sorted(lines[topic_id].items())},
		})
	with open("src/main/resources/com/chattynpcs/characters.json", "w", encoding="utf-8", newline="\n") as f:
		json.dump({"topics": topics}, f, indent="\t", ensure_ascii=False)
		f.write("\n")
	print("%d topics, %d lines, for %d characters" % (len(topics), sum(len(ls) for t in lines.values() for ls in t.values()), len({t["character"]["name"] for t in topics})))


if __name__ == "__main__":
	main()
