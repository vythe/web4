package net.vbelev.bothall.core;

import java.util.*;

import net.vbelev.bothall.core.BHCollection.EntityTypeEnum;
import net.vbelev.utils.Utils;

public class BHOperations
{

	// * direction index, same as BHLandscape.closestCells:
	// x-- = 12; x++ = 14; y-- = 10; y++ = 16
	// * basetimecode: 
	// when a movement is posted, we save the timecode in the atom props as MOVE_TC;
	// when the movement is processed, we check that its basetimecode matches the atom's prop.
	// if there are two movements in the (timers) queue, only the last one will be processed.
	
	/** move after a delay; props: [basetimecode, direction, repeatflag, delay] */
	public static final String ACTION_MOVE = "MOVE".intern();
	/** immediate move; props: [basetimecode, direction, repeatflag, delay]
	 *  "delay" is only needed if repeatflag > 0, it will be used to repeat the move
	 *  */
	public static final String ACTION_JUMP = "JUMP".intern();
	
	public static final String ACTION_STOPCYCLING = "STOPCYCLING".intern();
	
	/** Same as ACTION_MOVE, props: [basetimecode, direction, repeatflag, delay] 
	 * This buff will cancel itself if MOVE_TS is changed (i.e. another move is engaged)
	 * */
	public static final String BUFF_MOVE = "BUFF_MOVE".intern();
	
	public static int MOVE_SPEED = 4;
	
	public static class BHAction
	{
		public static int instanceCounter = 0;
		public final int ID = ++instanceCounter;
		
		/** actionType should be interned or set from BHOperations constants */
		public String actionType; 
		public BHCollection.EntityTypeEnum actorType;
		public int actorID;
		public BHCollection.EntityTypeEnum targetType;
		public int targetID;
		public String message;
		// different action types will have different props, but we know which prop is where
		public int[] intProps = null;
		// later
		
		public String toString()
		{
			return "[BHA:" + actionType + ", actorID=" + actorID + ", props=" + Arrays.toString(intProps) + "]"; 
		}
	}

	public static class BHTimer implements Comparable<BHTimer>
	{
		public BHAction action;
		public long timecode;
		@Override
		public int compareTo(BHTimer arg0)
		{
			if (timecode < arg0.timecode) return -1;
			if (timecode > arg0.timecode) return 1;
			return action.ID - arg0.action.ID;
		}
	}
	
	/**
	 * Buff is an action that is executed every tick for a given number of ticks, 
	 * or until the buff cancels itself.
	 * Note that the buff is stateful.
	 * @author Vythe
	 *
	 */
	public static class BHBuff extends BHAction
	{
		/** This is when the buff was created or changed, 
		 * for reporting it to the clients
		 */
		public long timecode;
		public int ticks;
		public boolean isCancelled;
	}
	
