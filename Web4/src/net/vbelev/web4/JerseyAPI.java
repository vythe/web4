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
		aff = engine.getAffinity(g, g2.ID, false);
		
		GetAffinity res = new GetAffinity(aff, engine);
		return Response.ok(res).header("Access-Control-Allow-Origin", "*").build();
	}
}
