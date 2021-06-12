package net.vbelev.bothall.core;

import java.io.*;
import net.vbelev.utils.*;

/**
 * The subclass of BHEngine to implement a pacman-style walking board: a
 * collection of landscape cells in BHLandscape; a collection of items (gold
 * coins and pac berries); a collection of mobiles (monsters and the pacman);
 * 
 * plus some basic action processing - move and jump
 */
public class BHBoard extends BHEngine
{
	public interface IClientCallback
	{
		void onPublish(long timecode);
		void processAction(Action action);
		void processTriggers();
		boolean processBuff(Buff buff);
	}

	public static final class ACTION
	{
		public static final String MOVE = "MOVE".intern();
		/**
		 * immediate move; props: [basetimecode, direction, repeatflag, delay]
		 * "delay" is only needed if repeatflag > 0, it will be used to repeat
		 * the move
		 */
		public static final String JUMP = "JUMP".intern();

		public static final String STOPCYCLING = "STOPCYCLING".intern();
	}

	public static final class BUFF
	{
		public static final String MOVE = "MOVE".intern();
	}
	
	private BHLandscape landscape = null;
	private BHCollection items = null;

	public BHBoard() {
		this.landscape = new BHLandscape();
		this.items = new BHCollection();
	}

	public BHLandscape getLandscape()
	{
		return landscape;
	}

	public BHCollection getCollection()
	{
		return items;
	}

	public static BHBoard testEngine(int size)
	{
		BHBoard res = new BHBoard();

		res.landscape = testLandscape(size);
		BHLandscape.Cell[] landCells = res.landscape.cells.stream()
				.filter(q -> q.getTerrain() == BHLandscape.TerrainEnum.LAND)
				.toArray(BHLandscape.Cell[]::new);
		int heroCell = Utils.random.nextInt(landCells.length - 1);
		int m1Cell = Utils.random.nextInt(landCells.length - 1);
		int m2Cell = Utils.random.nextInt(landCells.length - 1);
		int m3Cell = Utils.random.nextInt(landCells.length - 1);

		res.items = new BHCollection();
		BHCollection.Atom hero = res.items.addAtom("HERO",
				BHCollection.Atom.GRADE.HERO);
		hero.setCoords(landCells[heroCell]);
		BHCollection.Atom m1 = res.items.addAtom("MONSTER",
				BHCollection.Atom.GRADE.MONSTER);
		m1.setCoords(landCells[m1Cell]);
		BHCollection.Atom m2 = res.items.addAtom("MONSTER",
				BHCollection.Atom.GRADE.MONSTER);
		m2.setCoords(landCells[m2Cell]);
		BHCollection.Atom m3 = res.items.addAtom("MONSTER",
				BHCollection.Atom.GRADE.MONSTER);
		m3.setCoords(landCells[m3Cell]);

		res.publish();
		return res;
	}

