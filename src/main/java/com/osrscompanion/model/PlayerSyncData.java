package com.osrscompanion.model;

import java.util.List;
import java.util.Map;

/**
 * Top-level data model written to local JSON sync files.
 * Serialized to JSON via Gson.
 */
public class PlayerSyncData
{
	public int schemaVersion = 1;
	public String lastUpdated;

	public PlayerInfo player;
	public Map<String, SkillEntry> skills;
	public BankData bank;
	public List<InventoryItem> inventory;
	public Map<String, ItemEntry> equipment;
	public List<QuestEntry> quests;
	public Map<String, DiaryRegion> achievementDiaries;
	public CombatAchievementData combatAchievements;

	public static class PlayerInfo
	{
		public String username;
		public int combatLevel;
		public int world;

		public PlayerInfo(String username, int combatLevel, int world)
		{
			this.username = username;
			this.combatLevel = combatLevel;
			this.world = world;
		}
	}

	public static class SkillEntry
	{
		public int level;
		public int xp;

		public SkillEntry(int level, int xp)
		{
			this.level = level;
			this.xp = xp;
		}
	}

	public static class ItemEntry
	{
		public int itemId;
		public String name;
		public int quantity;

		public ItemEntry(int itemId, String name, int quantity)
		{
			this.itemId = itemId;
			this.name = name;
			this.quantity = quantity;
		}
	}

	public static class InventoryItem extends ItemEntry
	{
		public int slot;

		public InventoryItem(int itemId, String name, int quantity, int slot)
		{
			super(itemId, name, quantity);
			this.slot = slot;
		}
	}

	public static class BankTab
	{
		public int tabIndex;
		public List<ItemEntry> items;

		public BankTab(int tabIndex, List<ItemEntry> items)
		{
			this.tabIndex = tabIndex;
			this.items = items;
		}
	}

	public static class BankData
	{
		public int totalItems;
		public List<BankTab> tabs;

		public BankData(int totalItems, List<BankTab> tabs)
		{
			this.totalItems = totalItems;
			this.tabs = tabs;
		}
	}

	public static class QuestEntry
	{
		public String name;
		public String displayName;
		public String state;

		public QuestEntry(String name, String displayName, String state)
		{
			this.name = name;
			this.displayName = displayName;
			this.state = state;
		}
	}

	public static class DiaryRegion
	{
		public boolean easy;
		public boolean medium;
		public boolean hard;
		public boolean elite;

		public DiaryRegion(boolean easy, boolean medium, boolean hard, boolean elite)
		{
			this.easy = easy;
			this.medium = medium;
			this.hard = hard;
			this.elite = elite;
		}
	}

	public static class CombatAchievementData
	{
		public boolean easyComplete;
		public boolean mediumComplete;
		public boolean hardComplete;
		public boolean eliteComplete;
		public List<String> completedTasks;

		public CombatAchievementData(
			boolean easyComplete, boolean mediumComplete,
			boolean hardComplete, boolean eliteComplete,
			List<String> completedTasks)
		{
			this.easyComplete = easyComplete;
			this.mediumComplete = mediumComplete;
			this.hardComplete = hardComplete;
			this.eliteComplete = eliteComplete;
			this.completedTasks = completedTasks;
		}
	}
}
