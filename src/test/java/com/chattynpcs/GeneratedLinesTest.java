package com.chattynpcs;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Lines from the generator in linegen/ are held to the same rules as the bank, and to two more:
 * every claim about the world cites a fact the speaker could know, and nothing is said that the
 * player hasn't reached yet.
 *
 * Each folder in linegen/ with a cells.json is a batch: the pilot, the named characters. For each,
 * this also writes build/linegen/(batch)/baseline.tsv, what the plugin says in each cell today, and
 * lint.tsv, which candidates pass.
 */
public class GeneratedLinesTest
{
	private static final Path LINEGEN = Paths.get("linegen");
	private static final Path LORE = Paths.get("linegen", "lore");
	private static final Path OUT = Paths.get("build", "linegen");
	private static final Set<String> FOOTINGS = new LinkedHashSet<>(Arrays.asList("to", "aside", "gossip", "self", "pet"));
	private static final Pattern MOVE = Pattern.compile("M([1-9]|10)");
	private static final Pattern YOU = Pattern.compile("(?i)\\byou(r|rs|rself)?\\b");
	private static final Pattern SHOUTING = Pattern.compile(".*\\b[A-Z]{2,}\\b.*");

	static class Voice
	{
		String npc;
		int x;
		int y;
		int plane;
		/**
		 * The speech.json voice they talk in, if any
		 */
		String speech;
	}

	static class Cell
	{
		String id;
		String voice;
		String topic;
		String tone;
		Map<String, String> worn = Collections.emptyMap();
		String pet;
		List<String> facts = Collections.emptyList();
		/**
		 * The player's progress this cell assumes, the only quests its lines may speak of
		 */
		Map<String, String> quests = Collections.emptyMap();
	}

	static class Cells
	{
		Map<String, Voice> voices = Collections.emptyMap();
		List<Cell> cells = Collections.emptyList();
	}

	static class Fact
	{
		String id;
		String says;
		String source;
		String canon;
		/**
		 * Region ids from regions.json where people would know this, or "everywhere"
		 */
		List<String> knownIn = Collections.emptyList();
		/**
		 * Only sayable once the player has got this far, so nobody spoils a quest
		 */
		Gate gate;
		/**
		 * More gates, all of which must hold too: Glough's escape gives away how his first story
		 * ended as well
		 */
		List<Gate> gates = Collections.emptyList();

		List<Gate> allGates()
		{
			List<Gate> all = new ArrayList<>(gates);
			if (gate != null)
			{
				all.add(gate);
			}
			return all;
		}
	}

	static class Gate
	{
		String quest;
		List<String> states = Collections.emptyList();
	}

	static class Lore
	{
		List<Fact> facts = Collections.emptyList();
	}

	/**
	 * One folder of cells, and the lines written for them
	 */
	static class Run
	{
		String name;
		Path dir;
		Cells cells;
		final Map<String, Cell> byId = new HashMap<>();
	}

	static class Line
	{
		String id;
		String cell;
		String line;
		String move;
		String footing;
		List<String> facts = Collections.emptyList();
	}

	private final Gson gson = new Gson();
	private ChatterData data;
	private LineRules rules;
	private final List<Run> runs = new ArrayList<>();
	private final Map<String, Fact> facts = new HashMap<>();

