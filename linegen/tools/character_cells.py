"""
Turns the named characters' cards into a batch: linegen/<batch>/cells.json, one cell for each
character, state and tone. Checks each card's spot is inside its region, since that region is how
the plugin tells two people of the same name apart.

A batch can also have gear.json: new items and pets, and who remarks on them. Each topic gets a
cell for each of those voices and each tone.

Run from the repository root: python3 linegen/tools/character_cells.py [batch]
(the named characters, without one)
"""
import glob
import json
import os
import sys

TONES = ["remark", "compliment", "insult"]
# Their life in general, then the moments that matter most: a dozen states each would be written thin
STATES = 3


def inside(regions, region, x, y):
	for r in regions:
		if r["id"] == region:
			return any(b[0] <= x <= b[2] and b[1] <= y <= b[3] for b in r["boxes"])
	return False


def main():
	with open("src/main/resources/com/chattynpcs/regions.json", encoding="utf-8") as f:
		regions = json.load(f)["regions"]
	batch = "linegen/%s/" % (sys.argv[1] if len(sys.argv) > 1 else "characters")
	voices, cells = {}, []
	for path in sorted(glob.glob(batch + "cards/*.json")):
		with open(path, encoding="utf-8") as f:
			card = json.load(f)
		if card.get("region") and not inside(regions, card["region"], card["x"], card["y"]):
			print("%s stands outside %s: fix the card's spot or region" % (card["id"], card["region"]))
		voices[card["id"]] = {"npc": card["npc"], "x": card["x"], "y": card["y"], "plane": card.get("plane", 0), "speech": card.get("speech")}
		states = card["states"][:STATES]
		# Most players meet them after their story, so that moment is always written too
		later = [st for st in card["states"][STATES:] if st["id"].startswith("after_")]
		for state in states + later[:1]:
			for tone in TONES:
				cells.append({"id": "%s/%s/%s" % (card["id"], state["id"], tone), "voice": card["id"],
					"topic": "%s_%s" % (card["id"], state["id"]), "tone": tone, "quests": state.get("quests", {})})
	gear_cells = gear(batch, voices)
	cells += gear_cells
	with open(batch + "cells.json", "w", encoding="utf-8", newline="\n") as f:
		json.dump({"voices": voices, "cells": cells}, f, indent="\t", ensure_ascii=False)
		f.write("\n")
	print("%d voices, %d cells (%d on gear)" % (len(voices), len(cells), len(gear_cells)))


def gear(batch, voices):
	if not os.path.exists(batch + "gear.json"):
		return []
	with open(batch + "gear.json", encoding="utf-8") as f:
		g = json.load(f)
	cells = []
	for voice_id, voice in g["voices"].items():
		voices[voice_id] = {k: voice.get(k) for k in ("npc", "x", "y", "plane", "speech")}
		for topic in g["topics"]:
			for tone in TONES:
				cell = {"id": "%s/%s/%s" % (voice_id, topic["id"], tone), "voice": voice_id, "topic": topic["id"],
					"tone": tone, "quests": voice.get("quests", {})}
				if "worn" in topic:
					cell["worn"] = topic["worn"]
				if "follower" in topic:
					cell["pet"] = topic["follower"]
				cells.append(cell)
	return cells


if __name__ == "__main__":
	main()
