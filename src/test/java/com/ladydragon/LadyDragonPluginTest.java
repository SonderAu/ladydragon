package com.ladydragon;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LadyDragonPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LadyDragonPlugin.class);
		RuneLite.main(args);
	}
}