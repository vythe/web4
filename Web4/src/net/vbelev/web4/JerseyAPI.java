package net.vbelev.web4;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.media.multipart.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import net.vbelev.web4.core.*;
import net.vbelev.web4.ui.*;
import net.vbelev.web4.utils.Utils;

public class JerseyAPI
{
	@Context 
	private HttpServletRequest request;

	@Context
	private ServletContext context;
	
	protected static final String SESSION_WEB4_PARAM = "Web4Session";
	protected static final String CONTEXT_ENGINE_PARAM = "GBEngine";
	
	public static class Web4Session
	{
		
		/** this might not be saved at all, a new user */
		public WebUser user;
		/** currently selected profile, there will always be an instance */
		public GBProfile currentProfile;
		
		/** the bill record for editing */
		public GBBill editingBill;
		/** the group set for editing - it may be null. It's a different instance from the engine's group set*/
		public GBGroupSet editingSet; 
		
		public final List<String> messages = new ArrayList<String>();
	}

	protected Web4Session getWeb4Session()
	{
		Web4Session res = null;
		if (request == null ||  request.getSession() == null)
		{
			res = new Web4Session();
			res.messages.add("No web session found");
		}
		else 
		{
			Object oRes = request.getSession().getAttribute(SESSION_WEB4_PARAM);
			if (oRes == null || !(oRes instanceof Web4Session))
			{
				res = new Web4Session();
				request.getSession().setAttribute(SESSION_WEB4_PARAM, res);
			}
			else
			{
				res = (Web4Session)oRes;
			}
		}
		if (res.user == null)
		{
			res.user = new WebUser();
		}
		if (res.currentProfile == null)
		{
			res.currentProfile = new GBProfile();
			res.currentProfile.setID = 0; // the default set ?
		}
		return res;
	}
/*
	protected GBEngine getGBEngine(boolean forceNew)
	{
		if (forceNew)
		{
			request.getSession().removeAttribute(CONTEXT_ENGINE_PARAM);
		}
		return getGBEngine();
	}
*/	
	protected GBEngine getGBEngine(boolean forceNew)
	{
		synchronized(CONTEXT_ENGINE_PARAM) // to keep it simple, make everybody wait until the engine is loaded.
		{
			GBEngine res = null;			
			Object engineObj = context == null? null : context.getAttribute(CONTEXT_ENGINE_PARAM);
			
			if (forceNew || engineObj == null || !(engineObj instanceof GBEngine))
			{
				//GBFileStorage storage = new GBFileStorage(root);
				String mongoConnection = Utils.NVL(context.getInitParameter("mongoConnection"), "");
				String dataFolder = Utils.NVL(context.getInitParameter("dataFolder"), ".");
				if (!Utils.IsEmpty(mongoConnection))
				{
					GBMongoStorage storage = new GBMongoStorage(mongoConnection);
					res = GBEngine.loadEngine(storage);
				}
				else
				{
					GBFileStorage storage = new GBFileStorage(dataFolder);
					res = GBEngine.loadEngine(storage);
				}
			}
			else 
			{
				res = (GBEngine)engineObj;
			}
			res.storage.ping(false);
			return res;
		}
	}
	
// ==== Shared model classes =====
	
	public static class GetAffinity
	{
		public String toMoniker;
		public double value;
		public String quality;
		
		public GetAffinity()
		{
		}
		
		public GetAffinity(GBAffinity aff, GBGroupSet gblist)
		{
			this.toMoniker = gblist.getGroupMoniker(aff.toID());
			this.value = aff.value();
			if (this.toMoniker == null)
			{
				 this.quality = GBAffinity.QualityEnum.NONE.name();
			}
			else
			{
				this.quality = aff.quality().name();
			}
		}
	}
	
	public static class GetGroup
	{
		public String moniker;
		public String name;
		public List<GetAffinity> affinities;
		
		public GetGroup()
		{
		}
		
		public void fromGBGroup(GBGroup g, GBGroupSet gblist)
		{
			this.name = g.name;
			this.moniker = g.moniker;
			this.affinities = new ArrayList<GetAffinity>();
			for (GBGroup g2 : gblist.getGroups())
			{
				GBAffinity gf = gblist.getAffinity(g,  g2.ID, false);
				this.affinities.add(new GetAffinity(gf, gblist));
			}			
		}
	}
	
	
	public static class GetGroupSet
	{
		public Integer ID;
		public String title;
		public String description;
		public List<GetGroup> groups;
		
