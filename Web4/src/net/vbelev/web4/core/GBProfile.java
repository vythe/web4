package net.vbelev.web4.core;
import java.util.*;

import net.vbelev.web4.utils.Utils;

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
	public Integer setID;
	public String groupListID;
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
	
	public GBVote getVote(Integer billID)
	{
		if (billID == null) return null;
		
		GBVote vote = votes.stream()
				.filter(q -> q.billID == billID)
				.findFirst()
				.orElse(null)
			;
		return vote;	
	}
	
	public void addVote(Double[] groupVals, GBVote vote)
	{
		GBVote oldVote = votes.stream()
				.filter(q -> q.billID == vote.billID)
				.findFirst()
				.orElse(null)
		;
		if (oldVote != null && oldVote.say != vote.say && oldVote.say != GBVote.SayEnum.NONE)
		{
			throw new IllegalArgumentException("An existing vote for " + vote.billID + " cannot be changed");
		}
		else if (oldVote != null && oldVote.say != GBVote.SayEnum.NONE)
		{
			return;
		}
		else if (oldVote != null)
		{
			this.votes.remove(oldVote);
		}
		GBVote myVote = new GBVote(vote);
		this.votes.add(myVote);
		
		double step;
		switch (myVote.say)
		{
		case AYE: step = 1.; break;
		case NAY: step = -0.5; break;
		case PASS: step = -0.25; break;
		default: step = 0; break;
		}
		this.applyInvAffinityStep(groupVals, step);
	}
	/**
	 * returns 0 if the affinity is not stored. Approvals are not really set,
	 * they are saved and modified from whatever business logic.
	 * @return
	 */
	public double getInvAffinity(int group)
	{
		//if (!affinities.containsKey(group)) return null;
		double d = invAffinities.getOrDefault(group, 0.);
		return Math.round(d * 1000.) / 1000.;
	}

	public double[] getInvAffinityValues(int size)
	{
		double[] res = new double[size];
		for (int i : invAffinities.keySet())
		{
			res[i] = getInvAffinity(i);
		}
		return res;
	}
	
	public void setInvAffinity(int group, Double val)
	{
		if (val == null || val == 0)
		{
			invAffinities.remove(group);
		}
		else
		{
			invAffinities.put(group, Math.round(val * 1000.) / 1000.);
		}
	}
	
	/** this is for restoring inv affinities from a backup */
	public void setInvAffinities(double[] vals)
	{
		invAffinities.clear();
		for (int i = 0; i < vals.length; i++)
		{
			if (vals[i] != 0)
			{
				invAffinities.put(i, Math.round(vals[i] * 1000.) / 1000.);
			}
		}
	}
	/**
	 * Applies a vote say to the invAffinities list.
	 * Each invAffinity[i] gets a step proportional to groupVals[i] 
	 * with the common  multiplier of step.
	 * For aye, the step would be 1, for Nay it would be -1, Pass is -0.5 or so.
	 * Returns the array of inv affinities, including 0s.
	 */
	public double[] calculateInvAffinityStep(Double[] groupVals, double step)
	{
		double[] res = new double[groupVals.length];
		for (int i = 0; i < groupVals.length; i++)
		{
			double groupVal = (Utils.NVL(groupVals[i], 0.) - 0.5);
			
			double val = calculateStep(getInvAffinity(i), groupVal * step);	
			val = Math.round(val * 1000.) / 1000.;
			res[i] = val;
		}
		return res;
	}

	/** calls calculateInvAffinityStep and saves the result in invAffinities;
	 * 
	 * @param groupVals
	 * @param step
	 * @return
	 */
	public double[] applyInvAffinityStep(Double[] groupVals, double step)
	{
		double[] res = calculateInvAffinityStep(groupVals, step);
		for (int i = 0; i < groupVals.length; i++)
		{
			setInvAffinity(i, res[i]);
		}
		return res;
	}
	/** the curve function is y = x / (x + 1), the inverse is  x = y / (1 - y). 
	 * results are rounded to 0.001. 
	 * */
	public static double calculateStep(double from, double step)
	{
		if (from < 0) from = 0.;
		if (from > 1) from = 1.;
		
		if (step == 0)
		{
			return from;
		}
		else if (from <= 0.001 && step < 0)
		{
			return 0;
		}
		else if (from >= 0.999 && step > 0)
		{
			return 1.;
		}
		else if (step < 0)
		{
			return 1. - calculateStep(1. - from, -step);
		}
		else
		{
			double x = from / (1. - from) + step;
			double newVal = x / (x + 1.);
			
			return newVal; //Math.round(newVal * 1000.) / 1000.;
		}
	}
}
