package com.chattynpcs;

import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(ChattyNpcsConfig.GROUP)
public interface ChattyNpcsConfig extends Config
{
	String GROUP = "chattynpcs";
	String DEBUG_MODE = "debugMode";
	/**
	 * Saved per account: the name the Fremennik gave you, picked up from their dialogue
	 */
	String FREMENNIK_NAME = "fremennikName";

	@ConfigSection(
		name = "How often",
		description = "How often the locals speak up, and how close you need to be",
		position = 0
	)
	String frequencySection = "frequency";

	@ConfigSection(
		name = "What they talk about",
		description = "The tone of what's said, and how much of it is news or local colour",
		position = 10
	)
	String contentSection = "content";

	@ConfigSection(
		name = "Display",
		description = "Where comments show up",
		position = 20
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Debug",
		description = "Tools for testing what the locals say",
		position = 100,
		closedByDefault = true
	)
	String debugSection = "debug";

	@RequiredArgsConstructor
	enum Mood
	{
		KIND("Kind"),
		BALANCED("Balanced"),
		RUDE("Rude");

		private final String displayName;

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	@RequiredArgsConstructor
	enum PretendQuests
	{
		REAL("Real"),
		NONE("None"),
		QUEST_CAPE("Quest cape");

		private final String displayName;

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	@RequiredArgsConstructor
	enum PretendStats
	{
		REAL("Real"),
		NOOB("Noob"),
		MAXED("Maxed");

		private final String displayName;

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	@Range(min = 5, max = 3600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "globalCooldown",
		name = "Time between comments",
		description = "The shortest gap between any two comments, from anyone",
		section = frequencySection,
		position = 0
	)
	default int globalCooldown()
	{
		return 45;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "radius",
		name = "Earshot (tiles)",
		description = "How close you have to walk past someone for them to comment",
		section = frequencySection,
		position = 1
	)
	default int radius()
	{
		return 3;
	}

	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "chattiness",
		name = "Chattiness",
		description = "Once the time between comments has passed, the chance each tick that someone nearby speaks up",
		section = frequencySection,
		position = 2
	)
	default int chattiness()
	{
		return 30;
	}

	@Range(min = 0, max = 3600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "npcCooldown",
		name = "Same person again after",
		description = "How long someone keeps quiet after commenting",
		section = frequencySection,
		position = 3
	)
	default int npcCooldown()
	{
		return 300;
	}

	@Range(min = 0, max = 3600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "topicCooldown",
		name = "Same subject again after",
		description = "How long before anyone brings up the same thing (your cape, a quest) again",
		section = frequencySection,
		position = 4
	)
	default int topicCooldown()
	{
		return 1200;
	}

	@ConfigItem(
		keyName = "mood",
		name = "Mood",
		description = "Kind only pays compliments, Rude only insults, Balanced is a bit of everything. Overheard gossip comes up in every mood",
		section = contentSection,
		position = 0
	)
	default Mood mood()
	{
		return Mood.BALANCED;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "questGossip",
		name = "Quest gossip",
		description = "At most how much of what's said is news of quests, so your gear and looks still get noticed. Bankers always talk news",
		section = contentSection,
		position = 1
	)
	default int questGossip()
	{
		return 33;
	}

	@Range(min = 0, max = 200)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "localFlavour",
		name = "Local flavour",
		description = "How strongly people sound like where they're from: accents, native words and local sayings. 0% is plain English everywhere. Babble you can't understand stays",
		section = contentSection,
		position = 2
	)
	default int localFlavour()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "cityGuards",
		name = "City guards",
		description = "Guards on watch in a city pass comment too, though you can't talk to them",
		section = contentSection,
		position = 3
	)
	default boolean cityGuards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankGreetings",
		name = "Bank greetings",
		description = "Bankers welcome you once per visit, in the local tongue",
		section = contentSection,
		position = 4
	)
	default boolean bankGreetings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "housePets",
		name = "House pets",
		description = "Pets roaming your house pass comment too. Turn off and they keep their thoughts to themselves",
		section = contentSection,
		position = 5
	)
	default boolean housePets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInChat",
		name = "Show in chatbox",
		description = "Also put comments in the chatbox, in case you miss the speech bubble",
		section = displaySection,
		position = 0
	)
	default boolean showInChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "publicChatStyle",
		name = "Look like public chat",
		description = "Show comments in the chatbox as \"[NPC] Name: words\" in your own public chat colours, rather than as a game message",
		section = displaySection,
		position = 1
	)
	default boolean publicChatStyle()
	{
		return true;
	}

	@ConfigItem(
		keyName = DEBUG_MODE,
		name = "Debug mode",
		description = "For testing: 5 second cooldowns, 100% chattiness, 3 tile earshot, without changing your settings, and a Chatty NPCs panel in the sidebar with Make someone talk and Describe my character buttons",
		section = debugSection,
		position = 0
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pretendQuests",
		name = "Pretend quests",
		description = "Make the locals think you've done no quests, or every quest, instead of your real ones",
		section = debugSection,
		position = 1
	)
	default PretendQuests pretendQuests()
	{
		return PretendQuests.REAL;
	}

	@ConfigItem(
		keyName = "pretendStats",
		name = "Pretend stats",
		description = "Make the locals think you look like a fresh level 3 or a fearsome level 126, instead of your real combat level",
		section = debugSection,
		position = 2
	)
	default PretendStats pretendStats()
	{
		return PretendStats.REAL;
	}
}
