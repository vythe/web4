package net.vbelev.web4.xml;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

import net.vbelev.web4.core.*;
import net.vbelev.web4.utils.*;

@XmlRootElement(name = "groups")
public class GBGroupListXML 
{
	public static final String STORAGE_NAME = "groups"; 

	@XmlAttribute
	public Integer ID;
	public String title;
	public String description;

	@XmlAttribute
	public Date saveTime;
		
	public static class GBGroupXML
	{
		@XmlAttribute
		public String moniker;
		public String name;
		public List<GBAffinityXML> affinities;
	
		public void fromGBGroup(GBGroupSet gblist, GBGroup group)
		{
			this.moniker = group.moniker;
			this.name = group.name;
			this.affinities = new ArrayList<GBAffinityXML>();
			for (GBAffinity aff : group.getAffinities(false))
			{
				GBAffinityXML ax = new GBAffinityXML();
				ax.fromGBAffinity(gblist, aff);
				this.affinities.add(ax);
			}			
		}
		
		public GBGroup toGBGroupCore(GBGroup g)
		{
			if (g == null) g = new GBGroup();
			g.moniker = this.moniker;
			g.name = this.name;
			
			return g;
		}
		
		public GBGroup toGBGroupAffinities(GBGroupSet gblist, GBGroup g)
		{
			if (g == null) g = new GBGroup();
			
			g.affinities.clear();
			if (this.affinities != null)
			{
			for (GBAffinityXML ax : this.affinities)
			{
				GBAffinity aff = ax.toGBAffinity(gblist);
				g.affinities.put(aff.toID(), aff);
			}
			}
			return g;
		}
	}
	
	public List<GBGroupXML> groups;
	
	public void fromGroupSet(GBGroupSet set)
	{
		groups = new ArrayList<GBGroupXML>();
		for (GBGroup g : set.getGroups())
		{
			GBGroupXML gx = new GBGroupXML();
			gx.fromGBGroup(set, g);
			groups.add(gx);			
		}
		
		this.ID = set.ID;
		this.title = set.title;
		this.description = set.description;
		if (set.timestamp <= 0)
		{
		saveTime = new Date();
		}
		else
		{
			saveTime = set.getTimeModified();
		}
	}
	
	public void toGroupSet(GBGroupSet gblist)
	{
		int size = this.groups.size();
		gblist.setSize(size);
		for (int i = 0; i < size; i++)
		{
			this.groups.get(i).toGBGroupCore(gblist.getGroup(i));
		}
		for (int i = 0; i < size; i++)
		{
			this.groups.get(i).toGBGroupAffinities(gblist, gblist.getGroup(i));
		}
		gblist.ID = Utils.NVL(this.ID, 0);
		gblist.title = this.title;
		gblist.description = this.description;
		if (this.saveTime != null)
		{
			gblist.timestamp = this.saveTime.getTime();			
		}
		else
		{
			gblist.timestamp =new Date().getTime();
		}
		
	}
}
