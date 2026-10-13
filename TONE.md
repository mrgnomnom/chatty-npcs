# Chatty NPCs: tone guide

How the locals should sound. Every line in `chatter.json` and `quests.json` should follow this. It's
based on what we've agreed, and on research into how NPCs talk across 21 quests (novice to
grandmaster) and about 40 individual NPCs' dialogue on the OSRS Wiki.

## North star: world immersion

The whole point of Chatty NPCs is to make Gielinor feel more alive: a world that notices you, talks
about what you've done, and gets on with its own business around you. Every rule below serves that.

When a choice isn't covered here, or two rules pull against each other, pick whichever keeps the
player in the world. A line that's funny but breaks the spell (it knows something it shouldn't,
sounds like our world, or gets the lore wrong) is worse than a quieter line that feels true.

## What a line is

- Something a Gielinor local says as you walk past them. It's a remark they make on the spur of the
  moment. It isn't narration, and it isn't the game telling you something.
- Short enough to read in a speech bubble: one or two short sentences, ideally under 60 characters
  and never over 70.
- British the way the game is British: plain, clear, slightly old-fashioned English. Dry,
  class-aware, silly but sensible. British spelling (armour, colour).

## The game's voice

OSRS NPCs make dry, sensible remarks about absurd things ("I have four children and a goat to look
after"; "Others say it's just a sheep"). Their comedy is deadpan, class comedy, mild puns, gentle
mockery of adventurers (forgetful, fetching and carrying, poking about in sheds), and the odd wink
at the player. Nobody swears. Everything they mention exists in Gielinor.

**The goat test:** could this line be said by someone who has never left Gielinor and has never seen
a television?

**Rules**

- Plain English first. At most one dialect marker per line, and only where the game uses one
  (pirates' "Arr"/"ye", Varrock street "guv", gnomes' "human", the Fremennik's "outerlander").
- The joke is a sensible reaction to something absurd, or an absurd reaction delivered sensibly.
- Use the game's own ways of addressing you: adventurer, traveller, friend, my friend, citizen
  (royals), human (gnomes), outerlander (Fremennik), guv (Varrock street), dear (older women), "love"
  only occasionally from barkeeps.
- Reference in-world things: Saradomin, Guthix, goblins, the Duke, the Varrock Herald, Karamja rum,
  the sewers, the Party Room, the Wizards' Tower.
- Period exclamations: Blimey, Cor, Good heavens, Ye gods, Gadzooks, By Guthix.
- Gossip the way the game does: "I hear...", "Word is...", "They say...", "We received word from...".

## They only know what they could plausibly know

The comments should feel observant, not clairvoyant. A local can remark on:

- **What they can see:** your worn gear and weapon, your cape, your hair (or lack of it), being
  starkers, mismatched armour, looking flash or skint, looking hurt (low HP), being out of breath
  (low run energy), your pet, where you are (e.g. the Wilderness).
- **What they've heard:** finished quests spread as rumour. Big deeds (grandmasters, Recipe for
  Disaster) travel everywhere. Others are known in the city they happened in, or near the quest
  start if it isn't in a city.
- **What they lived through themselves:** a quest giver remembers you helped them, or is still
  waiting for help.

**No numbers about you, ever.** No combat level, total level, skill levels, XP or gold value in any
line. Describe what the number would look like instead: "you look like you could handle yourself",
"that's a castle's worth of armour".

## Never name the quest

Quest titles are player language. NPCs talk about what happened: the people, monsters, places and
objects ("are you the one who did for Galvek?", "the Duke's birthday cake", "Count Draynor"), never
"Dragon Slayer II" or "Cook's Assistant". The same goes for other player language: "free
experience", "the big one", "maxed", "total level", "guide".

## Who says what about quests

| Situation | How it sounds | Example |
| --- | --- | --- |
| Someone else's quest, finished | News spreading, rumour, hearsay | "Word is you staked Count Draynor. Draynor sleeps easy now." |
| Someone else's quest, not done | Gossip about the trouble | "Doric's still waiting on clay. It's clay. It just lies there." |
| Their own quest, finished | A familiar face in passing, who knows you and remembers | "Wipe your feet, egg-fetcher. I've just mopped." |
| Their own quest, not done | Muttering to themselves about their trouble, or flagging you down for help | "Eggs... milk... flour... the Duke's cake... I'm done for." |

Quest givers would much rather talk about their own quest than anything else.

**How far news travels depends on the quest's tier:**

| Tier | Who can bring it up |
| --- | --- |
| Novice, intermediate, experienced | Only NPCs near where the quest starts |
| Master | Any NPC in its city or region (`regions.json`) |
| Grandmaster (and Recipe for Disaster) | Anyone anywhere, but locals in its region far more often |

This covers both finished deeds and unfinished troubles.

**What people know depends on where they are.** News gathers where people gather and dries up
where they're cut off:

- **The Grand Exchange hears everything.** It's where the whole world comes to trade, so clerks,
  bankers and traders there can pass on any quest news from anywhere, whatever its tier.
- **Burgh de Rott barely knows anything.** It's poor, cut off and mostly can't read, so locals there
  only hear Morytania's own news, never far-off deeds, not even the grandest. They talk plainly,
  can't read the notices, and get their news by word of mouth.

**Languages and accents.** Who you are comes before where you are. Races with a tongue of their
own keep it wherever they stand: TzHaar call you "JalYt" and pay in TokKul, gnomes drop the odd
Gnomish word ("Sarko!", "lemantolly"), dwarves say "human" and "By Guthix", the Fremennik say
"outerlander". Anyone else takes on the accent of where they're standing: Canifis's "stranger",
Darkmeyer's "human", the elves' "By Seren", the desert's "effendi", Karamja's "Bwana", Varlamore's
"Nilsal", "Tetamo" and "Kuani". Kourend's houses each have their own: Arceuus's formal
"Greetings, traveller", Piscarilius's "outsider" and word on the streets, Shayzien's "citizen",
Lovakengj's "human". Places the game gives no distinct voice (the Misthalin and Asgarnia
heartlands, Kingstown, Kebos) stay plain.

