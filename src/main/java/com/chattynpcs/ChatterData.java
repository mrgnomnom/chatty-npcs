package com.chattynpcs;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

/**
 * Everything the locals can say. Personas live in chatter.json, and topics are spread across
 * chatter.json and quests.json.
 */
class ChatterData
{
	private static final String[] TOPIC_FILES = {"quests.json", "characters.json"};

	static final String COMPLIMENT = "compliment";
	static final String INSULT = "insult";
	static final String REMARK = "remark";
	/**
	 * News overheard rather than said to you, so it comes up in every mood
	 */
	static final String GOSSIP = "gossip";
	static final List<String> TONES = Collections.unmodifiableList(Arrays.asList(COMPLIMENT, INSULT, REMARK, GOSSIP));

	/**
	 * How sharp a persona's tongue is, from most to least polite. Lines keyed "plain/insult" or
	 * "sharp/insult" only reach personas at least that sharp; unprefixed lines suit everyone.
	 */
	static final List<String> TEMPERAMENTS = Collections.unmodifiableList(Arrays.asList("polite", "plain", "sharp"));

	/**
	 * Hair kit ids that render as a bald head.
	 */
	List<Integer> baldHairKits = Collections.emptyList();
	/**
	 * What a slayer master tells you when you ask for tips, by task name in lower case. A topic's
	 * {tip} is one of these for the player's current task
	 */
	Map<String, List<String>> slayerTips = Collections.emptyMap();

	/**
	 * Checked in order; the first persona with a matching NPC name wins. A persona with no
	 * name patterns is the fallback for everyone else.
	 */
	List<Persona> personas = Collections.emptyList();

	List<Topic> topics = Collections.emptyList();

	/**
	 * Named areas of the map, from regions.json
	 */
	List<Region> regions = Collections.emptyList();

	/**
	 * How people talk, from speech.json: a race's own tongue first, then the local accent of
	 * wherever they're standing
	 */
	List<Speech> speech = Collections.emptyList();

	static class Persona
	{
		String id;
		List<String> npcNames = Collections.emptyList();
		/**
		 * Relative weight of each tone for this persona.
		 */
		Map<String, Double> tones = Collections.emptyMap();
		List<String> openers = Collections.emptyList();
		List<String> closers = Collections.emptyList();
		double openerChance;
		double closerChance;
		/**
		 * One of {@link #TEMPERAMENTS}
		 */
		String temperament = "plain";
		/**
		 * Only NPCs standing in one of these regions get this persona. With no npcNames, everyone
		 * there does, for a place with a voice of its own.
		 */
		List<String> regions = Collections.emptyList();
		/**
		 * The speech.json id of a race this persona is for. Anyone recognised by name as one of that
		 * race gets it, so the names live in one place.
		 */
		String speech;
		/**
		 * If set, this persona only says lines written specifically for it ("id/tone" keys),
		 * never the generic ones. Used for people who don't talk like everyone else, e.g. kids.
		 */
		boolean ownLinesOnly;
		/**
		 * With ownLinesOnly, still pass on the shared gossip. Guards relay the city's news.
		 */
		boolean sharesGossip;
		/**
		 * Said once per visit, before anything else: "Welcome, {you}, to {place} bank!". {you} is
		 * what the locals call you, {place} the most specific named place they're standing in.
		 */
		List<String> greetings = Collections.emptyList();
		/**
		 * Only talks quest news: half the time a local quest (unfinished ones first), half the
		 * time news from far away. Anything else only if there's no news to tell.
		 */
		boolean newsOnly;
		/**
		 * Knows every quest that starts in their own region, not just within earshot: the town
		 * watch hears the city's news
		 */
		boolean cityNews;
		/**
		 * Openers and closers only where nobody has a voice of their own: "Blimey." suits Lumbridge,
		 * not Darkmeyer
		 */
		boolean plainOnly;
		/**
		 * Only while the player hasn't finished this quest: the Moon Clan are cold to you until
		 * you've made their peace with the Fremennik. List the warmer persona after this one.
		 */
		String untilQuest;
		/**
		 * Their own oaths in place of "Blimey" and "Gadzooks" on shared lines: a pirate's "Arr"
		 */
		Words exclaim;
		/**
		 * Says nothing at all. Inside a raid nobody's there for a natter, and Osmumten has been
		 * sealed in a tomb for decades, so he'd know nothing of what passes for news.
		 */
		boolean silent;
		/**
		 * Talks without a Talk-to option, and despite a combat level, as long as they can't be
		 * attacked: the alliance's people in their own camp at the Fractured Archive, where a general
		 * has a level and a druid on watch has nothing to click.
		 */
		boolean atEase;
		/**
		 * The share of what they say that's their own: their character's lines, and lines written for
		 * this persona. The rest is what anyone says. The alliance talks of the Archive four times in
		 * five, and of your hat or the news the fifth. Zero leaves it to the topics' weights.
		 */
		double ownShare;

