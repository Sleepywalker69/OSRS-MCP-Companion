package com.mcpsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcpsync.model.PlayerSyncData;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes player data as JSON to a local file in ~/.runelite/mcp-sync/.
 */
@Slf4j
public class PlayerDataWriter
{
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final File syncDir;

	public PlayerDataWriter()
	{
		this.syncDir = new File(System.getProperty("user.home"), ".runelite/mcp-sync");
	}

	/**
	 * Write player data to a local JSON file.
	 * File is written to ~/.runelite/mcp-sync/{username}.json
	 *
	 * @return true if write succeeded, false otherwise
	 */
	public boolean write(PlayerSyncData data)
	{
		if (data.player == null || data.player.username == null)
		{
			log.debug("MCP Sync: No player data available, skipping write");
			return false;
		}

		if (!syncDir.exists() && !syncDir.mkdirs())
		{
			log.warn("MCP Sync: Failed to create sync directory: {}", syncDir.getAbsolutePath());
			return false;
		}

		String filename = data.player.username.toLowerCase().replaceAll("[^a-z0-9_-]", "_") + ".json";
		File outputFile = new File(syncDir, filename);
		String json = GSON.toJson(data);

		try (FileWriter writer = new FileWriter(outputFile))
		{
			writer.write(json);
			log.debug("MCP Sync: Saved data for {} to {}", data.player.username, outputFile.getAbsolutePath());
			return true;
		}
		catch (IOException e)
		{
			log.warn("MCP Sync: Failed to write data file", e);
			return false;
		}
	}
}
