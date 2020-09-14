package net.vbelev.bothall.web;

import java.io.IOException;
import java.util.*;
import net.vbelev.utils.*;
import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.*;

/** 
 * A set of objects to run a session over an engine.
 * Session is shared across threats (multiple clients).
 * @author Vythe
 *
 */
public class BHSession
{
	
	/** Note that the update bin is not serialized, so as to support json clients.
	 *  */
	public static class UpdateBin
	{
		public final List<BHClient.Cell> cells = new ArrayList<BHClient.Cell>();
		public final List<BHClient.Item> items = new ArrayList<BHClient.Item>();
		public final List<BHClient.Mobile> mobiles = new ArrayList<BHClient.Mobile>();
		public final List<BHClient.Buff> buffs = new ArrayList<BHClient.Buff>();
		public final List<BHClient.Message> messages = new ArrayList<BHClient.Message>();
		public BHClient.Status status;
	}
	
	private static int sessionInstanceSeq = 0;
	private static final ArrayList<BHSession> sessionList = new ArrayList<BHSession>();
	
	private int sessionID = 0;
	public final BHEngine engine;

	private int engineTimecode = 0;
	private final Map<Integer, String> itemCereals;
	private final Map<Integer, BHClient.Item> itemExports;
	private final Map<Integer, String> mobileCereals;
	private final Map<Integer, BHClient.Mobile> mobileExports;
	private final DryCereal dryer = new DryCereal();

	
	public static BHSession createSession(BHEngine e)
	{
		BHSession s = new BHSession(e);
		s.sessionID = ++sessionInstanceSeq;
		synchronized(sessionList)
		{
			sessionList.add(s);
		}
		
		return s;
	}
	
	public static BHSession getSession(int id)
	{
		synchronized(sessionList)
		{
			for (BHSession s : sessionList)
			{
				if (s.sessionID == id) return s;
			}
		}
		return null;
	}	
	
	private BHSession(BHEngine e)
	{
		engine = e;
		engine.onPublish = (tc) -> onPublish(tc);

		itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
		mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
	
	}
	
