package net.vbelev.bothall.web;

import java.util.*;
import java.io.*;

import net.vbelev.bothall.client.BHClient;
import net.vbelev.bothall.client.StreamClient;
import net.vbelev.bothall.core.*;
import net.vbelev.bothall.core.BHCollection.EntityTypeEnum;
import net.vbelev.bothall.core.BHOperations.BHAction;
import net.vbelev.bothall.core.BHOperations.BHBuff;
import net.vbelev.utils.*;
/**
 * Here we put the methods specific to pacman
 * @author Vythe
 *
 */
public class PacmanSession extends BHSession
{
	
	public static class INT_PROPS //extends BHCollection.Atom.INT_PROPS
	{
		private static final int COUNT = 8;
		public static final int STATUS = 0;
		public static final int X = 1;
		public static final int Y = 2;
		public static final int Z = 3;
		//public static final int DX = 3;
		//public static final int DY = 4;
		//public static final int DZ = 5;
		/** base timecode for movements */
		public static final int MOVE_TC = 4;
		public static final int MOVE_DIR = 5;
		public static final int HERO_GOLD = 6;
		public static final int HERO_LIVES = 7;
		
		private INT_PROPS() {}
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
		
		private ATOM() {}
	}
	
	public static class COMMAND
	{
		public static final String REFRESH = "refresh".intern();
		public static final String MOVE = "move".intern(); //inrArgs: [direction]
		public static final String PACMAN = "pacman".intern();
		public static final String DIE = "die".intern();
		public static final String ROBOT = "robot".intern(); // stringArgs: [clientKey, robotType], where clientKey may be null
		
		private COMMAND() {}
	}
	
	public static class BUFF
	{
		public static final String RESURRECT = "RESURRECT".intern();
		public static final String PACMONSTER = "PACMONSTER".intern();
		public static final String PACHERO = "PACHERO".intern();
		/**
		 * Port properties [target portal item ID, moveTC, post-shift direction, phase].
		 * When entering the portal cell, set the buff with phase=0 (leaving);
		 * after porting, set phase=1. The buff with phase==1 stays on until you leave the cell
		 * and prevents further porting. 
		 */
		public static final String PORT = "PORT".intern();
	}
	
	public static final String ACTION_DIE = "DIE".intern();

	public static final int PACMAN_DURATION = 1000;
	
	//public static final Object lock = new Object();
	//private static int sessionInstanceSeq = 0;
	//private static final ArrayList<PacmanSession> sessionList = new ArrayList<PacmanSession>();
	
	//private int sessionID = 0;
	//private String sessionKey = "";
	//private BHEngine engine;

	//public int engineTimecode = 0;
	//public boolean isProtected = false;
	//public final Date createdDate = new Date();
	//public final BHStorage storage = new BHStorage();
	//public final EventBox.Event<BHSession.PublishEventArgs> publishEvent = new EventBox.Event<BHSession.PublishEventArgs>();
	public final Map<Integer, BHLandscape.Coords> startingPoints = new java.util.Hashtable<Integer, BHLandscape.Coords>();
	
	public static PacmanSession createSession()
	{
		BHEngine e = PacmanSession.loadFile("/../data/pacman.txt");
		e.CYCLE_MSEC = 200;
		PacmanSession s =  PacmanSession.createSession(e);
		
		return s;
	}

	/*
	public static int getSessionCount()
	{
		return sessionList.size();
	}
	*/
	public static PacmanSession createSession(BHEngine e)
	{
		PacmanSession s = new PacmanSession();
		s.engine = e;
		e.clientCallback = s.new EngineCallback();
		
/* session will be registed in the super constructor BHSession()
 * 		s.sessionID = ++sessionInstanceSeq;
		synchronized(lock)
		{
			s.sessionKey = PacmanSession.generateSessionKey(8);
			sessionList.add(s);
		}
*/		
		e.publish();
		
		for (BHCollection.Atom a : e.getCollection().all())
		{
			if (a.getGrade() != BHCollection.Atom.GRADE.ITEM)
			{
				s.startingPoints.put(a.getID(), BHLandscape.coordsPoint(a));
			}
		}
		
		return s;
	}	

