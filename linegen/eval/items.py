"""
Everything there is to rate, from every source, in one shape. Standard library only, so the rating
site runs without the eval environment.

An item is one line plus who says it and when: the context a person or a judge needs to rate it.
What the pipeline thought of it (fact check, judge, shipped or not) is kept apart, so rating can
be blind.
"""
import glob
import hashlib
import json
import os
import re

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
LINEGEN = os.path.join(ROOT, "linegen")
EVAL = os.path.join(LINEGEN, "eval")
BANK = os.path.join(ROOT, "src", "main", "resources", "com", "chattynpcs")
# Overridable so a test run never touches your real labels
LABELS = os.environ.get("CHATTY_EVAL_LABELS", os.path.join(EVAL, "labels.jsonl"))
CUSTOM = os.environ.get("CHATTY_EVAL_CUSTOM", os.path.join(EVAL, "custom.jsonl"))
RESULTS = os.environ.get("CHATTY_EVAL_RESULTS", os.path.join(EVAL, "results"))
SCORES = os.path.join(RESULTS, "scores.jsonl")

STATES = {"NOT_STARTED": "not started", "IN_PROGRESS": "in progress", "FINISHED": "finished"}

# The faults a curator cuts lines for: the judge's questions (judge.md) and the fact check's
# (verify.md), in plain words
TAGS = [
	("hook", "nothing to hang on"),
	("generic", "generic, anyone could say it"),
	("voice", "not their voice"),
	("explains", "explains the joke or feeling"),
	("mood", "too mean, or gushing"),
	("stale", "stock joke, or stale"),
	("knows", "knows what they couldn't"),
	("looks", "claims my looks, gear or past"),
	("lore", "wrong or invented lore"),
	("spoiler", "spoiler"),
	("spoken", "written, not spoken"),
	("modern", "modern or real-world"),
	("footing", "unclear who it's said to"),
	("long", "too long or two jobs"),
]


def read_jsonl(path):
	if not os.path.exists(path):
		return []
	with open(path, encoding="utf-8") as f:
		return [json.loads(line) for line in f if line.strip()]


def read_json(path):
	with open(path, encoding="utf-8") as f:
		return json.load(f)


def line_hash(text):
	return hashlib.sha1(text.encode("utf-8")).hexdigest()[:10]


def _quest_titles():
	titles = {}
	path = os.path.join(ROOT, "src", "test", "resources", "com", "chattynpcs", "quest_titles.txt")
	if os.path.exists(path):
		with open(path, encoding="utf-8") as f:
			for line in f:
				line = line.strip()
				if line and not line.startswith("#"):
					titles[re.sub(r"[^A-Z0-9]", "", line.upper())] = line
	return titles


_TITLES = _quest_titles()


def quest_title(enum_name):
	return _TITLES.get(re.sub(r"[^A-Z0-9]", "", enum_name), enum_name.replace("_", " ").title())


def describe_quests(quests):
	"""{"THE_LOST_TRIBE": "NOT_STARTED"} as "The Lost Tribe: not started" """
	parts = []
	for quest, state in sorted(quests.items()):
		states = state if isinstance(state, list) else [state]
		parts.append("%s: %s" % (quest_title(quest), " or ".join(STATES.get(s, s.lower()) for s in states)))
	return "; ".join(parts)


def _facts():
	facts = {}
	for path in glob.glob(os.path.join(LINEGEN, "lore", "*.json")):
		for fact in read_json(path)["facts"]:
			facts[fact["id"]] = fact["says"]
	return facts


def _card_summary(card):
	"""What a rater needs to know about the speaker, from a character or voice card"""
	keep = ("who", "notices", "values", "address", "grammar", "rhythm", "neverSay", "taboos", "avoid")
	out = []
	for key in keep:
		value = card.get(key)
		if not value:
			continue
		if isinstance(value, list):
			value = "; ".join(v if isinstance(v, str) else json.dumps(v, ensure_ascii=False) for v in value)
		elif not isinstance(value, str):
			value = json.dumps(value, ensure_ascii=False)
		out.append("%s: %s" % (key, value))
	return "\n".join(out)