		transient List<Pattern> compiledNames;
		transient Quest compiledUntil;
	}

	static class Topic
	{
		String id;
		/**
		 * How notable this is, 1-100. Notable things are much more likely to get mentioned.
		 */
		int priority = 10;
		/**
		 * The tone people naturally take about this. It gets a boost when picking a tone.
		 */
		String lean;
		/**
		 * Whether persona openers and closers may go round its lines. Off for grave news, where
		 * "Next, please." after a death would jar.
		 */
		boolean wrap = true;
		/**
		 * What the topic is about. Set exactly one of these.
		 * fact is computed by {@link CharacterReader}, e.g. "bald".
		 */
		String fact;
		ItemRule item;
		PetRule pet;
		QuestRule quest;
		/**
		 * One named person's own lines: who they are and what they've been through. Said before
		 * anything else they could say, until you've heard them all.
		 */
		CharacterRule character;
		/**
		 * The speech.json id of the people who say this, for sayings of their own
		 */
		String speech;
		/**
		 * The lines are already in the speaker's own words, so they aren't translated
		 */
		boolean inVoice;
		/**
		 * Only NPCs standing in one of these regions say it
		 */
		List<String> regions = Collections.emptyList();
		/**
		 * Only once the player has finished this quest
		 */
		String afterQuest;
		transient Quest compiledAfter;
		/**
		 * Keyed by tone ("insult"), persona and tone ("kid/insult"), or "starter/insult" for
		 * what the NPC who gives out a quest, or the person a character topic is about, says.
		 */
		Map<String, List<String>> lines = Collections.emptyMap();
	}

	static class ItemRule
	{
		/**
		 * EquipmentInventorySlot names to check. Empty means any slot.
		 */
		List<String> slots = Collections.emptyList();
		/**
		 * Case-insensitive regex searched for in the item name.
		 */
		String pattern;
		/**
		 * How many worn items have to match, for "head to toe in bronze" style topics.
		 */
		int minCount = 1;
		/**
		 * What to call the item in lines. Defaults to the name of the first matching item.
		 */
		String label;

		transient Pattern compiled;
	}

	static class CharacterRule
	{
		/**
		 * Their name exactly as the game shows it. There are eight Cooks, so put a topic's regions
		 * to say which.
		 */
		String name;
		/**
		 * Only while the player's quests are in these states: the Cook frets about his cake until
		 * you've helped, then he doesn't
		 */
		Map<String, List<String>> quests = Collections.emptyMap();

		transient Map<Quest, Set<QuestState>> compiledQuests;
	}

	static class PetRule
	{
		/**
		 * Case-insensitive regex searched for in the name of the pet following the player
		 */
		String pattern;

		transient Pattern compiled;
	}

	static class QuestRule
	{
		/**
		 * A Quest enum name, e.g. COOKS_ASSISTANT.
		 */
		String name;
		/**
		 * QuestState names the player has to be in, e.g. ["NOT_STARTED", "IN_PROGRESS"].
		 */
		List<String> states = Collections.emptyList();
		/**
		 * Only NPCs this close to the quest start bring it up. For quests that don't start in a
		 * city or region.
		 */
		Near near;
		/**
		 * The regions.json id of where the quest happens. Only NPCs inside it bring the quest up,
		 * unless anywhere is set.
		 */
		String region;
		/**
		 * Everyone has heard about this one (grandmasters), though locals in its region bring it up
		 * more often.
		 */
		boolean anywhere;
		/**
		 * Later chapters of the same storyline. Once any of them is finished this is old news, e.g.
		 * nobody talks about Ranis Drakan once Lord Drakan is dead.
		 */
		List<String> supersededBy = Collections.emptyList();
		/**
		 * Quests that have to be finished first, so trouble isn't gossiped about before it's started
		 */
		List<String> requires = Collections.emptyList();
		/**
		 * Regex for the NPC who gives out the quest. They say the "starter/" lines instead.
		 */
		String starter;

