package net.vbelev.bothall.web;

import java.io.IOException;
import java.util.*;
import net.vbelev.utils.*;
import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.*;

/** 
 * A subclass of BHBoard (engine plus pacman-style board) 
 * with a set of objects to run a session over an engine.
 * Session is shared across threads (multiple clients).
 */
public class BHSession extends BHBoard
{
	
	public static class PS_STATUS
	{
		public static final int NEW = 0;
		public static final int ACTIVE = 1;
		public static final int PAUSE = 2;
		/** All commands are rejected except "stage"; all effects are cleared - next stage or end*/
		public static final int STAGEEND = 3;
		/** The DEAD status flags the session for removal: everybody disconnects and shuts down */
		public static final int DEAD = 4;
		
		public static String toString(int v)
		{
			switch (v)
			{
				case NEW: return "New";
				case ACTIVE: return "Active";
				case PAUSE: return "Pause";
				case STAGEEND: return "Done";
				case DEAD: return "Dead";
				default: return "" + v;
			}
		}
	}

	/**
	 * These commands are processed by BHSession itself, not by the subclasses 
	 */
	public static class COMMAND
	{
		/**
		 * JOINCLIENT is processed by the listener (StreamServer)
		 * and not passed to BHSession for processing at all
		 */
		public static final String JOINCLIENT = "JOINCLIENT".intern();
		/** Release the agent and close the connection (does not stop the session itself */
		public static final String CLOSE = "CLOSE".intern();
		public static final String JOIN = "JOIN".intern(); //inrArgs: [direction]
		public static final String CREATE = "CREATE".intern();
		public static final String CYCLE = "CYCLE".intern();
		//public static final String DIE = "die".intern();
		//public static final String ROBOT = "robot".intern(); // stringArgs: [clientKey, robotType], where clientKey may be null
		
		protected COMMAND() {}
	}
	/** Note that the update bin is not serialized, so as to support json clients.
	 *  */
	public static class UpdateBinOld
	{
		public final List<BHClient.Cell> cells = new ArrayList<BHClient.Cell>();
		public final List<BHClient.Item> items = new ArrayList<BHClient.Item>();
		public final List<BHClient.Mobile> mobiles = new ArrayList<BHClient.Mobile>();
		public final List<BHClient.Buff> buffs = new ArrayList<BHClient.Buff>();
		public final List<BHClient.Message> messages = new ArrayList<BHClient.Message>();
		public BHClient.Status status;
	}
	
	/** Event handler for the engine.publishEvent */
	public static class PublishEventArgs extends EventBox.EventArgs
	{
		public long timecode;
		
		public PublishEventArgs(long timecode)
		{
			this.timecode = timecode;
		}
	}
	
	
	private static int sessionInstanceSeq = 0;
	private static final ArrayList<BHSession> sessionList = new ArrayList<BHSession>();
	public static final Object lock = new Object();
	
	private int sessionID;
	private String sessionDescription;
	private String sessionKey;

	//protected BHBoard engine;

	public long engineTimecode = 0;
	public boolean isProtected = false;
	public final Map<Integer, String> itemCereals;
	public final Map<Integer, BHClient.Item> itemExports;
	public final Map<Integer, String> mobileCereals;
	public final Map<Integer, BHClient.Mobile> mobileExports;
	public final DryCereal dryer = new DryCereal();
	public final Date createdDate = new Date();

	
	public final EventBox.Event<PublishEventArgs> publishEvent = new EventBox.Event<PublishEventArgs>();
	private BHStorage storage; // = new BHStorage();
	
	/** values from PS_STATUS */
	public int sessionStatus = PS_STATUS.NEW;
	
	public static BHSession createSession()
	{
		BHSession s = new BHSession();
		registerSession(s);
		
		return s;
	}
	
	public static boolean destroySession(BHSession s)
	{
		synchronized(lock)
		{
			if (!sessionList.remove(s))
				; // whatever
				//return false;			
		}
		s.stopCycling();
		s.getMessages().clear();
		s.sessionID = 0;
		s.sessionKey = null;
		
		return true;
	}
	
	
	public static int getSessionCount()
	{
		return sessionList.size();
	}
	
	public static List<BHSession> getSessionList()
	{
		ArrayList<BHSession> res = new ArrayList<BHSession>();
		
		synchronized(lock)
		{
			res.addAll(sessionList);
		}
		return res;
	}
	
	public static String generateSessionKey(int length) 
	{
		int cnt = 0;
		if (length <= 0) return "";
		
		do
		{
			String res = Utils.randomString(length);			
			BHSession s = getSession(res);
			if (s == null)
			{
				return res;
			}
		} while (cnt < 10);
		throw new Error("Failed to create a unique password of length " + length);
	}	
	
