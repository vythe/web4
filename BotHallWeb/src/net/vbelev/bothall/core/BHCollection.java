package net.vbelev.bothall.core;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

//import net.vbelev.utils.DryCereal;
import net.vbelev.utils.*;
import net.vbelev.bothall.client.*;

/**
 * The base class for BH object
 * @author Vythe
 *
 */
public class BHCollection
{
	public enum EntityTypeEnum
	{
		GLOBAL,
		ITEM,
		LANDSCAPE
	}
		
	
	public enum MovementModeEnum
	{
		STANDING,
		FLYING,
		FALLING
	}
	
	private int latestAtomID = 0;
	private boolean isObsolete = false;
	private final Map<Integer, Atom> atoms;
	private final Map<Integer, String> itemCereals;
	private final Map<Integer, String> mobileCereals;
	/** The list of items, sorted by timecode. Unpublished items will not appear here! */
	private final TreeSet<Atom> atomsByTimecode;
	/** 
	 * This class is tricky: 
	 * @author Vythe
	 *
	 */
	public static class Atom implements BHLandscape.Coords
	{
		public static class GRADE
		{
			public final static int NONE = 0;
			public final static int HERO = 1;
			public final static int MONSTER = 2;
			public final static int ITEM = 3;
			
			private static final String[] names = new String[]{
					"", "HERO", "MONSTER", "ITEM"
			};
			
			public static String toString(int val)
			{
				if (val < 0 || val >= names.length) return "";
				return names[val];
			}
			
			public static int fromString(String name)
			{
				if (name == null) return NONE;
				for (int i = 0; i < names.length; i++)
				{
					if (name.equals(names[i])) return i;
				}
				return -1;					
			}
		}

		public static class ITEM_STATUS
		{
			public static final int OK = 0;
			public static final int DELETE = 1;
		}
			
		public static class INT_PROPS
		{
			private static final int COUNT = 10;
			public static final int X = 0;
			public static final int Y = 1;
			public static final int Z = 2;
			public static final int DX = 3;
			public static final int DY = 4;
			public static final int DZ = 5;
			/** base timecode for movements */
			public static final int MOVE_TC = 6;
			public static final int MOVE_DIR = 7;
			public static final int STATUS = 8;
			public static final int HERO_GOLD = 9;
			
			private INT_PROPS() {}
		}
		
		public static class STRING_PROPS
		{		
			private static final int COUNT = 1;
			public static final int NAME = 0;
		}
		
		private final int[] intProps = new int[INT_PROPS.COUNT];
		private final int[] intPropsNew = new int[INT_PROPS.COUNT];

		private final String[] stringProps = new String[STRING_PROPS.COUNT];
		private final String[] stringPropsNew = new String[STRING_PROPS.COUNT];

		private int id;
		private int grade;
		private String type;
		private long timecode = 0;
		private boolean m_isChanged;

		private MovementModeEnum movementMode = MovementModeEnum.STANDING;
		private MovementModeEnum movementModeNew = MovementModeEnum.STANDING;
	
		public String toString()
		{
			return "[" + type + "#" + id + "(" + intProps[INT_PROPS.X] + "," + intProps[INT_PROPS.Y] + ")]";
		}
		
		public int getIntProp(int ind)
		{
			return intProps[ind]; // if he used an index out of bounds, it's his own fault
		}
		
		public boolean setIntProp(int ind, int val)
		{
			synchronized(this)
			{
				intPropsNew[ind] = val;
				if (intProps[ind] != val)
				{
					m_isChanged = true;
				}
				else
				{
					updateIsChanged();
				}
				return m_isChanged;
			}
		}
		
		public String getStringProp(int id)
		{
			return stringProps[id];
		}
		
		public boolean setStringProp(int ind, String val)
		{
			synchronized(this)
			{
				stringPropsNew[ind] = val;
				if (!Utils.equals(val, stringProps[ind]))
				{
					m_isChanged = true;
				}
				else
				{
					updateIsChanged();
				}
				return m_isChanged;
			}
		}
		
