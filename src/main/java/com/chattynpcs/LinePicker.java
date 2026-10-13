package com.chattynpcs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

/**
 * Picks what an NPC says about the player: the most notable topic wins more often, the tone comes
 * from the persona and mood, and recently used lines and topics are avoided.
 */
class LinePicker
{
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");
	private static final int RECENT_LINES = 40;
	/**
	 * Never repeated within this many lines, even when there's nothing fresh left to say: better to
	 * keep quiet
	 */
	private static final int NO_REPEAT_LINES = 5;
	/**
	 * Don't wrap lines that are already long, overhead text gets hard to read
	 */
	private static final int MAX_WRAPPED_LENGTH = 70;
	private static final double LEAN_BOOST = 2.5;
	/**
	 * How much more a quest giver wants to talk about their own quest than anything else
	 */
	private static final double STARTER_BOOST = 50;
	/**
	 * How much more locals talk about the big story that happened on their doorstep
	 */
	private static final double REGION_BOOST = 3;
	private static final String STARTER = "starter/";
	/**
	 * Lines for an ordinary town above ground: sun, hedges, chickens, queues
	 */
	static final String HEARTLAND = "heartland/";
	/**
	 * A topic with fewer lines than this left unsaid comes up less, in proportion
	 */
	private static final int FRESH_LINES = 3;
	/**
	 * The most of what anyone says that can be quest news, so the locals still notice you
	 */
	private static final double DEFAULT_NEWS_SHARE = 1.0 / 3;
	/**
	 * How many recent comments the news budget is kept over
	 */
	private static final int NEWS_MEMORY = 6;
	/**
	 * Away from a place this long and the next visit gets greeted again
	 */
	private static final long VISIT_GAP_MILLIS = 15 * 60 * 1000;
	private static final double LOCAL_NEWS_SHARE = 0.5;
	/**
	 * Underground areas sit far north of the surface map
	 */
	private static final int UNDERGROUND_Y = 6400;
	/**
	 * Words in NPC names that don't make a line about them: "Elder" Krill, "Gnome" child
	 */
	private static final Set<String> TITLE_WORDS = new HashSet<>(Arrays.asList(
		"Elder", "Captain", "Brother", "Father", "Sister", "Master", "Chieftain", "Gnome", "Monkey", "Ghost", "Villager",
		"Banker", "Guard", "Shop", "Keeper", "Child", "Woman", "Lady", "Lord", "King", "Queen", "Prince", "Princess",
		"Dame", "Wizard", "Farmer", "Merchant", "Trader", "Clerk", "Priest", "Monk", "Dwarf", "Barbarian", "Pirate",
		"Sailor", "Fisherman", "Miner", "Bartender", "Street", "Urchin", "Seer", "Huntsman", "Navigator", "Warrior",
		"Reveller", "Barkeep", "Professor", "Doctor", "General", "Commander", "Sergeant", "Squire", "Sir"));
	/**
	 * Openers that are exclamations, never put in front of a line that already exclaims
	 */
	private static final Pattern OATH = Pattern.compile(
		"\\b(By \\w+|Blimey|Cor|Gadzooks|Ye gods|Good heavens|Goodness|Great Guthix|Shiver me timbers|Avast|Hah|Arr|Gah|Tetamo|Megal|Grrr|Hur|I say|Oh, dear)\\b");
	/**
	 * A line that already opens with an exclamation of its own: "Oi! ...", "Grrr! ...", "Blimey. ..."
	 */
	private static final Pattern INTERJECTION = Pattern.compile("^[A-Z][\\w']*(?:,? [\\w']+)?[!.] ");
	private static final Pattern WRAPPER_TONES = Pattern.compile("^([a-z|]+):\\s*(.*)$");
	/**
	 * How much more a news-teller would rather talk about local trouble that's still unsolved
	 */
	private static final double UNFINISHED_BOOST = 4;
	private static final List<String> DEFAULT_ADDRESS = Arrays.asList("adventurer", "traveller", "friend");

	@Value
	static class Comment
	{
		String topicId;
		String persona;
		String tone;
		/**
		 * The lines key the line came from, e.g. "kid/insult"
		 */
		String key;
		String text;
		/**
		 * Said in the speaker's own way: a saying of their own, a line their speech changed, or babble
		 */
		boolean local;
		/**
		 * The line as written, before any accent, opener, closer or babble, for reviewing
		 */
		String written;
	}

	/**
	 * A line template and the lines key it's filed under
	 */
	@Value
	private static class Line
	{
		String key;
		String template;
	}

	@Value
	private static class Picked
	{
		String tone;
		String key;
		String text;
	}

	private final ChatterData data;
	private final Random random;
	private final Set<Quest> quests = EnumSet.noneOf(Quest.class);
	private final Map<String, ChatterData.Region> regions = new HashMap<>();
	private final Map<String, ChatterData.Speech> speechById = new HashMap<>();
	private final Deque<String> recentLines = new ArrayDeque<>();
	private final Map<String, Long> topicLastUsed = new HashMap<>();
	/**
	 * Whether each of the last few comments was quest news, oldest first
	 */
	private final Deque<Boolean> recentNews = new ArrayDeque<>();
	/**
	 * When someone with greetings last spoke at each place, to greet once per visit
	 */
	private final Map<String, Long> visits = new HashMap<>();

	/**
	 * The most of what's said that can be quest news, from the settings
	 */
	private double newsShare = DEFAULT_NEWS_SHARE;
	/**
	 * How strongly people sound like where they're from, 1 as written
	 */
	private double flavour = 1;
	private boolean greetings = true;

	/**
	 * Who's speaking right now, so they never gossip about themselves
	 */
	private List<String> speakerNameWords = Collections.emptyList();
	/**
	 * The speaker has no voice of their own and stands above ground, in the sort of town the
	 * heartland/ lines (sun, hedges, chickens, queues) are written for
	 */
	private boolean heartland;
	/**
	 * Who's listening right now, for what they've earned the right to be called
	 */
	private CharacterReader.Snapshot hearer;

	/**
	 * Applies the player's settings
	 */
	void tune(double newsShare, double flavour, boolean greetings)
	{
		this.newsShare = newsShare;
		this.flavour = flavour;
		this.greetings = greetings;
	}