	@Before
	public void before() throws Exception
	{
		data = ChatterData.load(gson);
		rules = LineRules.load(data);
		try (DirectoryStream<Path> dirs = Files.newDirectoryStream(LINEGEN, Files::isDirectory))
		{
			for (Path dir : dirs)
			{
				if (!Files.exists(dir.resolve("cells.json")))
				{
					continue;
				}
				Run run = new Run();
				run.name = dir.getFileName().toString();
				run.dir = dir;
				try (Reader in = Files.newBufferedReader(dir.resolve("cells.json"), StandardCharsets.UTF_8))
				{
					run.cells = gson.fromJson(in, Cells.class);
				}
				run.cells.cells.forEach(c -> run.byId.put(c.id, c));
				runs.add(run);
			}
		}
		if (Files.isDirectory(LORE))
		{
			try (DirectoryStream<Path> files = Files.newDirectoryStream(LORE, "*.json"))
			{
				for (Path file : files)
				{
					try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8))
					{
						for (Fact fact : gson.fromJson(in, Lore.class).facts)
						{
							assertTrue(file + ": " + fact.id + " is in two lore files", facts.put(fact.id, fact) == null);
						}
					}
				}
			}
		}
	}

	private List<Line> read(Run run, String file) throws IOException
	{
		Path path = run.dir.resolve(file);
		List<Line> lines = new ArrayList<>();
		if (Files.exists(path))
		{
			for (String json : Files.readAllLines(path, StandardCharsets.UTF_8))
			{
				if (!json.trim().isEmpty())
				{
					lines.add(gson.fromJson(json, Line.class));
				}
			}
		}
		return lines;
	}

	/**
	 * Everything wrong with a generated line, or nothing
	 */
	private List<String> problems(Run run, Line l)
	{
		List<String> problems = new ArrayList<>();
		Cell cell = run.byId.get(l.cell);
		if (cell == null)
		{
			problems.add("no cell " + l.cell);
			return problems;
		}
		String line = l.line;
		if (line.length() > 70)
		{
			problems.add("too long for a speech bubble");
		}
		if (line.matches(".*\\d.*"))
		{
			problems.add("has a number in it");
		}
		if (line.contains("{"))
		{
			problems.add("has a placeholder");
		}
		if (LineRules.BANNED.matcher(line).find())
		{
			problems.add("uses a banned word");
		}
		if (LineRules.AI_TELLS.matcher(line).find())
		{
			problems.add("has a machine's tell");
		}
		if (SHOUTING.matcher(line.replace("HAM", "")).matches())
		{
			problems.add("shouts in capitals");
		}
		for (String name : rules.unknownNames(line))
		{
			problems.add("names " + name + ", which the game doesn't have");
		}
		String quest = rules.namedQuest(line);
		if (quest != null)
		{
			problems.add("names a quest");
		}

		if (l.move == null || !MOVE.matcher(l.move).matches())
		{
			problems.add("no move");
		}
		if (!FOOTINGS.contains(l.footing))
		{
			problems.add("no footing");
		}
		else
		{
			if (ChatterData.GOSSIP.equals(cell.tone) != "gossip".equals(l.footing))
			{
				problems.add("gossip is overheard, and only gossip");
			}
			if (("aside".equals(l.footing) || "self".equals(l.footing)) && YOU.matcher(line).find())
			{
				problems.add("talks to you in a line not said to you");
			}
			String address = address(run, cell);
			// "Humans" is fine in gossip; "human" as what they call you is only for you
			if (address != null && !"to".equals(l.footing) && Pattern.compile("(?i)\\b" + Pattern.quote(address) + "\\b").matcher(line).find())
			{
				problems.add("calls you " + address + " in a line not said to you");
			}
		}

		Voice voice = run.cells.voices.get(cell.voice);
		for (String id : l.facts)
		{
			Fact fact = facts.get(id);
			if (fact == null)
			{
				problems.add("cites " + id + ", which isn't in linegen/lore");
				continue;
			}
			// Everyone knows their own life, wherever they stand
			if (!id.startsWith("char." + cell.voice + ".") && !knownAt(fact, voice))
			{
				problems.add("cites " + id + ", which nobody here would know");
			}
			for (Gate gate : fact.allGates())
			{
				String state = cell.quests.get(gate.quest);
				if (state == null || !gate.states.contains(state))
				{
					problems.add("cites " + id + " before the player has got that far in " + gate.quest);
				}
			}
		}
		return problems;
	}

	private String address(Run run, Cell cell)
	{
		String speech = run.cells.voices.get(cell.voice).speech;
		for (ChatterData.Speech s : data.speech)
		{
			if (s.id.equals(speech) && s.address != null && s.address.word != null)
			{
				return s.address.word.toLowerCase(Locale.ROOT);
			}
		}
		return null;
	}

	private boolean knownAt(Fact fact, Voice voice)
	{
		for (String place : fact.knownIn)
		{
			if (place.equals("everywhere"))
			{
				return true;
			}
			for (ChatterData.Region region : data.regions)
			{
				if (region.id.equals(place) && region.contains(voice.x, voice.y))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void theLoreHoldsTogether()
	{
		List<String> problems = new ArrayList<>();
		Set<String> regions = new LinkedHashSet<>();
		data.regions.forEach(r -> regions.add(r.id));
		for (Fact fact : facts.values())
		{
			if (fact.says == null || fact.source == null || !fact.source.startsWith("https://oldschool.runescape.wiki/"))
			{
				problems.add(fact.id + " needs what it says and the Wiki page it comes from");
			}
			if (fact.knownIn.isEmpty())
			{
				problems.add(fact.id + " needs knownIn");
			}
			for (String place : fact.knownIn)
			{
				if (!place.equals("everywhere") && !regions.contains(place))
				{
					problems.add(fact.id + ": no region " + place);
				}
			}
			for (Gate gate : fact.allGates())
			{
				try
				{
					Quest.valueOf(gate.quest);
					gate.states.forEach(QuestState::valueOf);
				}
				catch (IllegalArgumentException e)
				{
					problems.add(fact.id + ": bad gate, " + e.getMessage());
				}
			}
		}
		for (Run run : runs)
		{
			for (Cell cell : run.cells.cells)
			{
				if (!run.cells.voices.containsKey(cell.voice))
				{
					problems.add(run.name + ": " + cell.id + " has no voice " + cell.voice);
				}
				cell.quests.forEach((q, s) ->
				{
					Quest.valueOf(q);
					QuestState.valueOf(s);
				});
			}
		}
		assertTrue(String.join("\n", problems), problems.isEmpty());
	}

	/**
	 * Anything that made it into accepted.jsonl has to keep passing, however the rules change
	 */
	@Test
	public void acceptedLinesHoldUp() throws IOException
	{
		List<String> failures = new ArrayList<>();
		for (Run run : runs)
		{
			for (Line line : read(run, "accepted.jsonl"))
			{
				for (String problem : problems(run, line))
				{
					failures.add(run.name + "/" + line.id + " " + problem + ": " + line.line);
				}
			}
		}
		assertTrue(String.join("\n", failures), failures.isEmpty());
	}

	/**
	 * Not a check: marks which candidates pass, for the pipeline to read
	 */
	@Test
	public void lintCandidates() throws IOException
	{
		for (Run run : runs)
		{
			StringBuilder out = new StringBuilder("id\tok\tproblems\n");
			for (Line line : read(run, "candidates.jsonl"))
			{
				List<String> problems = problems(run, line);
				out.append(line.id).append('\t').append(problems.isEmpty()).append('\t').append(String.join("; ", problems)).append('\n');
			}
			Files.createDirectories(OUT.resolve(run.name));
			Files.write(OUT.resolve(run.name).resolve("lint.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * Not a check: what the plugin says in each cell today, as written and as said, to put the
	 * generated lines up against. A cell for a topic the bank doesn't have yet, like a named
	 * character's own, gets whatever that speaker says now.
	 */
	@Test
	public void baseline() throws IOException
	{
		Set<String> topics = new LinkedHashSet<>();
		data.topics.forEach(t -> topics.add(t.id));
		for (Run run : runs)
		{
			StringBuilder out = new StringBuilder("cell\twritten\tsaid\n");
			for (Cell cell : run.cells.cells)
			{
				Voice voice = run.cells.voices.get(cell.voice);
				CharacterReader.Snapshot s = new CharacterReader.Snapshot();
				s.facts.add("always");
				s.facts.addAll(cell.facts);
				cell.worn.forEach((slot, item) -> s.worn.put(EquipmentInventorySlot.valueOf(slot), item));
				s.pet = cell.pet;
				cell.quests.forEach((q, state) -> s.quests.put(Quest.valueOf(q), QuestState.valueOf(state)));
				boolean anything = !topics.contains(cell.topic);

				Map<String, String> heard = new LinkedHashMap<>();
				for (int i = 0; i < 400; i++)
				{
					LinePicker picker = new LinePicker(data, new Random(new Random(i).nextLong()));
					LinePicker.Comment c = picker.pick(s, voice.npc, new WorldPoint(voice.x, voice.y, voice.plane), ChattyNpcsConfig.Mood.BALANCED, 0, 0);
					if (c != null && (anything || cell.topic.equals(c.getTopicId()) && cell.tone.equals(c.getTone())))
					{
						heard.putIfAbsent(c.getText(), c.getWritten());
					}
				}
				heard.forEach((said, written) -> out.append(cell.id).append('\t').append(written).append('\t').append(said).append('\n'));
			}
			Files.createDirectories(OUT.resolve(run.name));
			Files.write(OUT.resolve(run.name).resolve("baseline.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
		}
	}
}
