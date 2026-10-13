package com.chattynpcs;

import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CharacterReaderTest
{
	private Client client;
	private ItemManager itemManager;
	private ItemContainer worn;
	private PlayerComposition appearance;
	private CharacterReader reader;

	@Before
	public void before()
	{
		client = mock(Client.class);
		itemManager = mock(ItemManager.class);
		worn = mock(ItemContainer.class);
		appearance = mock(PlayerComposition.class);
		Player player = mock(Player.class);

		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getPlayerComposition()).thenReturn(appearance);
		when(player.getCombatLevel()).thenReturn(70);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
		when(client.getRealSkillLevel(Skill.HITPOINTS)).thenReturn(70);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(70);
		when(client.getEnergy()).thenReturn(10000);
		when(appearance.getKitId(KitType.HAIR)).thenReturn(5);

		reader = new CharacterReader(client, itemManager);
	}

	private void wear(EquipmentInventorySlot slot, int id, String name, long price, boolean tradeable)
	{
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getName()).thenReturn(name);
		when(composition.isTradeable()).thenReturn(tradeable);
		when(itemManager.getItemComposition(id)).thenReturn(composition);
		when(itemManager.getItemPrice(id)).thenReturn(price);
		when(worn.getItem(slot.getSlotIdx())).thenReturn(new Item(id, 1));
	}

	private CharacterReader.Snapshot read()
	{
		return reader.read(Collections.singletonList(0), Collections.emptyList(),
			ChattyNpcsConfig.PretendQuests.REAL, ChattyNpcsConfig.PretendStats.REAL);
	}

	@Test
	public void onlyArrowsCountsAsStarkers()
	{
		when(worn.getItem(anyInt())).thenReturn(null);
		wear(EquipmentInventorySlot.AMMO, 1, "Bronze arrow", 1, true);
		assertTrue(read().facts.contains("starkers"));
	}

	@Test
	public void untradeableGearIsNeverSkint()
	{
		wear(EquipmentInventorySlot.CAPE, 1, "Fire cape", 0, false);
		wear(EquipmentInventorySlot.BODY, 2, "Void knight top", 0, false);
		assertFalse(read().facts.contains("skint"));
	}

	@Test
	public void cheapTradeableGearIsSkint()
	{
		wear(EquipmentInventorySlot.BODY, 1, "Leather body", 20, true);
		CharacterReader.Snapshot s = read();
		assertTrue(s.facts.contains("skint"));
		assertTrue(s.facts.contains("no_weapon"));
	}

	@Test
	public void baldHairKitIsBald()
	{
		wear(EquipmentInventorySlot.BODY, 1, "Leather body", 20, true);
		assertFalse(read().facts.contains("bald"));
		when(appearance.getKitId(KitType.HAIR)).thenReturn(0);
		assertTrue(read().facts.contains("bald"));
	}

	@Test
	public void threeMetalTiersIsMismatched()
	{
		wear(EquipmentInventorySlot.HEAD, 1, "Bronze full helm", 100, true);
		wear(EquipmentInventorySlot.BODY, 2, "Rune platebody", 38000, true);
		wear(EquipmentInventorySlot.LEGS, 3, "Dragon platelegs", 160000, true);
		assertTrue(read().facts.contains("mixed_metals"));
	}

	@Test
	public void lowHitpointsAndRunEnergyShow()
	{
		wear(EquipmentInventorySlot.BODY, 1, "Leather body", 20, true);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(10);
		when(client.getEnergy()).thenReturn(500);
		CharacterReader.Snapshot s = read();
		assertTrue(s.facts.contains("low_hp"));
		assertTrue(s.facts.contains("knackered"));
	}

	@Test
	public void noticesThePetFollowingYou()
	{
		wear(EquipmentInventorySlot.BODY, 1, "Leather body", 20, true);
		NPC pet = mock(NPC.class);
		when(pet.getName()).thenReturn("Heron");
		when(client.getFollower()).thenReturn(pet);
		assertEquals("Heron", read().pet);
	}

	@Test
	public void questStatesAreOnlyReadAgainWhenQuestPointsChange()
	{
		when(client.getIntStack()).thenReturn(new int[]{2});
		java.util.List<Quest> quests = java.util.Arrays.asList(Quest.COOKS_ASSISTANT, Quest.DRAGON_SLAYER_II);
		for (int i = 0; i < 5; i++)
		{
			CharacterReader.Snapshot s = reader.read(Collections.emptyList(), quests,
				ChattyNpcsConfig.PretendQuests.REAL, ChattyNpcsConfig.PretendStats.REAL);
			assertEquals(QuestState.FINISHED, s.quests.get(Quest.DRAGON_SLAYER_II));
		}
		// One script per quest, once
		verify(client, times(2)).runScript(anyInt(), anyInt());

		reader.questsChanged();
		reader.read(Collections.emptyList(), quests, ChattyNpcsConfig.PretendQuests.REAL, ChattyNpcsConfig.PretendStats.REAL);
		verify(client, times(4)).runScript(anyInt(), anyInt());
	}

	@Test
	public void pretendQuestsNeverRunScripts()
	{
		reader.read(Collections.emptyList(), java.util.Arrays.asList(Quest.COOKS_ASSISTANT),
			ChattyNpcsConfig.PretendQuests.QUEST_CAPE, ChattyNpcsConfig.PretendStats.REAL);
		verify(client, never()).runScript(anyInt(), anyInt());
	}
}