	LinePicker(ChatterData data, Random random)
	{
		this.data = data;
		this.random = random;

		for (ChatterData.Region region : data.regions)
		{
			regions.put(region.id, region);
		}
		for (ChatterData.Region region : data.regions)
		{
			for (String heard : region.onlyHears)
			{
				requireRegion(heard, region.id);
			}
		}
		for (ChatterData.Persona persona : data.personas)
		{
			for (String region : persona.regions)
			{
				requireRegion(region, persona.id);
			}
		}
		for (ChatterData.Speech speech : data.speech)
		{
			speechById.put(speech.id, speech);
			for (String region : speech.regions)
			{
				requireRegion(region, speech.id);
			}
			for (String region : speech.notIn)
			{
				requireRegion(region, speech.id);
			}
			speech.compiledNames = compileAll(speech.npcNames);
			speech.compiledOutsiders = compileAll(speech.outsiders);
			for (ChatterData.Swap swap : speech.swaps)
			{
				swap.compiled = Pattern.compile(swap.pattern, Pattern.CASE_INSENSITIVE);
			}
			if (speech.address != null && speech.address.untilQuest != null)
			{
				speech.address.compiledUntil = Quest.valueOf(speech.address.untilQuest);
				quests.add(speech.address.compiledUntil);
			}
			if (speech.address != null)
			{
				speech.address.compiledVocative = Pattern.compile("(?:(?<=, )|^)(?:my )?(?:"
					+ String.join("|", speech.address.replaces) + ")\\b(?=[,.!?])", Pattern.CASE_INSENSITIVE);
			}
			ChatterData.Babble babble = speech.babble;
			if (babble != null)
			{
				babble.compiledWearing = babble.understoodWearing == null ? null : Pattern.compile(babble.understoodWearing, Pattern.CASE_INSENSITIVE);
				babble.compiledCarrying = babble.understoodCarrying == null ? null : Pattern.compile(babble.understoodCarrying, Pattern.CASE_INSENSITIVE);
				babble.compiledAfter = new ArrayList<>();
				for (String name : babble.understoodAfter)
				{
					babble.compiledAfter.add(Quest.valueOf(name));
				}
				quests.addAll(babble.compiledAfter);
			}
		}

		for (ChatterData.Persona persona : data.personas)
		{
			persona.compiledNames = compileAll(persona.npcNames);
			if (persona.speech != null && !speechById.containsKey(persona.speech))
			{
				throw new IllegalArgumentException(persona.id + " has an unknown speech " + persona.speech);
			}
		}
		for (ChatterData.Topic topic : data.topics)
		{
			if (topic.item != null)
			{
				if (topic.item.minCount < 1)
				{
					throw new IllegalArgumentException(topic.id + " needs a minCount of at least 1");
				}
				topic.item.compiled = Pattern.compile(topic.item.pattern, Pattern.CASE_INSENSITIVE);
			}
			if (topic.pet != null)
			{
				topic.pet.compiled = Pattern.compile(topic.pet.pattern, Pattern.CASE_INSENSITIVE);
			}
			if (topic.speech != null && !speechById.containsKey(topic.speech))
			{
				throw new IllegalArgumentException(topic.id + " has an unknown speech " + topic.speech);
			}
			for (String region : topic.regions)
			{
				requireRegion(region, topic.id);
			}
			if (topic.quest != null)
			{
				ChatterData.QuestRule rule = topic.quest;
				rule.compiled = Quest.valueOf(rule.name);
				rule.compiledStates = EnumSet.noneOf(QuestState.class);
				for (String state : rule.states)
				{
					rule.compiledStates.add(QuestState.valueOf(state));
				}
				rule.compiledSupersededBy = new ArrayList<>();
				for (String name : rule.supersededBy)
				{
					rule.compiledSupersededBy.add(Quest.valueOf(name));
				}
				rule.compiledRequires = new ArrayList<>();
				for (String name : rule.requires)
				{
					rule.compiledRequires.add(Quest.valueOf(name));
				}
				quests.addAll(rule.compiledSupersededBy);
				quests.addAll(rule.compiledRequires);
				if (rule.region != null && !regions.containsKey(rule.region))
				{
					throw new IllegalArgumentException(topic.id + " has an unknown region " + rule.region);
				}
				if (rule.starter != null)
				{
					rule.compiledStarter = Pattern.compile(rule.starter, Pattern.CASE_INSENSITIVE);
				}
				quests.add(rule.compiled);
			}
		}
	}

	/**
	 * @return every quest a topic needs the state of
	 */
	Set<Quest> quests()
	{
		return Collections.unmodifiableSet(quests);
	}

	private static List<Pattern> compileAll(List<String> patterns)
	{
		List<Pattern> compiled = new ArrayList<>();
		for (String pattern : patterns)
		{
			compiled.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
		}
		return compiled;
	}

	private static boolean anyFind(List<Pattern> patterns, String text)
	{
		return patterns.stream().anyMatch(p -> p.matcher(text).find());
	}

	private void requireRegion(String region, String usedBy)
	{
		if (!regions.containsKey(region))
		{
			throw new IllegalArgumentException(usedBy + " has an unknown region " + region);
		}
	}

	ChatterData.Persona personaFor(String npcName)
	{
		return personaFor(npcName, null);
	}

	/**
	 * @param location where the NPC is standing, for personas tied to a place
	 */
	ChatterData.Persona personaFor(String npcName, WorldPoint location)
	{
		ChatterData.Persona fallback = null;
		for (ChatterData.Persona persona : data.personas)
		{
			if (!persona.regions.isEmpty() && persona.regions.stream().noneMatch(r -> inRegion(r, location)))
			{
				continue;
			}
			if (persona.speech != null)
			{
				ChatterData.Speech race = raceOf(npcName, location);
				if (race != null && race.id.equals(persona.speech))
				{
					return persona;
				}
				continue;
			}
			if (persona.compiledNames.isEmpty())
			{
				if (!persona.regions.isEmpty())
				{
					return persona;
				}
				if (fallback == null)
				{
					fallback = persona;
				}
				continue;
			}
			if (anyFind(persona.compiledNames, npcName))
			{
				return persona;
			}
		}
		return fallback;
	}

	/**
	 * The race someone is, by name, if they have a tongue of their own
	 */
	private ChatterData.Speech raceOf(String npcName, WorldPoint location)
	{
		for (ChatterData.Speech speech : data.speech)
		{
			if (anyFind(speech.compiledNames, npcName) && speech.notIn.stream().noneMatch(r -> inRegion(r, location)))
			{
				return speech;
			}
		}
		return null;
	}

	/**
	 * How someone talks: their race's own way if they have one, otherwise the accent of wherever
	 * they're standing, otherwise plain
	 */
	ChatterData.Speech speechFor(String npcName, WorldPoint location)
	{
		ChatterData.Speech race = raceOf(npcName, location);
		if (race != null)
		{
			return race;
		}
		for (ChatterData.Speech speech : data.speech)
		{
			if (speech.regions.stream().anyMatch(r -> inRegion(r, location)) && !anyFind(speech.compiledOutsiders, npcName))
			{
				return speech;
			}
		}
		return null;
	}

