package com.chattynpcs;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/**
 * Not a check: writes build/render.tsv, what real speakers in real places actually say to a few
 * kinds of player, with the line as written next to what came out. For reading, the way a player
 * would hear it, to catch lines the pieces don't fit together into. Every test run refreshes it.
 */
public class RenderReport
{
	private static final Object[][] SPEAKERS = {
		{"Hans", "Lumbridge", 3222, 3218}, {"Banker", "Varrock West bank", 3185, 3438}, {"Bartender", "Varrock", 3225, 3400},
		{"Father Aereck", "Lumbridge church", 3243, 3206}, {"Guard", "Falador", 2965, 3380}, {"Shop keeper", "Falador", 2958, 3388},
		{"Gnome", "Gnome Stronghold", 2460, 3440}, {"Gnome child", "Gnome Stronghold", 2465, 3445}, {"Gnome banker", "Grand Tree", 2449, 3482},
		{"TzHaar-Hur-Tel", "Mor Ul Rek", 2480, 5170}, {"Dwarf", "Keldagrim", 2850, 10200}, {"Thurgo", "Port Sarim", 3000, 3145},
		{"Brundt the Chieftain", "Rellekka", 2660, 3680}, {"Man", "Jatizso", 2400, 3810}, {"Man", "Neitiznot", 2335, 3805},
		{"Oneiromancer", "Lunar Isle", 2150, 3870}, {"Rantz", "Feldip Hills", 2630, 2981}, {"Elder Krill", "the Great Conch", 3190, 2420},
		{"Man", "Canifis", 3495, 3490}, {"Ghost villager", "Port Phasmatys", 3680, 3490}, {"Man", "Darkmeyer", 3620, 3360},
		{"Man", "Burgh de Rott", 3502, 3220}, {"Man", "Al Kharid", 3293, 3180}, {"Man", "Pollnivneach", 3359, 2970},
		{"Man", "Sophanem", 3300, 2780}, {"Man", "Shantay Pass", 3300, 3120}, {"Man", "Tai Bwo Wannai", 2790, 3060},
		{"Man", "Civitas illa Fortis", 1680, 3130}, {"Man", "Arceuus", 1680, 3780}, {"Man", "Port Piscarilius", 1810, 3730},
		{"Man", "Shayzien", 1500, 3600}, {"Man", "Lovakengj", 1500, 3800}, {"Man", "the Pandemonium", 3050, 2980},
		{"Elf", "Lletya", 2340, 3170}, {"Monkey", "Ape Atoll", 2770, 2780}, {"Vannaka", "Edgeville", 3145, 9914},
		{"Granny", "Draynor", 3090, 3250}, {"Wizard Mizgog", "Wizards' Tower", 3105, 3160}, {"Pirate", "Brimhaven", 2790, 3170},
		{"Duke Horacio", "Lumbridge Castle", 3210, 3220}, {"Fred the Farmer", "Lumbridge", 3190, 3273}, {"Street urchin", "Varrock", 3215, 3425},
	};

	private static Map<String, CharacterReader.Snapshot> players(LinePicker picker)
	{
		Map<String, CharacterReader.Snapshot> players = new LinkedHashMap<>();

		CharacterReader.Snapshot fresh = new CharacterReader.Snapshot();
		fresh.facts.add("always");
		fresh.facts.add("low_combat");
		fresh.facts.add("skint");
		fresh.facts.add("no_slayer_task");
		fresh.worn.put(EquipmentInventorySlot.BODY, "Bronze platebody");
		fresh.worn.put(EquipmentInventorySlot.LEGS, "Bronze platelegs");
		fresh.worn.put(EquipmentInventorySlot.WEAPON, "Bronze sword");
		for (Quest q : picker.quests())
		{
			fresh.quests.put(q, QuestState.NOT_STARTED);
		}
		players.put("new player", fresh);

		CharacterReader.Snapshot mid = new CharacterReader.Snapshot();
		mid.facts.add("always");
		mid.facts.add("bald");
		mid.facts.add("slayer_task");
		mid.vars.put("task", "hill giants");
		mid.worn.put(EquipmentInventorySlot.HEAD, "Helm of neitiznot");
		mid.worn.put(EquipmentInventorySlot.BODY, "Green d'hide body");
		mid.worn.put(EquipmentInventorySlot.WEAPON, "Rune scimitar");
		mid.worn.put(EquipmentInventorySlot.CAPE, "Fire cape");
		mid.pet = "Rocky";
		int i = 0;
		for (Quest q : picker.quests())
		{
			mid.quests.put(q, i++ % 2 == 0 ? QuestState.FINISHED : QuestState.NOT_STARTED);
		}
		players.put("mid player", mid);

		CharacterReader.Snapshot end = new CharacterReader.Snapshot();
		end.facts.add("always");
		end.facts.add("high_combat");
		end.facts.add("loaded");
		end.facts.add("slayer_task_nearly_done");
		end.facts.add("slayer_task");
		end.vars.put("task", "abyssal demons");
		end.worn.put(EquipmentInventorySlot.HEAD, "Torva full helm");
		end.worn.put(EquipmentInventorySlot.BODY, "Torva platebody");
		end.worn.put(EquipmentInventorySlot.WEAPON, "Twisted bow");
		end.worn.put(EquipmentInventorySlot.CAPE, "Infernal cape");
		end.worn.put(EquipmentInventorySlot.AMULET, "Ghostspeak amulet");
		end.pet = "Olmlet";
		for (Quest q : picker.quests())
		{
			end.quests.put(q, QuestState.FINISHED);
		}
		players.put("endgame player", end);
		return players;
	}

	@Test
	public void render() throws Exception
	{
		ChatterData data = ChatterData.load(new Gson());
		StringBuilder out = new StringBuilder("player\tmood\tspeaker\twhere\tpersona\ttopic\tkey\twritten\tsaid\n");
		ChattyNpcsConfig.Mood[] moods = {ChattyNpcsConfig.Mood.BALANCED, ChattyNpcsConfig.Mood.BALANCED, ChattyNpcsConfig.Mood.KIND, ChattyNpcsConfig.Mood.RUDE};
		int seed = 0;
		for (Object[] speaker : SPEAKERS)
		{
			LinePicker picker = new LinePicker(data, new Random(seed++));
			for (Map.Entry<String, CharacterReader.Snapshot> player : players(picker).entrySet())
			{
				long now = 0;
				for (ChattyNpcsConfig.Mood mood : moods)
				{
					for (int k = 0; k < 4; k++)
					{
						now += 60_000;
						LinePicker.Comment c = picker.pick(player.getValue(), (String) speaker[0], new WorldPoint((int) speaker[2], (int) speaker[3], 0), mood, 300_000, now);
						if (c != null)
						{
							out.append(player.getKey()).append('\t').append(mood).append('\t').append(speaker[0]).append('\t').append(speaker[1]).append('\t')
								.append(c.getPersona()).append('\t').append(c.getTopicId()).append('\t').append(c.getKey()).append('\t')
								.append(c.getWritten()).append('\t').append(c.getText()).append('\n');
						}
					}
				}
			}
		}
		Files.write(Paths.get("build/render.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
	}
}
