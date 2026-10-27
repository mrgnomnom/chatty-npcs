package com.chattynpcs;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Nobody makes up a person, place or quest. A name the game doesn't have is either one the Wiki
 * knows that belongs in names.txt, or one somebody invented, which is a line to rewrite.
 */
public class LoreTest
{
	private ChatterData data;
	private LineRules rules;

	@Before
	public void before() throws Exception
	{
		data = ChatterData.load(new Gson());
		rules = LineRules.load(data);
	}

	@Test
	public void everyNameIsOneTheGameKnows()
	{
		List<String> unknown = new ArrayList<>();
		for (String[] line : LineRules.spoken(data))
		{
			for (String name : rules.unknownNames(line[1]))
			{
				unknown.add(name + " in " + line[0] + ": " + line[1]);
			}
		}
		assertTrue("Names the game doesn't know. Check each against the Wiki, then add it to names.txt or rewrite the line:\n"
			+ String.join("\n", unknown), unknown.isEmpty());
	}

	@Test
	public void nobodyNamesAQuest()
	{
		List<String> named = new ArrayList<>();
		for (String[] line : LineRules.spoken(data))
		{
			String quest = rules.namedQuest(line[1]);
			if (quest != null)
			{
				named.add(quest + " in " + line[0] + ": " + line[1]);
			}
		}
		assertTrue("Lines that name a quest:\n" + String.join("\n", named), named.isEmpty());
	}

	@Test
	public void theChecksCatchWhatTheyShould()
	{
		assertEquals(Arrays.asList("Glimmerhold"), rules.unknownNames("Fresh from Glimmerhold, are you? In that hat?"));
		assertEquals(Arrays.asList(), rules.unknownNames("The Fitzharmons and the Black Knights' lot. JalYt-Ket, maybe."));
		assertEquals(Arrays.asList(), rules.unknownNames("Well, I'm off, and I'd say I've earned it."));
		assertNotNull(rules.namedQuest("Did you ever finish Monkey Madness, then?"));
		assertNull(rules.namedQuest("Something's eating the Grand Tree, they say."));
		assertNull(rules.namedQuest("Narnode's monkey madness has gone on long enough."));
		assertTrue(LineRules.AI_TELLS.matcher("It's not just a cape, it's a statement.").find());
		assertTrue(LineRules.BANNED.matcher("See you Sundays.").find());
	}

	@Test
	public void noLineSoundsWrittenByAMachine()
	{
		List<String> tells = new ArrayList<>();
		for (String[] line : LineRules.spoken(data))
		{
			if (LineRules.AI_TELLS.matcher(line[1]).find())
			{
				tells.add(line[0] + ": " + line[1]);
			}
		}
		assertTrue("Lines with a machine's tells:\n" + String.join("\n", tells), tells.isEmpty());
	}
}