	private static void registerSession(BHSession s)
	{
		s.sessionID = ++sessionInstanceSeq;
		synchronized(sessionList)
		{
			s.sessionKey = generateSessionKey(8);
			sessionList.add(s);
		}
	}
	
	public static BHSession getSession(Integer id)
	{
		if (id == null || id <= 0) return null;
		synchronized(sessionList)
		{
			for (BHSession s : sessionList)
			{
				if (s.sessionID == id) return s;
			}
		}
		return null;
	}	
	
	public static BHSession getSession(String key)
	{
		if (Utils.IsEmpty(key)) return null;
		synchronized(lock)
		{
			for (BHSession s : sessionList)
			{
				if (key.equals(s.sessionKey)) return s;
			}
		}
		return null;
		
	}
	
	public BHClientRegistration createAgent()
	{
		BHClientRegistration agent = BHClientRegistration.createAgent();
		agent.sessionID = this.getID();
		agent.subscriptionID = this.getMessages().addSubscription();
		
		return agent;
	}

	public void detachAgent(BHClientRegistration agent)
	{
		if (agent == null  || agent.getID() == 0) return;
		// we need to check the session and possibly stop it...
		
		BHSession s = this; //PacmanSession.getSession(agent.sessionID);	
		if (s != null)
		{
			if (s.getID() != agent.sessionID)
			{
				throw new IllegalArgumentException("client session Id (" + agent.sessionID + ") does not match the session id (" + s.getID()  + ")");
			}
			s.getEngine().getMessages().removeSubscription(agent.subscriptionID);
		}
		agent.subscriptionID = 0;
		agent.sessionID = 0;
		agent.detach();
	}
	
	private void _reset()
	{
		itemCereals.clear();
		itemExports.clear();
		mobileCereals.clear();
		mobileExports.clear();
	}
	
	protected BHSession()
	{
		itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
		mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
		storage = new BHStorage();

		registerSession(this);
	}
	
	protected BHSession(BHStorage storage)
	{
		itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
		mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
		mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());

		this.storage = storage;
		