		public int getID() { return id; }
		public int getGrade() { return grade; }
		public long getTimecode() { return timecode; }
		public boolean isChanged() { return m_isChanged || timecode == 0; }
		public int getX() { return intProps[INT_PROPS.X];}
		public int getY() { return intProps[INT_PROPS.Y];}
		public int getZ() { return intProps[INT_PROPS.Z];}
		public int getStatus() { return intProps[INT_PROPS.STATUS];}
		public int[] getProps() { return this.intProps; }
		/** "type" should always be an interned string, so we can compare directly */
		public String getType() { return this.type; }
		public MovementModeEnum getMovementMode() { return movementMode; }
		
		public boolean setX(int val) { return setIntProp(INT_PROPS.X, val); }
		public boolean setY(int val) { return setIntProp(INT_PROPS.Y, val); }
		public boolean setZ(int val) { return setIntProp(INT_PROPS.Z, val); }
		public boolean setCoords(BHLandscape.Coords c)
		{
			return  setIntProp(INT_PROPS.X, c.getX()) && setIntProp(INT_PROPS.Y, c.getY()) && setIntProp(INT_PROPS.Z, c.getZ());
		}
		public boolean setStatus(int val) { return setIntProp(INT_PROPS.STATUS, val); }
		
		public boolean setMovementMode(MovementModeEnum mode)
		{
			this.movementModeNew = mode;
			if (this.movementMode != mode)
			{
				m_isChanged = true;
			}
			else
			{
				updateIsChanged();
			}
			return m_isChanged;
			
		}
		
		public Atom()
		{
			setIntProp(INT_PROPS.STATUS, ITEM_STATUS.OK);
			movementMode = MovementModeEnum.STANDING;
		}
	
		private boolean updateIsChanged() 
		{
			synchronized (this) 
			{
				for (int i = 0; i < INT_PROPS.COUNT; i++)
				{
					if (intProps[i] != intPropsNew[i])
					{
						m_isChanged = true;
						return true;
					}	
				}
				for (int i = 0; i < STRING_PROPS.COUNT; i++)
				{
					if (!Utils.equals(stringProps[i], stringPropsNew[i]))
					{
						m_isChanged = true;
						return true;				
					}
				}
				// check other properties
				m_isChanged = (
						movementMode != movementModeNew
				);
				return m_isChanged;
			}
		}
		
		public Atom publish (long timecode)
		{
			Atom res = new Atom();
			res.id = this.id;
			res.type = this.type;
			res.timecode = timecode;
			res.grade = this.grade;
			res.m_isChanged = false;
			for (int i = 0; i < INT_PROPS.COUNT; i++)
			{
				res.intPropsNew[i] = res.intProps[i] = this.intPropsNew[i];
			}
			for (int i = 0; i < STRING_PROPS.COUNT; i++)
			{
				res.stringPropsNew[i] = res.stringProps[i] = this.stringPropsNew[i];
			}
						
			res.movementMode = this.movementModeNew;
			
			return res;
		}
	}

	public BHCollection()
	{
		atoms = new TreeMap<Integer, Atom>();
		itemCereals = new TreeMap<Integer, String>();
		mobileCereals = new TreeMap<Integer, String>();
		atomsByTimecode = new TreeSet<BHCollection.Atom>(new Comparator<BHCollection.Atom>()
				{
					@Override
					public int compare(Atom arg0, Atom arg1)
					{
						if (arg0.timecode < arg1.timecode) return -1;
						if (arg0.timecode > arg1.timecode) return 1;
						if (arg0.id < arg1.id) return -1;
						if (arg0.id > arg1.id) return 1;
						return 0;
					}
				}
		);
	}
	
	private BHCollection(BHCollection from)
	{
		atoms = new TreeMap<Integer, Atom>(from.atoms);
		itemCereals = new TreeMap<Integer, String>(from.itemCereals);
		mobileCereals = new TreeMap<Integer, String>(from.mobileCereals);
		this.latestAtomID = from.latestAtomID;

		atomsByTimecode = new TreeSet<BHCollection.Atom>(from.atomsByTimecode);
		/*
		// note how itemsByTimecode are still empty here
		itemsByTimecode = new TreeSet<BHCollection.Atom>(new Comparator<BHCollection.Atom>()
		{
			@Override
			public int compare(Atom arg0, Atom arg1)
			{
				if (arg0.timecode < arg1.timecode) return -1;
				if (arg0.timecode > arg1.timecode) return 1;
				if (arg0.id < arg1.id) return -1;
				if (arg0.id > arg1.id) return 1;
				return 0;
			}
		});
		*/
	}
	public Atom getItem(int id)
	{
		return atoms.get(id);
	}	

