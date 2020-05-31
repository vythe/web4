package net.vbelev.web4.xml;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

import net.vbelev.web4.core.*;
import net.vbelev.web4.utils.*;

/**
 * affinities are always stored inside a host entity (as a list),
 * so there is no need for a "moniker" field.
 * @author vythe
 *
 */
@XmlRootElement(name = "affinity")
public class GBAffinityXML 
{
	public String toMoniker;
	public double value;
	/** blank quality means "SET" */
	public String quality;

	public GBAffinityXML()
	{
	}
	
	public String toString()
	{
		return "GBAffinityXML: " + toMoniker + "=" + value;
	}
	
	public void fromGBAffinity(GBGroupSet gblist, GBAffinity aff)
	{
		this.toMoniker = gblist.getGroupMoniker(aff.toID());
		this.value = aff.value();
		this.quality = (aff.quality() == GBAffinity.QualityEnum.SET)? null : aff.quality().name();
	}
	
	public GBAffinity toGBAffinity(GBGroupSet gblist)
	{
		GBAffinity.QualityEnum affQ = Utils.tryParseEnum(this.quality,  GBAffinity.QualityEnum.SET);
		GBGroup g2 = gblist.getGroup(this.toMoniker);
		if (g2 == null)
		{
			throw new IllegalArgumentException("Invalid group moniker: " + this.toMoniker);
		}
		double checkedValue = this.value < 0? 0 : this.value > 1? 1 : this.value;
		return new GBAffinity(g2.ID, checkedValue, affQ);
	}
}
