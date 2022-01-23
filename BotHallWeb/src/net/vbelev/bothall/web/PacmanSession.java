package net.vbelev.bothall.web;

import java.util.*;
import java.io.*;

import net.vbelev.bothall.client.BHClient;
import net.vbelev.bothall.client.PacmanClient;
import net.vbelev.bothall.client.StreamClient;
import net.vbelev.bothall.core.*;
import net.vbelev.utils.*;
/**
 * Here we put the methods specific to pacman
 *
 * Let's try this hierarchy: 1) BHEngine implements the main loop, doesn't know
 * anything about particular actions; 2) BHBoard implements a 3-d basic arena
 * (board): landscape, items, mobs; 3) Pacman extends BHBoard to implement
 * pacman details and rules
 *
 */
public class PacmanSession extends BHSession
{

	public static class INT_PROPS // extends BHCollection.Atom.INT_PROPS
	{
		private static final int COUNT = 8;
		/** here be session status from PS_STATUS */
		public static final int STATUS = 0;
		public static final int X = 1;
		public static final int Y = 2;
		public static final int Z = 3;
		// public static final int DX = 3;
		// public static final int DY = 4;
		// public static final int DZ = 5;
		/** base timecode for movements */
		public static final int MOVE_TC = 4;
		public static final int MOVE_DIR = 5;
		public static final int HERO_GOLD = 6;
		public static final int HERO_LIVES = 7;

		private INT_PROPS() {
		}
	}

	public static class STRING_PROPS
	{
		private static final int COUNT = 1;
		public static final int NAME = 0;
	}

	public static class ATOM
	{
		public static final String GOLD = "GOLD".intern();
		public static final String MONSTER = "MONSTER".intern();
		public static final String HERO = "HERO".intern();
		public static final String PAC = "PAC".intern();
		public static final String PORTAL = "PORTAL".intern();

		private ATOM() {
		}
	}

	/**
	 * Commands are business-level instructions, coming from outside (from
	 * users); Actions (BHEngine.Action) are internal, engine-level
	 * instructions. Commands are processed immediately but must only prepare
	 * and submit actions as needed; actions are queued in the engine and
	 * processed by processAction() in due course.
	 * 
	 * Need to fix processCommand() to make sure commands do not modify the
	 * engine state.
	 */
	public static class COMMAND extends BHSession.COMMAND
	{
		public static final String REFRESH = "refresh".intern();
		public static final String MOVE = "move".intern(); // inrArgs:
															// [direction]
		public static final String PACMAN = "pacman".intern();
		public static final String DIE = "die".intern();
		public static final String ROBOT = "robot".intern(); // stringArgs:
																// [clientKey,
																// robotType],
																// where
																// clientKey may
																// be null
		public static final String START = "start".intern(); // stringArgs:
																// [sessionKey]
		/** Load the next stage and put it into the paused state */
		public static final String STAGE = "stage".intern(); // stringArgs:
																// [sessionKey]
		public static final String PAUSE = "pause".intern(); // optional
																// intArgs:
																// [pausedBy]

		protected COMMAND() {
		}
	}

	public static class BUFF extends BHBoard.BUFF
	{
		/**
		 * After dying (from actionDie()), the hero gets this buff and
		 * resurrects after some time.
		 */
		public static final String RESURRECT = "RESURRECT".intern();
		/**
		 * Monsters with this buff will be eaten by the hero
		 */
		public static final String PACMONSTER = "PACMONSTER".intern();
		public static final String PACHERO = "PACHERO".intern();
		/**
		 * Port properties [target portal item ID, moveTC, post-shift direction,
		 * phase]. When entering the portal cell, set the buff with phase=0
		 * (leaving); after porting, set phase=1. The buff with phase==1 stays
		 * on until you leave the cell and prevents further porting.
		 */
		public static final String PORT = "PORT".intern();
		public static final String PAUSE = "PAUSE".intern(); // optional
																// stringArgs:
																// [message]
	}

	public static class ACTION extends BHBoard.ACTION
	{
		public static final String DIE = "DIE".intern();
		public static final String PAUSE = "PAUSE".intern();
	}
	// public static final String ACTION_DIE = "DIE".intern();

	public static final int PACMAN_DURATION = 1000;
	public static int MOVE_SPEED = 4;

	// public static final Object lock = new Object();
	// private static int sessionInstanceSeq = 0;
	// private static final ArrayList<PacmanSession> sessionList = new
	// ArrayList<PacmanSession>();

	// private int sessionID = 0;
	// private String sessionKey = "";
	// private BHEngine engine;

	// public int engineTimecode = 0;
	// public boolean isProtected = false;
	// public final Date createdDate = new Date();
	// public final BHStorage storage = new BHStorage();
	// public final EventBox.Event<BHSession.PublishEventArgs> publishEvent =
	// new EventBox.Event<BHSession.PublishEventArgs>();

	/** The list of coords where killed monsters and heroes resurrect */
	public final Map<Integer, BHLandscape.Coords> startingPoints = new java.util.Hashtable<Integer, BHLandscape.Coords>();

	/*
	 * public static PacmanSession createSession() { BHBoard e =
	 * PacmanSession.loadFile("/../data/pacman.txt"); e.CYCLE_MSEC = 200;
	 * PacmanSession s = PacmanSession.createSession(e);
	 * 
	 * return s; }
	 */

	/*
	 * public static int getSessionCount() { return sessionList.size(); }
	 */

	private final BHBoard engine;

	public int pacmanStage;

	private final List<BHLandscape.Coords> monsterPoints = new ArrayList<BHLandscape.Coords>();

	public static class PacmanStorage extends BHStorage
	{
		@Override
		public BHClient.UpdateBin getUpdate(BHBoard engine, long timecode, int subscriptionID, int mobileID)
		{
			BHClient.UpdateBin res = new BHClient.UpdateBin();
			PacmanClient.PacmanStatus status = new PacmanClient.PacmanStatus();

			res.status = status;

			super.getUpdate(res, engine, timecode, subscriptionID, mobileID);
			if (engine instanceof PacmanSession)
			{
				PacmanSession ps = (PacmanSession) engine;
				status.pacmanStage = ps.pacmanStage;
				res.status.sessionStatus = BHSession.PS_STATUS.toString(ps.sessionStatus);
			}

			return res;
		}
	}

