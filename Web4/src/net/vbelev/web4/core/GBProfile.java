package net.vbelev.web4.core;
import java.util.*;

/**
 * A GB (user) profile, storing a set of affinities plus 
 * a name and such.
 * We only hold inverse affinities, because nobody cares how much
 * you personally approve of that social group.
 * @author vythe
 *
 */
public class GBProfile 
{
	/**
	 * note that profile IDs are primary keys, they are not mapped to anything
	 */
	public Integer ID;
	public String name;
	public Date saveDate;

	/** this is "reverse" affinities. 
	 * invAffinties[groupA] = 0.3 means "30% of groupA members approve of me". 
	 * Affinities only store the FORCED values, so they are bare numbers.
	 */
	public final Hashtable<Integer, Double> invAffinities = new Hashtable<Integer, Double>();
	public final List<GBVote> votes = new ArrayList<GBVote>();
	
	public GBProfile()
	{
	
	}
	
	/**
	 * returns null if the affinity is not set.
	 * @param group
	 * @return
	 */
	public Double getInvAffinity(int group)
	{
		//if (!affinities.containsKey(group)) return null;
		return invAffinities.getOrDefault(group, null);
	}


}
