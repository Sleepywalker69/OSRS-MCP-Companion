package com.osrscompanion;

import java.util.HashMap;
import java.util.Map;

/**
 * Lookup table for commonly known varp and varbit IDs.
 */
public final class VarNames
{
	private static final Map<String, String> NAMES = new HashMap<>();

	static
	{
		// Varps
		varp(48,   "Special Attack Energy");
		varp(83,   "Run Energy (varp)");
		varp(173,  "Skull Timer");
		varp(261,  "Favour Points");
		varp(300,  "Poison");
		varp(375,  "Diary Lumbridge");
		varp(1775, "Auto Retaliate");
		varp(3079, "Game Timer");

		// Combat / Prayer
		varbit(4101, "Auto Retaliate");
		varbit(4102, "Attack Style");
		varbit(4103, "Defensive Casting");
		varbit(5711, "Preserve Prayer");
		varbit(5948, "Rigour Prayer");
		varbit(5949, "Augury Prayer");
		varbit(6371, "Quick Prayer");
		varbit(8121, "Vengeance Active");

		// Run
		varbit(25, "Run Toggle");

		// Stats
		varbit(3618, "Hitpoints Regen Timer");
		varbit(6003, "Antifire Charges");

		// Bank
		varbit(4150, "Bank Tab Count");

		// Slayer
		varbit(4069, "Slayer Task Size");
		varbit(4070, "Slayer Points");

		// Skills
		varbit(13756, "Mining Rock State");

		// Quest
		varbit(2310, "Theatre of Blood State");
		varbit(6440, "Chambers of Xeric State");
		varbit(9632, "Tombs of Amascut State");
		varbit(11877, "Colosseum Wave");

		// Boss / Raid
		varbit(5982, "Corp Beast Dmg");
		varbit(6385, "CoX Points");
		varbit(6386, "CoX Total Points");

		// Leagues
		varbit(12390, "League Points");
		varbit(12391, "League Task Progress");
		varbit(12392, "League Task Count");

		// Misc
		varbit(227,  "NPC Contact Cooldown");
		varbit(3534, "Cannon Balls");
		varbit(4070, "Slayer Reward Points");
		varbit(10151, "Collection Log Count");
	}

	private static void varp(int id, String name)
	{
		NAMES.put("varp_" + id, name);
	}

	private static void varbit(int id, String name)
	{
		NAMES.put("varbit_" + id, name);
	}

	public static String lookup(String type, int id)
	{
		return NAMES.get(type + "_" + id);
	}

	private VarNames() {}
}
