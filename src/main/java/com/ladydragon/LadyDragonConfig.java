package com.ladydragon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface LadyDragonConfig extends Config
{
	@ConfigItem(
			keyName = "serverUrl",
			name = "Server URL",
			position = 0,
			description = "Enter the URL of the server to send player data."

	)
	default String serverUrl() {
		return "http://localhost:8080"; // Default URL, change as needed
	}
	@ConfigItem(
			keyName = "username",
			name = "Username",
			position = 1,
			description = "Enter the username for authentication."
	)
	default String username() {
		return ""; // Default username, change as needed
	}

	@ConfigItem(
			keyName = "password",
			name = "Password",
			position = 2,
			secret = true,
			description = "Enter the password for authentication."
	)
	default String password() {
		return ""; // Default password, change as needed
	}
}
