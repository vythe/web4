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
		@Utils.EnumElement(code=0)
		GLOBAL,
		@Utils.EnumElement(code=1)
		ITEM,
		@Utils.EnumElement(code=2)
		LANDSCAPE,
		@Utils.EnumElement(code=3)
		ERROR,
		@Utils.EnumElement(code=4)
		RECEIPT
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
			private static final int COUNT = 6;
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
			//public static final int HERO_GOLD = 6;
			
			private INT_PROPS() {}
		}
		
		public static class STRING_PROPS
		{		
			private static final int COUNT = 1;
			public static final int NAME = 0;
		}
		
		private final int[] intProps; //= new int[INT_PROPS.COUNT];
		private final int[] intPropsNew; //= new int[INT_PROPS.COUNT];

		private final String[] stringProps; //= new String[STRING_PROPS.COUNT];
		private final String[] stringPropsNew; //= new String[STRING_PROPS.COUNT];

		private int id;
		private int grade;
		private int gradeNew;
		private String type;
		private String typeNew;
		
		private long timecode = 0;
		private boolean m_isChanged;

		private MovementModeEnum movementMode = MovementModeEnum.STANDING;
		private MovementModeEnum movementModeNew = MovementModeEnum.STANDING;
	
		public Atom()
		{
			intProps = new int[INT_PROPS.COUNT];
			intPropsNew = new int[INT_PROPS.COUNT];

			stringProps = new String[STRING_PROPS.COUNT];
			stringPropsNew = new String[STRING_PROPS.COUNT];
			
			setIntProp(INT_PROPS.STATUS, ITEM_STATUS.OK);
			movementMode = MovementModeEnum.STANDING;			
		}
	
		public Atom(int intPropCount, int strPropCount)
		{
			intProps = new int[intPropCount];
			intPropsNew = new int[intPropCount];

			stringProps = new String[strPropCount];
			stringPropsNew = new String[strPropCount];
			
			setIntProp(INT_PROPS.STATUS, ITEM_STATUS.OK);
			movementMode = MovementModeEnum.STANDING;			
		}
	
		
		public String toString()
		{
			return "[" + type + "#" + id + "(" + intProps[INT_PROPS.X] + "," + intProps[INT_PROPS.Y] + ")]";
		}
		
		public final int getIntProp(int ind)
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
		public long getTimecode() { return timecode; }
		public boolean isChanged() { return m_isChanged || timecode == 0; }
		public int getStatus() { return intProps[INT_PROPS.STATUS];}
		public int[] getProps() { return this.intProps; }
		
		public int getGrade() { return grade; }
		public boolean setGrade(int grade) {
			this.gradeNew = grade;
			if (this.grade != grade)
			{
				m_isChanged = true;
			}
			else
			{
				updateIsChanged();
			}
			return m_isChanged;
		}
		
		/** "type" should always be an interned string, so we can compare directly */
		public String getType() { return this.type; }
		public boolean setType(String type) {
			this.typeNew = type;
			if (this.type != type)
			{
				m_isChanged = true;
			}
			else
			{
				updateIsChanged();
			}
			return m_isChanged;
		}
		
		public MovementModeEnum getMovementMode() { return movementMode; }
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
		
		
		public int getX() { return intProps[INT_PROPS.X];}
		public int getY() { return intProps[INT_PROPS.Y];}
		public int getZ() { return intProps[INT_PROPS.Z];}
		public boolean setX(int val) { return setIntProp(INT_PROPS.X, val); }
		public boolean setY(int val) { return setIntProp(INT_PROPS.Y, val); }
		public boolean setZ(int val) { return setIntProp(INT_PROPS.Z, val); }
		public boolean setCoords(BHLandscape.Coords c)
		{
			return  setIntProp(INT_PROPS.X, c.getX()) & setIntProp(INT_PROPS.Y, c.getY()) & setIntProp(INT_PROPS.Z, c.getZ());
		}

		public boolean setStatus(int val) { return setIntProp(INT_PROPS.STATUS, val); }
		
		private boolean updateIsChanged() 
		{
			synchronized (this) 
			{
				for (int i = 0; i < intProps.length; i++)
				{
					if (intProps[i] != intPropsNew[i])
					{
						m_isChanged = true;
						return true;
					}	
				}
				for (int i = 0; i < stringProps.length; i++)
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
						|| type != typeNew
						|| grade != gradeNew
				);
				return m_isChanged;
			}
		}
		
		public Atom publish (long timecode)
		{
			Atom res = new Atom(this.intProps.length, this.stringProps.length);
			
			res.id = this.id;
			res.timecode = timecode;
			res.typeNew = res.type = this.typeNew;
			res.gradeNew = res.grade = this.gradeNew;
			res.movementModeNew = res.movementMode = this.movementModeNew;
			res.m_isChanged = false;
			for (int i = 0; i < this.intPropsNew.length; i++)
			{
				res.intPropsNew[i] = res.intProps[i] = this.intPropsNew[i];
			}
			for (int i = 0; i < this.stringPropsNew.length; i++)
			{
				res.stringPropsNew[i] = res.stringProps[i] = this.stringPropsNew[i];
			}
						
			
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
		//SortedSet<Atom> res0 = atomsByTimecode.headSet(test);
		SortedSet<Atom> res = atomsByTimecode.tailSet(test);
		
		return res;
	}
	
	public Collection<Atom> atCoords(int x, int y, int z, boolean withDeleted)
	{
		ArrayList<Atom> res = new ArrayList<Atom>();
		for (Atom a : atoms.values())
		{
			if (!withDeleted && a.getStatus() == Atom.ITEM_STATUS.DELETE) continue;
			
			if (a.getX() == x
			&& a.getY() == y
			&& a.getZ() == z)
			{
				res.add(a);
			}				
		}
		return res;
	}
	
	public Collection<Atom> atCoords(BHLandscape.Coords c, boolean withDeleted)
	{
		return atCoords(c.getX(), c.getY(), c.getZ(), withDeleted);
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
	
	public Atom addAtom(Atom from)
	{
		Atom res = from.publish(0);
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
	
	//private DryCereal dryer = new DryCereal();
	
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
