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
	public Date saveTime;
	
	public static class GBGroupXML
	{
		@XmlAttribute
		public String moniker;
		public String name;
		public List<GBAffinityXML> affinities;
	
		public void fromGBGroup(GBEngine engine, GBGroup group)
		{
			this.moniker = group.moniker;
			this.name = group.name;
			this.affinities = new ArrayList<GBAffinityXML>();
			for (GBAffinity aff : group.getAffinities(false))
			{
				GBAffinityXML ax = new GBAffinityXML();
				ax.fromGBAffinity(engine, aff);
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
		
		public GBGroup toGBGroupAffinities(GBEngine engine, GBGroup g)
		{
			if (g == null) g = new GBGroup();
			
			g.affinities.clear();
			if (this.affinities != null)
			{
			for (GBAffinityXML ax : this.affinities)
			{
				GBAffinity aff = ax.toGBAffinity(engine);
				g.affinities.put(aff.toID(), aff);
			}
			}
			return g;
		}
	}
	
	public List<GBGroupXML> groups;
	
	public void fromEngine(GBEngine engine)
	{
		groups = new ArrayList<GBGroupXML>();
		for (GBGroup g : engine.getGroups())
		{
			GBGroupXML gx = new GBGroupXML();
			gx.fromGBGroup(engine, g);
			groups.add(gx);			
		}
		
		saveTime = new Date();
	}
	
	public void toEngine(GBEngine engine)
	{
		int size = this.groups.size();
		engine.setSize(size);
		for (int i = 0; i < size; i++)
		{
			this.groups.get(i).toGBGroupCore(engine.getGroup(i));
		}
		for (int i = 0; i < size; i++)
		{
			this.groups.get(i).toGBGroupAffinities(engine, engine.getGroup(i));
		}
	}
}
