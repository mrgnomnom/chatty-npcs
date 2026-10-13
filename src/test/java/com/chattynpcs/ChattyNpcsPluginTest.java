package com.chattynpcs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ChattyNpcsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChattyNpcsPlugin.class);
		RuneLite.main(args);
	}
}
