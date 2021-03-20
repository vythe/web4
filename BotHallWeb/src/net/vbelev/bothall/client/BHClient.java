package net.vbelev.bothall.client;

import java.io.*;
import java.util.*;

import net.vbelev.utils.DryCereal;
import net.vbelev.utils.Utils;

/**
 * This will be a client-side mirror of the engine - terrain, items, mobs.
 * The server will send in serialized versions of these structs.
 *
 * Why not use the standard java serialization: 
 * it needs to work with javascript and generally export plain text lines.
 * Why not json? too much chaff and we don't need
 * 
 *  This class is a static collection of session data classes.
 */
public class BHClient
{
	public interface Element
	{
		public void toCereal(DryCereal to) throws IOException;
		public void fromCereal(DryCereal.Reader from);
		public int getElementCode();
	}
	
	private BHClient() {}
	
	
	public static class Buff implements Element
	{
		public int id;		
		public String type;
		public int actorID;
		public int actorType;
		/** ticks are not guaranteed to be published every time it changes */
		public int ticks;
		public long timecode;
		public boolean isCancelled;
		//public long changeTimecode;

		public int getElementCode() { return ElementCode.BUFF; }
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addMoniker(type);
			to.addInt(actorID);
			to.addInt(actorType);
			to.addInt(ticks);
			to.addLong(timecode);
			to.addByte(isCancelled?1 : 0);
		}
		