class Catalogue:
	"""Every item, built once; the pipeline's verdicts on each kept apart in `pipeline`"""

	def __init__(self):
		self.items = {}
		self.pipeline = {}
		facts = _facts()
		self._characters(facts)
		self._pilot(facts)
		self._bank()
		self.load_custom()

	def _add(self, item, verdicts=None):
		self.items[item["id"]] = item
		if verdicts:
			self.pipeline[item["id"]] = verdicts

	def _batch(self, folder, facts, source, card_for, situation):
		cells = {c["id"]: c for c in read_json(os.path.join(folder, "cells.json"))["cells"]}
		verified = {v["id"]: v for v in read_jsonl(os.path.join(folder, "verified.jsonl"))}
		judged = {j["id"]: j for j in read_jsonl(os.path.join(folder, "judged.jsonl"))}
		accepted = {a["id"] for a in read_jsonl(os.path.join(folder, "accepted.jsonl"))}
		cards = {}
		for row in read_jsonl(os.path.join(folder, "thinned.jsonl")):
			cell = cells.get(row["cell"])
			if cell is None:
				continue
			voice = cell["voice"]
			if voice not in cards:
				cards[voice] = card_for(voice)
			card = cards[voice]
			cited = [facts[f] for f in row.get("facts", []) if f in facts]
			state, where = situation(cell)
			item = {
				"id": row["id"],
				"source": source,
				"speaker": card.get("npc") or voice,
				"voice": voice,
				"state": state,
				"stateKind": _state_kind(row["cell"]) if source == "characters" else ("gear" if cell.get("worn") else "news"),
				"tone": cell["tone"],
				"cell": row["cell"],
				"line": row["line"],
				"situation": where,
				"facts": cited,
				"card": _card_summary(card),
				"cardWho": card.get("who", ""),
			}
			v, j = verified.get(row["id"]), judged.get(row["id"])
			self._add(item, {
				"shipped": row["id"] in accepted,
				"grounded": v["grounded"] if v else None,
				"unsupported": [c["claim"] for c in v["claims"] if c["support"] == "none"] if v else [],
				"factNote": v.get("note", "") if v else "",
				"judgePass": j["pass"] if j else None,
				"judgeRank": j.get("rank") if j else None,
				"judgeNo": {q: a for q, a in j["answers"].items() if str(a).startswith("NO")} if j else {},
			})

	def _characters(self, facts):
		folder = os.path.join(LINEGEN, "characters")
		if not os.path.exists(os.path.join(folder, "thinned.jsonl")):
			return

		def card_for(voice):
			return read_json(os.path.join(folder, "cards", voice + ".json"))

		def situation(cell):
			state = cell["id"].split("/")[1]
			quests = describe_quests(cell.get("quests", {}))
			if not quests:
				return "any time", "Any point in the player's story."
			return state.replace("_", " "), "The player's progress: %s." % quests

		self._batch(folder, facts, "characters", card_for, situation)

	def _pilot(self, facts):
		folder = os.path.join(LINEGEN, "pilot")
		if not os.path.exists(os.path.join(folder, "thinned.jsonl")):
			return

		def card_for(voice):
			card = read_json(os.path.join(LINEGEN, "voices", voice + ".json"))
			card.setdefault("npc", {"gnome": "A gnome of the Stronghold", "varrock": "A Varrock local"}.get(voice, voice))
			return card

		def situation(cell):
			if cell.get("worn"):
				worn = ", ".join(cell["worn"].values())
				return cell["topic"].replace("_", " "), "The player walks past wearing: %s." % worn
			return cell["topic"].replace("_", " "), "Quest news. The player's progress: %s." % describe_quests(cell.get("quests", {}))

		self._batch(folder, facts, "pilot", card_for, situation)

	def _bank(self):
		"""What the plugin says today: every shared and quest line, unattributed to any one NPC"""
		for name in ("chatter.json", "quests.json", "characters.json"):
			path = os.path.join(BANK, name)
			if not os.path.exists(path):
				continue
			for topic in read_json(path).get("topics", []):
				about = topic["id"].replace("_", " ")
				if topic.get("quest"):
					about = "%s (%s)" % (quest_title(topic["quest"]["name"]), " or ".join(STATES.get(s, s) for s in topic["quest"].get("states", [])))
				elif topic.get("character"):
					about = "%s's own line, %s" % (topic["character"]["name"], describe_quests(topic["character"].get("quests", {})) or "any time")
				for key, lines in topic.get("lines", {}).items():
					parts = key.split("/")
					tone, who = parts[-1], parts[0] if len(parts) > 1 else "anyone"
					if who == "starter":
						who = topic["character"]["name"] if topic.get("character") else "the quest giver"
					for line in lines:
						item_id = "bank:%s:%s" % (topic["id"], line_hash(key + line))
						self._add({
							"id": item_id,
							"source": "bank",
							"speaker": who,
							"voice": who,
							"state": about,
							"stateKind": "bank",
							"tone": tone,
							"cell": "%s/%s" % (topic["id"], key),
							"line": line,
							"situation": "Shipped in %s, topic %s. Said by: %s." % (name, about, who),
							"facts": [],
							"card": "",
							"cardWho": "",
						}, {"shipped": True})

	def load_custom(self):
		for row in read_jsonl(CUSTOM):
			self._add(row)

	def add_custom(self, speaker, situation, tone, lines):
		added = []
		with open(CUSTOM, "a", encoding="utf-8", newline="\n") as f:
			for line in lines:
				line = line.strip()
				if not line:
					continue
				item = {
					"id": "custom:%s" % line_hash(speaker + "|" + situation + "|" + tone + "|" + line),
					"source": "custom",
					"speaker": speaker or "someone",
					"voice": speaker or "someone",
					"state": "custom",
					"stateKind": "custom",
					"tone": tone or "remark",
					"cell": "custom/%s/%s" % (speaker or "someone", tone or "remark"),
					"line": line,
					"situation": situation,
					"facts": [],
					"card": "",
					"cardWho": "",
				}
				if item["id"] not in self.items:
					f.write(json.dumps(item, ensure_ascii=False) + "\n")
					self.items[item["id"]] = item
				added.append(item["id"])
		return added


def _state_kind(cell_id):
	state = cell_id.split("/")[1] if cell_id.count("/") >= 2 else ""
	for kind in ("always", "before", "after"):
		if state.startswith(kind):
			return kind
	return "during" if state else "other"


def judge_input(item):
	"""The context a judge model gets with each line"""
	parts = ["Speaker: %s" % item["speaker"]]
	if item["card"]:
		parts.append("About the speaker:\n%s" % item["card"])
	parts.append("Situation: %s" % item["situation"])
	parts.append("Tone wanted: %s (said in passing, as the player walks by; at most 70 characters)" % item["tone"])
	if item["facts"]:
		parts.append("Facts the speaker knows:\n- " + "\n- ".join(item["facts"]))
	return "\n\n".join(parts)


def latest_labels():
	"""The last label given to each item"""
	labels = {}
	for row in read_jsonl(LABELS):
		labels[row["id"]] = row
	return labels


def latest_scores():
	"""{id: {metric: row}} for scores of the line as it now stands"""
	scores = {}
	for row in read_jsonl(SCORES):
		scores.setdefault(row["id"], {})[row["metric"]] = row
	return scores
