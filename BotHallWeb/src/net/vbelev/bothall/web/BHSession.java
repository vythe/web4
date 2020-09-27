package net.vbelev.bothall.web;

import java.io.IOException;
import java.util.*;
import net.vbelev.utils.*;
import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.*;
import net.vbelev.bothall.core.BHOperations.BHAction;
import net.vbelev.bothall.core.BHOperations.BHBuff;

/** 
 * A set of objects to run a session over an engine.
 * Session is shared across threats (multiple clients).
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
	private BHEngine engine;

	public int engineTimecode = 0;
	public final Map<Integer, String> itemCereals;
	public final Map<Integer, BHClient.Item> itemExports;
	public final Map<Integer, String> mobileCereals;
	public final Map<Integer, BHClient.Mobile> mobileExports;
	public final DryCereal dryer = new DryCereal();

	
	public static BHSession createSession()
	{
		BHEngine e = BHEngine.loadFileEngine("/../data/pacman.txt");
		e.publish();
		e.CYCLE_MSEC = 200;
		return BHSession.createSession(e);
	}
	
	
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
	
	private static void registerSession(BHSession s)
	{
		s.sessionID = ++sessionInstanceSeq;
		synchronized(sessionList)
		{
			sessionList.add(s);
		}
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
	
	private BHSession()
	{
		itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
		mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
		
		registerSession(this);
	}
	
	private BHSession(BHEngine e)
	{		
		engine = e;
		engine.clientCallback = new EngineCallback();

		itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
		mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
		
		registerSession(this);
	}
	
	private BHSession(BHEngine e, BHEngine.IClientCallback callback)
	{
		engine = e;
		engine.clientCallback = callback;

		itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
		mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
		
		registerSession(this);	
	}
	
	public class EngineCallback implements BHEngine.IClientCallback
	{
		public void onPublish(int timecode)
		{
			//System.out.println("session publish called! tc=" + timecode);
	
			for (BHCollection.Atom a : engine.getCollection().allByTimecode(BHSession.this.engineTimecode))
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
			BHSession.this.engineTimecode = timecode;
		}
		
		public void processAction(BHAction action)
		{
			BHOperations.processAction(engine, action);
			
		}

		public boolean processBuff(BHBuff buff)
		{
			return BHOperations.processBuff(engine, buff);		
		}		
		
		public void processTriggers()
		{
			BHOperations.processTriggers(engine);		
		}
	}
	
	public int getID() { return sessionID; }
	
	public BHEngine getEngine() { return engine; }
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
		mobile.moveTick =atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC) + BHOperations.MOVE_SPEED;
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
	
	/**
	 * This method is an old copy of PacmanSession.doMove(), for archive purposes.
	 *  
	 * check if the direction is open and move there;
	 * if the direction is not open, but the mobile is moving - post the movement buff;
	 * else return null. 
	 */
	public static Integer doMove(BHSession s, int mobileID, int direction)
	{
		
		BHCollection.Atom me = s.getEngine().getCollection().getItem(mobileID);
		if (me == null)
		{
			return null; //"No atom id" + session.myID;
		}
		if (direction < 0 || direction >= BHLandscape.cellShifts.length)
		{
			return null; //return "Invalid direction: " + direction;
		}
		if (me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == direction)
		{
			return null; // already moving that way
		}
		BHLandscape.Cell tCell = s.engine.getLandscape().getCell(me.getX(), me.getY(), me.getZ(), direction);
		if (tCell.getTerrain() == BHLandscape.TerrainEnum.LAND)
		{
			// monsters won't move into each other
			boolean canMove = true;
			if (me.getGrade() == BHCollection.Atom.GRADE.MONSTER)
			{
				for(BHCollection.Atom a : s.engine.getCollection().atCoords(tCell))
				{
					if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER 
						&& (a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == 0
								|| a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == BHLandscape.cellShifts[direction][3]
						)
					)
					{
						canMove = false;
						break;
					}
				}
			}
			if (canMove)
			{
				BHOperations.BHAction action = new BHOperations.BHAction();
				action.actionType = BHOperations.ACTION_MOVE;
				action.actorID = mobileID;
				action.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
				
				s.engine.postAction(action, 0);
				return action.ID; //"action ID=" + action.ID;
			}			
		}
		else if (me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) != 0
				&&  me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) != direction
				)
		{
			//s.engine.g
			// the way buff_move works: 
			BHOperations.BHBuff moveBuff = new BHOperations.BHBuff();
			moveBuff.actionType = BHOperations.BUFF_MOVE;
			moveBuff.actorID = me.getID();
			moveBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
			moveBuff.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
			
			BHOperations.BHBuff oldBuff = null;
			for (BHOperations.BHBuff b : s.engine.buffs)
			{
				if (!b.isCancelled 
						&& b.actorID == moveBuff.actorID 
						&& b.actionType == moveBuff.actionType
						//&& b.intProps[1] == direction
						)
				{
					oldBuff = b;
					break;
				}
			}
			
			if (oldBuff != null && oldBuff.intProps[1] != direction)
			{
				oldBuff.isCancelled = true;
				oldBuff = null;			
			}

			if (oldBuff == null)
			{
				s.engine.postBuff(moveBuff);
			}
			
			return 0; // it's not null, but we do not know the action id yet
		}
		

		/*
		BHOperations.BHAction action = new BHOperations.BHAction();
		action.actionType = BHOperations.ACTION_MOVE;
		action.actorID = agent.controlledMobileID;
		action.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);

		
		s.engine.postAction(action, 0);
		return action.ID; //"action ID=" + action.ID;
		*/
		return null;
	}
	
}
