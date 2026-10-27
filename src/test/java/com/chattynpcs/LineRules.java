package com.chattynpcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What any line has to pass, whoever wrote it. The tests hold the bank to these, and
 * GeneratedLinesTest holds the line generator's output to them too.
 */
final class LineRules
{
	static final Pattern BANNED = Pattern.compile("(?i)\\b(bloody|arse|arsed|muppet|just saying|no offence|lovely jubbly|sweet as a nut|on my life|oi oi|pet|duck|petal|ta-ra|telly|radio|lycra|rave|insurance|CV|fiver|tenner|cash|charity shop|stag do|bins|guide|cute|awesome|baddie|pathetic|useless|(mon|tues|wednes|thurs|fri|satur|sun)days?)\\b");

	/**
	 * The tells of a line written by a model rather than said by a person: long dashes, "not just X,
	 * it's Y", and the words models reach for that nobody in Gielinor says
	 */
	static final Pattern AI_TELLS = Pattern.compile("(?i)[—–]|\\bnot (just|only|merely|simply)\\b[^.!?]*,\\s(it's|it is|but|that's)\\b|\\b(testament|tapestry|delv\\w*|resonat\\w*|embark\\w*|unwavering|unparalleled|showcas\\w*|elevat\\w*|vibrant|bustling|intricate|meticulous\\w*|symphony|realm of|in the heart of|a sight to behold)\\b");

	private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z']*");
	private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?])\\s+");
	private static final Pattern INNER_CAPITAL = Pattern.compile("[a-z][A-Z]");

	/**
	 * Lower-case words of every name the game or the reviewed lore list knows
	 */
	private final Set<String> names = new HashSet<>();
	/**
	 * Quest titles that aren't also the name of a place, person or thing: saying "the Grand Tree" is
	 * fine, saying "Monkey Madness" is naming a quest
	 */
	private final List<Pattern> questTitles = new ArrayList<>();

	static LineRules load(ChatterData data) throws IOException
	{
		LineRules rules = new LineRules();
		Set<String> places = new HashSet<>();
		List<String> known = read("wiki_names.txt");
		known.addAll(read("names.txt"));
		for (String name : known)
		{
			rules.addName(name);
			// The Pandemonium is an island, as well as a quest
			places.add(name.toLowerCase(Locale.ROOT).replaceFirst("^the ", ""));
		}
		// Their own words count: if a gnome says it in the game, a gnome can say it here
		for (ChatterData.Speech speech : data.speech)
		{
			if (speech.address != null)
			{
				rules.addName(speech.address.word);
				rules.addName(speech.address.thenWord);
			}
			for (ChatterData.Words words : new ChatterData.Words[]{speech.exclaim, speech.praise, speech.greet, speech.farewell})
			{
				if (words != null)
				{
					words.words.forEach(rules::addName);
				}
			}
			speech.swaps.forEach(swap -> rules.addName(swap.replace));
			speech.evidence.forEach(rules::addName);
			if (speech.babble != null)
			{
				speech.babble.phrases.forEach(rules::addName);
			}
		}
		for (String title : read("quest_titles.txt"))
		{
			// "Dragon Slayer I" is spoken of as "Dragon Slayer"
			String spoken = title.replaceFirst("^The ", "").replaceFirst(" I+$", "");
			if (!places.contains(spoken.toLowerCase(Locale.ROOT)))
			{
				rules.questTitles.add(Pattern.compile("\\b" + Pattern.quote(spoken) + "\\b"));
			}
		}
		return rules;
	}

	private static List<String> read(String file) throws IOException
	{
		List<String> lines = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(LineRules.class.getResourceAsStream(file), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = in.readLine()) != null)
			{
				// names.txt explains itself in comments
				int comment = line.indexOf('#');
				String name = (comment < 0 ? line : line.substring(0, comment)).trim();
				if (!name.isEmpty())
				{
					lines.add(name);
				}
			}
		}
		return lines;
	}

	private void addName(String name)
	{
		if (name == null)
		{
			return;
		}
		Matcher word = WORD.matcher(name);
		while (word.find())
		{
			names.add(bare(word.group()).toLowerCase(Locale.ROOT));
		}
	}

	/**
	 * The word without its possessive or contraction: "Knights'" is "Knights", "Room'll" is "Room",
	 * "I'm" is "I"
	 */
	private static String bare(String word)
	{
		return word.replaceFirst("'(s|ll|d|re|ve|m)?$", "");
	}

	/**
	 * Words a line capitalises mid-sentence, which no name in the game contains. Each one is either
	 * a name worth checking against the Wiki or one the writer made up.
	 */
	List<String> unknownNames(String line)
	{
		List<String> unknown = new ArrayList<>();
		for (String sentence : SENTENCE.split(line.replaceAll("\\{[a-z_]+}", "x")))
		{
			Matcher word = WORD.matcher(sentence);
			boolean first = true;
			while (word.find())
			{
				String w = bare(word.group());
				boolean name = !first && Character.isUpperCase(w.charAt(0)) || INNER_CAPITAL.matcher(w).find();
				first = false;
				if (!name || w.equals("I"))
				{
					continue;
				}
				String lower = w.toLowerCase(Locale.ROOT);
				// The Fitzharmons are Fitzharmons
				if (!names.contains(lower) && !(lower.endsWith("s") && names.contains(lower.substring(0, lower.length() - 1))))
				{
					unknown.add(w);
				}
			}
		}
		return unknown;
	}

	/**
	 * The quest a line names by its title, or null. Nobody in Gielinor calls their troubles by a
	 * quest's name.
	 */
	String namedQuest(String line)
	{
		for (Pattern title : questTitles)
		{
			if (title.matcher(line).find())
			{
				return title.pattern();
			}
		}
		return null;
	}

	/**
	 * Every line the plugin can say, as {where it lives, the line}
	 */
	static List<String[]> spoken(ChatterData data)
	{
		List<String[]> spoken = new ArrayList<>();
		for (ChatterData.Topic topic : data.topics)
		{
			for (Map.Entry<String, List<String>> entry : topic.lines.entrySet())
			{
				for (String line : entry.getValue())
				{
					spoken.add(new String[]{topic.id + "/" + entry.getKey(), line});
				}
			}
		}
		for (ChatterData.Persona persona : data.personas)
		{
			for (List<String> wrappers : java.util.Arrays.asList(persona.openers, persona.closers, persona.greetings))
			{
				for (String line : wrappers)
				{
					spoken.add(new String[]{persona.id, line.replaceFirst("^[a-z|]+:\\s*", "")});
				}
			}
		}
		for (Map.Entry<String, List<String>> tips : data.slayerTips.entrySet())
		{
			for (String tip : tips.getValue())
			{
				spoken.add(new String[]{"slayer tip for " + tips.getKey(), tip});
			}
		}
		return spoken;
	}
}
