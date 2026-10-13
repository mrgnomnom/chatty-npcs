package com.chattynpcs;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class LinePickerTest
{
	private static final WorldPoint LUMBRIDGE_CASTLE = new WorldPoint(3208, 3214, 0);
	private static final WorldPoint FALADOR = new WorldPoint(2965, 3380, 0);

	private ChatterData data;

	@Before
	public void before() throws Exception
	{
		data = ChatterData.load(new Gson());
	}

	private static CharacterReader.Snapshot snapshot(String... facts)
	{
		CharacterReader.Snapshot s = new CharacterReader.Snapshot();
		s.facts.add("always");
		for (String fact : facts)
		{
			s.facts.add(fact);
		}
		return s;
	}

	/**
	 * A picker with no memory of what's been said, for tests about who says what rather than
	 * repetition
	 */
	private LinePicker fresh(int seed)
	{
		// Seeded from a seeded Random, since Random's first draw barely changes between close seeds
		return new LinePicker(data, new Random(new Random(seed).nextLong()));
	}

	private LinePicker.Comment pick(LinePicker picker, CharacterReader.Snapshot s, String npc, WorldPoint where, ChattyNpcsConfig.Mood mood, long now)
	{
		return picker.pick(s, npc, where, mood, 0, now);
	}

	@Test
	public void everyTemplateFills()
	{
		Map<String, String> vars = new java.util.HashMap<>();
		vars.put("item", "x");
		vars.put("task", "x");
		for (ChatterData.Topic topic : data.topics)
		{
			for (List<String> lines : topic.lines.values())
			{
				for (String line : lines)
				{
					assertNotNull(topic.id + ": " + line, LinePicker.fill(line, vars));
				}
			}
		}
	}

	/**
	 * The rules from TONE.md that a computer can check
	 */
	@Test
	public void linesFollowTheToneGuide()
	{
		Set<String> prefixes = new HashSet<>(ChatterData.TEMPERAMENTS);
		prefixes.add("starter");
		prefixes.add("heartland");
		data.personas.forEach(p -> prefixes.add(p.id));
		Set<String> tones = new HashSet<>(ChatterData.TONES);
		Pattern banned = Pattern.compile("(?i)\\b(bloody|arse|arsed|muppet|just saying|no offence|lovely jubbly|sweet as a nut|on my life|oi oi|pet|duck|petal|ta-ra|telly|radio|lycra|rave|insurance|CV|fiver|tenner|cash|charity shop|stag do|bins|guide|cute|awesome|baddie|pathetic|useless|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b");

		for (ChatterData.Topic topic : data.topics)
		{
			for (Map.Entry<String, List<String>> entry : topic.lines.entrySet())
			{
				String key = entry.getKey();
				String[] parts = key.split("/");
				assertTrue(topic.id + ": bad key " + key, tones.contains(parts[parts.length - 1]));
				assertTrue(topic.id + ": bad key " + key, parts.length == 1 || (parts.length == 2 && prefixes.contains(parts[0])));

				for (String line : entry.getValue())
				{
					String where = topic.id + "/" + key + ": " + line;
					assertTrue(where + " is too long for a speech bubble", line.length() <= 70);
					assertFalse(where + " has a number in it", line.matches(".*\\d.*"));
					// Only villains get to say "pathetic"
					Matcher bannedWord = banned.matcher(line);
					while (bannedWord.find())
					{
						assertTrue(where + " uses a banned word", key.startsWith("shady/") && bannedWord.group().equalsIgnoreCase("pathetic"));
					}
					assertTrue(where + " uses {item} outside an item topic", topic.item != null || !line.contains("{item}"));
					assertTrue(where + " uses {task} outside a slayer task topic", (topic.fact != null && topic.fact.startsWith("slayer_task")) || !line.contains("{task}"));
					// Excitement is an exclamation mark, not capitals. HAM is a name.
					assertFalse(where + " shouts in capitals", line.replace("HAM", "").matches(".*\\b[A-Z]{2,}\\b.*"));
				}
			}
		}
		for (ChatterData.Persona persona : data.personas)
		{
			assertTrue(persona.id + " has a bad temperament", ChatterData.TEMPERAMENTS.contains(persona.temperament));
		}
	}

	@Test
	public void politePersonasNeverGetSharpLines()
	{
		LinePicker picker = new LinePicker(data, new Random(11));
		CharacterReader.Snapshot s = snapshot("bald", "starkers", "loaded");
		s.worn.put(EquipmentInventorySlot.HEAD, "Red partyhat");

		for (int i = 0; i < 300; i++)
		{
			LinePicker.Comment c = pick(picker, s, "Banker", null, ChattyNpcsConfig.Mood.RUDE, i);
			assertNotNull(c);
			assertFalse(c.getKey() + ": " + c.getText(), c.getKey().startsWith("plain/") || c.getKey().startsWith("sharp/"));
		}
	}

	@Test
	public void everythingInTheJsonResolves()
	{
		// Throws on a misspelt quest or quest state
		LinePicker picker = new LinePicker(data, new Random(0));
		assertTrue(picker.quests().contains(Quest.COOKS_ASSISTANT));
	}

	@Test
	public void personas()
	{
		LinePicker picker = new LinePicker(data, new Random(1));
		assertEquals("kid", picker.personaFor("Gnome child").id);
		assertEquals("banker", picker.personaFor("Banker").id);
		assertEquals("toff", picker.personaFor("Duke Horacio").id);
		assertEquals("trader", picker.personaFor("Shop keeper").id);
		assertEquals("barkeep", picker.personaFor("Bartender").id);
		assertEquals("local", picker.personaFor("Hans").id);
		assertEquals("wizard", picker.personaFor("Wizard Mizgog").id);
		assertEquals("shady", picker.personaFor("Mage of Zamorak").id);
		assertEquals("dwarf", picker.personaFor("Doric").id);
		assertEquals("gnome", picker.personaFor("King Narnode Shareen").id);
		assertEquals("fremennik", picker.personaFor("Brundt the Chieftain").id);
		assertEquals("pirate", picker.personaFor("Redbeard Frank").id);
		assertEquals("rough", picker.personaFor("Fred the Farmer").id);
		assertEquals("vicar", picker.personaFor("Father Aereck").id);
		assertEquals("nan", picker.personaFor("Hetty").id);
	}

	@Test
	public void notableGearWinsAndFillsItem()
	{
		LinePicker picker = new LinePicker(data, new Random(2));
		CharacterReader.Snapshot s = snapshot();
		s.worn.put(EquipmentInventorySlot.CAPE, "Infernal cape");

		int infernal = 0;
		for (int i = 0; i < 200; i++)
		{
			LinePicker.Comment c = pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, i);
			assertNotNull(c);
			assertFalse(c.getText(), c.getText().contains("{"));
			if (c.getTopicId().equals("infernal_cape"))
			{
				infernal++;
			}
		}
		assertTrue("infernal cape came up " + infernal + " times", infernal > 150);
	}

	@Test
	public void rudeOnlyInsultsAndKindOnlyCompliments()
	{
		LinePicker picker = new LinePicker(data, new Random(3));
		CharacterReader.Snapshot s = snapshot("bald", "skint", "low_hp");
		s.worn.put(EquipmentInventorySlot.CAPE, "Fire cape");
		s.quests.put(Quest.COOKS_ASSISTANT, QuestState.NOT_STARTED);
		s.quests.put(Quest.DRAGON_SLAYER_II, QuestState.FINISHED);

		String[] npcs = {"Hans", "Banker", "Gnome child", "Duke Horacio", "Cook", "Granny"};
		for (int i = 0; i < 300; i++)
		{
			String npc = npcs[i % npcs.length];
			// A banker's welcome is the same whatever the mood
			LinePicker.Comment rude = pick(picker, s, npc, LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.RUDE, i);
			assertTrue(npc + ": " + rude.getText(), rude.getTone().equals(ChatterData.INSULT) || rude.getTone().equals(ChatterData.GOSSIP) || rude.getTopicId().equals("greeting"));
			LinePicker.Comment kind = pick(picker, s, npc, LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.KIND, i);
			assertTrue(npc + ": " + kind.getText(), kind.getTone().equals(ChatterData.COMPLIMENT) || kind.getTone().equals(ChatterData.GOSSIP) || kind.getTopicId().equals("greeting"));
		}
	}

	@Test
	public void topicCooldown()
	{
		LinePicker picker = new LinePicker(data, new Random(4));
		CharacterReader.Snapshot s = snapshot("bald");
		s.facts.remove("always");

		assertNotNull(picker.pick(s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 60_000, 0));
		assertNull(picker.pick(s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 60_000, 30_000));
		assertNotNull(picker.pick(s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 60_000, 61_000));
	}

	@Test
	public void kidsOnlyUseTheirOwnLines()
	{
		LinePicker picker = new LinePicker(data, new Random(5));
		CharacterReader.Snapshot s = snapshot("bald");
		for (int i = 0; i < 50; i++)
		{
			LinePicker.Comment c = pick(fresh(i), s, "Gnome child", null, ChattyNpcsConfig.Mood.RUDE, i);
			assertNotNull(c);
			assertEquals("kid", c.getPersona());
			assertTrue(c.getKey() + ": " + c.getText(), c.getKey().startsWith("kid/"));
		}
	}

	@Test
	public void questsOnlyComeUpNearTheirStart()
	{
		LinePicker picker = new LinePicker(data, new Random(7));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		s.quests.put(Quest.COOKS_ASSISTANT, QuestState.NOT_STARTED);

		assertNull(pick(picker, s, "Hans", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0));
		assertEquals("cooks_assistant_todo", pick(picker, s, "Hans", LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.BALANCED, 1).getTopicId());
	}

	@Test
	public void questGiverTalksInFirstPerson()
	{
		LinePicker picker = new LinePicker(data, new Random(8));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		s.quests.put(Quest.COOKS_ASSISTANT, QuestState.NOT_STARTED);

		List<String> starterLines = data.topics.stream()
			.filter(t -> t.id.equals("cooks_assistant_todo"))
			.findFirst().get().lines.get("starter/insult");
		LinePicker.Comment c = pick(picker, s, "Cook", LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.RUDE, 0);
		assertTrue(c.getText(), starterLines.stream().anyMatch(c.getText()::contains));
	}

	@Test
	public void questGiversPreferTheirOwnQuest()
	{
		LinePicker picker = new LinePicker(data, new Random(10));
		CharacterReader.Snapshot s = snapshot("bald");
		s.worn.put(EquipmentInventorySlot.CAPE, "Infernal cape");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.NOT_STARTED);
		}

		// Until he's run out of fresh things to say about it, then he doesn't repeat himself
		int own = 0;
		for (int i = 0; i < 5; i++)
		{
			if (pick(picker, s, "Cook", LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.BALANCED, i).getTopicId().equals("cooks_assistant_todo"))
			{
				own++;
			}
		}
		assertTrue("Cook talked about his own quest " + own + " times out of 5", own >= 4);
	}

	private static ChatterData.Topic regionalTopic(String id, boolean anywhere, int priority)
	{
		ChatterData.Topic topic = new ChatterData.Topic();
		topic.id = id;
		topic.priority = priority;
		topic.quest = new ChatterData.QuestRule();
		topic.quest.name = "SINS_OF_THE_FATHER";
		topic.quest.states = Arrays.asList("FINISHED");
		topic.quest.region = "testland";
		topic.quest.anywhere = anywhere;
		topic.lines = new java.util.HashMap<>();
		topic.lines.put(ChatterData.GOSSIP, Arrays.asList("Did you hear? Somebody did something."));
		return topic;
	}

	private void addTestRegion()
	{
		ChatterData.Region region = new ChatterData.Region();
		region.id = "testland";
		region.boxes = Arrays.asList(new int[]{3400, 3400, 3500, 3500});
		data.regions = new ArrayList<>(data.regions);
		data.regions.add(region);
	}

	@Test
	public void regionalQuestsOnlyComeUpInTheirRegion()
	{
		addTestRegion();
		data.topics = new ArrayList<>(Arrays.asList(regionalTopic("local_news", false, 50)));
		LinePicker picker = new LinePicker(data, new Random(12));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		s.quests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);

		assertNull(pick(picker, s, "Hans", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0));
		LinePicker.Comment c = pick(picker, s, "Hans", new WorldPoint(3450, 3450, 0), ChattyNpcsConfig.Mood.RUDE, 1);
		// Gossip isn't aimed at you, so even Rude mood passes it on
		assertEquals(ChatterData.GOSSIP, c.getTone());
	}

	@Test
	public void bigNewsComesUpMoreInItsOwnRegion()
	{
		addTestRegion();
		data.topics = new ArrayList<>(Arrays.asList(regionalTopic("big_news", true, 30), regionalTopic("other_news", false, 30)));
		data.topics.get(1).quest.region = null;
		data.topics.get(1).quest.anywhere = true;
		LinePicker picker = new LinePicker(data, new Random(13));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		s.quests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);

		int home = 0;
		int away = 0;
		for (int i = 0; i < 400; i++)
		{
			if (pick(fresh(i), s, "Hans", new WorldPoint(3450, 3450, 0), ChattyNpcsConfig.Mood.BALANCED, i).getTopicId().equals("big_news"))
			{
				home++;
			}
			if (pick(fresh(i), s, "Hans", FALADOR, ChattyNpcsConfig.Mood.BALANCED, i).getTopicId().equals("big_news"))
			{
				away++;
			}
		}
		// Two equal topics: about 75% at home with the region boost, 50% away
		assertTrue("home " + home, home > 260);
		assertTrue("away " + away, away > 160 && away < 240);
	}

	private static final WorldPoint BURGH_DE_ROTT = new WorldPoint(3502, 3220, 0);

	@Test
	public void oldNewsGoesQuietOnceTheStoryMovesOn()
	{
		LinePicker picker = new LinePicker(data, new Random(14));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.NOT_STARTED);
		}
		s.quests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);
		assertTrue(picker.matchingTopics(s, BURGH_DE_ROTT).contains("sins_of_the_father_done"));

		s.quests.put(Quest.THE_BLOOD_MOON_RISES, QuestState.FINISHED);
		List<String> topics = picker.matchingTopics(s, BURGH_DE_ROTT);
		assertFalse(topics.contains("sins_of_the_father_done"));
		assertTrue(topics.contains("blood_moon_rises_local"));
	}

	@Test
	public void troubleIsOnlyGossipedAboutOnceItsStarted()
	{
		LinePicker picker = new LinePicker(data, new Random(15));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.NOT_STARTED);
		}
		assertFalse(picker.matchingTopics(s, BURGH_DE_ROTT).contains("blood_moon_rises_local_todo"));

		s.quests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);
		assertTrue(picker.matchingTopics(s, BURGH_DE_ROTT).contains("blood_moon_rises_local_todo"));
		// And it's Morytania's business, not Falador's
		assertFalse(picker.matchingTopics(s, FALADOR).contains("blood_moon_rises_local_todo"));
	}

	private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3164, 3488, 0);

	@Test
	public void theGrandExchangeHearsEverything()
	{
		LinePicker picker = new LinePicker(data, new Random(24));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.NOT_STARTED);
		}
		s.quests.put(Quest.VAMPYRE_SLAYER, QuestState.FINISHED);

		// Draynor news, normally only heard near Draynor
		assertFalse(picker.matchingTopics(s, FALADOR).contains("vampyre_slayer_done"));
		assertTrue(picker.matchingTopics(s, GRAND_EXCHANGE).contains("vampyre_slayer_done"));
	}

	@Test
	public void burghDeRottOnlyHearsMorytaniasNews()
	{
		LinePicker picker = new LinePicker(data, new Random(25));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.FINISHED);
		}

		List<String> topics = picker.matchingTopics(s, BURGH_DE_ROTT);
		assertTrue(topics.contains("blood_moon_rises_done"));
		assertFalse(topics.contains("dragon_slayer_ii_done"));
		assertFalse(topics.contains("song_of_the_elves_done"));
		assertEquals("burgher", picker.personaFor("Cornelius", BURGH_DE_ROTT).id);
		assertEquals("local", picker.personaFor("Cornelius", FALADOR).id);
	}

	private static final WorldPoint TESTLAND = new WorldPoint(3450, 3450, 0);

	private ChatterData.Speech testSpeech(String id)
	{
		addTestRegion();
		ChatterData.Speech speech = new ChatterData.Speech();
		speech.id = id;
		speech.regions = Arrays.asList("testland");
		// Ahead of the real ones, so it wins in testland
		data.speech = new ArrayList<>(data.speech);
		data.speech.add(0, speech);
		return speech;
	}

	private static ChatterData.Swap swap(String pattern, String replace)
	{
		ChatterData.Swap swap = new ChatterData.Swap();
		swap.pattern = pattern;
		swap.replace = replace;
		return swap;
	}

	private static ChatterData.Address address(String word, double append)
	{
		ChatterData.Address address = new ChatterData.Address();
		address.word = word;
		address.swap = 1;
		address.append = append;
		return address;
	}

	@Test
	public void accentsBendLinesWhereTheyreSpoken()
	{
		testSpeech("test").swaps = Arrays.asList(swap("\\byou\\b", "ye"));
		LinePicker picker = new LinePicker(data, new Random(26));
		assertEquals("Lovely cape. Shame about the rest of ye.", picker.translate("Lovely cape. Shame about the rest of you.", "Hans", TESTLAND, ChatterData.REMARK));
		assertEquals("Ye look well.", picker.translate("You look well.", "Hans", TESTLAND, ChatterData.REMARK));
		assertEquals("You look well.", picker.translate("You look well.", "Hans", FALADOR, ChatterData.REMARK));
	}

	@Test
	public void accentsUseGroupsAndNeverOverrunALine()
	{
		ChatterData.Speech speech = testSpeech("test");
		speech.swaps = Arrays.asList(swap("\\b(\\w+)ing\\b", "$1in'"));
		LinePicker picker = new LinePicker(data, new Random(26));
		assertEquals("Stop starin', I'm thinkin'.", picker.translate("Stop staring, I'm thinking.", "Hans", TESTLAND, ChatterData.REMARK));

		speech.swaps = Arrays.asList(swap("\\.$", ", and that's a fact, so it is, so help me Saradomin."));
		picker = new LinePicker(data, new Random(26));
		String line = "That's a very long line that is nearly at the limit already.";
		assertEquals(line, picker.translate(line, "Hans", TESTLAND, ChatterData.REMARK));
	}

	@Test
	public void gossipIsNeverAddressedToYou()
	{
		testSpeech("test").address = address("mortal", 1);
		LinePicker picker = new LinePicker(data, new Random(26));
		assertEquals("Nice cape, mortal.", picker.translate("Nice cape.", "Hans", TESTLAND, ChatterData.COMPLIMENT));
		assertEquals("The king's dead.", picker.translate("The king's dead.", "Hans", TESTLAND, ChatterData.GOSSIP));
	}

	@Test
	public void addressWordsReplaceHowYouAreCalled()
	{
		ChatterData.Speech speech = testSpeech("test");
		speech.address = address("outerlander", 0);
		LinePicker picker = new LinePicker(data, new Random(26));
		assertEquals("Hello, outerlander.", picker.translate("Hello, traveller.", "Hans", TESTLAND, ChatterData.REMARK));
		assertEquals("Thanks, outerlander!", picker.translate("Thanks, my friend!", "Hans", TESTLAND, ChatterData.REMARK));
		// Not a noun for everyone: "an outerlander" fine, but leave it to those who say so
		assertEquals("Ask an adventurer.", picker.translate("Ask an adventurer.", "Hans", TESTLAND, ChatterData.REMARK));
		speech.address.asNoun = true;
		assertEquals("Ask an outerlander.", picker.translate("Ask an adventurer.", "Hans", TESTLAND, ChatterData.REMARK));
		speech.address.word = "human";
		assertEquals("Ask a human.", picker.translate("Ask an adventurer.", "Hans", TESTLAND, ChatterData.REMARK));
	}

	@Test
	public void exclamationsCanBeSwappedOrDropped()
	{
		ChatterData.Speech speech = testSpeech("test");
		speech.exclaim = new ChatterData.Words();
		speech.exclaim.words = Arrays.asList("By Seren");
		LinePicker picker = new LinePicker(data, new Random(26));
		assertEquals("By Seren, that's a big hat.", picker.translate("Blimey, that's a big hat.", "Hans", TESTLAND, ChatterData.REMARK));
		speech.exclaim.words = Arrays.asList("");
		assertEquals("That's a big hat.", picker.translate("Blimey, that's a big hat.", "Hans", TESTLAND, ChatterData.REMARK));
		assertEquals("Big hat.", picker.translate("Big hat. Ye gods.", "Hans", TESTLAND, ChatterData.REMARK));
	}

	@Test
	public void formalSpeechSpellsOutContractions()
	{
		testSpeech("test").formal = 1;
		LinePicker picker = new LinePicker(data, new Random(26));
		assertEquals("I do not know. You cannot. They are late.", picker.translate("I don't know. You can't. They're late.", "Hans", TESTLAND, ChatterData.REMARK));
		assertEquals("Isn't it grand?", picker.translate("Isn't it grand?", "Hans", TESTLAND, ChatterData.REMARK));
	}

	@Test
	public void accentsLeavePersonaAndQuestGiverLinesAlone()
	{
		testSpeech("test").swaps = Arrays.asList(swap("\\bhat\\b", "bonnet"));
		ChatterData.Topic topic = oneLineTopic("hats", "kid/remark", "Nice hat!");
		topic.lines.put(ChatterData.REMARK, Arrays.asList("Nice hat."));
		data.topics = new ArrayList<>(Arrays.asList(topic));
		LinePicker picker = new LinePicker(data, new Random(27));
		for (int i = 0; i < 30; i++)
		{
			LinePicker.Comment c = pick(fresh(i), snapshot(), i % 2 == 0 ? "Street urchin" : "Hans", TESTLAND, ChattyNpcsConfig.Mood.BALANCED, i);
			assertTrue(c.getText(), c.getKey().startsWith("kid/") ? c.getText().contains("hat!") : c.getText().contains("bonnet"));
		}
	}

	@Test
	public void racesTalkTheirOwnWayEvenOnPersonaLinesAndAwayFromHome()
	{
		ChatterData.Speech place = testSpeech("place");
		place.swaps = Arrays.asList(swap("\\bhat\\b", "bonnet"));
		ChatterData.Speech race = new ChatterData.Speech();
		race.id = "race";
		race.npcNames = Arrays.asList("^Gnome");
		race.swaps = Arrays.asList(swap("\\bhat\\b", "cap"));
		data.speech.add(0, race);
		ChatterData.Topic topic = oneLineTopic("hats", "kid/remark", "Nice hat!");
		data.topics = new ArrayList<>(Arrays.asList(topic));
		LinePicker picker = new LinePicker(data, new Random(27));

		// Race before place, wherever they stand
		assertEquals("race", picker.speechFor("Gnome child", TESTLAND).id);
		assertEquals("race", picker.speechFor("Gnome child", FALADOR).id);
		assertEquals("place", picker.speechFor("Hans", TESTLAND).id);
		assertNull(picker.speechFor("Hans", FALADOR));
		assertTrue(pick(picker, snapshot(), "Gnome child", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getText().contains("Nice cap!"));
	}

	@Test
	public void babbleUnlessYouCanUnderstandThem()
	{
		LinePicker picker = new LinePicker(data, new Random(28));
		CharacterReader.Snapshot s = snapshot("bald");
		for (int i = 0; i < 20; i++)
		{
			String text = pick(picker, s, "Monkey", null, ChattyNpcsConfig.Mood.BALANCED, i * 1000L).getText();
			assertTrue(text, text.matches("((Ook|Ah|Uh)[.!]? ?)+"));
			assertTrue(text, text.chars().filter(c -> c == ' ').count() >= 7);
		}

		s.worn.put(EquipmentInventorySlot.AMULET, "M'speak amulet");
		assertFalse(pick(picker, s, "Monkey", null, ChattyNpcsConfig.Mood.BALANCED, 99000).getText().startsWith("Ook"));
		s.worn.put(EquipmentInventorySlot.AMULET, "Amulet of glory");
		s.worn.put(EquipmentInventorySlot.WEAPON, "Ninja monkey greegree");
		assertFalse(pick(picker, s, "Monkey", null, ChattyNpcsConfig.Mood.BALANCED, 99500).getText().startsWith("Ook"));
		// Having finished their story isn't enough: you have to be wearing something
		s.worn.clear();
		s.quests.put(Quest.MONKEY_MADNESS_II, QuestState.FINISHED);
		assertTrue(pick(picker, s, "Monkey", null, ChattyNpcsConfig.Mood.BALANCED, 99800).getText().matches("((Ook|Ah|Uh)[.!]? ?)+"));

		String ghost = pick(picker, s, "Ghost villager", null, ChattyNpcsConfig.Mood.BALANCED, 100000).getText();
		assertTrue(ghost, ghost.matches("[WwOoh .!]+"));
		s.worn.put(EquipmentInventorySlot.LEGS, "Morytania legs 3");
		assertFalse(pick(picker, s, "Ghost villager", null, ChattyNpcsConfig.Mood.BALANCED, 101000).getText().startsWith("Woo"));
	}

	@Test
	public void racesHaveSayingsOfTheirOwn()
	{
		LinePicker picker = new LinePicker(data, new Random(29));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		Set<String> topics = new HashSet<>();
		for (int i = 0; i < 50; i++)
		{
			LinePicker.Comment c = pick(picker, s, "TzHaar-Hur-Tel", null, ChattyNpcsConfig.Mood.BALANCED, i);
			if (c != null)
			{
				topics.add(c.getTopicId());
			}
		}
		assertTrue(topics.toString(), topics.contains("tzhaar_sayings"));
		assertNull(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 0));
	}

	@Test
	public void questNewsNeverDrownsOutEverythingElse()
	{
		// A well-quested adventurer in Port Phasmatys, with debug mode's short cooldowns
		LinePicker picker = new LinePicker(data, new Random(31));
		CharacterReader.Snapshot s = snapshot("high_combat");
		s.worn.put(EquipmentInventorySlot.CAPE, "Fire cape");
		s.worn.put(EquipmentInventorySlot.HEAD, "Neitiznot faceguard");
		s.worn.put(EquipmentInventorySlot.WEAPON, "Abyssal whip");
		s.worn.put(EquipmentInventorySlot.AMULET, "Ghostspeak amulet");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.FINISHED);
		}
		WorldPoint phasmatys = new WorldPoint(3680, 3490, 0);

		int news = 0;
		int spoken = 0;
		java.util.Deque<String> recent = new java.util.ArrayDeque<>();
		for (int i = 0; i < 300; i++)
		{
			LinePicker.Comment c = picker.pick(s, "Ghost villager", phasmatys, ChattyNpcsConfig.Mood.BALANCED, 5000, i * 5000L);
			if (c == null)
			{
				continue;
			}
			spoken++;
			if (data.topics.stream().anyMatch(t -> t.id.equals(c.getTopicId()) && t.quest != null))
			{
				news++;
			}
			String line = c.getText();
			assertFalse("repeated within a few lines: " + line, recent.contains(line));
			recent.addLast(line);
			if (recent.size() > 10)
			{
				recent.removeFirst();
			}
		}
		assertTrue("quest news was " + news + " of " + spoken, news < spoken * 0.45);
		assertTrue("only said " + spoken, spoken > 250);
	}

	@Test
	public void placeSayingsStayInTheirPlace()
	{
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		s.worn.put(EquipmentInventorySlot.AMULET, "Ghostspeak amulet");
		Set<String> inPort = new HashSet<>();
		Set<String> inLumbridge = new HashSet<>();
		for (int i = 0; i < 60; i++)
		{
			LinePicker.Comment port = pick(fresh(i), s, "Ghost villager", new WorldPoint(3680, 3490, 0), ChattyNpcsConfig.Mood.BALANCED, 0);
			LinePicker.Comment lumbridge = pick(fresh(i), s, "Restless ghost", LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.BALANCED, 0);
			inPort.add(port.getTopicId());
			inLumbridge.add(lumbridge.getTopicId());
		}
		assertTrue(inPort.toString(), inPort.contains("ghost_sayings_port_phasmatys"));
		assertFalse(inLumbridge.toString(), inLumbridge.contains("ghost_sayings_port_phasmatys"));
		assertTrue(inLumbridge.toString(), inLumbridge.contains("ghost_sayings"));
	}

	@Test
	public void placesWithTheirOwnVoiceOutrankARaceByName()
	{
		LinePicker picker = new LinePicker(data, new Random(32));
		// There's a Freygerd in Rellekka and another on Jatizso
		assertEquals("fremennik", picker.speechFor("Freygerd", new WorldPoint(2660, 3680, 0)).id);
		assertEquals("jatizso", picker.speechFor("Freygerd", new WorldPoint(2400, 3810, 0)).id);
		assertEquals("dwarf", picker.speechFor("Dwarf", FALADOR).id);
		assertEquals("lovakengj", picker.speechFor("Dwarf", new WorldPoint(1500, 3800, 0)).id);
		assertEquals("local", picker.personaFor("Freygerd", new WorldPoint(2400, 3810, 0)).id);
	}

	private static final WorldPoint VARROCK_WEST_BANK = new WorldPoint(3185, 3438, 0);
	private static final WorldPoint GRAND_TREE = new WorldPoint(2449, 3482, 1);

	@Test
	public void bankersGreetYouOncePerVisit()
	{
		LinePicker picker = new LinePicker(data, new Random(33));
		CharacterReader.Snapshot s = snapshot("bald");
		// Due the moment you arrive, so it needn't wait for cooldowns; never for anyone else
		assertTrue(picker.greetingDue("Banker", VARROCK_WEST_BANK, 0));
		assertFalse(picker.greetingDue("Hans", VARROCK_WEST_BANK, 0));
		String greeting = pick(picker, s, "Banker", VARROCK_WEST_BANK, ChattyNpcsConfig.Mood.BALANCED, 0).getText();
		assertFalse(picker.greetingDue("Banker", VARROCK_WEST_BANK, 60_000));
		assertTrue(greeting, greeting.matches(".*[Ww]elcome.*Varrock bank.*"));
		// Not again while you're still there, nor from the banker at the next booth
		for (int i = 1; i <= 10; i++)
		{
			assertFalse(pick(picker, s, "Banker", VARROCK_WEST_BANK, ChattyNpcsConfig.Mood.BALANCED, i * 60_000L).getText().contains("Varrock bank"));
		}
		// Standing there a long while without a word is still the same visit
		for (long t = 11 * 60_000L; t <= 50 * 60_000L; t += 60_000L)
		{
			picker.stillHere("Banker", VARROCK_WEST_BANK, t);
		}
		assertFalse(picker.greetingDue("Banker", VARROCK_WEST_BANK, 60 * 60_000L));
		// Back after a long while away, and they greet you again
		assertTrue(pick(picker, s, "Banker", VARROCK_WEST_BANK, ChattyNpcsConfig.Mood.BALANCED, 2 * 60 * 60_000L).getText().contains("Varrock bank"));
		assertTrue(picker.talksFreely("Banker", VARROCK_WEST_BANK));
		assertFalse(picker.talksFreely("Hans", VARROCK_WEST_BANK));
	}

	@Test
	public void bankersGreetYouInTheLocalTongue()
	{
		for (int i = 0; i < 10; i++)
		{
			String greeting = pick(fresh(i), snapshot(), "Gnome banker", GRAND_TREE, ChattyNpcsConfig.Mood.BALANCED, 0).getText();
			assertTrue(greeting, greeting.contains("Ta Quir Priw bank") && greeting.contains("human"));
		}
		// A ghost's welcome is still only "Woooo" without something to understand them by
		String ghost = pick(fresh(0), snapshot(), "Ghost banker", new WorldPoint(3688, 3467, 0), ChattyNpcsConfig.Mood.BALANCED, 0).getText();
		assertTrue(ghost, ghost.matches("[WwOoh .!]+"));
	}

	@Test
	public void bankersTalkLocalTroubleFirstThenNewsFromAfar()
	{
		CharacterReader.Snapshot s = snapshot("bald");
		int local = 0;
		int unfinished = 0;
		int global = 0;
		for (int i = 0; i < 200; i++)
		{
			LinePicker picker = fresh(i);
			for (Quest quest : picker.quests())
			{
				s.quests.put(quest, QuestState.FINISHED);
			}
			s.quests.put(Quest.COOKS_ASSISTANT, QuestState.NOT_STARTED);
			pick(picker, s, "Banker", LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.BALANCED, 0);
			LinePicker.Comment c = pick(picker, s, "Banker", LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.BALANCED, 1);
			ChatterData.Topic topic = data.topics.stream().filter(t -> t.id.equals(c.getTopicId())).findFirst().get();
			assertNotNull(c.getTopicId(), topic.quest);
			if (topic.quest.anywhere)
			{
				global++;
			}
			else
			{
				// The one thing still unsolved round here comes up most
				unfinished += topic.id.equals("cooks_assistant_todo") ? 1 : 0;
				local++;
			}
		}
		assertTrue(local + " local, " + global + " global", local > 70 && global > 70);
		assertTrue(unfinished + " of " + local, unfinished > local / 2);
	}

	/**
	 * pet_names.txt is every follower's in-game name, from the Wiki's pet pages
	 */
	@Test
	public void everyPetInTheGameGetsNoticed() throws Exception
	{
		List<Pattern> pets = new ArrayList<>();
		for (ChatterData.Topic topic : data.topics)
		{
			if (topic.pet != null)
			{
				pets.add(Pattern.compile(topic.pet.pattern, Pattern.CASE_INSENSITIVE));
			}
		}
		try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(
			getClass().getResourceAsStream("pet_names.txt"), java.nio.charset.StandardCharsets.UTF_8)))
		{
			String name;
			while ((name = in.readLine()) != null)
			{
				String pet = name.trim();
				if (!pet.isEmpty())
				{
					assertTrue(pet + " has nothing said about it", pets.stream().anyMatch(p -> p.matcher(pet).find()));
				}
			}
		}
	}

	@Test
	public void bankersKnowTheirWholeRegionsNewsButNoMore()
	{
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		Set<String> varrock = new HashSet<>();
		Set<String> exchange = new HashSet<>();
		for (int i = 0; i < 150; i++)
		{
			for (Quest quest : fresh(0).quests())
			{
				s.quests.put(quest, QuestState.NOT_STARTED);
			}
			LinePicker picker = fresh(i);
			pick(picker, s, "Banker", VARROCK_WEST_BANK, ChattyNpcsConfig.Mood.BALANCED, 0);
			varrock.add(pick(picker, s, "Banker", VARROCK_WEST_BANK, ChattyNpcsConfig.Mood.BALANCED, 1).getTopicId());
			picker = fresh(i);
			pick(picker, s, "Grand Exchange Clerk", GRAND_EXCHANGE, ChattyNpcsConfig.Mood.BALANCED, 0);
			exchange.add(pick(picker, s, "Grand Exchange Clerk", GRAND_EXCHANGE, ChattyNpcsConfig.Mood.BALANCED, 1).getTopicId());
		}
		// Reldo's trouble starts well beyond earshot of the west bank, but it's Varrock's news
		assertTrue(varrock.toString(), varrock.contains("shield_of_arrav_todo"));
		// The Grand Exchange hears everything, so news from all over still turns up
		assertTrue(exchange.toString(), exchange.contains("cooks_assistant_todo") || exchange.contains("dorics_quest_todo"));
	}

	@Test
	public void outsidersKeepTheirOwnVoice()
	{
		LinePicker picker = new LinePicker(data, new Random(34));
		assertEquals("gnome", picker.speechFor("Bolongo", GRAND_TREE).id);
		// A human slayer master in the Stronghold, and a troll in Rellekka who talks like a troll
		assertNull(picker.speechFor("Nieve", GRAND_TREE));
		assertEquals("troll", picker.speechFor("Lalli", new WorldPoint(2660, 3680, 0)).id);
		// A dwarf in Kourend keeps a dwarf's voice
		assertEquals("dwarf", picker.speechFor("Kaston", new WorldPoint(1500, 3600, 0)).id);
		// Lumbridge's Sigmund is no Fremennik
		assertNull(picker.speechFor("Sigmund", LUMBRIDGE_CASTLE));
	}

	@Test
	public void settingsTurnFlavourAndGossipDown()
	{
		CharacterReader.Snapshot s = snapshot("bald");
		WorldPoint mine = new WorldPoint(2480, 5170, 0);
		for (int i = 0; i < 40; i++)
		{
			// No local flavour: no sayings, and a dwarf speaks plainly...
			LinePicker picker = fresh(i);
			picker.tune(1.0 / 3, 0, true);
			LinePicker.Comment c = pick(picker, s, "Dwarf", new WorldPoint(2850, 10200, 0), ChattyNpcsConfig.Mood.BALANCED, 0);
			assertTrue(c == null || !(c.getTopicId().contains("_sayings") || c.getText().contains("By Guthix") || c.getKey().startsWith("dwarf/")));
			// ...but a TzHaar has no plain English to fall back on
			picker = fresh(i);
			picker.tune(1.0 / 3, 0, true);
			c = pick(picker, s, "TzHaar-Hur-Tel", mine, ChattyNpcsConfig.Mood.BALANCED, 0);
			assertTrue(c == null || c.getKey().startsWith("tzhaar/"));

			// No quest gossip while there's anything else to say
			picker = fresh(i);
			picker.tune(0, 1, true);
			CharacterReader.Snapshot quested = snapshot("bald");
			for (Quest quest : picker.quests())
			{
				quested.quests.put(quest, QuestState.FINISHED);
			}
			String topic = pick(picker, quested, "Hans", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getTopicId();
			assertTrue(topic, topic.equals("bald") || topic.equals("small_talk"));
		}
	}

	@Test
	public void everyMonkeyOfMarimNeedsTheAmulet()
	{
		LinePicker picker = new LinePicker(data, new Random(35));
		WorldPoint marim = new WorldPoint(2770, 2780, 0);
		String[] monkeys = {"Aberab", "Awowogei", "Bobawu", "Bonzara", "Daga", "Denadu", "Dugopul", "Duke", "Elder Guard",
			"Hafuba", "Hamab", "Ifaba", "Iwazaru", "Jumaane", "Kikazaru", "Kruk", "Lofu", "Mizaru", "Monkey Archer",
			"Monkey Child", "Monkey Guard", "Muruwoi", "Oipuis", "Oobapohk", "Ouhai", "Padulah", "Sleeping Monkey",
			"Solihib", "The Monkey's Aunt", "The Monkey's Uncle", "Trefaji", "Tutab", "Uodai", "Uwogo", "Uyoro",
			"Some monkey nobody has named yet"};
		for (String monkey : monkeys)
		{
			assertEquals(monkey, "monkey", picker.speechFor(monkey, monkey.startsWith("Some") ? marim : FALADOR).id);
		}
		// The gnomes of the 10th squad are no monkeys
		for (String gnome : new String[]{"Garkor", "Lumo", "Bunkdo", "Bunkwicket", "Carado", "Karam", "Waymottin", "Zooknock", "Captain Shoracks"})
		{
			assertEquals(gnome, "gnome", picker.speechFor(gnome, marim).id);
		}
	}

	@Test
	public void slayerMastersTalkAboutYourTask()
	{
		CharacterReader.Snapshot s = snapshot("slayer_task");
		s.vars.put("task", "abyssal demons");
		WorldPoint edgeville = new WorldPoint(3094, 3480, 0);
		boolean mentioned = false;
		for (int i = 0; i < 30; i++)
		{
			LinePicker.Comment c = pick(fresh(i), s, "Vannaka", edgeville, ChattyNpcsConfig.Mood.BALANCED, 0);
			assertEquals("slayer_master", c.getPersona());
			mentioned |= c.getText().contains("abyssal demons");
		}
		assertTrue(mentioned);
		// Nobody else knows what you've been told to kill
		for (int i = 0; i < 30; i++)
		{
			assertFalse(pick(fresh(i), s, "Hans", edgeville, ChattyNpcsConfig.Mood.BALANCED, 0).getText().contains("abyssal demons"));
		}
		assertEquals("TzHaar", CharacterReader.inSentence("TzHaar"));
		assertEquals("abyssal demons", CharacterReader.inSentence("Abyssal demons"));
	}

	@Test
	public void theSeamsHold()
	{
		WorldPoint rellekka = new WorldPoint(2660, 3680, 0);
		CharacterReader.Snapshot s = snapshot("bald");
		s.vars.put("kin", "sister");
		s.quests.put(Quest.THE_FREMENNIK_TRIALS, QuestState.FINISHED);
		for (int i = 0; i < 60; i++)
		{
			// After the trials, nobody calls you outerlander
			String said = pick(fresh(i), s, "Brundt the Chieftain", rellekka, ChattyNpcsConfig.Mood.BALANCED, 0).getText();
			assertFalse(said, said.contains("outerlander"));
			// Thurgo never gossips about Thurgo
			assertFalse(pick(fresh(i), s, "Thurgo", rellekka, ChattyNpcsConfig.Mood.BALANCED, 0).getText().contains("Thurgo"));
		}

		// Never both an opener and a closer
		ChatterData.Topic topic = oneLineTopic("plain", ChatterData.REMARK, "Nice day.");
		data.topics = new ArrayList<>(Arrays.asList(topic));
		for (int i = 0; i < 60; i++)
		{
			String said = pick(fresh(i), snapshot(), "Granny", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getText();
			assertFalse(said, said.matches(".+ Nice day\\. .+"));
		}

		// Heartland lines only from plain speakers above ground
		topic.lines = new java.util.HashMap<>();
		topic.lines.put("heartland/remark", Arrays.asList("The sun's out."));
		topic.lines.put(ChatterData.REMARK, Arrays.asList("Nice day."));
		topic.wrap = false;
		boolean heardSun = false;
		for (int i = 0; i < 40; i++)
		{
			heardSun |= pick(fresh(i), snapshot(), "Hans", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getText().contains("sun");
			assertFalse(pick(fresh(i), snapshot(), "Dwarf", new WorldPoint(2850, 10200, 0), ChattyNpcsConfig.Mood.BALANCED, 0).getText().contains("sun"));
			assertFalse(pick(fresh(i), snapshot(), "Man", new WorldPoint(3620, 3360, 0), ChattyNpcsConfig.Mood.BALANCED, 0).getText().contains("sun"));
		}
		assertTrue(heardSun);
	}

	@Test
	public void goblinsOnlyEverSoundLikeGoblins()
	{
		CharacterReader.Snapshot s = snapshot("bald", "skint");
		s.worn.put(EquipmentInventorySlot.BODY, "Rune platebody");
		for (Quest quest : fresh(0).quests())
		{
			s.quests.put(quest, QuestState.FINISHED);
		}
		for (int i = 0; i < 80; i++)
		{
			LinePicker.Comment c = pick(fresh(i), s, "Goblin", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0);
			if (c != null)
			{
				assertTrue(c.getKey() + ": " + c.getText(), c.getKey().startsWith("goblin/") || c.getTopicId().startsWith("goblin_sayings"));
			}
		}
		assertEquals("goblin", fresh(0).personaFor("Goblin guard").id);
		for (int i = 0; i < 60; i++)
		{
			LinePicker.Comment c = pick(fresh(i), s, "Burntmeat", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0);
			assertTrue(c == null || c.getKey().startsWith("troll/") || c.getTopicId().startsWith("troll_sayings"));
		}
		assertEquals("dorgeshuun", fresh(0).speechFor("Cave goblin miner", FALADOR).id);
	}

	@Test
	public void monkeysNoticeYourGreegreeNotYourArmour()
	{
		WorldPoint marim = new WorldPoint(2770, 2780, 0);
		CharacterReader.Snapshot s = snapshot("bald");
		s.worn.put(EquipmentInventorySlot.BODY, "Rune platebody");
		s.worn.put(EquipmentInventorySlot.WEAPON, "Ninja monkey greegree (small)");
		boolean greegree = false;
		for (int i = 0; i < 40; i++)
		{
			for (String monkey : new String[]{"Monkey", "Monkey Child", "Monkey Guard", "Hafuba"})
			{
				LinePicker.Comment c = pick(fresh(i), s, monkey, marim, ChattyNpcsConfig.Mood.BALANCED, 0);
				if (c == null)
				{
					continue;
				}
				assertEquals("monkey", c.getPersona());
				// Understood, since a small greegree counts, and never about the armour
				assertFalse(c.getText(), c.getText().startsWith("Ook Ah") || c.getTopicId().equals("rune_gear") || c.getTopicId().equals("bald"));
				greegree |= c.getTopicId().equals("greegree_ninja");
			}
		}
		assertTrue(greegree);
	}

	@Test
	public void catsCamelsAndMosPiratesNeedTheRightThing()
	{
		CharacterReader.Snapshot s = snapshot("bald");
		WorldPoint mos = new WorldPoint(3680, 2963, 0);
		for (int i = 0; i < 20; i++)
		{
			String cat = pick(fresh(i), s, "Neite", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getText();
			assertTrue(cat, cat.matches("[A-Za-z .,!]*") && cat.toLowerCase().matches(".*(m[eia]+o*w|mew|purr|hiss).*"));
			LinePicker.Comment pirate = pick(fresh(i), s, "Pirate", mos, ChattyNpcsConfig.Mood.BALANCED, 0);
			assertTrue(pirate == null || pirate.getPersona().equals("mos_pirate"));
		}
		s.worn.put(EquipmentInventorySlot.AMULET, "Catspeak amulet (e)");
		s.carried.add("Book o' piracy");
		boolean understood = false;
		for (int i = 0; i < 20; i++)
		{
			understood |= !pick(fresh(i), s, "Neite", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getText().toLowerCase().contains("meow");
		}
		assertTrue(understood);
		assertEquals("banker", fresh(0).personaFor("Banker", mos).id);
	}

	@Test
	public void racePersonasComeFromSpeech()
	{
		LinePicker picker = new LinePicker(data, new Random(30));
		assertEquals("gnome", picker.personaFor("Hudo").id);
		assertEquals("dwarf", picker.personaFor("Thurgo").id);
		assertEquals("fremennik", picker.personaFor("Sigli the Huntsman").id);
		assertEquals("kid", picker.personaFor("Gnome child").id);
	}

	@Test
	public void grandmastersComeUpAnywhere()
	{
		LinePicker picker = new LinePicker(data, new Random(9));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		s.quests.put(Quest.DRAGON_SLAYER_II, QuestState.FINISHED);

		assertEquals("dragon_slayer_ii_done", pick(picker, s, "Hans", FALADOR, ChattyNpcsConfig.Mood.BALANCED, 0).getTopicId());
	}

	@Test
	public void repeatsTheOldestLineWhenEverythingsBeenSaidButNeverJustNow()
	{
		ChatterData.Topic topic = new ChatterData.Topic();
		topic.id = "only_six";
		topic.fact = "always";
		topic.wrap = false;
		topic.lines = new java.util.HashMap<>();
		topic.lines.put(ChatterData.REMARK, Arrays.asList("One.", "Two.", "Three.", "Four.", "Five.", "Six."));
		data.topics = new ArrayList<>(Arrays.asList(topic));
		LinePicker picker = new LinePicker(data, new Random(16));
		CharacterReader.Snapshot s = snapshot();

		List<String> said = new ArrayList<>();
		for (int i = 0; i < 6; i++)
		{
			said.add(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, i).getText());
		}
		assertEquals(6, new HashSet<>(said).size());
		// All six have been said now, so it goes back to the one said longest ago
		assertEquals(said.get(0), pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 6).getText());

		// With only two lines, it keeps quiet rather than repeat one of the last few
		topic.lines.put(ChatterData.REMARK, Arrays.asList("First.", "Second."));
		picker = new LinePicker(data, new Random(16));
		assertNotNull(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 0));
		assertNotNull(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 1));
		assertNull(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 2));
	}

	@Test
	public void regionsHandleReversedCornersAndSeveralBoxes()
	{
		ChatterData.Region region = new ChatterData.Region();
		region.boxes = Arrays.asList(new int[]{20, 20, 10, 10}, new int[]{100, 100, 110, 110});
		assertTrue(region.contains(15, 15));
		assertTrue(region.contains(105, 100));
		assertFalse(region.contains(50, 50));
		assertTrue(region.contains(10, 20));
	}

	@Test(expected = IllegalArgumentException.class)
	public void itemTopicsNeedAMinCountOfAtLeastOne()
	{
		data.topics.stream().filter(t -> t.item != null).findFirst().get().item.minCount = 0;
		new LinePicker(data, new Random(17));
	}

	@Test
	public void unknownPlaceholdersMakeALineUnusable()
	{
		assertNull(LinePicker.fill("Nice {hat}.", new java.util.HashMap<>()));
		assertEquals("Nice cape.", LinePicker.fill("Nice {item}.", java.util.Collections.singletonMap("item", "cape")));
	}

	@Test
	public void strangersGetTheLocalVoice()
	{
		assertEquals("local", new LinePicker(data, new Random(18)).personaFor("Somebody Nobody Has Heard Of").id);
	}

	@Test
	public void speechBubblesLastLongEnoughToRead()
	{
		// Client cycles are 20ms: 9 seconds at least, 18 at most
		assertEquals(450 + 6 * 10, ChattyNpcsPlugin.overheadCycles("0123456789"));
		assertEquals(900, ChattyNpcsPlugin.overheadCycles(new String(new char[200])));
	}

	@Test
	public void gossipAndQuestGiversAreNeverWrapped()
	{
		LinePicker picker = new LinePicker(data, new Random(20));
		CharacterReader.Snapshot s = snapshot();
		s.facts.remove("always");
		for (Quest quest : picker.quests())
		{
			s.quests.put(quest, QuestState.NOT_STARTED);
		}
		s.quests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);
		s.quests.put(Quest.THE_BLOOD_MOON_RISES, QuestState.FINISHED);

		for (int i = 0; i < 200; i++)
		{
			LinePicker.Comment c = pick(picker, s, i % 2 == 0 ? "Banker" : "Cook", i % 2 == 0 ? BURGH_DE_ROTT : LUMBRIDGE_CASTLE, ChattyNpcsConfig.Mood.BALANCED, i);
			if (c == null)
			{
				continue;
			}
			if (c.getTone().equals(ChatterData.GOSSIP) || c.getKey().startsWith("starter/"))
			{
				String raw = data.topics.stream().filter(t -> t.id.equals(c.getTopicId())).findFirst().get().lines.get(c.getKey())
					.stream().filter(c.getText()::equals).findFirst().orElse(null);
				assertNotNull("wrapped: " + c.getText(), raw);
			}
		}
	}

	private static ChatterData.Topic oneLineTopic(String id, String key, String line)
	{
		ChatterData.Topic topic = new ChatterData.Topic();
		topic.id = id;
		topic.fact = "always";
		topic.lines = new java.util.HashMap<>();
		topic.lines.put(key, Arrays.asList(line));
		return topic;
	}

	@Test
	public void topicsCanOptOutOfWrapping()
	{
		ChatterData.Topic topic = oneLineTopic("grave", ChatterData.REMARK, "We buried what came out.");
		topic.wrap = false;
		data.topics = new ArrayList<>(Arrays.asList(topic));
		LinePicker picker = new LinePicker(data, new Random(21));
		for (int i = 0; i < 50; i++)
		{
			assertEquals("We buried what came out.", pick(fresh(i), snapshot(), "Granny", null, ChattyNpcsConfig.Mood.BALANCED, i).getText());
		}
	}

	@Test
	public void plainPersonasGetPlainButNotSharpLines()
	{
		data.topics = new ArrayList<>(Arrays.asList(
			oneLineTopic("plain_one", "plain/insult", "Plain jab."),
			oneLineTopic("sharp_one", "sharp/insult", "Sharp jab.")));
		LinePicker picker = new LinePicker(data, new Random(22));
		boolean sawPlain = false;
		for (int i = 0; i < 50; i++)
		{
			LinePicker.Comment c = pick(fresh(i), snapshot(), "Hans", null, ChattyNpcsConfig.Mood.RUDE, i);
			assertNotNull(c);
			assertFalse(c.getText(), c.getText().contains("Sharp jab."));
			sawPlain |= c.getText().contains("Plain jab.");
		}
		assertTrue(sawPlain);
	}

	@Test
	public void guardsShareGossipButNothingElse()
	{
		ChatterData.Topic news = oneLineTopic("news", ChatterData.GOSSIP, "Big news in town.");
		news.lines.put(ChatterData.INSULT, Arrays.asList("Shared jab."));
		data.topics = new ArrayList<>(Arrays.asList(news));
		LinePicker picker = new LinePicker(data, new Random(23));
		for (int i = 0; i < 50; i++)
		{
			LinePicker.Comment c = pick(fresh(i), snapshot(), "Guard", null, ChattyNpcsConfig.Mood.BALANCED, i);
			assertEquals("Big news in town.", c.getText());
		}
	}

	@Test
	public void openersAndClosersDontRepeatTheLine()
	{
		assertTrue(LinePicker.saysAlready("Good day. How may I help you?", "Good day."));
		assertTrue(LinePicker.saysAlready("Rats in the cellar. Pint?", "Pint?"));
		assertFalse(LinePicker.saysAlready("Lovely cape.", "Next, please."));
		assertTrue(LinePicker.saysAlready("You fight like an outerlander.", "Hail, outerlander."));
		assertTrue(LinePicker.saysAlready("Not even a hat, human?", "Hello, human!"));
		assertTrue(LinePicker.saysAlready("Put a hat on, dear.", "Wrap up warm, dear."));
		assertFalse(LinePicker.saysAlready("Endearing, that.", "Wrap up warm, dear."));
	}

	@Test
	public void petsAreNoticedByName()
	{
		ChatterData.Topic topic = new ChatterData.Topic();
		topic.id = "pet_test";
		topic.pet = new ChatterData.PetRule();
		topic.pet.pattern = "^(Rock golem|Rocky)$";
		topic.lines = new java.util.HashMap<>();
		topic.lines.put(ChatterData.REMARK, Arrays.asList("Your rock's following you."));
		data.topics = new ArrayList<>(Arrays.asList(topic));
		LinePicker picker = new LinePicker(data, new Random(19));

		CharacterReader.Snapshot s = snapshot();
		assertNull(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 0));
		s.pet = "Rock golem";
		assertEquals("pet_test", pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 1).getTopicId());
		s.pet = "Rock golemite";
		assertNull(pick(picker, s, "Hans", null, ChattyNpcsConfig.Mood.BALANCED, 2));
	}

	@Test
	public void itemNamesReadNaturally()
	{
		assertEquals("dragon scimitar", LinePicker.inSentence("Dragon scimitar"));
		assertEquals("Ahrim's hood", LinePicker.inSentence("Ahrim's hood"));
	}
}