	public static PacmanSession getSession(Integer id)
	{
		BHSession res = BHSession.getSession(id);
		if (res instanceof PacmanSession)
		{
			return (PacmanSession)res;
		}
		/*
		if (id == null || id <= 0) return null;
		synchronized(lock)
		{
			for (PacmanSession s : sessionList)
			{
				if (s.sessionID == id) return s;
			}
		}
		*/
		return null;
	}	
	
	public static PacmanSession getSession(String key)
	{
		BHSession res = BHSession.getSession(key);
		if (res instanceof PacmanSession)
		{
			return (PacmanSession)res;
		}
		/*
		if (Utils.IsEmpty(key)) return null;
		synchronized(lock)
		{
			for (PacmanSession s : sessionList)
			{
				if (key.equals(s.sessionKey)) return s;
			}
		}
		*/
		return null;
		
	}
	/*
	public static boolean destroySession(PacmanSession s)
	{
		synchronized(lock)
		{
			if (!sessionList.remove(s))
				return false;			
		}
		s.getEngine().stopCycling();
		s.getEngine().getMessages().clear();
		return true;
		
	}
	public static String generateSessionKey(int length) 
	{
		int cnt = 0;
		if (length <= 0) return "";
		
		do
		{
			String res = Utils.randomString(length);			
			PacmanSession s = getSession(res);
			if (s == null)
			{
				return res;
			}
		} while (cnt < 10);
		throw new Error("Failed to create a unique password of length " + length);
	}
	*/
	public static List<PacmanSession> sessionList()
	{
		/*
		ArrayList<PacmanSession> res = new ArrayList<PacmanSession>();
		
		synchronized(sessionList)
		{
			res.addAll(sessionList);
			//for (PacmanSession s : sessionList)
			//{
			//	res.add(s);
			//}
		}
		*/
		List<BHSession> r1 = BHSession.getSessionList();
		ArrayList<PacmanSession> res = new ArrayList<PacmanSession>();
		
		for (BHSession s : r1)
		{
			if (s instanceof PacmanSession)
				res.add((PacmanSession)s);
		}
		return res;
	}
	
	/*
	public BHClientAgent createAgent()
	{
		BHClientAgent agent = BHClientAgent.createAgent();
		agent.sessionID = this.getID();
		agent.subscriptionID = this.getEngine().getMessages().addSubscription();
		
		return agent;
	}

	public void detachAgent(BHClientAgent agent)
	{
		if (agent == null  || agent.getID() == 0) return;
		// we need to check the session and possibly stop it...
		
		PacmanSession s = this; //PacmanSession.getSession(agent.sessionID);	
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
	*/
	