		transient Quest compiled;
		transient List<Quest> compiledSupersededBy;
		transient List<Quest> compiledRequires;
		transient Set<QuestState> compiledStates;
		transient Pattern compiledStarter;
	}

	static class Region
	{
		String id;
		String name;
		/**
		 * Guards on watch here may speak (see TONE.md)
		 */
		boolean city;
		/**
		 * The boxes are a best guess rather than taken from the map, so may need adjusting
		 */
		boolean estimated;
		/**
		 * Everyone passes through here (the Grand Exchange), so NPCs here have heard every bit of quest
		 * news, wherever it happened
		 */
		boolean hearsEverything;
		/**
		 * What locals call the place, in their own tongue where the game has a name for it, for
		 * greetings: "Welcome to Ta Quir Priw bank!"
		 */
		String place;
		/**
		 * Only there to name a spot, like one bank in a city: never anyone's home for the news
		 */
		boolean nameOnly;
		/**
		 * Not the sort of town the heartland/ lines are written for, though nobody here has a
		 * voice of their own: Karamja, islands, the wilds
		 */
		boolean notHeartland;
		/**
		 * A cut-off place: NPCs here only hear news from these regions, and nothing from further
		 * afield, not even the grandest deeds
		 */
		List<String> onlyHears = Collections.emptyList();
		/**
		 * Rectangles of world tiles: [x1, y1, x2, y2], corners inclusive
		 */
		List<int[]> boxes = Collections.emptyList();

		long area()
		{
			long area = 0;
			for (int[] box : boxes)
			{
				area += (long) (Math.abs(box[2] - box[0]) + 1) * (Math.abs(box[3] - box[1]) + 1);
			}
			return area;
		}