		public GetGroupSet()
		{
		}
			
		public void fromGBGroupSet(GBGroupSet gblist)
		{
			if (gblist == null)
			{
				return;
			}
			this.ID = gblist.ID;
			this.title = Utils.NVL(gblist.title, "");
			this.description = Utils.NVL(gblist.description, "");
			this.groups = new ArrayList<GetGroup>();
			
			for (GBGroup g : gblist.getGroups())
			{
				GetGroup gg = new GetGroup();
				gg.fromGBGroup(g, gblist);
				this.groups.add(gg);
			}
		}
	}
	
	public static class GetBill
	{
		public Integer ID;
		public String title;
		public String description;
		public String status;
		public String publishedDate;
		public String profileSay;
		public Date profileSayDate;
		/** The balance of assigned values only: when editing, aim for the editScore of 0 */
		public double editScore;
		/** The balance of all bill values, assigned and calculated */
		public double billScore;
		public List<GetAffinity> invAffinities;
		/** what actions are allowed on this bill, for this user. The idea is to control the workflow from the server side **/
		public List<String> actions;
		
		public void fromGBBill(GBBill bill, GBEngine engine, Collection<GBVote> votes)
		{
			this.ID = bill.ID;
			this.title = bill.title;
			this.description = bill.description;
			/*
			switch (bill.status)
			{
			case NEW: this.status = "(Not saved)"; break;
			case PUBLISHED: this.status = "Published"; break;
			default: 	this.status = bill.status.name(); break;
			}
			*/
			this.status = bill.status == null? "(null)" : bill.status.name();
			this.publishedDate = Utils.formatDateTime(bill.publishedDate);
			this.editScore = bill.getScore(true);
			this.billScore = bill.getScore(false);
			this.invAffinities = new ArrayList<GetAffinity>();
			
			GBGroupSet gblist = engine.getGroupSet(bill.setID);
			
			for (int ind = 0; ind < gblist.getSize(); ind++)
			{
				GBAffinity aff = bill.getInvAffinity(ind);
				GetAffinity gaf = new GetAffinity(aff, gblist);
				this.invAffinities.add(gaf);
			}
			if (votes != null && bill.ID != null)
			{
				
				GBVote vote = votes.stream()
					.filter(q -> q.billID == bill.ID)
					.findFirst()
					.orElse(null)
				;
				
				if (vote != null)
				{
					this.profileSay = vote.say.name();
					this.profileSayDate = vote.sayDate;
				}
			}
		}
	}

	public List<GetGroup> getGroups(GBGroupSet gblist)
	{
		List<GBGroup> allGroups = gblist.getGroups();
		ArrayList<GetGroup> res = new ArrayList<GetGroup>();
		for (GBGroup g : allGroups)
		{
			GetGroup gg = new GetGroup();
			gg.fromGBGroup(g, gblist);
			res.add(gg);
		}
		//return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
		return res;
	}
	
	
	
	/** The workflow control method - eventually we'll move it to a separate class.
	 */
	public List<String> billActions(GBBill bill, Collection<GBVote> votes)
	{
		List<String> res = new ArrayList<String>();
		GBVote vote = null;
		// "vote":
		if (bill.ID != null && votes != null)
		{
			
			vote = votes.stream()
				.filter(q -> q.billID == bill.ID)
				.findFirst()
				.orElse(null)
			;
		}
		if (bill.status == GBBill.StatusEnum.PUBLISHED && vote == null)
		{
			res.add("vote");
		}
		
		if (bill.status == GBBill.StatusEnum.NEW)
		{
			res.add("edit");
			res.add("delete");
			double editScore = bill.getScore(true);
			if (editScore > - 0.1 && editScore < 0.1)
			{
				res.add("publish");
			}
			
		}
		else if (bill.status == GBBill.StatusEnum.PUBLISHED)
		{
			res.add("close");
		}
		
		return res;

	}

	public Response response500(String message)
	{
			return Response.status(500, message).build();
				//.header("Access-Control-Allow-Origin", "*").build();		
	
	}
}
