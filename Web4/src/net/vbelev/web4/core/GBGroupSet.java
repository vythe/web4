package net.vbelev.web4.core;

import java.util.*;

import net.vbelev.web4.utils.Utils;

/**
 * A set ot GBGroup's (social groups), cross-referencing each other.
 * It's a square matrix of group affinities.
 */
public class GBGroupSet implements Cloneable
{
	public Integer ID;
	public String title;
	public String description;
	public long timestamp;
	
	private GBGroup[] groups;
	
	public GBGroupSet() {
		this(0);
	}
	
	public GBGroupSet(int size) {
		 //groups = new GBGroup[size];
		setSize(size);
	}
	
	public Object clone()
	{
		GBGroupSet res = new GBGroupSet();

		res.ID = this.ID;
		res.title = this.title;
		res.description = this.description;
		res.timestamp = this.timestamp;
		
		if (this.groups == null)
		{
			res.groups = null;
		}
		else
		{
			res.groups = new GBGroup[this.groups.length];
			for (int i = 0; i < this.groups.length; i++)
			{
				GBGroup elem = this.groups[i] == null? null : (GBGroup)this.groups[i].clone();
				res.groups[i] = elem;
			}
		}
	
		return res;
	}
	
	public Date getTimeModified() { return new Date(timestamp); }
	
	public String getSortName() { return Utils.NVL(title, "" + ID); }
	
	public int getSize()
	{
		if (groups == null) return 0;
		return groups.length;
	}
	
	public void setSize(int size)
	{
		int copySize = (groups == null)? 0 
				: groups.length < size? groups.length 
				: size
		;
	
		GBGroup[] newGroups = new GBGroup[size];
		for (int i = 0; i < copySize; i++)
		{
			newGroups[i] = groups[i];
			newGroups[i].ID = i;
		}
		for (int i = copySize; i < size; i++)
		{
			newGroups[i] = new GBGroup();
			newGroups[i].ID = i;
		}
		this.groups = newGroups;
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
	
	public GBGroup getGroup(int ID)
	{
		if (groups == null || ID < 0 || ID >= groups.length)
		{
			return null;
		}
		return groups[ID];
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
		if (groups == null || ID < 0 || ID >= groups.length)
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

	/**
	 * returns an array of affinity values of all groups to the group[forID].
	 * values will be null for qualities NONE.
	 * @return
	 */
	public Double[] getAffinitiesTo(int toID)
	{
		Double[] res = new Double[groups.length];
		for (GBGroup g : groups)
		{
			GBAffinity aff = g.getAffinity(toID);
			if (aff != null && aff.quality() != GBAffinity.QualityEnum.NONE)
			{
				res[g.ID] = aff.value();
			}	
		}
		return res;
	}
		
	public GBAffinity getAffinity(GBGroup group, int toGroup, boolean forceRecalc)
	{
		//GBGroup toGroup = getGroup(toMoniker);
		//if (toGroup == null) return GBAffinity.EMPTY
		if (toGroup < 0 || toGroup >= this.getSize())
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

	public void calculateAll()
	{
		// clear all calculated values first
		int listSize = groups.length;
		for (GBGroup g: groups)
		{
			for (int g2 = 0; g2 < listSize; g2++)
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
			for (int g2 = 0; g2 < listSize; g2++)
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
		if (forGroup.ID == toGroup) return 1.;
		
		Double[] listFrom = this.getAffinitesFor(forGroup.ID);
		Double[] listTo = this.getAffinitiesTo(toGroup);
		listFrom[forGroup.ID] = null; // exclude the "self" link
		
		return GBAffinity.calculateAffinity(listFrom, listTo);
	}	
	
	public Double calculateAffinityOld(GBGroup forGroup, int toGroup)
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
	
	/**
	 * returns an array of affinity values of group[forID] to all other groups.
	 * values will be null for qualities NONE.
	 * @return
	 */
	public Double[] getAffinitesFor(int forID)
	{
		Double[] res = new Double[this.getSize()];
		GBGroup g = this.getGroup(forID);
		
		for(GBAffinity aff : g.getAffinities(true))
		{
			res[aff.toID()] = aff.value();
		}	
		
		return res;
	}
	
	
}