		boolean contains(int x, int y)
		{
			for (int[] box : boxes)
			{
				if (x >= Math.min(box[0], box[2]) && x <= Math.max(box[0], box[2])
					&& y >= Math.min(box[1], box[3]) && y <= Math.max(box[1], box[3]))
				{
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * How one people or place talks. Shared lines pass through it, so a generic remark comes out
	 * in the speaker's own words.
	 */
	static class Speech
	{
		String id;
		/**
		 * Anyone with a matching name talks like this wherever they are, e.g. gnomes. Checked before
		 * any region.
		 */
		List<String> npcNames = Collections.emptyList();
		/**
		 * Places with a voice of their own, where the names above don't count: Jatizso has a
		 * Freygerd too, and she sounds like Jatizso, not Rellekka
		 */
		List<String> notIn = Collections.emptyList();
		/**
		 * People who stand in these regions without being of them, so don't talk like them:
		 * Nieve in the Gnome Stronghold is human
		 */
		List<String> outsiders = Collections.emptyList();
		/**
		 * Anyone standing here not recognised by name talks like this. The first match wins, so
		 * specific places go before broad ones.
		 */
		List<String> regions = Collections.emptyList();
		Address address;
		/**
		 * Replaces exclamations like "Blimey" and "Ye gods". An empty word drops the exclamation.
		 */
		Words exclaim;
		/**
		 * Replaces praise like "Lovely" and "Splendid"
		 */
		Words praise;
		/**
		 * Said before a line addressed to you
		 */
		Words greet;
		/**
		 * Said after a line addressed to you
		 */
		Words farewell;
		/**
		 * Chance of spelling out each contraction: "don't" becomes "do not"
		 */
		double formal;
		/**
		 * Anything else, in the speaker's words
		 */
		List<Swap> swaps = Collections.emptyList();
		/**
		 * Everything they say comes out as this unless you can understand them
		 */
		Babble babble;
		/**
		 * Lines of their own, in their own words, keyed like topic lines
		 */
		Map<String, List<String>> sayings = Collections.emptyMap();
		/**
		 * Sayings that only make sense in one place, keyed by region: a ghost talks about the
		 * Ectofuntus in Port Phasmatys, not in Lumbridge
		 */
		Map<String, Map<String, List<String>>> sayingsIn = Collections.emptyMap();
		int sayingsPriority = 40;
		/**
		 * Their sayings only once the player has finished this quest: friendlier talk for friends
		 */
		String sayingsAfter;
		/**
		 * Quotes from in-game dialogue, or the Wiki, showing they really talk like this
		 */
		List<String> evidence = Collections.emptyList();

		transient List<Pattern> compiledNames;
		transient List<Pattern> compiledOutsiders;
	}

	/**
	 * What someone calls you
	 */
	static class Address
	{
		String word;
		/**
		 * The ways of addressing you this replaces: "Hello, traveller." becomes "Hello, human."
		 */
		List<String> replaces = Arrays.asList("traveller", "adventurer", "friend", "stranger");
		double swap = 0.5;
		/**
		 * Also replace "an adventurer" or "the traveller" in the middle of a sentence, for words
		 * that work as nouns ("a human", not "an effendi")
		 */
		boolean asNoun;
		/**
		 * Chance of adding it to a line that doesn't address you by name: "Nice cape, human."
		 */
		double append = 0.25;
		/**
		 * Once this quest is done, they call you thenWord instead: the Fremennik's "outerlander"
		 * becomes "{kin}" (brother or sister) after their trials
		 */
		String untilQuest;
		String thenWord;
		/**
		 * What they call you to your face once you've earned the word, built from what you're known
		 * by, e.g. "{tzhaar_name}". Talk about you keeps the plain word: "a JalYt", never a full name.
		 * Until it's known, the plain word does
		 */
		String vocative;

		transient Pattern compiledVocative;
		transient Quest compiledUntil;
	}

	static class Words
	{
		List<String> words = Collections.emptyList();
		double chance = 1;
		/**
		 * Only on lines of these tones, if set: "Tetamo" is frustration, not for compliments
		 */
		List<String> tones = Collections.emptyList();
	}

	static class Swap
	{
		/**
		 * Case-insensitive regex. Each match is replaced with the given chance.
		 */
		String pattern;
		/**
		 * May use $1 and so on. Keeps the capital if the matched text started with one.
		 */
		String replace;
		double chance = 1;

		transient Pattern compiled;
	}

	static class Babble
	{
		List<String> phrases = Collections.emptyList();
		/**
		 * Phrases are strung together until the line has at least this many spaces, as the game does
		 * for monkeys
		 */
		int spaces = 1;
		/**
		 * What lets you understand them. With neither, nobody can.
		 */
		String understoodWearing;
		/**
		 * Or understood while carrying this, for things you hold rather than wear
		 */
		String understoodCarrying;
		List<String> understoodAfter = Collections.emptyList();

		transient Pattern compiledWearing;
		transient Pattern compiledCarrying;
		transient List<Quest> compiledAfter;
	}

	static class Near
	{
		int x;
		int y;
		int radius = 25;
	}

	static ChatterData load(Gson gson) throws IOException
	{
		ChatterData data = read(gson, "chatter.json");
		List<Topic> topics = new ArrayList<>(data.topics);
		for (String file : TOPIC_FILES)
		{
			topics.addAll(read(gson, file).topics);
		}
		data.topics = topics;
		data.regions = read(gson, "regions.json").regions;
		data.speech = read(gson, "speech.json").speech;
		for (Speech speech : data.speech)
		{
			if (!speech.sayings.isEmpty())
			{
				topics.add(sayings(speech, speech.id + "_sayings", speech.sayings, Collections.emptyList()));
			}
			for (Map.Entry<String, Map<String, List<String>>> place : speech.sayingsIn.entrySet())
			{
				topics.add(sayings(speech, speech.id + "_sayings_" + place.getKey(), place.getValue(), Collections.singletonList(place.getKey())));
			}
		}
		return data;
	}

	private static Topic sayings(Speech speech, String id, Map<String, List<String>> lines, List<String> regions)
	{
		Topic sayings = new Topic();
		sayings.id = id;
		sayings.priority = speech.sayingsPriority;
		sayings.speech = speech.id;
		sayings.lines = lines;
		sayings.regions = regions;
		sayings.inVoice = true;
		sayings.wrap = false;
		sayings.afterQuest = speech.sayingsAfter;
		return sayings;
	}

	private static ChatterData read(Gson gson, String file) throws IOException
	{
		try (InputStream in = ChatterData.class.getResourceAsStream(file))
		{
			if (in == null)
			{
				throw new IOException(file + " is missing");
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return gson.fromJson(reader, ChatterData.class);
			}
		}
	}
}
