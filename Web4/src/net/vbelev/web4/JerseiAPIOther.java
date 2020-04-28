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

@Path("/")
public class JerseiAPIOther extends JerseyAPI
{


	// ====== Groups =====	
		@Path("/get_groups")	
	    @GET
	    @Produces(MediaType.APPLICATION_JSON)
	    public GetGroupSet getGroups(@QueryParam("force") String force) {
	        //return Response.ok(httpHeaders.getRequestHeaders()).build();
			GBEngine engine = this.getGBEngine(force != null);
			Web4Session session = getWeb4Session();

			GetGroupSet res = new GetGroupSet();
			res.fromGBGroupSet(engine.getGroupSet(Utils.NVL(session.currentProfile.setID, 0)));
					
			return res;
			/*
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
			*/
		}
		
	// ====== Bills =====	
		@Path("/get_bill")	
	    @GET
	    @Produces(MediaType.APPLICATION_JSON)
		public GetBill getBill(@QueryParam("billID") Integer billID)
		{
			GetBill res = new GetBill();
			GBBill bill = null;
			GBEngine engine = this.getGBEngine(false); 
			Web4Session session = getWeb4Session();
			if (billID == null)
			{
				if (session.editingBill == null)
				{
					session.editingBill = new GBBill();
				}
				bill = session.editingBill; 
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
				session.editingBill = bill;
			}
			
			res.fromGBBill(bill, engine, session.currentProfile.votes);
			//return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
			//	.build()
			//;
			res.actions = billActions(bill, session.currentProfile.votes);
			return res;
		}

		@Path("/get_bills")	
		@GET
		@Produces(MediaType.APPLICATION_JSON)
		public List<GetBill> getBills(@QueryParam("force") String force)
		{
			ArrayList<GetBill> res = new ArrayList<GetBill>();
			GBEngine engine = this.getGBEngine(false); 
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
				g.actions = billActions(bill, profile.votes);
				res.add(g);
			}
			//return Response.ok(res)
					//.header("Access-Control-Allow-Origin", "*")
			//		.build()
			//	;
			return res;		
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
				GBGroupSet set = engine.getGroupSet(profile.setID);
				for (int ind = 0; ind < set.getSize(); ind++)
				{
					GBAffinity aff = new GBAffinity(ind, profile.getInvAffinity(ind));
					GetAffinity gaf = new GetAffinity(aff, set);
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
			GBEngine engine = this.getGBEngine(false); 
			Web4Session session = getWeb4Session();
			GBProfile profile = session.currentProfile;
			GetProfile res = new GetProfile();
			res.fromGBProfile(profile, engine);
			
//			return Response.ok(res)
//					//.header("Access-Control-Allow-Origin", "*")
//					.build()
//				;
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
		public Response loadProfile(@QueryParam("profileID") Integer profileID, @QueryParam("setID") Integer setID, @QueryParam("force") String force)
		{
			GBEngine engine = this.getGBEngine(false); 
			Web4Session session = getWeb4Session();
			GBProfile profile = null;
			GBGroupSet gblist = null;
			
			if (setID == null)
			{
				setID = Utils.NVL(session.currentProfile.setID, 0);
			}
			
			gblist = engine.getGroupSet(setID);
			if (gblist == null)
			{
				throw new IllegalArgumentException("Invalid set ID: " + setID);
			}
			
			if (force == null && Utils.equals(session.currentProfile.ID, profileID))
			{
				profile = session.currentProfile;
			}
			else if (profileID == null) // start a new profile
			{
				profile = new GBProfile();
				profile.setID = setID;
				
				session.currentProfile = profile;
			}
			else
			{
				profile = engine.loadProfile(profileID);
				if (profile == null)
				{
					return response500( "Invalid profileID: " + profileID); // we can just throw an exception
				}
			}
			
			if (profile.setID != setID) // get the first matching profile
			{
				profile = null;
				for (Integer userPID : session.user.profiles)
				{
					GBProfile userP = engine.getProfile(userPID);
					if (userP != null && userP.setID != null && userP.setID == setID)
					{
						profile = userP;
						break;
					}
				}
				if (profile == null)
				{
					profile = new GBProfile();
					profile.setID = setID;
				}
			}
			
			if (session.currentProfile != profile) // object comparison
			{
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
			/** only for new, unsaved profiles */
			public Integer setID; 
		}
		
		@Path("/update_profile")	
	    @POST
	    @Produces(MediaType.APPLICATION_JSON)
		@Consumes(MediaType.APPLICATION_JSON)
		public GetProfile updateProfile(UpdateProfile args)
		{
			Web4Session session = getWeb4Session();
			GBProfile profile = session.currentProfile;
			Integer newSetID = profile.setID;
			
			if (profile.ID == null && args.setID != null) // new profiles can select the group set.
			{
				newSetID = args.setID;
			}
			if (args.name != null)
			{
				profile.name = args.name.trim();
			}
			if ("reset".equals(args.action) || newSetID != profile.setID)
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
			GBEngine engine = this.getGBEngine(false); 
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
			GBEngine engine = this.getGBEngine(false); 
			Web4Session session = getWeb4Session();
			GBProfile profile = session.currentProfile;

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
			if (bill.setID != profile.setID)
			{
				throw new IllegalArgumentException("Bill and profile group sets do not match");
			}
		
			GBGroupSet set = engine.getGroupSet(profile.setID);
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
			profile.addVote(bill.getInvAffinityValues(set.getSize()), newVote );
			
			GetProfile res = new GetProfile();
			res.fromGBProfile(profile, engine);
			
			return Response.ok(res)
					//.header("Access-Control-Allow-Origin", "*")
					.build()
				;	
		}

}
