package net.vbelev.web4;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.media.multipart.*;

import java.io.IOException;
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

	protected GBEngine getGBEngine(boolean forceNew)
	{
		if (forceNew)
		{
			request.getSession().removeAttribute(CONTEXT_ENGINE_PARAM);
		}
		return getGBEngine();
	}
	protected GBEngine getGBEngine()
	{
		synchronized(CONTEXT_ENGINE_PARAM) // to keep it simple, make everybody wait until the engine is loaded.
		{
			GBEngine res = null;
			String root = Utils.NVL(context.getInitParameter("dataFolder"), ".");
			
			if (context == null)
			{
				//GBFileStorage storage = new GBFileStorage(root);
				GBMongoStorage storage = new GBMongoStorage("");
				res = GBEngine.loadEngine(storage);
			}
			else 
			{
				Object oRes = request.getSession().getAttribute(CONTEXT_ENGINE_PARAM);
				if (oRes == null || !(oRes instanceof GBEngine))
				{
					//GBFileStorage storage = new GBFileStorage(root);
					GBMongoStorage storage = new GBMongoStorage("");
					res = GBEngine.loadEngine(storage);
					request.getSession().setAttribute(CONTEXT_ENGINE_PARAM, res);
				}
				else
				{
					res = (GBEngine)oRes;
				}
			}
			res.storage.ping(false);
			return res;
		}
	}
	
// ====== Web Users =====	
	public static class GetUserModel
	{
		public Integer ID;
		public String name;
		public String status;
		public Integer currentProfileID;
		public Hashtable<Integer, String> profiles;
		
		public void FromWebUser(WebUser user, GBEngine engine, GBProfile profile)
		{
			List<Integer> profiles = new ArrayList<Integer>();
			synchronized(user)
			{
			this.ID = user.ID;
			this.name = Utils.NVL(user.name, "(Unknown)");
			this.status = user.status.toString();
			if (profile != null)
			{
				this.currentProfileID = profile.ID;
			}

			profiles.addAll(user.profiles);
			}
			this.profiles = new Hashtable<Integer, String>();
			//this.profiles.put(null,  "(New Profile)");
			for (Integer profileID : profiles)
			{
				GBProfile f = engine.getProfile(profileID.intValue());
				if (f != null)
				{
					this.profiles.put(profileID,  f.name);
				}
			}
		}
	}
	
	@Path("/get_user")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetUserModel getUser()
	//public Response getUser()
	{
		GetUserModel res = new GetUserModel();
		
		GBEngine engine = getGBEngine();
		Web4Session session = getWeb4Session();
		WebUser u = session.user;
		res.FromWebUser(u, engine, session.currentProfile);
		//return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
		return res;
	}
	
	public class UpdateUserModel
	{
		public String name;	
		public String login;
	}
	
	@Path("/update_user")	
    @POST
    @Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public GetUserModel updateUser(UpdateUserModel args)
	//public Response updateUser(UpdateUserModel args)
	{
		Web4Session session = getWeb4Session();
		synchronized(session)
		{
			WebUser u = session.user;
			u.name = args.name;
			u.login = args.login;
		}
		return getUser();
	}

	@Path("/init_user")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetUserModel initUser(@QueryParam("login") String login, @QueryParam("withProfile") String withProfile)
	{
		Web4Session session = getWeb4Session();
		if (session.user.ID != null)
		{
			throw new IllegalArgumentException("The current user is already initialized (ID=" + session.user.ID + ")");
		}
		login = Utils.NVL(login, session.user.login, "").trim().toLowerCase();
		if (!GBEngine.webUserLoginPattern.matcher(login).matches())
		{
			throw new IllegalArgumentException("The login name is not valid: " + login);
		}
		
		GBEngine engine = this.getGBEngine(); 
		Hashtable<String, Integer> userIndex = new Hashtable<String, Integer>();
		engine.loadWebUserIndex(userIndex);
		
		if (userIndex.containsKey(login))
		{
			throw new IllegalArgumentException("The login name is not available: " + login);
		}
		
		session.user.login = login;
		session.user.status = WebUser.StatusEnum.ACTIVE;
		if (Utils.IsEmpty(session.user.name))
		{
			session.user.name = login;
		}
		engine.saveWebUser(session.user);
		if (withProfile != null)
		{
			engine.saveProfile(session.currentProfile);
		}
		GetUserModel res = new GetUserModel();
		res.FromWebUser(session.user, engine, session.currentProfile);
		return res;
	}
	
	@Path("/login")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetUserModel loginUser(@QueryParam("login") String login)
	{
		Web4Session session = getWeb4Session();
		if (session.user.ID != null)
		{
			throw new IllegalArgumentException("There is a logged-in user " + session.user.name + " already, (ID=" + session.user.ID + ")");
		}
		login = Utils.NVL(login, session.user.login, "").trim().toLowerCase();
		if (!GBEngine.webUserLoginPattern.matcher(login).matches())
		{
			throw new IllegalArgumentException("The login name is not valid: " + login);
		}
		
		GBEngine engine = this.getGBEngine(); 
		Hashtable<String, Integer> userIndex = new Hashtable<String, Integer>();
		engine.loadWebUserIndex(userIndex);
		
		Integer userID = userIndex.getOrDefault(login,  null);
		if (userID == null)
		{
			throw new IllegalArgumentException("The login name is not known: " + login);
		}
		
		session.user = engine.loadWebUser(userID);
		if (session.user != null && !session.user.profiles.isEmpty())
		{
			Integer profileID = session.user.profiles.get(0);
			GBProfile profile = engine.loadProfile(profileID);
			session.currentProfile = profile;
		}

		return getUser();
	}
	
	@Path("/logout")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetUserModel logoutUser()
	{
		Web4Session session = getWeb4Session();
		
		session.user = new WebUser();
		session.currentProfile = new GBProfile();

		return getUser();
	}
	
	@Path("/http-headers")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllHttpHeaders(final @Context HttpHeaders httpHeaders) {
        return Response.ok(httpHeaders.getRequestHeaders()).build();
    }
	