	public static void processAction(BHEngine engine, BHAction action)
	{
		//System.out.println("Action processed by " + engine.engineInstance + ": #" + action.ID + " " + action.message);
		
		BHCollection.Atom me = engine.getCollection().getItem(action.actorID);
		if (me == null)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0, "ActorID not found: " + action.actorID);
			//return "No atom id" + session.myID;
		}
		else
		{
			//app.engine.publish();
			//engine.getMessages().addMessage(BHEngine.EntityTypeEnum.GLOBAL, 0, "ActorID " + action.actorID + " moved!");
		}
		if (action.actionType == ACTION_MOVE)
		{
			doMove(engine, me, action);
		}
		else if (action.actionType == ACTION_JUMP)
		{
			doJump(engine, me, action);
		}
		else if (action.actionType == ACTION_STOPCYCLING)
		{
			engine.stopCycling();
		}
	}
	
	/** 
	 * get the atom, update it to "moving", set the timer to jump after the delay.
	 */
	public static void doMove(BHEngine engine, BHCollection.Atom me, BHAction action)
	{
		//[basetimecode, direction, repeatflag, delay]
		int actionTimecode = action.intProps[0];
		int direction = action.intProps[1];
		int repeatFlag = action.intProps[2];
		int delay = action.intProps[3];
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);
		//if (actionTimecode > 0 && baseTimecode > 0 && actionTimecode != baseTimecode)
		//{
		//	return; // quietly ignore it end return
		//}
		
		int[] shift = BHLandscape.cellShifts[direction];
		
		if (direction == 0) // the special case of "move nowhere", it means stop
		{
			me.setIntProp(BHCollection.Atom.INT_PROPS.DX, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DY, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, 0);
			return;
		}
		
		if (me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR) == direction) // we are already moving there 
		{
			return; 
		}
			
			
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		if (targetCell.getTerrain() != BHLandscape.TerrainEnum.LAND) 
		{
			if (baseTimecode == 0) // we are not moving, so there is no point starting a buff 
			{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "BOMM!");
			}
			else
			{
				BHBuff moveBuff = new BHBuff();
				moveBuff.actionType = BHOperations.BUFF_MOVE;
				moveBuff.actorID = me.getID();
				moveBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
				moveBuff.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
				engine.postBuff(moveBuff);
				engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "Posted buff to move to " + direction);
			}
			return;
		}
		BHAction jump = new BHAction();
		jump.actionType = BHOperations.ACTION_JUMP;
		jump.actorID = me.getID();
		jump.actorType = BHCollection.EntityTypeEnum.ITEM;
		jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
		
		me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
		me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
		me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);
		
		engine.postAction(jump, delay - 1); // 1 tick is already spent on doMove
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "I am moving to " + targetCell + " (" + direction + ")");
		
	}

	public static void doJump(BHEngine engine, BHCollection.Atom me, BHAction action)
	{
		//[basetimecode, direction, repeatflag, delay]
		int actionTimecode = action.intProps[0];
		int direction = action.intProps[1];
		int repeatFlag = action.intProps[2];
		int delay = (repeatFlag > 0 && action.intProps.length > 3)? action.intProps[3] : 0;
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);
		
		if (actionTimecode > 0 && baseTimecode > 0 && actionTimecode != baseTimecode)
		{
			return; // quietly ignore it end return
		}
		
		int[] shift = BHLandscape.cellShifts[direction];
		
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		if (targetCell.getTerrain() != BHLandscape.TerrainEnum.LAND) 
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "BOMM-M!");
			return;
		}
		
		me.setX(me.getX() + shift[0]);
		me.setY(me.getY() + shift[1]);
		me.setZ(me.getZ() + shift[2]);
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "I moved to " + direction);
		
		boolean shallRepeat = false;
		if (repeatFlag > 0)
		{
			BHLandscape.Cell nextCell = engine.getLandscape().closestCells(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2])[direction];
			if (nextCell.getTerrain() == BHLandscape.TerrainEnum.LAND)
			{
				shallRepeat = true;
			}
		}
		if (shallRepeat) // same as doMove
		{
			/*
			BHAction move = new BHAction();
			move.actionType = BHOperations.ACTION_JUMP;
			move.actorID = me.getID();
			move.actorType = BHEngine.EntityTypeEnum.ITEM;
			move.intProps = Utils.intArray(engine.timecode, direction, 1, delay);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timeco
			engine.postAction(move, 0);
			*/
			BHAction jump = new BHAction();
			jump.actionType = BHOperations.ACTION_JUMP;
			jump.actorID = me.getID();
			jump.actorType = BHCollection.EntityTypeEnum.ITEM;
			jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
			
			me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
			
			engine.postAction(jump, delay);
		}
		else
		{
			me.setIntProp(BHCollection.Atom.INT_PROPS.DX, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DY, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, 0);
		}		
	}

	/** Return false to mark the buff as disabled and subject to removal.
	 * [basetimecode, direction, repeatflag, delay]
	 *  */
	public static boolean doBuffMove(BHEngine engine, BHBuff buff)
	{
		if (buff.isCancelled || buff.actorID <= 0) 
		{
			System.out.println("buffMove: buff is cancelled, stopping");
			return false;
		}
		
		BHCollection.Atom me = engine.getCollection().getItem(buff.actorID);

		if (me == null || me.getID() == BHCollection.Atom.ITEM_STATUS.DELETE)
		{
			System.out.println("buffMove: atom not found stopping");
			buff.isCancelled = true;
			return false;
		}
	
		int actionTimecode = buff.intProps[0];
		int direction = buff.intProps[1];
		int repeatFlag = buff.intProps[2];
		int delay = (repeatFlag > 0 && buff.intProps.length > 3)? buff.intProps[3] : 0;
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);
		int baseDirection = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
		
		if (baseTimecode != 0 && direction == baseDirection)
		{
			System.out.println("buffMove: baseTimecode=" + baseTimecode + ", direction=" + direction + " already moving there, stopping buff");
			buff.isCancelled = true;
			return false;			
		}
		/*
		if (actionTimecode > 0 && baseTimecode > 0 && actionTimecode != baseTimecode)
		{
			buff.isCancelled = true;
			return false; // quietly ignore it end return
		}
		*/
		int[] shift = BHLandscape.cellShifts[direction];		
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		
		System.out.println("buffMove: testing cell " + targetCell);
		// if it's land, lodge a jump and stop the buff
		if (targetCell.getTerrain() == BHLandscape.TerrainEnum.LAND) 
		{
			BHAction jump = new BHAction();
			jump.actionType = BHOperations.ACTION_JUMP;
			jump.actorID = me.getID();
			jump.actorType = BHCollection.EntityTypeEnum.ITEM;
			jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
			
			me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
			System.out.println("buffMove: posted action " + jump + ", stopping");
			engine.postAction(jump, delay - 1);
			return false;
		}
		else if (baseTimecode == 0 || baseDirection == 0) // not moving anywhere
		{
			System.out.println("buffMove: baseTimecod=" + baseTimecode + ", baseDirection=" + baseDirection + ", no movement - stopping buff");
		}
		// if it's not land, skip and try again later
		System.out.println("buffMove: try again next tick"); 
		return true;
	}
	
	/** some actions are taken automatically on some conditions, without anybody posting them */
	public static void processTriggers(BHEngine engine)
	{
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
	}
	
	public static boolean processBuff(BHEngine engine, BHBuff buff)
	{
		if (buff.actionType == BUFF_MOVE)
		{
			return doBuffMove(engine, buff);
		}
		else
		{
			engine.getMessages().addMessage(EntityTypeEnum.GLOBAL, 0, "Unknown buff: " + buff.actionType);
			return false;
		}
	}
}