Most races speak the common tongue in-game, so a native word is seasoning, never the meal: one at
most, and only where its meaning is clear from the line. Some you can't understand at all: monkeys
"Ook" unless you're wearing an M'speak amulet or a monkey greegree, ghosts "Woooo" unless
you wear a Ghostspeak amulet (or Morytania legs 2 and up), and skavids only ever speak Skavid. Never
invent words: the Moon Clan, vampyres, elves and the Fremennik have no attested vocabulary, so they
get none. Rules live in `speech.json`, each with in-game quotes as evidence, and `LANGUAGES.md` lists
every attested word.

**Regional news:** the big stories (grandmasters and the major storylines) are talked about most in
the region where they happened, about three times as often as elsewhere. Morytania talks about its
vampyres, Prifddinas about Seren, the gnomes about Glough. Locals there don't all agree: some are
glad of what you did, and some (vampyre sympathisers, Iorwerth loyalists, Glough's supporters) are
bitter about it.

**Gossip** is news overheard rather than said to you ("Did you hear? The vampyres are..."). It's its
own tone, and it comes up in every mood, because it isn't aimed at you. A recognition line ("Wait,
are you the one who...?") is a compliment, and a sympathiser's dig is an insult.

## Mood

- **Kind:** compliments only. Warm, chuffed, affectionate, sincere. Never gushing or American
  ("awesome", "you absolute legend").
- **Rude:** insults only. Clever and cheeky rather than blunt; it should feel like having the mickey
  taken out of you, not like being hated. Avoid flat verdicts ("Pathetic.", "Useless, you.",
  "Tragic.", "Typical.") that just call you names. Do what the game does:
  - a sarcastic frame ("Good job you're not the King")
  - a misattribution ("You're not the one who keeps leaving my gates open?")
  - a withering comparison ("a do-gooder who fetches and carries for smarter people")
  - an impossible condition ("the day the skies turn red...")
  - deflating the hero ("Plenty of other adventurers about besides yourself")
  - a cheerful afterthought ("Sorry, I only had the one quest")
  - politeness as a weapon ("Do not think me rude, but...")

  The wit is the insult.
- **Balanced:** a mix of both, plus neutral remarks (muttering, observations, small talk).
- **Gossip** (any mood): news overheard, not aimed at you.
- **Grave deeds stay grave.** Grandmaster deeds (Galvek, Lucien, the Fallen Empire, Prifddinas) get
  awe or sober gratitude. Even Rude mood only manages understatement about them ("You'd never know to
  look at you"). Proper teasing is for small and mid-level deeds (eggs, sheep, imps, a prince in a
  skirt).

## Personas

Each persona is a voice. They share most lines, but add their own openers and closers, lean
towards certain tones, and each has a ceiling on how rude they can be.

| Persona | Who | Voice (grounded in game NPCs) | Rude ceiling |
| --- | --- | --- | --- |
| local | Anyone not matched below | Random Man/Woman: weather, goblins, the king, "I'm in a hurry" | Medium, curt |
| toff | Dukes, kings, lords, sirs, squires | Duke Horacio, King Roald: "citizen", "the peasantry", "Good heavens" | Medium-high, snobbish |
| trader | Shopkeepers, merchants | Zaff, Ali Morrisane, Benny: "my friend", "an absolute bargain", puns; "guv" in Varrock only | Low-medium, salesy |
| barkeep | Bartenders, barmaids, innkeepers, cooks | Blue Moon bartender, Emily: "What can I do yer for?", gossip, "love" sparingly | Medium, dry |
| banker | Bankers, clerks | Greets you once per visit in the local tongue ("Welcome, human, to Ta Quir Priw bank!"), then passes on quest news: the local trouble still unsolved first, or news from afar, half and half. Only small talk if there's no news | Very low |
| nan | Old women, witches | Gertrude, Aggie, Almera: "dear", fussing, fond threats of toad-shape | Low, fond |
| vicar | Priests, monks, brothers | Father Aereck, monks: "Greetings traveller", "Saradomin smile on you"; Urhney-style grump allowed | Low |
| kid | Children, urchins, gnome children | Hudon, Benny, urchins: literal, mercenary, cheeky, excitable (exclamation marks, never capitals). Own lines only | High, innocent |
| guard | City guards (see Who can talk) | Black Guard, Fortress Guard: "Stay out of trouble", "Keeping an eye on you", official word relayed flat | Medium, officious |
| wizard | Wizards, archmages | Grayzag, Traiborn: condescending or dotty | Medium, superior |
| rough | Farmers, fishermen, miners, sailors, smiths | Fred, Ned, Gerald, Oziach: gruff, suspicious, "ye", "Blimey!" | Medium-high, earthy |
| shady | Villains, gang members, Zamorakians | Glough, Mage of Zamorak, Katrine: cold contempt, "your sort", "meddler" | High, cold |
| dwarf | Dwarves | Doric, Thurgo, Nurmof: craft pride, pie, beer, "By Guthix!" | Low-medium |
| gnome | Gnomes | Narnode, Daero: "human", formal or whimsical | Low |
| fremennik | Rellekka folk | "outerlander" before the trials, "brother"/"sister" after; boasts | Medium, honour |
| pirate | Pirates, sea dogs | Redbeard Frank: "Arr", "ye", "matey", rum | Medium, bawdy |
| burgher | Anyone in Burgh de Rott | Poor, plain-spoken, can't read: "Can't read it. Looked important." Own lines only, plus local gossip | Plain, wry |

Shared insult lines carry a temperament, so sharp lines never reach polite personas:

- **polite:** banker, vicar, nan, trader, gnome, dwarf
- **plain:** local, barkeep, kid, fremennik, pirate, rough
- **sharp:** toff, wizard, shady, guard

Blunt words like "pathetic" belong to shady characters only. Exasperated royals can manage
"imbecile", wrapped in a sentence that does some work.

## British flavour

- Understatement ("Brave choice."), literal-mindedness ("Others say it's just a sheep."), passive
  politeness ("Do not think me rude, but...").
- Everyday words we keep, because they're timeless and the user loves them: blimey, chuffed,
  knackered, skint, proper, faff, dog's dinner, in the wars, face like a wet weekend.
- Tea, pints, weather, queues, the Party Room, the Varrock Herald.
- **Banned:**
  - swearing of any kind ("bloody", "arse", "can't be arsed")
  - modern slang and catchphrases ("muppet", "just saying", "no offence", "lovely jubbly", "sweet as
    a nut", "on my life", "oi oi")
  - Northern terms of endearment ("pet", "duck", "petal", "ta-ra"); "love" only occasionally from
    barkeeps
  - anything from our world: telly, radio, PE, lycra, raves, insurance, CVs, fivers, tenners, cash,
    charity shops, stag dos, wheelie bins, film references, calendar years, weekday names, "guides", "last decade"
  - "cute" as an insult, and Americanisms in general

## Gear and pets

**Gear should surprise and delight.** People notice specific, recognisable pieces and react to what
each says about you, not just its tier:

- **Wealth and flash:** trimmed (t), gold-trimmed (g), heraldic (h1-h5), gilded, ornament kits (or),
  anything third age.
- **Physical impressions:** obsidian looks heavy, Fremennik gear looks cold, crystal is see-through,
  Barrows armour looks like it's seen things.
- **Cosmetics and oddities:** event and random event outfits (mime, zombie, lederhosen, camo), the
  Bob shirts, a powdered wig, pirate gear. These should get the most delighted reactions.
- **Lots of variety:** many specific pieces, each with only a few lines, beats a few pieces with
  lots of lines.

**Name only what a passer-by would know.** A stranger sees "old armour" or "a grand cape", not
"Dharok's platebody". Use the item's name only when it's obvious at a glance (a red partyhat, a
rubber chicken, a twisted bow). Otherwise describe what they see: "You dug that gear out of a grave,
didn't you?"

**Pets get noticed.** People react to the pet following you, recognised by name. Examine text isn't
available to plugins (the server only sends it when you click Examine), so pet lines are
hand-written.

Use the examine text as **inspiration, not a script.** A passer-by has never read it, so they
shouldn't quote it back at you ("loves to nibble, does it?" makes no sense from a stranger). Borrow
its idea when it fits naturally, and otherwise react to something they can actually see about the
pet: what it looks like, what it's doing, what it might do to them. The reaction matches the pet's
nature: fearsome boss pets make people nervous; cute or silly pets get fondness or bafflement.

**Match the examine register**, though: idioms taken literally ("Found somewhere between a rock and a
hard place"), deflating fearsome things (TzRek-Jad: "This is not going to hurt... but it might
tickle."), dry bathos on silly items (lederhosen hat: "A hat with a goat's hair attached."), and the
occasional item that answers back (camo top: "Examine what?").

### Pet reference

The examine text is there for inspiration. The example lines react to the pet itself.

| Pet | What it is | Examine | Reaction | Example line |
| --- | --- | --- | --- | --- |
| TzRek-Jad | Tiny lava beast from the Fight Caves | This is not going to hurt... but it might tickle. | intimidated | "Is that thing smouldering? Keep it off the thatch." |
| Jal-nib-rek | Little Inferno nibbler | It loves to nibble. | intimidated | "Does it bite? It looks like it bites." |
| Olmlet | Baby Great Olm, a pale cave lizard | The cuddly Spawn of the Guardian in the Deep. | intimidated | "What is that? Some cave lizard? It's staring at me." |
| Vorki | Baby undead blue dragon | Three legs on my dragon... | fond | "A little dragon! Mind it doesn't breathe on anything." |
| Pet chaos elemental | Swirling ball of chaos | D'aw look at the liddle... | baffled | "What is that? It keeps changing its mind." |
| Little nightmare | Miniature Nightmare of Ashihama | Quite the little nightmare. | intimidated | "Keep that away from me. I'll not sleep tonight." |
| Prince black dragon | Three-headed baby black dragon | Not quite the full royalty yet. | intimidated | "Three heads on a baby dragon. That's two more than I'd like." |
| Lil' Zik | Small vampyric spider | What has eight legs and runs a Vampyric Theatre? | intimidated | "There's a spider following you. A big one. Just so you know." |
| Ikkle hydra | Baby many-headed hydra | How does it not fall over? | baffled | "All those heads. How does it not fall over?" |
| Rocky | Raccoon | Raccoons, like pandas but worse. | baffled | "Keep an eye on that raccoon. It's eyeing my purse." |
| Heron | Wading bird | Smells a little fishy. | fond | "That bird's eyeing the fish stall, you know." |
| Beaver | Beaver | Looks like it's gotten through a lot of wood. | fond | "That beaver's looking at my fence. Don't let it." |
| Baby chinchompa | Fluffy explosive rodent | Fluffy and cute, keep away from fire! | fond | "Ooh, fluffy! Don't those go bang?" |
| Giant squirrel | Very large squirrel | A giant squirrel with beautiful markings. | baffled | "That's a big squirrel. Nobody else mentioning how big that is?" |
| Rock golem | Little walking rock | Found somewhere between a rock and a hard place. | baffled | "Your rock's following you. Is it meant to do that?" |
| Rift guardian | Floating rift from the Abyss | An abyssal rift guardian. | intimidated | "Is that thing floating? Things shouldn't float." |
| Tangleroot | Little walking plant | Don't be hasty. | fond | "Your plant's following you. Plants don't usually do that." |
| Gull | Seagull | Quite the scrongly little chicken. | fond | "Is that scrongly little chicken following you?" |
| Baby mole | Mole | Keep Molin', molin' molin' molin'! | fond | "A mole! Shouldn't it be underground?" |
| Pet General Graardor | Tiny ourg general, always shouting | Totally inaudible. | baffled | "It's shouting something. Can't hear a word." |
| Hellpuppy | Fiery hellhound pup | A fiery little pup! | intimidated | "Your dog's on fire. Is that normal?" |
| Herbi | Boar covered in herbs | A boar with an impressive mane of dried herbs. | fond | "That boar's covered in herbs. Are you planning to eat it?" |

The Gull line keeps "scrongly" because it was the user's own example, and it works as a word a
local might use. The Ikkle hydra line keeps its examine idea because anyone would wonder the same.

Gear examines worth borrowing from: third age ("Fabulously ancient armour beaten from magical
silver."), obsidian platebody ("Provides excellent protection."), mime ("A mime would wear this."),
lederhosen hat ("A hat with a goat's hair attached."), Bob's red shirt ("Bob says: Never give your
password out to anyone."), powdered wig ("A big do about nothing."), camo top ("Examine what?"),
zombie head ("Alas...I hardly knew him."), crystal body ("Sturdier than it looks.").

## Who can talk

Only NPCs you could stop for a chat: they have a Talk-to option, can't be attacked, and aren't a
pet.

**Exception: city guards.** Guards are attackable and have no Talk-to option, but a guard
inside a city may speak while they aren't fighting anyone. Guards talk like the town watch on a slow
shift: short imperatives, official word relayed flat ("Word from the palace..."), suspicion of
anyone armed, irritation at anyone unclothed. They mostly comment on the city's quest news, and on
anything a watchman would eye: someone geared to the teeth or carrying a menacing weapon (a possible
threat), someone mostly naked (a public nuisance), or anything else suspicious.

## Decisions

- **Britishness:** "Jagex British", plus the timeless slang above. No modern regional sitcom voice.
- **Swearing:** none.
- **Numbers:** none about the player, in any line.
- **Personas:** all 16 above, each with a rude ceiling and a temperament.
- **Address:** "adventurer"/"traveller" by default, with persona-specific words as listed.
- **Setting:** in-world nouns, British attitude. Tea, pubs, Christmas and Halloween exist in Gielinor.
  Anything that needs electricity, a bank card, a calendar year or a TV schedule doesn't.
