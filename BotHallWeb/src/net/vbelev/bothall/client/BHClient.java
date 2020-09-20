package net.vbelev.bothall.client;

import java.io.*;
import java.util.*;

import net.vbelev.utils.DryCereal;

/**
 * This will be a client-side mirror of the engine - terrain, items, mobs.
 * The server will send in serialized versions of these structs.
 *
 * Why not use the standard java serialization: 
 * it needs to work with javascript and generally export plain text lines.
 * Why not json? too much chaff and we don't need 
 */
public class BHClient
{
	public static class Buff
	{
		public int id;
		
		public String type;
		/** ticks are not guaranteed to be published every time it changes */
		public int ticks;
		public long timecode;
		public boolean isCancelled;
		//public long changeTimecode;

		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addMoniker(type);
			to.addInt(ticks);
			to.addLong(timecode);
			to.addByte(isCancelled?1 : 0);
		}
		
		public void fromCereal(DryCereal.Reader from)
		{
			id = (Integer)from.next().value;
			type = (String)from.next().value;
			ticks = (Integer)from.next().value;
			timecode = (Long)from.next().value;
			byte bc = (Byte)from.next().value;
			isCancelled = (bc > 0);
		}
	}

	/** same cellShifts as in BHLandscape, it should be shared somehow... */
	public static final int[][] cellShifts = {
			{ 0,  0,  0}, // 0 : self
			{ 1,  0,  0}, // 1: x++
			{-1,  0,  0}, // 2: x--
			{ 0,  1,  0}, // 3: y++
			{ 1,  1,  0},
			{-1,  1,  0}, 
			{ 0, -1,  0}, // 6: y--
			{ 1, -1,  0},
			{-1, -1,  0},
			{ 0,  0,  1}, // 9 : z++
			{ 1,  0,  1}, //10
			{-1,  0,  1},
			{ 0,  1,  1},
			{ 1,  1,  1}, 
			{-1,  1,  1}, 
			{ 0, -1,  1}, // 15
			{ 1, -1,  1},
			{-1, -1,  1},
			{ 0,  0, -1}, //18 : z--
			{ 1,  0, -1},
			{-1,  0, -1}, //20
			{ 0,  1, -1},
			{ 1,  1, -1},
			{-1,  1, -1}, 
			{ 0, -1, -1}, 
			{ 1, -1, -1},
			{-1, -1, -1} // 26
	};
	
	public static class TypeCode
	{
		public static final int STATUS = 1;
		public static final int CELL = 1;
		public static final int ITEM = 2;
		public static final int MOBILE = 3;
		public static final int BUFF = 4;		
	}
	
	public static class Cell
	{
		public int id;
		
		public int x;
		public int y;
		public int z;
		
		public String terrain;
		
		public Buff[] buffs;
		
		public Cell()
		{
		}
		
		public Cell(int x, int y, int z, String terrain)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.terrain = terrain;
		}
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addShort(x);
			to.addShort(y);
			to.addShort(z);
			to.addMoniker(terrain);
			to.addObjectStart((short)(buffs == null? 0 : buffs.length));
			if (buffs != null)
			{
				for (Buff b : buffs) b.toCereal(to);
			}
			to.addObjectEnd((short)(buffs == null? 0 : buffs.length));
		}
		
		public void fromCereal(DryCereal.Reader from) throws IOException
		{
			id = (Integer)from.next().value;
			x = (Short)from.next().value;
			y = (Short)from.next().value;
			z = (Short)from.next().value;
			terrain = (String)from.next().value;
			short key = (Short)from.next().value;
			buffs = new Buff[key];
			for (int b = 0; b < key; b++)
			{
				BHClient.Buff buff = new BHClient.Buff();
				buff.fromCereal(from);
				buffs[b] = buff;
			}
			from.next(); // object end
		}
	}

	public static class Item
	{
		public int id;
		public int status;
		
		public int x;
		public int y;
		public int z;
		
		public String itemtype;
		
		public Buff[] buffs;
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addByte(status);
			to.addShort(x);
			to.addShort(y);
			to.addShort(z);
			to.addMoniker(itemtype);
			to.addObjectStart((short)(buffs == null? 0 : buffs.length));
			if (buffs != null)
			{
				for (Buff b : buffs) b.toCereal(to);
			}
			to.addObjectEnd((short)(buffs == null? 0 : buffs.length));
		}
		
		public void fromCereal(DryCereal.Reader from) throws IOException
		{
			id = (Integer)from.next().value;
			status = (Byte)from.next().value;
			x = (Short)from.next().value;
			y = (Short)from.next().value;
			z = (Short)from.next().value;
			itemtype = (String)from.next().value;
			short key = (Short)from.next().value;
			buffs = new Buff[key];
			for (int b = 0; b < key; b++)
			{
				BHClient.Buff buff = new BHClient.Buff();
				buff.fromCereal(from);
				buffs[b] = buff;
			}
			from.next(); // object end
		}		
	}

	public static class Mobile
	{
		public int id;
		public int status;
		
		public int x;
		public int y;
		public int z;
		
		// here will be some movement info
		public int dir; 
		/** the tick when the movement will be completed */
		public int moveTick; 
		
		public String mobiletype;
		public String name;
		
		public Buff[] buffs;
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addByte(status);
			to.addShort(x);
			to.addShort(y);
			to.addShort(z);
			to.addByte(dir);
			to.addInt(moveTick);
			to.addMoniker(mobiletype);
			to.addString(name);
			to.addObjectStart((short)(buffs == null? 0 : buffs.length));
			if (buffs != null)
			{
				for (Buff b : buffs) b.toCereal(to);
			}
			to.addObjectEnd((short)(buffs == null? 0 : buffs.length));
		}
		
		public void fromCereal(DryCereal.Reader from) throws IOException
		{
			id = (Integer)from.next().value;
			status = (Byte)from.next().value;
			x = (Short)from.next().value;
			y = (Short)from.next().value;
			z = (Short)from.next().value;
			dir = (Byte)from.next().value;
			moveTick = (Integer)from.next().value;
			mobiletype = (String)from.next().value;
			name = (String)from.next().value;
			short key = (Short)from.next().value;
			buffs = new Buff[key];
			for (int b = 0; b < key; b++)
			{
				BHClient.Buff buff = new BHClient.Buff();
				buff.fromCereal(from);
				buffs[b] = buff;
			}
			from.next(); // object end
		}		
	}

	public static class Message
	{		
		public int id; 

		public String targetType;
		public int targetID;
		public String message;
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addMoniker(targetType);
			to.addInt(targetID);
			to.addString(message);
		}

		public void fromCereal(DryCereal.Reader from) throws IOException
		{
			id = (Integer)from.next().value;
			targetType = (String)from.next().value;
			targetID = (Integer)from.next().value;
			message = (String)from.next().value;
		}		
	}
	
	public static class Status
	{
		public static class SessionStatus
		{
			public static final String NONE = "";
			public static final String NEW = "NEW";
			public static final String ACTIVE = "ACTIVE";
			public static final String STOPPED = "STOPPED";
			public static final String DEAD = "DEAD";
		}
		
		public int timecode;
		public long updateTS;
		public int sessionID;
		public int controlledMobileID;
		public int cycleMsec;
		public int cycleLoad;
		public String sessionStatus;
	
		//public List<BHMessageList.Message> messages;
		//

		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(timecode);
			to.addLong(updateTS);
			to.addInt(sessionID);
			to.addInt(controlledMobileID);
			to.addInt(cycleMsec);
			to.addInt(cycleLoad);
			to.addMoniker(sessionStatus);
		}

		public void fromCereal(DryCereal.Reader from) throws IOException
		{
			timecode = (Integer)from.next().value;
			updateTS = (Long)from.next().value;
			sessionID = (Integer)from.next().value;
			controlledMobileID = (Integer)from.next().value;
			cycleMsec = (Integer)from.next().value;
			cycleLoad = (Integer)from.next().value;
			sessionStatus = (String)from.next().value;
		}
	}
	
	private static class CellsByCoordsComparator implements Comparator<Cell>
	{

		@Override
		public int compare(Cell arg0, Cell arg1)
		{
			if (arg0.x < arg1.x) return -1;
			if (arg0.x > arg1.x) return 1;
			if (arg0.y < arg1.y) return -1;
			if (arg0.y > arg1.y) return 1;
			if (arg0.z < arg1.z) return -1;
			if (arg0.z > arg1.z) return 1;
			return 0;
		}
	}
	/** Collection is not thread-safe, because it's supposed to serve one client only */
	public static class Collection
	{
		public Status status;
		public Map<Integer, Cell> cells;
		public TreeSet<Cell> orderedCells;
		
		public Map<Integer, Item> items;
		public Map<Integer, Mobile> mobiles;
		
		public Collection()
		{
			orderedCells = new TreeSet<Cell>(new CellsByCoordsComparator());
		}
		
		public Cell getCell(int id)
		{
			return cells.get(id);
		}
		
		public Cell putCell(Cell c)
		{
			Cell oldCell = cells.put(c.id, c);
			if (oldCell != null)
			{
				orderedCells.remove(oldCell);
			}
			orderedCells.add(c);
			return oldCell;
		}
		
		public Cell getCellAtPoint(int x, int y, int z)
		{
			Cell testCell = new Cell();
			testCell.x = x;
			testCell.y = y;
			testCell.z = z;
			Cell res = orderedCells.ceiling(testCell);
			if (res != null && orderedCells.comparator().compare(testCell, res) == 0)
				return res;
			
			return null;
		}
		
		public Cell[] closestCells(int x, int y, int z, String voidTerrain)
		{
			Cell[] res = new Cell[27]; 
			// shifts are 0, 1, -1
			// 0 -> 0, 1 -> 1, -1 -> 2
			// (x + 1) % 3 - 1
			cells.values().stream()
				.filter(q -> 
					q.x >= x - 1 && q.x <= x + 1 &&
					q.y >= y - 1 && q.y <= y + 1 &&
					q.z >= z - 1 && q.z <= z + 1)
				.forEach(q -> 
					//res[(q.m_x - x) + (q.m_y - y) * 3 + (q.m_z - z) * 9 + 13] = q
				res[(q.x - x + 3) % 3 + ((q.y - y + 3) % 3)* 3 + ((q.z - z + 3) % 3) * 9] = q
				)
			;
			if (voidTerrain != null)
			{
				for (int i = 0; i < 27; i++)
				{
					if (res[i] == null)
					{
						int[] shift = cellShifts[i];
						res[i] = new Cell(x + shift[0], 
								y + shift[1], 
								z + shift[2], 
								voidTerrain
						);
					}			
				}
			}
			return res;
		}
	}
	
}
