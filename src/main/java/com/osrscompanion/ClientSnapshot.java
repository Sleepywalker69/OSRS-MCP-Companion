package com.osrscompanion;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable snapshot of client state captured on the client thread,
 * safe to read from any thread (AWT, timer, etc.).
 */
public final class ClientSnapshot
{
	public final boolean loggedIn;
	public final String playerName;
	public final int combatLevel;
	public final int worldX;
	public final int worldY;
	public final int plane;
	public final int world;
	public final int tickCount;

	public final int hp;
	public final int maxHp;
	public final int prayer;
	public final int maxPrayer;
	public final int runEnergy;
	public final int specEnergy;

	public final int[] skillExperience;
	public final int[] realLevels;
	public final int[] boostedLevels;

	private ClientSnapshot(boolean loggedIn, String playerName, int combatLevel,
		int worldX, int worldY, int plane, int world, int tickCount,
		int hp, int maxHp, int prayer, int maxPrayer, int runEnergy, int specEnergy,
		int[] skillExperience, int[] realLevels, int[] boostedLevels)
	{
		this.loggedIn = loggedIn;
		this.playerName = playerName;
		this.combatLevel = combatLevel;
		this.worldX = worldX;
		this.worldY = worldY;
		this.plane = plane;
		this.world = world;
		this.tickCount = tickCount;
		this.hp = hp;
		this.maxHp = maxHp;
		this.prayer = prayer;
		this.maxPrayer = maxPrayer;
		this.runEnergy = runEnergy;
		this.specEnergy = specEnergy;
		this.skillExperience = skillExperience;
		this.realLevels = realLevels;
		this.boostedLevels = boostedLevels;
	}

	public static ClientSnapshot capture(Client client)
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		if (!loggedIn)
		{
			return new ClientSnapshot(false, null, 0,
				0, 0, 0, 0, client.getTickCount(),
				0, 1, 0, 1, 0, 0,
				new int[Skill.values().length],
				new int[Skill.values().length],
				new int[Skill.values().length]);
		}

		Player local = client.getLocalPlayer();
		String name = (local != null && local.getName() != null) ? local.getName() : "Unknown";
		int combat = local != null ? local.getCombatLevel() : 0;

		int wx = 0, wy = 0, pl = 0;
		if (local != null)
		{
			WorldPoint wp = local.getWorldLocation();
			if (wp != null)
			{
				wx = wp.getX();
				wy = wp.getY();
				pl = wp.getPlane();
			}
		}

		int world = client.getWorld();
		int tick = client.getTickCount();

		int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
		int run = client.getEnergy() / 100;
		int spec = client.getVarpValue(48) / 10;

		Skill[] skills = Skill.values();
		int[] xp = new int[skills.length];
		int[] real = new int[skills.length];
		int[] boosted = new int[skills.length];
		for (int i = 0; i < skills.length; i++)
		{
			if (skills[i] == Skill.OVERALL) continue;
			xp[i] = client.getSkillExperience(skills[i]);
			real[i] = client.getRealSkillLevel(skills[i]);
			boosted[i] = client.getBoostedSkillLevel(skills[i]);
		}

		return new ClientSnapshot(true, name, combat,
			wx, wy, pl, world, tick,
			hp, maxHp, prayer, maxPrayer, run, spec,
			xp, real, boosted);
	}

	public int getSkillExperience(Skill skill)
	{
		return skillExperience[skill.ordinal()];
	}

	public int getRealLevel(Skill skill)
	{
		return realLevels[skill.ordinal()];
	}

	public int getBoostedLevel(Skill skill)
	{
		return boostedLevels[skill.ordinal()];
	}
}
