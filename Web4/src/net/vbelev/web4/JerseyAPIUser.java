package net.vbelev.web4;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
//import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.media.multipart.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import net.vbelev.web4.core.*;
import net.vbelev.web4.ui.*;
import net.vbelev.web4.utils.Utils;

/**
 * API methods for the web user maintenance: login, logout, register
 */
@Path("/")
public class JerseyAPIUser extends JerseyAPI
{

	// ====== Web Users =====	
		public static class GetUser
		{
			public Integer ID;
			public String name;
			public String status;
			public Integer currentProfileID;
			/** the selected group set ID (should match the current profile) */
			public Integer currentSetID;
			/** an ID would be better, but... a new set? */
			public String editingSetTitle; 
			/**  it's a hashtable, because Javascript will convert it to an object, not an array */
			public Hashtable<Integer, String> profiles;
			public Hashtable<Integer, String> groupSets;
			
			public void FromWebUser(WebUser user, GBEngine engine, Web4Session session) //Integer profileID, int setID)
			{
				List<Integer> profiles = new ArrayList<Integer>();
				synchronized(user) // why do i need it synchronized?
				{
					this.ID = user.ID;
					this.name = Utils.NVL(user.name, "(Unknown)");
					this.status = user.status.toString();
					//this.currentProfileID = profileID;
					if (session.editingSet != null)
					{
						this.editingSetTitle = Utils.NVL(session.editingSet.title, "(New Set)");
					}
	
					profiles.addAll(user.profiles);
				}

				this.groupSets = new Hashtable<Integer, String>();
				for (GBGroupSet s : engine.groupList.stream()
						.sorted(Comparator.comparing(GBGroupSet::getSortName))
						.toArray(GBGroupSet[]::new)
						) // these streams are too much pain				
				{
					this.groupSets.put(s.ID, Utils.NVL(s.title, ""));
				}

				this.profiles = new Hashtable<Integer, String>();
				this.currentProfileID = null;
				//this.profiles.put(null,  "(New Profile)");
				for (Integer pid : profiles)
				{
					GBProfile f = engine.getProfile(pid);
					if (f != null && f.setID != null && f.setID == this.currentSetID) 
					{
						this.profiles.put(pid,  f.name);
						if (pid == session.currentProfile.ID)
						{
							this.currentProfileID = pid;
						}
					}
				}
				
			}
		}
		
		@Path("/get_user")	
	    @GET
	    @Produces(MediaType.APPLICATION_JSON)
		public GetUser getUser()
		//public Response getUser()
		{
			GetUser res = new GetUser();
			
			GBEngine engine = getGBEngine(false);
			Web4Session session = getWeb4Session();
			WebUser u = session.user;
			res.FromWebUser(u, engine, session); //.currentProfile.ID, Utils.NVL(session.currentProfile.setID, 0));
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
		public GetUser updateUser(UpdateUserModel args)
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
		public GetUser initUser(@QueryParam("login") String login, @QueryParam("withProfile") String withProfile)
		{
			Web4Session session = getWeb4Session();
			if (session.user.ID != null)
			{
				throw new IllegalArgumentException("The current user is already initialized (ID=" + session.user.ID + ")");
			}
			login = Utils.NVL(login, session.user.login, "").trim().toLowerCase();
			if (!WebUser.loginPattern.matcher(login).matches())
			{
				throw new IllegalArgumentException("Login name is not valid: " + login);
			}
			
			GBEngine engine = this.getGBEngine(false); 
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
			GetUser res = new GetUser();
			res.FromWebUser(session.user, engine, session); //.currentProfile.ID, Utils.NVL(session.currentProfile.setID, 0));
			return res;
		}
		
		@Path("/login")	
	    @GET
	    @Produces(MediaType.APPLICATION_JSON)
		public GetUser loginUser(@QueryParam("login") String login, @QueryParam("password") String password)
		{
			Web4Session session = getWeb4Session();
			if (session.user.ID != null)
			{
				throw new IllegalArgumentException("There is a logged-in user " + session.user.name + " already, (ID=" + session.user.ID + ")");
			}
			if (Utils.IsEmpty(login))
			{
				throw new IllegalArgumentException("Login name is missing");
			}
			login = login.trim().toLowerCase();
			if (!WebUser.loginPattern.matcher(login).matches())
			{
				throw new IllegalArgumentException("The login name is not valid: " + login);
			}
			
			GBEngine engine = this.getGBEngine(false); 
			Hashtable<String, Integer> userIndex = new Hashtable<String, Integer>();
			engine.loadWebUserIndex(userIndex);
			
			Integer userID = userIndex.getOrDefault(login,  null);
			WebUser dbUser = engine.loadWebUser(userID);
			
			if (dbUser != null && dbUser.passwordKind != WebUser.PasswordKindEnum.NONE)
			{
				if (dbUser.passwordKind == WebUser.PasswordKindEnum.PLAIN)
				{
					if (!Utils.NVL(dbUser.password, "").equals(Utils.NVL(password, "")))
					{
					dbUser = null;
					}
					else
					{
						throw new IllegalArgumentException("Unsupported password kind: " + dbUser.passwordKind);
					}
				}
			}
			if (userID == null)
			{
				throw new IllegalArgumentException("Login failed: " + login);
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
		public GetUser logoutUser()
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
			
}