// ====== Groups =====	
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
    public List<GetGroup> getGroups(@QueryParam("force") String force) {
        //return Response.ok(httpHeaders.getRequestHeaders()).build();
		GBEngine engine = this.getGBEngine(force != null); 
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
		//return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
		return res;
	}
	
	public static class UpdateGroup
	{
		public String moniker;
		public String name;
	}
	
	@Path("/update_group")	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public List<GetGroup> updateGroup(UpdateGroup group)
	{
		GBEngine engine = this.getGBEngine(); 

		if (Utils.IsEmpty(group.moniker) || Utils.IsEmpty(group.name))
		{
			throw new IllegalArgumentException("Moniker and name must not be empty");
					
		}
		GBGroup oldGroup = engine.getGroup(group.moniker);
		if (oldGroup != null && group.name.contentEquals(oldGroup.name))
		{
			// do nothing
		}
		else if (oldGroup != null)
		{
			oldGroup.name = group.name;
			engine.saveGroups();
		}
		else
		{
			int size = engine.getSize();
			engine.setSize(size + 1);
			engine.getGroup(size).name = group.name;
			engine.saveGroups();
		}
		return getGroups(null);
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
		return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
				.build();
	}
	
// ====== Bills =====	
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
	public GetBill getBill(@QueryParam("billID") Integer billID)
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
			throw new IllegalArgumentException("Invalid billID: " + billID);
			//return Response.status(500, "Invalid billID: " + billID)
			//	//.header("Access-Control-Allow-Origin", "*")
			//	.build()
			//;		
		}
		else
		{
			GetBill res = new GetBill();
			res.fromGBBill(bill, engine, session.currentProfile.votes);
			//return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
			//	.build()
			//;
			return res;
		}
	}

	@Path("/get_bills")	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<GetBill> getBills(@QueryParam("force") String force)
	{
		ArrayList<GetBill> res = new ArrayList<GetBill>();
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		if (profile == null)
		{
			profile = new GBProfile();
		}
		if (force != null)
		{
			engine.loadBills();
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
		//return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
		//		.build()
		//	;
		return res;		
	}

	@Path("/test_bill")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill testBill()
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
	
	/** sets the current session bill and returns it for editing;
	 * it's rather similar to getBill
	 * 
	 * @param billID
	 * @return
	 */
	@Path("/edit_bill")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill editBill(@QueryParam("billID") Integer billID, @QueryParam("reload") String reload)
	{
		GBBill bill = null;
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		
		if (session.currentBill != null
				&& reload == null
				&& (billID == null || Utils.equals(session.currentBill.ID, billID)))
		{
			bill = session.currentBill;
		}
		else if (billID == null)
		{
			session.currentBill = new GBBill();
			bill = session.currentBill; 
		}
		else
		{
			bill = engine.getBill(billID.intValue(), false);
			if (bill == null)
			{
				throw new IllegalArgumentException("Invalid billID: " + billID);
				//return Response.status(500, "Invalid billID: " + billID)
				//	//.header("Access-Control-Allow-Origin", "*")
				//	.build()
				//;		
			}
				
//			if (bill.status != GBBill.StatusEnum.NEW)
//			{
//				throw new IllegalArgumentException("bill ID " + billID + " is " + bill.status + " and cannot be edited");
//			}
			session.currentBill = bill;
		}
		GetBill res = new GetBill();
		res.fromGBBill(bill, engine, null);
		//return Response.ok(res)
			//.header("Access-Control-Allow-Origin", "*")
		//	.build()
		//;
		return res;
	}

	public static class UpdateBill
	{
		public String title;
		public String description;
		public String action;
		public List<GetAffinity> invAffinities;
	}
	
	@Path("/update_bill")	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill updateBill(UpdateBill update)
	{
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBBill bill = session.currentBill;
		
		if (bill.status != GBBill.StatusEnum.NEW)
		{
			throw new IllegalArgumentException("bill ID " + bill.ID + " is " + bill.status + " and cannot be edited");
		}
		
		bill.title = Utils.NVL(update.title, "").trim();
		bill.description = Utils.NVL(update.description, "").trim();
		bill.invAffinities.clear();
		if (update.invAffinities != null)
		{
			for (GetAffinity ga : update.invAffinities)
			{
				GBGroup g = engine.getGroup(ga.toMoniker);
				GBAffinity.QualityEnum q = Utils.tryParseEnum(ga.quality, GBAffinity.QualityEnum.SET);
				if (g != null && q == GBAffinity.QualityEnum.SET)
				{
					bill.setInvAffinity(g.ID, ga.value);
				}
			}
			bill.calculateInvAffinities(engine);
		}
		
		if ("save".equals(update.action))
		{
			engine.saveBill(bill);
		}
		else if ("publish".equals(update.action))
		{
			bill.status = GBBill.StatusEnum.PUBLISHED;
			engine.saveBill(bill);
		}
		return editBill(null, null);
	}
	
// ====== Profiles =====	
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
	 * Always returns the current (session) profile. 
	 * @param profileID
	 * @return
	 */
	@Path("/get_profile")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetProfile getProfile()
	{
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		GetProfile res = new GetProfile();
		res.fromGBProfile(profile, engine);
		
//		return Response.ok(res)
//				//.header("Access-Control-Allow-Origin", "*")
//				.build()
//			;
		return res;
	}

	/**
	 * If the profileID is not null, then loads the profile as current and returns it.
	 * If the profileID is null, creates a new profile, sets it as current and returns it.
	 * If force is null and the current profile ID matches profileID, return the current profile.
	 * If force is not null, abandond the current profile and load/create it again.
	 * @param profileID
	 * @return
	 */
	@Path("/load_profile")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response loadProfile(@QueryParam("profileID") Integer profileID, @QueryParam("force") String force)
	{
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = null;
		if (force == null && Utils.equals(session.currentProfile.ID, profileID))
		{
			profile = session.currentProfile;
		}
		else if (profileID == null)
		{
			profile = new GBProfile();
			session.currentProfile = profile;
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
				//.header("Access-Control-Allow-Origin", "*")
				.build()
			;
	}

	public static class UpdateProfile
	{
		public String name;
		public String action;
	}
	
	@Path("/update_profile")	
    @POST
    @Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public GetProfile updateProfile(UpdateProfile args)
	{
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		if (args.name != null)
		{
			profile.name = args.name.trim();
		}
		if ("reset".equals(args.action))
		{
			profile.votes.clear();
			profile.invAffinities.clear();
		}
		return getProfile();
	}	
	/** saves the current profile. If it's a new profile, assigns an ID to it
	 * and adds it ot the WebUser. Saves the current WebUser if needed.
	 * 
	 * @return
	 */
	@Path("/save_profile")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response saveProfile()
	{
		GBEngine engine = this.getGBEngine(); 
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		if (session.user.ID == null)
		{
			throw new IllegalArgumentException("Only registered users can save profiles");
		}
		boolean updateWebUser = (profile.ID == null);
		if (Utils.IsEmpty(profile.name))
		{
			profile.name = "Created on " + Utils.formatDateTime(new Date());
		}
		engine.saveProfile(profile);
		
		if (updateWebUser)
		{
			if (!session.user.profiles.contains(profile.ID))
			{
				session.user.profiles.add(profile.ID);
			}
			engine.saveWebUser(session.user);
			
		}
		GetProfile res = new GetProfile();
		res.fromGBProfile(profile, engine);
		
		return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
				.build()
			;	
	}
	
	@Path("/set_vote")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response setVote(@QueryParam("billID") Integer billID, @QueryParam("say") String say)
	{
		GBEngine engine = this.getGBEngine(); 

		GBVote.SayEnum argSay = Utils.tryParseEnum(say, GBVote.SayEnum.NONE);
		GBBill bill = engine.getBill(billID, true);
		if (argSay == GBVote.SayEnum.NONE || bill == null)
		{
			throw new IllegalArgumentException("Invalid arguments: " + say + ", " + billID);
		}
		if (bill.status != GBBill.StatusEnum.PUBLISHED)
		{
			throw new IllegalArgumentException("Bill #" + billID + " is not open for voting (" + bill.status.name() + ")");
		}
		
		Web4Session session = getWeb4Session();
		GBProfile profile = session.currentProfile;
		GBVote oldVote = profile.getVote(billID);
		if (oldVote != null)
		{
			throw new IllegalArgumentException("Bill #" + billID 
					+ " is already decided: " + oldVote.say.name()
					+ " on " + Utils.formatDateTime(oldVote.sayDate)
			);
		}
		GBVote newVote = new GBVote();
		newVote.billID = bill.ID;
		newVote.say = argSay;
		newVote.sayDate = new Date();
		profile.addVote(bill.getInvAffinityValues(engine.getSize()), newVote );
		
		GetProfile res = new GetProfile();
		res.fromGBProfile(profile, engine);
		
		return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
				.build()
			;	
	}
}
