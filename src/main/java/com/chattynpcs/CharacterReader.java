package com.chattynpcs;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

/**
 * Turns the local player's appearance, gear and stats into facts the locals can comment on.
 * Must be called on the client thread.
 */
@Singleton
class CharacterReader
{
	private static final String[] METAL_TIERS = {
		"bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon",
	};

	private final Client client;
	private final ItemManager itemManager;

	/**
	 * Quest states only change when a quest is finished, and each one costs a client script to read,
	 * so they're read once and kept until {@link #questsChanged()}
	 */
	private final Map<Quest, QuestState> questCache = new EnumMap<>(Quest.class);
	private boolean questsStale = true;

	@Inject
	CharacterReader(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	static class Snapshot
	{
		final Set<String> facts = new HashSet<>();
		final Map<EquipmentInventorySlot, String> worn = new EnumMap<>(EquipmentInventorySlot.class);
		/**
		 * What's in the backpack, by name: only for the few things a local can tell you're carrying,
		 * like a Book o' piracy that lets you follow the Mos Le'Harmless pirates
		 */
		final Set<String> carried = new HashSet<>();
		final Map<Quest, QuestState> quests = new EnumMap<>(Quest.class);
		/**
		 * Values for placeholders in fact lines, e.g. "task" for the current slayer assignment
		 */
		final Map<String, String> vars = new HashMap<>();
		/**
		 * The name of the pet following the player, if any
		 */
		String pet;
		int hairKit = -1;
	}

	/**
	 * Kills left at or under this, and the task is nearly done
	 */
	private static final int SLAYER_NEARLY_DONE = 10;

	private final Map<Integer, String> slayerTaskNames = new HashMap<>();

	/**
	 * What a slayer master can see in their book: the creature you're assigned, and whether you're
	 * nearly through it. Never the number itself.
	 */
	private void readSlayerTask(Snapshot s)
	{
		int left = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		if (left <= 0)
		{
			s.facts.add("no_slayer_task");
			return;
		}
		String task = slayerTaskName(client.getVarpValue(VarPlayerID.SLAYER_TARGET));
		if (task == null)
		{
			return;
		}
		s.facts.add("slayer_task");
		s.vars.put("task", task);
		if (left <= SLAYER_NEARLY_DONE)
		{
			s.facts.add("slayer_task_nearly_done");
		}
	}

	private String slayerTaskName(int taskId)
	{
		return slayerTaskNames.computeIfAbsent(taskId, id ->
		{
			List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, id);
			if (rows == null || rows.isEmpty())
			{
				return null;
			}
			Object[] name = client.getDBTableField(rows.get(0), DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
			return name != null && name.length > 0 && name[0] instanceof String ? inSentence((String) name[0]) : null;
		});
	}

	/**
	 * "Abyssal demons" reads as "abyssal demons" mid-sentence, but "TzHaar" keeps its capitals
	 */
	static String inSentence(String name)
	{
		String firstWord = name.split(" ", 2)[0];
		if (name.isEmpty() || !firstWord.substring(1).equals(firstWord.substring(1).toLowerCase(Locale.ROOT)))
		{
			return name;
		}
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	/**
	 * The next read looks quest states up again. Call it on login and whenever quest points change.
	 */
	void questsChanged()
	{
		questsStale = true;
	}

	/**
	 * Quest states may be looked up with a client script, so this can't be called from inside a
	 * script callback (such as a chat command).
	 */
	Snapshot read(Collection<Integer> baldHairKits, Collection<Quest> quests,
		ChattyNpcsConfig.PretendQuests pretendQuests, ChattyNpcsConfig.PretendStats pretendStats)
	{
		Snapshot s = new Snapshot();
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return s;
		}

		s.facts.add("always");

		readGear(s);
		readCarried(s);
		readPet(s);
		readAppearance(s, player, baldHairKits);
		readTzHaarName(s, player);
		readStats(s, player, pretendStats);
		readQuests(s, quests, pretendQuests);
		readSlayerTask(s);

		Widget wildernessLevel = client.getWidget(InterfaceID.PvpIcons.WILDERNESSLEVEL);
		if (wildernessLevel != null && !wildernessLevel.isHidden())
		{
			s.facts.add("wilderness");
		}

		return s;
	}

	private void readCarried(Snapshot s)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return;
		}
		for (Item item : inventory.getItems())
		{
			if (item != null && item.getId() >= 0)
			{
				s.carried.add(itemManager.getItemComposition(item.getId()).getName());
			}
		}
	}

	private void readGear(Snapshot s)
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		long value = 0;
		// Untradeable gear (fire capes, Void, fighter torsos) has no price, so it says nothing about
		// how skint you are
		boolean allTradeable = true;
		Set<String> metals = new HashSet<>();

		if (equipment != null)
		{
			for (EquipmentInventorySlot slot : EquipmentInventorySlot.values())
			{
				Item item = equipment.getItem(slot.getSlotIdx());
				if (item == null || item.getId() < 0)
				{
					continue;
				}

				ItemComposition composition = itemManager.getItemComposition(item.getId());
				String itemName = composition.getName();
				s.worn.put(slot, itemName);
				value += itemManager.getItemPrice(item.getId()) * (long) item.getQuantity();
				allTradeable &= composition.isTradeable();

				String lower = itemName.toLowerCase(Locale.ROOT);
				for (String metal : METAL_TIERS)
				{
					if (lower.startsWith(metal + " "))
					{
						metals.add(metal);
					}
				}
			}
		}

		boolean wearingClothes = s.worn.keySet().stream().anyMatch(slot -> slot != EquipmentInventorySlot.AMMO);
		if (!wearingClothes)
		{
			s.facts.add("starkers");
		}
		else
		{
			if (!s.worn.containsKey(EquipmentInventorySlot.WEAPON))
			{
				s.facts.add("no_weapon");
			}
			if (value < 15_000 && allTradeable)
			{
				s.facts.add("skint");
			}
		}

		if (value >= 100_000_000)
		{
			s.facts.add("loaded");
		}
		if (metals.size() >= 3)
		{
			s.facts.add("mixed_metals");
		}

	}

	private void readPet(Snapshot s)
	{
		NPC follower = client.getFollower();
		if (follower != null)
		{
			s.pet = follower.getName();
		}
	}

	/**
	 * The name the TzHaar of Mor Ul Rek give you: race, caste from your strongest pair of skills,
	 * Xo once you've shown them your fire cape, Kal once you've beaten TzKal-Zuk, then your own name
	 */
	private void readTzHaarName(Snapshot s, Player player)
	{
		String name = player.getName();
		if (name == null)
		{
			return;
		}
		// Ties go to the first caste listed
		String caste = "Mej";
		int best = level(Skill.MAGIC) + level(Skill.PRAYER);
		int ket = level(Skill.STRENGTH) + level(Skill.DEFENCE);
		if (ket > best)
		{
			caste = "Ket";
			best = ket;
		}
		int xil = level(Skill.ATTACK) + level(Skill.AGILITY);
		if (xil > best)
		{
			caste = "Xil";
			best = xil;
		}
		if (level(Skill.CRAFTING) + level(Skill.SMITHING) > best)
		{
			caste = "Hur";
		}

		String cape = s.worn.getOrDefault(EquipmentInventorySlot.CAPE, "");
		String honour = "";
		if (client.getVarpValue(VarPlayerID.TOTAL_ZUK_KILLS) > 0 || cape.startsWith("Infernal"))
		{
			honour = "-Kal";
		}
		else if (client.getVarbitValue(VarbitID.INFERNO_SACRIFICED_FIRECAPE) > 0 || cape.startsWith("Fire cape") || cape.startsWith("Fire max cape"))
		{
			honour = "-Xo";
		}
		s.vars.put("tzhaar_name", "JalYt-" + caste + honour + "-" + name.replace('\u00A0', ' '));
	}

	private int level(Skill skill)
	{
		return client.getRealSkillLevel(skill);
	}

	private void readAppearance(Snapshot s, Player player, Collection<Integer> baldHairKits)
	{
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return;
		}
		s.vars.put("kin", composition.isFemale() ? "sister" : "brother");

		// A helmet that hides the hair replaces the kit with nothing, so this is -1 under a full helm
		s.hairKit = composition.getKitId(KitType.HAIR);
		if (s.hairKit >= 0 && baldHairKits.contains(s.hairKit))
		{
			s.facts.add("bald");
		}
	}

	private void readStats(Snapshot s, Player player, ChattyNpcsConfig.PretendStats pretend)
	{
		// Combat level is the one stat people can judge by looking at you, so it only ever comes out
		// as an impression ("you look like you could handle yourself"), never a number
		int combat = pretend == ChattyNpcsConfig.PretendStats.NOOB ? 3
			: pretend == ChattyNpcsConfig.PretendStats.MAXED ? 126
			: player.getCombatLevel();
		if (combat >= 115)
		{
			s.facts.add("high_combat");
		}
		else if (combat < 30)
		{
			s.facts.add("low_combat");
		}

		int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
		if (hp * 10 < maxHp * 3)
		{
			s.facts.add("low_hp");
		}

		// Run energy is 0-10000
		if (client.getEnergy() < 1000)
		{
			s.facts.add("knackered");
		}
	}

	private void readQuests(Snapshot s, Collection<Quest> quests, ChattyNpcsConfig.PretendQuests pretend)
	{
		for (Quest quest : quests)
		{
			switch (pretend)
			{
				case NONE:
					s.quests.put(quest, QuestState.NOT_STARTED);
					break;
				case QUEST_CAPE:
					s.quests.put(quest, QuestState.FINISHED);
					break;
				default:
					s.quests.put(quest, realQuestState(quest, quests));
			}
		}
	}

	private QuestState realQuestState(Quest quest, Collection<Quest> quests)
	{
		if (questsStale || !questCache.containsKey(quest))
		{
			for (Quest q : quests)
			{
				questCache.put(q, q.getState(client));
			}
			questsStale = false;
		}
		return questCache.get(quest);
	}
}
