package com.chattynpcs;

import com.google.gson.Gson;
import java.util.*;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/**
 * Not a check: writes build/language-share.txt, showing how often each speech.json entry actually
 * changes what's said. Every test run refreshes it.
 * <ul>
 * <li>local: said in their own way at all</li>
 * <li>sayings: one of their own sayings</li>
 * <li>translated: a shared line their speech changed (mostly what they call you)</li>
 * <li>native: has a word of their own language in it</li>
 * <li>nat.swap: a shared line that picked up a native word</li>
 * </ul>
 */
public class LanguageShareReport
{
	@Test
	public void share() throws Exception
	{
		ChatterData data = ChatterData.load(new Gson());
		Map<String, ChatterData.Region> regions = new HashMap<>();
		data.regions.forEach(r -> regions.put(r.id, r));
		// speech id -> NPC name to test with (null: an ordinary "Man" standing in its first region)
		Map<String, String> speakers = new LinkedHashMap<>();
		speakers.put("tzhaar", "TzHaar-Hur-Tel");
		speakers.put("gnome", "Gnome");
		speakers.put("dwarf", "Dwarf");
		speakers.put("fremennik", "Brundt the Chieftain");
		speakers.put("ghost", "Ghost villager");
		speakers.put("monkey", "Monkey");
		speakers.put("moon_clan", "Oneiromancer");
		speakers.put("ogre", "Rantz");
		speakers.put("tortugan", "Elder Krill");
		speakers.put("old_ones", "Eyatlalli");
		StringBuilder out = new StringBuilder(String.format("%-18s %6s %8s %10s %7s %7s %9s%n", "speech", "local", "sayings", "translated", "plain", "native", "nat.swap"));
		for (ChatterData.Speech speech : data.speech)
		{
			if (speech.babble != null && speech.babble.understoodWearing == null && speech.babble.understoodAfter.isEmpty())
			{
				out.append(String.format("%-18s always babble%n", speech.id));
				continue;
			}
			String npc = speakers.getOrDefault(speech.id, "Man");
			WorldPoint at = null;
			// Where they're most at home: their own region, or where their local sayings are said
			List<String> home = !speech.regions.isEmpty() ? speech.regions : new ArrayList<>(speech.sayingsIn.keySet());
			if (!home.isEmpty())
			{
				int[] box = regions.get(home.get(0)).boxes.get(0);
				at = new WorldPoint((box[0] + box[2]) / 2, (box[1] + box[3]) / 2, 0);
			}
			LinePicker picker = new LinePicker(data, new Random(speech.id.hashCode()));
			CharacterReader.Snapshot s = new CharacterReader.Snapshot();
			s.facts.add("always"); s.facts.add("high_combat");
			s.worn.put(EquipmentInventorySlot.CAPE, "Fire cape");
			s.worn.put(EquipmentInventorySlot.HEAD, "Neitiznot faceguard");
			s.worn.put(EquipmentInventorySlot.WEAPON, "Abyssal whip");
			s.worn.put(EquipmentInventorySlot.BODY, "Bandos chestplate");
			s.worn.put(EquipmentInventorySlot.AMULET, "Ghostspeak amulet");
			s.worn.put(EquipmentInventorySlot.RING, "Not an M'speak amulet");
			int i = 0;
			for (Quest q : picker.quests()) s.quests.put(q, i++ % 2 == 0 ? QuestState.FINISHED : QuestState.NOT_STARTED);
			if (speech.id.equals("monkey")) s.worn.put(EquipmentInventorySlot.AMULET, "M'speak amulet");
			// Words from the language itself, not just a way of addressing you
			java.util.regex.Pattern nativeWords = java.util.regex.Pattern.compile("(?i)\\b(sarko|kar|lemantolly|ta quir priw|cinqo|megal|gnomish|es ataris|tokkul|haar-tok|tetamo|kuani|nilsal|timoiva|iknami)\\b");
			int n = 0, local = 0, sayings = 0, translated = 0, nat = 0, natTranslated = 0;
			List<String> examples = new ArrayList<>();
			for (int k = 0; k < 400; k++)
			{
				LinePicker.Comment c = picker.pick(s, npc, at, ChattyNpcsConfig.Mood.BALANCED, 300000, k * 60000L);
				if (c == null) continue;
				n++;
				if (nativeWords.matcher(c.getText()).find())
				{
					nat++;
					if (!c.getTopicId().contains("_sayings")) natTranslated++;
				}
				if (c.isLocal())
				{
					local++;
					if (c.getTopicId().contains("_sayings")) sayings++; else translated++;
					if (examples.size() < 2 && !c.getTopicId().contains("_sayings")) examples.add(c.getText());
				}
			}
			out.append(String.format("%-18s %5d%% %7d%% %9d%% %6d%% %6.1f%% %8.1f%%%n", speech.id + (speech.npcNames.isEmpty() ? "" : "*"),
				100 * local / n, 100 * sayings / n, 100 * translated / n, 100 * (n - local) / n, 100.0 * nat / n, 100.0 * natTranslated / n));
		}
		java.nio.file.Files.write(java.nio.file.Paths.get("build/language-share.txt"), out.toString().getBytes());
	}
}
