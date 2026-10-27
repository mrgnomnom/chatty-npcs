# Writing a named character's lines

Read `prompts/write.md` first. Everything there applies, with these differences.

You are writing what one particular person says as a stranger passes: Hans, the Cook, Reldo. Their
card is `linegen/characters/cards/<id>.json`; their facts are the `char.<id>.` ones in
`linegen/lore/`. Each cell is one of their `states` (the player's progress in their story) and a
tone.

- **They talk about their own life.** Their work, their worries, the people they know, what's
  happened to them, what they want. A passer-by overhears a person, not a quest log. The best line
  is one only this character could say.
- **The state is the moment.** In a `before_` state their trouble is current and the player hasn't
  helped. In an `after_` state it's over, and they may be grateful, changed, or moved on, and they
  remember it was this player who helped. In `always`, say nothing that a quest would make false.
- **No quest dialogue.** Don't ask the player to start a quest, give instructions, or repeat
  what they say when you talk to them. Their real lines are in the card's `canon` to show the
  voice; never copy or closely rework one.
- **Their voice, not their culture's.** A dwarf who's a person first: Thurgo loves pie before he
  loves rivets. Use the card's `rhythm`, `grammar` and `address`.
- **Facts.** Anything about their life or the world cites a fact, as in `write.md`. Their feelings
  and opinions need none, but must fit the card.
- Tones: `compliment` and `insult` are about the player as they pass, in this person's way; `remark`
  is about their own business. These lines play whatever the player is wearing, so never mention
  particular gear or looks: aim at the player as an adventurer, a stranger, a customer, or at what
  they did in this character's story.

Write 8 candidates per cell. Ids like `hans-always-remark-03`.