	/**
	 * @param npcLocation where the NPC is standing, for quests only mentioned near their start
	 * @return null if this NPC has nothing to say about the player right now
	 */
	Comment pick(CharacterReader.Snapshot snapshot, String npcName, WorldPoint npcLocation, ChattyNpcsConfig.Mood mood, long topicCooldownMillis, long now)
	{
		ChatterData.Persona persona = personaFor(npcName, npcLocation);
		if (persona == null)
		{
			return null;
		}

		ChatterData.Speech speech = speechFor(npcName, npcLocation);
		boolean race = speech != null && speech == raceOf(npcName, npcLocation);
		speakerNameWords = nameWords(npcName);
		hearer = snapshot;
		heartland = speech == null && npcLocation != null && npcLocation.getY() < UNDERGROUND_Y
			&& !"sharp".equals(persona.temperament)
			&& data.regions.stream().noneMatch(r -> r.notHeartland && r.contains(npcLocation.getX(), npcLocation.getY()));

		if (greetings && !persona.greetings.isEmpty())
		{
			String visit = visitKey(npcLocation);
			Long last = visits.put(visit, now);
			if (last == null || now - last > VISIT_GAP_MILLIS)
			{
				String greeting = greeting(persona, speech, npcLocation);
				if (speech != null && speech.babble != null && !understands(snapshot, speech.babble))
				{
					greeting = babble(speech.babble);
				}
				return new Comment("greeting", persona.id, ChatterData.REMARK, persona.id + "/greeting", greeting, speech != null, "(greeting)");
			}
		}

		// A news-teller hears everything that starts in their own part of the world
		ChatterData.Region home = persona.newsOnly ? homeRegion(npcLocation) : null;

		List<ChatterData.Topic> candidates = new ArrayList<>();
		List<Map<String, String>> candidateVars = new ArrayList<>();
		for (ChatterData.Topic topic : data.topics)
		{
			// Sayings are many different things, kept from repeating by the line history instead
			Long lastUsed = topicLastUsed.get(topic.id);
			if (!topic.inVoice && lastUsed != null && now - lastUsed < topicCooldownMillis)
			{
				continue;
			}

			Map<String, String> vars = match(topic, snapshot, npcLocation, speech, home);
			if (vars != null)
			{
				candidates.add(topic);
				candidateVars.add(vars);
			}
		}

		if (persona.newsOnly)
		{
			List<ChatterData.Topic> news = new ArrayList<>(candidates);
			List<Map<String, String>> newsVars = new ArrayList<>(candidateVars);
			Set<ChatterData.Topic> local = keepNews(news, newsVars, home, npcLocation);
			Comment comment = say(news, newsVars, local, snapshot, persona, speech, race, npcName, npcLocation, mood, now);
			if (comment != null)
			{
				return comment;
			}
		}
		return say(candidates, candidateVars, Collections.emptySet(), snapshot, persona, speech, race, npcName, npcLocation, mood, now);
	}

	/**
	 * @param localNews quest news from the speaker's own region, where unsolved trouble is preferred
	 */
	private Comment say(List<ChatterData.Topic> candidates, List<Map<String, String>> candidateVars, Set<ChatterData.Topic> localNews, CharacterReader.Snapshot snapshot,
		ChatterData.Persona persona, ChatterData.Speech speech, boolean race, String npcName, WorldPoint npcLocation,
		ChattyNpcsConfig.Mood mood, long now)
	{
		double[] weights = topicWeights(candidates, candidateVars, localNews, persona, npcName, npcLocation, mood);
		int idx;
		while ((idx = pickWeighted(weights)) >= 0)
		{
			weights[idx] = 0;
			ChatterData.Topic topic = candidates.get(idx);
			Map<String, String> vars = candidateVars.get(idx);

			boolean starter = isStarter(topic, npcName);
			Picked picked = pickLine(topic, persona, starter, mood, vars);
			if (picked != null)
			{
				topicLastUsed.put(topic.id, now);
				// A news-teller's news is their trade, and shouldn't use up everyone else's
				if (!persona.newsOnly)
				{
					recentNews.addLast(isNews(topic, npcName));
				}
				while (recentNews.size() > NEWS_MEMORY)
				{
					recentNews.removeFirst();
				}
				// Quest givers speak in complete lines of their own, gossip is overheard rather than
				// said to you, and some topics are too grave for "Next, please."
				boolean wrapped = !starter && topic.wrap && !ChatterData.GOSSIP.equals(picked.getTone());
				// Quest givers' lines are already in their own voice. A race's tongue reaches persona
				// lines too, but a place's accent only bends the lines everyone shares.
				String text = picked.getText();
				boolean local = topic.inVoice;
				// Lines written in a race's own voice (gnome/, tzhaar/) are already said their way
				boolean voiced = persona.speech != null && picked.getKey().startsWith(persona.id + "/");
				boolean translated = speech != null && !starter && !topic.inVoice && !voiced && (race || isShared(picked.getKey()));
				if (translated)
				{
					String plain = text;
					text = translate(text, speech, picked.getTone());
					local |= !text.equals(plain);
				}
				if (persona.exclaim != null && isShared(picked.getKey()))
				{
					text = replace(text, EXCLAMATION, persona.exclaim.chance, m -> pickWord(persona.exclaim) + (m.group(2) == null ? "" : m.group(2)));
				}
				text = wrapped ? wrap(text, persona, picked.getTone(), speech == null) : text;
				if (translated)
				{
					String plain = text;
					text = greet(text, speech, picked.getTone());
					local |= !text.equals(plain);
				}
				text = earnedAddress(text, speech);
				if (speech != null && speech.babble != null && !understands(snapshot, speech.babble))
				{
					text = babble(speech.babble);
					local = true;
				}
				return new Comment(topic.id, persona.id, picked.getTone(), picked.getKey(), text, local, picked.getText());
			}
		}
		return null;
	}

	List<String> matchingTopics(CharacterReader.Snapshot snapshot, WorldPoint location)
	{
		List<String> ids = new ArrayList<>();
		for (ChatterData.Topic topic : data.topics)
		{
			if (match(topic, snapshot, location, null, null) != null)
			{
				ids.add(topic.id);
			}
		}
		return ids;
	}

	/**
	 * @return the placeholder values for this topic, or null if it doesn't apply to the player
	 */
	/**
	 * @param home for a news-teller, their own region: quests starting anywhere in it are known
	 */
	private Map<String, String> match(ChatterData.Topic topic, CharacterReader.Snapshot snapshot, WorldPoint npcLocation, ChatterData.Speech speech, ChatterData.Region home)
	{
		if (!topic.regions.isEmpty() && topic.regions.stream().noneMatch(r -> inRegion(r, npcLocation)))
		{
			return null;
		}
		// Only item topics have a placeholder to fill
		Map<String, String> vars = Collections.emptyMap();
		if (topic.fact != null)
		{
			return snapshot.facts.contains(topic.fact) ? snapshot.vars : null;
		}
		if (topic.item != null)
		{
			return matchItem(topic.item, snapshot, new HashMap<>());
		}
		if (topic.pet != null)
		{
			return snapshot.pet != null && topic.pet.compiled.matcher(snapshot.pet).find() ? vars : null;
		}
		if (topic.speech != null)
		{
			return speech != null && speech.id.equals(topic.speech) ? vars : null;
		}
		if (topic.quest != null)
		{
			return matchQuest(topic.quest, snapshot, npcLocation, home, vars);
		}
		return null;
	}

