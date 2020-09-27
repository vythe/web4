package net.vbelev.bothall.web;

import java.util.*;
import net.vbelev.bothall.core.*;
import net.vbelev.bothall.core.BHOperations.BHAction;
import net.vbelev.bothall.core.BHOperations.BHBuff;
import net.vbelev.utils.Utils;
/**
 * Here we put the methods specific to pacman
 * @author Vythe
 *
 */
public class PacmanSession 
{
	public static final String ACTION_DIE = "DIE".intern();
	public static final String BUFF_DIE = "DIE".intern();

	
	private static int sessionInstanceSeq = 0;
	private static final ArrayList<PacmanSession> sessionList = new ArrayList<PacmanSession>();
	
	private int sessionID = 0;
	private BHEngine engine;

	public int engineTimecode = 0;

	public final BHStorage storage = new BHStorage();
	
	public final Map<Integer, BHLandscape.Coords> startingPoints = new java.util.Hashtable<Integer, BHLandscape.Coords>();
	
	public static PacmanSession createSession()
	{
		BHEngine e = BHEngine.loadFileEngine("/../data/pacman.txt");
		e.CYCLE_MSEC = 200;
		PacmanSession s =  PacmanSession.createSession(e);
		
		return s;
	}
	
	
	public static PacmanSession createSession(BHEngine e)
	{
		PacmanSession s = new PacmanSession();
		s.engine = e;
		e.clientCallback = s.new EngineCallback();
		
		s.sessionID = ++sessionInstanceSeq;
		synchronized(sessionList)
		{
			sessionList.add(s);
		}
		e.publish();
		
		for (BHCollection.Atom a : e.getCollection().all())
		{
			if (a.getGrade() != BHCollection.Atom.GRADE.ITEM)
			{
				s.startingPoints.put(a.getID(), new BHLandscape.CoordsBase(a));
			}
		}
		
		return s;
	}	

	public static PacmanSession getSession(int id)
	{
		synchronized(sessionList)
		{
			for (PacmanSession s : sessionList)
			{
				if (s.sessionID == id) return s;
			}
		}
		return null;
	}	
	
	public int getID() { return sessionID; }
	
	public BHEngine getEngine() { return engine; }

	
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
		}
		
		public void processAction(BHAction action)
		{
			BHCollection.Atom me = engine.getCollection().getItem(action.actorID);
			if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
			{
				return; //"No atom id" + session.myID;
			}
			if (action.actionType == PacmanSession.ACTION_DIE)
			{
				PacmanSession.this.processActionDie(action.actorID);
			}
			else
			{
				BHOperations.processAction(engine, action);
			}
			
		}

		public boolean processBuff(BHBuff buff)
		{
			if (buff.actionType == PacmanSession.BUFF_DIE)
			{
				return PacmanSession.this.processBuffDie(buff);
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
				if (a.getGrade() == BHCollection.Atom.GRADE.HERO)
				{
					// *) eat the gold here
					Collection<BHCollection.Atom> spot = engine.getCollection().atCoords(a);
					if (spot.size() <= 1) continue;  // nothing to see here
					for (BHCollection.Atom it : spot)
					{
						if (it.getID() == a.getID()) continue;
						if (it.getType() == "GOLD" && it.getStatus() != BHCollection.Atom.ITEM_STATUS.DELETE)
						{
							int goldCount = a.getIntProp(BHCollection.Atom.INT_PROPS.HERO_GOLD) + 1;
							a.setIntProp(BHCollection.Atom.INT_PROPS.HERO_GOLD, goldCount);
							engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  a.getID(), "Yum! Count=" + goldCount);
							it.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
						}
					}
				}
				else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER)
				{
					int moveDir = a.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
					// *) eat the hero here					
					Collection<BHCollection.Atom> spot = engine.getCollection().atCoords(a);
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
							processActionDie(it.getID());
						}
						else if (it.getGrade() == BHCollection.Atom.GRADE.MONSTER
								&& moveDir != 0
								&& it.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == moveDir
								&& a.getID() < it.getID()
								)
						{
							engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL,  0, "Jam! id1=" + a.getID() + ", id2=" + it.getID());
							BHOperations.doStop(engine, it);
						}
					}
				}
			}
		}
	}
	
	/** 
	 * check if the direction is open and move there;
	 * if the direction is not open, but the mobile is moving - post the movement buff;
	 * else return null. 
	 */
	public Integer doMove(int mobileID, int direction)
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
				action.actorID = me.getID();
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
	
	public int processActionDie(int mobileID)
	{
		BHCollection.Atom me = engine.getCollection().getItem(mobileID);
		if (me == null || me.getStatus() == BHCollection.Atom.ITEM_STATUS.DELETE)
		{
			return 0; //"No atom id" + session.myID;
		}		
		me.setStatus(BHCollection.Atom.ITEM_STATUS.DELETE);
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  mobileID, "You got died!");
		
		BHOperations.BHBuff dieBuff = new BHOperations.BHBuff();
		dieBuff.actionType = PacmanSession.BUFF_DIE;
		dieBuff.actorID = mobileID;
		dieBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
		//moveBuff.intProps = Utils.intArray(s.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
		
		dieBuff.ticks = 5;
		
		this.engine.postBuff(dieBuff);
		
		return dieBuff.ID;
	}
	
	public boolean processBuffDie(BHBuff buff)
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
}
