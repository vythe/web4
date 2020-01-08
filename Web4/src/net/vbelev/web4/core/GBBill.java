package net.vbelev.web4.core;
import java.util.*;

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
	 * note that profile IDs are primary keys, they are not mapped to anything.
	 * ID will be null for non-saved bills
	 */
	public Integer ID;
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
	
	public void calculateInvAffinities(GBEngine engine)
	{
		int size = engine.getSize();
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
				
				Double[] listFrom = engine.getAffinitesFor(i);
				Double newValue = GBAffinity.calculateAffinity(listFrom, listTo);
				if (newValue != null && (aff == null || aff.quality() == GBAffinity.QualityEnum.NONE || aff.value() != newValue))
				{
					gotChanges = true;
					this.setInvAffinity(i,  newValue, GBAffinity.QualityEnum.CALCULATED);
				}
			}	
		}
		while (gotChanges || repeatCount < size);

	}

}