		public void fromCereal(DryCereal.Reader from)
		{
			id = from.next().getInteger();
			type = from.next().getString();
			actorID = from.next().getInteger();
			actorType = from.next().getInteger();
			ticks = from.next().getInteger();
			timecode = from.next().getLong();
			byte bc = from.next().getByte();
			isCancelled = (bc > 0);
		}
	}

	/** same cellShifts as in BHLandscape, it should be shared somehow... */
	public static final int[][] cellShifts = {
			{ 0,  0,  0,  0}, // 0 : self
			{ 1,  0,  0,  2}, // 1: x++
			{-1,  0,  0,  1}, // 2: x--
			{ 0,  1,  0,  6}, // 3: y++
			{ 1,  1,  0,  8},
			{-1,  1,  0,  7}, 
			{ 0, -1,  0,  3}, // 6: y--
			{ 1, -1,  0,  5},
			{-1, -1,  0,  4},
			{ 0,  0,  1, 18}, // 9 : z++
			{ 1,  0,  1, 20}, //10
			{-1,  0,  1, 19},
			{ 0,  1,  1, 24},
			{ 1,  1,  1, 26}, 
			{-1,  1,  1, 25}, 
			{ 0, -1,  1, 21}, // 15
			{ 1, -1,  1, 23},
			{-1, -1,  1, 22},
			{ 0,  0, -1,  9}, //18 : z--
			{ 1,  0, -1, 11},
			{-1,  0, -1, 10}, //20
			{ 0,  1, -1, 15},
			{ 1,  1, -1, 17},
			{-1,  1, -1, 16}, 
			{ 0, -1, -1, 12}, 
			{ 1, -1, -1, 14},
			{-1, -1, -1, 13} // 26
	};

	
	public static class ElementCode
	{
		public static final int ERROR = 0;		
		public static final int STATUS = 1;
		public static final int UPDATEBIN = 2;
		public static final int CELL = 3;
		public static final int ITEM = 4;
		public static final int MOBILE = 5;
		public static final int BUFF = 6;		
		public static final int COMMAND = 7;		
		public static final int MESSAGE = 8;		
	}
	
	public static Element fromCereal(int elementCode, DryCereal.Reader reader)
	{
		Element res = null;
		switch (elementCode)
		{
			case ElementCode.BUFF: res = new BHClient.Buff(); break;
			case ElementCode.CELL: res = new BHClient.Cell(); break;
			case ElementCode.COMMAND: res = new BHClient.Command(); break;
			case ElementCode.ERROR: res = new BHClient.Error(); break;
			case ElementCode.ITEM: res = new BHClient.Item(); break;
			case ElementCode.MESSAGE: res = new BHClient.Message(); break;
			case ElementCode.MOBILE: res = new BHClient.Mobile(); break;
			case ElementCode.STATUS: res = new BHClient.Status(); break;
		}
		if (reader != null && res != null)
			res.fromCereal(reader);
		return res;
		
	}
	
	public static class Cell implements Element, Comparable<Cell>
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
		
		public int getElementCode() { return ElementCode.CELL; }
		

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
		
		public void fromCereal(DryCereal.Reader from) //throws IOException
		{
			id = from.next().getInteger();
			x = from.next().getShort();
			y = from.next().getShort();
			z = from.next().getShort();
			terrain = from.next().getString();
			short key = from.next().getShort();
			buffs = new Buff[key];
			for (int b = 0; b < key; b++)
			{
				BHClient.Buff buff = new BHClient.Buff();
				buff.fromCereal(from);
				buffs[b] = buff;
			}
			from.next(); // object end
		}

		@Override
		public int compareTo(Cell o)
		{
			if (this.x < o.x) return -1;
			if (this.x > o.x) return 1;
			if (this.y < o.y) return -1;
			if (this.y > o.y) return 1;
			if (this.z < o.z) return -1;
			if (this.z > o.z) return 1;
			return 0;
		}
		
		
		@Override
		public String toString()
		{
			String res = "[Cell (" + x + "," + y + ") " + terrain + ", " + (buffs == null? 0 : buffs.length) + " buffs]";
			
			return res;
		}		
	}

	public static class Item implements Element
	{
		public int id;
		public int status;
		
		public int x;
		public int y;
		public int z;
		
		public String itemtype;
		
		public Buff[] buffs;
		
		public int getElementCode() { return ElementCode.ITEM; }
		
		public String toString()
		{
			return "[" + itemtype + " #" + id + " (" + x + ", " + y + ") status=" + status + "]";
		}
		
		
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
		
		public void fromCereal(DryCereal.Reader from) //throws IOException
		{
			id = (Integer)from.next().getInteger();
			status = (Byte)from.next().getByte();
			x = (Short)from.next().getShort();
			y = (Short)from.next().getShort();
			z = (Short)from.next().getShort();
			itemtype = (String)from.next().getString();
			short key = (Short)from.next().getShort();
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

	public static class Mobile implements Element
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
		
		public String toString()
		{
			return "[" + mobiletype + " #" + this.id + " (" + x + ", " + y + "), dir=" + dir + "]";
		}
		
		public int getElementCode() { return ElementCode.MOBILE; }
		
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
		
		public void fromCereal(DryCereal.Reader from)
		{
			id = (Integer)from.next().getInteger();
			status = (Byte)from.next().getByte();
			x = (Short)from.next().getShort();
			y = (Short)from.next().getShort();
			z = (Short)from.next().getShort();
			dir = (Byte)from.next().getByte();
			moveTick = (Integer)from.next().getInteger();
			mobiletype = (String)from.next().getString();
			name = (String)from.next().getString();
			short key = (Short)from.next().getShort();
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

	public static class Message implements Element
	{		
		public int id; 

		public String targetType;
		public int targetID;
		public String message;
		
		public String toString()
		{
			return "[" + targetType + " targetID:" + this.targetID + " " + message + "]";
		}
		
		public int getElementCode() { return ElementCode.MESSAGE; }
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(id);
			to.addMoniker(targetType);
			to.addInt(targetID);
			to.addString(message);
		}

		public void fromCereal(DryCereal.Reader from)
		{
			id = (Integer)from.next().getInteger();
			targetType = (String)from.next().getString();
			targetID = (Integer)from.next().getInteger();
			message = (String)from.next().getString();
		}		
	}
	
	public static class Command implements Element
	{
		public String command = "";
		/** the timecode at which the command was issued. It is for reference only */
		public int timecode;
		public int[] intArgs;
		public String[] stringArgs;
			
		public int getElementCode() { return ElementCode.COMMAND; }
		
		public Command()
		{
			intArgs = null;
			stringArgs = null;
		}
		public Command(int intArgsCount, int stringArgsCount)
		{
			intArgs = new int[intArgsCount];
			stringArgs = new String[stringArgsCount];
		}
		
		public Command(String cmd, int[] intArgs, String[] stringArgs)
		{
			command = cmd;
			if (intArgs == null)
			{
				this.intArgs = new int[0];
			}
			else
			{
				this.intArgs = new int[intArgs.length];
				for (int i = 0; i < intArgs.length; i++) 
					this.intArgs[i] = intArgs[i];
			}
			if (stringArgs == null)
			{
				this.stringArgs = new String[0];
			}
			else
			{
				this.stringArgs = new String[stringArgs.length];
				for (int i = 0; i < stringArgs.length; i++) 
					this.stringArgs[i] = stringArgs[i];
			}
		}
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addMoniker(command);
			to.addInt(timecode);
			to.addObjectStart((short)(intArgs == null? 0 : intArgs.length));
			if (intArgs != null)
			{
				for (int a : intArgs) to.addInt(a);
			}			
			to.addObjectEnd((short)(intArgs == null? 0 : intArgs.length));
			to.addObjectStart((short)(stringArgs == null? 0 : stringArgs.length));
			if (stringArgs != null)
			{
				for (String s : stringArgs) to.addString(s);
			}			
			to.addObjectEnd((short)(stringArgs == null? 0 : stringArgs.length));
		}
		
		public void fromCereal(DryCereal.Reader from)
		{
			command = (String)from.next().getString();
			timecode = (Integer)from.next().getInteger();
			short intArgsKey = (Short)from.next().getShort();
			intArgs = new int[intArgsKey];
			for (int b = 0; b < intArgsKey; b++)
			{
				intArgs[b] = (Integer)from.next().getInteger();
			}
			short intArgsEnd = from.next().getShort(); // object end
			
			short stringArgsKey = (Short)from.next().getShort();
			stringArgs = new String[stringArgsKey];
			for (int b = 0; b < stringArgsKey; b++)
			{
				stringArgs[b] = (String)from.next().getString();
			}
			short stringArgsEnd = from.next().getShort(); // object end
		}		
		
		@Override
		public String toString()
		{
			String res = "[Command " + command + " (" + timecode + ") ("
			+ String.join(", ", new Utils.StringIterable<Integer>(Utils.box(intArgs))) + "), ("
			+ String.join(", ", stringArgs) + ")"
			;
			
			return res;
		}
	}
	
	public static class Error implements Element
	{
		/** the timecode at which the error happened. It is for reference only */
		public int timecode;
		public String message = "";
		
		public Error()
		{
		}
		
		public Error(int timecode, String message)
		{
			this.timecode = timecode;
			this.message = message;
		}
		
		public int getElementCode() { return ElementCode.ERROR; }
		
		public void toCereal(DryCereal to) throws IOException
		{
			to.addInt(timecode);
			to.addString(message);
		}
		
		public void fromCereal(DryCereal.Reader from)
		{
			timecode = (Integer)from.next().getInteger();
			message = (String)from.next().getString();
		}		
		
		@Override
		public String toString()
		{
			String res = "[Error " + message + " (" + timecode + ")]";			
			return res;
		}
	}

	public static class Status implements Element
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

		public int getElementCode() { return ElementCode.STATUS; }
		
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

		public void fromCereal(DryCereal.Reader from)
		{
			timecode = (Integer)from.next().getInteger();
			updateTS = (Long)from.next().getLong();
			sessionID = (Integer)from.next().getInteger();
			controlledMobileID = (Integer)from.next().getInteger();
			cycleMsec = (Integer)from.next().getInteger();
			cycleLoad = (Integer)from.next().getInteger();
			sessionStatus = (String)from.next().getString();
		}
	}
	/*
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
	*/
	
	/** the Client collection is not thread-safe, because it's supposed to serve one client only */
	public static class Client
	{
		public Status status;
		public Map<Integer, Cell> cells;
		public TreeSet<Cell> orderedCells;
		
		public Map<Integer, Item> items;
		public Map<Integer, Mobile> mobiles;
		
		public Client()
		{
			cells = new Hashtable<Integer, Cell>();
			orderedCells = new TreeSet<Cell>();
			items = new Hashtable<Integer, Item>();
			mobiles = new Hashtable<Integer, Mobile>();
			//orderedCells = new TreeSet<Cell>(new CellsByCoordsComparator());
			//orderedCells = new TreeSet<Cell>(new Comparator<Cell>()
			//{
			//	@Override
			//	public int compare(Cell arg0, Cell arg1)
			//	{
			//		if (arg0.x < arg1.x) return -1;
			//		if (arg0.x > arg1.x) return 1;
			//		if (arg0.y < arg1.y) return -1;
			//		if (arg0.y > arg1.y) return 1;
			//		if (arg0.z < arg1.z) return -1;
			//		if (arg0.z > arg1.z) return 1;
			//		return 0;
			//	}
			//});
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
			Cell testCell = new Cell(x, y, z, "");			
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
			// okay, we tried streams here. Not worth the effort.
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
		
		public  Mobile[] getMobiles(int x, int y, int z)
		{
			Mobile[] res = new Mobile[this.mobiles.size()];
			int cnt = 0;
			for (Mobile m : this.mobiles.values())
			{
				if (m.x == x && m.y == y && m.z == z)
				{
					res[cnt++] = m;
				}
			}
			return Arrays.copyOf(res, cnt);
		}
	}
	
}