	public BHBoard loadFileEngine(String fileName)
	{
		BHBoard res = this; // new BHBoard();

		try
		{
			InputStream is = BHEngine.class.getResourceAsStream(fileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			int x = -1;
			int y = -1;
			int z = 0;
			// BHCollection.Atom hero = null;
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
						res.landscape.setCell(new BHLandscape.Cell(x, y, z,
								BHLandscape.TerrainEnum.STONE));
					}
					else if (c == ' ')
					{
						res.landscape.setCell(new BHLandscape.Cell(x, y, z,
								BHLandscape.TerrainEnum.LAND));
					}
					else if (c == '@')
					{
						res.landscape.setCell(new BHLandscape.Cell(x, y, z,
								BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom hero = res.items.addAtom("HERO",
								BHCollection.Atom.GRADE.HERO);
						hero.setX(x);
						hero.setY(y);
						hero.setZ(z);
					}
					else if (c == 'M')
					{
						res.landscape.setCell(new BHLandscape.Cell(x, y, z,
								BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom hero = res.items.addAtom("MONSTER",
								BHCollection.Atom.GRADE.MONSTER);
						hero.setX(x);
						hero.setY(y);
						hero.setZ(z);
					}
					else if (c == '.')
					{
						res.landscape.setCell(new BHLandscape.Cell(x, y, z,
								BHLandscape.TerrainEnum.LAND));
						BHCollection.Atom item = res.items.addAtom("GOLD",
								BHCollection.Atom.GRADE.ITEM);
						item.setX(x);
						item.setY(y);
						item.setZ(z);
					}
					else
					{
						// res.landscape.setCell(new BHLandscape.Cell(x, y, z,
						// BHLandscape.TerrainEnum.VOID));
						System.out.println("Unsupported mark [" + (char) c
								+ "] when reading " + fileName);
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

	public static BHLandscape testLandscape(int size)
	{
		BHLandscape res = new BHLandscape();

		for (int x = 0; x < size; x++)
		{
			for (int y = 0; y < size; y++)
			{
				res.setCell(new BHLandscape.Cell(x, y, 0,
						BHLandscape.TerrainEnum.VOID));
			}
		}

		// add a mountain
		res.setCell(new BHLandscape.Cell(Utils.random.nextInt(size - 1),
				Utils.random.nextInt(size - 1), 0,
				BHLandscape.TerrainEnum.STONE));
		res = res.publish(0); // no need to advance the timecode at the
								// initialization
		boolean hasMore = true;
		int rounds = 0;
		while (hasMore && rounds < 100)
		{
			hasMore = false;
			rounds++;
			for (int x = 0; x < size; x++)
			{
				for (int y = 0; y < size; y++)
				{
					BHLandscape.Cell c = res.getCell(x, y, 0);
					if (c.getTerrain() == BHLandscape.TerrainEnum.STONE)
					{
						BHLandscape.Cell[] closest = res.closestCells(c.getX(),
								c.getY(), c.getZ());
						for (BHLandscape.Cell c1 : closest)
						{

							if (c1.getTerrain() != BHLandscape.TerrainEnum.VOID
									|| c1.getX() < 0 || c1.getX() >= size
									|| c1.getY() < 0 || c1.getY() >= size
									|| c1.getZ() != 0)
							{
								continue;
							}
							BHLandscape.Cell[] closest2 = res.closestCells(
									c1.getX(), c1.getY(), c1.getZ());
							int stoneCount = 0;
							int rndMark = 0;
							for (BHLandscape.Cell c2 : closest2)
							{
								if (c2.getTerrain() == BHLandscape.TerrainEnum.STONE)
									stoneCount++;
							}
							if (stoneCount == 1) rndMark = 70;
							else if (stoneCount == 2) rndMark = 50;
							else if (stoneCount == 3) rndMark = 30;

							int rnd = Utils.random.nextInt(100);

							// System.out.println("for x=" + c1.getX() + ", y="
							// + c1.getY() + ", rnd=" + rnd);
							if (rnd <= rndMark)
							{
								res.setCell(c1.terrain(
										BHLandscape.TerrainEnum.STONE));
								hasMore = true;
							}
							else
							{
								res.setCell(c1
										.terrain(BHLandscape.TerrainEnum.LAND));
							}
						}
					}
				}
			}
			res = res.publish(0); // no need to advance the timecode at the
									// initialization
		}
		for (int x = 0; x < size; x++)
		{
			for (int y = 0; y < size; y++)
			{
				if (res.getCell(x, y, 0)
						.getTerrain() == BHLandscape.TerrainEnum.VOID)
					res.setCell(new BHLandscape.Cell(x, y, 0,
							BHLandscape.TerrainEnum.LAND));
			}
		}

		return res;
	}

	public IClientCallback clientCallback = null;

	@Override
	public long publish()
	{
		// note that super.publish() increments the timecode and
		// we need to either call it before the rest of publishing logic,
		// or remember to use (timecode + 1)
		long newTimecode = super.publish();

		this.landscape = this.landscape.publish(newTimecode);
		this.items = items.publish(newTimecode);

		if (clientCallback != null)
		{
			clientCallback.onPublish(newTimecode);
		}

		return newTimecode;
	}

	@Override
	public void processAction(BHEngine.Action action)
	{
		// myEngine.getMessages().addMessage(action.targetType, action.targetID,
		// "Action " + action.ID + ": " + action.message);
		// BHOperations.processAction(BHEngine.this, action);

		BHCollection.Atom me = getCollection().getItem(action.actorID);
		if (me == null)
		{
			getMessages().addMessage(BHCollection.EntityTypeEnum.GLOBAL, 0,
					"ActorID not found: " + action.actorID);
			// return "No atom id" + session.myID;
		}
		else
		{
			// app.engine.publish();
			// engine.getMessages().addMessage(BHEngine.EntityTypeEnum.GLOBAL,
			// 0, "ActorID " + action.actorID + " moved!");
		}
		if (action.actionType == ACTION.MOVE)
		{
			processActionMove(me, action);
		}
		else if (action.actionType == ACTION.JUMP)
		{
			actionJump(me, action);
		}
		else if (action.actionType == ACTION.STOPCYCLING)
		{
			// this should be overridden in the bhsession class
			this.stopCycling();
		}
		else if (clientCallback != null)
		{
			clientCallback.processAction(action);
		}
		// System.out.println("Action processed by queueProcessor " + instanceID
		// + ": #" + action.ID + " " + action.message);
	}

	@Override
	public boolean processBuff(BHEngine.Buff buff)
	{
		if (buff.action.actionType == BUFF.MOVE)
		{
			return processBuffMove(buff);
		}
		else if (clientCallback != null)
		{
			return clientCallback.processBuff(buff);
		}
		return false;
	}

	@Override
	public void processTriggers()
	{
		if (clientCallback != null)
		{
			clientCallback.processTriggers();
		}
	}

	public void doStop(BHCollection.Atom me, boolean hardStop)
	{
		BHEngine engine = this;
		// me.setIntProp(BHCollection.Atom.INT_PROPS.DX, 0);
		// me.setIntProp(BHCollection.Atom.INT_PROPS.DY, 0);
		// me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, 0);
		// me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC,
				me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC) + 1);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, 0);
		if (hardStop)
		{
			me.setCoords(me);
		}
	}

	public void processActionMove(BHCollection.Atom me, BHEngine.Action action)
	{
		BHBoard engine = this;

		// [basetimecode, direction, repeatflag, delay]
		int actionTimecode = action.intProps[0];
		int direction = action.intProps[1];
		int repeatFlag = action.intProps[2];
		int delay = action.intProps[3];
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);
		int baseDirection = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);

		// if (actionTimecode > 0 && baseTimecode > 0 && actionTimecode !=
		// baseTimecode)
		// {
		// return; // quietly ignore it end return
		// }

		if (direction == 0) // the special case of "move nowhere", it means stop
		{
			doStop(me, false);
			return;
		}

		if (baseDirection == direction) // we are already moving there
		{
			return;
		}

		int[] shift = BHLandscape.cellShifts[direction];
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(
				me.getX() + shift[0], me.getY() + shift[1],
				me.getZ() + shift[2]);

		// System.out.println("doMove: id=" + me.getID() + ", dir=" + direction
		// + ", targetCell=" + targetCell);
		if (targetCell.getTerrain() != BHLandscape.TerrainEnum.LAND)
		{
			if (baseDirection == 0) // we are not moving, so there is no point
									// starting a buff
			{
				engine.getMessages().addMessage(
						BHCollection.EntityTypeEnum.ITEM, me.getID(), "BOMM!");
				doStop(me, false);
				System.out.println("BOMM");
			}
			else
			{
				BHEngine.Buff moveBuff = new BHEngine.Buff();
				BHEngine.Action moveAction = new BHEngine.Action(
						BUFF.MOVE, me.getID(), 0, 0);
				// moveBuff.actionType = BHOperations.BUFF_MOVE;
				// moveBuff.actorID = me.getID();
				// moveBuff.actorType = BHCollection.EntityTypeEnum.ITEM;
				moveAction.intProps = Utils.intArray(engine.timecode, direction,
						repeatFlag, delay);
				moveBuff.action = moveAction;
				moveBuff.ticks = 0;
				engine.postBuff(moveBuff);
				engine.getMessages().addMessage(
						BHCollection.EntityTypeEnum.ITEM, me.getID(),
						"Posted buff to move to " + direction);
				System.out.println("Posted buff move, delay=" + delay);
			}
			return;
		}

		Action jump = new Action();
		jump.actionType = ACTION.JUMP;
		jump.actorID = me.getID();
		// jump.actorType = BHCollection.EntityTypeEnum.ITEM;
		jump.intProps = Utils.intArray(engine.timecode, direction, repeatFlag,
				delay);

		// me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
		// me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
		// me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
		me.setCoords(me);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
		me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);

		engine.postAction(jump, delay - 1); // 1 tick is already spent on doMove
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
				me.getID(),
				"I am moving to " + targetCell + " (" + direction + ")");
		System.out.println("Posted jump action-1 for id=" + jump.actorID
				+ ", dir=" + direction + ", delay=" + delay + ", timecode="
				+ engine.timecode);

	}

