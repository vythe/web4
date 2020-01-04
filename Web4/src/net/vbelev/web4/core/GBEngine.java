package net.vbelev.web4.core;

import java.util.*;

/**
 * A coherent storage for profiles and groups
 * @author vythe
 *
 */
public class GBEngine {

	private String dataFolder;
	
	private GBGroup[] groups = new GBGroup[0];
	
	private GBEngine(String root)
	{
		dataFolder = root;
	}
	
	public static GBEngine loadEngine(String root)
	{
		GBEngine res = new GBEngine(root);
		res.testSet();
		
		return res;
	}
	
	public void testSet()
	{
		groups = new GBGroup[4];
				
		GBGroup g1 = new GBGroup();
		g1.moniker = "g1";
		g1.name = "Group1";
		g1.ID = 0;
		g1.setAffinity(1,  0.2);
		g1.setAffinity(2,  0.9);
		groups[0] = g1;
				
		GBGroup g2 = new GBGroup();
		g2.moniker = "g2";
		g2.name = "Group2";
		g2.ID = 1;
		g2.setAffinity(2,  0.6);
		g2.setAffinity(3,  0.3);
		groups[1] = g2;
		
		
		GBGroup g3 = new GBGroup();
		g3.moniker = "g3";
		g3.name = "Group3";
		g3.ID = 2;
		g3.setAffinity(3, 0.5);
		g3.setAffinity(0, 0.5);
		groups[2] = g3;
		
		GBGroup g4 = new GBGroup();
		g4.moniker = "g4";
		g4.name = "Group4";
		g4.ID = 3;
		g4.setAffinity(0, 0.8);
		g4.setAffinity(1,0.3);
		groups[3] = g4;
	}

	public boolean hasGroup(String moniker)
	{
		if (moniker == null) return false;
		for (GBGroup g : groups)
		{
			if (g.moniker.equals(moniker)) return true;
		}
		return false;
	}
	
	public GBGroup getGroup(String moniker)
	{
		if (moniker == null) return null;
		for (GBGroup g : groups)
		{
			if (g.moniker.equals(moniker)) return g;
		}
		return null;
	}

	public String getGroupMoniker(int ID)
	{
		if (ID < 0 || ID >= groups.length)
		{
			return null;
		}
		return groups[ID].moniker;
	}
	
	public List<GBGroup> getGroups()
	{
		ArrayList<GBGroup> res = new ArrayList<GBGroup>();
		for (GBGroup g : groups)
		{
			res.add(g);
		}
		return res;
	}
	
	public void calculateAll()
	{
		// clear all calculated values first
		for (GBGroup g: groups)
		{
			for (int g2 = 0; g2 < groups.length; g2++)
			{
				//if (g2 == g.ID) continue;
				GBAffinity aff = g.getAffinity(g2);
				if (aff == null || aff.quality() != GBAffinity.QualityEnum.SET)
				{
					g.setAffinity(g2,  0,  GBAffinity.QualityEnum.NONE);
				}
			}
				
		}
		// recalculate it all three times
		for (int repeat = 0; repeat < 3; repeat++)
		{
		for (GBGroup g: groups)
		{
			for (int g2 = 0; g2 < groups.length; g2++)
			{
				//if (g2 == g.ID) continue;
				GBAffinity aff = g.getAffinity(g2);
				if (aff == null || aff.quality() != GBAffinity.QualityEnum.SET)
				{
					aff = getAffinity(g, g2, true);
				}
			}
				
		}
		}
	}
	public Double calculateAffinity(GBGroup forGroup, int toGroup)
	{
		double nom = 0;
		double denom = 0;
		for (GBAffinity aff : forGroup.getAffinities(true))
		{
			if (aff.toID() == forGroup.ID)
			{
				continue;
			}
			double coef = aff.value() * aff.value();
			GBAffinity aff2 = groups[aff.toID()].getAffinity(toGroup);
			if (aff2.quality() != GBAffinity.QualityEnum.NONE)
			{
				nom += coef * aff2.value();
				denom += coef;
			}
		}
		if (denom <= 0)
		{
			return null;
		}
		return nom / denom;
	}
	
	public GBAffinity getAffinity(GBGroup group, int toGroup, boolean forceRecalc)
	{
		//GBGroup toGroup = getGroup(toMoniker);
		//if (toGroup == null) return GBAffinity.EMPTY
		if (toGroup < 0 || toGroup >= groups.length)
		{
			return GBAffinity.EMPTY;
		}
		
		GBAffinity aff = group.getAffinity(toGroup);
		if (aff.quality() != GBAffinity.QualityEnum.SET
				 && (forceRecalc || aff.quality() == GBAffinity.QualityEnum.NONE)
		)
		{
			Double newValue = calculateAffinity(group, toGroup);
			if (newValue == null)
			{
				aff = group.setAffinity(toGroup, 0, GBAffinity.QualityEnum.NONE);
			}
			else
			{
			aff = group.setAffinity(toGroup, newValue, GBAffinity.QualityEnum.CALCULATED);
			}
		}
		
		return aff;
	}
	
	public GBProfile getProfile(int id)
	{
		GBProfile res = new GBProfile();
		res.ID = id;
		res.name = "profile#" + id;

		return res;
	}
}
