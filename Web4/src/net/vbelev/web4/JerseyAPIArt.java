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
 * API methods for writing the story: group editing, bill editing
 * @author Vythe
 *
 */
@Path("/")
public class JerseyAPIArt extends JerseyAPI
{

// ==== Group Editing ====
	@Path("/edit_group_set")
	@GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetGroupSet editGroupSet(@QueryParam("id") Integer setID)
	{
		GetGroupSet res = new GetGroupSet();
		
		GBEngine engine = this.getGBEngine(false);
		Web4Session session = this.getWeb4Session();
		GBGroupSet gblist = session.editingSet;

		if (setID == null && gblist == null)
		{
			throw new IllegalArgumentException("No group set selected for editing");
		}
		else if (setID != null)
		{
			gblist = engine.getGroupSet(setID);
			if (gblist == null)
			{
				throw new IllegalArgumentException("Invalid set ID: " + setID);
			}
			session.editingSet = (GBGroupSet)gblist.clone(); // need to clone this!
		}
		res.fromGBGroupSet(gblist);
		return res;
	}
	
	public static class UpdateGroupSet
	{
		public String title;
		public String description;
	}
	
	@Path("/update_group_set")	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public GetGroupSet updateGroupSet(UpdateGroupSet gset)
	{
		//GBEngine engine = this.getGBEngine(false); 
		Web4Session session = this.getWeb4Session();
		GBGroupSet gblist = session.editingSet;

		if (gblist == null)
		{
			throw new IllegalArgumentException("No group set selected for editing");
		}

		gblist.title = Utils.NVL(gset.title, "").trim();
		gblist.description = Utils.NVL(gset.description, "").trim();
		
		GetGroupSet res = new GetGroupSet();
		res.fromGBGroupSet(gblist);
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
	public GetGroupSet updateGroup(UpdateGroup group)
	{
		//GBEngine engine = this.getGBEngine(false); 
		Web4Session session = this.getWeb4Session();
		GBGroupSet gblist = session.editingSet;

		if (Utils.IsEmpty(group.moniker) || Utils.IsEmpty(group.name))
		{
			throw new IllegalArgumentException("Moniker and name must not be empty");					
		}
		if (gblist == null)
		{
			throw new IllegalArgumentException("No group set selected for editing");
		}
		group.moniker = group.moniker.trim();
		group.name = group.name.trim();

		GBGroup resGroup = gblist.getGroup(group.moniker);
		if (resGroup != null && group.name.contentEquals(resGroup.name))
		{
			// do nothing
		}
		else if (resGroup != null)
		{
			resGroup.name = group.name;
			//engine.saveGroups();
		}
		else
		{
			int size = gblist.getSize();
			gblist.setSize(size + 1);
			resGroup = gblist.getGroup(size);
			resGroup.name = group.name;
			resGroup.moniker = group.moniker;
			
			//engine.saveGroups();
		}
		GetGroupSet res = new GetGroupSet();
		res.fromGBGroupSet(gblist);
		return res;
	}
	
	/* the old way
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
	...
		GetAffinity res = new GetAffinity(aff, engine);
		//return Response.ok(res)
		//		//.header("Access-Control-Allow-Origin", "*")
		//		.build();
	}
	*/
	
	/** Update one affinity value in the currently edited group set. Recalculate and return the whole group set */
	@Path("/set_affinity")	
    @GET
    //@Consumes(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
	public GetGroupSet setAffinity(@QueryParam("moniker") String moniker, @QueryParam("toMoniker") String toMoniker, @QueryParam("value") Double value)
	{
		//GBEngine engine = this.getGBEngine(false);
		Web4Session session = this.getWeb4Session();
		GBGroupSet gblist = session.editingSet;
		
		if (gblist == null)
		{
			throw new IllegalArgumentException("No active updateable set");
		}
		 
		GBGroup g = gblist.getGroup(moniker);
		GBGroup g2 = gblist.getGroup(toMoniker);
		GBAffinity aff = null;
		
		if (g == null || g2 == null || g.ID == g2.ID)
		{
			throw new IllegalArgumentException("Invalid group monikers provided");
		}
		if (value == null) // can it be null?
		{
			g.setAffinity(g2.ID,  0, GBAffinity.QualityEnum.NONE);
			aff = gblist.getAffinity(g, g2.ID, true);
		}
		else if (value < 0 || value > 1) 
		{
			throw new IllegalArgumentException("Value must be between 0 and 1");
		}
		else
		{
			aff = g.setAffinity(g2.ID,  value, GBAffinity.QualityEnum.SET);
		}
		gblist.calculateAll();
		
		//engine.saveGroups();
		GetGroupSet res = new GetGroupSet();
		res.fromGBGroupSet(gblist);
		return res;
	}
	@Path("/action_group_set")	
    @GET
    //@Consumes(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
	public GetGroupSet actionGroupSet(@QueryParam("action") String action)
	{
		GBEngine engine = this.getGBEngine(false);
		Web4Session session = this.getWeb4Session();
		GBGroupSet gblist = session.editingSet;
		
		if("discard".equals(action)) {
			//Integer setID = gblist.ID;
			session.editingSet = null;			
			return null;  
		}
		
		if (gblist == null)
		{
			throw new IllegalArgumentException("No active updateable set");
		}
		 
		if("reset".equals(action)) {
			Integer setID = gblist.ID;
			session.editingSet = null;
			
			return editGroupSet(setID); // this should force a reload or reinit of the set 
		}
		else if ("save".equals(action)) 
		{
			if (Utils.IsEmpty(gblist.title))
			{
				throw new IllegalArgumentException("Title must not be empty");					
			}
	
			if (gblist.ID == null)
			{
				int newID = 0;
				for (GBGroupSet ss : engine.groupList)
				{
					if (ss.ID >= newID)
					{
						newID = ss.ID + 1;
					}
				}
				gblist.ID = newID;				
			}
			engine.saveGroup(gblist);
			for (GBBill b : engine.bills.values())
			{
				if (b.setID == gblist.ID)
				{
					b.calculateInvAffinities(gblist);
				}				
			}
					
		}
		else if ("newset".equals(action))
		{
			gblist = new GBGroupSet();

			session.editingSet = gblist;
		}
		GetGroupSet res = new GetGroupSet();
		res.fromGBGroupSet(gblist);
		return res;
	}	
// ==== Bill Editing ====

	/**
	 * A list of bills for editing and searching. When getBills shows only current bills,
	 * this will be used for viewing unfinished and archived bills, probably with a filter.
	 * This does not include your current in-memory bill. 
	 * @param mode
	 * @return
	 */
	@Path("/get_bills_archive")	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<GetBill> getBillsArchive(@QueryParam("mode") String mode, @QueryParam("force") String force)
	{
		ArrayList<GetBill> res = new ArrayList<GetBill>();
		GBEngine engine = this.getGBEngine(false); 
		Web4Session session = getWeb4Session();
		//GBProfile profile = session.currentProfile;
		//if (profile == null)
		//{
		//	profile = new GBProfile();
		//}
		if (force != null)
		{
			engine.loadBills();
		}
		Stream<GBBill> billStream = engine.bills.values().stream();
		if ("published".equals(mode))
		{
			billStream = billStream.filter(q -> q.status == GBBill.StatusEnum.PUBLISHED); 
		}
		else if ("new".equals(mode))
		{
			billStream = billStream.filter(q -> q.status == GBBill.StatusEnum.NEW); 
		}
		GBBill[] bills = billStream.toArray(GBBill[]::new);
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
			g.fromGBBill(bill, engine, null);
			g.actions = billActions(bill, null);
			res.add(g);
		}
		//return Response.ok(res)
				//.header("Access-Control-Allow-Origin", "*")
		//		.build()
		//	;
		return res;		
	}	
	