	public PacmanSession() {
		super(new PacmanStorage());
		engine = this;
	}

	@Override
	public void reset()
	{
		super.reset();
		if (monsterPoints != null) // fields are initialized _after_ the super
									// constructor is called,
		{
			monsterPoints.clear();
		}
		if (startingPoints != null)
		{
			startingPoints.clear();
		}
	}

	public static PacmanSession createSession()
	{
		PacmanSession s = new PacmanSession();
		s.loadStage(1);
		/*
		 * s.loadFile("/../data/pacman.txt"); s.CYCLE_MSEC = 200;
		 * s.sessionStatus = PS_STATUS.NEW; //s.engine = e; //e.clientCallback =
		 * s.new EngineCallback();
		 * 
		 * s.publish(); for (BHCollection.Atom a : s.getCollection().all()) { if
		 * (a.getGrade() != BHCollection.Atom.GRADE.ITEM) {
		 * s.startingPoints.put(a.getID(), BHLandscape.coordsPoint(a)); } }
		 */
		return s;
	}

	public static PacmanSession getSession(Integer id)
	{
		BHSession res = BHSession.getSession(id);
		if (res instanceof PacmanSession)
		{
			return (PacmanSession) res;
		}
		/*
		 * if (id == null || id <= 0) return null; synchronized(lock) { for
		 * (PacmanSession s : sessionList) { if (s.sessionID == id) return s; }
		 * }
		 */
		return null;
	}

	public static PacmanSession getSession(String key)
	{
		BHSession res = BHSession.getSession(key);
		if (res instanceof PacmanSession)
		{
			return (PacmanSession) res;
		}
		/*
		 * if (Utils.IsEmpty(key)) return null; synchronized(lock) { for
		 * (PacmanSession s : sessionList) { if (key.equals(s.sessionKey))
		 * return s; } }
		 */
		return null;

	}
	/*
	 * public static boolean destroySession(PacmanSession s) {
	 * synchronized(lock) { if (!sessionList.remove(s)) return false; }
	 * s.getEngine().stopCycling(); s.getEngine().getMessages().clear(); return
	 * true;
	 * 
	 * } public static String generateSessionKey(int length) { int cnt = 0; if
	 * (length <= 0) return "";
	 * 
	 * do { String res = Utils.randomString(length); PacmanSession s =
	 * getSession(res); if (s == null) { return res; } } while (cnt < 10); throw
	 * new Error("Failed to create a unique password of length " + length); }
	 */
	public static List<PacmanSession> sessionList()
	{
		/*
		 * ArrayList<PacmanSession> res = new ArrayList<PacmanSession>();
		 * 
		 * synchronized(sessionList) { res.addAll(sessionList); //for
		 * (PacmanSession s : sessionList) //{ // res.add(s); //} }
		 */
		List<BHSession> r1 = BHSession.getSessionList();
		ArrayList<PacmanSession> res = new ArrayList<PacmanSession>();

		for (BHSession s : r1)
		{
			if (s instanceof PacmanSession) res.add((PacmanSession) s);
		}
		return res;
	}

	/*
	 * public BHClientAgent createAgent() { BHClientAgent agent =
	 * BHClientAgent.createAgent(); agent.sessionID = this.getID();
	 * agent.subscriptionID = this.getEngine().getMessages().addSubscription();
	 * 
	 * return agent; }
	 * 
	 * public void detachAgent(BHClientAgent agent) { if (agent == null ||
	 * agent.getID() == 0) return; // we need to check the session and possibly
	 * stop it...
	 * 
	 * PacmanSession s = this; //PacmanSession.getSession(agent.sessionID); if
	 * (s != null) { if (s.getID() != agent.sessionID) { throw new
	 * IllegalArgumentException("client session Id (" + agent.sessionID +
	 * ") does not match the session id (" + s.getID() + ")"); }
	 * s.getEngine().getMessages().removeSubscription(agent.subscriptionID); }
	 * agent.subscriptionID = 0; agent.sessionID = 0; agent.detach(); }
	 */

	/*
	 * public int getID() { return sessionID; }
	 * 
	 * public String getSessionKey() { return sessionKey; }
	 * 
	 * public BHEngine getEngine() { return engine; }
	 */

	/** This is for loading the map from a file, loadFile() */
	private static BHCollection.Atom addAtom(BHCollection coll, String type, int grade)
	{
		BHCollection.Atom res = new BHCollection.Atom(INT_PROPS.COUNT, STRING_PROPS.COUNT);
		res.setType(type);
		res.setGrade(grade);
		res = coll.addAtom(res);

		return res;
	}

