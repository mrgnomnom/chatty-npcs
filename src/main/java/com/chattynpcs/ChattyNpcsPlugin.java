package com.chattynpcs;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Chatty NPCs",
	description = "The locals pass comment on your gear, your pets, your deeds and your haircut as you walk by. Terribly British about it.",
	tags = {"npc", "chat", "overhead", "banter", "british", "fun", "immersion", "quests", "pets", "gossip"}
)
public class ChattyNpcsPlugin extends Plugin
{
	/**
	 * How far away "Make someone talk" will look for someone
	 */
	private static final int TEST_RADIUS = 15;
	private static final int DEBUG_COOLDOWN_SECONDS = 5;
	private static final int DEBUG_RADIUS = 3;
	/**
	 * After a nearby NPC had nothing to say, wait this long before trying again rather than re-reading
	 * the character every tick
	 */
	private static final long RETRY_MILLIS = 3000;
	/**
	 * The game's own public chat colours, for players who haven't picked their own
	 */
	private static final Color OPAQUE_PUBLIC_CHAT = new Color(0x0000FF);
	private static final Color TRANSPARENT_PUBLIC_CHAT = new Color(0x9090FF);
	// "Guard dog" matches too. That's deliberate: a dog passing on the city's news is funny
	private static final Pattern GUARD = Pattern.compile("\\bguard\\b", Pattern.CASE_INSENSITIVE);
	/**
	 * Goblins pass comment even though you could attack them, so long as they aren't fighting.
	 * Cave goblins are Dorgeshuun, and talkable anyway.
	 */
	/**
	 * Every Fremennik name: one of sixteen beginnings and one of sixteen endings
	 */
	private static final Pattern FREMENNIK_NAME = Pattern.compile(
		"\\b(Bal|Bar|Dal|Dar|Den|Dok|Jar|Jik|Lar|Rak|Ral|Ril|Sig|Tal|Thor|Ton)(dar|dor|dur|kal|kar|kir|kur|lah|lak|lim|lor|rak|tin|ton|tor|vald)\\b");
	private static final Pattern GOBLIN = Pattern.compile("^(?!Cave goblin).*\\bgoblin\\b", Pattern.CASE_INSENSITIVE);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	/**
	 * The debug tools' sidebar button, there only while debug mode is on. Swing thread only.
	 */
	private NavigationButton debugButton;

	@Inject
	private ChattyNpcsConfig config;

	@Inject
	private CharacterReader characterReader;

	@Inject
	private Gson gson;

	@Inject
	private ConfigManager configManager;

	// Only touched on the client thread
	/**
	 * NPCs in the scene that could plausibly talk to you: Talk-to option, not attackable, not a pet.
	 * Plus every guard, who only speaks when on watch in a city.
	 */
	private final Set<NPC> chattyNpcs = new HashSet<>();
	private final Map<Integer, Long> npcLastSpoke = new HashMap<>();
	private final Random random = new Random();
	private long lastComment;
	private long nextAttempt;
	private CharacterReader.Snapshot cachedSnapshot;

