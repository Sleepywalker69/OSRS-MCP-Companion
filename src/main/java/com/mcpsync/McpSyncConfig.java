package com.mcpsync;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("mcpsync")
public interface McpSyncConfig extends Config
{
	@ConfigSection(
		name = "Storage",
		description = "Local file storage settings",
		position = 0
	)
	String storageSection = "storage";

	@ConfigItem(
		keyName = "syncIntervalSeconds",
		name = "Save Interval (seconds)",
		description = "How often to save updated data to disk (minimum 30)",
		section = storageSection,
		position = 0
	)
	default int syncIntervalSeconds()
	{
		return 60;
	}

	@ConfigSection(
		name = "Data",
		description = "Choose what data to sync",
		position = 1
	)
	String dataSection = "data";

	@ConfigItem(
		keyName = "syncSkills",
		name = "Sync Skills",
		description = "Include skill levels and XP",
		section = dataSection,
		position = 0
	)
	default boolean syncSkills()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncBank",
		name = "Sync Bank",
		description = "Include bank contents (captured when bank is opened)",
		section = dataSection,
		position = 1
	)
	default boolean syncBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncInventory",
		name = "Sync Inventory",
		description = "Include current inventory contents",
		section = dataSection,
		position = 2
	)
	default boolean syncInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncEquipment",
		name = "Sync Equipment",
		description = "Include currently worn equipment",
		section = dataSection,
		position = 3
	)
	default boolean syncEquipment()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncQuests",
		name = "Sync Quests",
		description = "Include quest completion status",
		section = dataSection,
		position = 4
	)
	default boolean syncQuests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncDiaries",
		name = "Sync Achievement Diaries",
		description = "Include achievement diary completion",
		section = dataSection,
		position = 5
	)
	default boolean syncDiaries()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncCombatAchievements",
		name = "Sync Combat Achievements",
		description = "Include combat achievement tasks",
		section = dataSection,
		position = 6
	)
	default boolean syncCombatAchievements()
	{
		return true;
	}
}
