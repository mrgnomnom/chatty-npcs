# Chatty NPCs

The locals pass comment on your gear, your deeds and your haircut as you walk by. Terribly British
about it, except where they aren't British at all.

> "Iron. We use that for horseshoes, human." *(a dwarf)*
>
> "Little JalYt. Tz-Kih would eat you. Tz-Kih is a bat." *(TzHaar)*
>
> "Blue armour. Blue make Grubfoot sad. Make me sad too, looking." *(a goblin)*
>
> "Welcome, human, to Ta Quir Priw bank!" *(the Grand Tree's banker)*
>
> "Still on abyssal demons, I see. Don't let me keep you." *(Vannaka)*

## What they notice

- **Your gear**: around 300 items from bronze to the Tumeken's shadow, described the way a
  passer-by would see them ("That's a tooth on a handle. Keep it pointed at the ground.")
- **Your pet**: every pet in the game
- **Your looks and state**: bald, starkers, skint, hurt, out of breath, dripping in wealth
- **Your deeds**: news of the quests you've done (and the trouble you haven't sorted yet)
  travels as gossip. Small quests are only talked about nearby, master quests in their region, and
  grandmasters everywhere. Quest givers remember you in the first person, and nobody names a quest
- **Your slayer task**, if a slayer master is the one talking

## How they sound

Everyone speaks in their own voice. Gnomes, dwarves, the Fremennik, the Moon Clan and tortugans each
have their own way of talking and their own sayings, and the places of Gielinor have their accents:
Canifis's "stranger", the desert's "effendi", Varlamore's "Nilsal", Kourend's five houses, the
Pandemonium's pirates. Goblins, ogres, trolls, TzHaar, Tool Leprechauns and parrots only ever speak
in their own thick voice.

Some you can't understand at all without the right gear: monkeys say "Ook" unless you're wearing a
greegree or an M'speak amulet, ghosts say "Woooo" unless you're wearing a Ghostspeak amulet, cats
"Meow" without a Catspeak amulet, camels grunt without a Camulet, the pirates of Mos Le'Harmless
talk in jargon unless you're carrying a Book o' piracy, and skavids only ever speak Skavid. Bankers welcome you once per visit in the local tongue and pass on
the news while you bank.

Every native word and accent is taken from in-game dialogue (see `LANGUAGES.md`), and every bit of
quest news was checked against the OSRS Wiki.

## Who talks

Only NPCs you could stop for a chat: they need a Talk-to option, and anything you can attack (or
your pet) stays quiet. Two exceptions: city guards on watch, who pass on the city's news, and
goblins, who mutter at anyone when they aren't fighting.

Comments appear above the NPC's head for 9 to 18 seconds, and optionally in the chatbox.

## Settings

### How often

- **Time between comments**: the shortest gap between any two comments, from anyone
- **Earshot**: how close you have to walk past someone for them to comment
- **Chattiness**: once that time has passed, the chance each tick that someone nearby speaks up
- **Same person again after**: how long someone keeps quiet after commenting
- **Same subject again after**: how long before anyone brings up the same thing again

### What they talk about

- **Mood**: Kind only pays compliments, Rude only insults, Balanced is a bit of everything.
  Overheard gossip comes up in every mood
- **Quest gossip**: at most how much of what's said is quest news, so your gear and looks still
  get noticed (bankers always talk news)
- **Local flavour**: how strongly people sound like where they're from, from plain English
  everywhere (0%) to laid on thick (200%). Babble you can't understand stays babble
- **City guards**: guards on watch in a city pass comment too
- **Bank greetings**: bankers welcome you once per visit, in the local tongue

### Display

- **Show in chatbox**: also put comments in the chatbox
- **Look like public chat** (on by default): show them as "[NPC] Name: words" in your own public chat
  colours, rather than as a game message

### Debug

- **Debug mode**: 5 second cooldowns and 100% chattiness, for testing. It doesn't touch your
  settings: untick it and your own apply again
- **Pretend quests**: Real, None (nothing started) or Quest cape (everything done)
- **Pretend stats**: Real, Noob (looks like a fresh level 3) or Maxed (looks like a level 126)

While debug mode is on, a **Chatty NPCs** panel in the sidebar has two buttons:

- **Make someone talk**: the nearest chatty NPC says something now, ignoring cooldowns
- **Describe my character**: lists what the locals can see about you (and your hair kit id) in the
  chatbox

## Adding lines

Everything they say lives in `src/main/resources/com/chattynpcs/`: personas and gear, pet and
appearance topics in `chatter.json`, quest news in `quests.json`, map areas in `regions.json`, and
how each people and place talks in `speech.json`. Read `TONE.md` for how lines should sound and
`WRITING_LINES.md` for a step-by-step guide, including how to check the lore.

- **topics** are things people notice: a `fact` about you, an `item` regex against your worn gear,
  a `pet` regex against your follower's name, or a `quest` with where it's talked about (`near` the
  start, a `region`, or `anywhere`). Higher `priority` comes up more often
- **lines** are keyed by tone (`compliment`, `insult`, `remark`, `gossip`), optionally prefixed by a
  persona (`kid/insult`), a temperament (`plain/`, `sharp/`), `starter/` for quest givers, or
  `heartland/` for lines that only suit an ordinary town above ground. Placeholders: `{item}` (the
  worn item) and `{task}` (the slayer task)
- **personas** match NPC names to a voice: tone weights, openers and closers (which can be limited to
  some tones), and a temperament that decides how pointed their lines can be
- **speech** is how a race or place talks: what they call you, oaths, native words, babble, and
  sayings of their own, every entry backed by in-game quotes

Run `./gradlew test` after editing. Besides checking every line against the rules (length, no
numbers, banned words, valid keys, every quest and region exists), it writes
`build/render.tsv`, what real speakers in real places actually say, for reading the way a player
would hear it, and `build/language-share.txt`, how often each place sounds like itself.