	/** Create a test bill with random properties. A test method */
	@Path("/test_bill")	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill testBill()
	{
		GBEngine engine = this.getGBEngine(false); 
		Web4Session session = getWeb4Session();
		GBGroupSet set = null;
		if (session.currentProfile != null && session.currentProfile.setID != null)
		{
			set = engine.getGroupSet(session.currentProfile.setID);
		}
		else
		{
			set = engine.getGroupSet(0);
		}
		GBBill bill = new GBBill();
		bill.publishedDate = new Date();
		bill.title = "Test " + Utils.formatDateTime(bill.publishedDate);
		bill.description = "Description of " + bill.title;
		bill.status = GBBill.StatusEnum.NEW;
		bill.setID = set.ID;
		
		// give it two affinities
		bill.setInvAffinity(engine.random.nextInt(set.getSize()), Math.random());
		bill.setInvAffinity(engine.random.nextInt(set.getSize()), Math.random());
		bill.calculateInvAffinities(set);
		
		engine.saveBill(bill);
		
		GetBill resBill = new GetBill();
		resBill.fromGBBill(bill, engine, null);
		//return getBill(bill.ID);
		return resBill;
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
		GBEngine engine = this.getGBEngine(false); 
		Web4Session session = getWeb4Session();
		
		if (session.editingBill != null
				&& reload == null
				&& (billID == null || Utils.equals(session.editingBill.ID, billID)))
		{
			bill = session.editingBill;
		}
		else if (billID == null)
		{
			session.editingBill = new GBBill();
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
				
//			if (bill.status != GBBill.StatusEnum.NEW)
//			{
//				throw new IllegalArgumentException("bill ID " + billID + " is " + bill.status + " and cannot be edited");
//			}
			session.editingBill = (GBBill)bill.clone();
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
		public Integer ID;
		public String title;
		public String description;
		public String action;
		public List<GetAffinity> invAffinities;
	}
	
	/**
	 * This will receive the bill object (UpdateBill), copy it to the session.currentBill, 
	 * recalculate affinities and return the whole bill (GetBill).
	 * The optional action will save the bill after updating (put it into storage).
	 * The trick is to send UpdateBill correctly. 
	 * @param update
	 * @return
	 */
	@Path("/update_bill")	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill updateBill(UpdateBill update)
	{
		GBEngine engine = this.getGBEngine(false); 
		Web4Session session = getWeb4Session();
		GBBill bill = session.editingBill;
		GBGroupSet set = null;
		if (bill == null || bill.ID != update.ID)
		{
			if (update.ID != null)
			{
				bill = engine.getBill(update.ID, true);
			}
			else
			{
				bill = new GBBill();
			}
			session.editingBill = bill;
		}
		if (bill.setID == null)
		{
			if (session.currentProfile != null && session.currentProfile.setID != null)
			{
				set = engine.getGroupSet(session.currentProfile.setID);
			}
			else
			{
				set = engine.getGroupSet(0);
			}
			bill.setID = set.ID;
		}
		else
		{
			set = engine.getGroupSet(bill.setID);
		}
			
		List<String> actions = this.billActions(bill, null);
		
		if (!Utils.InList("edit", actions)) //(false && bill.status != GBBill.StatusEnum.NEW)
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
				GBGroup g = set.getGroup(ga.toMoniker);
				GBAffinity.QualityEnum q = Utils.tryParseEnum(ga.quality, GBAffinity.QualityEnum.SET);
				if (g != null && q == GBAffinity.QualityEnum.SET)
				{
					bill.setInvAffinity(g.ID, ga.value);
				}
			}
			bill.calculateInvAffinities(set);
		}
		
