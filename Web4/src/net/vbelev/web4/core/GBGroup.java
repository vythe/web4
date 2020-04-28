package net.vbelev.web4.core;
import java.util.*;

/**
 * A social group, which is basically a name. 
 * It holds affinities to other groups.
 * Groups also have affinities (approval ratings) toward Profiles,
 * but those are stored in GBProfile. 
 * @author vythe
 *
 */
public class GBGroup {

	/** This ID is set by GBEngine to back-reference the position in groups[] */
	public int ID;
	/** This is the stable (exportable) ID to be used for storage.
	 * We need the monikers to link bill records with the groups */
	public String moniker;
	public String name;
	/** affinity[groupB] = 0.3 here means "30% of my members are also in groupB" */  
	public final Hashtable<Integer, GBAffinity> affinities = new Hashtable<Integer, GBAffinity>();

	/**
	 * returns null if the affinity is not set.
	 * @param group
	 * @return
	 */
	public Double getAffinityValue(int toGroup)
	{
		//if (!affinities.containsKey(group)) return null;
		GBAffinity aff = affinities.getOrDefault(toGroup, null);
		if (aff == null || aff.quality() == GBAffinity.QualityEnum.NONE) return null;
		
		return aff.value();
	}
	public GBAffinity getAffinity(int toGroup)
	{
		//if (!affinities.containsKey(group)) return null;
		GBAffinity aff = affinities.getOrDefault(toGroup, null);
		if (aff == null) return GBAffinity.EMPTY;
		
		return aff;
	}
	
	public GBAffinity setAffinity(GBAffinity aff)
	{
		affinities.put(aff.toID(), aff);
		return aff;
	}
	public GBAffinity setAffinity(int toGroup, double value)
	{
		return setAffinity(toGroup, value, GBAffinity.QualityEnum.SET);
	}
	
	public GBAffinity setAffinity(int toGroup, double value, GBAffinity.QualityEnum quality)
	{
		if (quality == GBAffinity.QualityEnum.NONE)
		{
			affinities.remove(toGroup);
			return GBAffinity.EMPTY;
		}
		else
		{
			GBAffinity aff = new GBAffinity(toGroup, value, quality);
			affinities.put(toGroup, aff);
			return aff;
		}
	}
	
	public List<GBAffinity> getAffinities(boolean withCalculated)
	{
		ArrayList<GBAffinity> res = new ArrayList<GBAffinity>();
		for (GBAffinity aff : this.affinities.values())
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
}
