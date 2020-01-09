package net.vbelev.web4;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.media.multipart.*;

import java.util.*;
import net.vbelev.web4.core.*;
import net.vbelev.web4.ui.*;
import net.vbelev.web4.utils.Utils;

@Path("/")
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
		public GBProfile currentProfile;
		public GBBill currentBill;
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
		}
		return res;
	}

	protected GBEngine getGBEngine()
	{
		GBEngine res = null;
		String root = Utils.NVL(context.getInitParameter("dataFolder"), ".");
		
		if (context == null)
		{
			res = GBEngine.loadEngine(root);
		}
		else 
		{
			Object oRes = request.getSession().getAttribute(CONTEXT_ENGINE_PARAM);
			if (oRes == null || !(oRes instanceof GBEngine))
			{
				res = GBEngine.loadEngine(root);
				request.getSession().setAttribute(CONTEXT_ENGINE_PARAM, res);
			}
			else
			{
				res = (GBEngine)oRes;
			}
		}
		return res;
	}
	
	public static class GetUserModel
	{
		public String name;
		public String status;
		public Hashtable<Integer, String> profiles;
	}
	
	@Path("/get_user")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	//public GetUserModel getUserModel()
	public Response getUserModel()
	{
		GetUserModel res = new GetUserModel();
		
		GBEngine engine = getGBEngine();
		Web4Session session = getWeb4Session();
		List<Integer> profiles = new ArrayList<Integer>();
		synchronized(session)
		{
			WebUser u = session.user;
			res.name = Utils.NVL(u.name, "(Unknown)");
			res.status = u.status.toString();
			profiles.addAll(u.profiles);
		}
		
		profiles.add(17);
		profiles.add(18);
		profiles.add(19);
		
		res.profiles = new Hashtable<Integer, String>();
		for (Integer profileID : profiles)
		{
			GBProfile f = engine.getProfile(profileID.intValue());
			res.profiles.put(profileID,  f.name);
		}
		return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
		//return res;
	}
	
	public class UpdateUserModel
	{
		public String name;	
	}
	
	@Path("/update_user")	
    @POST
    @Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	//public GetUserModel updateUser(UpdateUserModel args)
	public Response updateUser(UpdateUserModel args)
	{
		Web4Session session = getWeb4Session();
		synchronized(session)
		{
			WebUser u = session.user;
			u.name = args.name;
		}
		return getUserModel();
	}

	@Path("/http-headers")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllHttpHeaders(final @Context HttpHeaders httpHeaders) {
        return Response.ok(httpHeaders.getRequestHeaders()).build();
    }
	
	public static class GetAffinity
	{
		public String toMoniker;
		public double value;
		public String quality;
		
		public GetAffinity()
		{
		}
		
		public GetAffinity(GBAffinity aff, GBEngine engine)
		{
			this.toMoniker = engine.getGroupMoniker(aff.toID());
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
	}
	
	@Path("/get_groups")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        //return Response.ok(httpHeaders.getRequestHeaders()).build();
		GBEngine engine = this.getGBEngine(); 
		List<GBGroup> allGroups = engine.getGroups();
		List<GetGroup> res = new ArrayList<GetGroup>();
		for (GBGroup g : allGroups)
		{
			GetGroup gg = new GetGroup();
			gg.name = g.name;
			gg.moniker = g.moniker;
			gg.affinities = new ArrayList<GetAffinity>();
			for (GBGroup g2 : allGroups)
			{
				GBAffinity gf = engine.getAffinity(g,  g2.ID, false);
				gg.affinities.add(new GetAffinity(gf, engine));
			}			
			res.add(gg);
		}
		return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
	}
	
	@XmlRootElement
	public static class SetAffinity
	{
		public String moniker;
		public String toMoniker;
		public Double value;
		
		public SetAffinity()
		{
		}
	}
	
	@Path("/set_affinity")	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
	public Response setAffinity(SetAffinity args)
	{
		GBEngine engine = this.getGBEngine(); 
		GBGroup g = engine.getGroup(args.moniker);
		GBGroup g2 = engine.getGroup(args.toMoniker);
		GBAffinity aff = null;
		
		if (g == null || g2 == null || g.ID == g2.ID)
		{
			throw new IllegalArgumentException("Invalid group monikers provided");
		}
		if (args.value < 0 || args.value > 1) 
		{
			throw new IllegalArgumentException("Value must be between 0 and 1");
		}
		if (args.value == null)
		{
			g.setAffinity(g2.ID,  0, GBAffinity.QualityEnum.NONE);
			aff = engine.getAffinity(g, g2.ID, true);
		}
		else
		{
			aff = g.setAffinity(g2.ID,  args.value, GBAffinity.QualityEnum.SET);
		}
		engine.calculateAll();
		
		engine.saveGroups();
		
		aff = engine.getAffinity(g, g2.ID, false);
		
		GetAffinity res = new GetAffinity(aff, engine);
		return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
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
		public List<GetAffinity> invAffinities;
		
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
			this.status = bill.status.name();
			this.publishedDate = Utils.formatDateTime(bill.publishedDate);
			this.invAffinities = new ArrayList<GetAffinity>();
			
			for (int ind = 0; ind < engine.getSize(); ind++)
			{
				GBAffinity aff = bill.getInvAffinity(ind);
				GetAffinity gaf = new GetAffinity(aff, engine);
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

	@Path("/get_bill")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response getBill(@QueryParam("billID") Integer billID)
	{
		GBBill bill = null;
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		if (billID == null)
		{
			if (session.currentBill == null)
			{
				session.currentBill = new GBBill();
			}
			bill = session.currentBill; 
		}
		else
		{
			bill = engine.getBill(billID.intValue(), false);
		}
		if (bill == null)
		{
			return Response.status(500, "Invalid billID: " + billID)
				.header("Access-Control-Allow-Origin", "*").build()
			;
		}
		else
		{
			GetBill res = new GetBill();
			res.fromGBBill(bill, engine, session.currentProfile.votes);
			return Response.ok(res)
				.header("Access-Control-Allow-Origin", "*").build()
			;
		}
	}

	@Path("/get_bills")	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBills()
	{
		ArrayList<GetBill> res = new ArrayList<GetBill>();
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		if (profile == null)
		{
			profile = new GBProfile();
		}
		GBBill[] bills = engine.bills.values().stream()
				.filter(q -> q.status == GBBill.StatusEnum.PUBLISHED)
				.toArray(GBBill[]::new)
		;
		Arrays.sort(bills, new Comparator<GBBill>() {

			@Override
			public int compare(GBBill b1, GBBill b2)
			{
				if (b1 == null && b2 == null) return 0;
				else if (b1 == null) return -1;
				else if (b2 == null) return 1;
				else if (b1.publishedDate == null && b2.publishedDate == null) return 0;
				else if (b1.publishedDate == null) return -1;
				else if (b2.publishedDate == null) return 1;
				else return b2.publishedDate.compareTo(b2.publishedDate);
			}
		});
		
		for (GBBill bill : bills)
		{
			GetBill g = new GetBill();
			g.fromGBBill(bill, engine, profile.votes);
			res.add(g);
		}
		return Response.ok(res)
				.header("Access-Control-Allow-Origin", "*").build()
			;
		
	}

	@Path("/test_bill")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response testBill()
	{
		GBEngine engine = this.getGBEngine(); 
		//Web4Session session = getWeb4Session();
		GBBill bill = new GBBill();
		bill.publishedDate = new Date();
		bill.title = "Test " + Utils.formatDateTime(bill.publishedDate);
		bill.description = "Description of " + bill.title;
		bill.status = GBBill.StatusEnum.PUBLISHED;
		
		// give it two affinities
		bill.setInvAffinity(engine.random.nextInt(engine.getSize()), Math.random());
		bill.setInvAffinity(engine.random.nextInt(engine.getSize()), Math.random());
		bill.calculateInvAffinities(engine);
		
		engine.saveBill(bill);
		
		return getBill(bill.ID);
	}
	
	public static class GetProfile
	{
		public Integer ID;
		public String name;
		public String saveDate;
		
		public List<GetAffinity> invAffinities;
		
		public GetProfile()
		{
		}
		
		public void fromGBProfile(GBProfile profile, GBEngine engine)
		{
			this.ID = profile.ID;
			this.name = profile.name;
			this.saveDate = Utils.formatDateTime(profile.saveDate);
			this.invAffinities = new ArrayList<GetAffinity>();
			
			for (int ind = 0; ind < engine.getSize(); ind++)
			{
				GBAffinity aff = new GBAffinity(ind, profile.getInvAffinity(ind));
				GetAffinity gaf = new GetAffinity(aff, engine);
				this.invAffinities.add(gaf);
			}
		}
	}
	
	/**
	 * Always returns the current (session) profile. If the profileID is not null,
	 * then loads the profile as current and then returns it.
	 * @param profileID
	 * @return
	 */
	@Path("/get_profile")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	Response getProfile(Integer profileID)
	{
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = null;
		if (profileID == null)
		{
			profile = session.currentProfile;
		}
		else
		{
			profile = engine.loadProfile(profileID);
			if (profile == null)
			{
				return Response.status(500, "Invalid profileID: " + profileID)
						.header("Access-Control-Allow-Origin", "*").build()
					;
			}
			session.currentProfile = profile;
		}
		GetProfile res = new GetProfile();
		res.fromGBProfile(profile, engine);
		
		return Response.ok(res)
				.header("Access-Control-Allow-Origin", "*").build()
			;
	}

	@Path("/save_profile")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response saveProfile()
	{
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		engine.saveProfile(profile);
		
		GetProfile res = new GetProfile();
		res.fromGBProfile(profile, engine);
		
		return Response.ok(res)
				.header("Access-Control-Allow-Origin", "*").build()
			;	
	}
	
	
}