	/*
	 public int getID() { return sessionID; }	 
	
	public String getSessionKey() { return sessionKey; }
	
	public BHEngine getEngine() { return engine; }
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
	
	public static BHEngine loadFile(String fileName)
	{
		BHEngine res = new BHEngine();
		
		try
		{
		InputStream is = BHEngine.class.getResourceAsStream(fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		int x = -1;
		int y = -1;
		int z = 0;
		//BHCollection.Atom hero = null;
		BHLandscape landscape = res.getLandscape();
		BHCollection collection = res.getCollection();
		
		while ((line = br.readLine()) != null)
		{
			y++;
			StringReader sr = new StringReader(line);
			int c;
			x = -1;
			while ((c = sr.read())!= -1)
			{
				x++;
				BHLandscape.TerrainEnum terrain;
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
					BHCollection.Atom hero = addAtom(collection, ATOM.MONSTER, BHCollection.Atom.GRADE.MONSTER);
					hero.setX(x);
					hero.setY(y);
					hero.setZ(z);
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
					//res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.VOID));
					System.out.println("Unsupported mark [" + (char)c + "] when reading " + fileName);
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
		return res;
	}
	
	
	
	public class EngineCallback implements BHEngine.IClientCallback
	{

		public void onPublish(int timecode)
		{
			//System.out.println("session publish called! tc=" + timecode);
	
			for (BHCollection.Atom a : engine.getCollection().allByTimecode(PacmanSession.this.engineTimecode))
			{
				String cereal;
				if (a.getGrade() == BHCollection.Atom.GRADE.ITEM)
				{
					storage.atomToItemCache(a);
				}
				else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER)
				{
					storage.atomToMobileCache(a);
				}
				else if (a.getGrade() == BHCollection.Atom.GRADE.HERO)
				{
					storage.atomToHeroCache(a);
				}
				else
				{
					cereal = null;
				}
			}
			PacmanSession.this.engineTimecode = timecode;
			publishEvent.trigger(new BHSession.PublishEventArgs(timecode));
		}
		
		public void processAction(BHAction action)
		{
			BHCollection.Atom me = engine.getCollection().getItem(action.actorID);
			if (action.actionType == PacmanSession.ACTION_DIE)
			{
				if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
				{
					return; //"No atom id" + session.myID;
				}
				PacmanSession.this.actionDie(action.actorID);
			}
			else
			{
				BHOperations.processAction(engine, action);
			}
			
		}

		public boolean processBuff(BHBuff buff)
		{
			if (buff.actionType == PacmanSession.BUFF.RESURRECT)
			{
				return PacmanSession.this.processBuffResurrect(buff);
			}
			else if (buff.actionType == PacmanSession.BUFF.PACMONSTER)
			{
				return PacmanSession.this.processBuffPacMonster(buff);
			}
			else if (buff.actionType == PacmanSession.BUFF.PACHERO)
			{
				return PacmanSession.this.processBuffPacHero(buff);
			}
			else if (buff.actionType == PacmanSession.BUFF.PORT)
			{
				return PacmanSession.this.processBuffPortal(buff);
			}
			else
			{
				return BHOperations.processBuff(engine, buff);
			}
		}
		
		public void processTriggers()
		{
			//BHOperations.processTriggers(engine);
			/*
			// *) eat the gold here
			for (BHCollection.Atom hero : (Iterable<BHCollection.Atom>)engine.getCollection().all().stream().filter(q -> q.getType() == "HERO")::iterator)
			{
				Collection<BHCollection.Atom> spot = engine.getCollection().atCoords(hero);
				if (spot.size() <= 1) continue;  // nothing to see here
				for (BHCollection.Atom it : spot)
				{
					if (it.getID() == hero.getID()) continue;
					if (it.getType() == "GOLD" && it.getStatus() != BHCollection.Atom.ITEM_STATUS.DELETE)
					{
						int goldCount = hero.getIntProp(BHCollection.Atom.INT_PROPS.HERO_GOLD) + 1;
						hero.setIntProp(BHCollection.Atom.INT_PROPS.HERO_GOLD, goldCount);
						engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  hero.getID(), "Yum! Count=" + goldCount);
						it.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
					}
				}
				
			}			
			*/
			for (BHCollection.Atom a : engine.getCollection().all())
			{
				if (a.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE) continue;
				
				if (a.getGrade() == BHCollection.Atom.GRADE.HERO)
				{
					// *) eat the gold here
					Collection<BHCollection.Atom> spot = engine.getCollection().atCoords(a, false);
					if (spot.size() <= 1) continue;  // nothing to see here
					for (BHCollection.Atom it : spot)
					{
						if (it.getID() == a.getID()) continue;
						if (it.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE) continue;
						
						if (it.getType() == PacmanSession.ATOM.GOLD)
						{
							int goldCount = a.getIntProp(PacmanSession.INT_PROPS.HERO_GOLD) + 1;
							a.setIntProp(PacmanSession.INT_PROPS.HERO_GOLD, goldCount);
							engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  a.getID(), "Yum! Count=" + goldCount);
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
					Collection<BHCollection.Atom> spot = engine.getCollection().atCoords(a, false);
					if (spot.size() <= 1) continue;  // nothing to see here
					for (BHCollection.Atom it : spot)
					{
						if (it.getID() == a.getID()) continue;
						if (it.getGrade() == BHCollection.Atom.GRADE.HERO)
						{
							/*
							BHOperations.BHAction action = new BHOperations.BHAction();
							action.actionType = PacmanSession.ACTION_DIE;
							action.actorID = it.getID();
							//action.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);

							
							PacmanSession.this.getEngine().postAction(action, 0);
							//return action.ID; //"action ID=" + action.ID;
							*/
							
							BHBuff monsterBuff = engine.getBuff(BHCollection.EntityTypeEnum.ITEM, a.getID(), BUFF.PACMONSTER);
							if (monsterBuff != null && !monsterBuff.isCancelled)
							{
								it.setIntProp(INT_PROPS.HERO_GOLD, it.getIntProp(INT_PROPS.HERO_GOLD) + 100);
								engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  it.getID(), "CHOMP!");
								actionDie(a.getID());
							}
							else
							{
								actionDie(it.getID());
								BHLandscape.Coords c = startingPoints.get(a.getID());
								a.setCoords(c);
								engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  a.getID(), "M-M-M-CHOMP!");
							}
						}
						else if (it.getGrade() == BHCollection.Atom.GRADE.MONSTER
								&& moveDir != 0
								&& it.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == moveDir
								&& a.getID() < it.getID()
								)
						{
							engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL,  0, "Jam! id1=" + a.getID() + ", id2=" + it.getID());
							BHOperations.doStop(engine, it, true);
						}
					}
				}
			}
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
			Integer actionID = commandMove(agent.atomID, direction);
			if (actionID == null)
			{
				return "move to dir " + direction + " failed";
			}
			*/
		}
		else if (COMMAND.PACMAN.equals(cmd))
		{
			BHClient.Command clientCmd = new BHClient.Command(0, 0);
			clientCmd.command = cmd;
			BHSession.processCommand(clientKey, clientCmd);
			/*
			triggerPacman();
			*/
		}
		else if (COMMAND.DIE.equals(cmd))
		{
			BHClient.Command clientCmd = new BHClient.Command(0, 0);
			clientCmd.command = cmd;
			BHSession.processCommand(clientKey, clientCmd);
			/*
			actionDie(atomID);
			*/
		}
		else if (COMMAND.REFRESH.equals(cmd))
		{
			//agent.timecode = 0;
			// refresh doesn't do anything on the session, it's a client thing
		}
		return "";
	}
	
	@Override
	public BHClient.Element processCommand(BHClientAgent agent, BHClient.Command cmd)
	{
		if (COMMAND.MOVE.equals(cmd.command)) 
		{
			Integer direction = cmd.intArgs[0]; //Utils.tryParseInt(cmd.intArgs[0]);			
			Integer actionID = commandMove(agent.atomID, direction);
			if (actionID == null)
			{
				return new BHClient.Error(agent.timecode, "move to dir " + direction + " failed");
			}
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
		else if (COMMAND.ROBOT.equals(cmd.command)) // [], [robotName, sessionKey]
		{
			// have to start a robot here
			return commandRobot(agent, cmd);
		}
		return cmd;
	}
	
	/** 
	 * check if the direction is open and move there;
	 * if the direction is not open, but the mobile is moving - post the movement buff;
	 * else return null. 
	 */
	public Integer commandMove(int mobileID, int direction)
	{
		PacmanSession s = this;
		
		BHCollection.Atom me = s.engine.getCollection().getItem(mobileID);
		if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
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
				action.actorID = me.getID();
				action.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
				
				//action.actionType = BHOperations.ACTION_MOVE;
				//s.engine.postAction(action, 0);
				me.setCoords(me);
				me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
				me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);
				action.actionType = BHOperations.ACTION_JUMP;
				s.engine.postAction(action, BHOperations.MOVE_SPEED);
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
	
	public static class Shambler
	{
		private StreamClient client;
		public String clientKey;
		
		public void setClient(StreamClient c)
		{
			if (client != null)
			{
				client.setOnUpdate(null);
			}
			client = c;
			if (c != null)
			{
				client.setOnUpdate(new StreamClient.OnUpdate()
				{
					
					@Override
					public void onUpdate()
					{
						shamble();
					}
				});
				c.start();
			}
			/*
			client.updateEvent.subscribe(new EventBox.EventHandler<EventBox.EventArgs>()
			{

				@Override
				public boolean isListening()
				{
					// TODO Auto-generated method stub
					return true;
				}

				@Override
				public void invoke(EventBox.EventArgs e)
				{
					// TODO Auto-generated method stub
					shamble();
				}
			});
			*/
		}

		private static int[] pacmanValidDirs = new int[]{1,2,3,6};

		public static boolean mayTurnMonster(BHClient.Cell[] closest, int currentDir) 
		{

			if (currentDir == 0) return true;
			//if (!closest[currentDir]) {
			//	console.log("INVALID currentDir: " + currentDir);
			//	return true;
			//}
			
			if (closest[currentDir].terrain != "LAND") return true;
			
			int cnt = 0;
			for (int k = 0; k < pacmanValidDirs.length && cnt < 3; k++) {
				if (closest[pacmanValidDirs[k]].terrain == "LAND") cnt++;		
			}
			return cnt != 2;		 
		}
		
		/**
		 * Choose a valid direction at random
		 */
		public void shamble()
		{
			
			try
			{
				int myMobileID = this.client.collection.status.controlledMobileID;
				System.out.println("I am shambling with the key " + this.clientKey + ", controlled ID=" + myMobileID);
				BHClient.Mobile me = null;
				for (BHClient.Mobile m : this.client.collection.mobiles.values())
				{
					if  (m.id == myMobileID) me = m;
					System.out.println("mobile " + m.toString());
				}
				if (me == null)
				{
					return;
				}
				BHClient.Cell[] closest = this.client.collection.closestCells(me.x, me.y, me.z, BHLandscape.TerrainEnum.VOID.name());
				BHClient.Mobile[] mobs = this.client.collection.getMobiles(me.x, me.y, me.z);
				
				if (!mayTurnMonster(closest, me.dir))
				{
					return;
				}
				// test all valid dirs and select one at random
				int cnt = 0;
				int[] goodDirs = new int[pacmanValidDirs.length];
				for (int k = 0; k < pacmanValidDirs.length; k++) 
				{
					BHClient.Cell c = closest[pacmanValidDirs[k]];
					boolean goodDir = true;
					if (c.terrain != "LAND") goodDir = false;
					for (BHClient.Mobile m : this.client.collection.mobiles.values())
					{
						if (m.x == c.x && m.y == c.y && m.z == c.z
								&& (m.dir != 0 && m.dir != BHClient.cellShifts[k][3])
								)
						{
							goodDir = false;
							break;
						}						
					}
					if (goodDir)
					{
						goodDirs[cnt++] = pacmanValidDirs[k];
					}
				}
				if (cnt == 0) return; // no good options
				int selectedDir = goodDirs[Utils.random.nextInt(cnt)];
				
				this.client.writeCommand("move", new int[] {selectedDir}, null);
				System.out.println("Shambling! pos=" + me.toString() + ", moved to " + selectedDir);
			//this.client.
//			System.out.println("I am shambling! timecode=" + 
//					client.collection.status.timecode
//			);
			}
			catch (Exception x)
			{
				System.out.println("I am shambling, exception=" + Utils.NVL(x.getMessage(), x.getClass().getName()));
			}
		}
		
	}
	
	public BHClient.Element commandRobot(BHClientAgent agent, BHClient.Command cmd)
	{
		try
		{
			// we only need the BotHallServer to get the port number.
			// in a "real" client the port woult be configured somehow (and the server name, too)
			BHListener bhs = BHListener.getSocketServer(); // this will start the server if needed
			
			java.net.Socket s = new java.net.Socket();
			s.setSoTimeout(1000);
			s.connect(new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), bhs.getPort()));
			
			StreamClient client = new StreamClient(s);
			/*
			Shambler shambler = new Shambler();
			shambler.clientKey = agent.clientKey;
			shambler.setClient(new StreamClient(s));
			*/
			net.vbelev.bothall.client.PacmanShambler shambler = new net.vbelev.bothall.client.PacmanShambler();
			shambler.clientKey = agent.clientKey;
			shambler.setClient(client);
			//shambler.client
			
			BHClient.Command joinCmd = new BHClient.Command("JOINCLIENT", null, new String[] {agent.clientKey});
			client.writeCommand(joinCmd);
			//while (!shambler.client.readUp())				;
			//BHSession sess = BHSession.getSession(agent.sessionID);
			//sess.publishEvent.subscribe();
		}
		catch (Exception x)
		{
			return new BHClient.Error(17, x.getClass().getName() + ": " + x.getMessage());
		}
		return cmd; //new BHClient.Error(0, "Not implemented robot yet");
	}
	/** 
	 * Start the pacman mode
	 */
	public void triggerPacman()
	{
		for(BHCollection.Atom a : engine.getCollection().all())
		{
			if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER)			
			{
				BHOperations.BHBuff b = engine.getBuff(EntityTypeEnum.ITEM, a.getID(), BUFF.PACMONSTER);
				if (b == null)
				{
					b = new BHOperations.BHBuff();
					b.actionType = PacmanSession.BUFF.PACMONSTER;
					b.isVisible = true;
					b.actorID = a.getID();
					b.actorType = BHCollection.EntityTypeEnum.ITEM;
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
				BHOperations.BHBuff b = engine.getBuff(EntityTypeEnum.ITEM, a.getID(), BUFF.PACHERO);
				if (b == null)
				{
					b = new BHOperations.BHBuff();
					b.actionType = PacmanSession.BUFF.PACHERO;
					b.isVisible = true;
					b.actorID = a.getID();
					b.actorType = BHCollection.EntityTypeEnum.ITEM;
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
		BHOperations.BHBuff oldBuff = engine.getBuff(EntityTypeEnum.ITEM, me.getID(), PacmanSession.BUFF.PORT);
		if (oldBuff != null && !oldBuff.isCancelled)
		{
			return;
		}				
		int direction = me.getIntProp(PacmanSession.INT_PROPS.MOVE_DIR);
		
		// 1) find the other portal and set up the buff
		BHCollection.Atom thePortal = null;
		for (BHCollection.Atom item : engine.getCollection().all())
		{
			if (item.getGrade() == BHCollection.Atom.GRADE.ITEM
					&& item.getType() == ATOM.PORTAL
					&& !BHLandscape.equalCoords(item, me)
					)
			{
				thePortal = item;
				break;
			}
		}
		
		if (thePortal == null)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "destination portal not found!");
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
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "destination portal is broken!");
			return;
		}
		
		BHOperations.BHBuff portBuff = new BHOperations.BHBuff();
		portBuff.actionType = PacmanSession.BUFF.PORT;
		portBuff.actorID = me.getID();
		portBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
		portBuff.intProps = Utils.intArray(thePortal.getID(), engine.timecode, direction, 0);
		portBuff.isVisible = true;
		portBuff.ticks = BHOperations.MOVE_SPEED - 1;
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
			return 0; //"No atom id" + session.myID;
		}		
		me.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
		BHOperations.doStop(engine, me, true);
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  mobileID, "You got died!");
		
		for (BHOperations.BHBuff b : engine.getBuffs(BHCollection.EntityTypeEnum.ITEM, mobileID, null))
		{
			b.isCancelled = true;
		}
		
		BHOperations.BHBuff dieBuff = new BHOperations.BHBuff();
		dieBuff.actionType = PacmanSession.BUFF.RESURRECT;
		dieBuff.actorID = mobileID;
		dieBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
		//moveBuff.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
		
		dieBuff.ticks = 5;
		
		this.engine.postBuff(dieBuff);
		
		return dieBuff.ID;
	}
	
	public boolean processBuffResurrect(BHBuff buff)
	{
		if (buff.ticks > 0)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  buff.actorID, "Dying... " + buff.ticks);
			return true;
		}
		else
		{
			BHCollection.Atom me = engine.getCollection().getItem(buff.actorID);
			if (me != null)
			{
				BHLandscape.Coords c = startingPoints.get(me.getID());
				me.setCoords(c);
				me.setStatus(BHCollection.Atom.ITEM_STATUS.OK);
			}		
			return false;
		}
	}
		
	public boolean processBuffPacMonster(BHBuff buff)
	{
		return true; // let's handle pacman in PacHero
	}	

	public boolean processBuffPacHero(BHBuff buff)
	{
		/*
		BHCollection.Atom me = engine.getCollection().getItem(buff.actorID);
		if (me == null) return false;
		
		for (BHCollection.Atom a : engine.getCollection().atCoords(me))
		{
			if (a.getID() == me.getID()) continue;
			if (a.getType() != ATOM.MONSTER) continue;
			BHBuff monsterBuff = engine.getBuff(BHCollection.EntityTypeEnum.ITEM, me.getID(), BUFF_PACMONSTER);
			if (monsterBuff == null || monsterBuff.isCancelled) continue;
			
			me.setIntProp(INT_PROPS.HERO_GOLD, me.getIntProp(INT_PROPS.HERO_GOLD) + 100);
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  buff.actorID, "CHOMP!");
			processActionDie(a.getID());
		}
		*/
		return true;
	}

	/** props: [target portal item ID, moveTC, post-shift direction, phase] */
	public boolean processBuffPortal(BHBuff buff)
	{
		BHCollection.Atom me = engine.getCollection().getItem(buff.actorID);
		if (me == null) return false;
		
		int direction = buff.intProps[2];
		int phase = buff.intProps[3];
		
		if (phase == 0 && buff.intProps[1] != me.getIntProp(INT_PROPS.MOVE_TC)
				//&& direction != me.getIntProp(INT_PROPS.MOVE_DIR)
				)
		{
			return false; // another move happened
		}
		
		if (buff.ticks > 0)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  buff.actorID, "Beaming! " + buff.ticks);
			return true;
		}
		BHCollection.Atom dest = engine.getCollection().getItem(buff.intProps[0]);
		
		if (phase == 0)
		{
			// finally, do the port
			if (dest == null || dest.getType() != ATOM.PORTAL)
			{
				engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  buff.actorID, "Beaming failed! ");
				return false;
			}
			me.setCoords(dest);
			buff.intProps[3] = 1;
			
			//doMove(me.getID(), buff.intProps[2]);
			BHOperations.BHAction action = new BHOperations.BHAction();
			action.actionType = BHOperations.ACTION_JUMP;
			action.actorID = me.getID();
			action.intProps = Utils.intArray(engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
			
			engine.postAction(action, BHOperations.MOVE_SPEED);
			me.setIntProp(INT_PROPS.MOVE_TC, engine.timecode);
			me.setIntProp(INT_PROPS.MOVE_DIR, direction);
			
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  buff.actorID, "Beamed! " + me.toString());			
		}

		if (phase == 1 && !BHLandscape.equalCoords(dest, me)) // check if it's time to remove the buff
		{
			return false;
		}
		
		return true;
	}
}