	private static Map<String, String> matchItem(ChatterData.ItemRule rule, CharacterReader.Snapshot snapshot, Map<String, String> vars)
	{
		int count = 0;
		String first = null;
		for (Map.Entry<EquipmentInventorySlot, String> worn : snapshot.worn.entrySet())
		{
			if (!rule.slots.isEmpty() && !rule.slots.contains(worn.getKey().name()))
			{
				continue;
			}
			if (rule.compiled.matcher(worn.getValue()).find())
			{
				count++;
				if (first == null)
				{
					first = worn.getValue();
				}
			}
		}

		if (count < rule.minCount)
		{
			return null;
		}
		vars.put("item", rule.label != null ? rule.label : inSentence(first));
		return vars;
	}

	private Map<String, String> matchQuest(ChatterData.QuestRule rule, CharacterReader.Snapshot snapshot, WorldPoint npcLocation, ChatterData.Region home, Map<String, String> vars)
	{
		QuestState state = snapshot.quests.get(rule.compiled);
		if (state == null || !rule.compiledStates.contains(state))
		{
			return null;
		}
		for (Quest later : rule.compiledSupersededBy)
		{
			if (snapshot.quests.get(later) == QuestState.FINISHED)
			{
				return null;
			}
		}
		for (Quest earlier : rule.compiledRequires)
		{
			if (snapshot.quests.get(earlier) != QuestState.FINISHED)
			{
				return null;
			}
		}

		// At the Grand Exchange, everyone's heard everything
		if (!hearsEverything(npcLocation))
		{
			if (rule.near != null && !withinReach(rule.near, npcLocation) && (home == null || !home.contains(rule.near.x, rule.near.y)))
			{
				return null;
			}

			if (rule.region != null && !rule.anywhere && !inRegion(rule.region, npcLocation))
			{
				return null;
			}
		}

		// In a cut-off place, even the grandest news from elsewhere never arrives
		if (rule.anywhere && !hearsOf(rule.region, npcLocation))
		{
			return null;
		}

		return vars;
	}

	private boolean hearsEverything(WorldPoint location)
	{
		return location != null && data.regions.stream().anyMatch(r -> r.hearsEverything && r.contains(location.getX(), location.getY()));
	}

	/**
	 * Whether news from a region reaches someone standing here
	 */
	private boolean hearsOf(String newsRegion, WorldPoint location)
	{
		if (location == null)
		{
			return true;
		}
		for (ChatterData.Region region : data.regions)
		{
			if (!region.onlyHears.isEmpty() && region.contains(location.getX(), location.getY())
				&& (newsRegion == null || !region.onlyHears.contains(newsRegion)))
			{
				return false;
			}
		}
		return true;
	}

	private boolean inRegion(String region, WorldPoint location)
	{
		return location != null && regions.get(region).contains(location.getX(), location.getY());
	}