	public void actionJump(BHCollection.Atom me, BHEngine.Action action)
	{
		BHBoard engine = this;
		// [basetimecode, direction, repeatflag, delay]
		int actionTimecode = action.intProps[0];
		int direction = action.intProps[1];
		int repeatFlag = action.intProps[2];
		int delay = (repeatFlag > 0 && action.intProps.length > 3)
				? action.intProps[3]
				: 0;
		int baseTimecode = me.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC);

		// System.out.println("doJump, id=" + me.getID() + ", dir=" +
		// direction);
		if (actionTimecode > 0 && baseTimecode > 0
				&& actionTimecode != baseTimecode)
		{
			// System.out.println("timecode does not match, returning.
			// actionTimecode" + actionTimecode + ", me.timecode=" +
			// baseTimecode);
			return; // quietly ignore it end return
		}

		int[] shift = BHLandscape.cellShifts[direction];
		BHLandscape.Coords newPos = BHLandscape.coordsPoint(
				me.getX() + shift[0], me.getY() + shift[1],
				me.getZ() + shift[2]);

		// BHLandscape.Cell targetCell = engine.getLandscape().getCell(me.getX()
		// + shift[0], me.getY() + shift[1], me.getZ() + shift[2]);
		BHLandscape.Cell targetCell = engine.getLandscape().getCell(newPos);
		if (targetCell.getTerrain() != BHLandscape.TerrainEnum.LAND)
		{
			engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
					me.getID(), "BOMM-M!");
			doStop(me, false);
			// System.out.println("Hit the wall, escape, target=" + targetCell);
			return;
		}
		/*
		 * // don't let monsters move into each other if (me.getGrade() ==
		 * BHCollection.Atom.GRADE.MONSTER) { for (BHCollection.Atom a :
		 * engine.getCollection().atCoords(newPos, false)) { if (a.getGrade() ==
		 * BHCollection.Atom.GRADE.MONSTER) // don't move into another
		 * monster... hero is okay { doStop(engine, me);
		 * engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
		 * me.getID(), "BOMM-M-M!");
		 * System.out.println("Hit a monster, escaping, target=" + targetCell);
		 * return;
		 * 
		 * } } }
		 */
		// System.out.println("actionJump, id=" + me.getID() + ", dir=" +
		// direction + ", new pos=" + newPos.toString());
		me.setCoords(newPos);
		engine.getMessages().addMessage(BHCollection.EntityTypeEnum.ITEM,
				me.getID(), "I moved to " + direction);

		boolean shallRepeat = false;
		if (repeatFlag > 0)
		{
			// BHLandscape.Cell nextCell =
			// engine.getLandscape().getNextCell(me.getX() + shift[0], me.getY()
			// + shift[1], me.getZ() + shift[2], direction);
			BHLandscape.Cell nextCell = engine.getLandscape()
					.getNextCell(newPos, direction);
			if (nextCell.getTerrain() == BHLandscape.TerrainEnum.LAND)
			{
				shallRepeat = true;
			}
		}
		if (shallRepeat) // same as doMove
		{
			/*
			 * BHAction move = new BHAction(); move.actionType =
			 * BHOperations.ACTION_JUMP; move.actorID = me.getID();
			 * move.actorType = BHEngine.EntityTypeEnum.ITEM; move.intProps =
			 * Utils.intArray(engine.timecode, direction, 1, delay);
			 * me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timeco
			 * engine.postAction(move, 0);
			 */
			Action jump = new Action();
			jump.actionType = ACTION.JUMP;
			jump.actorID = me.getID();
			// jump.actorType = BHCollection.EntityTypeEnum.ITEM;
			jump.intProps = Utils.intArray(engine.timecode, direction,
					repeatFlag, delay);

			// me.setIntProp(BHCollection.Atom.INT_PROPS.DX, delay * shift[0]);
			// me.setIntProp(BHCollection.Atom.INT_PROPS.DY, delay * shift[1]);
			// me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, delay * shift[2]);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, engine.timecode);
			me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, direction);

			// System.out.println("actionJump shall repeat to dir=" + direction
			// + ", delay=" + delay);
			engine.postAction(jump, delay);
		}
		else
		{
			doStop(me, false);
			/*
			 * me.setIntProp(BHCollection.Atom.INT_PROPS.DX, 0);
			 * me.setIntProp(BHCollection.Atom.INT_PROPS.DY, 0);
			 * me.setIntProp(BHCollection.Atom.INT_PROPS.DZ, 0);
			 * me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC, 0);
			 * me.setIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR, 0);
			 */
		}
	}

	public boolean processBuffMove(BHEngine.Buff buff)
	{
		BHBoard engine = this;
		
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
			jump.actionType = ACTION.JUMP;
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

}
