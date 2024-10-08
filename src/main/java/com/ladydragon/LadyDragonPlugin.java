package com.ladydragon;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import java.io.IOException;

@Slf4j
@PluginDescriptor(
		name = "Account Tracker",
		description = "Collects player data and sends it to a Spring Boot server."
)
public class LadyDragonPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private LadyDragonConfig config;
	private final OkHttpClient httpClient = new OkHttpClient();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Plugin activated!");

		String username = config.username();
		String password = config.password();

		if (!username.isEmpty() && !password.isEmpty()) {
			authenticateUser(username, password);
		} else {
			log.warn("Username or password not set in config.");
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Plugin stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		clientThread.invoke(() -> {
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer != null) {
				String playerName = localPlayer.getName(); // Ensure this is not null
				SkillingService skillingService = new SkillingService(playerName, client);
				sendPlayerSkillDataToServer(skillingService);
			}
		});
	}
	private void sendPlayerSkillDataToServer(SkillingService skillingService) {
		// Debugging log to check values before sending
		String playerName = skillingService.whoOwnsThis();
		System.out.println("Sending skill data for player: " + playerName);

		// Convert skill data to JSON
		String json = skillingServiceToJson(skillingService);

		// Debugging log to check the JSON string before sending
		System.out.println("Skill data JSON: " + json);

		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		Request request = new Request.Builder()
				.url(config.serverUrl() + "/playerdata")  // Adjust endpoint as needed
				.post(body)
				.build();

		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				// Enhanced debugging log on failure
				System.err.println("Failed to send skill data for player: " + playerName);
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				// Enhanced debugging log on response
				try {
					if (!response.isSuccessful()) {
						System.err.println("Failed to send skill data for player: " + playerName + ", Unexpected code: " + response.code());
						throw new IOException("Unexpected code " + response);
					}
					System.out.println("Skill data sent successfully for player: " + playerName);
				} finally {
					// Ensure the response is closed to prevent resource leaks
					response.close();
				}
			}
		});
	}


	// Helper function to convert SkillingService data to JSON format
	private String skillingServiceToJson(SkillingService skillingService) {
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");
		jsonBuilder.append("\"playerName\":\"").append(skillingService.whoOwnsThis()).append("\",");

		int[] skillData = (int[]) skillingService.get();  // Cast to int[]
		String[] skillNames = {
				"agility_xp", "attack_xp", "construction_xp", "cooking_xp", "crafting_xp", "defence_xp",
				"farming_xp", "firemaking_xp", "fishing_xp", "fletching_xp", "herblore_xp", "hitpoints_xp",
				"hunter_xp", "magic_xp", "mining_xp", "prayer_xp", "ranged_xp", "runecraft_xp",
				"slayer_xp", "smithing_xp", "strength_xp", "thieving_xp", "woodcutting_xp"
		};

		for (int i = 0; i < skillData.length; i++) {
			jsonBuilder.append("\"").append(skillNames[i]).append("\":").append(skillData[i]);
			if (i < skillData.length - 1) {
				jsonBuilder.append(",");
			}
		}
		jsonBuilder.append("}");

		// Log the JSON string for debugging
		System.out.println("Constructed Skill Data JSON: " + jsonBuilder.toString());

		return jsonBuilder.toString();
	}

	private void authenticateUser(String username, String password) {
		String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		Request request = new Request.Builder()
				.url(config.serverUrl() + "/authenticate")  // Adjust endpoint as needed
				.post(body)
				.build();

		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				log.error("Authentication failed: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try {
					if (!response.isSuccessful()) {
						throw new IOException("Unexpected code " + response);
					}
					System.out.println("Skill data sent successfully");
					// Optionally, read the response body if needed
					String responseBody = response.body().string();
					System.out.println("Response from server: " + responseBody);
				} finally {
					// Ensure the response body is closed to prevent leaks
					response.close();
				}}
		});
	}

	@Provides
	LadyDragonConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LadyDragonConfig.class);
	}
}
