package net.vbelev.web4.core;
import java.util.*;

import net.vbelev.web4.utils.Utils;

/**
 * A statement or action that social groups approve of.
 * 
 * @author vythe
 *
 */
public class GBBill 
{
	public static enum StatusEnum
	{
		NEW,
		PUBLISHED,
		CLOSED,
		CANCELLED
	}
	
	/**
	 * note that bill IDs are primary keys, they are not mapped to anything.
	 * ID will be null for non-saved bills
	 */
	public Integer ID;
	/**
	 * The GBGroupSet reference. It is not used in the bill itself, only for linking it withe group set.
	 */
	public Integer setID;
	public String title;
	public String description;
	/** we will display the date only, but internally we store the date-time (for better sorting) */
	public Date publishedDate;
	public StatusEnum status;
	
	/** this is "reversed" affinities. 
	 * invAffinties[groupA] = 0.3 means "30% of groupA members approve of me". 
	 * The stupid thing is a copy-paste from GBGroup with some renaming.
	 */
	public final Hashtable<Integer, GBAffinity> invAffinities = new Hashtable<Integer, GBAffinity>();

	/**
	 * returns null if the affinity is not set.
	 * @param group
	 * @return
	 */
	public Double getInvAffinityValue(int forGroup)
	{
		//if (!affinities.containsKey(group)) return null;
		GBAffinity aff = invAffinities.getOrDefault(forGroup, null);
		if (aff == null || aff.quality() == GBAffinity.QualityEnum.NONE) return null;
		
		return aff.value();
	}
	/**
	 * This is an inverse affinity, so the GBAffinity.toID() will 
	 * hold the "for" group ID.
	 * @param forGroup
	 * @return
	 */
	public GBAffinity getInvAffinity(int forGroup)
	{
		//if (!affinities.containsKey(group)) return null;
		GBAffinity aff = invAffinities.getOrDefault(forGroup, null);
		if (aff == null) return GBAffinity.EMPTY;
		
		return aff;
	}
	
	public GBAffinity setInvAffinity(GBAffinity aff)
	{
		invAffinities.put(aff.toID(), aff);
		return aff;
	}
	public GBAffinity setInvAffinity(int forGroup, double value)
	{
		return setInvAffinity(forGroup, value, GBAffinity.QualityEnum.SET);
	}
	
	public GBAffinity setInvAffinity(int forGroup, double value, GBAffinity.QualityEnum quality)
	{
		if (quality == GBAffinity.QualityEnum.NONE)
		{
			invAffinities.remove(forGroup);
			return GBAffinity.EMPTY;
		}
		else
		{
			GBAffinity aff = new GBAffinity(forGroup, value, quality);
			invAffinities.put(forGroup, aff);
			return aff;
		}
	}
	
	public List<GBAffinity> getInvAffinities(boolean withCalculated)
	{
		ArrayList<GBAffinity> res = new ArrayList<GBAffinity>();
		for (GBAffinity aff : this.invAffinities.values())
		{
			if (aff != null 
					&& aff.quality() != GBAffinity.QualityEnum.NONE
					&& (withCalculated || aff.quality() == GBAffinity.QualityEnum.SET)
			)
			{
				res.add(aff);
			}
		}
		return res;
	}
	
	public Double[] getInvAffinityValues(int size)
	{
		Double[] res = new Double[size];
		for (GBAffinity aff : this.invAffinities.values())
		{
			if (aff != null && aff.quality() != GBAffinity.QualityEnum.NONE)
			{
				res[aff.toID()] = aff.value();
			}
		}
		return res;
	}
	
	public double getScore(boolean assignedOnly)
	{
		double res = 0;
		double count = 0;
		for (GBAffinity aff : this.invAffinities.values())
		{
			double affValue = 0;
			if (aff == null) continue;
			if (assignedOnly && aff.quality() != GBAffinity.QualityEnum.SET) continue;
			affValue = aff.value();
			if (affValue > 0.5)
			{
				affValue -= 0.5;
				res += affValue * affValue  * 4;
			}
			else
			{
				affValue = 0.5 - affValue;
				res -= affValue * affValue  * 4;
			}
			count++;
		}

		if (count == 0) return 0;
		return Math.round(res * 1000. / count) / 1000.;
	}
	
	public void calculateInvAffinities(GBGroupSet gblist)
	{
		int size = gblist.getSize();
		// clear it first
		for (int i = 0; i < size; i++)
		{
			GBAffinity aff = invAffinities.getOrDefault(i, null);
			if (aff != null && aff.quality() != GBAffinity.QualityEnum.SET)
			{
				invAffinities.remove(i);
			}
		}
		boolean gotChanges = false;
		int repeatCount = 0;
		do
		{
			Double[] listTo = getInvAffinityValues(size);
			gotChanges = false;
			repeatCount++;
			for (int i = 0; i < size; i++)
			{
				GBAffinity aff = invAffinities.getOrDefault(i, null);
				if (aff != null && aff.quality() == GBAffinity.QualityEnum.SET) continue;
				
				Double[] listFrom = gblist.getAffinitesFor(i);
				Double newValue = GBAffinity.calculateAffinity(listFrom, listTo);
				if (newValue != null && (aff == null || aff.quality() == GBAffinity.QualityEnum.NONE || Math.abs(aff.value() - newValue) > 0.01))
				{
					gotChanges = true;
					this.setInvAffinity(i,  newValue, GBAffinity.QualityEnum.CALCULATED);
				}
			}	
		}
		while (gotChanges && repeatCount < size);

	}

}