	public Collection<Atom> all()
	{
		return atoms.values();
	}
	
	/** returns published items with the timecode of minTimecode or greater */
	public Collection<Atom> allByTimecode(long minTimecode)
	{
		Atom test = new Atom();
		test.id = 0;
		test.timecode = minTimecode;
		SortedSet<Atom> res0 = atomsByTimecode.headSet(test);
		SortedSet<Atom> res = atomsByTimecode.tailSet(test);
		
		return res;
	}
	
	public Collection<Atom> atCoords(int x, int y, int z)
	{
		return atCoords(new BHLandscape.CoordsBase(x, y, z));
	}
	
	public Collection<Atom> atCoords(BHLandscape.Coords c)
	{
		ArrayList<Atom> res = new ArrayList<Atom>();
		for (Atom a : atoms.values())
		{
			if (a.getX() == c.getX()
			&& a.getY() == c.getY()
			&& a.getZ() == c.getZ())
			{
				res.add(a);
			}				
		}
		return res;
	}	
	
	public Atom addAtom(String type, int grade)
	{
		Atom res = new Atom();
		res.type = type;
		res.grade = grade;
		res.m_isChanged = true;
		res.timecode = 0; // this means it is unpublished yet
		synchronized(atoms)
		{
			do
			{
				res.id = ++latestAtomID;
			} 
			while (atoms.containsKey(res.id));
			atoms.put(res.id, res);
		}
		return res;		
	}
	
	public BHCollection publish(long timecode)
	{
		synchronized (this)
		{
			BHCollection res = new BHCollection(this);
			for(Entry<Integer, Atom> ea : this.atoms.entrySet())
			{
				Atom a = ea.getValue();
				if (a.isChanged())
				{
					res.atomsByTimecode.remove(a); // remove with the old timecode
					Atom newA = a.publish(timecode);
					res.atoms.put(newA.getID(), newA);
					res.atomsByTimecode.add(newA); // add with the new timecode
					/*
					String cereal;
					if (a.grade == BHCollection.Atom.GRADE.ITEM)
					{
						cereal = atomToItemCereal(a);
						this.itemCereals.put(a.id, cereal);
					}
					else if (a.grade == BHCollection.Atom.GRADE.MOBILE || a.grade == BHCollection.Atom.GRADE.HERO)
					{
						cereal = atomToMobileCereal(a);
						this.mobileCereals.put(a.id, cereal);
					}
					else
					{
						cereal = null;
					}
						*/		
				}
			}
			this.isObsolete = true;
			return res;
		}		
	}
	
	private DryCereal dryer = new DryCereal();
	
	/** This works over the published atom */
	public static String atomToItemCereal(DryCereal dryer, Atom atom)
	{
		BHClient.Item item = new BHClient.Item();
		item.id = atom.id;
		item.x = atom.intProps[Atom.INT_PROPS.X];
		item.y = atom.intProps[Atom.INT_PROPS.Y];
		item.z = atom.intProps[Atom.INT_PROPS.Z];
		item.itemtype = atom.type;
		
		try
		{
			// we need to go through BHClient.Item to make sure the serialization is consistent.
			item.toCereal(dryer);
			return dryer.pull();
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
	/** This works over the published atom */
	public static String atomToMobileCereal(DryCereal dryer, Atom atom)
	{
		BHClient.Mobile mobile = new BHClient.Mobile();
		mobile.id = atom.id;
		mobile.x = atom.intProps[Atom.INT_PROPS.X];
		mobile.y = atom.intProps[Atom.INT_PROPS.Y];
		mobile.z = atom.intProps[Atom.INT_PROPS.Z];
		mobile.dir = atom.intProps[Atom.INT_PROPS.MOVE_DIR];
		mobile.mobiletype = atom.type;
		mobile.name = atom.stringProps[Atom.STRING_PROPS.NAME];
		
		try
		{
			mobile.toCereal(dryer);
			return dryer.pull();
			
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
}