	// Set on the client thread, but read by the config handler on the Swing thread
	private volatile ChatterData data;
	private volatile LinePicker picker;
	private volatile int cachedTick = -1;
	@Override
	protected void startUp() throws Exception
	{
		ChatterData loaded = ChatterData.load(gson);
		LinePicker loadedPicker = new LinePicker(loaded, random);
		showDebugTools(config.debugMode());

		// startUp runs on the Swing thread, and everything below belongs to the client thread
		clientThread.invoke(() ->
		{
			chattyNpcs.clear();
			npcLastSpoke.clear();
			lastComment = 0;
			nextAttempt = 0;
			cachedTick = -1;
			characterReader.questsChanged();
			data = loaded;
			picker = loadedPicker;

			if (client.getGameState() == GameState.LOGGED_IN)
			{
				for (NPC npc : client.getTopLevelWorldView().npcs())
				{
					track(npc);
				}
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		showDebugTools(false);
		clientThread.invoke(() ->
		{
			chattyNpcs.clear();
			npcLastSpoke.clear();
			cachedSnapshot = null;
			picker = null;
			data = null;
		});
	}

	@Provides
	ChattyNpcsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChattyNpcsConfig.class);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		track(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		chattyNpcs.remove(event.getNpc());
		track(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		chattyNpcs.remove(event.getNpc());
		npcLastSpoke.remove(event.getNpc().getIndex());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			chattyNpcs.clear();
			npcLastSpoke.clear();
		}
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// The next login may be a different account. Quest points arriving as the next login
			// syncs also marks quests changed, so region loads don't need to
			characterReader.questsChanged();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Every quest we talk about gives quest points when it's finished, so this is the moment a
		// quest's state changes in a way the locals would care about
		if (event.getVarpId() == VarPlayerID.QP)
		{
			characterReader.questsChanged();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		learnFremennikName();

		Player me = client.getLocalPlayer();
		if (picker == null || me == null)
		{
			return;
		}

		long now = System.currentTimeMillis();

		// A banker's welcome doesn't wait for cooldowns or chattiness: it's the first thing you hear
		picker.tune(config.questGossip() / 100.0, config.localFlavour() / 100.0, config.bankGreetings());
		for (NPC npc : nearbyChattyNpcs(me.getWorldLocation(), radius(), now, true))
		{
			if (picker.greetingDue(npc.getName(), mapLocation(npc), now))
			{
				speak(npc, now);
				return;
			}
			picker.stillHere(npc.getName(), mapLocation(npc), now);
		}

		if (now - lastComment < globalCooldownMillis() || now < nextAttempt)
		{
			return;
		}
		if (random.nextInt(100) >= chattiness())
		{
			return;
		}

		List<NPC> nearby = nearbyChattyNpcs(me.getWorldLocation(), radius(), now, false);
		if (!nearby.isEmpty() && !speak(nearby.get(random.nextInt(nearby.size())), now))
		{
			nextAttempt = now + RETRY_MILLIS;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ChattyNpcsConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		// Pretend quests or stats may have changed
		cachedTick = -1;

		if (ChattyNpcsConfig.DEBUG_MODE.equals(event.getKey()))
		{
			SwingUtilities.invokeLater(() -> showDebugTools(config.debugMode()));
		}
	}

	/**
	 * Debug mode puts its buttons in the sidebar, since the settings panel can't have buttons
	 */
	private void showDebugTools(boolean show)
	{
		if (show && debugButton == null)
		{
			BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
			debugButton = NavigationButton.builder()
				.tooltip("Chatty NPCs debug")
				.icon(ImageUtil.resizeImage(icon, 11, 16))
				.priority(10)
				.panel(new ChattyNpcsPanel(
					() -> clientThread.invokeLater(this::forceComment),
					() -> clientThread.invokeLater(this::describeCharacter)))
				.build();
			clientToolbar.addNavigation(debugButton);
		}
		else if (!show && debugButton != null)
		{
			clientToolbar.removeNavigation(debugButton);
			debugButton = null;
		}
	}

	private long globalCooldownMillis()
	{
		return (config.debugMode() ? DEBUG_COOLDOWN_SECONDS : config.globalCooldown()) * 1000L;
	}

	private long npcCooldownMillis()
	{
		return (config.debugMode() ? DEBUG_COOLDOWN_SECONDS : config.npcCooldown()) * 1000L;
	}

	private long topicCooldownMillis()
	{
		return (config.debugMode() ? DEBUG_COOLDOWN_SECONDS : config.topicCooldown()) * 1000L;
	}

	private int chattiness()
	{
		return config.debugMode() ? 100 : config.chattiness();
	}

	private int radius()
	{
		return config.debugMode() ? DEBUG_RADIUS : config.radius();
	}

	private void forceComment()
	{
		Player me = client.getLocalPlayer();
		if (picker == null || me == null)
		{
			return;
		}

		WorldPoint myLocation = me.getWorldLocation();
		List<NPC> nearby = nearbyChattyNpcs(myLocation, TEST_RADIUS, System.currentTimeMillis(), true);
		NPC closest = null;
		for (NPC npc : nearby)
		{
			if (closest == null || npc.getWorldLocation().distanceTo(myLocation) < closest.getWorldLocation().distanceTo(myLocation))
			{
				closest = npc;
			}
		}

		if (closest == null)
		{
			gameMessage("Chatty NPCs: nobody nearby to have a natter with.");
		}
		else if (!speak(closest, System.currentTimeMillis()))
		{
			gameMessage("Chatty NPCs: " + closest.getName() + " has nothing to say about you. Topics on cooldown, perhaps.");
		}
	}

	private void describeCharacter()
	{
		Player me = client.getLocalPlayer();
		LinePicker picker = this.picker;
		if (picker == null || me == null)
		{
			return;
		}

		CharacterReader.Snapshot snapshot = snapshot();
		gameMessage("Chatty NPCs: hair kit " + snapshot.hairKit + ", facts " + snapshot.facts);
		gameMessage("Chatty NPCs: topics that apply here " + picker.matchingTopics(snapshot, mapLocation(me)));
	}

	/**
	 * What the character looks like this tick. Reading quest states runs a client script per quest,
	 * so it's only done once per tick at most.
	 */
	private CharacterReader.Snapshot snapshot()
	{
		int tick = client.getTickCount();
		if (cachedSnapshot == null || cachedTick != tick)
		{
			ChatterData data = this.data;
			LinePicker picker = this.picker;
			cachedSnapshot = characterReader.read(data.baldHairKits, picker.quests(), config.pretendQuests(), config.pretendStats());
			String fremennikName = configManager.getRSProfileConfiguration(ChattyNpcsConfig.GROUP, ChattyNpcsConfig.FREMENNIK_NAME);
			if (fremennikName != null && !fremennikName.isEmpty())
			{
				cachedSnapshot.vars.put("fremennik_name", fremennikName);
			}
			cachedTick = tick;
		}
		return cachedSnapshot;
	}

	private List<NPC> nearbyChattyNpcs(WorldPoint myLocation, int radius, long now, boolean ignoreCooldowns)
	{
		long npcCooldown = npcCooldownMillis();
		List<NPC> nearby = new ArrayList<>();
		for (NPC npc : chattyNpcs)
		{
			if (!ignoreCooldowns && (picker == null || !picker.talksFreely(npc.getName(), mapLocation(npc))))
			{
				Long lastSpoke = npcLastSpoke.get(npc.getIndex());
				if (lastSpoke != null && now - lastSpoke < npcCooldown)
				{
					continue;
				}
			}

			// Don't talk over them if they're already saying something
			if (npc.getOverheadText() != null && npc.getOverheadCycle() > 0)
			{
				continue;
			}

			if (!config.housePets() && isHousePet(npc))
			{
				continue;
			}

			// Re-check, they might have become attackable since they spawned
			if ((isChatty(npc) || isGuardOnWatch(npc) || isGoblinAtEase(npc)) && npc.getWorldLocation().distanceTo(myLocation) <= radius)
			{
				nearby.add(npc);
			}
		}
		return nearby;
	}

	private boolean speak(NPC npc, long now)
	{
		LinePicker picker = this.picker;
		if (picker == null)
		{
			return false;
		}

		picker.tune(config.questGossip() / 100.0, config.localFlavour() / 100.0, config.bankGreetings());
		LinePicker.Comment comment = picker.pick(snapshot(), npc.getName(), mapLocation(npc), config.mood(), topicCooldownMillis(), now);
		if (comment == null)
		{
			return false;
		}

		log.debug("{} ({}, {}) on {}: {}", npc.getName(), comment.getPersona(), comment.getTone(), comment.getTopicId(), comment.getText());

		npc.setOverheadText(comment.getText());
		npc.setOverheadCycle(overheadCycles(comment.getText()));
		if (config.showInChat())
		{
			// A game message rather than NPC chat, so other plugins (chat filters, loggers) can tell
			// it apart from what the NPC really said
			gameMessage(config.publicChatStyle()
				? publicChatStyle(npc.getName(), comment.getText())
				: npc.getName() + " says, \"" + comment.getText() + "\"");
		}

		lastComment = now;
		npcLastSpoke.put(npc.getIndex(), now);
		return true;
	}

	/**
	 * How long a line stays above the NPC's head, in client cycles (20ms each): 9 seconds, up to 18
	 * for the longest lines, so there's time to read them while walking past
	 */
	static int overheadCycles(String text)
	{
		return Math.min(450 + text.length() * 6, 900);
	}

	/**
	 * Where an actor is on the world map. Inside instances (Prifddinas, for one) the server location
	 * is in instance space, which regions.json knows nothing about.
	 */
	private WorldPoint mapLocation(Actor actor)
	{
		LocalPoint local = actor.getLocalLocation();
		return local == null ? actor.getWorldLocation() : WorldPoint.fromLocalInstance(client, local);
	}

	private void track(NPC npc)
	{
		// Skip NPCs on boats and other moving world views, where map regions don't apply
		if (npc.getWorldView() == null || !npc.getWorldView().isTopLevel())
		{
			return;
		}

		// Guards wander in and out of town, so keep track of them all and check where they are later.
		// Goblins too, since they mutter even when you could attack them.
		if (isChatty(npc) || isGuard(npc) || isGoblin(npc))
		{
			chattyNpcs.add(npc);
		}
	}

	/**
	 * Only people you could stop for a chat: they have Talk-to, and you can't attack them
	 */
	private boolean isChatty(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null || composition.isFollower() || npc == client.getFollower())
		{
			return false;
		}

		// A combat level on its own counts as attackable, even without an Attack option showing
		String name = composition.getName();
		if (name == null || name.isEmpty() || "null".equals(name) || composition.getCombatLevel() > 0)
		{
			return false;
		}

		String[] actions = composition.getActions();
		if (actions == null)
		{
			return false;
		}

		boolean talkable = false;
		for (String action : actions)
		{
			if ("Attack".equalsIgnoreCase(action))
			{
				return false;
			}
			if ("Talk-to".equalsIgnoreCase(action))
			{
				talkable = true;
			}
		}
		return talkable;
	}

	/**
	 * The Fremennik never tell the plugin your name, but they say it to you: when one greets you in
	 * dialogue, remember it for this account
	 */
	private void learnFremennikName()
	{
		Widget text = client.getWidget(InterfaceID.ChatLeft.TEXT);
		if (text == null || text.isHidden() || text.getText() == null)
		{
			return;
		}
		Widget speaker = client.getWidget(InterfaceID.ChatLeft.NAME);
		String speakerName = speaker != null && speaker.getText() != null ? speaker.getText() : "";
		java.util.regex.Matcher m = FREMENNIK_NAME.matcher(text.getText());
		while (m.find())
		{
			String name = m.group();
			// Real Fremennik who happen to fit the pattern, so they can be talked about
			if (name.equals("Thorvald") || name.equals("Jarvald") || speakerName.contains(name))
			{
				continue;
			}
			// Only a name they've given you
			if (Quest.THE_FREMENNIK_TRIALS.getState(client) != QuestState.FINISHED)
			{
				return;
			}
			if (!name.equals(configManager.getRSProfileConfiguration(ChattyNpcsConfig.GROUP, ChattyNpcsConfig.FREMENNIK_NAME)))
			{
				configManager.setRSProfileConfiguration(ChattyNpcsConfig.GROUP, ChattyNpcsConfig.FREMENNIK_NAME, name);
				cachedTick = -1;
			}
			return;
		}
	}

	/**
	 * One of the player's pets let loose in their house, which is an instance, unlike the
	 * same pet out in the world following someone
	 */
	private boolean isHousePet(NPC npc)
	{
		ChatterData data = this.data;
		String name = npc.getName();
		if (data == null || name == null || npc.getWorldView() == null || !npc.getWorldView().isInstance())
		{
			return false;
		}
		for (ChatterData.Topic topic : data.topics)
		{
			if (topic.pet != null && topic.pet.compiled != null && topic.pet.compiled.matcher(name).find())
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isGoblin(NPC npc)
	{
		String name = npc.getName();
		return name != null && GOBLIN.matcher(name).find();
	}

	/**
	 * A goblin with nothing better to do: not fighting anyone, and not hurt
	 */
	private static boolean isGoblinAtEase(NPC npc)
	{
		return isGoblin(npc) && npc.getInteracting() == null && npc.getHealthRatio() == -1;
	}

	private static boolean isGuard(NPC npc)
	{
		String name = npc.getName();
		return name != null && GUARD.matcher(name).find();
	}

	/**
	 * The one exception to "never attackable": a guard on watch in a city, as long as they aren't
	 * fighting anyone
	 */
	private boolean isGuardOnWatch(NPC npc)
	{
		ChatterData data = this.data;
		if (data == null || !config.cityGuards() || !isGuard(npc) || npc.getInteracting() != null || npc.getHealthRatio() != -1)
		{
			return false;
		}

		WorldPoint location = mapLocation(npc);
		for (ChatterData.Region region : data.regions)
		{
			if (region.city && region.contains(location.getX(), location.getY()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * "[NPC] Hans: Nice hat." in the colours the player has chosen for public chat, opaque or transparent
	 */
	private String publicChatStyle(String name, String text)
	{
		boolean transparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;
		int setting = client.getVarpValue(transparent ? VarPlayerID.OPTION_CHAT_COLOUR_PUBLIC_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_PUBLIC_OPAQUE);
		// The setting holds the colour plus one, or nothing for the game's default
		Color words = setting != 0 ? new Color(setting - 1) : (transparent ? TRANSPARENT_PUBLIC_CHAT : OPAQUE_PUBLIC_CHAT);
		Color speaker = transparent ? Color.WHITE : Color.BLACK;
		// Tagged, so nobody mistakes the locals for a real player
		return ColorUtil.wrapWithColorTag("[NPC] " + name + ":", speaker) + " " + ColorUtil.wrapWithColorTag(text, words);
	}

	private void gameMessage(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}
}