	/**
	 * The NPC who gives out a quest talks about it in the first person
	 */
	private static boolean isStarter(ChatterData.Topic topic, String npcName)
	{
		if (topic.quest == null || topic.quest.compiledStarter == null || !topic.quest.compiledStarter.matcher(npcName).find())
		{
			return false;
		}
		for (String key : topic.lines.keySet())
		{
			if (key.startsWith(STARTER))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Weighted by priority squared, so notable things get mentioned far more than mundane ones.
	 * Quest givers would much rather talk about their own quest, and locals about the big story that
	 * happened on their doorstep.
	 * <p>
	 * A topic with only a few lines it hasn't said lately fades back, and one with none is left out,
	 * so nobody repeats themselves while there's anything else to say. Quest news shares a budget of
	 * {@link #newsShare}, so a well-travelled adventurer still hears about their hat.
	 */
	private double[] topicWeights(List<ChatterData.Topic> topics, List<Map<String, String>> vars, Set<ChatterData.Topic> local, ChatterData.Persona persona,
		String npcName, WorldPoint npcLocation, ChattyNpcsConfig.Mood mood)
	{
		double[] weights = new double[topics.size()];
		int[] fresh = new int[topics.size()];
		boolean anyFresh = false;
		for (int i = 0; i < topics.size(); i++)
		{
			ChatterData.Topic topic = topics.get(i);
			boolean starter = isStarter(topic, npcName);
			weights[i] = (double) topic.priority * topic.priority * (starter ? STARTER_BOOST : 1);
			if (topic.quest != null && topic.quest.anywhere && topic.quest.region != null && inRegion(topic.quest.region, npcLocation))
			{
				weights[i] *= REGION_BOOST;
			}
			if (topic.inVoice)
			{
				weights[i] *= flavour;
			}
			if (local.contains(topic) && !topic.quest.compiledStates.contains(QuestState.FINISHED))
			{
				weights[i] *= UNFINISHED_BOOST;
			}
			fresh[i] = freshLines(topic, persona, starter, mood, vars.get(i));
			anyFresh |= fresh[i] > 0;
		}
		// Everything's been said lately: fall back to repeating whatever was said longest ago
		if (anyFresh)
		{
			for (int i = 0; i < weights.length; i++)
			{
				weights[i] *= Math.min(fresh[i], FRESH_LINES) / (double) FRESH_LINES;
			}
		}

		double news = 0;
		double other = 0;
		for (int i = 0; i < weights.length; i++)
		{
			if (isNews(topics.get(i), npcName))
			{
				news += weights[i];
			}
			else
			{
				other += weights[i];
			}
		}
		// Over budget lately: let them talk about something else while there's anything else
		if (other > 0 && recentNews.size() >= NEWS_MEMORY && recentNews.stream().filter(n -> n).count() >= newsShare * NEWS_MEMORY)
		{
			for (int i = 0; i < weights.length; i++)
			{
				if (isNews(topics.get(i), npcName))
				{
					weights[i] = 0;
				}
			}
		}
		else if (other > 0 && news / (news + other) > newsShare)
		{
			double scale = newsShare * other / ((1 - newsShare) * news);
			for (int i = 0; i < weights.length; i++)
			{
				if (isNews(topics.get(i), npcName))
				{
					weights[i] *= scale;
				}
			}
		}
		return weights;
	}

	/**
	 * Narrows the candidates to one kind of news: local (starting in the speaker's own region) or
	 * from further afield, half and half. Leaves them alone if there's no news at all.
	 *
	 * @return the local news among what's left
	 */
	private Set<ChatterData.Topic> keepNews(List<ChatterData.Topic> candidates, List<Map<String, String>> vars, ChatterData.Region home, WorldPoint location)
	{
		Set<ChatterData.Topic> local = new HashSet<>();
		Set<ChatterData.Topic> afar = new HashSet<>();
		for (ChatterData.Topic topic : candidates)
		{
			if (topic.quest != null)
			{
				(isLocal(topic.quest, home, location) ? local : afar).add(topic);
			}
		}
		if (local.isEmpty() && afar.isEmpty())
		{
			return local;
		}
		boolean pickLocal = !local.isEmpty() && (afar.isEmpty() || random.nextDouble() < LOCAL_NEWS_SHARE);
		Set<ChatterData.Topic> keep = pickLocal ? local : afar;
		for (int i = candidates.size() - 1; i >= 0; i--)
		{
			if (!keep.contains(candidates.get(i)))
			{
				candidates.remove(i);
				vars.remove(i);
			}
		}
		return pickLocal ? local : Collections.emptySet();
	}

	/**
	 * Whether a quest belongs to this part of the world: it starts in the region, or it's the
	 * region's own story. The Grand Exchange hears the whole world's news, but only its own is local.
	 */
	private boolean isLocal(ChatterData.QuestRule rule, ChatterData.Region home, WorldPoint location)
	{
		if (rule.anywhere)
		{
			return false;
		}
		if (rule.near != null)
		{
			return home != null ? home.contains(rule.near.x, rule.near.y) : withinReach(rule.near, location);
		}
		return rule.region != null && inRegion(rule.region, location);
	}

	private static boolean withinReach(ChatterData.Near near, WorldPoint location)
	{
		return location != null
			&& Math.abs(location.getX() - near.x) <= near.radius
			&& Math.abs(location.getY() - near.y) <= near.radius;
	}

	/**
	 * The smallest region here: the speaker's own part of the world
	 */
	private ChatterData.Region homeRegion(WorldPoint location)
	{
		ChatterData.Region best = null;
		for (ChatterData.Region region : data.regions)
		{
			if (location != null && region.contains(location.getX(), location.getY()) && (best == null || region.area() < best.area()))
			{
				best = region;
			}
		}
		return best;
	}

	/**
	 * The most specific named place here, or null
	 */
	private ChatterData.Region placeAt(WorldPoint location)
	{
		ChatterData.Region best = null;
		long bestArea = Long.MAX_VALUE;
		for (ChatterData.Region region : data.regions)
		{
			if (region.place != null && location != null && region.contains(location.getX(), location.getY()) && region.area() < bestArea)
			{
				best = region;
				bestArea = region.area();
			}
		}
		return best;
	}

	/**
	 * Whether this NPC would welcome you right now: the first thing said on a visit is a greeting,
	 * and it shouldn't wait its turn
	 */
	boolean greetingDue(String npcName, WorldPoint location, long now)
	{
		ChatterData.Persona persona = personaFor(npcName, location);
		if (!greetings || persona == null || persona.greetings.isEmpty())
		{
			return false;
		}
		Long last = visits.get(visitKey(location));
		return last == null || now - last > VISIT_GAP_MILLIS;
	}

	/**
	 * You're still here, within earshot of someone who greets you, so it's the same visit
	 */
	void stillHere(String npcName, WorldPoint location, long now)
	{
		ChatterData.Persona persona = personaFor(npcName, location);
		if (persona != null && !persona.greetings.isEmpty())
		{
			visits.computeIfPresent(visitKey(location), (key, last) -> now);
		}
	}

	/**
	 * News-tellers are there to talk while you stand with them, so they don't take turns keeping quiet
	 */
	boolean talksFreely(String npcName, WorldPoint location)
	{
		ChatterData.Persona persona = personaFor(npcName, location);
		return persona != null && persona.newsOnly;
	}

	private static String visitKey(WorldPoint location)
	{
		// Wide enough that every booth in one bank (even the Grand Exchange) shares a visit
		return location == null ? "nowhere" : (location.getX() >> 5) + "," + (location.getY() >> 5) + "," + location.getPlane();
	}

	/**
	 * "Welcome, human, to Ta Quir Priw bank!", with what the locals call you and their place
	 */
	private String greeting(ChatterData.Persona persona, ChatterData.Speech speech, WorldPoint location)
	{
		ChatterData.Region region = placeAt(location);
		String you = speech != null && speech.address != null ? addressWord(speech.address) : DEFAULT_ADDRESS.get(random.nextInt(DEFAULT_ADDRESS.size()));
		String text = persona.greetings.get(random.nextInt(persona.greetings.size()))
			.replace("{you}", you)
			.replace("{place}", region != null ? region.place : "our");
		text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
		if (speech != null)
		{
			text = greet(translate(text, speech, ChatterData.REMARK), speech, ChatterData.REMARK, false);
		}
		return text;
	}

	/**
	 * Quest news, but not a quest giver going on about their own quest
	 */
	private static boolean isNews(ChatterData.Topic topic, String npcName)
	{
		return topic.quest != null && !isStarter(topic, npcName);
	}

	/**
	 * @return an index picked in proportion to its weight, or -1 if every weight is zero
	 */
	private int pickWeighted(double[] weights)
	{
		double total = 0;
		for (double weight : weights)
		{
			total += weight;
		}
		if (total <= 0)
		{
			return -1;
		}
		double roll = random.nextDouble() * total;
		int last = -1;
		for (int i = 0; i < weights.length; i++)
		{
			if (weights[i] > 0)
			{
				last = i;
				roll -= weights[i];
				if (roll < 0)
				{
					return i;
				}
			}
		}
		return last;
	}

	/**
	 * How many lines this persona could say about a topic in this mood without repeating a recent one
	 */
	private int freshLines(ChatterData.Topic topic, ChatterData.Persona persona, boolean starter, ChattyNpcsConfig.Mood mood, Map<String, String> vars)
	{
		int fresh = 0;
		for (String tone : ChatterData.TONES)
		{
			if (toneWeight(topic, persona, mood, tone) <= 0)
			{
				continue;
			}
			for (Line line : linesFor(topic, persona, starter, tone))
			{
				if (!recentLines.contains(line.getTemplate()) && fill(line.getTemplate(), vars) != null)
				{
					fresh++;
				}
			}
		}
		return fresh;
	}

	private static double toneWeight(ChatterData.Topic topic, ChatterData.Persona persona, ChattyNpcsConfig.Mood mood, String tone)
	{
		double weight = persona.tones.getOrDefault(tone, 1.0) * moodWeight(mood, tone);
		return tone.equals(topic.lean) ? weight * LEAN_BOOST : weight;
	}

	private Picked pickLine(ChatterData.Topic topic, ChatterData.Persona persona, boolean starter, ChattyNpcsConfig.Mood mood, Map<String, String> vars)
	{
		Picked picked = pickLine(topic, persona, starter, mood, vars, false);
		if (picked == null)
		{
			// Everything's been said recently, so repeat whatever was said longest ago, unless it was just now
			picked = pickLine(topic, persona, starter, mood, vars, true);
		}
		return picked;
	}

	/**
	 * Recently said lines are left out before tones are weighed, so in Balanced mood a topic whose
	 * insults were all said recently leans on its other tones for a while. That's intended: variety
	 * matters more than keeping the exact mix.
	 */
	private Picked pickLine(ChatterData.Topic topic, ChatterData.Persona persona, boolean starter, ChattyNpcsConfig.Mood mood, Map<String, String> vars, boolean allowRecent)
	{
		Map<String, List<Line>> usableByTone = new LinkedHashMap<>();
		Map<String, Double> weights = new LinkedHashMap<>();
		for (String tone : ChatterData.TONES)
		{
			double weight = toneWeight(topic, persona, mood, tone);
			if (weight <= 0)
			{
				continue;
			}

			List<Line> usable = new ArrayList<>();
			for (Line line : linesFor(topic, persona, starter, tone))
			{
				boolean said = allowRecent ? saidJustNow(line.getTemplate()) : recentLines.contains(line.getTemplate());
				if (!said && fill(line.getTemplate(), vars) != null)
				{
					usable.add(line);
				}
			}
			if (!usable.isEmpty())
			{
				usableByTone.put(tone, usable);
				weights.put(tone, weight);
			}
		}

		if (usableByTone.isEmpty())
		{
			return null;
		}

		double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
		double roll = random.nextDouble() * total;
		String chosenTone = null;
		for (Map.Entry<String, Double> entry : weights.entrySet())
		{
			chosenTone = entry.getKey();
			roll -= entry.getValue();
			if (roll < 0)
			{
				break;
			}
		}

		List<Line> lines = usableByTone.get(chosenTone);
		Line line = allowRecent ? oldest(lines) : lines.get(random.nextInt(lines.size()));
		recentLines.remove(line.getTemplate());
		recentLines.addLast(line.getTemplate());
		while (recentLines.size() > RECENT_LINES)
		{
			recentLines.removeFirst();
		}
		return new Picked(chosenTone, line.getKey(), fill(line.getTemplate(), vars));
	}

	private boolean saidJustNow(String template)
	{
		Iterator<String> newestFirst = recentLines.descendingIterator();
		for (int i = 0; i < NO_REPEAT_LINES && newestFirst.hasNext(); i++)
		{
			if (newestFirst.next().equals(template))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the line said longest ago, or one never said at all
	 */
	private Line oldest(List<Line> lines)
	{
		for (String recent : recentLines)
		{
			for (Line line : lines)
			{
				if (line.getTemplate().equals(recent))
				{
					return line;
				}
			}
		}
		return lines.get(0);
	}

	private List<Line> linesFor(ChatterData.Topic topic, ChatterData.Persona persona, boolean starter, String tone)
	{
		List<Line> lines = new ArrayList<>();
		if (starter)
		{
			addLines(lines, topic, STARTER + tone);
			return lines;
		}
		lines = sharedAndOwnLines(topic, persona, tone);
		// Nobody gossips about themselves: "They say Thurgo..." from Thurgo
		lines.removeIf(line -> mentionsSpeaker(line.getTemplate()));
		return lines;
	}

	private List<Line> sharedAndOwnLines(ChatterData.Topic topic, ChatterData.Persona persona, String tone)
	{
		List<Line> lines = new ArrayList<>();

		// With no local flavour at all, a race speaks plainly, like everyone else
		// Races sealed to their own lines (goblins, TzHaar, ogres) have no plain English to fall back on
		boolean plainRace = flavour <= 0 && persona.speech != null && !persona.ownLinesOnly;
		if (!plainRace)
		{
			addLines(lines, topic, persona.id + "/" + tone);
		}
		if (persona.ownLinesOnly && !plainRace)
		{
			// A race's own sayings are theirs, even when shared lines aren't
			boolean ownSayings = topic.inVoice && (persona.speech != null || !persona.regions.isEmpty());
			if (ownSayings || (persona.sharesGossip && tone.equals(ChatterData.GOSSIP)))
			{
				addLines(lines, topic, tone);
			}
			return lines;
		}

		addLines(lines, topic, tone);
		if (heartland)
		{
			addLines(lines, topic, HEARTLAND + tone);
		}
		// Sharper lines only for personas with the tongue for them
		int sharpness = ChatterData.TEMPERAMENTS.indexOf(persona.temperament);
		for (int i = 1; i <= sharpness; i++)
		{
			addLines(lines, topic, ChatterData.TEMPERAMENTS.get(i) + "/" + tone);
		}
		return lines;
	}

	private static List<String> nameWords(String npcName)
	{
		List<String> words = new ArrayList<>();
		for (String word : npcName.split("[^A-Za-z']+"))
		{
			if (word.length() >= 4 && Character.isUpperCase(word.charAt(0)) && !TITLE_WORDS.contains(word))
			{
				words.add(word);
			}
		}
		return words;
	}

	private boolean mentionsSpeaker(String line)
	{
		for (String word : speakerNameWords)
		{
			if (line.matches("(?s).*\\b" + Pattern.quote(word) + "\\b.*"))
			{
				return true;
			}
		}
		return false;
	}

	private static void addLines(List<Line> lines, ChatterData.Topic topic, String key)
	{
		for (String template : topic.lines.getOrDefault(key, Collections.emptyList()))
		{
			lines.add(new Line(key, template));
		}
	}

	/**
	 * At most one addition per line, an opener or a closer, and only one that suits the line
	 *
	 * @param plain the speaker has no local voice, so plain wrappers like "Blimey." suit them
	 */
	private String wrap(String line, ChatterData.Persona persona, String tone, boolean plain)
	{
		// "By Guthix!" and "Hello, human!" are local flavour too
		if ((persona.plainOnly && !plain) || (flavour <= 0 && persona.speech != null && !persona.ownLinesOnly))
		{
			return line;
		}
		if (random.nextDouble() < persona.openerChance)
		{
			String opener = pickWrapper(persona.openers, tone);
			if (opener != null && line.length() + opener.length() < MAX_WRAPPED_LENGTH && !saysAlready(line, opener)
				&& !(OATH.matcher(opener).find() && OATH.matcher(line).find())
				&& !INTERJECTION.matcher(line).find() && !openerEchoes(opener, line)
				&& !(addressesYou(opener) && addressesYou(line)))
			{
				return opener + " " + line;
			}
		}
		else if (random.nextDouble() < persona.closerChance)
		{
			String closer = pickWrapper(persona.closers, tone);
			if (closer != null && line.length() + closer.length() < MAX_WRAPPED_LENGTH && !saysAlready(line, closer) && !echoes(line, closer)
				&& !openerEchoes(closer, line) && !(OATH.matcher(closer).find() && OATH.matcher(line).find())
				&& !(addressesYou(closer) && addressesYou(line)))
			{
				return line + " " + closer;
			}
		}
		return line;
	}

	/**
	 * A wrapper written "insult|remark: Stoopid!" only goes with those tones
	 */
	private String pickWrapper(List<String> wrappers, String tone)
	{
		List<String> suitable = new ArrayList<>();
		for (String wrapper : wrappers)
		{
			Matcher m = WRAPPER_TONES.matcher(wrapper);
			if (!m.matches())
			{
				suitable.add(wrapper);
			}
			else if (Arrays.asList(m.group(1).split("\\|")).contains(tone))
			{
				suitable.add(m.group(2));
			}
		}
		return suitable.isEmpty() ? null : suitable.get(random.nextInt(suitable.size()));
	}

	/**
	 * "Have a look at my wares. Look at you..." trips over itself
	 */
	private static boolean openerEchoes(String opener, String line)
	{
		String[] start = line.toLowerCase(Locale.ROOT).split("[^a-z']+");
		String[] openerWords = opener.toLowerCase(Locale.ROOT).split("[^a-z']+");
		if (start.length > 0 && openerWords.length > 0 && start[0].equals(openerWords[0]))
		{
			return true;
		}
		Set<String> opening = new HashSet<>(Arrays.asList(start).subList(0, Math.min(4, start.length)));
		for (String word : opener.toLowerCase(Locale.ROOT).split("[^a-z']+"))
		{
			if (word.length() >= 4 && opening.contains(word))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * "...mind. Mind how you go." trips over itself
	 */
	private static boolean echoes(String line, String closer)
	{
		String[] lineWords = line.toLowerCase(Locale.ROOT).split("[^a-z']+");
		String first = closer.toLowerCase(Locale.ROOT).split("[^a-z']+")[0];
		return lineWords.length > 0 && lineWords[lineWords.length - 1].equals(first);
	}


	/**
	 * Whether anyone can say lines under this key, rather than one persona or the quest giver
	 */
	private static boolean isShared(String key)
	{
		int slash = key.indexOf('/');
		return slash < 0 || ChatterData.TEMPERAMENTS.contains(key.substring(0, slash)) || key.startsWith(HEARTLAND);
	}

	private static final Pattern EXCLAMATION = Pattern.compile("\\b(Good heavens|Goodness|Ye gods|Gadzooks|Blimey|Cor)\\b([.!,]\\s*)?");
	private static final Pattern GREETING = Pattern.compile("^(Hello|Hi|Heya|Hail|Greetings|Good day|Welcome|Well met|Ho there|Oh, hello|Now then|Ahoy)\\b");
	private static final Pattern PRAISE = Pattern.compile("\\b(Lovely|Marvellous|Splendid|Smashing|Fair play)\\b(?=[.!,])");
	private static final Pattern NOUN_ADDRESS = Pattern.compile("\\b(?:(an?) )?(?:traveller|adventurer)\\b(?!s)", Pattern.CASE_INSENSITIVE);
	/**
	 * The first sentence, if it's short and doesn't already have a comma in it
	 */
	private static final Pattern FIRST_SENTENCE = Pattern.compile("^([^.!?,]{8,45})([.!?])");
	/**
	 * Spelt-out contractions, leaving question tags like "isn't it?" alone
	 */
	private static final List<ChatterData.Swap> FORMAL = Arrays.asList(
		swap("\\b(do|did|does|is|are|was|were|would|could|should|has|have|had)n't\\b(?! (it|you|he|she|they|we|I|that|there)\\b)", "$1 not"),
		swap("\\bwon't\\b(?! (it|you|he|she|they|we|I|that|there)\\b)", "will not"),
		swap("\\bcan't\\b", "cannot"),
		swap("\\b(you|we|they)'re\\b", "$1 are"));

	private static ChatterData.Swap swap(String pattern, String replace)
	{
		ChatterData.Swap swap = new ChatterData.Swap();
		swap.pattern = pattern;
		swap.replace = replace;
		swap.compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		return swap;
	}

	/**
	 * Says a line the way someone talks there, for testing
	 */
	String translate(String line, String npcName, WorldPoint location, String tone)
	{
		ChatterData.Speech speech = speechFor(npcName, location);
		return speech == null ? line : greet(translate(line, speech, tone), speech, tone);
	}

	/**
	 * Puts a line into someone's own words
	 *
	 * @param tone the line's tone. Gossip is overheard, so it's never addressed to the player.
	 */
	private String translate(String line, ChatterData.Speech speech, String tone)
	{
		boolean toYou = !ChatterData.GOSSIP.equals(tone);
		String out = line;
		// One of their own words per line is seasoning; two is a different language
		for (ChatterData.Swap swap : speech.swaps)
		{
			String swapped = replace(out, swap.compiled, strength(swap.chance), m -> expand(swap.replace, m));
			if (!swapped.equals(out))
			{
				out = swapped;
				break;
			}
		}
		for (ChatterData.Swap swap : FORMAL)
		{
			out = replace(out, swap.compiled, strength(speech.formal), m -> expand(swap.replace, m));
		}
		if (speech.exclaim != null)
		{
			double exclaimChance = speech.exclaim.tones.isEmpty() || speech.exclaim.tones.contains(tone) ? strength(speech.exclaim.chance) : 0;
			out = replace(out, EXCLAMATION, exclaimChance, m ->
			{
				String word = pickWord(speech.exclaim);
				return word.isEmpty() ? "" : word + (m.group(2) == null ? "" : m.group(2));
			});
			out = out.trim();
			if (out.isEmpty())
			{
				return line;
			}
			out = Character.toUpperCase(out.charAt(0)) + out.substring(1);
		}
		if (speech.praise != null)
		{
			out = replace(out, PRAISE, strength(speech.praise.chance), m -> pickWord(speech.praise));
		}
		ChatterData.Address address = speech.address;
		if (address != null)
		{
			String word = addressWord(address);
			out = replace(out, address.compiledVocative, strength(address.swap), m -> word);
			if (address.asNoun)
			{
				out = replace(out, NOUN_ADDRESS, strength(address.swap), m -> m.group(1) == null ? word : article(word) + " " + word);
			}
			boolean aimedAtYou = ChatterData.COMPLIMENT.equals(tone) || ChatterData.INSULT.equals(tone);
			if (aimedAtYou && !addressesYou(out) && !out.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT)) && random.nextDouble() < strength(address.append))
			{
				out = FIRST_SENTENCE.matcher(out).replaceFirst("$1, " + Matcher.quoteReplacement(word) + "$2");
			}
		}
		return out.length() <= MAX_WRAPPED_LENGTH ? out : line;
	}

	/**
	 * Hello and goodbye in their own words, round a line that's already been wrapped, so they don't
	 * follow a persona's "Hello there."
	 */
	private String greet(String line, ChatterData.Speech speech, String tone)
	{
		return greet(line, speech, tone, true);
	}

	/**
	 * @param withFarewell whether goodbye may follow: not after a welcome
	 */
	private String greet(String line, ChatterData.Speech speech, String tone, boolean withFarewell)
	{
		if (ChatterData.GOSSIP.equals(tone))
		{
			return line;
		}
		String out = line;
		boolean greeted = false;
		if (speech.greet != null && !GREETING.matcher(out).find() && random.nextDouble() < strength(speech.greet.chance))
		{
			out = pickWord(speech.greet) + " " + out;
			greeted = true;
		}
		if (!greeted && withFarewell && speech.farewell != null && out.matches(".*[.!?]$") && random.nextDouble() < strength(speech.farewell.chance))
		{
			String farewell = pickWord(speech.farewell);
			out = out.endsWith(farewell) ? out : out + " " + farewell;
		}
		return out.length() <= MAX_WRAPPED_LENGTH ? out : line;
	}

	/**
	 * A chance from speech.json, scaled by the local flavour setting
	 */
	private double strength(double chance)
	{
		return Math.min(1, chance * flavour);
	}

	/**
	 * Once you've earned it, the old name goes everywhere, even in lines written with it
	 */
	private String earnedAddress(String text, ChatterData.Speech speech)
	{
		if (speech == null || speech.address == null || speech.address.compiledUntil == null)
		{
			return text;
		}
		String word = addressWord(speech.address);
		return word.equals(speech.address.word) ? text : text.replaceAll("\\b" + Pattern.quote(speech.address.word) + "\\b", Matcher.quoteReplacement(word));
	}

	/**
	 * What they call you: "outerlander" until you've earned "brother" or "sister"
	 */
	private String addressWord(ChatterData.Address address)
	{
		if (address.compiledUntil != null && hearer != null && hearer.quests.get(address.compiledUntil) == QuestState.FINISHED)
		{
			return address.thenWord.replace("{kin}", hearer.vars.getOrDefault("kin", "friend"));
		}
		return address.word;
	}

	private String pickWord(ChatterData.Words words)
	{
		return words.words.get(random.nextInt(words.words.size()));
	}

	private static String article(String word)
	{
		return "aeiouAEIOU".indexOf(word.charAt(0)) >= 0 ? "an" : "a";
	}

	/**
	 * Whether you can follow what they're saying
	 */
	private static boolean understands(CharacterReader.Snapshot snapshot, ChatterData.Babble babble)
	{
		if (babble.compiledWearing != null && snapshot.worn.values().stream().anyMatch(item -> babble.compiledWearing.matcher(item).find()))
		{
			return true;
		}
		if (babble.compiledCarrying != null && snapshot.carried.stream().anyMatch(item -> babble.compiledCarrying.matcher(item).find()))
		{
			return true;
		}
		return babble.compiledAfter.stream().anyMatch(quest -> snapshot.quests.get(quest) == QuestState.FINISHED);
	}

	/**
	 * What it sounds like when you can't understand a word: phrases strung together the way the
	 * game does it
	 */
	private String babble(ChatterData.Babble babble)
	{
		StringBuilder sb = new StringBuilder();
		String last = null;
		while (sb.chars().filter(c -> c == ' ').count() < babble.spaces)
		{
			String phrase = babble.phrases.get(random.nextInt(babble.phrases.size()));
			if (phrase.equals(last) && babble.phrases.size() > 1)
			{
				continue;
			}
			sb.append(phrase).append(' ');
			last = phrase;
		}
		return sb.toString().trim();
	}

	/**
	 * Replaces each match with the given chance, keeping the capital if the match started with one
	 */
	private String replace(String line, Pattern pattern, double chance, java.util.function.Function<Matcher, String> replacement)
	{
		if (chance <= 0)
		{
			return line;
		}
		Matcher m = pattern.matcher(line);
		StringBuilder sb = new StringBuilder();
		int last = 0;
		while (m.find())
		{
			sb.append(line, last, m.start());
			String match = m.group();
			String replaced = match;
			if (random.nextDouble() < chance)
			{
				replaced = replacement.apply(m);
				if (!match.isEmpty() && !replaced.isEmpty() && Character.isUpperCase(match.charAt(0)))
				{
					replaced = Character.toUpperCase(replaced.charAt(0)) + replaced.substring(1);
				}
			}
			sb.append(replaced);
			last = m.end();
		}
		sb.append(line, last, line.length());
		return sb.toString();
	}

	/**
	 * Fills $1, $2 and so on in a replacement from the match's groups
	 */
	private static String expand(String replace, Matcher m)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < replace.length(); i++)
		{
			char c = replace.charAt(i);
			if (c == '$' && i + 1 < replace.length() && Character.isDigit(replace.charAt(i + 1)))
			{
				int group = replace.charAt(++i) - '0';
				String value = group <= m.groupCount() ? m.group(group) : null;
				sb.append(value == null ? "" : value);
			}
			else
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Ways of addressing you. Short, but saying one twice ("Hello, dear. Ooh, put a hat on, dear.")
	 * is as bad as repeating a long word.
	 */
	private static final Set<String> ADDRESS_WORDS = new HashSet<>(Arrays.asList(
		"dear", "human", "guv", "love", "friend", "mate", "matey", "citizen", "traveller", "adventurer",
		"outerlander", "outlander", "stranger", "mortal", "effendi", "bwana", "iknami", "iknil", "jalyt", "creature", "sailor", "dweller"));

	private static boolean addressesYou(String line)
	{
		for (String word : line.toLowerCase(Locale.ROOT).split("[^a-z']+"))
		{
			if (ADDRESS_WORDS.contains(word))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether the line already says the opener or closer, or one of its distinctive words, so
	 * "Hail, outerlander." doesn't go in front of a line that already calls you outerlander
	 */
	static boolean saysAlready(String line, String wrapper)
	{
		String lowerLine = line.toLowerCase(Locale.ROOT);
		String words = wrapper.replaceAll("[^A-Za-z' ]", "").trim().toLowerCase(Locale.ROOT);
		if (words.isEmpty())
		{
			return false;
		}
		if (lowerLine.contains(words))
		{
			return true;
		}
		for (String word : words.split(" +"))
		{
			if ((word.length() >= 6 || ADDRESS_WORDS.contains(word)) && lowerLine.matches("(?s).*\\b" + Pattern.quote(word) + "\\b.*"))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the line with placeholders filled in, or null if one of them has no value
	 */
	static String fill(String template, Map<String, String> vars)
	{
		Matcher m = PLACEHOLDER.matcher(template);
		StringBuilder sb = new StringBuilder();
		while (m.find())
		{
			String value = vars.get(m.group(1));
			if (value == null)
			{
				return null;
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(value));
		}
		m.appendTail(sb);

		// Placeholders like {item} can start a sentence
		if (sb.length() > 0)
		{
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		}
		return sb.toString();
	}

	/**
	 * "Dragon scimitar" reads as "dragon scimitar" mid-sentence, but "Ahrim's hood" keeps its capital
	 */
	static String inSentence(String itemName)
	{
		String firstWord = itemName.split(" ", 2)[0];
		if (firstWord.endsWith("'s") || itemName.length() < 2)
		{
			return itemName;
		}
		return Character.toLowerCase(itemName.charAt(0)) + itemName.substring(1);
	}

	/**
	 * Kind and Rude stick to their tone, so you never get a compliment when you asked for abuse.
	 * Gossip isn't aimed at you, so it comes up in any mood.
	 */
	private static double moodWeight(ChattyNpcsConfig.Mood mood, String tone)
	{
		if (tone.equals(ChatterData.GOSSIP))
		{
			return 1;
		}
		switch (mood)
		{
			case KIND:
				return tone.equals(ChatterData.COMPLIMENT) ? 1 : 0;
			case RUDE:
				return tone.equals(ChatterData.INSULT) ? 1 : 0;
			default:
				return 1;
		}
	}
}
