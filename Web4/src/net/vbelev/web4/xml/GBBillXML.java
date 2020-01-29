package net.vbelev.web4.xml;

import java.util.*;
import javax.xml.bind.annotation.*;

import net.vbelev.web4.core.*;
import net.vbelev.web4.utils.*;

@XmlRootElement
public class GBBillXML {
	
	public static final String STORAGE_NAME = "bills"; 
	
	@XmlAttribute
	public Integer ID;
	public String title;
	public String description;
	@XmlAttribute
	public Date publishDate;
	@XmlAttribute
	public String status;
	
	public List<GBAffinityXML> invAffinities;

	public void fromGBBill(GBEngine engine, GBBill bill)
	{
		this.ID = bill.ID;
		this.title = Utils.NVL(bill.title, "");
		this.description = Utils.NVL(bill.description, "");
		this.publishDate = bill.publishedDate;
		
		if (bill.status == GBBill.StatusEnum.PUBLISHED)
		{
			this.status = null;
		}
		else
		{
			this.status = bill.status.name();
		}
		this.invAffinities = new ArrayList<GBAffinityXML>();
		for (GBAffinity aff : bill.getInvAffinities(false))
		{
			GBAffinityXML ax = new GBAffinityXML();
			ax.fromGBAffinity(engine, aff);
			this.invAffinities.add(ax);
		}			
	}
	
	public GBBill toGBBill(GBEngine engine, GBBill bill)
	{
		if (bill == null) bill = new GBBill();
		bill.ID = this.ID;
		bill.title = Utils.NVL(this.title, "");
		bill.description = Utils.NVL(this.description, "");
		bill.publishedDate = this.publishDate;
		
		bill.status = Utils.tryParseEnum(this.status,  GBBill.StatusEnum.PUBLISHED);
				
		bill.invAffinities.clear();
		if (this.invAffinities != null)
		{
		for (GBAffinityXML ax : this.invAffinities)
		{
			GBAffinity aff = ax.toGBAffinity(engine);
			bill.invAffinities.put(aff.toID(), aff);
		}
		}
		return bill;
	}
}
