package net.vbelev.web4.xml;

import java.util.*;
import javax.xml.bind.annotation.*;

import net.vbelev.web4.core.*;
import net.vbelev.web4.utils.*;

@XmlRootElement
public class GBProfileXML 
{
	
	public static class GBVoteXML
	{
		public int billID;
		public String say;
		public Date sayDate;
		
		public void fromGBVote(GBVote vote)
		{
			this.billID = vote.billID;
			this.say = vote.say.name();
			this.sayDate = vote.sayDate;
		}
		
		public GBVote toGBVote()
		{
			GBVote res = new GBVote();
			res.billID = this.billID;
			res.say = Utils.tryParseEnum(this.say, GBVote.SayEnum.NONE);
			res.sayDate = this.sayDate;
			
			return res;
		}
	}
	
	public static final String STORAGE_NAME = "profiles"; 
	
	@XmlAttribute
	public Integer ID;
	@XmlAttribute
	public Integer setID;
	public String name;
	@XmlAttribute
	public Date saveDate;
	
	public List<GBAffinityXML> invAffinities;
	public List<GBVoteXML> votes;

	public void fromGBProfile(GBGroupSet gblist, GBProfile profile)
	{
		this.ID = profile.ID;
		this.ID = profile.setID;
		this.name = Utils.NVL(profile.name, "");
		this.saveDate = new Date();
		
		this.invAffinities = new ArrayList<GBAffinityXML>();
		for (Integer id : profile.invAffinities.keySet())
		{
			GBAffinityXML ax = new GBAffinityXML();
			ax.toMoniker = gblist.getGroupMoniker(id);
			ax.value = profile.invAffinities.get(id);
			ax.quality = null;
			this.invAffinities.add(ax);
		}
		
		this.votes = new ArrayList<GBVoteXML>();
		for (GBVote vote : profile.votes)
		{
			GBVoteXML vx = new GBVoteXML();
			vx.fromGBVote(vote);
			this.votes.add(vx);
		}
	}
	
	public GBProfile toGBProfile(GBGroupSet gblist, GBProfile profile)
	{
		if (profile == null) profile = new GBProfile();
		profile.ID = this.ID;
		profile.setID = Utils.NVL(this.setID, 0);
		
		profile.name = Utils.NVL(this.name, "");
		profile.saveDate = this.saveDate;
		
		profile.invAffinities.clear();
		if (this.invAffinities != null)
		{
		for (GBAffinityXML ax : this.invAffinities)
		{
			GBAffinity aff = ax.toGBAffinity(gblist);
			profile.invAffinities.put(aff.toID(), aff.value());
		}
		}
		
		profile.votes.clear();
		if (this.votes != null)
		{
			for (GBVoteXML vx : this.votes)
			{
				GBVote vote = vx.toGBVote();
				profile.votes.add(vote);
			}
		}
		return profile;
	}
}