		if ("save".equals(update.action))
		{
			if (!Utils.InList("edit", actions)) //(false && bill.status != GBBill.StatusEnum.NEW)
			{
				throw new IllegalArgumentException("bill ID " + bill.ID + " is " + bill.status + " and cannot be saved");
			}
					
			engine.saveBill(bill);
		}
		else if ("publish".equals(update.action))
		{
			bill.status = GBBill.StatusEnum.PUBLISHED;
			engine.saveBill(bill);
		}
		return editBill(null, null);
	}
	
	/** The method will publish or delete the given bill and return GetBill, 
	 * but it will not change the session.currentBill (except when deleting)
	 * @param billID
	 * @param action
	 * @return
	 */
	@Path("/action_bill")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill actionBill(@QueryParam("billID") Integer billID, @QueryParam("action") String action)
	{
		GBEngine engine = this.getGBEngine(false); 
		Web4Session session = getWeb4Session();
		GBBill bill;
		GetBill res = new GetBill();
		
		if (billID != null)
		{
			bill = engine.getBill(billID, true);
		}
		else
		{
			bill = session.editingBill;
		}
			
		if (bill == null)
		{
			throw new IllegalArgumentException("Invalid or missing bill, action cannot be completed");
		}
		List<String> actions = this.billActions(bill, null);
		
		if (action == null || action.length() == 0)
		{
			res.fromGBBill(bill, engine, null);			
		}
		else if ("cancel".equals(action))
		{
			if (!Utils.InList("cancel", actions))
			{
				throw new IllegalArgumentException("The bill " + bill.ID + " cannot be cancelled");
			}
			bill.status = GBBill.StatusEnum.CANCELLED;
			engine.saveBill(bill);
			if (session.editingBill != null && session.editingBill.ID == bill.ID)
			{
				session.editingBill = bill;
			}
			
			res.fromGBBill(bill, engine, null);			
		}
		else if ("delete".equals(action))
		{
			if (!Utils.InList("delete", actions))
			{
				throw new IllegalArgumentException("The bill " + bill.ID + " cannot be deleted");
			}
			if (bill.ID != null)
			{
				engine.deleteBill(bill.ID);
			}
			bill.status = GBBill.StatusEnum.CANCELLED;
			res.fromGBBill(bill, engine, null);
			// delete it here
		}
		else if ("publish".equals(action))
		{
			if (!Utils.InList("publish", actions))
			{
				throw new IllegalArgumentException("The bill " + bill.ID + " cannot be published");
			}
			bill.status = GBBill.StatusEnum.PUBLISHED;
			bill.publishedDate = new Date();
			engine.saveBill(bill);
			res.fromGBBill(bill, engine, null);
		}
		else if ("close".equals(action))
		{
			if (!Utils.InList("close", actions))
			{
				throw new IllegalArgumentException("The bill " + bill.ID + " cannot be closed");
			}
			bill.status = GBBill.StatusEnum.CLOSED;
			engine.saveBill(bill);
			res.fromGBBill(bill, engine, null);
		}
		
		return res;
	}
	/**
	 * the full updateBill() is heavy, it causes an OPTIONS call.
	 * So, to support simple recalcs, we make a GET update of one aff value only.
	 * This method will not load a bill by ID, but it will create a blank one if needed
	 * @param update
	 * @return
	 */
	@Path("/update_bill_aff")	
    @GET
    //@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public GetBill updateBillAff(@QueryParam("moniker") String moniker, @QueryParam("value") double value)
	{
		GBEngine engine = this.getGBEngine(false); 
		Web4Session session = getWeb4Session();
		GBBill bill = session.editingBill;
		GBGroupSet set = null;
		
		if (bill == null)
		{
			bill = new GBBill();
			if (session.currentProfile != null && session.currentProfile.setID != null)
			{
				set = engine.getGroupSet(session.currentProfile.setID);
			}
			else
			{
				set = engine.getGroupSet(0);
			}
			bill.setID = set.ID;
			
			session.editingBill = bill;
		}
		else
		{
			set = engine.getGroupSet(bill.setID);
		}
		if (set == null) // a fallback scenario, it shouldn't happen
		{
			bill = new GBBill();
			bill.setID = 0;
			set = engine.getGroupSet(bill.setID);
			session.editingBill = bill;
		}
		
		GBGroup g = set.getGroup(moniker);
		if (g != null)
		{
			bill.setInvAffinity(g.ID, value);
		}
		bill.calculateInvAffinities(set);

		return editBill(null, null);
	}
	
	
}