	public void loadFile(String fileName)
	{
		BHBoard res = this; // new BHBoard();
		res.reset();

		try
		{
			InputStream is = BHEngine.class.getResourceAsStream(fileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			int x = -1;
			int y = -1;
			int z = 0;
			// BHCollection.Atom hero = null;
			BHLandscape landscape = res.getLandscape();
			BHCollection collection = res.getCollection();

			while ((line = br.readLine()) != null)
			{
				y++;
				StringReader sr = new StringReader(line);
				int c;
				x = -1;
				while ((c = sr.read()) != -1)
				{
					x++;
					// BHLandscape.TerrainEnum terrain;
					if (c == '#')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.STONE));
					}
					else if (c == ' ')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
					}
					else if (c == '@')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom hero = addAtom(collection, ATOM.HERO, BHCollection.Atom.GRADE.HERO);
						hero.setX(x);
						hero.setY(y);
						hero.setZ(z);
					}
					else if (c == 'M')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
						/*
						 * BHCollection.Atom monster = addAtom(collection,
						 * ATOM.MONSTER, BHCollection.Atom.GRADE.MONSTER);
						 * monster.setX(x); monster.setY(y); monster.setZ(z);
						 */
						monsterPoints.add(new BHLandscape.CoordsBase(x, y, z));
					}
					else if (c == '.')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom item = addAtom(collection, ATOM.GOLD, BHCollection.Atom.GRADE.ITEM);
						item.setX(x);
						item.setY(y);
						item.setZ(z);
					}
					else if (c == '*')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom item = addAtom(collection, ATOM.PAC, BHCollection.Atom.GRADE.ITEM);
						item.setX(x);
						item.setY(y);
						item.setZ(z);
					}
					else if (c == '=')
					{
						landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom item = addAtom(collection, ATOM.PORTAL, BHCollection.Atom.GRADE.ITEM);
						item.setX(x);
						item.setY(y);
						item.setZ(z);
					}
					else
					{
						// res.landscape.setCell(new BHLandscape.Cell(x, y, z,
						// BHLandscape.TerrainEnum.VOID));
						System.out.println("Unsupported mark [" + (char) c + "] when reading " + fileName);
					}
				}

			}
		}
		catch (IOException x)
		{
			System.out.println("Failed to read " + fileName);
			x.printStackTrace();
		}
		res.publish();
		res.timecode++; // make it at least 1 so the clients will load it
		// return res;
	}

	public void loadStage(int stage)
	{
		this.reset();
		this.loadFile("/../data/pacman.txt");

		// s.CYCLE_MSEC = 200;
		this.sessionStatus = PS_STATUS.NEW;
		this.pacmanStage = stage;
		BHCollection collection = this.getCollection();

		if (monsterPoints.size() > 0)
		{
			int monsterCount = stage + 2;
			for (int i = 0; i < monsterCount; i++)
			{
				BHCollection.Atom monster = addAtom(collection, ATOM.MONSTER, BHCollection.Atom.GRADE.MONSTER);
				monster.setCoords(monsterPoints.get(i % monsterPoints.size()));
				System.out.println("added monster, id=" + monster.getID() + ", coords=" + monster.getX() + ", " + monster.getY());
			}
		}
		this.publish();
		collection = this.getCollection();
		for (BHCollection.Atom a : collection.all())
		{
			if (a.getGrade() != BHCollection.Atom.GRADE.ITEM)
			{
				this.startingPoints.put(a.getID(), BHLandscape.coordsPoint(a));
			}
		}
	}

	@Override
	public BHClient.UpdateBin getUpdate(long timecode, int subscriptionID, int mobileID)
	{
		// BHClient.UpdateBin

		return getStorage().getUpdate(this, timecode, subscriptionID, mobileID);
	}

	@Override
	public long publish()
	{
		return super.publish();
	}

	@Override
	public void processAction(BHEngine.Action action)
	{
		// 1) priority actions that work on stopped sessions
		if (action.actionType == BHBoard.ACTION.STOPCYCLING)
		{
			super.processAction(action);
			return;
			// engine.stopCycling();
			// PacmanSession.this.sessionStatus = PS_STATUS.PAUSE;
			// return;
		}

		if (PacmanSession.this.sessionStatus == PS_STATUS.PAUSE)
		{
			System.out.println("PacmanSession.processAction, pause - " + action.toString());
			// the action came here through the Pause block - it means it was in a timer.
			// convert it to a buff and let it be
			Buff bPause = null;
			Buff bAction = null;
			synchronized (engine.buffs)
			{
				for (Buff b : engine.buffs)
				{
					if (b.action == null) continue;
					if (b.action.actionType == ACTION.PAUSE)
					{
						bPause = b;
					}
					if (b.action.ID == action.ID)
					{
						bAction = b;
					}
				}
			}
			if (bAction != null)
			{
				System.out.println("processAction, Pause: action already in a buff: " + bAction.toString());
				return; // this action is already in a buff
			}
			// if there is no valid Pause buff - something is wrong. Complete the action
			if (bPause != null && bPause.action instanceof PauseAction)
			{
				
				bAction = new Buff();
				bAction.action = action;
				bAction.ticks = 2;
				bAction.timecode = engine.timecode;
				engine.postBuff(bAction);
				//engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, action.actorID, "paused action converted to a buff: " + action.toString());				
				engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "paused action converted to a buff: " + action.toString());				
				System.out.println("processAction, Pause: action converted to a buff: " + bAction.toString());
				return;
			}
			else
			{
				System.out.println("processAction, Pause: no valid bPause: " + action.toString());
			}
		}

		if (PacmanSession.this.sessionStatus != PS_STATUS.ACTIVE
		&& PacmanSession.this.sessionStatus != PS_STATUS.PAUSE)
		{
			System.out.println("PacmanSession.processAction, status " + PacmanSession.this.sessionStatus + " - " + action.toString());
			return;
		}
		System.out.println("PacmanSession.processAction, normal - " + action.toString());
		// 2) normal actions that work only on running sessions
		BHCollection.Atom me = this.getCollection().getItem(action.actorID);
		if (action.actionType == PacmanSession.ACTION.DIE)
		{
			if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
			{
				return; // "No atom id" + session.myID;
			}
			PacmanSession.this.actionDie(action.actorID);
		}
		else
		{
			super.processAction(action);
		}

	}

	@Override
	public boolean processBuff(BHEngine.Buff buff)
	{
		if (buff.action.actionType == PacmanSession.BUFF.RESURRECT)
		{
			return PacmanSession.this.processBuffResurrect(buff);
		}
		else if (buff.action.actionType == PacmanSession.BUFF.PACMONSTER)
		{
			return PacmanSession.this.processBuffPacMonster(buff);
		}
		else if (buff.action.actionType == PacmanSession.BUFF.PACHERO)
		{
			return PacmanSession.this.processBuffPacHero(buff);
		}
		else if (buff.action.actionType == PacmanSession.BUFF.PORT)
		{
			return PacmanSession.this.processBuffPortal(buff);
		}
		else if (buff.action.actionType == BUFF.PAUSE)
		{
			return buffPause(buff);
		}
		else
		{
			return super.processBuff(buff);
		}
	}

	@Override
	public void processTriggers()
	{
		for (BHCollection.Atom a : this.getCollection().all())
		{
			if (a.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE) continue;

			if (a.getGrade() == BHCollection.Atom.GRADE.HERO)
			{
				// *) eat the gold here
				Collection<BHCollection.Atom> spot = this.getCollection().atCoords(a, false);
				if (spot.size() <= 1) continue; // nothing to see here
				for (BHCollection.Atom it : spot)
				{
					if (it.getID() == a.getID()) continue;
					if (it.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE) continue;

					if (it.getType() == PacmanSession.ATOM.GOLD)
					{
						int goldCount = a.getIntProp(PacmanSession.INT_PROPS.HERO_GOLD) + 1;
						a.setIntProp(PacmanSession.INT_PROPS.HERO_GOLD, goldCount);
						this.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, a.getID(), "Yum! Count=" + goldCount);
						it.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
					}
					else if (it.getType() == PacmanSession.ATOM.PAC)
					{
						triggerPacman();
						it.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
					}
					else if (it.getType() == PacmanSession.ATOM.PORTAL)
					{
						triggerPortal(a);
					}
				}
			}
			else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER)
			{
				int moveDir = a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
				// *) eat the hero here
				Collection<BHCollection.Atom> spot = this.getCollection().atCoords(a, false);
				if (spot.size() <= 1) continue; // nothing to see here
				for (BHCollection.Atom it : spot)
				{
					if (it.getID() == a.getID()) continue;
					if (it.getGrade() == BHCollection.Atom.GRADE.HERO)
					{
						/*
						 * BHOperations.BHAction action = new
						 * BHOperations.BHAction(); action.actionType =
						 * PacmanSession.ACTION_DIE; action.actorID =
						 * it.getID(); //action.intProps =
						 * Utils.intArray(s.engine.timecode, direction, 1,
						 * BHOperations.MOVE_SPEED);
						 * 
						 * 
						 * PacmanSession.this.getEngine().postAction(action, 0);
						 * //return action.ID; //"action ID=" + action.ID;
						 */

						BHEngine.Buff monsterBuff = this.getBuff(a.getID(), BUFF.PACMONSTER);
						if (monsterBuff != null && !monsterBuff.isCancelled)
						{
							it.setIntProp(INT_PROPS.HERO_GOLD, it.getIntProp(INT_PROPS.HERO_GOLD) + 100);
							this.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, it.getID(), "CHOMP!");
							actionDie(a.getID());
						}
						else
						{
							actionDie(it.getID());
							BHLandscape.Coords c = startingPoints.get(a.getID());
							a.setCoords(c);
							this.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, a.getID(), "M-M-M-CHOMP!");
						}
					}
					else if (it.getGrade() == BHCollection.Atom.GRADE.MONSTER && moveDir != 0 && it.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == moveDir && a.getID() < it.getID())
					{
						this.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Jam! id1=" + a.getID() + ", id2=" + it.getID());
						doStop(it, true);
					}
				}
			}
		}
	}

	public class EngineCallback_depr // implements BHBoard.IClientCallback
	{

		public void onPublish(long timecode)
		{
			// System.out.println("session publish called! tc=" + timecode);
			/*
			 * for (BHCollection.Atom a :
			 * engine.getCollection().allByTimecode(PacmanSession.this.
			 * engineTimecode)) { String cereal; if (a.getGrade() ==
			 * BHCollection.Atom.GRADE.ITEM) { storage.atomToItemCache(a); }
			 * else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER) {
			 * storage.atomToMobileCache(a); } else if (a.getGrade() ==
			 * BHCollection.Atom.GRADE.HERO) { storage.atomToHeroCache(a); }
			 * else { cereal = null; } } PacmanSession.this.engineTimecode =
			 * timecode; publishEvent.trigger(new
			 * BHSession.PublishEventArgs(timecode));
			 */
		}

		public void processAction(BHEngine.Action action)
		{
			/*
			 * // 1) priority actions that work on stopped sessions if
			 * (action.actionType == BHOperations.ACTION_STOPCYCLING) {
			 * engine.stopCycling(); PacmanSession.this.sessionStatus =
			 * PS_STATUS.PAUSE; return; }
			 * 
			 * if (PacmanSession.this.sessionStatus != PS_STATUS.ACTIVE) {
			 * return; } // 2) normal actions that work only on running sessions
			 * BHCollection.Atom me =
			 * engine.getCollection().getItem(action.actorID); if
			 * (action.actionType == PacmanSession.ACTION_DIE) { if (me == null
			 * || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE) {
			 * return; //"No atom id" + session.myID; }
			 * PacmanSession.this.actionDie(action.actorID); } else {
			 * BHOperations.processAction(engine, (BHAction)action); }
			 */
		}

		public boolean processBuff(BHEngine.Buff buff)
		{
			/*
			 * if (buff.action.actionType == PacmanSession.BUFF.RESURRECT) {
			 * return PacmanSession.this.processBuffResurrect(buff); } else if
			 * (buff.action.actionType == PacmanSession.BUFF.PACMONSTER) {
			 * return PacmanSession.this.processBuffPacMonster(buff); } else if
			 * (buff.action.actionType == PacmanSession.BUFF.PACHERO) { return
			 * PacmanSession.this.processBuffPacHero(buff); } else if
			 * (buff.action.actionType == PacmanSession.BUFF.PORT) { return
			 * PacmanSession.this.processBuffPortal(buff); } else { return
			 * BHOperations.processBuff(engine, buff); }
			 */
			return false;
		}

		public void processTriggers()
		{
			/*
			 * for (BHCollection.Atom a : engine.getCollection().all()) { if
			 * (a.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE) continue;
			 * 
			 * if (a.getGrade() == BHCollection.Atom.GRADE.HERO) { // *) eat the
			 * gold here Collection<BHCollection.Atom> spot =
			 * engine.getCollection().atCoords(a, false); if (spot.size() <= 1)
			 * continue; // nothing to see here for (BHCollection.Atom it :
			 * spot) { if (it.getID() == a.getID()) continue; if (it.getStatus()
			 * == BHCollection.Atom.ITEM_STATUS.DELETE) continue;
			 * 
			 * if (it.getType() == PacmanSession.ATOM.GOLD) { int goldCount =
			 * a.getIntProp(PacmanSession.INT_PROPS.HERO_GOLD) + 1;
			 * a.setIntProp(PacmanSession.INT_PROPS.HERO_GOLD, goldCount);
			 * engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
			 * a.getID(), "Yum! Count=" + goldCount);
			 * it.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE); } else if
			 * (it.getType() == PacmanSession.ATOM.PAC) { triggerPacman();
			 * it.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE); } else if
			 * (it.getType() == PacmanSession.ATOM.PORTAL) { triggerPortal(a); }
			 * } } else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER) {
			 * int moveDir = a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
			 * // *) eat the hero here Collection<BHCollection.Atom> spot =
			 * engine.getCollection().atCoords(a, false); if (spot.size() <= 1)
			 * continue; // nothing to see here for (BHCollection.Atom it :
			 * spot) { if (it.getID() == a.getID()) continue; if (it.getGrade()
			 * == BHCollection.Atom.GRADE.HERO) {
			 * 
			 * 
			 * BHEngine.Buff monsterBuff = engine.getBuff(a.getID(),
			 * BUFF.PACMONSTER); if (monsterBuff != null &&
			 * !monsterBuff.isCancelled) { it.setIntProp(INT_PROPS.HERO_GOLD,
			 * it.getIntProp(INT_PROPS.HERO_GOLD) + 100);
			 * engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
			 * it.getID(), "CHOMP!"); actionDie(a.getID()); } else {
			 * actionDie(it.getID()); BHLandscape.Coords c =
			 * startingPoints.get(a.getID()); a.setCoords(c);
			 * engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
			 * a.getID(), "M-M-M-CHOMP!"); } } else if (it.getGrade() ==
			 * BHCollection.Atom.GRADE.MONSTER && moveDir != 0 &&
			 * it.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == moveDir &&
			 * a.getID() < it.getID() ) {
			 * engine.getMessages().addMessage(BHCollection.EntityTypeEnum.
			 * GLOBAL, 0, "Jam! id1=" + a.getID() + ", id2=" + it.getID());
			 * BHOperations.doStop(engine, it, true); } } } }
			 */
		}
	}

	/**
	 * Process a game command as issued by atomID; return "" if successful.
	 * Return an error message if there is a problem.
	 */
	public String command(String cmd, String clientKey, List<String> args)
	{
		if (Utils.IsEmpty(cmd)) return "Invalid command";

		if (COMMAND.MOVE.equals(cmd))
		{
			Integer direction = Utils.tryParseInt(args.get(0));
			BHClient.Command clientCmd = new BHClient.Command(1, 0);
			clientCmd.command = cmd;
			clientCmd.intArgs[0] = direction;

			BHSession.processCommand(clientKey, clientCmd);
			/*
			 * Integer actionID = commandMove(agent.atomID, direction); if
			 * (actionID == null) { return "move to dir " + direction +
			 * " failed"; }
			 */
		}
		else if (COMMAND.PACMAN.equals(cmd))
		{
			BHClient.Command clientCmd = new BHClient.Command(0, 0);
			clientCmd.command = cmd;
			BHSession.processCommand(clientKey, clientCmd);
			/*
			 * triggerPacman();
			 */
		}
		else if (COMMAND.DIE.equals(cmd))
		{
			BHClient.Command clientCmd = new BHClient.Command(0, 0);
			clientCmd.command = cmd;
			BHSession.processCommand(clientKey, clientCmd);
			/*
			 * actionDie(atomID);
			 */
		}
		else if (COMMAND.REFRESH.equals(cmd))
		{
			// agent.timecode = 0;
			// refresh doesn't do anything on the session, it's a client thing
		}
		return "";
	}

	@Override
	public BHClient.IElement processCommand(BHClientRegistration agent, BHClient.Command cmd)
	{
		if (COMMAND.MOVE.equals(cmd.command))
		{
			if (PacmanSession.this.sessionStatus != PS_STATUS.ACTIVE)
			{
				return new BHClient.Error(agent.timecode, "The session is not started: " + PacmanSession.this.sessionStatus);
			}

			Integer direction = cmd.intArgs[0]; // Utils.tryParseInt(cmd.intArgs[0]);
			Integer actionID = commandMove(agent.atomID, direction);
			if (actionID == null)
			{
				return new BHClient.Error(agent.timecode, "move to dir " + direction + " failed");
			}
		}
		else if (COMMAND.START.equals(cmd.command))
		{
			return PacmanSession.this.commandStart(cmd);
		}

		else if (COMMAND.PACMAN.equals(cmd.command))
		{
			triggerPacman();
		}
		else if (COMMAND.DIE.equals(cmd.command))
		{
			actionDie(agent.atomID);
		}
		else if (COMMAND.REFRESH.equals(cmd.command))
		{
			agent.timecode = 0;
			// refresh doesn't do anything on the session, it's a client thing
		}
		else if (COMMAND.ROBOT.equals(cmd.command)) // [], [robotName,
													// sessionKey]
		{
			// have to start a robot here
			return commandRobot(agent, cmd);
		}
		else if (COMMAND.PAUSE.equals(cmd.command)) // [], [robotName,
													// sessionKey]
		{
			// have to start a robot here
			return commandPause(agent, cmd);
		}
		else
		{
			return super.processCommand(agent, cmd);
		}
		return cmd;
	}

	/**
	 * check if the direction is open and move there; if the direction is not
	 * open, but the mobile is moving - post the movement buff; else return
	 * null.
	 */
	public Integer commandMove(int mobileID, int direction)
	{
		PacmanSession s = this;

		BHCollection.Atom me = s.getCollection().getItem(mobileID);
		if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
		{
			return null; // "No atom id" + session.myID;
		}
		if (direction < 0 || direction >= BHLandscape.cellShifts.length)
		{
			return null; // return "Invalid direction: " + direction;
		}
		
		if (s.sessionStatus != PS_STATUS.ACTIVE)
		{
			s.engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, me.getID(), "Moving while " + PS_STATUS.toString(s.sessionStatus) + " - blocked");
			return null;
		}
		boolean hasMove = (me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) > 0 && me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC) > 0);
		// boolean hasMove = false; // this test did not work: auto-continue in
		// BHOperations.actionJump keeps posting
		// new moves, so there is always a move in progress

		if (me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == direction)
		{
			System.out.println("move to " + direction + " failed: already moving that way");
			return null; // already moving that way
		}
		BHLandscape.Cell tCell = s.getLandscape().getNextCell(me, direction);
		if (!hasMove && tCell.getTerrain() == BHLandscape.TerrainEnum.LAND)
		{
			// monsters won't move into each other
			boolean canMove = true;
			if (me.getGrade() == BHCollection.Atom.GRADE.MONSTER)
			{
				for (BHCollection.Atom a : s.getCollection().atCoords(tCell, false))
				{
					if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER && (a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == 0 || a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == BHLandscape.cellShifts[direction][3]))
					{
						canMove = false;
						break;
					}
				}
			}
			if (canMove)
			{
				BHEngine.Action action = new BHEngine.Action();
				action.actionType = ACTION.JUMP;
				action.actorID = me.getID();
				action.intProps = Utils.intArray(s.timecode, direction, 1, MOVE_SPEED);

				// action.actionType = BHOperations.ACTION_MOVE;
				// s.engine.postAction(action, 0);
				me.setCoords(me);
				me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
				me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);

				s.engine.postAction(action, MOVE_SPEED);
				return action.ID; // "action ID=" + action.ID;
			}
			else
			{
				System.out.println("move to " + direction + " failed: monster collision");
			}
		}
		else if (me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) != 0 && me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) != direction)
		{
			// s.engine.g
			// the way buff_move works:
			BHEngine.Buff moveBuff = new BHEngine.Buff();
			BHEngine.Action moveAction = new BHEngine.Action(BHBoard.ACTION.MOVE, me.getID(), 0, 0);
			// moveBuff.actionType = BHOperations.BUFF_MOVE;
			// moveBuff.actorID = me.getID();
			// moveBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
			moveAction.intProps = Utils.intArray(s.engine.timecode, direction, 1, MOVE_SPEED);
			moveBuff.action = moveAction;

			BHEngine.Buff oldBuff = null;
			for (BHEngine.Buff b : s.engine.buffs)
			{
				if (!b.isCancelled && b.action.actorID == moveBuff.action.actorID && b.action.actionType == moveBuff.action.actionType
				// && b.intProps[1] == direction
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

		/*
		 * BHOperations.BHAction action = new BHOperations.BHAction();
		 * action.actionType = BHOperations.ACTION_MOVE; action.actorID =
		 * agent.controlledMobileID; action.intProps =
		 * Utils.intArray(s.engine.timecode, direction, 1,
		 * BHOperations.MOVE_SPEED);
		 * 
		 * 
		 * s.engine.postAction(action, 0); return action.ID; //"action ID=" +
		 * action.ID;
		 */
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public BHClient.IElement commandStage(BHClient.Command cmd)
	{
		if (cmd.stringArgs.length == 0 || !this.getSessionKey().equals(cmd.stringArgs[0]))
		{
			return new BHClient.Error(0, "Invalid session key");
		}

		if (this.pacmanStage <= 0)
		{
			loadStage(1);
		}
		else
		{
			loadStage(this.pacmanStage + 1);
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Stage set to " + this.pacmanStage);
		}

		return cmd;
	}

	public BHClient.IElement commandRobot(BHClientRegistration agent, BHClient.Command cmd)
	{
		try
		{
			// we only need the BotHallServer to get the port number.
			// in a "real" client the port woult be configured somehow (and the
			// server name, too)
			StreamHost bhs = StreamHost.getListener(); // this will start the
														// server if needed

			java.net.Socket s = new java.net.Socket();
			s.setSoTimeout(1000);
			s.connect(new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), bhs.getPort()));

			StreamClient<PacmanClient> client = new StreamClient<PacmanClient>(s, new PacmanClient());
			/*
			 * Shambler shambler = new Shambler(); shambler.clientKey =
			 * agent.clientKey; shambler.setClient(new StreamClient(s));
			 */
			net.vbelev.bothall.client.PacmanShambler shambler = new net.vbelev.bothall.client.PacmanShambler();
			shambler.clientKey = agent.clientKey;
			shambler.setClient(client);
			// shambler.client

			BHClient.Command joinCmd = new BHClient.Command("JOINCLIENT", null, new String[]{agent.clientKey});
			client.writeCommand(joinCmd);
			// while (!shambler.client.readUp()) ;
			// BHSession sess = BHSession.getSession(agent.sessionID);
			// sess.publishEvent.subscribe();
		}
		catch (Exception x)
		{
			return new BHClient.Error(17, x.getClass().getName() + ": " + x.getMessage());
		}
		return cmd; // new BHClient.Error(0, "Not implemented robot yet");
	}

	public BHClient.IElement commandStart(BHClient.Command cmd)
	{
		if (cmd.stringArgs.length == 0 || !this.getSessionKey().equals(cmd.stringArgs[0]))
		{
			return new BHClient.Error(0, "Invalid session key");
		}
		if (this.sessionStatus == PS_STATUS.ACTIVE) // it is already running
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Pacman session is already running");
			return new BHClient.Error(0, "Pacman session is already running");
		}
		/*
		 * // check that there are gold items left... who cares boolean hasItems
		 * = false; for (BHCollection.Atom a :
		 * this.engine.getCollection().all()) { if (a.getGrade() ==
		 * BHCollection.Atom.GRADE.ITEM && a.getType() == ATOM.GOLD &&
		 * a.getStatus() == BHCollection.Atom.ITEM_STATUS.OK) { hasItems = true;
		 * break; } }
		 */
		if (this.pacmanStage == 0)
		{
			loadStage(1);
		}

		// check if there are any unattached mobiles
		List<BHClientRegistration> allAgents = BHClientRegistration.agentList(this.getID());
		for (BHCollection.Atom a : this.engine.getCollection().all())
		{
			if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER)
			{
				boolean hasAgent = false;
				for (BHClientRegistration r : allAgents)
				{
					if (r.atomID == a.getID())
					{
						hasAgent = true;
						break;
					}
				}
				if (!hasAgent)
				{
					BHClientRegistration r = BHClientRegistration.createAgent();
					r.sessionID = this.getID();
					r.subscriptionID = this.getEngine().getMessages().addSubscription();
					r.atomID = a.getID();
					BHClient.Command robotCmd = new BHClient.Command(0, 0);
					robotCmd.command = COMMAND.ROBOT;
					this.commandRobot(r, robotCmd);
					engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Created a robot for mobileID: " + a.getID());
				}
			}
		}

		this.sessionStatus = PS_STATUS.ACTIVE;
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Pacman started!");
		return cmd;
	}

	/**
	 * If the session is active - pause it indefinitely, possibly with a
	 * message; if the session is paused - remove the pause (only is it was
	 * paused by the same agent); for other session statuses, do nothing.
	 */
	public BHClient.IElement commandPause(BHClientRegistration agent, BHClient.Command cmd)
	{
		BHEngine.Buff oldBuff = null;
		for (BHEngine.Buff b : engine.buffs)
		{
			if (!b.isCancelled && b.action.actionType == PacmanSession.BUFF.PAUSE
			// && b.intProps[1] == direction
			)
			{
				oldBuff = b;
				break;
			}
		}

		if (this.sessionStatus == PS_STATUS.PAUSE)
		{
			this.sessionStatus = PS_STATUS.ACTIVE;
			if (oldBuff != null)
			{
				oldBuff.isCancelled = true;
			}
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Pause removed by " + agent.userName);
		}
		else if (this.sessionStatus == PS_STATUS.ACTIVE)
		{
			this.sessionStatus = PS_STATUS.PAUSE;

			if (oldBuff != null)
			{ // it should not happen, but just in case
				oldBuff.isCancelled = true;
				oldBuff = null;
			}
			// return new BHClient.Error(this.engineTimecode, "Invalid session
			// key");
			BHEngine.Buff pauseBuff = new BHEngine.Buff();
			//BHEngine.Action pauseAction = new BHEngine.Action(PacmanSession.ACTION.PAUSE, agent.atomID, 0, 0);
			PauseAction pauseAction = new PauseAction();
			pauseAction.actionType = PacmanSession.ACTION.PAUSE;
			pauseAction.actorID = agent.atomID;
			// moveAction.intProps = Utils.intArray(s.engine.timecode,
			// direction, 1, MOVE_SPEED);
			pauseBuff.action = pauseAction;
			pauseBuff.ticks = 0; // indefinite
			engine.postBuff(pauseBuff);
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "Paused by " + agent.userName);
			System.out.println("pause started, action=" + pauseAction.toString());

			return cmd; // it's not null, but we do not know the action id yet

		}
		else
		{
			return new BHClient.Error(this.engineTimecode, "Invalid session status: " + this.sessionStatus);
		}

		return cmd;
	}

	/**
	 * Start the pacman mode
	 */
	public void triggerPacman()
	{
		for (BHCollection.Atom a : engine.getCollection().all())
		{
			if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER)
			{
				BHEngine.Buff b = engine.getBuff(a.getID(), BUFF.PACMONSTER);
				if (b == null)
				{
					b = new BHEngine.Buff();
					b.action = new BHEngine.Action(PacmanSession.BUFF.PACMONSTER, a.getID(), 0, 0);
					// b.actionType = PacmanSession.BUFF.PACMONSTER;
					b.isVisible = true;
					// b.actorID = a.getID();
					// b.actorType = BHCollection.EntityTypeEnum.ITEM;
					b.ticks = PACMAN_DURATION;
					engine.postBuff(b);
				}
				else
				{
					b.ticks = PACMAN_DURATION;
				}
			}
			else if (a.getGrade() == BHCollection.Atom.GRADE.HERO)
			{
				BHEngine.Buff b = engine.getBuff(a.getID(), BUFF.PACHERO);
				if (b == null)
				{
					b = new BHEngine.Buff();
					b.action = new BHEngine.Action(PacmanSession.BUFF.PACHERO, a.getID(), 0, 0);
					// b.actionType = PacmanSession.BUFF.PACHERO;
					b.isVisible = true;
					// b.actorID = a.getID();
					// b.actorType = BHCollection.EntityTypeEnum.ITEM;
					b.ticks = PACMAN_DURATION;
					engine.postBuff(b);
				}
				else
				{
					b.ticks = PACMAN_DURATION;
				}
			}
		}
	}

	public void triggerPortal(BHCollection.Atom me)
	{
		// 0) check for a pre-existing buff
		BHEngine.Buff oldBuff = engine.getBuff(me.getID(), PacmanSession.BUFF.PORT);
		if (oldBuff != null && !oldBuff.isCancelled)
		{
			return;
		}
		int direction = me.getIntProp(PacmanSession.INT_PROPS.MOVE_DIR);

		// 1) find the other portal and set up the buff
		BHCollection.Atom thePortal = null;
		for (BHCollection.Atom item : engine.getCollection().all())
		{
			if (item.getGrade() == BHCollection.Atom.GRADE.ITEM && item.getType() == ATOM.PORTAL && !BHLandscape.equalCoords(item, me))
			{
				thePortal = item;
				break;
			}
		}

		if (thePortal == null)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, me.getID(), "destination portal not found!");
			return;
		}

		if (direction == 0)
		{
			BHLandscape.Cell[] closest = engine.getLandscape().closestCells(thePortal.getX(), thePortal.getY(), thePortal.getZ());
			for (int d = 1; d < closest.length; d++)
			{
				BHLandscape.Cell c = closest[d];
				if (c.getTerrain() == BHLandscape.TerrainEnum.LAND)
				{
					direction = d;
					break;
				}
			}
		}
		if (direction == 0)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, me.getID(), "destination portal is broken!");
			return;
		}

		BHEngine.Buff portBuff = new BHEngine.Buff();
		BHEngine.Action portAction = new BHEngine.Action(PacmanSession.BUFF.PORT, me.getID(), 0, 0);
		// portBuff.actionType = PacmanSession.BUFF.PORT;
		// portBuff.actorID = me.getID();
		// portBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
		portAction.intProps = Utils.intArray(thePortal.getID(), engine.timecode, direction, 0);
		portBuff.action = portAction;
		portBuff.isVisible = true;
		portBuff.ticks = MOVE_SPEED - 1;
		engine.postBuff(portBuff);

		// 2) add fake move props to imitate movement
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);
	}

	public int actionDie(int mobileID)
	{
		BHCollection.Atom me = engine.getCollection().getItem(mobileID);
		if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
		{
			return 0; // "No atom id" + session.myID;
		}
		me.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
		doStop(me, true);
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, mobileID, "You got died!");

		for (BHEngine.Buff b : engine.getBuffs(mobileID, null))
		{
			b.isCancelled = true;
		}

		BHEngine.Buff dieBuff = new BHEngine.Buff();
		dieBuff.action = new BHEngine.Action(PacmanSession.BUFF.RESURRECT, mobileID, 0, 0);
		// dieBuff.actionType = PacmanSession.BUFF.RESURRECT;
		// dieBuff.actorID = mobileID;
		// dieBuff.actorType = BHCollection.EntityTypeEnum.ITEM;

		dieBuff.ticks = 5;

		this.engine.postBuff(dieBuff);

		return dieBuff.action.ID;
	}
	public boolean processBuffResurrect(BHEngine.Buff buff)
	{
		if (buff.ticks > 0)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, buff.action.actorID, "Dying... " + buff.ticks);
			return true;
		}
		else
		{
			BHCollection.Atom me = engine.getCollection().getItem(buff.action.actorID);
			if (me != null)
			{
				BHLandscape.Coords c = startingPoints.get(me.getID());
				me.setCoords(c);
				me.setStatus(BHCollection.Atom.ITEM_STATUS.OK);
			}
			return false;
		}
	}

	public boolean processBuffPacMonster(BHEngine.Buff buff)
	{
		return true; // let's handle pacman in PacHero
	}

	public boolean processBuffPacHero(BHEngine.Buff buff)
	{
		/*
		 * BHCollection.Atom me = engine.getCollection().getItem(buff.actorID);
		 * if (me == null) return false;
		 * 
		 * for (BHCollection.Atom a : engine.getCollection().atCoords(me)) { if
		 * (a.getID() == me.getID()) continue; if (a.getType() != ATOM.MONSTER)
		 * continue; BHBuff monsterBuff =
		 * engine.getBuff(BHCollection.EntityTypeEnum.ITEM, me.getID(),
		 * BUFF_PACMONSTER); if (monsterBuff == null || monsterBuff.isCancelled)
		 * continue;
		 * 
		 * me.setIntProp(INT_PROPS.HERO_GOLD, me.getIntProp(INT_PROPS.HERO_GOLD)
		 * + 100);
		 * engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
		 * buff.actorID, "CHOMP!"); processActionDie(a.getID()); }
		 */
		return true;
	}

	/** props: [target portal item ID, moveTC, post-shift direction, phase] */
	public boolean processBuffPortal(BHEngine.Buff buff)
	{
		BHCollection.Atom me = engine.getCollection().getItem(buff.action.actorID);
		if (me == null) return false;

		int direction = buff.action.intProps[2];
		int phase = buff.action.intProps[3];

		if (phase == 0 && buff.action.intProps[1] != me.getIntProp(INT_PROPS.MOVE_TC)
		// && direction != me.getIntProp(INT_PROPS.MOVE_DIR)
		)
		{
			return false; // another move happened
		}

		if (buff.ticks > 0)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, buff.action.actorID, "Beaming! " + buff.ticks);
			return true;
		}
		BHCollection.Atom dest = engine.getCollection().getItem(buff.action.intProps[0]);

		if (phase == 0)
		{
			// finally, do the port
			if (dest == null || dest.getType() != ATOM.PORTAL)
			{
				engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, buff.action.actorID, "Beaming failed! ");
				return false;
			}
			me.setCoords(dest);
			buff.action.intProps[3] = 1;

			// doMove(me.getID(), buff.intProps[2]);
			BHEngine.Action action = new BHEngine.Action(BHBoard.ACTION.JUMP, me.getID(), 0, 0);
			// action.actionType = BHOperations.ACTION_JUMP;
			// action.actorID = me.getID();

			action.intProps = Utils.intArray(engine.timecode, direction, 1, MOVE_SPEED);

			engine.postAction(action, MOVE_SPEED);
			me.setIntProp(INT_PROPS.MOVE_TC, engine.timecode);
			me.setIntProp(INT_PROPS.MOVE_DIR, direction);

			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM, buff.action.actorID, "Beamed! " + me.toString());
		}

		if (phase == 1 && !BHLandscape.equalCoords(dest, me)) // check if it's
																// time to
																// remove the
																// buff
		{
			return false;
		}

		return true;
	}

	public static class PauseAction extends Action
	{
		public final Hashtable<Integer, Integer> buffTicks = new Hashtable<Integer, Integer>();
		
		@Override
		public String toString()
		{
			return "[BHA " + this.ID + ":" + this.actionType + " - PauseAction]";
		}
	}
	/**
	 * The Pause buff does not expire and has no properties. It stays on until
	 * somebody cancels it from outside or until the session changes status.
	 */
	public boolean buffPause(BHEngine.Buff buff)
	{
		PacmanSession engine = this;
		if (engine.sessionStatus != PS_STATUS.PAUSE)
		{
			System.out.println("buffPause, status changed to " + engine.sessionStatus + " - return false");
			return false;
		}
		if (!(buff.action instanceof PauseAction))
		{
			System.out.println("buffPause, force new PauseAction");
			buff.action = new PauseAction();
			buff.action.actionType = ACTION.PAUSE;
		}

		PauseAction act = (PauseAction) buff.action;

		synchronized (engine.buffs)
		{
			for (Buff b : engine.buffs)
			{
				if (b.action == null) continue;
				if (b.action.actionType == ACTION.PAUSE)
				{
					//System.out.println("buffPause - skip self, action ID=" + b.action.ID);
					continue;
				}
				Integer bTicks = act.buffTicks.get(b.action.ID);
				if (bTicks == null)
				{
					act.buffTicks.put(b.action.ID, b.ticks);
				}
				else
				{
					b.ticks = bTicks;
				}
				System.out.println("buffPause, bTicks=" + bTicks + ", buff ticks=" + b.ticks + ", updated " + b.action.toString());
			}
		}
		// don't touch timers - the move action will handle the pause correctly
		return true;
	}

}
