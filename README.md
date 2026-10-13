# Chatty NPCs

The locals have noticed you. They have opinions. They will share them, whether or not you asked.

Walk past anyone you could stop for a chat and they'll pass comment on your gear, your pet, your
haircut, your deeds and the quests you've conspicuously failed to finish. Most annoyingly British about it.


![A TzHaar remarks that warm JalYt are good, and cold JalYt are dead](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/tzhaar-warm.png)

## What they notice

- **Your gear**, from bronze to the Tumeken's shadow, described the way a passer-by would see it
- **Your pet**, every last one of them
- **Your looks**: bald, starkers, skint, bleeding, out of breath, dripping in gold
- **Your deeds**: quest news travels as gossip. Small quests stay local, grandmasters go
  everywhere, and quest givers remember you personally. Nobody ever names a quest, because nobody
  in Gielinor talks like that
- **Your slayer task**, if a slayer master happens to be the one talking

![A TzHaar wonders whether the player's sweat is a leak](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/tzhaar-sweat.png)

## Who says it, and how

Everyone sounds like where they're from. Gnomes call you human, dwarves judge your metalwork, the
Fremennik want to know if you've earned it, goblins argue about armour colour, and Varlamore says
Nilsal. Every native word comes from the game itself, and every bit of gossip was checked against
the OSRS Wiki, so you can be quietly corrected on lore by a shopkeeper.

![Tortugans on the Great Conch talk about their spears and remembrance baskets](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/tortugans.png)

Some folk you'll need help to understand at all:

| Who | Without | With |
| --- | --- | --- |
| Ghosts | "Woooo" | A Ghostspeak amulet |
| Monkeys | "Ook" | A greegree or M'speak amulet |
| Cats | "Meow" | A Catspeak amulet |
| Camels | Camel noises | A Camulet |
| Mos Le'Harmless pirates | Pure jargon | A Book o' piracy in your pack |

![A ghost says only "Oooooh! Woooo!"](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/ghost-babble.png)

![With a Ghostspeak amulet, the ghost is delighted to meet a warm-blood with manners](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/ghost-understood.png)

Bankers greet you once per visit, in the local tongue, then fill you in on the news while you
rummage for your prayer potions.

![The Shilo Village banker welcomes the player, Bwana](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/shilo-bank.png)

![The tortugan banker welcomes the player to the Conch bank](https://raw.githubusercontent.com/mrgnomnom/chatty-npcs/main/docs/conch-bank.png)

## Who talks

Only people you could stop for a chat: they need a Talk-to option, and anything you can hit stays
quiet. Except city guards on watch, who relay the news, and goblins, who simply cannot help
themselves.

Comments float above their heads, and can also turn up in your chatbox as `[NPC] Name: words`, so
nobody mistakes Hans for a real person.

## Settings

**How often**: time between comments, earshot, chattiness, and how long before the same person or
subject comes up again.

**What they talk about**: mood (Kind, Balanced or Rude; gossip comes up regardless), how much of it
is quest gossip, how thick the local flavour is (0% for plain English, 200% for laid on with a
trowel), and whether city guards and bankers join in.

**Display**: show in the chatbox, and make it look like public chat.

**Debug**: 5 second cooldowns and 100% chattiness without touching your settings, plus pretend
quests and stats, and a sidebar panel with **Make someone talk** and **Describe my character**.

## For the meddling sort

Everything they say lives in `src/main/resources/com/chattynpcs/` as JSON: gear, pets and looks in
`chatter.json`, quest news in `quests.json`, map areas in `regions.json`, and how each people and
place talks in `speech.json`. Read `TONE.md` before writing a word, `WRITING_LINES.md` for how, and
`LANGUAGES.md` for which words exist.

`./gradlew test` checks every line against the rules (length, no numbers, no swearing, no quest
names, everything resolves) and writes `build/render.tsv`, a sample of what real people in real
places actually say, for reading the way a player would hear it.