		registerSession(this);		
	}
	
	@Override
	public void reset()
	{
		_reset();
	}
	
	/*
	private BHSession(BHBoard e)
	{		
		this();
		
		engine = e;
		engine.clientCallback = new EngineCallback();
	}
	
	private BHSession(BHBoard e, BHBoard.IClientCallback callback)
	{
		this();
		
		engine = e;
		engine.clientCallback = callback;
	}
	*/
	public void finalize()
	{
		destroySession(this);
	}
	
	@Override
	public long publish()
	{
		//System.out.println("session publish called! tc=" + timecode);
		synchronized (this.getMutex())
		{
		long newTimecode = super.publish();
		
		for (BHCollection.Atom a : getCollection().allByTimecode(newTimecode))
		{
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
				//cereal = null;
			}
		}
		BHSession.this.engineTimecode = newTimecode; // this.timecode
		publishEvent.trigger(new BHSession.PublishEventArgs(timecode));
		
		return newTimecode;
		}
	}
	
	@Override
	public void processAction(BHEngine.Action action)
	{
		// 1) priority actions that work on stopped sessions
		if (action.actionType == BHBoard.ACTION.STOPCYCLING)
		{
			super.processAction(action);
			BHSession.this.sessionStatus = PS_STATUS.PAUSE;
			return;
		}
		
		super.processAction(action);
	}
	@Override
	public boolean processBuff(BHEngine.Buff buff)
	{
		return super.processBuff(buff);		
	}		
	
	@Override
	public void processTriggers()
	{
		super.processTriggers();		
	}
	
	
	public class EngineCallback_depr //implements BHBoard.IClientCallback
	{
		public void onPublish(long timecode)
		{
			/*
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
			*/
		}
		
		public void processAction(BHEngine.Action action)
		{
			/*
			// 1) priority actions that work on stopped sessions
			if (action.actionType == BHOperations.ACTION_STOPCYCLING)
			{
				engine.stopCycling();
				BHSession.this.sessionStatus = PS_STATUS.PAUSE;
				return;
			}
			
			BHOperations.processAction(engine, action);
			*/
		}

		public boolean processBuff(BHEngine.Buff buff)
		{
			return false; //BHOperations.processBuff(engine, buff);		
		}		
		
		public void processTriggers()
		{
			//BHOperations.processTriggers(engine);		
		}
	}
	
	public int getID() { return sessionID; }
	
	public String getSessionKey() { return sessionKey; }
	
	public String getDescription() { return Utils.NVL(sessionDescription, "Session #" + sessionID); }
	
	public BHBoard getEngine() { return this; }
	
	public BHStorage getStorage() {return storage; }
	
	public BHClient.UpdateBin getUpdate(long timecode, int subscriptionID, int mobileID)
	{
		return storage.getUpdate(this, timecode, subscriptionID, mobileID);
	}
	/**
	 * Takes a client command wrapped in BHClient.Command and returns 
	 * a commands' execution result, wrapped in BHClient.Command, or an Error
	 * 
	 * This method should be overridden in a subclass. 
	 */
	public BHClient.IElement processCommand(BHClientRegistration agent, BHClient.Command cmd)
	{
		return null;
	}
	
	/**
	 * A special case: commands like "login", "create session", "join session"
	 * can be performed without a BHClientAgent
	 */
	public static BHClient.IElement processCommand(String clientKey, BHClient.Command cmd)
	{
		BHClientRegistration agent = null;
		BHClient.IElement res;
		if (clientKey != null)
		{
			agent = BHClientRegistration.getClient(null, clientKey);
		}
		
		// server-level commands do not require an agent
		if (COMMAND.CREATE.equals(cmd.command))  // [userKey, session type, is protected]
		{
			String userKey = cmd.stringArgs.length > 0? cmd.stringArgs[0] : "";
			String sessionType = cmd.stringArgs.length > 1? cmd.stringArgs[1] : "";
			String isProtected = cmd.stringArgs.length > 2? cmd.stringArgs[2] : "";
			
			boolean success = true;
			
			if (!BHUser.defaultUser.isValidUserKey(userKey))
			{
				success = false;
				res = new BHClient.Error(0, "Invalid  user key");
			}
			else if (!Utils.IsEmpty(sessionType) && !"pacman".equals(sessionType))
			{
				success = false;
				res = new BHClient.Error(0, "Invalid  session type");
			}
			if (success)
			{
				BHSession s = PacmanSession.createSession();
				BHClient.Command resCommand = new BHClient.Command(1, 1);
				res = resCommand;
				resCommand.command = "SESSION";
				resCommand.intArgs[0] = s.sessionID;
				resCommand.stringArgs[0] = s.sessionKey;
			}
			else
			{
				res = null; // won't happen
			}
			return res;
		}
		else if (cmd.command.equals(COMMAND.CYCLE)) // intArgs: [], stringArgs: [sessionKey, mode]; mode Y = start, N = stop, O = one cycle; 
		{
			String sessionKey = cmd.stringArgs.length > 0? cmd.stringArgs[0] : "";
			String mode = cmd.stringArgs.length > 1? cmd.stringArgs[1] : "";
			
			BHClient.Command resCommand = new BHClient.Command(1, 0);
			resCommand.command = "SESSION";
			BHSession s = BHSession.getSession(sessionKey);
			res = resCommand;
			if (s == null)
			{
				res = new BHClient.Error(0, "Invalid session key");
			}
			else if (mode.equals("Y"))
			{
				s.getEngine().startCycling();
				s.sessionStatus = PS_STATUS.ACTIVE;
				resCommand.intArgs[0] = s.getID();
			}
			else if (mode.equals("N"))
			{
				s.getEngine().stopCycling();
				s.sessionStatus = PS_STATUS.PAUSE;
				resCommand.intArgs[0] = s.getID();				
			}
			else if (mode.equals("O"))
			{
				BHEngine.Action stopAction = new BHEngine.Action();
				stopAction.actionType  = BHBoard.ACTION.STOPCYCLING;
				
				s.getEngine().postAction(stopAction, 0);
				s.getEngine().startCycling();
				s.sessionStatus = PS_STATUS.ACTIVE;
				resCommand.intArgs[0] = s.getID();				
			}
			return res;			
		}
		else if (cmd.command.equals(COMMAND.JOIN)) // intArgs: [sessionID, atom ID], stringArgs: [userKey, sessionKey] return clientKey
		{
			BHClient.Command resCommand = new BHClient.Command(0, 1);
			res = resCommand;
			resCommand.command = "AGENT";
			int sessionID = cmd.intArgs[0];
			int atomID = cmd.intArgs[1];
			String userKey = cmd.stringArgs.length > 0? cmd.stringArgs[0] : "";
			String sessionKey = cmd.stringArgs.length > 1? cmd.stringArgs[1] : "";
			boolean success = true;
			BHSession s = BHSession.getSession(sessionID);
			
			if (s.isProtected && (Utils.IsEmpty(sessionKey) || !sessionKey.equals(s.getSessionKey())))
			{
				//res.stringArgs[0] = "";
				success = false;
			}
			else
			{
				synchronized(BHClientRegistration.lock)
				{
					if (atomID > 0)
					{
						// check that the atom is available
						List<BHClientRegistration> agents = BHClientRegistration.agentList(s.sessionID);
						for (BHClientRegistration a : agents)
						{
							if (a.atomID == atomID)
								//res.stringArgs[0] = ""; // already used; do not create agent
								success = false;
						}
					}
					if (success)
					{
						agent = s.createAgent();
						agent.atomID = atomID;
						agent.userKey = userKey;
						agent.userName = "User #" + userKey;
						resCommand.stringArgs[0] = agent.clientKey;
					}			
				}				
			}
			if (!success)
			{
				resCommand.stringArgs[0] = "";
			}
			return res;
		}
		else if (agent == null)
		{
			res = new BHClient.Error(0, "Invalid or missing client key");
			return res;
		}
		
			
		BHSession session = BHSession.getSession(agent.sessionID);
		if  (session == null)
		{
			res = new BHClient.Error(0, "Invalid or missing session ID: " + agent.sessionID);
			return res;
		}
		return session.processCommand(agent, cmd);
	}

	public static boolean postMessage(String clientKey, BHCollection.MessageTargetEnum target, int targetID, String message)
	{
		BHClientRegistration agent = null;
		BHSession s = null;
		if (clientKey != null)
		{
			agent = BHClientRegistration.getClient(null, clientKey);
		}
		if (agent != null && agent.sessionID > 0)
		{
			s = BHSession.getSession(agent.sessionID);
		}
		if (s != null)
		{
			s.getEngine().getMessages().addMessage(new BHMessageList.Message(target, targetID, message));
			return true;
		}
		return false;
	}
	
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
		mobile.moveTick =atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC); // + BHOperations.MOVE_SPEED;
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
	public UpdateBinOld getUpdateOld(int timecode, int subscriptionID, int mobileID)
	{
		UpdateBinOld res = new UpdateBinOld();
		
		for(BHCollection.Atom a : this.getCollection().allByTimecode(timecode + 1))
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
		
		for (BHLandscape.Cell c : this.getLandscape().cells)
		{
			if (c.getTimecode() > timecode)
			{
				BHClient.Cell cell = new BHClient.Cell();
				cell.id = c.getID();
				cell.terrain = c.getTerrain().name().intern();
				cell.x = c.getX();
				cell.y = c.getY();
				cell.z = c.getZ();
				
				res.cells.add(cell);
			}
		}
		
		res.status = new BHClient.Status();
		res.status.cycleLoad = this.cycleLoad;
		res.status.cycleMsec = (int)this.CYCLE_MSEC;
		res.status.sessionID = this.sessionID;
		res.status.controlledMobileID = 0; // we don't know the controlled id here. maybe it should be moved
		res.status.timecode = this.timecode;
		res.status.sessionStatus = BHSession.PS_STATUS.toString(this.sessionStatus); //this.isRunning? BHClient.Status.SessionStatus.ACTIVE : BHClient.Status.SessionStatus.STOPPED;
		//res.status.updateTS = engine.pub
		
		// everything is visible for now
		for (BHMessageList.Message m : this.getMessages().getMessages(subscriptionID))
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
		/*
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
		BHLandscape.Cell tCell = s.engine.getLandscape().getNextCell(me, direction);
		if (tCell.getTerrain() == BHLandscape.TerrainEnum.LAND)
		{
			// monsters won't move into each other
			boolean canMove = true;
			if (me.getGrade() == BHCollection.Atom.GRADE.MONSTER)
			{
				for(BHCollection.Atom a : s.engine.getCollection().atCoords(tCell, false))
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
			BHEngine.Buff moveBuff = new BHEngine.Buff();
			moveBuff.action = new BHEngine.Action(BHOperations.BUFF_MOVE, me.getID(), 0, 0);
			//moveBuff.actionType = BHOperations.BUFF_MOVE;
			//moveBuff.actorID = me.getID();
			//moveBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
			moveBuff.action.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
			
			BHEngine.Buff oldBuff = null;
			for (BHEngine.Buff b : s.engine.buffs)
			{
				if (!b.isCancelled 
						&& b.action.actorID == moveBuff.action.actorID 
						&& b.action.actionType == moveBuff.action.actionType
						//&& b.intProps[1] == direction
						)
				{
					oldBuff = b;
					break;
				}
			}
			
			if (oldBuff != null && oldBuff.action.intProps[1] != direction)
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
		

		*/
		return null;
	}
	
}
