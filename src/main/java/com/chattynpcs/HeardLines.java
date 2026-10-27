package com.chattynpcs;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Every line this account has heard, and when. Nobody says a line again for four hours after you've
 * heard it, from anyone: when someone has nothing fresh, they keep quiet. Kept between sessions.
 */
class HeardLines
{
	static final long REHEAR_MILLIS = TimeUnit.HOURS.toMillis(4);
	private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);

	/**
	 * When each line was heard, by a hash of the line as written
	 */
	private final Map<Long, Long> heardAt = new HashMap<>();
	private boolean changed;

	boolean recently(String template, long now)
	{
		Long at = heardAt.get(hash(template));
		return at != null && now - at < REHEAR_MILLIS;
	}

	void heard(String template, long now)
	{
		heardAt.put(hash(template), now);
		changed = true;
	}

	/**
	 * @return whether anything was heard since the last time this was asked
	 */
	boolean takeChanged()
	{
		boolean was = changed;
		changed = false;
		return was;
	}

	/**
	 * What's on disk: when each line was last heard, in minutes since the epoch, by its hash in hex
	 */
	static class Saved
	{
		Map<String, Long> heard = new HashMap<>();
	}

	/**
	 * What's worth keeping, for saving to disk
	 */
	Saved save(long now)
	{
		Saved saved = new Saved();
		heardAt.forEach((hash, at) ->
		{
			if (now - at < REHEAR_MILLIS)
			{
				saved.heard.put(Long.toHexString(hash), at / MINUTE);
			}
		});
		return saved;
	}

	/**
	 * Adds lines heard in an earlier session. Anything heard since logging in stays as it is.
	 */
	void load(Saved saved)
	{
		if (saved != null && saved.heard != null)
		{
			saved.heard.forEach((hash, minute) -> heardAt.putIfAbsent(Long.parseUnsignedLong(hash, 16), minute * MINUTE));
		}
	}

	/**
	 * FNV-1a, 64 bits: two lines sharing a hash would cost one line its turn, at worst
	 */
	private static long hash(String template)
	{
		long hash = 0xcbf29ce484222325L;
		for (byte b : template.getBytes(StandardCharsets.UTF_8))
		{
			hash ^= b & 0xff;
			hash *= 0x100000001b3L;
		}
		return hash;
	}
}
