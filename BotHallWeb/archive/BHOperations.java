package net.vbelev.bothall.core;

import java.util.*;

import net.vbelev.bothall.core.BHCollection.EntityTypeEnum;
import net.vbelev.utils.Utils;

/**
 * A collection of classes used by BHEngine and
 * some standard action handlers 
 */
public class BHOperations
{

	// * direction index, same as BHLandscape.closestCells:
	// x-- = 12; x++ = 14; y-- = 10; y++ = 16
	// * basetimecode: 
	// when a movement is posted, we save the timecode in the atom props as MOVE_TC;
	// when the movement is processed, we check that its basetimecode matches the atom's prop.
	// if there are two movements in the (timers) queue, only the last one will be processed.
	
	/** 
	 * move after a delay; props: [basetimecode, direction, repeatflag, delay] 
	 * */
	public static final String ACTION_MOVE = "MOVE".intern();
	/** 
	 * immediate move; props: [basetimecode, direction, repeatflag, delay]
	 * "delay" is only needed if repeatflag > 0, it will be used to repeat the move
	 *  */
	public static final String ACTION_JUMP = "JUMP".intern();
	
	public static final String ACTION_STOPCYCLING = "STOPCYCLING".intern();
	
	/** Same as ACTION_MOVE, props: [basetimecode, direction, repeatflag, delay] 
	 * This buff will cancel itself if MOVE_TS is changed (i.e. another move is engaged)
	 * */
	public static final String BUFF_MOVE = "BUFF_MOVE".intern();
	
	public static int MOVE_SPEED = 4;