	private void onPublish(int timecode)
	{
		//System.out.println("session publish called! tc=" + timecode);

		for (BHCollection.Atom a : engine.getCollection().allByTimecode(this.engineTimecode))
		{
			String cereal;
			if (a.getGrade() == BHCollection.Atom.GRADE.ITEM)
			{
				atomToItemCache(a);
			}
			else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER || a.getGrade() == BHCollection.Atom.GRADE.HERO)
			{
				atomToMobileCache(a);
			}
			else
			{
				cereal = null;
			}
		}
		this.engineTimecode = timecode;

	}
	
	public int getID() { return sessionID; }
	
	/*
	public final List<BHClientAgent> agents = new ArrayList<BHClientAgent>();	

	public BHClientAgent getAgent(int id)
	{
		synchronized(agents)
		{
			for (BHClientAgent a : agents)
			{
				if (a.agentID == id) return a;
			}
		}
		return null;
	}
	
	public void addAgent(BHClientAgent a)
	{
		a.subscriptionID = engine.getMessages().addSubscription();
		synchronized(agents)
		{			
			agents.add(a);
		}
	}
	*/
	
	/** This works over the published atom */
	public BHClient.Item atomToItemCache(BHCollection.Atom atom)
	{
		BHClient.Item item = new BHClient.Item();
		item.id = atom.getID();
		item.x = atom.getIntProp(BHCollection.Atom.INT_PROPS.X);
		item.y = atom.getIntProp(BHCollection.Atom.INT_PROPS.Y);
		item.z = atom.getIntProp(BHCollection.Atom.INT_PROPS.Z);
		item.itemtype = atom.getType();
		item.status = atom.getStatus();
		
		try
		{
			// we need to go through BHClient.Item to make sure the serialization is consistent.
			item.toCereal(dryer);
			String cereal = dryer.pull();
			itemExports.put(item.id, item);
			itemCereals.put(item.id, cereal);
			
			return item;
			/*
			dryer.addInt(atom.id);
			dryer.addInt(atom.intProps[Atom.INT_PROPS.X]);
			dryer.addInt(atom.intProps[Atom.INT_PROPS.Y]);
			dryer.addInt(atom.intProps[Atom.INT_PROPS.Z]);
			dryer.addString(atom.type);
			return dryer.pull();
			*/
		}
		catch (IOException x)
		{
			return null;			
		}
	}
	/** This works over the published atom.
	 * If it's too slow, we'll go back to the methods inside BHCollection
	 *  */
	public BHClient.Mobile atomToMobileCache(BHCollection.Atom atom)
	{
		BHClient.Mobile mobile = new BHClient.Mobile();
		mobile.id = atom.getID();
		mobile.x = atom.getIntProp(BHCollection.Atom.INT_PROPS.X);
		mobile.y = atom.getIntProp(BHCollection.Atom.INT_PROPS.Y);
		mobile.z = atom.getIntProp(BHCollection.Atom.INT_PROPS.Z);
		mobile.dir = atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
		mobile.status = atom.getStatus();
		mobile.mobiletype = atom.getType();
		mobile.name = atom.getStringProp(BHCollection.Atom.STRING_PROPS.NAME);
		
		try
		{
			mobile.toCereal(dryer);
			String cereal = dryer.pull();
			mobileExports.put(mobile.id, mobile);
			mobileCereals.put(mobile.id, cereal);
			
			return mobile;
			/*
			dryer.addInt(atom.id);
			dryer.addInt(atom.intProps[Atom.INT_PROPS.X]);
			dryer.addInt(atom.intProps[Atom.INT_PROPS.Y]);
			dryer.addInt(atom.intProps[Atom.INT_PROPS.Z]);
			dryer.addString(atom.type);
			return dryer.pull();
			*/
		}
		catch (IOException x)
		{
			return null;
			
		}
	}	
	
	/** returns an update set from the given timecode until now */
	public UpdateBin getUpdate(int timecode, int subscriptionID, int mobileID)
	{
		UpdateBin res = new UpdateBin();
		
		for(BHCollection.Atom a : engine.getCollection().allByTimecode(timecode + 1))
		{
			if (a.getGrade() == BHCollection.Atom.GRADE.ITEM)
			{
				BHClient.Item item = itemExports.get(a.getID());
				if (item == null)
				{
					item = atomToItemCache(a);
				}
				if (item != null)
				{
					res.items.add(item);
				}
			}
			else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER || a.getGrade() == BHCollection.Atom.GRADE.HERO)
			{
				BHClient.Mobile mobile = mobileExports.get(a.getID());
				if (mobile == null)
				{
					mobile = atomToMobileCache(a);
				}
				if (mobile != null)
				{
					res.mobiles.add(mobile);
				}
			}
		}
		
		for (BHLandscape.Cell c : engine.getLandscape().cells)
		{
			if (c.getTimecode() > timecode)
			{
				BHClient.Cell cell = new BHClient.Cell();
				cell.id = c.getID();
				cell.terrain = c.getTerrain().name();
				cell.x = c.getX();
				cell.y = c.getY();
				cell.z = c.getZ();
				
				res.cells.add(cell);
			}
		}
		
		res.status = new BHClient.Status();
		res.status.cycleLoad = engine.cycleLoad;
		res.status.cycleMsec = (int)engine.CYCLE_MSEC;
		res.status.sessionID = this.sessionID;
		res.status.controlledMobileID = 0; // we don't know the controlled id here. maybe it should be moved
		res.status.timecode = engine.timecode;
		res.status.sessionStatus = engine.isRunning? BHClient.Status.SessionStatus.ACTIVE : BHClient.Status.SessionStatus.STOPPED;
		//res.status.updateTS = engine.pub
		
		// everything is visible for now
		for (BHMessageList.Message m : engine.getMessages().getMessages(subscriptionID))
		{
			BHClient.Message message = new BHClient.Message();
			message.id = m.ID;
			message.message = m.message;
			message.targetID = m.targetID;
			message.targetType = m.target.name();
			res.messages.add(message);
		}

		return res;
	}
}
