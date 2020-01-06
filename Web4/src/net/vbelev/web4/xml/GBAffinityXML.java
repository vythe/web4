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
	
	public void fromGBAffinity(GBEngine engine, GBAffinity aff)
	{
		this.toMoniker = engine.getGroupMoniker(aff.toID());
		this.value = aff.value();
		this.quality = (aff.quality() == GBAffinity.QualityEnum.SET)? null : aff.quality().name();
	}
	
	public GBAffinity toGBAffinity(GBEngine engine)
	{
		GBAffinity.QualityEnum affQ = Utils.tryParseEnum(this.quality,  GBAffinity.QualityEnum.SET);
		GBGroup g2 = engine.getGroup(this.toMoniker);
		if (g2 == null)
		{
			throw new IllegalArgumentException("Invalid group moniker: " + this.toMoniker);
		}
		return new GBAffinity(g2.ID, this.value, affQ);
	}
}