	public static void processAction(BHBoard engine, BHEngine.Action action)
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
			processActionMove(engine, me, action);
		}
		else if (action.actionType == ACTION_JUMP)
		{
			actionJump(engine, me, action);
		}
		else if (action.actionType == ACTION_STOPCYCLING)
		{
			// this should be overridden in the bhsession class
			engine.stopCycling();
		}
	}
	
	public static void doStop(BHEngine engine, BHCollection.Atom me, boolean hardStop)
	{
		//me.setIntProp(BHCollection.Atom.INT_PROPS.DX, 0);
		//me.setIntProp(BHCollection.Atom.INT_PROPS.DY, 0);
		//me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, 0);
		//me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC) + 1);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, 0);
		if (hardStop)
		{
			me.setCoords(me);
		}
	}
	
	/** 
	 * get the atom, update it to "moving", set the timer to jump after the delay.
	 */
	public static void processActionMove(BHBoard engine, BHCollection.Atom me, BHEngine.Action action)
	{
		//[basetimecode, direction, repeatflag, delay]
		int actionTimecode = action.intProps[0];
		int direction = action.intProps[1];
		int repeatFlag = action.intProps[2];
		int delay = action.intProps[3];
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);
		int baseDirection = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
		
		//if (actionTimecode > 0 && baseTimecode > 0 && actionTimecode != baseTimecode)
		//{
		//	return; // quietly ignore it end return
		//}
		
		if (direction == 0) // the special case of "move nowhere", it means stop
		{
			doStop(engine, me, false);
			return;
		}
		
		if (baseDirection == direction) // we are already moving there 
		{
			return; 
		}
			
		int[] shift = BHLandscape.cellShifts[direction];
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		
		//System.out.println("doMove: id=" + me.getID() + ", dir=" + direction + ", targetCell=" + targetCell);
		if (targetCell.getTerrain() != BHLandscape.TerrainEnum.LAND) 
		{
			if (baseDirection == 0) // we are not moving, so there is no point starting a buff 
			{
				engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "BOMM!");
				doStop(engine, me, false);
				System.out.println("BOMM");
			}
			else
			{
				BHEngine.Buff moveBuff = new BHEngine.Buff();
				BHEngine.Action moveAction = new BHEngine.Action(BHOperations.BUFF_MOVE, me.getID(), 0, 0);
				//moveBuff.actionType = BHOperations.BUFF_MOVE;
				//moveBuff.actorID = me.getID();
				//moveBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
				moveAction.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
				moveBuff.action = moveAction;
				moveBuff.ticks = 0;
				engine.postBuff(moveBuff);
				engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "Posted buff to move to " + direction);
				System.out.println("Posted buff move, delay=" + delay);
			}
			return;
		}
		
		BHEngine.Action jump = new BHEngine.Action();
		jump.actionType = BHOperations.ACTION_JUMP;
		jump.actorID = me.getID();
		//jump.actorType = BHCollection.EntityTypeEnum.ITEM;
		jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
		
		//me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
		//me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
		//me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
		me.setCoords(me);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);
		
		engine.postAction(jump, delay - 1); // 1 tick is already spent on doMove
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "I am moving to " + targetCell + " (" + direction + ")");
		System.out.println("Posted jump action-1 for id=" + jump.actorID + ", dir=" + direction + ", delay=" + delay + ", timecode=" + engine.timecode);
		
	}

	public static void actionJump(BHBoard engine, BHCollection.Atom me, BHEngine.Action action)
	{
		//[basetimecode, direction, repeatflag, delay]
		int actionTimecode = action.intProps[0];
		int direction = action.intProps[1];
		int repeatFlag = action.intProps[2];
		int delay = (repeatFlag > 0 && action.intProps.length > 3)? action.intProps[3] : 0;
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);
		
		//System.out.println("doJump, id=" + me.getID() + ", dir=" + direction);		
		if (actionTimecode > 0 && baseTimecode > 0 && actionTimecode != baseTimecode)
		{
			//System.out.println("timecode does not match, returning. actionTimecode" + actionTimecode + ", me.timecode=" + baseTimecode);
			return; // quietly ignore it end return
		}
		
		int[] shift = BHLandscape.cellShifts[direction];
		BHLandscape.Coords newPos = BHLandscape.coordsPoint(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		
		//BHLandscape.Cell targetCell = engine.getLandscape().getCell(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(newPos);
		if (targetCell.getTerrain() != BHLandscape.TerrainEnum.LAND) 
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "BOMM-M!");
			doStop(engine, me, false);
			//System.out.println("Hit the wall, escape, target=" + targetCell);
			return;
		}
		/*
		// don't let monsters move into each other
		if (me.getGrade() == BHCollection.Atom.GRADE.MONSTER)
		{
			for (BHCollection.Atom a : engine.getCollection().atCoords(newPos, false))
			{
				if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER) // don't move into another monster... hero is okay
				{
					doStop(engine, me);
					engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "BOMM-M-M!");
					System.out.println("Hit a monster, escaping, target=" + targetCell);
					return;
					
				}
			}
		}
		*/
		//System.out.println("actionJump, id=" + me.getID() + ", dir=" + direction + ", new pos=" + newPos.toString());
		me.setCoords(newPos);
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,  me.getID(), "I moved to " + direction);
		
		boolean shallRepeat = false;
		if (repeatFlag > 0)
		{
			//BHLandscape.Cell nextCell = engine.getLandscape().getNextCell(me.getX() + shift[0], me.getY() + shift[1], me.getZ() + shift[2], direction);
			BHLandscape.Cell nextCell = engine.getLandscape().getNextCell(newPos, direction);
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
			BHEngine.Action jump = new BHEngine.Action();
			jump.actionType = BHOperations.ACTION_JUMP;
			jump.actorID = me.getID();
			//jump.actorType = BHCollection.EntityTypeEnum.ITEM;
			jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
			
			//me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
			//me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
			//me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);
			
			//System.out.println("actionJump shall repeat to dir=" + direction + ", delay=" + delay);
			engine.postAction(jump, delay);
		}
		else
		{
			doStop(engine, me, false);
			/*
			me.setIntProp(BHCollection.Atom.INT_PROPS.DX, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DY, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, 0);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, 0);
			*/
		}		
	}

	
	
	/** Return false to mark the buff as disabled and subject to removal.
	 * [basetimecode, direction, repeatflag, delay]
	 *  */
	public static boolean processBuffMove(BHBoard engine, BHEngine.Buff buff)
	{
		if (buff.isCancelled || buff.action.actorID <= 0) 
		{
			System.out.println("buffMove: buff is cancelled, stopping");
			return false;
		}
		
		BHCollection.Atom me = engine.getCollection().getItem(buff.action.actorID);

		if (me == null || me.getID() == BHCollection.Atom.ITEM_STATUS.DELETE)
		{
			System.out.println("buffMove: atom not found stopping");
			buff.isCancelled = true;
			return false;
		}
	
		int actionTimecode = buff.action.intProps[0];
		int direction = buff.action.intProps[1];
		int repeatFlag = buff.action.intProps[2];
		//int delay = (repeatFlag > 0 && buff.intProps.length > 3)? buff.intProps[3] : 0;
		int delay = ( buff.action.intProps.length > 3)? buff.action.intProps[3] : 0;
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
		
		//System.out.println("buffMove: testing cell " + targetCell);
		// if it's land, lodge a jump and stop the buff
		if (targetCell.getTerrain() == BHLandscape.TerrainEnum.LAND) 
		{
			BHEngine.Action jump = new BHEngine.Action();
			jump.actionType = BHOperations.ACTION_JUMP;
			jump.actorID = me.getID();
			//jump.actorType = BHCollection.EntityTypeEnum.ITEM;
			jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag, delay);
			
			//me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
			//me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
			//me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
			me.setCoords(me);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);
			//System.out.println("buffMove: posted action " + jump + ", stopping");
			engine.postAction(jump, delay - 1);
			return false;
		}
		else if (baseTimecode == 0 || baseDirection == 0) // not moving anywhere
		{
			//System.out.println("buffMove: baseTimecode=" + baseTimecode + ", baseDirection=" + baseDirection + ", no movement - stopping buff");
			return false;
		}
		// if it's not land, skip and try again later
		//System.out.println("buffMove: try again next tick"); 
		return true;
	}
	
	/** some actions are taken automatically on some conditions, without anybody posting them */
	public static void processTriggers(BHEngine engine)
	{
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
	}
	
	public static boolean processBuff(BHBoard engine, BHEngine.Buff buff)
	{
		if (buff.action.actionType == BUFF_MOVE)
		{
			return processBuffMove(engine, buff);
		}
		else
		{
			engine.getMessages().addMessage(EntityTypeEnum.GLOBAL, 0, "Unknown buff: " + buff.action.actionType);
			return false;
		}
	}
}
